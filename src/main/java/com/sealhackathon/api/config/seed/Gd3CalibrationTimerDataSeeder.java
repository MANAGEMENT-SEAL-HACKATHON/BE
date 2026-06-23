package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.calibration_sessions.entity.CalibrationSession;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
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
 * Seed GĐ3 — calibration session OPEN + queue timer PAUSED / QA.
 *
 * <p>Doc: {@code docs/testing/gd3-full-test-matrix-and-seeds.md} § Profile E
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3CalibrationTimerDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final TrackRepository trackRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd3.calibration-timer.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd3CalibrationTimerDataSeeder] Tắt (app.seed.gd3.calibration-timer.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(true, true, false, false, 2, 4);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd3CalibrationTimerSeedConstants.SLUG_GD3_CALIBRATION_TIMER,
                "SEAL GĐ3 — Calibration & timer",
                HackathonStatus.ONGOING,
                "Seed GĐ3 — calibration OPEN, queue PAUSED (T1) + QA (T2)",
                prelimState,
                new HackathonDevSeedHelper.FinalState(false, false),
                seedHelper.computeGd3ActivePrelimDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        if (needsRepair(hackathon, prelim, finalRound)) {
            seedHelper.repairHackathonForGd3Retest(hackathon, prelim, finalRound);
            hackathon = hackathonRepository.findById(hackathon.getId()).orElse(hackathon);
            prelim = loadPrelim(hackathon.getId());
            finalRound = loadFinal(hackathon.getId());
            track1 = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId()).get(0);
            track2 = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId()).get(1);
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd3CalibrationTimerSeedConstants.SLUG_GD3_CALIBRATION_TIMER,
                seedHelper.computeGd3ActivePrelimDates());
        prelim = loadPrelim(hackathon.getId());

        User coordinator = seedHelper.requireCoordinator();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = now.minusHours(3);

        List<Submission> track1Subs = new ArrayList<>();
        List<Submission> track2Subs = new ArrayList<>();

        for (int i = 0; i < Gd3CalibrationTimerSeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd3CalibrationTimerSeedConstants.studentEmail(idx),
                    Gd3CalibrationTimerSeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeamForLeader(
                    hackathon, Gd3CalibrationTimerSeedConstants.TEAM_NAMES[i], leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            Track track = idx <= 3 ? track1 : track2;
            seedHelper.ensureLottery(
                    hackathon,
                    prelim,
                    track,
                    Gd3CalibrationTimerSeedConstants.GROUPS[i],
                    team,
                    coordinator,
                    now);
            Submission sub = seedHelper.ensurePrelimSubmission(
                    hackathon, prelim, track, team, SubmissionStatus.SUBMITTED, false, submittedAt);
            seedHelper.markSubmissionSlideSeeded(sub);
            seedHelper.clearSubmissionScores(sub.getId());
            if (idx <= 3) {
                track1Subs.add(sub);
            } else {
                track2Subs.add(sub);
            }
        }

        seedCalibrationAndQueues(prelim, track1, track2, track1Subs, track2Subs, coordinator);

        log.info("""
                [Gd3CalibrationTimerDataSeeder] slug={} prelimRoundId={}
                  Calibration OPEN (sample=CT01) | T1: CT03 PAUSED | T2: CT04 QA
                  students: {} … {} password={}
                """,
                Gd3CalibrationTimerSeedConstants.SLUG_GD3_CALIBRATION_TIMER,
                prelim.getId(),
                Gd3CalibrationTimerSeedConstants.studentEmail(1),
                Gd3CalibrationTimerSeedConstants.studentEmail(5),
                DevSeedCatalog.DEV_STUDENT_PASSWORD);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd3CalibrationTimerSeedConstants.SLUG_GD3_CALIBRATION_TIMER).ifPresent(h -> {
            seedHelper.syncHackathonCalendarFromDates(
                    Gd3CalibrationTimerSeedConstants.SLUG_GD3_CALIBRATION_TIMER,
                    seedHelper.computeGd3ActivePrelimDates());
            reseedIfPresent(h);
        });
    }

    private void reseedIfPresent(Hackathon hackathon) {
        Round prelim = loadPrelim(hackathon.getId());
        List<Track> tracks = trackRepository.findByRoundIdOrderBySequenceOrderAsc(prelim.getId());
        if (tracks.size() < 2) {
            return;
        }
        Track track1 = tracks.get(0);
        Track track2 = tracks.get(1);
        User coordinator = seedHelper.requireCoordinator();

        List<Submission> track1Subs = new ArrayList<>();
        List<Submission> track2Subs = new ArrayList<>();
        for (int i = 0; i < Gd3CalibrationTimerSeedConstants.TEAM_NAMES.length; i++) {
            int idx = i + 1;
            var teamOpt = teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(
                    hackathon.getId(), Gd3CalibrationTimerSeedConstants.TEAM_NAMES[i]);
            if (teamOpt.isEmpty()) {
                return;
            }
            Submission sub = seedHelper.ensurePrelimSubmission(
                    hackathon,
                    prelim,
                    idx <= 3 ? track1 : track2,
                    teamOpt.get(),
                    SubmissionStatus.SUBMITTED,
                    false,
                    LocalDateTime.now().minusHours(3));
            seedHelper.markSubmissionSlideSeeded(sub);
            if (idx <= 3) {
                track1Subs.add(sub);
            } else {
                track2Subs.add(sub);
            }
        }
        if (track1Subs.size() == 3 && track2Subs.size() == 2) {
            seedCalibrationAndQueues(prelim, track1, track2, track1Subs, track2Subs, coordinator);
        }
    }

    private void seedCalibrationAndQueues(
            Round prelim,
            Track track1,
            Track track2,
            List<Submission> track1Subs,
            List<Submission> track2Subs,
            User coordinator) {
        CalibrationSession session = seedHelper.ensureOpenCalibrationSession(
                prelim,
                track1Subs.get(0),
                coordinator,
                8.0f,
                "Chấm thử bài mẫu CT01 — đồng bộ thang điểm trước khi chấm live.");

        // Track1: CT01+CT02 DONE, CT03 PRESENTING + PAUSED
        seedHelper.seedPresentationQueue(
                prelim,
                track1,
                track1Subs,
                2,
                HackathonDevSeedHelper.PresentationTimerSeed.pausedFromPresenting());
        // Track2: CT04 PRESENTING + QA, CT05 WAITING
        seedHelper.seedPresentationQueue(
                prelim,
                track2,
                track2Subs,
                0,
                HackathonDevSeedHelper.PresentationTimerSeed.qa());

        log.debug("[Gd3CalibrationTimerDataSeeder] calibrationSessionId={}", session.getId());
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd3CalibrationTimerSeedConstants.SLUG_GD3_CALIBRATION_TIMER);
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
