package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.dto.request.ResolveTiebreakRequest;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.dto.response.TiebreakItemResponse;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.guard.RoundAccessGuard;
import com.sealhackathon.api.rounds.mapper.RoundMapper;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.query.ScoringProgressQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.service.RoundLockScoringService;
import com.sealhackathon.api.rounds.value_object.RoundType;
import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import com.sealhackathon.api.tiebreak_evaluations.entity.TiebreakEvaluation;
import com.sealhackathon.api.tiebreak_evaluations.repository.TiebreakEvaluationRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TB-01..TB-07 — ghost-tiebreak detector (effective score = totalScore − penalty).
 */
@ExtendWith(MockitoExtension.class)
class RoundProgressionServiceImplTiebreakDetectionTest {

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
    @Mock private TiebreakEvaluationRepository tiebreakEvaluationRepository;
    @Mock private com.sealhackathon.api.teams.repository.TeamRepository teamRepository;
    @Mock private com.sealhackathon.api.wildcard_reviews.repository.WildcardReviewRepository wildcardReviewRepository;
    @Mock private com.sealhackathon.api.wildcard_reviews.repository.WildcardOverrideHistoryRepository wildcardOverrideHistoryRepository;
    @Mock private com.sealhackathon.api.rounds.support.RoundProblemStatementStorage problemStatementStorage;
    @Mock private com.sealhackathon.api.teams.repository.TeamMemberRepository teamMemberRepository;
    @Mock private com.sealhackathon.api.submissions.repository.SubmissionRepository submissionRepository;
    @Mock private RoundLockScoringService roundLockScoringService;
    @Mock private com.sealhackathon.api.announcements.service.AnnouncementService announcementService;
    @Mock private com.sealhackathon.api.live_scoring.PresentationQueuePublisher presentationQueuePublisher;

    @InjectMocks
    private RoundProgressionServiceImpl service;

    private Hackathon hackathon;
    private Round prelimRound;
    private Round finalRound;

    @BeforeEach
    void setUp() {
        hackathon = new Hackathon();
        hackathon.setId(7);
        hackathon.setStatus(HackathonStatus.ONGOING);

        prelimRound = Round.builder()
                .id(11)
                .hackathon(hackathon)
                .roundType(RoundType.PRELIMINARY)
                .name("Sơ loại")
                .isFinal(false)
                .isActive(true)
                .scoringLocked(true)
                .topNAdvance(1)
                .tiebreakRule(TiebreakRule.COORDINATOR_DECISION)
                .build();

        finalRound = Round.builder()
                .id(50)
                .hackathon(hackathon)
                .isFinal(true)
                .name("Chung kết")
                .isActive(true)
                .scoringLocked(true)
                .tiebreakRule(TiebreakRule.COORDINATOR_DECISION)
                .build();
    }

