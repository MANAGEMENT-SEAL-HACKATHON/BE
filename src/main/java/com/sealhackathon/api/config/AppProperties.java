package com.sealhackathon.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình ứng dụng chung (URL FE cho email stub, v.v.).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * URL gốc frontend — dùng trong link mời tham gia / đăng ký.
     */
    private String frontendUrl = "https://seal-hackathon-fe.vercel.app";
}
