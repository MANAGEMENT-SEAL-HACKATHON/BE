package com.sealhackathon.api.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private boolean enabled = true;
    private String secret;
    private String issuer = "seal-hackathon-api";
    private int accessTtlMinutes = 30;
    private int refreshTtlDays = 7;
    private int emailVerifyTtlHours = 24;
    /** Dev: trả verify token trong response register. */
    private boolean devExposeVerifyToken = false;
    private int passwordResetTtlHours = 1;
    /** Dev: trả reset URL trong response forgot-password. */
    private boolean devExposePasswordResetToken = false;
}
