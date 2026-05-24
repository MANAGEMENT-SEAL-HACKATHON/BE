package com.sealhackathon.api.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthTokenResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresInSeconds;
    /** true → FE chuyển sang trang đổi mật khẩu (judge khách lần đầu). */
    private final boolean mustChangePassword;
}
