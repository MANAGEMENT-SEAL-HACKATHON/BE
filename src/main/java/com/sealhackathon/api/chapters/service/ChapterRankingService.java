package com.sealhackathon.api.chapters.service;

import com.sealhackathon.api.chapters.dto.response.ChapterRankingItemResponse;

import java.util.List;

public interface ChapterRankingService {

    List<ChapterRankingItemResponse> listByHackathon(Integer hackathonId);

    /** FR-33B — worker sau FINISHED. */
    void calculateAsync(Integer hackathonId);
}
