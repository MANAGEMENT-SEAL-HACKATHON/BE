package com.sealhackathon.api.prizes.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class UpdateAwardedPrizeRequest {
    private String prizeName;
    private Integer teamId;
    @NotBlank
    private String reason;
}
