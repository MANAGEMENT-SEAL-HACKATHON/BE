package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.calibration_sessions.entity.CalibrationSession;
import com.sealhackathon.api.calibration_sessions.repository.CalibrationSessionRepository;
import com.sealhackathon.api.calibration_sessions.value_object.CalibrationStatus;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.criteria.value_object.CriteriaType;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.mentor_team_assignments.entity.MentorTeamAssignment;
import com.sealhackathon.api.mentor_team_assignments.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
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
 * Seed GĐ3 — {@link GdExtendedSeedConstants#SLUG_GD3_PRELIM_OPEN}.
 *
 * <p>Idempotent mỗi lần start dev: teams, submissions, scores (draft {@code isFinal=false}),
 * mentor assignments, presentation queue.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class Gd3DataSeeder {

    private final HackathonDevSeedHelper seedHelper;
    private final SubmissionRepository submissionRepository;
    private final ScoreRepository scoreRepository;
    private final CriteriaRepository criteriaRepository;
    private final CalibrationSessionRepository calibrationSessionRepository;
    private final MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    private final PresentationSlotRepository presentationSlotRepository;

    @Transactional
    public void ensureSeed() {
        var structure = seedHelper.ensureHackathonStructure(
                GdExtendedSeedConstants.SLUG_GD3_PRELIM_OPEN,
                "SEAL GĐ3 Prelim Open",
                HackathonStatus.ONGOING,
                "Seed GĐ3 — test nộp bài / chấm / late / calibration / ranking / presentation queue.",
                new HackathonDevSeedHelper.PrelimState(true, true, false, false, 2, 6),
                new HackathonDevSeedHelper.FinalState(false, false));

        LocalDateTime now = LocalDateTime.now();
        Chapter hcm = seedHelper.requireChapter(Gd1SeedConstants.CHAPTER_FPT_HCM);
        User coordinator = seedHelper.requireCoordinator();
        User judge1 = seedHelper.requireJudge1();
        User judge2 = seedHelper.requireJudge2();
        User mentor = seedHelper.requireMentor();

        User s1 = seedHelper.upsertStudent(GdExtendedSeedConstants.GD3_STU_LEADER_01, "GD3 Leader 01", hcm);
        User s2 = seedHelper.upsertStudent(GdExtendedSeedConstants.GD3_STU_LEADER_02, "GD3 Leader 02", hcm);
        User s3 = seedHelper.upsertStudent(GdExtendedSeedConstants.GD3_STU_LEADER_03, "GD3 Leader 03", hcm);
        User s4 = seedHelper.upsertStudent(GdExtendedSeedConstants.GD3_STU_LEADER_04, "GD3 Leader 04", hcm);
        User s5 = seedHelper.upsertStudent(GdExtendedSeedConstants.GD3_STU_LEADER_05, "GD3 Leader 05", hcm);
        User s6 = seedHelper.upsertStudent(GdExtendedSeedConstants.GD3_STU_LEADER_06, "GD3 Leader 06", hcm);

        Team t1 = seedHelper.ensureActiveTeam(structure.hackathon(), GdExtendedSeedConstants.GD3_TEAM_SUBMITTED, s1, hcm, now);
        Team t2 = seedHelper.ensureActiveTeam(structure.hackathon(), GdExtendedSeedConstants.GD3_TEAM_LATE_PENDING, s2, hcm, now);
        Team t3 = seedHelper.ensureActiveTeam(structure.hackathon(), GdExtendedSeedConstants.GD3_TEAM_LATE_APPROVED, s3, hcm, now);
        Team t4 = seedHelper.ensureActiveTeam(structure.hackathon(), GdExtendedSeedConstants.GD3_TEAM_NO_SUBMISSION, s4, hcm, now);
        Team t5 = seedHelper.ensureActiveTeam(structure.hackathon(), GdExtendedSeedConstants.GD3_TEAM_T2_SCORED, s5, hcm, now);
        Team t6 = seedHelper.ensureActiveTeam(structure.hackathon(), GdExtendedSeedConstants.GD3_TEAM_T2_PARTIAL, s6, hcm, now);

        Track track1 = structure.track1();
        Track track2 = structure.track2();

        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), track1, "Bảng A", t1, coordinator, now);
        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), track1, "Bảng A", t2, coordinator, now);
        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), track1, "Bảng B", t3, coordinator, now);
        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), track1, "Bảng B", t4, coordinator, now);
        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), track2, "Bảng C", t5, coordinator, now);
        seedHelper.ensureLottery(structure.hackathon(), structure.prelim(), track2, "Bảng C", t6, coordinator, now);

        boolean scoresFinal = Boolean.TRUE.equals(structure.prelim().getScoringLocked());

        Submission sub1 = ensureSubmission(t1, structure, track1, SubmissionStatus.SUBMITTED, false);
        Submission subLate = ensureSubmission(t2, structure, track1, SubmissionStatus.LATE_PENDING, true);
        Submission subLateOk = ensureSubmission(t3, structure, track1, SubmissionStatus.LATE_APPROVED, true);
        Submission sub5 = ensureSubmission(t5, structure, track2, SubmissionStatus.SUBMITTED, false);
        Submission sub6 = ensureSubmission(t6, structure, track2, SubmissionStatus.SUBMITTED, false);

        ensureFullTrackScores(sub1, track1, List.of(judge1, judge2), scoresFinal, 8.2f);
        ensureFullTrackScores(subLateOk, track1, List.of(judge1, judge2), scoresFinal, 7.6f);
        ensureFullTrackScores(sub5, track2, List.of(judge1, judge2), scoresFinal, 9.0f);
        ensurePartialTrackScores(sub6, track2, List.of(judge1), scoresFinal, 2, 6.5f);

        seedCalibrationIfAbsent(structure, sub1, coordinator, CalibrationStatus.OPEN);
        seedCalibrationIfAbsent(structure, sub1, coordinator, CalibrationStatus.CLOSED);

        ensureMentorAndPresentation(structure, List.of(t1, t2, t3, t4, t5, t6), mentor, coordinator, now);

        log.info("""
                [Gd3DataSeeder] GĐ3 seed slug={} hackathonId={} prelimRoundId={} track1Id={} track2Id={}
                  scoring-progress: gradable=4 (2 scored full, 1 partial, 1 LATE_PENDING excluded)
                  teams: t1={} t2={} t3={} t4={} t5={} t6={}
                  submissions: sub1={} latePending={} lateApproved={} sub5={} sub6={}
                  Postman vars:
                    gd3HackathonSlug={}
                    prelimRoundId={}
                    teamId={} (student {})
                    lateSubmissionId={} (student {})
                    studentToken login: {} / {}
                    mentorToken: {} / {}
                """,
                GdExtendedSeedConstants.SLUG_GD3_PRELIM_OPEN,
                structure.hackathon().getId(),
                structure.prelim().getId(),
                track1.getId(),
                track2.getId(),
                t1.getId(), t2.getId(), t3.getId(), t4.getId(), t5.getId(), t6.getId(),
                sub1.getId(), subLate.getId(), subLateOk.getId(), sub5.getId(), sub6.getId(),
                GdExtendedSeedConstants.SLUG_GD3_PRELIM_OPEN,
                structure.prelim().getId(),
                t1.getId(), GdExtendedSeedConstants.GD3_STU_LEADER_01,
                subLate.getId(), GdExtendedSeedConstants.GD3_STU_LEADER_02,
                GdExtendedSeedConstants.GD3_STU_LEADER_01, GdExtendedSeedConstants.DEV_STUDENT_PASSWORD,
                Gd1SeedConstants.EMAIL_MENTOR, Gd1SeedConstants.DEV_MENTOR_PASSWORD);
    }

    private Submission ensureSubmission(
            Team team,
            HackathonDevSeedHelper.HackathonStructure structure,
            Track track,
            SubmissionStatus status,
            boolean late) {
        return submissionRepository.findByTeam_IdAndRound_Id(team.getId(), structure.prelim().getId())
                .stream()
                .findFirst()
                .or(() -> submissionRepository.findByTeam_IdAndTrack_Round_Id(team.getId(), structure.prelim().getId())
                        .stream()
                        .findFirst())
                .map(existing -> {
                    existing.setTrack(track);
                    existing.setRound(structure.prelim());
                    existing.setHackathon(structure.hackathon());
                    existing.setStatus(status);
                    existing.setIsLate(late);
                    existing.setSlideUrl("https://docs.google.com/presentation/d/seed-gd3-" + team.getId());
                    existing.setRepoUrl("https://github.com/seed/gd3-" + team.getId());
                    existing.setDemoUrl("https://demo.example.com/gd3-" + team.getId());
                    if (existing.getSubmittedAt() == null) {
                        existing.setSubmittedAt(LocalDateTime.now());
                    }
                    return submissionRepository.save(existing);
                })
                .orElseGet(() -> submissionRepository.save(Submission.builder()
                        .team(team)
                        .round(structure.prelim())
                        .hackathon(structure.hackathon())
                        .track(track)
                        .status(status)
                        .isLate(late)
                        .submittedAt(LocalDateTime.now())
                        .repoUrl("https://github.com/seed/gd3-" + team.getId())
                        .demoUrl("https://demo.example.com/gd3-" + team.getId())
                        .slideUrl("https://docs.google.com/presentation/d/seed-gd3-" + team.getId())
                        .build()));
    }

    private List<Criteria> gradableCriteria(Track track) {
        return criteriaRepository.findByTrackIdOrderByDisplayOrderAsc(track.getId()).stream()
                .filter(c -> c.getType() != CriteriaType.PENALTY)
                .toList();
    }

    private void ensureFullTrackScores(
            Submission sub,
            Track track,
            List<User> judges,
            boolean isFinal,
            float baseScore) {
        List<Criteria> criteria = gradableCriteria(track);
        for (int i = 0; i < criteria.size(); i++) {
            Criteria criterion = criteria.get(i);
            float value = baseScore + (i * 0.2f);
            for (User judge : judges) {
                upsertNormalScore(sub, criterion, judge, value, isFinal);
            }
        }
    }

    private void ensurePartialTrackScores(
            Submission sub,
            Track track,
            List<User> judges,
            boolean isFinal,
            int criteriaCount,
            float baseScore) {
        List<Criteria> criteria = gradableCriteria(track);
        int limit = Math.min(criteriaCount, criteria.size());
        for (int i = 0; i < limit; i++) {
            Criteria criterion = criteria.get(i);
            float value = baseScore + (i * 0.3f);
            for (User judge : judges) {
                upsertNormalScore(sub, criterion, judge, value, isFinal);
            }
        }
    }

    private void upsertNormalScore(Submission sub, Criteria crit, User judge, float value, boolean isFinal) {
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
        score.setComment("Seed GĐ3");
        score.setIsFinal(isFinal);
        score.setScoredAt(LocalDateTime.now());
        score.setUpdatedAt(LocalDateTime.now());
        scoreRepository.save(score);
    }

    private void ensureMentorAndPresentation(
            HackathonDevSeedHelper.HackathonStructure structure,
            List<Team> teams,
            User mentor,
            User coordinator,
            LocalDateTime now) {
        LocalDateTime examAt = structure.prelim().getExamAt() != null
                ? structure.prelim().getExamAt()
                : now.withMinute(0).withSecond(0).withNano(0);

        int order = 1;
        for (Team team : teams) {
            if (mentorTeamAssignmentRepository.findByTeam_IdAndRound_Id(team.getId(), structure.prelim().getId()).isEmpty()) {
                mentorTeamAssignmentRepository.save(MentorTeamAssignment.builder()
                        .mentor(mentor)
                        .team(team)
                        .round(structure.prelim())
                        .hackathon(structure.hackathon())
                        .assignedAt(now)
                        .assignedBy(coordinator)
                        .build());
            }

            if (presentationSlotRepository.findByRound_IdAndTeam_Id(structure.prelim().getId(), team.getId()).isEmpty()) {
                LocalDateTime start = examAt.plusMinutes((long) (order - 1) * 15);
                PresentationQueueStatus status = order == 1
                        ? PresentationQueueStatus.PRESENTING
                        : PresentationQueueStatus.WAITING;
                presentationSlotRepository.save(PresentationSlot.builder()
                        .round(structure.prelim())
                        .team(team)
                        .startsAt(start)
                        .endsAt(start.plusMinutes(15))
                        .location("Online (Teams) - Phòng " + ((order % 3) + 1))
                        .sequenceOrder(order)
                        .queueStatus(status)
                        .build());
            } else {
                int slotOrder = order;
                presentationSlotRepository.findByRound_IdAndTeam_Id(structure.prelim().getId(), team.getId())
                        .ifPresent(slot -> {
                            if (slot.getSequenceOrder() == null || slot.getSequenceOrder() <= 0) {
                                slot.setSequenceOrder(slotOrder);
                                presentationSlotRepository.save(slot);
                            }
                        });
            }
            order++;
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
