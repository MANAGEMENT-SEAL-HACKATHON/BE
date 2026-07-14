package com.sealhackathon.api.scores.service.impl;

import com.sealhackathon.api.calibration_sessions.entity.CalibrationSession;
import com.sealhackathon.api.calibration_sessions.repository.CalibrationSessionRepository;
import com.sealhackathon.api.calibration_sessions.value_object.CalibrationStatus;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.events.repository.JudgeSubmissionScoringConfirmationRepository;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.presentation.support.RoundPhaseResolver;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.dto.request.SubmitCalibrationScoreRequest;
import com.sealhackathon.api.scores.dto.response.ScoreResponse;
import com.sealhackathon.api.scores.entity.Score;
import com.sealhackathon.api.scores.guard.JudgeAssignmentGuard;
import com.sealhackathon.api.scores.guard.MentorJudgeConflictGuard;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.value_object.ScoreType;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScoreServiceImplCalibrationTest {

    @Mock private ScoreRepository scoreRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private CriteriaRepository criteriaRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private JudgeAssignmentGuard judgeAssignmentGuard;
    @Mock private MentorJudgeConflictGuard mentorJudgeConflictGuard;
    @Mock private TeamRoundTrackRepository teamRoundTrackRepository;
    @Mock private PresentationSlotRepository presentationSlotRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CalibrationSessionRepository calibrationSessionRepository;
    @Mock private RoundPhaseResolver roundPhaseResolver;
    @Mock private JudgeSubmissionScoringConfirmationRepository scoringConfirmationRepository;

    @InjectMocks
    private ScoreServiceImpl scoreService;

    @Test
    void happyOpenAssigned() {
        Fixture f = mockOpenFixture();
        when(scoreRepository.findBySubmission_IdAndJudge_IdAndCriterion_IdAndScoreType(
                42, 99, 1, ScoreType.CALIBRATION)).thenReturn(Optional.empty());
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> {
            Score s = inv.getArgument(0);
            s.setId(500);
            return s;
        });

        ScoreResponse res = scoreService.submitCalibrationScore(SubmitCalibrationScoreRequest.builder()
                .submissionId(42)
                .criterionId(1)
                .scoreValue(8f)
                .calibrationSessionId(7)
                .build());

        assertThat(res.getId()).isEqualTo(500);
        assertThat(res.getScoreType()).isEqualTo(ScoreType.CALIBRATION);
        verify(judgeAssignmentGuard).requireJudgeForSubmission(99, f.submission);
        verify(mentorJudgeConflictGuard).requireNoConflict(99, f.submission);
    }

    @Test
    void closedSession_throwsCalibrationSessionClosed() {
        mockOpenFixture();
        when(calibrationSessionRepository.findById(7)).thenReturn(Optional.of(CalibrationSession.builder()
                .id(7)
                .round(Round.builder().id(5).build())
                .status(CalibrationStatus.CLOSED)
                .build()));

        assertThatThrownBy(() -> scoreService.submitCalibrationScore(SubmitCalibrationScoreRequest.builder()
                        .submissionId(42)
                        .criterionId(1)
                        .scoreValue(8f)
                        .calibrationSessionId(7)
                        .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.CALIBRATION_SESSION_CLOSED);
    }

    @Test
    void missingSessionId_throwsRequired() {
        when(currentUserAccessor.currentUserId()).thenReturn(99);

        assertThatThrownBy(() -> scoreService.submitCalibrationScore(SubmitCalibrationScoreRequest.builder()
                        .submissionId(42)
                        .criterionId(1)
                        .scoreValue(8f)
                        .calibrationSessionId(null)
                        .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.CALIBRATION_SESSION_ID_REQUIRED);
    }

    @Test
    void scoreExceedsMax_throws() {
        mockOpenFixture();

        assertThatThrownBy(() -> scoreService.submitCalibrationScore(SubmitCalibrationScoreRequest.builder()
                        .submissionId(42)
                        .criterionId(1)
                        .scoreValue(99f)
                        .calibrationSessionId(7)
                        .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.SCORE_EXCEEDS_MAX);
    }

    @Test
    void wrongSampleSubmission_throwsInvalidState() {
        Fixture f = mockOpenFixture();
        when(calibrationSessionRepository.findById(7)).thenReturn(Optional.of(CalibrationSession.builder()
                .id(7)
                .round(f.round)
                .status(CalibrationStatus.OPEN)
                .sampleSubmission(Submission.builder().id(999).build())
                .build()));

        assertThatThrownBy(() -> scoreService.submitCalibrationScore(SubmitCalibrationScoreRequest.builder()
                        .submissionId(42)
                        .criterionId(1)
                        .scoreValue(8f)
                        .calibrationSessionId(7)
                        .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void judgeNotAssigned_throwsForbidden() {
        Fixture f = mockOpenFixture();
        doThrow(new AuthException(ErrorCode.JUDGE_NOT_ASSIGNED,
                "Judge chưa được phân công", HttpStatus.FORBIDDEN))
                .when(judgeAssignmentGuard).requireJudgeForSubmission(99, f.submission);

        assertThatThrownBy(() -> scoreService.submitCalibrationScore(SubmitCalibrationScoreRequest.builder()
                        .submissionId(42)
                        .criterionId(1)
                        .scoreValue(8f)
                        .calibrationSessionId(7)
                        .build()))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getCode())
                .isEqualTo(ErrorCode.JUDGE_NOT_ASSIGNED);
    }

    @Test
    void mentorJudgeConflict_onCalibrationPath_throws() {
        Fixture f = mockOpenFixture();
        doThrow(new BusinessRuleException(ErrorCode.CONFLICT_MENTOR_JUDGE_SAME_TRACK,
                "conflict"))
                .when(mentorJudgeConflictGuard).requireNoConflict(99, f.submission);

        assertThatThrownBy(() -> scoreService.submitCalibrationScore(SubmitCalibrationScoreRequest.builder()
                        .submissionId(42)
                        .criterionId(1)
                        .scoreValue(8f)
                        .calibrationSessionId(7)
                        .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.CONFLICT_MENTOR_JUDGE_SAME_TRACK);
    }

    @Test
    void wrongRoundBinding_throwsInvalidState() {
        mockOpenFixture();
        when(calibrationSessionRepository.findById(7)).thenReturn(Optional.of(CalibrationSession.builder()
                .id(7)
                .round(Round.builder().id(99).build())
                .status(CalibrationStatus.OPEN)
                .build()));

        assertThatThrownBy(() -> scoreService.submitCalibrationScore(SubmitCalibrationScoreRequest.builder()
                        .submissionId(42)
                        .criterionId(1)
                        .scoreValue(8f)
                        .calibrationSessionId(7)
                        .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void concurrentUpsert_lastWriteWins_no5xx() throws Exception {
        mockOpenFixture();
        when(scoreRepository.findBySubmission_IdAndJudge_IdAndCriterion_IdAndScoreType(
                eq(42), eq(99), eq(1), eq(ScoreType.CALIBRATION))).thenReturn(Optional.empty());
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> {
            Score s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(500);
            }
            return s;
        });

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ScoreResponse> a = pool.submit(() -> {
                start.await(2, TimeUnit.SECONDS);
                return scoreService.submitCalibrationScore(SubmitCalibrationScoreRequest.builder()
                        .submissionId(42).criterionId(1).scoreValue(7f).calibrationSessionId(7).build());
            });
            Future<ScoreResponse> b = pool.submit(() -> {
                start.await(2, TimeUnit.SECONDS);
                return scoreService.submitCalibrationScore(SubmitCalibrationScoreRequest.builder()
                        .submissionId(42).criterionId(1).scoreValue(9f).calibrationSessionId(7).build());
            });
            start.countDown();
            assertThat(a.get(5, TimeUnit.SECONDS).getId()).isNotNull();
            assertThat(b.get(5, TimeUnit.SECONDS).getId()).isNotNull();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentCloseRace_closedOrSuccess_no5xx() throws Exception {
        Fixture f = mockOpenFixture();
        AtomicReference<CalibrationStatus> status = new AtomicReference<>(CalibrationStatus.OPEN);
        when(calibrationSessionRepository.findById(7)).thenAnswer(inv -> Optional.of(CalibrationSession.builder()
                .id(7)
                .round(f.round)
                .status(status.get())
                .sampleSubmission(f.submission)
                .build()));
        when(scoreRepository.findBySubmission_IdAndJudge_IdAndCriterion_IdAndScoreType(
                eq(42), eq(99), eq(1), eq(ScoreType.CALIBRATION))).thenReturn(Optional.empty());
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> {
            Score s = inv.getArgument(0);
            s.setId(501);
            return s;
        });

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> scoreFuture = pool.submit(() -> {
                start.await(2, TimeUnit.SECONDS);
                try {
                    scoreService.submitCalibrationScore(SubmitCalibrationScoreRequest.builder()
                            .submissionId(42).criterionId(1).scoreValue(8f).calibrationSessionId(7).build());
                } catch (BusinessRuleException ex) {
                    assertThat(ex.getCode()).isEqualTo(ErrorCode.CALIBRATION_SESSION_CLOSED);
                }
                return null;
            });
            Future<?> closeFuture = pool.submit(() -> {
                start.await(2, TimeUnit.SECONDS);
                status.set(CalibrationStatus.CLOSED);
                return null;
            });
            start.countDown();
            scoreFuture.get(5, TimeUnit.SECONDS);
            closeFuture.get(5, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void sessionTrackMismatchSubmission_throwsInvalidState() {
        Fixture f = mockOpenFixture();
        Track trackA = Track.builder().id(3).build();
        Track trackB = Track.builder().id(99).build();
        f.submission.setTrack(trackB);
        when(criteriaRepository.findById(1)).thenReturn(Optional.of(
                Criteria.builder().id(1).maxScore(10).track(trackB).build()));
        when(calibrationSessionRepository.findById(7)).thenReturn(Optional.of(CalibrationSession.builder()
                .id(7)
                .round(f.round)
                .track(trackA)
                .status(CalibrationStatus.OPEN)
                .sampleSubmission(f.submission)
                .build()));

        assertThatThrownBy(() -> scoreService.submitCalibrationScore(SubmitCalibrationScoreRequest.builder()
                        .submissionId(42)
                        .criterionId(1)
                        .scoreValue(8f)
                        .calibrationSessionId(7)
                        .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.INVALID_STATE);
    }

    @Test
    void sessionTrackMatchesSubmission_ok() {
        Fixture f = mockOpenFixture();
        Track track = f.submission.getTrack();
        when(calibrationSessionRepository.findById(7)).thenReturn(Optional.of(CalibrationSession.builder()
                .id(7)
                .round(f.round)
                .track(track)
                .status(CalibrationStatus.OPEN)
                .sampleSubmission(f.submission)
                .build()));
        when(scoreRepository.findBySubmission_IdAndJudge_IdAndCriterion_IdAndScoreType(
                42, 99, 1, ScoreType.CALIBRATION)).thenReturn(Optional.empty());
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> {
            Score s = inv.getArgument(0);
            s.setId(501);
            return s;
        });

        ScoreResponse res = scoreService.submitCalibrationScore(SubmitCalibrationScoreRequest.builder()
                .submissionId(42)
                .criterionId(1)
                .scoreValue(8f)
                .calibrationSessionId(7)
                .build());

        assertThat(res.getId()).isEqualTo(501);
    }

    private Fixture mockOpenFixture() {
        Track track = Track.builder().id(3).build();
        Round round = Round.builder().id(5).scoringLocked(false).build();
        track.setRound(round);
        Submission submission = Submission.builder()
                .id(42)
                .team(Team.builder().id(9).build())
                .track(track)
                .round(round)
                .build();
        Criteria criterion = Criteria.builder().id(1).maxScore(10).track(track).build();
        CalibrationSession session = CalibrationSession.builder()
                .id(7)
                .round(round)
                .status(CalibrationStatus.OPEN)
                .sampleSubmission(submission)
                .build();

        when(currentUserAccessor.currentUserId()).thenReturn(99);
        when(submissionRepository.findById(42)).thenReturn(Optional.of(submission));
        when(criteriaRepository.findById(1)).thenReturn(Optional.of(criterion));
        when(calibrationSessionRepository.findById(7)).thenReturn(Optional.of(session));
        when(userRepository.findById(99)).thenReturn(Optional.of(User.builder().id(99).build()));
        doNothing().when(judgeAssignmentGuard).requireJudgeForSubmission(99, submission);
        doNothing().when(mentorJudgeConflictGuard).requireNoConflict(99, submission);

        return new Fixture(round, submission);
    }

    private record Fixture(Round round, Submission submission) {}
}
