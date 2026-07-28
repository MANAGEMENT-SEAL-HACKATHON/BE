package com.sealhackathon.api.rounds.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloseSubmissionEarlyResponse {

    private final RoundSummaryResponse round;
    private final boolean examAtAdjusted;
    private final boolean deadlineAdjusted;
    private final LocalDateTime closedAt;
}
