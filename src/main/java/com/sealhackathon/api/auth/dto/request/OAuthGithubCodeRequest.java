package com.sealhackathon.api.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuthGithubCodeRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String redirectUri;

    private String existingAccountPassword;
}
