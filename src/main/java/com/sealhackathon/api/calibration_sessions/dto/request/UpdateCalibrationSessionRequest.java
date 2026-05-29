package com.sealhackathon.api.calibration_sessions.dto.request;

import com.sealhackathon.api.calibration_sessions.value_object.CalibrationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** FR-29 — PATCH /calibration-sessions/{id} */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCalibrationSessionRequest {

    @NotNull
    private CalibrationStatus status;
}
