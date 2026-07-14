package com.sealhackathon.api.calibration_sessions.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR-29 — POST /calibration-sessions */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCalibrationSessionRequest {

    @NotNull
    private Integer roundId;

    /** Optional — GĐ3 per-track; null for GĐ5 round-scoped. */
    private Integer trackId;

    private Integer sampleSubmissionId;

    private Float targetScore;

    private String instructions;
}
