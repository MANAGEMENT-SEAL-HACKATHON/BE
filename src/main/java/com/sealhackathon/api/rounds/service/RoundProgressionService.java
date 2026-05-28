package com.sealhackathon.api.rounds.service;

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

import java.util.List;

public interface RoundProgressionService {

    RoundSummaryResponse releaseProblem(Integer roundId, ReleaseProblemRequest req);

    RoundSummaryResponse lockScoring(Integer roundId, LockScoringRequest req);

    RoundScoringProgressResponse scoringProgress(Integer roundId);

    List<RoundRankingItemResponse> ranking(Integer roundId);

    List<RoundRankingItemResponse> rankingPreview(Integer roundId);

    List<TiebreakItemResponse> tiebreak(Integer roundId);

    List<RoundRankingItemResponse> resolveTiebreak(Integer roundId, ResolveTiebreakRequest req);

    List<WildcardCandidateResponse> wildcardCandidates(Integer roundId);

    List<WildcardCandidateResponse> wildcardApprove(Integer roundId, WildcardDecisionRequest req);

    List<WildcardCandidateResponse> wildcardReject(Integer roundId, WildcardDecisionRequest req);

    AdvanceTeamsResponse advanceTeams(Integer roundId, AdvanceTeamsRequest req);

    FinalJudgeAssignmentResponse assignFinalJudges(Integer roundId, AssignFinalJudgesRequest req);

    RoundScoreboardResponse scoreboard(Integer roundId);
}
