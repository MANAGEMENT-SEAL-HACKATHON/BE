package com.sealhackathon.api.scores.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitCalibrationScoreRequest {

    @NotNull
    private Integer submissionId;

    @NotNull
    private Integer criterionId;

    @NotNull
    private Float scoreValue;

    private Integer calibrationSessionId;

    private String comment;
}
