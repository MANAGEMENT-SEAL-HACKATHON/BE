package com.sealhackathon.api.calibration_sessions.dto.response;

import com.sealhackathon.api.calibration_sessions.value_object.CalibrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class CalibrationSessionResponse {

    private final Integer id;
    private final Integer roundId;
    private final Integer sampleSubmissionId;
    private final CalibrationStatus status;
    private final Float targetScore;
    private final String instructions;
    private final LocalDateTime startedAt;
    private final LocalDateTime endedAt;
    private final Integer createdById;
}
