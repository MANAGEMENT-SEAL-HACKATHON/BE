package com.sealhackathon.api.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuthGithubRequest {

    @NotBlank
    private String accessToken;

    /**
     * Optional: dùng khi backend bật requirePasswordForAutoLink=true.
     */
    private String existingAccountPassword;
}
