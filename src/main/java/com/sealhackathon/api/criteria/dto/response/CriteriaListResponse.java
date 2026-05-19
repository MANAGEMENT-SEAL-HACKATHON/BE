package com.sealhackathon.api.criteria.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Response cho GET list criteria — đính kèm weightSummary để FE không phải gọi 2 lần.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CriteriaListResponse {

    private final List<CriterionResponse> items;
    private final WeightSummaryResponse weightSummary;
}
