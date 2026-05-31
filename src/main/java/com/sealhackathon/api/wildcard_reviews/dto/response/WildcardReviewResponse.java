package com.sealhackathon.api.wildcard_reviews.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class WildcardReviewResponse {

    private final Integer id;
    private final Integer roundId;
    private final Integer teamId;
    private final Float avgScore;
    private final Boolean coordinatorApproved;
    private final String coordinatorNote;
    private final LocalDateTime reviewedAt;
}
