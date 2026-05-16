package com.se194093.be.criteria.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * FR-04 GET /api/v1/rounds/{roundId}/criteria/weight-summary.
 *
 * <p>Trả về cho UI realtime — KHÔNG ném exception dù tổng lệch.
 *
 * <ul>
 *   <li>{@code status = OK}    nếu {@code |total - 1.0| <= 0.001}</li>
 *   <li>{@code status = WARN}  ngược lại</li>
 * </ul>
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeightSummaryResponse {

    private final Integer roundId;
    private final Double total;
    private final Double missing;
    private final String status;
    private final List<Item> items;

    @Getter
    @Builder
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {
        private final Integer criterionId;
        private final String  name;
        private final Float weight;
    }
}