    @Test
    @DisplayName("TB-01: 2 đội đồng điểm gốc tại ranh giới → tiebreak() trả 1 item")
    void tb01_twoTeamsTiedAtCutoff_returnsOneItem() {
        when(roundAccessGuard.requireRound(11)).thenReturn(prelimRound);
        stubEmptyEnrichLookups(11);
        when(roundRankingQueryService.rankingForRound(11, false)).thenReturn(List.of(
                rank(1, 101, "T01", 1, "A", 8.50, 0.0),
                rank(2, 102, "T02", 1, "A", 8.50, 0.0),
                rank(3, 103, "T03", 1, "A", 7.00, 0.0)
        ));

        List<TiebreakItemResponse> items = service.tiebreak(11);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getCandidateTeamIds()).containsExactlyInAnyOrder(101, 102);
    }

    @Test
    @DisplayName("TB-02: sau micro-penalty Sơ loại → tiebreak() rỗng")
    void tb02_afterPenaltyPrelim_tiebreakEmpty() {
        when(roundAccessGuard.requireRound(11)).thenReturn(prelimRound);
        when(roundRankingQueryService.rankingForRound(11, false)).thenReturn(List.of(
                rank(1, 101, "T01", 1, "A", 8.50, 0.0),
                rank(2, 102, "T02", 1, "A", 8.50, 0.01),
                rank(3, 103, "T03", 1, "A", 7.00, 0.0)
        ));

        assertThat(service.tiebreak(11)).isEmpty();
    }

    @Test
    @DisplayName("TB-03: sau micro-penalty Chung kết → tiebreak() rỗng")
    void tb03_afterPenaltyFinal_tiebreakEmpty() {
        when(roundAccessGuard.requireRound(50)).thenReturn(finalRound);
        when(roundRankingQueryService.rankingForRound(50, false)).thenReturn(List.of(
                rank(1, 201, "F01", null, null, 9.00, 0.0),
                rank(2, 202, "F02", null, null, 9.00, 0.01)
        ));

        assertThat(service.tiebreak(50)).isEmpty();
    }

    @Test
    @DisplayName("TB-04: resolve lần 2 nhóm đã resolve → 409 TIEBREAK_ALREADY_RESOLVED")
    void tb04_secondResolve_conflictAlreadyResolved() {
        when(roundAccessGuard.requireRound(11)).thenReturn(prelimRound);
        when(roundRepository.findByIdForUpdate(11)).thenReturn(Optional.of(prelimRound));
        User coord = User.builder().id(99).build();
        when(currentUserAccessor.currentUserId()).thenReturn(99);
        when(userRepository.findById(99)).thenReturn(Optional.of(coord));

        // Detector đã clear (điểm hiệu lực lệch) — không còn nhóm khớp
        when(roundRankingQueryService.rankingForRound(11, false)).thenReturn(List.of(
                rank(1, 101, "T01", 1, "A", 8.50, 0.0),
                rank(2, 102, "T02", 1, "A", 8.50, 0.01)
        ));
        // Có casting-vote từ lần resolve trước
        when(tiebreakEvaluationRepository.findByRound_IdAndTeam_Id(11, 101))
                .thenReturn(List.of());
        when(tiebreakEvaluationRepository.findByRound_IdAndTeam_Id(11, 102))
                .thenReturn(List.of(TiebreakEvaluation.builder()
                        .penaltyScore(0.01f)
                        .isCastingVote(true)
                        .build()));

        ResolveTiebreakRequest req = new ResolveTiebreakRequest();
        req.setOrderedTeamIds(List.of(101, 102));
        req.setNote("retry");

        assertThatThrownBy(() -> service.resolveTiebreak(11, req))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo(ErrorCode.TIEBREAK_ALREADY_RESOLVED));

        verify(tiebreakEvaluationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("TB-05a: detector rỗng → không còn item chặn advance (cùng nguồn gate TIEBREAK_REQUIRED)")
    void tb05a_emptyTiebreak_noUnresolvedForAdvanceGate() {
        when(roundAccessGuard.requireRound(11)).thenReturn(prelimRound);
        when(roundRankingQueryService.rankingForRound(11, false)).thenReturn(List.of(
                rank(1, 101, "T01", 1, "A", 8.50, 0.0),
                rank(2, 102, "T02", 1, "A", 8.50, 0.01)
        ));

        // advanceTeams / Prize / Confirm đều gọi tiebreak(roundId) — rỗng ⇒ không ném TIEBREAK_*
        assertThat(service.tiebreak(11)).isEmpty();
    }

    @Test
    @DisplayName("TB-05b: detector rỗng cho vòng CK — nguồn gate Prize TIEBREAK_UNRESOLVED")
    void tb05b_emptyFinalTiebreak_prizeGateSource() {
        when(roundAccessGuard.requireRound(50)).thenReturn(finalRound);
        when(roundRankingQueryService.rankingForRound(50, false)).thenReturn(List.of(
                rank(1, 201, "F01", null, null, 9.00, 0.0),
                rank(2, 202, "F02", null, null, 9.00, 0.01)
        ));
        assertThat(service.tiebreak(50)).isEmpty();
    }

    @Test
    @DisplayName("TB-05c: detector rỗng cho vòng CK — nguồn gate Confirm FINISHED")
    void tb05c_emptyFinalTiebreak_confirmGateSource() {
        when(roundAccessGuard.requireRound(50)).thenReturn(finalRound);
        when(roundRankingQueryService.rankingForRound(50, false)).thenReturn(List.of(
                rank(1, 201, "F01", null, null, 9.00, 0.0),
                rank(2, 202, "F02", null, null, 8.50, 0.0)
        ));
        assertThat(service.tiebreak(50)).isEmpty();
    }

    @Test
    @DisplayName("TB-06: pure read-time — chỉ ranking có penalty (không gọi resolve) → tiebreak rỗng")
    void tb06_pureReadTime_noResolveNeeded() {
        // Giả lập DB đã có penalty từ lần trước (hackathon id=7): chỉ stub ranking
        when(roundAccessGuard.requireRound(11)).thenReturn(prelimRound);
        when(roundRankingQueryService.rankingForRound(eq(11), eq(false))).thenReturn(List.of(
                rank(1, 101, "GD4-MAN-T01", 1, "A", 8.50, 0.0),
                rank(2, 102, "GD4-MAN-T02", 1, "A", 8.50, 0.01)
        ));

        assertThat(service.tiebreak(11)).isEmpty();
        // Không đụng write path
        verify(tiebreakEvaluationRepository, never()).saveAll(any());
        verify(roundRepository, never()).save(any());
        verify(roundRankingQueryService, atLeastOnce()).rankingForRound(11, false);
    }

    @Test
    @DisplayName("TB-07: 3 đội đồng điểm → 1 đội đã có penalty → vẫn còn 1 nhóm 2 đội đồng điểm")
    void tb07_partialResolve_stillReportsRemainingTie() {
        when(roundAccessGuard.requireRound(11)).thenReturn(prelimRound);
        stubEmptyEnrichLookups(11);
        // topN=1: T03 đã lệch (8.49), T01/T02 vẫn cùng 8.50 tại ranh giới
        when(roundRankingQueryService.rankingForRound(11, false)).thenReturn(List.of(
                rank(1, 101, "T01", 1, "A", 8.50, 0.0),
                rank(2, 102, "T02", 1, "A", 8.50, 0.0),
                rank(3, 103, "T03", 1, "A", 8.50, 0.01),
                rank(4, 104, "T04", 1, "A", 7.00, 0.0)
        ));

        List<TiebreakItemResponse> items = service.tiebreak(11);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getCandidateTeamIds()).containsExactlyInAnyOrder(101, 102);
        assertThat(items.get(0).getCandidateTeamIds()).doesNotContain(103);
    }

    @Test
    @DisplayName("effectiveScore: ONGOING trừ penalty; FINISHED không trừ lần hai")
    void effectiveScore_respectsDisplayNetsPenaltyFlag() {
        RoundRankingItemResponse item = rank(1, 1, "X", 1, "A", 8.49, 0.01);
        assertThat(RoundProgressionServiceImpl.effectiveScoreForTieDetection(item, false))
                .isEqualTo(8.48);
        assertThat(RoundProgressionServiceImpl.effectiveScoreForTieDetection(item, true))
                .isEqualTo(8.49);
    }

    private void stubEmptyEnrichLookups(int roundId) {
        when(submissionRepository.findByRound_Id(roundId)).thenReturn(List.of());
        when(submissionRepository.findByTrack_Round_Id(roundId)).thenReturn(List.of());
        when(tiebreakEvaluationRepository.findByRound_Id(roundId)).thenReturn(List.of());
    }

    private static RoundRankingItemResponse rank(
            int rank, int teamId, String name, Integer trackId, String group,
            double totalScore, double penalty) {
        return RoundRankingItemResponse.builder()
                .rank(rank)
                .teamId(teamId)
                .teamName(name)
                .trackId(trackId)
                .assignedGroup(group)
                .totalScore(totalScore)
                .penaltyScore(penalty)
                .participationStatus("PARTICIPATING")
                .build();
    }
}
