package com.sealhackathon.api.rbl.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** FR-30 — tiến độ chấm từ v_scoring_progress (scaffold). */
@Getter
@Builder
@AllArgsConstructor
public class RblScoringProgressResponse {

    private final Integer roundId;
    private final Integer totalSubmissions;
    private final Integer scoredSubmissions;
    private final Double completionPct;
}
