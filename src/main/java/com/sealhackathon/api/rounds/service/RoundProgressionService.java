package com.sealhackathon.api.rounds.service;

import com.sealhackathon.api.rounds.dto.request.AdvanceTeamsRequest;
import com.sealhackathon.api.rounds.dto.request.AssignFinalJudgesRequest;
import com.sealhackathon.api.rounds.dto.request.LockScoringRequest;
import com.sealhackathon.api.rounds.dto.request.ResolveTiebreakRequest;
import com.sealhackathon.api.rounds.dto.response.AdvanceTeamsResponse;
import com.sealhackathon.api.rounds.dto.response.AssignFinalJudgesResult;
import com.sealhackathon.api.rounds.dto.response.CloseSubmissionEarlyResponse;
import com.sealhackathon.api.rounds.dto.response.FinalJudgeAssignmentResponse;
import com.sealhackathon.api.rounds.dto.response.LockScoringResult;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoreboardResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoringProgressResponse;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;
import com.sealhackathon.api.rounds.dto.response.TiebreakItemResponse;
import com.sealhackathon.api.rounds.dto.response.WildcardCandidatesResponse;
import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardReviewDecisionRequest;
import com.sealhackathon.api.wildcard_reviews.dto.response.WildcardReviewResponse;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface RoundProgressionService {

    RoundSummaryResponse releaseProblem(Integer roundId, MultipartFile file);

    CloseSubmissionEarlyResponse closeSubmissionEarly(Integer roundId);

    LockScoringResult lockScoring(Integer roundId, LockScoringRequest req);

    RoundSummaryResponse publish(Integer roundId);

    RoundScoringProgressResponse scoringProgress(Integer roundId);

    List<RoundRankingItemResponse> ranking(Integer roundId);

    List<RoundRankingItemResponse> rankingPreview(Integer roundId);

    List<TiebreakItemResponse> tiebreak(Integer roundId);

    List<RoundRankingItemResponse> resolveTiebreak(Integer roundId, ResolveTiebreakRequest req);

    WildcardCandidatesResponse wildcardCandidates(Integer roundId);

    AdvanceTeamsResponse advanceTeams(Integer roundId, AdvanceTeamsRequest req);

    /** v4.1 alias — {@link #advanceTeams}. */
    default AdvanceTeamsResponse advance(Integer roundId, AdvanceTeamsRequest req) {
        return advanceTeams(roundId, req);
    }

    WildcardReviewResponse decideWildcardReview(Integer reviewId, WildcardReviewDecisionRequest req);

    AssignFinalJudgesResult assignFinalJudges(Integer roundId, AssignFinalJudgesRequest req);

    RoundScoreboardResponse scoreboard(Integer roundId);
}
