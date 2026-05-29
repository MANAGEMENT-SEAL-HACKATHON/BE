package com.sealhackathon.api.rbl.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** FR-30 — mục variance từ v_judge_score_variance (scaffold). */
@Getter
@Builder
@AllArgsConstructor
public class RblVarianceItemResponse {

    private final Integer criterionId;
    private final String criterionName;
    private final String criterionType;
    private final Integer judgeId;
    private final String judgeType;
    private final Double meanScore;
    private final Double stdDev;
}
