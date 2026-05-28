package com.sealhackathon.api.rounds.service.impl;

import com.sealhackathon.api.rounds.dto.request.AdvanceTeamsRequest;
import com.sealhackathon.api.rounds.dto.request.AssignFinalJudgesRequest;
import com.sealhackathon.api.rounds.dto.request.LockScoringRequest;
import com.sealhackathon.api.rounds.dto.request.ReleaseProblemRequest;
import com.sealhackathon.api.rounds.dto.request.ResolveTiebreakRequest;
import com.sealhackathon.api.rounds.dto.request.WildcardDecisionRequest;
import com.sealhackathon.api.rounds.dto.response.AdvanceTeamsResponse;
import com.sealhackathon.api.rounds.dto.response.FinalJudgeAssignmentResponse;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoreboardResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoringProgressResponse;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;
import com.sealhackathon.api.rounds.dto.response.TiebreakItemResponse;
import com.sealhackathon.api.rounds.dto.response.WildcardCandidateResponse;
import com.sealhackathon.api.rounds.service.RoundProgressionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RoundProgressionServiceImpl implements RoundProgressionService {

    @Override
    public RoundSummaryResponse releaseProblem(Integer roundId, ReleaseProblemRequest req) {
        // TODO: FR-21 set problem_released_at + url with one-way policy.
        return RoundSummaryResponse.builder().id(roundId).build();
    }

    @Override
    public RoundSummaryResponse lockScoring(Integer roundId, LockScoringRequest req) {
        // TODO: FR-26/36 lock scoring with optional force lock reason.
        return RoundSummaryResponse.builder().id(roundId).build();
    }

    @Override
    @Transactional(readOnly = true)
    public RoundScoringProgressResponse scoringProgress(Integer roundId) {
        // TODO: Aggregate scoring progress and warning flags.
        return RoundScoringProgressResponse.builder().roundId(roundId).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoundRankingItemResponse> ranking(Integer roundId) {
        // TODO: FR-27 ranking per partition with weighted scores.
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoundRankingItemResponse> rankingPreview(Integer roundId) {
        // TODO: FR-27 preview before lock/confirm.
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TiebreakItemResponse> tiebreak(Integer roundId) {
        // TODO: FR-28 list unresolved ties at cutoff.
        return List.of();
    }

    @Override
    public List<RoundRankingItemResponse> resolveTiebreak(Integer roundId, ResolveTiebreakRequest req) {
        // TODO: FR-28 persist tie resolution and return refreshed ranking.
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WildcardCandidateResponse> wildcardCandidates(Integer roundId) {
        // TODO: FR-29 compute wildcard candidates when min final teams not met.
        return List.of();
    }

    @Override
    public List<WildcardCandidateResponse> wildcardApprove(Integer roundId, WildcardDecisionRequest req) {
        // TODO: FR-29 approve wildcard candidate.
        return List.of();
    }

    @Override
    public List<WildcardCandidateResponse> wildcardReject(Integer roundId, WildcardDecisionRequest req) {
        // TODO: FR-29 reject wildcard candidate.
        return List.of();
    }

    @Override
    public AdvanceTeamsResponse advanceTeams(Integer roundId, AdvanceTeamsRequest req) {
        // TODO: FR-30 batch advance + eliminate with idempotent semantics.
        return AdvanceTeamsResponse.builder()
                .roundId(roundId)
                .advancedTeamIds(req.getAdvancedTeamIds())
                .eliminatedTeamIds(req.getEliminatedTeamIds())
                .build();
    }

    @Override
    public FinalJudgeAssignmentResponse assignFinalJudges(Integer roundId, AssignFinalJudgesRequest req) {
        // TODO: FR-31 assign final round external judges only.
        return FinalJudgeAssignmentResponse.builder()
                .roundId(roundId)
                .judgeIds(req.getJudgeIds())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RoundScoreboardResponse scoreboard(Integer roundId) {
        // TODO: Public scoreboard after lock and publication rules.
        return RoundScoreboardResponse.builder()
                .roundId(roundId)
                .ranking(List.of())
                .build();
    }
}
