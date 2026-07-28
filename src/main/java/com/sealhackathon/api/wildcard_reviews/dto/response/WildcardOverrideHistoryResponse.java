package com.sealhackathon.api.wildcard_reviews.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class WildcardOverrideHistoryResponse {

    private final Integer id;
    private final Integer roundId;
    private final Integer reviewId;
    private final Integer teamId;
    private final String teamName;
    private final String category;
    private final String note;
    private final Boolean beforeApproved;
    private final Boolean afterApproved;
    private final Integer byUserId;
    private final String byUserName;
    private final LocalDateTime overriddenAt;
}
