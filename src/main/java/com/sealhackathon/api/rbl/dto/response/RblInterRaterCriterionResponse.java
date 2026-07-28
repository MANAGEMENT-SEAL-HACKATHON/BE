package com.sealhackathon.api.rbl.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RblInterRaterCriterionResponse {

    private final Integer criterionId;
    private final String criterionName;
    private final String criterionType;
    /** Mean of per-submission inter-rater STDDEV across judges (PENALTY excluded). */
    private final Double meanInterRaterStdDev;
    private final Integer submissionCount;
}
