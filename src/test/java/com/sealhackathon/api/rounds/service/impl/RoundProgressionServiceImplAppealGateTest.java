package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.appeals.repository.AppealRepository;
import com.sealhackathon.api.appeals.service.AppealWindowService;
import com.sealhackathon.api.appeals.value_object.AppealStatus;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.dto.request.AdvanceTeamsRequest;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.service.RoundLockScoringService;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoundProgressionServiceImplAppealGateTest {

    @Mock private RoundRepository roundRepository;
    @Mock private RoundMapper roundMapper;
    @Mock private RoundAccessGuard roundAccessGuard;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private com.sealhackathon.api.notifications.service.NotificationService notificationService;
    @Mock private com.sealhackathon.api.tracks.repository.TrackRepository trackRepository;
    @Mock private com.sealhackathon.api.mentors.repository.MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository judgeAssignmentRepository;
    @Mock private com.sealhackathon.api.scores.repository.ScoreRepository scoreRepository;
    @Mock private com.sealhackathon.api.rounds.query.ScoringProgressQueryService scoringProgressQueryService;
    @Mock private com.sealhackathon.api.rounds.query.RoundRankingQueryService roundRankingQueryService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private com.sealhackathon.api.teams.repository.TeamRoundTrackRepository teamRoundTrackRepository;
    @Mock private com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository teamRoundParticipationRepository;
    @Mock private com.sealhackathon.api.judge_assignments.service.JudgeAssignmentService judgeAssignmentService;
    @Mock private com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository tiebreakEvaluationRepository;
    @Mock private com.sealhackathon.api.teams.repository.TeamRepository teamRepository;
    @Mock private com.sealhackathon.api.hackathons.repository.HackathonRepository hackathonRepository;
    @Mock private com.sealhackathon.api.rounds.support.RoundProblemStatementStorage problemStatementStorage;
    @Mock private com.sealhackathon.api.teams.repository.TeamMemberRepository teamMemberRepository;
    @Mock private com.sealhackathon.api.submissions.repository.SubmissionRepository submissionRepository;
    @Mock private com.sealhackathon.api.rounds.support.RoundPresentationReadiness roundPresentationReadiness;
    @Mock private com.sealhackathon.api.criteria.repository.CriteriaRepository criteriaRepository;
    @Mock private com.sealhackathon.api.announcements.service.AnnouncementService announcementService;
    @Mock private com.sealhackathon.api.live_scoring.PresentationQueuePublisher presentationQueuePublisher;
    @Mock private RoundLockScoringService roundLockScoringService;
    @Mock private AppealWindowService appealWindowService;
    @Mock private AppealRepository appealRepository;

    @InjectMocks private RoundProgressionServiceImpl service;

    private Hackathon hackathon;
    private Round prelim;
    private User publisher;

    @BeforeEach
    void setUp() {
        hackathon = Hackathon.builder().id(1).appealWindowMinutes(30).build();
        prelim = Round.builder()
                .id(10)
                .name("Sơ loại")
                .isFinal(false)
                .scoringLocked(true)
                .isPublished(false)
                .publishRevision(1)
                .hackathon(hackathon)
                .build();
        publisher = User.builder().id(5).build();
        when(roundAccessGuard.requireRound(10)).thenReturn(prelim);
        when(currentUserAccessor.currentUserId()).thenReturn(5);
        when(userRepository.findById(5)).thenReturn(Optional.of(publisher));
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roundRepository.findById(10)).thenReturn(Optional.of(prelim));
        when(roundMapper.toSummary(any(), anyInt(), anyInt(), anyFloat()))
                .thenReturn(RoundSummaryResponse.builder().id(10).build());
        when(teamRoundTrackRepository.findByTrack_Round_Id(10)).thenReturn(List.of());
    }

    @Test
    void publish_happyPath_emptyBody_opensWindowViaService() {
        RoundSummaryResponse resp = service.publish(10, null);

        assertThat(resp.getId()).isEqualTo(10);
        assertThat(prelim.getIsPublished()).isTrue();
        verify(appealWindowService).openOnFirstPublish(eq(prelim), eq(null), any());
    }

    @Test
    void advanceTeams_blockedWhenPendingAppeals() {
        prelim.setIsPublished(true);
        prelim.setScoringLocked(true);
        when(appealWindowService.expireOpenAppealsForRound(10)).thenReturn(0);
        when(appealRepository.existsByRound_IdAndStatusIn(eq(10), any())).thenReturn(true);
        when(appealRepository.countByRound_IdAndStatus(10, AppealStatus.PENDING)).thenReturn(1L);
        when(appealRepository.countByRound_IdAndStatus(10, AppealStatus.UNDER_REVIEW)).thenReturn(0L);

        AdvanceTeamsRequest req = new AdvanceTeamsRequest();
        req.setAdvancedTeamIds(List.of(1));
        req.setEliminatedTeamIds(List.of());

        assertThatThrownBy(() -> service.advanceTeams(10, req))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.APPEAL_PENDING_BLOCKS_ADVANCE);
    }

    @Test
    void advanceTeams_callsExpireThenAllowsWhenNoOpenAppeals() {
        prelim.setIsPublished(true);
        prelim.setScoringLocked(true);
        when(appealWindowService.expireOpenAppealsForRound(10)).thenReturn(2);
        when(appealRepository.existsByRound_IdAndStatusIn(eq(10), any())).thenReturn(false);
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(1)).thenReturn(Optional.of(
                Round.builder().id(20).isFinal(true).hackathon(hackathon).build()));

        AdvanceTeamsRequest req = new AdvanceTeamsRequest();
        req.setAdvancedTeamIds(List.of());
        req.setEliminatedTeamIds(List.of());

        try {
            service.advanceTeams(10, req);
        } catch (BusinessRuleException ex) {
            assertThat(ex.getCode()).isNotEqualTo(ErrorCode.APPEAL_PENDING_BLOCKS_ADVANCE);
        }
        verify(appealWindowService).expireOpenAppealsForRound(10);
    }
}
