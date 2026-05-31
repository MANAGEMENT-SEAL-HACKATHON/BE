package com.sealhackathon.api.individual_rankings.service.impl;

import com.sealhackathon.api.individual_rankings.dto.response.IndividualRankingItemResponse;
import com.sealhackathon.api.individual_rankings.repository.IndividualRankingRepository;
import com.sealhackathon.api.individual_rankings.service.IndividualRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class IndividualRankingServiceImpl implements IndividualRankingService {

    private final IndividualRankingRepository individualRankingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<IndividualRankingItemResponse> listByHackathon(Integer hackathonId) {
        // TODO: FR-33C/33D — 404 INDIVIDUAL_RANKING_NOT_AVAILABLE nếu cờ false
        return Collections.emptyList();
    }

    @Override
    public void calculateAsync(Integer hackathonId) {
        // TODO: FR-33C — persist individual_rankings, cumulative_score
    }
}
