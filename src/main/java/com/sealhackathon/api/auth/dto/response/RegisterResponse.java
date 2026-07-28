package com.sealhackathon.api.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterResponse {

    private final Integer userId;
    private final String email;
    private final String status;
    private final String message;
    /** Chỉ khi profile dev — token xác thực email. */
    private final String devVerificationToken;
    private final String devVerificationUrl;
}
