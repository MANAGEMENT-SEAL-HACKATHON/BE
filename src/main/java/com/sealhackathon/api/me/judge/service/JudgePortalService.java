package com.sealhackathon.api.me.judge.service;

import com.sealhackathon.api.me.judge.dto.request.JudgeScoreCommentRequest;
import com.sealhackathon.api.me.judge.dto.request.JudgeScoringCompletionRequest;
import com.sealhackathon.api.me.judge.dto.request.TiebreakVoteRequest;
import com.sealhackathon.api.me.judge.dto.response.*;

import java.util.List;

public interface JudgePortalService {

    List<JudgeTrackAssignmentResponse> listTrackAssignments();

    List<JudgeFinalAssignmentResponse> listFinalAssignments();

    List<JudgeScoringScheduleItemResponse> getScoringSchedule(Integer roundId);

    void updateScoringCompletion(JudgeScoringCompletionRequest request);

    List<JudgeScoreSummaryResponse> listMyScores(Integer roundId);

    List<JudgeSubmissionListItemResponse> listSubmissions(Integer roundId, Integer trackId);

    JudgeScoreSummaryResponse updateScoreComment(Integer scoreId, JudgeScoreCommentRequest request);

    TiebreakVoteResponse submitTiebreakVote(TiebreakVoteRequest request);

    JudgeHistoryResponse getHistory(Integer year);
}
