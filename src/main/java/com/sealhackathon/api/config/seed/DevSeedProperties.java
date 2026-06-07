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
     * Bật {@link Gd4TestDataSeeder} (tiebreak/wildcard trên {@link GdExtendedSeedConstants#SLUG_GD4_TIEBREAK}).
     * Mặc định {@code false} để không ghi đè {@link Gd1SeedConstants#SLUG_ONGOING}.
     */
    private boolean gd4Enabled = false;
}
