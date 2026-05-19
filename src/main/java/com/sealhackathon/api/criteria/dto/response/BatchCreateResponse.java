package com.sealhackathon.api.criteria.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * FR-04 POST /criteria/batch — trả về danh sách id vừa tạo + weight summary cập nhật.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchCreateResponse {

    private final List<Integer> createdIds;
    private final WeightSummaryResponse weightSummary;
}
