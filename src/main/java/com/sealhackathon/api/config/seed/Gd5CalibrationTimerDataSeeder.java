package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.calibration_sessions.entity.CalibrationSession;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
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
 * Seed GĐ5 — calibration OPEN trên CK + queue timer QA.
 *
 * <p>Doc: {@code docs/testing/gd5-full-test-matrix-and-seeds.md} § Profile C
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd5CalibrationTimerDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd5.calibration-timer.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd5CalibrationTimerDataSeeder] Tắt (app.seed.gd5.calibration-timer.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd5CalibrationTimerSeedConstants.SLUG_GD5_CALIBRATION_TIMER,
                "SEAL GĐ5 — Calibration & timer",
                HackathonStatus.ONGOING,
                "Seed GĐ5 — calibration OPEN trên CK, queue timer QA",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 2, 2),
                new HackathonDevSeedHelper.FinalState(true, false),
                seedHelper.computeGd5FinalActiveDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        seedHelper.syncHackathonCalendarFromDates(
                Gd5CalibrationTimerSeedConstants.SLUG_GD5_CALIBRATION_TIMER,
                seedHelper.computeGd5FinalActiveDates());
        seedHelper.repairHackathonForGd5Retest(hackathon, prelim, finalRound);

        User coordinator = seedHelper.requireCoordinator();
        User guestJudge = seedHelper.requireGuestJudge();
        Chapter chapter = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime qaStarted = now.minusMinutes(5);

        seedHelper.ensureGuestJudgeInvitation(hackathon, guestJudge, coordinator);

        List<Submission> finalSubs = new ArrayList<>();
        for (int i = 0; i < Gd5CalibrationTimerSeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd5CalibrationTimerSeedConstants.studentEmail(idx),
                    Gd5CalibrationTimerSeedConstants.studentDisplayName(idx),
                    chapter);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeam(
                    hackathon, Gd5CalibrationTimerSeedConstants.TEAM_NAMES[i], leader, chapter, now);
            Track track = (idx % 2 == 1) ? track1 : track2;
            String group = "BANG-" + ((idx % 2) + 1);
            seedHelper.ensureLottery(hackathon, prelim, track, group, team, coordinator, now);
            seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
            finalSubs.add(seedHelper.ensureFinalSubmission(
                    hackathon, finalRound, team, "https://github.com/seal-warriors/gd5c-team%02d".formatted(idx)));
        }

        CalibrationSession session = seedHelper.ensureOpenCalibrationSession(
                finalRound,
                finalSubs.get(0),
                coordinator,
                8.0f,
                "Seed GĐ5 — calibration CK mẫu");

        var presentingTimer = new HackathonDevSeedHelper.PresentationTimerSeed(
                PresentationTimerPhase.QA,
                qaStarted.minusMinutes(8),
                qaStarted,
                null,
                null,
                0);
        seedHelper.seedFinalPresentationQueue(finalRound, finalSubs, 2, presentingTimer);

        log.info("""
                [Gd5CalibrationTimerDataSeeder] slug={} finalRoundId={} calibrationSessionId={}
                  queue: T3 PRESENTING (QA timer)
                """,
                Gd5CalibrationTimerSeedConstants.SLUG_GD5_CALIBRATION_TIMER,
                finalRound.getId(),
                session.getId());
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd5CalibrationTimerSeedConstants.SLUG_GD5_CALIBRATION_TIMER).ifPresent(h -> {
            Round prelim = loadPrelim(h.getId());
            Round finalRound = loadFinal(h.getId());
            seedHelper.repairGd5ScoringLiveFeTesting(h, prelim, finalRound);
            reseedCalibrationState(h, finalRound);
        });
    }

    private void reseedCalibrationState(Hackathon hackathon, Round finalRound) {
        User coordinator = seedHelper.requireCoordinator();
        List<Submission> finalSubs = new ArrayList<>();
        for (String teamName : Gd5CalibrationTimerSeedConstants.TEAM_NAMES) {
            var teamOpt = teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(hackathon.getId(), teamName);
            if (teamOpt.isEmpty()) {
                return;
            }
            finalSubs.add(seedHelper.ensureFinalSubmission(
                    hackathon, finalRound, teamOpt.get(),
                    "https://github.com/seal-warriors/gd5c-" + teamOpt.get().getId()));
        }
        if (finalSubs.isEmpty()) {
            return;
        }
        seedHelper.ensureOpenCalibrationSession(
                finalRound, finalSubs.get(0), coordinator, 8.0f, "Seed GĐ5 — calibration CK mẫu");
        LocalDateTime qaStarted = LocalDateTime.now().minusMinutes(5);
        var presentingTimer = new HackathonDevSeedHelper.PresentationTimerSeed(
                PresentationTimerPhase.QA,
                qaStarted.minusMinutes(8),
                qaStarted,
                null,
                null,
                0);
        seedHelper.seedFinalPresentationQueue(finalRound, finalSubs, 2, presentingTimer);
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd5CalibrationTimerSeedConstants.SLUG_GD5_CALIBRATION_TIMER);
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
