package com.sealhackathon.api.rbl.service;

import com.sealhackathon.api.rbl.dto.response.RblScoringProgressResponse;
import com.sealhackathon.api.rbl.dto.response.RblVarianceItemResponse;

import java.util.List;

public interface RblDashboardService {

    List<RblVarianceItemResponse> varianceByRound(Integer roundId);

    RblScoringProgressResponse scoringProgress(Integer roundId);
}
