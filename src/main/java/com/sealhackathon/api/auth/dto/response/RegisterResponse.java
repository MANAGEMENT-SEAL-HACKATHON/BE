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
}
