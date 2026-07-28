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
 * Seed GĐ5 — hackathon {@link Gd5SeedConstants#SLUG_GD5_FINAL_ACTIVE}.
 *
 * <p>CK active, <b>3 đội ADVANCED</b> (+ 1 đội bị loại sơ loại còn ACTIVE), submit window mở.
 * <ul>
 *   <li>GD5-01, GD5-02: đã nộp CK (SUBMITTED)</li>
 *   <li>GD5-03: chưa nộp — login {@code student.gd5.leader03@fpt.edu.vn} để test nộp</li>
 * </ul>
 * Prelim: published scores + STT queues.
 *
 * <p>Doc: {@code docs/testing/gd5-full-test-matrix-and-seeds.md} § Profile 0
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd5FinalRoundDataSeeder {

    private static final float[] PRELIM_SCORES = {9.0f, 8.5f, 8.8f, 8.2f};

    private final HackathonDevSeedHelper seedHelper;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final DevSeedCleanup devSeedCleanup;

    @Value("${app.seed.gd5.enabled:true}")
    private boolean enabled;

    @Transactional
    public void ensureSeed() {
        if (!enabled) {
            log.info("[Gd5FinalRoundDataSeeder] Tắt (app.seed.gd5.enabled=false)");
            return;
        }

        HackathonDevSeedHelper.HackathonStructure structure = seedHelper.ensureHackathonStructure(
                Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE,
                "SEAL GĐ5 — Chung kết active",
                HackathonStatus.ONGOING,
                "Seed E2E GĐ5 — CK active, 3 ADVANCED (2 đã nộp + 1 trống test nộp)",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 1, 2),
                new HackathonDevSeedHelper.FinalState(true, false),
                seedHelper.computeGd5FinalActiveDates());

        Hackathon hackathon = structure.hackathon();
        Round prelim = structure.prelim();
        Round finalRound = structure.finalRound();
        Track track1 = structure.track1();
        Track track2 = structure.track2();

        seedHelper.syncHackathonCalendarFromDates(
                Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE, seedHelper.computeGd5FinalActiveDates());
        // Clear CK only — giữ prelim scores/queue
        seedHelper.clearFinalRoundArtifacts(hackathon.getId());
        seedHelper.repairGd5FeTestingScheduleAndState(hackathon, prelim, finalRound);
        prelim = roundRepository.findById(prelim.getId()).orElse(prelim);
        finalRound = roundRepository.findById(finalRound.getId()).orElse(finalRound);

        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();
        User guestJudge = seedHelper.requireGuestJudge();
        Chapter chapter = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime submittedAt = now.minusHours(96);

        seedHelper.ensureGuestJudgeInvitation(hackathon, guestJudge, coordinator);
        seedHelper.ensureFinalGuestJudgeAssignment(hackathon, finalRound);
        seedHelper.seedFinalRoundProblem(finalRound);
        seedHelper.seedPrelimTrackProblems(prelim);

        String[] teamNames = {
                Gd5SeedConstants.TEAM_01,
                Gd5SeedConstants.TEAM_02,
                Gd5SeedConstants.TEAM_03,
                Gd5SeedConstants.TEAM_04
        };
        List<Team> teams = new ArrayList<>();
        List<Submission> track1Subs = new ArrayList<>();
        List<Submission> track2Subs = new ArrayList<>();
        for (int i = 0; i < teamNames.length; i++) {
            int idx = i + 1;
            User leader = seedHelper.upsertStudent(
                    Gd5SeedConstants.studentEmail(idx),
                    Gd5SeedConstants.studentDisplayName(idx),
                    chapter);
            seedHelper.registerStudent(hackathon, leader);
            Team team = seedHelper.ensureActiveTeamForLeader(hackathon, teamNames[i], leader, chapter, now);
            seedHelper.ensureTeamLocked(team, now);
            Track track = (idx % 2 == 1) ? track1 : track2;
            User judge = (idx % 2 == 1) ? judge1 : judge2;
            String group = "BANG-" + ((idx % 2) + 1);
            seedHelper.ensureLottery(hackathon, prelim, track, group, team, coordinator, now);
            Submission prelimSub = seedHelper.ensurePrelimSubmission(
                    hackathon, prelim, track, team,
                    SubmissionStatus.SUBMITTED,
                    false,
                    submittedAt.minusMinutes(i));
            seedHelper.scoreAllTrackCriteria(prelimSub, track, judge, PRELIM_SCORES[i], true);
            // Top-N thực tế: 3 đội vào CK; đội 4 bị loại (vẫn ACTIVE) — test roster CK không lẫn đội loại
            if (idx <= 3) {
                seedHelper.markAdvanced(team, prelim, finalRound, hackathon);
            } else {
                seedHelper.markEliminatedFromPrelim(team, prelim, finalRound);
            }
            teams.add(team);
            if (idx % 2 == 1) {
                track1Subs.add(prelimSub);
            } else {
                track2Subs.add(prelimSub);
            }
        }

        seedHelper.seedPresentationQueue(prelim, track1, track1Subs, -1);
        seedHelper.seedPresentationQueue(prelim, track2, track2Subs, -1);

        // 2 đội đã nộp CK; đội 03 để trống — test nộp tay
        seedCkPartialSubmissions(hackathon, finalRound, teams);

        log.info("""
                [Gd5FinalRoundDataSeeder] slug={} hackathonId={} prelimRoundId={} finalRoundId={}
                  ADVANCED: {} (đã nộp) | {} (đã nộp) | {} (CHƯA nộp — test) | loại: {}
                  Test nộp CK: {} / {}
                  guestJudge={} (FINAL_EXTERNAL on CK)
                """,
                Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE,
                hackathon.getId(),
                prelim.getId(),
                finalRound.getId(),
                teams.get(0).getId(),
                teams.get(1).getId(),
                teams.get(2).getId(),
                teams.get(3).getId(),
                Gd5SeedConstants.studentEmail(Gd5SeedConstants.TEAM_INDEX_PENDING_SUBMIT),
                DevSeedCatalog.DEV_STUDENT_PASSWORD,
                Gd1SeedConstants.EMAIL_GUEST_JUDGE);
    }

    /**
     * GD5-01 + GD5-02 → SUBMITTED CK; GD5-03 không tạo submission.
     * Gọi sau {@code clearFinalRoundArtifacts} để không bị xóa.
     */
    private void seedCkPartialSubmissions(Hackathon hackathon, Round finalRound, List<Team> teams) {
        if (teams.size() < 3) {
            return;
        }
        seedHelper.ensureFinalSubmission(
                hackathon,
                finalRound,
                teams.get(0),
                "https://github.com/seal-warriors/gd5-team01");
        seedHelper.ensureFinalSubmission(
                hackathon,
                finalRound,
                teams.get(1),
                "https://github.com/seal-warriors/gd5-team02");
    }

    /** Đồng bộ lịch CK đang mở theo giờ máy — giữ 2 bài nộp CK mẫu + 1 slot trống. */
    @Transactional
    public void repairForFeTesting() {
        if (!enabled) {
            return;
        }
        var maybeHackathon = hackathonRepository.findBySlug(Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE);
        if (maybeHackathon.isEmpty()) {
            return;
        }
        Hackathon hackathon = maybeHackathon.get();
        Round prelim = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
        Round finalRound = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathon.getId()).stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsFinal()))
                .findFirst()
                .orElse(null);
        if (prelim == null || finalRound == null) {
            return;
        }
        seedHelper.clearFinalRoundArtifacts(hackathon.getId());
        seedHelper.repairGd5FeTestingScheduleAndState(hackathon, prelim, finalRound);
        int locked = seedHelper.ensureAllActiveTeamsLocked(hackathon.getId(), LocalDateTime.now());
        finalRound = roundRepository.findById(finalRound.getId()).orElse(finalRound);

        Chapter chapter = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        LocalDateTime now = LocalDateTime.now();
        List<Team> advanced = new ArrayList<>();
        for (int idx = 1; idx <= 3; idx++) {
            User leader = seedHelper.upsertStudent(
                    Gd5SeedConstants.studentEmail(idx),
                    Gd5SeedConstants.studentDisplayName(idx),
                    chapter);
            String name = idx == 1 ? Gd5SeedConstants.TEAM_01
                    : idx == 2 ? Gd5SeedConstants.TEAM_02
                    : Gd5SeedConstants.TEAM_03;
            advanced.add(seedHelper.ensureActiveTeamForLeader(hackathon, name, leader, chapter, now));
        }
        seedCkPartialSubmissions(hackathon, finalRound, advanced);

        log.info(
                "[Gd5FinalRoundDataSeeder] FE repair — 2/3 CK submitted, pending={} deadline={} lockedTeams={}",
                Gd5SeedConstants.studentEmail(Gd5SeedConstants.TEAM_INDEX_PENDING_SUBMIT),
                finalRound.getSubmissionDeadline(),
                locked);
    }

    /** Idempotent recreate — purge rồi seed lại khi cần DB sạch. */
    @Transactional
    public void resetAndSeed() {
        devSeedCleanup.purgeIfPresent(Gd5SeedConstants.SLUG_GD5_FINAL_ACTIVE);
        ensureSeed();
    }
}
