package com.sealhackathon.api.chapter_rankings.service.impl;

import com.sealhackathon.api.chapter_rankings.dto.response.ChapterRankingItemResponse;
import com.sealhackathon.api.chapter_rankings.repository.ChapterRankingRepository;
import com.sealhackathon.api.chapter_rankings.service.ChapterRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChapterRankingServiceImpl implements ChapterRankingService {

    private final ChapterRankingRepository chapterRankingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChapterRankingItemResponse> listByHackathon(Integer hackathonId) {
        // TODO: FR-33B — đọc chapter_rankings sau worker; gate HACKATHON_NOT_FINISHED
        return Collections.emptyList();
    }

    @Override
    public void calculateAsync(Integer hackathonId) {
        // TODO: FR-33B — persist chapter_rankings, formula_snapshot, audit
    }
}
