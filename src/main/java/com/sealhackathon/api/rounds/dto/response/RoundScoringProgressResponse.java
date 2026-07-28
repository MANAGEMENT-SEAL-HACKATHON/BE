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
public class RoundScoringProgressResponse {

    private Integer roundId;
    private Integer totalSubmissions;
    private Integer scoredSubmissions;
    private Integer pendingSubmissions;
    private Boolean scoringLocked;
    /** Track → team scoring rows (gradable submissions only). */
    private List<ScoringProgressItemResponse> items;
}
