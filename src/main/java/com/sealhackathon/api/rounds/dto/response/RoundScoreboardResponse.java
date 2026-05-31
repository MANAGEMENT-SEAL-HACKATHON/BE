package com.sealhackathon.api.rounds.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoundScoreboardResponse {

    private Integer roundId;
    private String roundName;
    private List<RoundRankingItemResponse> ranking;
}
