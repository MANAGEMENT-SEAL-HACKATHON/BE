package com.sealhackathon.api.chapter_rankings.service;

import com.sealhackathon.api.chapter_rankings.dto.response.ChapterRankingItemResponse;

import java.util.List;

public interface ChapterRankingService {

    List<ChapterRankingItemResponse> listByHackathon(Integer hackathonId);

    /** FR-33B — worker sau FINISHED. */
    void calculateAsync(Integer hackathonId);
}
