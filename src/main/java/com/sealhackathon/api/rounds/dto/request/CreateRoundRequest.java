package com.sealhackathon.api.rounds.dto.request;

import com.sealhackathon.api.rounds.value_object.LateSubmissionPolicy;
import com.sealhackathon.api.rounds.value_object.RoundType;
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
 * FR-02 POST /api/v1/hackathons/{hackathonId}/rounds
 *
 * <p><b>KHÔNG validate tổng weight Criteria</b> tại đây — Criteria chưa tồn tại.
 *
 * <p>Cross-field business validate:
 * <ul>
 *   <li>{@code submissionDeadline > submissionOpen} (nếu có) → 422 {@code ROUND_DEADLINE_INVALID}</li>
 *   <li>{@code submissionDeadline > NOW()} → 422 {@code ROUND_DEADLINE_INVALID}</li>
 *   <li>{@code forceLocked = true} → {@code forceLockReason} bắt buộc → 422 {@code ROUND_FORCE_LOCK_REASON}</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoundRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    @Min(1)
    private Integer sequenceOrder;

    private Boolean isFinal;

    private RoundType roundType;

    private LateSubmissionPolicy lateSubmissionPolicy;

    private LocalDateTime submissionOpen;

    @NotNull
    private LocalDateTime submissionDeadline;

    @Min(0)
    private Integer codingDurationHours;

    private String problemStatementUrl;

    private LocalDateTime problemReleasedAt;

    /**
     * Bắt buộc NOT NULL ở Round không phải cuối; NULL ở Round Chung kết. Validate nghiệp vụ — service phát warning.
     */
    @Min(1)
    private Integer topNAdvance;

    private Boolean wildcardEnabled;

    @Min(1)
    private Integer minTeamsFinal;

    private TiebreakRule tiebreakRule;
}
