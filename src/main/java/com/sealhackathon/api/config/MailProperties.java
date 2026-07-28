package com.sealhackathon.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình gửi mail hệ thống (prefix {@code app.mail}).
 *
 * <p>{@code enabled=false} (mặc định) → NoOpEmailServiceImpl chỉ log. Bật {@code true} kèm
 * {@code spring.mail.*} (host/port/username/password) để gửi SMTP thật.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    /** Bật gửi SMTP thật. */
    private boolean enabled = false;

    /** Địa chỉ người gửi (From). */
    private String from = "no-reply@sealhackathon.local";

    /** Tên hiển thị người gửi. */
    private String fromName = "SEAL Hackathon";
}
