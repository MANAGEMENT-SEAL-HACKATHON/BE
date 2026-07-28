package com.sealhackathon.api.wildcard_reviews.service.impl;

import com.sealhackathon.api.rounds.service.RoundProgressionService;
import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardOverrideRequest;
import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardReviewDecisionRequest;
import com.sealhackathon.api.wildcard_reviews.dto.response.WildcardReviewResponse;
import com.sealhackathon.api.wildcard_reviews.service.WildcardReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WildcardReviewServiceImpl implements WildcardReviewService {

    private final RoundProgressionService progressionService;

    @Override
    public WildcardReviewResponse decide(Integer reviewId, WildcardReviewDecisionRequest req) {
        log.warn(
                "DEPRECATED API called: decide wildcard review id={} — migrate to Plan C confirm/override",
                reviewId);
        return progressionService.decideWildcardReview(reviewId, req);
    }

    @Override
    public WildcardReviewResponse overrideReview(Integer reviewId, WildcardOverrideRequest req) {
        return progressionService.overrideWildcardReview(reviewId, req);
    }
}
