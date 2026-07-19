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

    /**
     * Chỉ hỗ trợ TRANSFER (không còn TAKEOVER). Không yêu cầu judge online —
     * field giữ lại cho tương thích FE, giá trị bị bỏ qua.
     */
    private String mode;
}
