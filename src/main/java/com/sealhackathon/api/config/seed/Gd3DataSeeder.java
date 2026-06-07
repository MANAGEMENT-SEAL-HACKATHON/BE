package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.calibration_sessions.entity.CalibrationSession;
import com.sealhackathon.api.calibration_sessions.repository.CalibrationSessionRepository;
import com.sealhackathon.api.calibration_sessions.value_object.CalibrationStatus;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Seed GĐ3 — {@link GdExtendedSeedConstants#SLUG_GD3_PRELIM_OPEN}.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3DataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final TeamRepository teamRepository;
    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final CalibrationSessionRepository calibrationSessionRepository;

    @Transactional
    public void ensureSeed() {
        var structure = seedHelper.ensureHackathonStructure(
                GdExtendedSeedConstants.SLUG_GD3_PRELIM_OPEN,
                "SEAL GĐ3 Prelim Open",
                HackathonStatus.ONGOING,
                "Seed GĐ3 — test nộp bài / chấm / late / calibration (chưa lock Sơ loại).",
                new HackathonDevSeedHelper.PrelimState(true, true, false, false, 2, 6),
                new HackathonDevSeedHelper.FinalState(false, false));

        if (teamRepository.existsByHackathon_IdAndTeamNameIgnoreCase(
                structure.hackathon().getId(), GdExtendedSeedConstants.GD3_TEAM_SUBMITTED)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();

        User s1 = seedHelper.upsertStudent(GdExtendedSeedConstants.GD3_STU_LEADER_01, "GD3 Leader 01", hcm);
        User s2 = seedHelper.upsertStudent(GdExtendedSeedConstants.GD3_STU_LEADER_02, "GD3 Leader 02", hcm);
        User s3 = seedHelper.upsertStudent(GdExtendedSeedConstants.GD3_STU_LEADER_03, "GD3 Leader 03", hcm);
        User s4 = seedHelper.upsertStudent(GdExtendedSeedConstants.GD3_STU_LEADER_04, "GD3 Leader 04", hcm);

        Team t1 = seedHelper.ensureActiveTeam(structure.hackathon(), GdExtendedSeedConstants.GD3_TEAM_SUBMITTED, s1, hcm, now);
        Team t2 = seedHelper.ensureActiveTeam(structure.hackathon(), GdExtendedSeedConstants.GD3_TEAM_LATE_PENDING, s2, hcm, now);
        Team t3 = seedHelper.ensureActiveTeam(structure.hackathon(), GdExtendedSeedConstants.GD3_TEAM_LATE_APPROVED, s3, hcm, now);
        Team t4 = seedHelper.ensureActiveTeam(structure.hackathon(), GdExtendedSeedConstants.GD3_TEAM_NO_SUBMISSION, s4, hcm, now);

        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), structure.track1(), "Bảng A", t1, coordinator, now);
        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), structure.track1(), "Bảng A", t2, coordinator, now);
        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), structure.track1(), "Bảng B", t3, coordinator, now);
        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), structure.track1(), "Bảng B", t4, coordinator, now);

        Criteria crit = seedHelper.firstCriterionForTrack(structure.track1());

        Submission sub1 = upsertSubmission(t1, structure, SubmissionStatus.SUBMITTED, false);
        upsertNormalScore(sub1, crit, judge1, 8.5f);
        upsertNormalScore(sub1, crit, judge2, 8.0f);

        upsertSubmission(t2, structure, SubmissionStatus.LATE_PENDING, true);
        upsertSubmission(t3, structure, SubmissionStatus.LATE_APPROVED, true);

        seedCalibrationIfAbsent(structure, sub1, coordinator, CalibrationStatus.OPEN);
        seedCalibrationIfAbsent(structure, sub1, coordinator, CalibrationStatus.CLOSED);

        log.info("""
                [Gd3DataSeeder] Seed GĐ3 slug={} hackathonId={} prelimRoundId={}
                  SUBMITTED+2 judges | LATE_PENDING | LATE_APPROVED | no submission
                  SV password: {}
                """,
                GdExtendedSeedConstants.SLUG_GD3_PRELIM_OPEN,
                structure.hackathon().getId(),
                structure.prelim().getId(),
                GdExtendedSeedConstants.DEV_STUDENT_PASSWORD);
    }

    private Submission upsertSubmission(
            Team team,
            HackathonDevSeedHelper.HackathonStructure structure,
            SubmissionStatus status,
            boolean late) {
        return submissionRepository.findByRound_Id(structure.prelim().getId()).stream()
                .filter(s -> s.getTeam().getId().equals(team.getId()))
                .findFirst()
                .orElseGet(() -> submissionRepository.save(Submission.builder()
                        .team(team)
                        .round(structure.prelim())
                        .hackathon(structure.hackathon())
                        .track(structure.track1())
                        .status(status)
                        .isLate(late)
                        .submittedAt(LocalDateTime.now())
                        .repoUrl("https://github.com/seed/gd3-" + team.getId())
                        .demoUrl("https://demo.example.com/gd3-" + team.getId())
                        .build()));
    }

    private void upsertNormalScore(Submission sub, Criteria crit, User judge, float value) {
        if (scoreRepository.findBySubmission_IdAndCriterion_IdAndScoreTypeAndIsFinal(
                sub.getId(), crit.getId(), ScoreType.NORMAL, true).isEmpty()) {
            scoreRepository.save(Score.builder()
                    .submission(sub)
                    .criterion(crit)
                    .judge(judge)
                    .scoreValue(value)
                    .scoreType(ScoreType.NORMAL)
                    .isFinal(true)
                    .scoredAt(LocalDateTime.now())
                    .build());
        }
    }

    private void seedCalibrationIfAbsent(
            HackathonDevSeedHelper.HackathonStructure structure,
            Submission sample,
            User coordinator,
            CalibrationStatus status) {
        boolean exists = calibrationSessionRepository.findByRound_IdOrderByStartedAtDesc(structure.prelim().getId())
                .stream()
                .anyMatch(c -> c.getStatus() == status);
        if (exists) {
            return;
        }
        calibrationSessionRepository.save(CalibrationSession.builder()
                .round(structure.prelim())
                .sampleSubmission(sample)
                .status(status)
                .targetScore(8.0f)
                .instructions("Seed calibration " + status)
                .createdBy(coordinator)
                .endedAt(status == CalibrationStatus.CLOSED ? LocalDateTime.now() : null)
                .build());
    }
}
