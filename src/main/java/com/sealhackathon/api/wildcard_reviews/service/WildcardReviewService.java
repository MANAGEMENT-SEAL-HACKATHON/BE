package com.sealhackathon.api.wildcard_reviews.service;

import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardOverrideRequest;
import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardReviewDecisionRequest;
import com.sealhackathon.api.wildcard_reviews.dto.response.WildcardReviewResponse;

public interface WildcardReviewService {

    WildcardReviewResponse decide(Integer reviewId, WildcardReviewDecisionRequest req);

    WildcardReviewResponse overrideReview(Integer reviewId, WildcardOverrideRequest req);
}
