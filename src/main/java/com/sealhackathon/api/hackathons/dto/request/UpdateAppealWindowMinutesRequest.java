package com.sealhackathon.api.hackathons.dto.request;

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
public class UpdateAppealWindowMinutesRequest {

    @NotNull
    @Min(0)
    private Integer appealWindowMinutes;
}
