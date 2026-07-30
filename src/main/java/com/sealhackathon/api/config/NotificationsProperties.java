package com.sealhackathon.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình fan-out thông báo (prefix {@code app.notifications}).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.notifications")
public class NotificationsProperties {

    /**
     * Khoảng cách tối thiểu giữa hai lần gửi email broadcast cùng hackathon (giây).
     * In-app vẫn gửi bình thường trong cửa sổ này.
     */
    private int broadcastDedupeSeconds = 60;
}
