package com.sealhackathon.api.rbl.service;

import com.sealhackathon.api.rbl.dto.response.RblScoringProgressResponse;
import com.sealhackathon.api.rbl.dto.response.RblVarianceResponse;

public interface RblDashboardService {

    RblVarianceResponse varianceByRound(Integer roundId);

    RblScoringProgressResponse scoringProgress(Integer roundId);
}
