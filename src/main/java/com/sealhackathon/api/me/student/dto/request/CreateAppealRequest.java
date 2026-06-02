package com.sealhackathon.api.me.student.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAppealRequest {

    @NotNull
    private Integer teamId;

    @NotNull
    private Integer roundId;

    @NotBlank
    private String reason;

    private String evidenceUrl;
}
