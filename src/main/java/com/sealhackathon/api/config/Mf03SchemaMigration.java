package com.sealhackathon.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @deprecated Dùng {@link Gd03V41SchemaMigration} (GD03 v4.1). Giữ class để không gãy import cũ.
 */
@Deprecated(since = "4.1", forRemoval = true)
@Slf4j
@Component
@Order(1)
public class Mf03SchemaMigration implements CommandLineRunner {

    @Override
    public void run(String... args) {
        log.debug("[Mf03SchemaMigration] No-op — superseded by Gd03V41SchemaMigration");
    }
}
