package com.sealhackathon.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình hackathon (prefix {@code app.hackathon}).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.hackathon")
public class HackathonProperties {

    /** Số lần tối đa được dời hạn đăng ký (mặc định 2). */
    private int maxRegistrationExtensions = 2;
}
