package com.sealhackathon.api.me.judge.service.impl;

import com.sealhackathon.api.me.judge.dto.request.JudgeScoreCommentRequest;
import com.sealhackathon.api.me.judge.dto.request.JudgeScoringCompletionRequest;
import com.sealhackathon.api.me.judge.dto.request.TiebreakVoteRequest;
import com.sealhackathon.api.me.judge.dto.response.*;
import com.sealhackathon.api.me.judge.service.JudgePortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JudgePortalServiceImpl implements JudgePortalService {

    @Override
    public List<JudgeTrackAssignmentResponse> listTrackAssignments() {
        // TODO: FR-J-05 — judge track assignments for current user
        return Collections.emptyList();
    }

    @Override
    public List<JudgeFinalAssignmentResponse> listFinalAssignments() {
        // TODO: FR-J-06 — final round assignments
        return Collections.emptyList();
    }

    @Override
    public List<JudgeScoringScheduleItemResponse> getScoringSchedule(Integer roundId) {
        // TODO: FR-J-12/J-17 — scoring windows; filter by roundId when set
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public void updateScoringCompletion(JudgeScoringCompletionRequest request) {
        // TODO: FR-J-16/20/21 — completion_status (schema backlog)
    }

    @Override
    public List<JudgeScoreSummaryResponse> listMyScores(Integer roundId) {
        // TODO: FR-J-24 — scores submitted by current judge; filter by roundId when set
        return Collections.emptyList();
    }

    @Override
    @Transactional
    public JudgeScoreSummaryResponse updateScoreComment(Integer scoreId, JudgeScoreCommentRequest request) {
        // TODO: FR-J-15 — comment only, no score change
        return JudgeScoreSummaryResponse.builder()
                .scoreId(scoreId)
                .comment(request.getComment())
                .build();
    }

    @Override
    @Transactional
    public TiebreakVoteResponse submitTiebreakVote(TiebreakVoteRequest request) {
        // TODO: FR-J-22/23 — HEAD judge, tiebreak_evaluations INSERT
        return TiebreakVoteResponse.builder()
                .roundId(request.getRoundId())
                .orderedTeamIds(request.getOrderedTeamIds())
                .status("SUBMITTED")
                .build();
    }

    @Override
    public JudgeHistoryResponse getHistory(Integer year) {
        // TODO: FR-J-26 — past judging assignments; filter by year when set
        return JudgeHistoryResponse.builder().items(Collections.emptyList()).build();
    }
}
