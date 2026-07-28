package com.sealhackathon.api.rbl.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Mở rộng GET /rbl/variance — giữ {@code perJudgeSpread} (shape cũ) + thêm inter-rater.
 */
@Getter
@Builder
@AllArgsConstructor
public class RblVarianceResponse {

    private final List<RblVarianceItemResponse> perJudgeSpread;
    private final List<RblInterRaterCriterionResponse> interRaterByCriterion;
}
