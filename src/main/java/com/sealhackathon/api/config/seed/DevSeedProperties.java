package com.sealhackathon.api.config.seed;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình seed dev — {@code application-dev.properties} / env.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.seed")
public class DevSeedProperties {

    /** Bật seed {@code seal-e2e-2026} (E2eWorkflowDataSeeder). */
    private boolean e2eEnabled = true;

    /**
     * Ép reset {@code seal-e2e-2026} về GĐ2 baseline dù đang GĐ3+.
     * Mặc định {@code false} — bảo vệ demo hội đồng A–Z.
     */
    private boolean e2eForceGd2Reset = false;
}
