package com.sealhackathon.api.rounds.dto.request;

import jakarta.validation.constraints.Size;
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
public class LockScoringRequest {

    @Builder.Default
    private Boolean force = false;

    @Size(max = 1000)
    private String reason;
}
