package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.dto.request.LockScoringRequest;
import com.sealhackathon.api.rounds.dto.response.RoundScoringProgressResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.query.ScoringProgressQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.support.RoundPresentationReadiness;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundProgressionServiceImplLockScoringSequenceTest {

    @Mock private RoundRepository roundRepository;
    @Mock private RoundMapper roundMapper;
    @Mock private AuditService auditService;
    @Mock private RoundAccessGuard roundAccessGuard;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ScoringProgressQueryService scoringProgressQueryService;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private RoundPresentationReadiness roundPresentationReadiness;
    @Mock private com.sealhackathon.api.hackathons.repository.HackathonRepository hackathonRepository;
    @Mock private com.sealhackathon.api.notifications.service.NotificationService notificationService;
    @Mock private com.sealhackathon.api.tracks.repository.TrackRepository trackRepository;
    @Mock private com.sealhackathon.api.mentors.repository.MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private com.sealhackathon.api.criteria.repository.CriteriaRepository criteriaRepository;
    @Mock private com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private com.sealhackathon.api.scores.repository.ScoreRepository scoreRepository;
    @Mock private com.sealhackathon.api.rounds.query.RoundRankingQueryService roundRankingQueryService;
    @Mock private com.sealhackathon.api.teams.repository.TeamRoundTrackRepository teamRoundTrackRepository;
    @Mock private com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository teamRoundParticipationRepository;
    @Mock private com.sealhackathon.api.judge_assignments.service.JudgeAssignmentService judgeAssignmentService;
    @Mock private com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository tiebreakEvaluationRepository;
    @Mock private com.sealhackathon.api.teams.repository.TeamRepository teamRepository;
    @Mock private com.sealhackathon.api.wildcard_reviews.repository.WildcardReviewRepository wildcardReviewRepository;
    @Mock private com.sealhackathon.api.wildcard_reviews.repository.WildcardOverrideHistoryRepository wildcardOverrideHistoryRepository;
    @Mock private com.sealhackathon.api.rounds.support.RoundProblemStatementStorage problemStatementStorage;
    @Mock private com.sealhackathon.api.teams.repository.TeamMemberRepository teamMemberRepository;
    @Mock private com.sealhackathon.api.submissions.repository.SubmissionRepository submissionRepository;

    @InjectMocks
    private RoundProgressionServiceImpl service;

    private Round round;

    @BeforeEach
    void setUp() {
        Hackathon hackathon = new Hackathon();
        hackathon.setId(1);
        LocalDateTime past = LocalDateTime.now().minusHours(1);
        round = Round.builder()
                .id(10)
                .hackathon(hackathon)
                .roundType(RoundType.PRELIMINARY)
                .name("Sơ loại")
                .isActive(true)
                .scoringLocked(false)
                .examAt(past.minusHours(2))
                .submissionDeadline(past)
                .submissionClosedEarlyAt(past)
                .build();
    }

    @Test
    void lockScoring_notShuffled_rejectsEvenWithForce() {
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);
        doThrow(new BusinessRuleException(ErrorCode.INVALID_ROUND_STATE_QUEUE_NOT_SHUFFLED, "x"))
                .when(roundPresentationReadiness).assertShuffled(round);

        assertThatThrownBy(() -> service.lockScoring(10,
                        LockScoringRequest.builder().force(true).reason("urgent").build()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.INVALID_ROUND_STATE_QUEUE_NOT_SHUFFLED));

        verify(roundRepository, never()).save(any());
    }

    @Test
    void lockScoring_presentationsIncomplete_rejectsEvenWithForce() {
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);
        doThrow(new BusinessRuleException(ErrorCode.INVALID_ROUND_STATE_PRESENTATIONS_INCOMPLETE, "x"))
                .when(roundPresentationReadiness).assertPresentationsComplete(round);

        assertThatThrownBy(() -> service.lockScoring(10,
                        LockScoringRequest.builder().force(true).reason("urgent").build()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.INVALID_ROUND_STATE_PRESENTATIONS_INCOMPLETE));

        verify(roundRepository, never()).save(any());
    }

    @Test
    void lockScoring_scoringIncomplete_withoutForce_rejects() {
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);
        when(scoringProgressQueryService.progressForRound(round)).thenReturn(
                RoundScoringProgressResponse.builder().pendingSubmissions(3).build());

        assertThatThrownBy(() -> service.lockScoring(10, LockScoringRequest.builder().build()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.INVALID_ROUND_STATE_SCORING_INCOMPLETE));

        verify(roundRepository, never()).save(any());
    }

    @Test
    void lockScoring_zeroPending_afterShuffledAndComplete_locks() {
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);
        when(scoringProgressQueryService.progressForRound(round)).thenReturn(
                RoundScoringProgressResponse.builder().pendingSubmissions(0).build());
        when(currentUserAccessor.currentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(User.builder().id(1).build()));
        when(roundRepository.save(any(Round.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roundMapper.toSummary(any(Round.class), anyInt(), anyInt(), anyFloat())).thenReturn(null);

        service.lockScoring(10, LockScoringRequest.builder().build());

        verify(roundPresentationReadiness).assertShuffled(round);
        verify(roundPresentationReadiness).assertPresentationsComplete(round);
        verify(roundRepository).save(any(Round.class));
    }

    @Test
    void lockScoring_scoringIncomplete_withForceAndReason_allows() {
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);
        when(scoringProgressQueryService.progressForRound(round)).thenReturn(
                RoundScoringProgressResponse.builder().pendingSubmissions(2).build());
        when(currentUserAccessor.currentUserId()).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(User.builder().id(1).build()));
        when(roundRepository.save(any(Round.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roundMapper.toSummary(any(Round.class), anyInt(), anyInt(), anyFloat())).thenReturn(null);

        service.lockScoring(10, LockScoringRequest.builder().force(true).reason("judge absent").build());

        verify(roundRepository).save(any(Round.class));
    }
}
