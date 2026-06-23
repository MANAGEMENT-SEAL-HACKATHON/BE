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
 * Seed GĐ3 bad/hybrid — nộp trễ, duyệt muộn, từ chối muộn, chưa nộp.
 *
 * <p>Doc: {@code docs/testing/gd3-full-test-matrix-and-seeds.md} § Profile A
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3LateReviewDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final TeamRepository teamRepository;
    private final TrackRepository trackRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd3.late-review.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd3LateReviewDataSeeder] Tắt (app.seed.gd3.late-review.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.PrelimState prelimState =
                new HackathonDevSeedHelper.PrelimState(true, true, false, false, 2, 4);
        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd3LateReviewSeedConstants.SLUG_GD3_LATE_REVIEW,
                "SEAL GĐ3 — Late review",
                HackathonStatus.ONGOING,
                "Seed GĐ3 bad path — LATE_PENDING / LATE_APPROVED / REJECTED / chưa nộp",
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
        }

        seedHelper.syncHackathonCalendarFromDates(
                Gd3LateReviewSeedConstants.SLUG_GD3_LATE_REVIEW,
                seedHelper.computeGd3ActivePrelimDates());
        prelim = loadPrelim(hackathon.getId());

        User coordinator = seedHelper.requireCoordinator();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime onTimeAt = prelim.getSubmissionDeadline().minusHours(2);
        LocalDateTime lateAt = prelim.getSubmissionDeadline().plusHours(1);

        String[] teamNames = {
                Gd3LateReviewSeedConstants.TEAM_ON_TIME,
                Gd3LateReviewSeedConstants.TEAM_LATE_PENDING,
                Gd3LateReviewSeedConstants.TEAM_LATE_APPROVED,
                Gd3LateReviewSeedConstants.TEAM_NO_SUBMIT,
                Gd3LateReviewSeedConstants.TEAM_LATE_REJECTED
        };
        SubmissionStatus[] statuses = {
                SubmissionStatus.SUBMITTED,
                SubmissionStatus.LATE_PENDING,
                SubmissionStatus.LATE_APPROVED,
                null,
                SubmissionStatus.REJECTED
        };
        boolean[] isLate = {false, true, true, false, true};
        LocalDateTime[] submittedAt = {onTimeAt, lateAt, lateAt, null, lateAt};

        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < teamNames.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd3LateReviewSeedConstants.studentEmail(idx),
                    Gd3LateReviewSeedConstants.studentDisplayName(idx),
                    hcm);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeamForLeader(hackathon, teamNames[i], leader, hcm, now);
            seedHelper.ensureTeamLocked(team, now);
            Track track = idx <= 3 ? track1 : track2;
            String group = idx <= 3 ? "BANG-A" : "BANG-B";
            seedHelper.ensureLottery(hackathon, prelim, track, group, team, coordinator, now);
            teams.add(team);
        }

        seedLateReviewSubmissions(hackathon, prelim, track1, track2, teams, statuses, isLate, submittedAt);
        seedHelper.clearPresentationQueues(prelim, track1, track2);

        log.info("""
                [Gd3LateReviewDataSeeder] slug={} prelimRoundId={}
                  L01 SUBMITTED | L02 LATE_PENDING | L03 LATE_APPROVED | L04 none | L05 REJECTED
                  students: {} … {} password={}
                """,
                Gd3LateReviewSeedConstants.SLUG_GD3_LATE_REVIEW,
                prelim.getId(),
                Gd3LateReviewSeedConstants.studentEmail(1),
                Gd3LateReviewSeedConstants.studentEmail(5),
                DevSeedCatalog.DEV_STUDENT_PASSWORD);
    }

    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        hackathonRepository.findBySlug(Gd3LateReviewSeedConstants.SLUG_GD3_LATE_REVIEW).ifPresent(h -> {
            seedHelper.syncHackathonCalendarFromDates(
                    Gd3LateReviewSeedConstants.SLUG_GD3_LATE_REVIEW,
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
        String[] teamNames = {
                Gd3LateReviewSeedConstants.TEAM_ON_TIME,
                Gd3LateReviewSeedConstants.TEAM_LATE_PENDING,
                Gd3LateReviewSeedConstants.TEAM_LATE_APPROVED,
                Gd3LateReviewSeedConstants.TEAM_NO_SUBMIT,
                Gd3LateReviewSeedConstants.TEAM_LATE_REJECTED
        };
        List<Team> teams = new ArrayList<>();
        for (String name : teamNames) {
            teamRepository.findByHackathon_IdAndTeamNameIgnoreCase(hackathon.getId(), name)
                    .ifPresent(teams::add);
        }
        if (teams.size() != 5) {
            return;
        }
        LocalDateTime onTimeAt = prelim.getSubmissionDeadline().minusHours(2);
        LocalDateTime lateAt = prelim.getSubmissionDeadline().plusHours(1);
        SubmissionStatus[] statuses = {
                SubmissionStatus.SUBMITTED,
                SubmissionStatus.LATE_PENDING,
                SubmissionStatus.LATE_APPROVED,
                null,
                SubmissionStatus.REJECTED
        };
        boolean[] isLate = {false, true, true, false, true};
        LocalDateTime[] submittedAt = {onTimeAt, lateAt, lateAt, null, lateAt};
        seedLateReviewSubmissions(hackathon, prelim, track1, track2, teams, statuses, isLate, submittedAt);
        seedHelper.clearPresentationQueues(prelim, track1, track2);
    }

    private void seedLateReviewSubmissions(
            Hackathon hackathon,
            Round prelim,
            Track track1,
            Track track2,
            List<Team> teams,
            SubmissionStatus[] statuses,
            boolean[] isLate,
            LocalDateTime[] submittedAt) {
        for (int i = 0; i < teams.size(); i++) {
            Team team = teams.get(i);
            if (statuses[i] == null) {
                seedHelper.clearPrelimSubmission(team, prelim);
                continue;
            }
            Track track = i < 3 ? track1 : track2;
            Submission sub = seedHelper.ensurePrelimSubmission(
                    hackathon, prelim, track, team, statuses[i], isLate[i], submittedAt[i]);
            seedHelper.clearSubmissionScores(sub.getId());
        }
    }

    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd3LateReviewSeedConstants.SLUG_GD3_LATE_REVIEW);
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
