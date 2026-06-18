package com.sealhackathon.api.me.judge.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeScoreSummaryResponse {

    private Integer scoreId;
    private Integer submissionId;
    private Integer criterionId;
    private String displayCode;
    private BigDecimal totalScore;
    private String comment;
}
