package com.sealhackathon.api.rbl.service.impl;

import com.sealhackathon.api.rbl.dto.response.RblScoringProgressResponse;
import com.sealhackathon.api.rbl.dto.response.RblVarianceItemResponse;
import com.sealhackathon.api.rbl.service.RblDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RblDashboardServiceImpl implements RblDashboardService {

    @Override
    public List<RblVarianceItemResponse> varianceByRound(Integer roundId) {
        // TODO: FR-30 query v_judge_score_variance for roundId.
        return Collections.emptyList();
    }

    @Override
    public RblScoringProgressResponse scoringProgress(Integer roundId) {
        // TODO: FR-30 query v_scoring_progress for roundId.
        return RblScoringProgressResponse.builder().roundId(roundId).build();
    }
}
