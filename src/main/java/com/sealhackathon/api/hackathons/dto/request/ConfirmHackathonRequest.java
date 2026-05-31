package com.sealhackathon.api.hackathons.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmHackathonRequest {

    @NotNull
    private Boolean confirm;

    private String note;
}
