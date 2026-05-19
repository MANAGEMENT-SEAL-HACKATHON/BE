package com.sealhackathon.api.rounds.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Bản tóm tắt Round + chỉ số tổng hợp Criteria. Dùng cho list endpoint.
 *
 * <p>Các field bổ sung:
 * <ul>
 *   <li>{@code trackCount}          — số Track con (Round Sơ loại; 0 nếu FINAL)</li>
 *   <li>{@code criteriaCount}       — số Criteria type ≠ PENALTY</li>
 *   <li>{@code currentWeightTotal}  — tổng weight (cho UI realtime ở Bước 4)</li>
 * </ul>
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoundSummaryResponse {

    private final Integer id;
    private final String name;
    private final Integer sequenceOrder;
    private final LocalDateTime submissionDeadline;
    private final Boolean isActive;
    private final Boolean scoringLocked;
    private final Integer trackCount;
    private final Integer criteriaCount;
    private final Float currentWeightTotal;
}
