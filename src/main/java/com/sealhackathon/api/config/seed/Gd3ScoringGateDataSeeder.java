package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Seed GĐ3 — prelim active, judge gán, slot WAITING (đội 2) → POST score → SCORING_NOT_OPEN.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3ScoringGateDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd3.scoring-gate.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd3ScoringGateDataSeeder] Tắt (app.seed.gd3.scoring-gate.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(true, true, false, false, 2, 4);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd3ScoringGateSeedConstants.SLUG_GD3_SCORING_GATE,
                "SEAL GĐ3 — Scoring gate",
                HackathonStatus.ONGOING,
                "Seed GĐ3 — judge chấm slot WAITING → SCORING_NOT_OPEN",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd3ActivePrelimDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();

        if (needsRepair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd3Retest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd3ScoringGateSeedConstants.SLUG_GD3_SCORING_GATE,
                seedHelper.computeGd3ActivePrelimDates());
        prelim = loadPrelim(hackathon.getId());

        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = prelim.getSubmissionDeadline().minusHours(2);

        String[] teamNames = {
                Gd3ScoringGateSeedConstants.TEAM_PRESENTING,
                Gd3ScoringGateSeedConstants.TEAM_WAITING
        };
        List<Submission> subs = new ArrayList<>();
        for (int i = 0; i < teamNames.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd3ScoringGateSeedConstants.studentEmail(idx),
                    Gd3ScoringGateSeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeamForLeader(hackathon, teamNames[i], leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            seedHelper.ensureLottery(hackathon, prelim, track1, "BANG-A", team, coordinator, now);
            Submission sub = seedHelper.ensurePrelimSubmission(
                    hackathon, prelim, track1, team, SubmissionStatus.SUBMITTED, false, submittedAt);
            seedHelper.markSubmissionSlideSeeded(sub);
            seedHelper.clearSubmissionScores(sub.getId());
            subs.add(sub);
        }

        seedHelper.ensureJudgeOnTrack(judge1, track1, coordinator);
        // Index 0 = PRESENTING, index 1 = WAITING — probe chấm submission đội 2 → SCORING_NOT_OPEN
        seedHelper.seedPresentationQueue(prelim, track1, subs, 0);

        log.info("""
                [Gd3ScoringGateDataSeeder] slug={} prelimRoundId={}
                  T01 PRESENTING | T02 WAITING — POST score T02 → SCORING_NOT_OPEN
                  judge: judge1@fpt.edu.vn | students: {} / {}
                """,
                Gd3ScoringGateSeedConstants.SLUG_GD3_SCORING_GATE,
                prelim.getId(),
                Gd3ScoringGateSeedConstants.studentEmail(1),
                Gd3ScoringGateSeedConstants.studentEmail(2));
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd3ScoringGateSeedConstants.SLUG_GD3_SCORING_GATE).ifPresent(h -> {
            seedHelper.syncHackathonCalendarFromDates(
                    Gd3ScoringGateSeedConstants.SLUG_GD3_SCORING_GATE,
                    seedHelper.computeGd3ActivePrelimDates());
            ensureSeed();
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd3ScoringGateSeedConstants.SLUG_GD3_SCORING_GATE);
        ensureSeed();
    }

    private boolean needsRepair(Hackathon hackathon, Round prelim, Round finalRound) {
        return hackathon.getStatus() != HackathonStatus.ONGOING
                || Boolean.TRUE.equals(prelim.getScoringLocked())
                || Boolean.TRUE.equals(prelim.getIsPublished())
                || Boolean.TRUE.equals(finalRound.getIsActive());
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
