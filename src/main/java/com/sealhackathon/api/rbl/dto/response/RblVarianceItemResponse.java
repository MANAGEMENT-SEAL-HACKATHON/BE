package com.sealhackathon.api.rbl.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * FR-30 — mục variance từ v_judge_score_variance (scaffold).
 * THESIS-RBL-02: không lộ judgeId thô — chỉ pseudonym ổn định (RblJudgeAnonymizer).
 */
@Getter
@Builder
@AllArgsConstructor
public class RblVarianceItemResponse {

    private final Integer criterionId;
    private final String criterionName;
    private final String criterionType;
    private final String anonymizedJudgeId;
    private final String judgeType;
    private final Double meanScore;
    private final Double stdDev;
}
