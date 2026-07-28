package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.dto.request.LockScoringRequest;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.query.ScoringProgressQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundProgressionServiceImplLockScoringGateTest {

    @Mock private RoundRepository roundRepository;
    @Mock private RoundMapper roundMapper;
    @Mock private AuditService auditService;
    @Mock private RoundAccessGuard roundAccessGuard;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ScoringProgressQueryService scoringProgressQueryService;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private com.sealhackathon.api.rounds.support.RoundPresentationReadiness roundPresentationReadiness;
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
        LocalDateTime pastExam = LocalDateTime.now().minusMinutes(30);
        round = Round.builder()
                .id(10)
                .hackathon(hackathon)
                .roundType(RoundType.PRELIMINARY)
                .name("Sơ loại")
                .isActive(true)
                .scoringLocked(false)
                .examAt(pastExam)
                .submissionOpen(pastExam)
                .submissionDeadline(LocalDateTime.now().plusHours(2))
                .build();
    }

    /** TC-GATE-03 — ONGOING (not closed early, deadline in future); force must not bypass. */
    @Test
    void lockScoring_ongoing_rejectsWithNotClosedCodeEvenWithForce() {
        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);

        assertThatThrownBy(() -> service.lockScoring(10,
                        LockScoringRequest.builder().force(true).reason("emergency").build()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.INVALID_ROUND_STATE_NOT_CLOSED));

        verify(roundRepository, never()).save(any());
    }

    /** TC-SYNC-04 — Final closed but not shuffled; force must not bypass Gate 2. */
    @Test
    void lockScoring_finalNotShuffled_rejectsQueueNotShuffledEvenWithForce() {
        Hackathon hackathon = new Hackathon();
        hackathon.setId(1);
        Round finalRound = Round.builder()
                .id(20)
                .hackathon(hackathon)
                .isFinal(true)
                .isActive(true)
                .scoringLocked(false)
                .presentationShuffled(false)
                .submissionClosedEarlyAt(LocalDateTime.now().minusMinutes(5))
                .examAt(LocalDateTime.now().minusHours(1))
                .submissionDeadline(LocalDateTime.now().plusHours(1))
                .build();
        when(roundAccessGuard.requireActiveRoundForUpdate(20)).thenReturn(finalRound);
        org.mockito.Mockito.doThrow(
                        new BusinessRuleException(ErrorCode.INVALID_ROUND_STATE_QUEUE_NOT_SHUFFLED, "x"))
                .when(roundPresentationReadiness).assertShuffled(finalRound);

        assertThatThrownBy(() -> service.lockScoring(20,
                        LockScoringRequest.builder().force(true).reason("emergency").build()))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode())
                        .isEqualTo(ErrorCode.INVALID_ROUND_STATE_QUEUE_NOT_SHUFFLED));

        verify(roundRepository, never()).save(any());
    }
}
