package com.sealhackathon.api.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthLinkStatusResponse {

    private final String provider;
    private final String email;
    private final boolean linked;
    private final String message;
}
