package com.sealhackathon.api.individual_rankings.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndividualRankingItemResponse {

    private Integer userId;
    private String fullName;
    private Float scoreThisHackathon;
    private Float cumulativeScore;
    private Integer rank;
}
