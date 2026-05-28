package com.sealhackathon.api.rounds.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoundRankingItemResponse {

    private Integer rank;
    private Integer teamId;
    private String teamName;
    private Integer trackId;
    private String assignedGroup;
    private Double totalScore;
    private Boolean tiebreakRequired;
}
