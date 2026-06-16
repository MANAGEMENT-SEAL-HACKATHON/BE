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

    /**
     * Bật seed hackathon {@link Gd5SeedConstants#SLUG_GD5_FINAL_ACTIVE}. Mặc định {@code true}.
     */
    private boolean gd5Enabled = true;

    /**
     * Bật seed hackathon {@link Gd6SeedConstants#SLUG_GD6_PENDING_CONFIRM}. Mặc định {@code true}.
     */
    private boolean gd6Enabled = true;

    /** Bật seed {@link Gd3SeedConstants#SLUG_GD3_PRELIM_OPEN}. */
    private boolean gd3Enabled = true;

    /** Bật seed {@link Gd4SeedConstants#SLUG_GD4_ADVANCE_READY}. */
    private boolean gd4Enabled = true;
}
