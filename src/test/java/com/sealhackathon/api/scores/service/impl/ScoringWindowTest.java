package com.sealhackathon.api.scores.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ScoringLockedException;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.criteria.entity.Criteria;
import com.sealhackathon.api.criteria.repository.CriteriaRepository;
import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.JudgeSubmissionScoringConfirmationRepository;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.presentation.support.RoundPhaseResolver;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.scores.dto.request.SubmitScoreRequest;
import com.sealhackathon.api.scores.guard.JudgeAssignmentGuard;
import com.sealhackathon.api.scores.guard.MentorJudgeConflictGuard;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.submissions.entity.Submission;
import com.sealhackathon.api.submissions.repository.SubmissionRepository;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScoringWindowTest {

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
    @Mock private JudgeSubmissionScoringConfirmationRepository scoringConfirmationRepository;
    @Spy private RoundPhaseResolver roundPhaseResolver = new RoundPhaseResolver();

    @InjectMocks
    private ScoreServiceImpl scoreService;

    @Test
    void normalScore_blockedWhenSlotWaiting() {
        mockHappyPath(SubmissionStatus.SUBMITTED);
        when(presentationSlotRepository.findByRound_IdAndSubmission_Id(5, 42))
                .thenReturn(Optional.of(PresentationSlot.builder()
                        .queueStatus(PresentationQueueStatus.WAITING)
                        .build()));

        assertThatThrownBy(() -> scoreService.submitScore(SubmitScoreRequest.builder()
                        .submissionId(42)
                        .criterionId(1)
                        .scoreValue(8f)
                        .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.SCORING_NOT_OPEN);
    }

    @Test
    void normalScore_blockedWhenTimerSetup() {
        mockHappyPath(SubmissionStatus.SUBMITTED);
        when(presentationSlotRepository.findByRound_IdAndSubmission_Id(5, 42))
                .thenReturn(Optional.of(PresentationSlot.builder()
                        .queueStatus(PresentationQueueStatus.PRESENTING)
                        .timerPhase(PresentationTimerPhase.SETUP)
                        .build()));

        assertThatThrownBy(() -> scoreService.submitScore(SubmitScoreRequest.builder()
                        .submissionId(42)
                        .criterionId(1)
                        .scoreValue(8f)
                        .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.SCORING_NOT_OPEN);
    }

    @Test
    void normalScore_blockedWhenTimerIdle() {
        mockHappyPath(SubmissionStatus.SUBMITTED);
        when(presentationSlotRepository.findByRound_IdAndSubmission_Id(5, 42))
                .thenReturn(Optional.of(PresentationSlot.builder()
                        .queueStatus(PresentationQueueStatus.PRESENTING)
                        .timerPhase(PresentationTimerPhase.IDLE)
                        .build()));

        assertThatThrownBy(() -> scoreService.submitScore(SubmitScoreRequest.builder()
                        .submissionId(42)
                        .criterionId(1)
                        .scoreValue(8f)
                        .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.SCORING_NOT_OPEN);
    }

    @Test
    void normalScore_allowedWhenJudgingAndPresenting() {
        mockHappyPath(SubmissionStatus.SUBMITTED);
        when(presentationSlotRepository.findByRound_IdAndSubmission_Id(5, 42))
                .thenReturn(Optional.of(PresentationSlot.builder()
                        .queueStatus(PresentationQueueStatus.PRESENTING)
                        .timerPhase(PresentationTimerPhase.PRESENTING)
                        .build()));
        when(scoreRepository.findBySubmission_IdAndJudge_IdAndCriterion_IdAndScoreType(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(scoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scoreService.submitScore(SubmitScoreRequest.builder()
                .submissionId(42)
                .criterionId(1)
                .scoreValue(8f)
                .build());
    }

    @Test
    void finalRoundScore_blockedWhenSlotWaiting() {
        mockHappyPath(SubmissionStatus.SUBMITTED, true);
        when(presentationSlotRepository.findByRound_IdAndSubmission_Id(5, 42))
                .thenReturn(Optional.of(PresentationSlot.builder()
                        .queueStatus(PresentationQueueStatus.WAITING)
                        .build()));

        assertThatThrownBy(() -> scoreService.submitScore(SubmitScoreRequest.builder()
                        .submissionId(42)
                        .criterionId(1)
                        .scoreValue(8f)
                        .build()))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(ErrorCode.SCORING_NOT_OPEN);
    }

    @Test
    void normalScore_blockedWhenScoringLocked() {
        mockHappyPath(SubmissionStatus.SUBMITTED);
        Round locked = Round.builder()
                .id(5)
                .isActive(true)
                .examAt(LocalDateTime.now().minusHours(4))
                .submissionDeadline(LocalDateTime.now().minusMinutes(10))
                .scoringLocked(true)
                .build();
        when(roundRepository.findByIdForUpdate(5)).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> scoreService.submitScore(SubmitScoreRequest.builder()
                        .submissionId(42)
                        .criterionId(1)
                        .scoreValue(8f)
                        .build()))
                .isInstanceOf(ScoringLockedException.class);
    }

    private void mockHappyPath(SubmissionStatus status) {
        mockHappyPath(status, false);
    }

    private void mockHappyPath(SubmissionStatus status, boolean isFinal) {
        Track track = Track.builder().id(3).build();
        Round round = Round.builder()
                .id(5)
                .isActive(true)
                .isFinal(isFinal)
                .examAt(LocalDateTime.now().minusHours(4))
                .submissionDeadline(LocalDateTime.now().minusMinutes(10))
                .scoringLocked(false)
                .build();
        track.setRound(round);
        Submission submission = Submission.builder()
                .id(42)
                .status(status)
                .team(Team.builder().id(9).build())
                .track(track)
                .round(round)
                .build();
        Criteria criterion = Criteria.builder().id(1).maxScore(10).track(track).build();

        when(currentUserAccessor.currentUserId()).thenReturn(99);
        when(submissionRepository.findById(42)).thenReturn(Optional.of(submission));
        when(criteriaRepository.findById(1)).thenReturn(Optional.of(criterion));
        when(roundRepository.findByIdForUpdate(5)).thenReturn(Optional.of(round));
        when(userRepository.findById(99)).thenReturn(Optional.of(User.builder().id(99).build()));
        doNothing().when(judgeAssignmentGuard).requireJudgeForSubmission(99, submission);
        doNothing().when(mentorJudgeConflictGuard).requireNoConflict(99, submission);
    }
}
