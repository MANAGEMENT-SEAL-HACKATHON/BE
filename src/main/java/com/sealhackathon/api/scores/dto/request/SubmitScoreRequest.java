package com.sealhackathon.api.scores.dto.request;

import com.sealhackathon.api.scores.value_object.ScoreType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class SubmitScoreRequest {

    @NotNull
    private Integer submissionId;

    @NotNull
    private Integer criterionId;

    @NotNull
    @Min(0)
    @Max(100)
    private Float scoreValue;

    private String comment;

    @Builder.Default
    private ScoreType scoreType = ScoreType.NORMAL;
}
