package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** GĐ5 — CK active, submission SUBMITTED, không judge → JUDGE_NOT_ASSIGNED. */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd5JudgeEdgeDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd5.judge-edge.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd5JudgeEdgeDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd5FinalActiveDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd5JudgeEdgeSeedConstants.SLUG_GD5_JUDGE_EDGE,
                "SEAL GĐ5 — Judge edge",
                HackathonStatus.ONGOING,
                "Seed GĐ5 — CK active, không judge assign → JUDGE_NOT_ASSIGNED",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 2, 2),
                new HackathonDevSeedHelper.FinalState(true, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        seedHelper.syncHackathonCalendarFromDates(Gd5JudgeEdgeSeedConstants.SLUG_GD5_JUDGE_EDGE, dates);
        seedHelper.repairHackathonForGd5Retest(hackathon, prelim, finalRound);
        finalRound = loadFinal(hackathon.getId());
        seedHelper.clearFinalJudgeAssignments(finalRound);

        User coordinator = seedHelper.requireCoordinator();
        Chapter chapter = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        User leader = seedHelper.upsertStudent(
                Gd5JudgeEdgeSeedConstants.studentEmail(),
                "GD5 Judge Edge Leader",
                chapter);
        seedHelper.registerStudent(hackathon, leader);
        Team team = seedHelper.ensureActiveTeam(
                hackathon, Gd5JudgeEdgeSeedConstants.TEAM_NAME, leader, chapter, now);
        seedHelper.ensureLottery(hackathon, prelim, track1, "BANG-A", team, coordinator, now);
        seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
        seedHelper.ensureFinalSubmission(hackathon, finalRound, team,
                SubmissionStatus.SUBMITTED, false, now.minusHours(1));

        log.info("""
                [Gd5JudgeEdgeDataSeeder] slug={} finalRoundId={}
                  judge1 POST score → JUDGE_NOT_ASSIGNED
                """,
                Gd5JudgeEdgeSeedConstants.SLUG_GD5_JUDGE_EDGE,
                finalRound.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd5JudgeEdgeSeedConstants.SLUG_GD5_JUDGE_EDGE).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            seedHelper.syncHackathonCalendarFromDates(
                    Gd5JudgeEdgeSeedConstants.SLUG_GD5_JUDGE_EDGE, seedHelper.computeGd5FinalActiveDates());
            seedHelper.repairHackathonForGd5Retest(h, prelim, finalRound);
            seedHelper.clearFinalJudgeAssignments(loadFinal(h.getId()));
            ensureSeed();
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd5JudgeEdgeSeedConstants.SLUG_GD5_JUDGE_EDGE);
        ensureSeed();
    }

    private Round loadPrelim(Integer hackathonId) {
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElseThrow();
    }

    private Round loadFinal(Integer hackathonId) {
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElseThrow();
    }
}
