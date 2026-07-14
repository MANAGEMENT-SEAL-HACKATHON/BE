package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.judge_assignments.service.JudgeAssignmentService;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.presentation.support.RoundPhaseResolver;
import com.sealhackathon.api.presentation.value_object.RoundPhase;
import com.sealhackathon.api.rounds.dto.response.CloseSubmissionEarlyResponse;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.query.ScoringProgressQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.support.RoundProblemStatementStorage;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.wildcard_reviews.repository.WildcardReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoundProgressionServiceImplCloseSubmissionEarlyTest {

    @Mock private RoundRepository roundRepository;
    @Mock private RoundMapper roundMapper;
    @Mock private RoundAccessGuard roundAccessGuard;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private TrackRepository trackRepository;
    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private ScoreRepository scoreRepository;
    @Mock private ScoringProgressQueryService scoringProgressQueryService;
    @Mock private RoundRankingQueryService roundRankingQueryService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private TeamRoundTrackRepository teamRoundTrackRepository;
    @Mock private TeamRoundParticipationRepository teamRoundParticipationRepository;
    @Mock private JudgeAssignmentService judgeAssignmentService;
    @Mock private TiebreakEvaluationRepository tiebreakEvaluationRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private WildcardReviewRepository wildcardReviewRepository;
    @Mock private HackathonRepository hackathonRepository;
    @Mock private RoundProblemStatementStorage problemStatementStorage;
    @Mock private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private RoundProgressionServiceImpl progressionService;

    private final RoundPhaseResolver phaseResolver = new RoundPhaseResolver();

    @Test
    void closeSubmissionEarly_activeFutureDeadlineAndExamAt_clampsBothAndAudits() {
        LocalDateTime future = LocalDateTime.now().plusHours(3);
        Round round = Round.builder()
                .id(10)
                .isActive(true)
                .scoringLocked(false)
                .examAt(future)
                .submissionDeadline(future)
                .build();
        RoundSummaryResponse summary = RoundSummaryResponse.builder().id(10).isActive(true).build();

        when(roundAccessGuard.requireActiveRoundForUpdate(10)).thenReturn(round);
        when(roundRepository.save(any(Round.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roundMapper.toSummary(any(Round.class), eq(0), eq(0), eq(0f))).thenReturn(summary);

        CloseSubmissionEarlyResponse result = progressionService.closeSubmissionEarly(10);

        assertTrue(result.isDeadlineAdjusted());
        assertTrue(result.isExamAtAdjusted());
        assertNotNull(round.getSubmissionClosedEarlyAt());
        assertFalse(round.getExamAt().isAfter(LocalDateTime.now().plusSeconds(1)));
        assertFalse(round.getSubmissionDeadline().isAfter(LocalDateTime.now().plusSeconds(1)));
        assertEquals(RoundPhase.JUDGING, phaseResolver.resolve(round));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> details = ArgumentCaptor.forClass(Map.class);
        verify(auditService).log(
                eq(AuditAction.ROUND_CLOSE_SUBMISSION_EARLY),
                eq("rounds"),
                eq(10),
                details.capture());
        assertEquals(true, details.getValue().get("examAtAdjusted"));
        assertEquals(true, details.getValue().get("deadlineAdjusted"));
    }

    @Test
    void closeSubmissionEarly_inactive_throwsRoundNotActive() {
        when(roundAccessGuard.requireActiveRoundForUpdate(11)).thenThrow(
                new BusinessRuleException(ErrorCode.ROUND_NOT_ACTIVE, "Round chưa được kích hoạt"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> progressionService.closeSubmissionEarly(11));
        assertEquals(ErrorCode.ROUND_NOT_ACTIVE, ex.getCode());
    }

    @Test
    void closeSubmissionEarly_scoringLocked_throwsInvalidState() {
        Round round = Round.builder()
                .id(12)
                .isActive(true)
                .scoringLocked(true)
                .build();
        when(roundAccessGuard.requireActiveRoundForUpdate(12)).thenReturn(round);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> progressionService.closeSubmissionEarly(12));
        assertEquals(ErrorCode.INVALID_STATE, ex.getCode());
    }

    @Test
    void closeSubmissionEarly_secondCall_throwsSubmissionAlreadyClosed() {
        Round round = Round.builder()
                .id(13)
                .isActive(true)
                .scoringLocked(false)
                .submissionClosedEarlyAt(LocalDateTime.now().minusMinutes(5))
                .examAt(LocalDateTime.now().minusMinutes(5))
                .submissionDeadline(LocalDateTime.now().minusMinutes(5))
                .build();
        when(roundAccessGuard.requireActiveRoundForUpdate(13)).thenReturn(round);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> progressionService.closeSubmissionEarly(13));
        assertEquals(ErrorCode.SUBMISSION_ALREADY_CLOSED, ex.getCode());
    }
}
