package com.sealhackathon.api.me.judge.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JudgeScoringCompletionRequest {

    @NotNull
    private Integer assignmentId;

    private String completionStatus;
}
