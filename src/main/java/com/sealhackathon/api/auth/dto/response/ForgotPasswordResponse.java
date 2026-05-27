package com.sealhackathon.api.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ForgotPasswordResponse {

    private final String message;
    /** Chỉ khi profile dev — link đặt lại mật khẩu stub. */
    private final String devResetToken;
    private final String devResetUrl;
}
