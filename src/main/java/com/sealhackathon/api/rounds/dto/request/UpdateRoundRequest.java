package com.sealhackathon.api.rounds.dto.request;

import com.sealhackathon.api.rounds.value_object.TiebreakRule;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * FR-03 PUT /api/v1/rounds/{id}
 *
 * <p>Lưu ý:
 * <ul>
 *   <li>KHÔNG nhận {@code trackId} — không cho phép di chuyển Round sang Track khác.</li>
 *   <li>KHÔNG nhận {@code isActive} — chỉ qua PATCH /activate (FR-06B).</li>
 *   <li>Cho phép sửa {@code scoringLocked = true} → audit {@code ROUND_LOCK}.</li>
 *   <li>{@code forceLocked = true} + {@code forceLockReason} rỗng → 422 {@code ROUND_FORCE_LOCK_REASON}.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRoundRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @Min(1)
    private Integer sequenceOrder;

    private LocalDateTime submissionOpen;

    @NotNull
    private LocalDateTime submissionDeadline;

    @Min(0)
    private Integer codingDurationHours;

    private String problemStatementUrl;

    private LocalDateTime problemReleasedAt;

    @Min(1)
    private Integer topNAdvance;

    private Boolean wildcardEnabled;

    @Min(1)
    private Integer minTeamsFinal;

    private TiebreakRule tiebreakRule;

    /**
     * Cho phép Coordinator lock chấm điểm thủ công ở MF-01.
     */
    private Boolean scoringLocked;

    private Boolean forceLocked;

    private String forceLockReason;
}
