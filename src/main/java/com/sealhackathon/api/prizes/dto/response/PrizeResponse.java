package com.sealhackathon.api.prizes.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sealhackathon.api.prizes.value_object.PrizeRank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrizeResponse {

    private final Integer id;
    private final Integer hackathonId;
    private final Integer roundId;
    private final Integer trackId;
    private final Integer teamId;
    private final String prizeName;
    private final PrizeRank prizeRank;
    private final String prizeValue;
    private final String description;
    private final LocalDateTime awardedAt;
    private final Integer awardedById;
}
