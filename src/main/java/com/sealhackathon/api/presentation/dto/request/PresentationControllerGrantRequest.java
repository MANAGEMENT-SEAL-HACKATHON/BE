package com.sealhackathon.api.presentation.dto.request;

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
public class PresentationControllerGrantRequest {

    @NotNull
    private Integer judgeId;

    /**
     * Optional race-guard: expected current controller (null = skip check).
     * When provided and mismatch → 409 CONTROLLER_CONFLICT.
     * Use 0 to expect no override controller.
     */
    private Integer expectedControllerJudgeId;

    /** TRANSFER requires target judge WS-online; TAKEOVER may skip online check. */
    private String mode;
}
