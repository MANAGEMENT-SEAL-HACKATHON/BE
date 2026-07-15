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
    private final LocalDateTime examAt;
    private final LocalDateTime submissionDeadline;
    private final Boolean isActive;
    private final Boolean scoringLocked;
    private final Boolean isPublished;
    private final LocalDateTime submissionClosedEarlyAt;
    private final Integer trackCount;
    private final Integer criteriaCount;
    private final Float currentWeightTotal;
    /** Gate 2 — đã xáo hàng đợi (tất cả track / CK). */
    private final Boolean isPresentationShuffled;
    /** Gate 3 — không còn WAITING/PRESENTING (0 slot OK nếu đã shuffle). */
    private final Boolean isPresentationsComplete;
}
