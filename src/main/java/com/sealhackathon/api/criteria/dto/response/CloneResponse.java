package com.sealhackathon.api.criteria.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * FR-04 POST /criteria/clone — trả về id mới + nguồn + weight summary cập nhật.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloneResponse {

    private final List<Integer> createdIds;
    private final Integer sourceRoundId;
    private final Integer count;
    private final WeightSummaryResponse weightSummary;
}
