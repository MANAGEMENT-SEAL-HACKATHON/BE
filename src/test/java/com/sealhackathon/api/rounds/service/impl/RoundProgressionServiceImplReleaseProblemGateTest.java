package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.query.ScoringProgressQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.support.RoundPresentationReadiness;
import com.sealhackathon.api.rounds.support.RoundProblemStatementStorage;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundProgressionServiceImplReleaseProblemGateTest {

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
    @Mock private TrackRepository trackRepository;
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
    @Mock private RoundProblemStatementStorage problemStatementStorage;
    @Mock private com.sealhackathon.api.teams.repository.TeamMemberRepository teamMemberRepository;
    @Mock private com.sealhackathon.api.submissions.repository.SubmissionRepository submissionRepository;
    @Mock private com.sealhackathon.api.rounds.service.RoundLockScoringService roundLockScoringService;
    @Mock private com.sealhackathon.api.appeals.service.AppealWindowService appealWindowService;
    @Mock private com.sealhackathon.api.appeals.repository.AppealRepository appealRepository;

    @InjectMocks
    private RoundProgressionServiceImpl service;

    private Round round;

    @BeforeEach
    void setUp() {
        Hackathon hackathon = new Hackathon();
        hackathon.setId(1);
        round = Round.builder()
                .id(10)
                .hackathon(hackathon)
                .roundType(RoundType.PRELIMINARY)
                .isFinal(false)
                .name("Sơ loại")
                .isActive(true)
                .examAt(LocalDateTime.now().plusMinutes(5))
                .submissionDeadline(LocalDateTime.now().plusHours(7))
                .build();
    }

    /** TC-SYNC-01 — Phát đề bị chặn khi chưa tới examAt. */
    @Test
    void releaseProblem_beforeExamAt_rejectsWhenActiveAndPdfReady() {
        when(roundAccessGuard.requireActiveRound(10)).thenReturn(round);

        assertThatThrownBy(() -> service.releaseProblem(10, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_ROUND_STATE_BEFORE_EXAM);
    }

    /** REL cascade — mọi track active được stamp problemReleasedAt. */
    @Test
    void releaseProblem_prelim_stampsAllActiveTracks() {
        round.setExamAt(LocalDateTime.now().minusMinutes(1));
        Track t1 = Track.builder().id(1).name("RAG").status(TrackStatus.OPEN)
                .problemStatementStorageKey("k1").build();
        Track t2 = Track.builder().id(2).name("Train").status(TrackStatus.OPEN)
                .problemStatementStorageKey("k2").build();
        when(roundAccessGuard.requireActiveRound(10)).thenReturn(round);
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(10)).thenReturn(List.of(t1, t2));
        when(roundRepository.save(any(Round.class))).thenAnswer(inv -> inv.getArgument(0));
        when(trackRepository.save(any(Track.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roundMapper.toSummary(any(Round.class), anyInt(), anyInt(), anyFloat())).thenReturn(null);

        try (MockedStatic<com.sealhackathon.api.tracks.support.TrackProblemStatementStorage> trackStorage =
                     mockStatic(com.sealhackathon.api.tracks.support.TrackProblemStatementStorage.class)) {
            trackStorage.when(() ->
                            com.sealhackathon.api.tracks.support.TrackProblemStatementStorage.hasProblemFile(any(Track.class)))
                    .thenReturn(true);

            assertThatCode(() -> service.releaseProblem(10, null)).doesNotThrowAnyException();
        }

        verify(trackRepository, org.mockito.Mockito.times(2)).save(any(Track.class));
        org.assertj.core.api.Assertions.assertThat(t1.getProblemReleasedAt()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(t2.getProblemReleasedAt()).isNotNull();
    }
}
