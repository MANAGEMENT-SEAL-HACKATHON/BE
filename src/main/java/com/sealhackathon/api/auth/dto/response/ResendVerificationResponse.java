package com.sealhackathon.api.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResendVerificationResponse {

    private final String message;
}
