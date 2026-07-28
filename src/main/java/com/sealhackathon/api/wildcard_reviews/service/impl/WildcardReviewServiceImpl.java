package com.sealhackathon.api.wildcard_reviews.service.impl;

import com.sealhackathon.api.rounds.service.RoundProgressionService;
import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardOverrideRequest;
import com.sealhackathon.api.wildcard_reviews.dto.response.WildcardReviewResponse;
import com.sealhackathon.api.wildcard_reviews.service.WildcardReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Thin facade over {@link RoundProgressionService} wildcard override (Plan C).
 */
@Service
@RequiredArgsConstructor
public class WildcardReviewServiceImpl implements WildcardReviewService {

    private final RoundProgressionService progressionService;

    @Override
    public WildcardReviewResponse overrideReview(Integer reviewId, WildcardOverrideRequest req) {
        return progressionService.overrideWildcardReview(reviewId, req);
    }
}
