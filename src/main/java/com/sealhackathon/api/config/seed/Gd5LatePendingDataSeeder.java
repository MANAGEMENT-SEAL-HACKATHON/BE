package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
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

/** GĐ5 — CK LATE_PENDING sau deadline (ALLOW_LATE_PENDING). */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd5LatePendingDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd5.late-pending.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd5LatePendingDataSeeder] Tắt");
            return;
        }

        HackathonDevSeedHelper.SeedDates dates = seedHelper.computeGd5LatePendingDates();
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd5LatePendingSeedConstants.SLUG_GD5_LATE_PENDING,
                "SEAL GĐ5 — CK late pending",
                HackathonStatus.ONGOING,
                "Seed GĐ5 — CK LATE_PENDING sau deadline",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 2, 2),
                new HackathonDevSeedHelper.FinalState(true, false),
                dates);

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        seedHelper.syncHackathonCalendarFromDates(Gd5LatePendingSeedConstants.SLUG_GD5_LATE_PENDING, dates);
        seedHelper.repairHackathonForGd5Retest(hackathon, prelim, finalRound);
        finalRound = loadFinal(hackathon.getId());
        finalRound.setLateSubmissionPolicy(LateSubmissionPolicy.ALLOW_LATE_PENDING);
        finalRound.setSubmissionDeadline(dates.finalDeadline());
        finalRound.setSubmissionOpen(dates.finalSubmissionOpen());
        roundRepository.save(finalRound);
        seedHelper.releaseFinalProblem(finalRound);

        User coordinator = seedHelper.requireCoordinator();
        Chapter chapter = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        User leader = seedHelper.upsertStudent(
                Gd5LatePendingSeedConstants.studentEmail(),
                "GD5 Late Pending Leader",
                chapter);
        seedHelper.registerStudent(hackathon, leader);
        Team team = seedHelper.ensureActiveTeam(
                hackathon, Gd5LatePendingSeedConstants.TEAM_NAME, leader, chapter, now);
        seedHelper.ensureLottery(hackathon, prelim, track1, "BANG-A", team, coordinator, now);
        seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
        seedHelper.ensureFinalSubmission(hackathon, finalRound, team,
                SubmissionStatus.LATE_PENDING, true, dates.finalDeadline().plusMinutes(30));

        log.info("""
                [Gd5LatePendingDataSeeder] slug={} submission=LATE_PENDING
                  Coord late-review CK / approve → LATE_PENDING_NOT_ALLOWED path
                """,
                Gd5LatePendingSeedConstants.SLUG_GD5_LATE_PENDING);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd5LatePendingSeedConstants.SLUG_GD5_LATE_PENDING).ifPresent(h -> {
            seedHelper.syncHackathonCalendarFromDates(
                    Gd5LatePendingSeedConstants.SLUG_GD5_LATE_PENDING,
                    seedHelper.computeGd5LatePendingDates());
            seedHelper.clearFinalRoundArtifacts(h.getId());
            ensureSeed();
        });
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd5LatePendingSeedConstants.SLUG_GD5_LATE_PENDING);
        ensureSeed();
    }

    private Round loadFinal(Integer hackathonId) {
        return roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElseThrow();
    }
}
