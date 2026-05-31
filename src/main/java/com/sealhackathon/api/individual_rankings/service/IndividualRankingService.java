package com.sealhackathon.api.individual_rankings.service;

import com.sealhackathon.api.individual_rankings.dto.response.IndividualRankingItemResponse;

import java.util.List;

public interface IndividualRankingService {

    List<IndividualRankingItemResponse> listByHackathon(Integer hackathonId);

    /** FR-33C — worker sau FINISHED; skip nếu individual_ranking_enabled=false. */
    void calculateAsync(Integer hackathonId);
}
