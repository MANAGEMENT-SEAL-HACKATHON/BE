package com.sealhackathon.api.rounds.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoundScoringProgressResponse {

    private Integer roundId;
    private Integer totalSubmissions;
    private Integer scoredSubmissions;
    private Integer pendingSubmissions;
    private Boolean scoringLocked;
}
