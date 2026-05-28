package com.sealhackathon.api.rounds.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WildcardCandidateResponse {

    private Integer teamId;
    private String teamName;
    private Double totalScore;
    private String reason;
}
