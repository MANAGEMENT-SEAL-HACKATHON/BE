package com.sealhackathon.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình GitHub API (prefix {@code app.github}) — PAT server-side, read-only.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.github")
public class GitHubProperties {

    /** Bật fetch metadata/commits qua GitHub API. */
    private boolean enabled = false;

    private String apiBaseUrl = "https://api.github.com";

    /** Fine-grained / classic PAT (read-only). */
    private String token = "";

    /** Số commit tối đa mỗi lần fetch. */
    private int commitLimit = 30;

    /** TTL cache response GitHub (giây). */
    private int cacheTtlSeconds = 300;
}
