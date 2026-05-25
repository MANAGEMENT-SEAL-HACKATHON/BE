package com.sealhackathon.api.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Nạp file {@code .env} ở thư mục gốc project (cùng cấp {@code pom.xml}) vào {@link System#setProperty}
 * trước khi Spring Boot khởi động — map alias quen thuộc (JWT_SECRET_KEY, JWT_EXPIRATION_MS, DB_*).
 */
public final class DotenvEnvironment {

    private DotenvEnvironment() {
    }

    public static void load() {
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir"))
                .filename(".env")
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));

        mapIfPresent(dotenv, "JWT_SECRET_KEY", "security.jwt.secret");
        mapIfPresent(dotenv, "SECURITY_JWT_SECRET", "security.jwt.secret");

        String expirationMs = firstNonBlank(dotenv, "JWT_EXPIRATION_MS");
        if (expirationMs != null) {
            long ms = Long.parseLong(expirationMs.trim());
            long minutes = Math.max(1, ms / 60_000L);
            System.setProperty("security.jwt.access-ttl-minutes", String.valueOf(minutes));
        }
        mapIfPresent(dotenv, "SECURITY_JWT_ACCESS_TTL_MINUTES", "security.jwt.access-ttl-minutes");

        mapIfPresent(dotenv, "DB_PASS", "spring.datasource.password");
        mapIfPresent(dotenv, "SPRING_DATASOURCE_PASSWORD", "spring.datasource.password");
        mapIfPresent(dotenv, "DB_USER", "spring.datasource.username");
        mapIfPresent(dotenv, "SPRING_DATASOURCE_USERNAME", "spring.datasource.username");
        mapIfPresent(dotenv, "DB_HOST", "db.host");
        mapIfPresent(dotenv, "SECURITY_JWT_ENABLED", "security.jwt.enabled");
        mapIfPresent(dotenv, "APP_FRONTEND_URL", "app.frontend-url");
    }

    private static void mapIfPresent(Dotenv dotenv, String envKey, String systemPropertyKey) {
        String value = dotenv.get(envKey);
        if (value != null && !value.isBlank()) {
            System.setProperty(systemPropertyKey, value.trim());
        }
    }

    private static String firstNonBlank(Dotenv dotenv, String key) {
        String value = dotenv.get(key);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
