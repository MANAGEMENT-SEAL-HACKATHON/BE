package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.dto.response.TiebreakItemResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.query.ScoringProgressQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * GD5-TIEBREAK-01 — chuỗi Chung kết: phát hiện đồng điểm → sau micro-penalty → detector rỗng
 * (nguồn gate Prize + Confirm FINISHED không còn TIEBREAK_UNRESOLVED).
 */
@ExtendWith(MockitoExtension.class)
class RoundProgressionServiceImplGd5TiebreakFlowTest {

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
    @Mock private RoundRankingQueryService roundRankingQueryService;
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
    @Mock private com.sealhackathon.api.announcements.service.AnnouncementService announcementService;
    @Mock private com.sealhackathon.api.live_scoring.PresentationQueuePublisher presentationQueuePublisher;

    @InjectMocks
    private RoundProgressionServiceImpl service;

    @Test
    @DisplayName("GD5-TIEBREAK-01: CK 2 đội đồng điểm → sau penalty → tiebreak rỗng (mở Prize + Confirm)")
    void gd5Tiebreak01_finalRoundResolveClearsGate() {
        Hackathon hackathon = new Hackathon();
        hackathon.setId(7);
        hackathon.setStatus(HackathonStatus.PENDING_CONFIRM);

        Round finalRound = Round.builder()
                .id(50)
                .hackathon(hackathon)
                .isFinal(true)
                .name("Chung kết")
                .isActive(true)
                .scoringLocked(true)
                .tiebreakRule(TiebreakRule.COORDINATOR_DECISION)
                .build();

        when(roundAccessGuard.requireRound(50)).thenReturn(finalRound);

        // Trước phân xử: 2 đội cùng điểm gốc
        when(roundRankingQueryService.rankingForRound(50, false)).thenReturn(List.of(
                RoundRankingItemResponse.builder()
                        .rank(1).teamId(201).teamName("CK-T01").totalScore(9.0).penaltyScore(0.0).build(),
                RoundRankingItemResponse.builder()
                        .rank(2).teamId(202).teamName("CK-T02").totalScore(9.0).penaltyScore(0.0).build()
        ));
        when(submissionRepository.findByRound_Id(50)).thenReturn(List.of());
        when(submissionRepository.findByTrack_Round_Id(50)).thenReturn(List.of());
        when(tiebreakEvaluationRepository.findByRound_Id(50)).thenReturn(List.of());

        List<TiebreakItemResponse> before = service.tiebreak(50);
        assertThat(before).as("trước phân xử phải có 1 nhóm đồng điểm CK").hasSize(1);
        assertThat(before.get(0).getCandidateTeamIds()).containsExactlyInAnyOrder(201, 202);

        // Sau phân xử (pure read-time): T02 bị micro-penalty — không cần migration
        when(roundRankingQueryService.rankingForRound(50, false)).thenReturn(List.of(
                RoundRankingItemResponse.builder()
                        .rank(1).teamId(201).teamName("CK-T01").totalScore(9.0).penaltyScore(0.0).build(),
                RoundRankingItemResponse.builder()
                        .rank(2).teamId(202).teamName("CK-T02").totalScore(9.0).penaltyScore(0.01).build()
        ));

        List<TiebreakItemResponse> after = service.tiebreak(50);
        assertThat(after)
                .as("sau penalty: PrizeServiceImpl + HackathonClosureServiceImpl không còn bị TIEBREAK_UNRESOLVED")
                .isEmpty();
    }
}
