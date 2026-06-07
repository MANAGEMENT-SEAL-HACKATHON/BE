package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.calibration_sessions.entity.CalibrationSession;
import com.sealhackathon.api.calibration_sessions.repository.CalibrationSessionRepository;
import com.sealhackathon.api.calibration_sessions.value_object.CalibrationStatus;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seed GĐ5 — {@link GdExtendedSeedConstants#SLUG_GD5_FINAL_ACTIVE}:
 * Sơ loại publish+lock, 4 đội ADVANCED, CK active chưa lock — test nộp/chấm/lock → PENDING_CONFIRM.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd5FinalRoundDataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final RoundRepository roundRepository;
    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final CriteriaRepository criteriaRepository;
    private final CalibrationSessionRepository calibrationSessionRepository;
    private final JudgeAssignmentRepository judgeAssignmentRepository;

    @Transactional
    public void ensureSeed() {
        var structure = seedHelper.ensureHackathonStructure(
                GdExtendedSeedConstants.SLUG_GD5_FINAL_ACTIVE,
                "SEAL GĐ5 Final Active",
                HackathonStatus.ONGOING,
                "Seed GĐ5 — Chung kết đang thi: nộp bài CK, chấm, lock CK → PATCH PENDING_CONFIRM.",
                new HackathonDevSeedHelper.PrelimState(false, true, true, true, 2, 6),
                new HackathonDevSeedHelper.FinalState(true, false));

        applyFinalRoundSetup(structure);
        ensurePrelimLockedPublished(structure.prelim());

        LocalDateTime now = LocalDateTime.now();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User guestJudge = seedHelper.requireGuestJudge();

        Team t1 = seedTeam(structure, GdExtendedSeedConstants.GD5_TEAM_FINAL_SCORED,
                GdExtendedSeedConstants.GD5_STU_01, "GD5 Leader 01", hcm, now);
        Team t2 = seedTeam(structure, GdExtendedSeedConstants.GD5_TEAM_FINAL_SUBMITTED,
                GdExtendedSeedConstants.GD5_STU_02, "GD5 Leader 02", hcm, now);
        Team t3 = seedTeam(structure, GdExtendedSeedConstants.GD5_TEAM_NO_FINAL_SUB,
                GdExtendedSeedConstants.GD5_STU_03, "GD5 Leader 03", hcm, now);
        Team t4 = seedTeam(structure, GdExtendedSeedConstants.GD5_TEAM_ADV_ONLY,
                GdExtendedSeedConstants.GD5_STU_04, "GD5 Leader 04", hcm, now);

        Track track1 = structure.track1();
        Criteria prelimCrit = gradableCriteria(track1).get(0);
        List<Criteria> finalCriteria = gradableFinalCriteria(structure.finalRound());

        for (Team team : List.of(t1, t2, t3, t4)) {
            seedHelper.markAdvanced(team, structure.prelim(), structure.finalRound(), structure.hackathon());
            Submission prelimSub = ensurePrelimSubmission(team, structure, track1);
            ensureSingleCriterionScore(prelimSub, prelimCrit, judge1, 8.5f, true);
        }

        Submission final1 = ensureFinalSubmission(t1, structure);
        ensureFullFinalScores(final1, finalCriteria, guestJudge, false, 9.0f);

        Submission final2 = ensureFinalSubmission(t2, structure);
        // t2: submitted CK, chưa chấm — test POST /scores

        seedCalibrationIfAbsent(structure, final1, coordinator, CalibrationStatus.OPEN);

        boolean guestAssignedCk = judgeAssignmentRepository.existsByJudgeIdAndRoundId(
                guestJudge.getId(), structure.finalRound().getId());

        log.info("""
                [Gd5FinalRoundDataSeeder] GĐ5 E2E slug={}
                  hackathonId={} status=ONGOING
                  prelimRoundId={} (published+locked) | finalRoundId={} (active, NOT locked)
                  track1Id={} finalCriterionId={} (first of CK)
                  teams: t1={} CK scored | t2={} CK submitted chưa chấm | t3={} t4={} chưa nộp CK
                  finalSubmissionId(t1)={} finalSubmissionId(t2)={}
                  guestJudgeId={} guestJudgeAssignedCK={} guestEmail={}
                  student01={} (CK scored) student02={} (test POST /scores) student03={} (test POST /submissions CK)
                  password={}
                  Flow 5.1–5.4: POST submissions → POST scores → lock CK → GET hackathon PENDING_CONFIRM
                  Doc: docs/testing/gd4-gd5-e2e-seed-data.md
                """,
                GdExtendedSeedConstants.SLUG_GD5_FINAL_ACTIVE,
                structure.hackathon().getId(),
                structure.prelim().getId(),
                structure.finalRound().getId(),
                track1.getId(),
                finalCriteria.isEmpty() ? "n/a" : finalCriteria.get(0).getId(),
                t1.getId(), t2.getId(), t3.getId(), t4.getId(),
                final1.getId(), final2.getId(),
                guestJudge.getId(), guestAssignedCk,
                Gd1SeedConstants.EMAIL_GUEST_JUDGE,
                GdExtendedSeedConstants.GD5_STU_01,
                GdExtendedSeedConstants.GD5_STU_02,
                GdExtendedSeedConstants.GD5_STU_03,
                GdExtendedSeedConstants.DEV_STUDENT_PASSWORD);
    }

    private void applyFinalRoundSetup(HackathonDevSeedHelper.HackathonStructure structure) {
        var finalRound = structure.finalRound();
        if (finalRound.getProblemReleasedAt() == null) {
            finalRound.setProblemStatementUrl("https://example.com/seed/gd5-debai-chung-ket.pdf");
            finalRound.setProblemReleasedAt(LocalDateTime.now());
        }
        finalRound.setIsActive(true);
        if (finalRound.getActivatedAt() == null) {
            finalRound.setActivatedAt(LocalDateTime.now());
        }
        finalRound.setScoringLocked(false);
        roundRepository.save(finalRound);
    }

    private void ensurePrelimLockedPublished(com.sealhackathon.api.rounds.entity.Round prelim) {
        prelim.setScoringLocked(true);
        prelim.setIsPublished(true);
        prelim.setIsActive(false);
        if (prelim.getScoringLockedAt() == null) {
            prelim.setScoringLockedAt(LocalDateTime.now());
        }
        if (prelim.getPublishedAt() == null) {
            prelim.setPublishedAt(LocalDateTime.now());
        }
        roundRepository.save(prelim);
    }

    private Team seedTeam(
            HackathonDevSeedHelper.HackathonStructure structure,
            String teamName,
            String studentEmail,
            String fullName,
            Chapter hcm,
            LocalDateTime now) {
        User leader = seedHelper.upsertStudent(studentEmail, fullName, hcm);
        Team team = seedHelper.ensureActiveTeam(structure.hackathon(), teamName, leader, hcm, now);
        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), structure.track1(),
                "Bảng A", team, seedHelper.requireCoordinator(), now);
        return team;
    }

    private Submission ensurePrelimSubmission(
            Team team,
            HackathonDevSeedHelper.HackathonStructure structure,
            Track track) {
        return submissionRepository.findByTeam_IdAndRound_Id(team.getId(), structure.prelim().getId())
                .stream()
                .findFirst()
                .orElseGet(() -> submissionRepository.save(Submission.builder()
                        .team(team)
                        .round(structure.prelim())
                        .hackathon(structure.hackathon())
                        .track(track)
                        .status(SubmissionStatus.SUBMITTED)
                        .submittedAt(LocalDateTime.now())
                        .repoUrl("https://github.com/seed/gd5-prelim-" + team.getId())
                        .build()));
    }

    private Submission ensureFinalSubmission(Team team, HackathonDevSeedHelper.HackathonStructure structure) {
        return submissionRepository.findByTeam_IdAndRound_Id(team.getId(), structure.finalRound().getId())
                .stream()
                .findFirst()
                .orElseGet(() -> submissionRepository.save(Submission.builder()
                        .team(team)
                        .round(structure.finalRound())
                        .hackathon(structure.hackathon())
                        .track(null)
                        .status(SubmissionStatus.SUBMITTED)
                        .submittedAt(LocalDateTime.now())
                        .repoUrl("https://github.com/seed/gd5-final-" + team.getId())
                        .demoUrl("https://demo.example.com/gd5-final-" + team.getId())
                        .slideUrl("https://slides.example.com/gd5-final-" + team.getId())
                        .build()));
    }

    private List<Criteria> gradableCriteria(Track track) {
        return criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId()).stream()
                .filter(c -> c.getType() != CriteriaType.PENALTY)
                .toList();
    }

    private List<Criteria> gradableFinalCriteria(com.sealhackathon.api.rounds.entity.Round finalRound) {
        return criteriaRepository.findByFinalRoundIdOrderByDisplayOrderAsc(finalRound.getId()).stream()
                .filter(c -> c.getType() != CriteriaType.PENALTY)
                .toList();
    }

    private void ensureSingleCriterionScore(
            Submission sub, Criteria crit, User judge, float value, boolean isFinal) {
        upsertScore(sub, crit, judge, value, isFinal);
    }

    private void ensureFullFinalScores(
            Submission sub,
            List<Criteria> criteria,
            User judge,
            boolean isFinal,
            float baseScore) {
        for (int i = 0; i < criteria.size(); i++) {
            upsertScore(sub, criteria.get(i), judge, baseScore + (i * 0.15f), isFinal);
        }
    }

    private void upsertScore(Submission sub, Criteria crit, User judge, float value, boolean isFinal) {
        Score score = scoreRepository
                .findBySubmission_IdAndJudge_IdAndCriterion_IdAndScoreType(
                        sub.getId(), judge.getId(), crit.getId(), ScoreType.NORMAL)
                .orElseGet(() -> Score.builder()
                        .submission(sub)
                        .criterion(crit)
                        .judge(judge)
                        .scoreType(ScoreType.NORMAL)
                        .build());
        score.setScoreValue(value);
        score.setComment("Seed GĐ5");
        score.setIsFinal(isFinal);
        score.setScoredAt(LocalDateTime.now());
        score.setUpdatedAt(LocalDateTime.now());
        scoreRepository.save(score);
    }

    private void seedCalibrationIfAbsent(
            HackathonDevSeedHelper.HackathonStructure structure,
            Submission sample,
            User coordinator,
            CalibrationStatus status) {
        boolean exists = calibrationSessionRepository.findByRound_IdOrderByStartedAtDesc(
                        structure.finalRound().getId()).stream()
                .anyMatch(c -> c.getStatus() == status);
        if (exists) {
            return;
        }
        calibrationSessionRepository.save(CalibrationSession.builder()
                .round(structure.finalRound())
                .sampleSubmission(sample)
                .status(status)
                .targetScore(8.8f)
                .instructions("Seed GĐ5 calibration " + status)
                .createdBy(coordinator)
                .endedAt(status == CalibrationStatus.CLOSED ? LocalDateTime.now() : null)
                .build());
    }
}
