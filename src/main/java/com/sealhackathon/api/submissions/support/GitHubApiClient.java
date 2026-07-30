package com.sealhackathon.api.submissions.support;

import com.sealhackathon.api.config.GitHubProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only GitHub REST client (server PAT).
 * Handles 404/403/rate-limit without throwing hard errors to callers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubApiClient {

    public enum Status {
        OK,
        NOT_FOUND,
        FORBIDDEN,
        RATE_LIMITED,
        DISABLED,
        ERROR
    }

    public record RepoInfo(
            String name,
            String fullName,
            String description,
            String language,
            String htmlUrl,
            Integer stars,
            LocalDateTime pushedAt
    ) {
    }

    public record CommitInfo(
            String sha,
            String message,
            String authorName,
            String authorAvatarUrl,
            Instant date,
            String htmlUrl
    ) {
    }

    public record ApiResult<T>(Status status, T body, String message) {
        public static <T> ApiResult<T> ok(T body) {
            return new ApiResult<>(Status.OK, body, null);
        }

        public static <T> ApiResult<T> of(Status status, String message) {
            return new ApiResult<>(status, null, message);
        }
    }

    private final GitHubProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public boolean isConfigured() {
        return properties.isEnabled() && StringUtils.hasText(properties.getToken());
    }

    public ApiResult<RepoInfo> getRepo(String owner, String repo) {
        if (!properties.isEnabled()) {
            return ApiResult.of(Status.DISABLED, "GitHub integration disabled");
        }
        if (!StringUtils.hasText(properties.getToken())) {
            return ApiResult.of(Status.DISABLED, "Missing GitHub API token");
        }
        try {
            HttpResponse<String> response = send("GET", "/repos/" + owner + "/" + repo);
            return mapRepo(response);
        } catch (Exception ex) {
            log.warn("[GitHub] getRepo {}/{} failed: {}", owner, repo, ex.getMessage());
            return ApiResult.of(Status.ERROR, ex.getMessage());
        }
    }

    public ApiResult<List<CommitInfo>> getCommits(String owner, String repo, int limit) {
        if (!properties.isEnabled()) {
            return ApiResult.of(Status.DISABLED, "GitHub integration disabled");
        }
        if (!StringUtils.hasText(properties.getToken())) {
            return ApiResult.of(Status.DISABLED, "Missing GitHub API token");
        }
        int perPage = Math.max(1, Math.min(limit, 100));
        try {
            HttpResponse<String> response = send(
                    "GET",
                    "/repos/" + owner + "/" + repo + "/commits?per_page=" + perPage);
            return mapCommits(response);
        } catch (Exception ex) {
            log.warn("[GitHub] getCommits {}/{} failed: {}", owner, repo, ex.getMessage());
            return ApiResult.of(Status.ERROR, ex.getMessage());
        }
    }

    private HttpResponse<String> send(String method, String path) throws Exception {
        String base = properties.getApiBaseUrl() == null
                ? "https://api.github.com"
                : properties.getApiBaseUrl().replaceAll("/$", "");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "SealHackathon-BE")
                .header("Authorization", "Bearer " + properties.getToken().trim());
        if ("GET".equalsIgnoreCase(method)) {
            builder.GET();
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private ApiResult<RepoInfo> mapRepo(HttpResponse<String> response) {
        Status status = resolveStatus(response);
        if (status != Status.OK) {
            return ApiResult.of(status, "HTTP " + response.statusCode());
        }
        try {
            JsonNode node = objectMapper.readTree(response.body());
            RepoInfo info = new RepoInfo(
                    text(node, "name"),
                    text(node, "full_name"),
                    textOrNull(node, "description"),
                    textOrNull(node, "language"),
                    text(node, "html_url"),
                    node.path("stargazers_count").asInt(0),
                    parseLocalDateTime(textOrNull(node, "pushed_at"))
            );
            return ApiResult.ok(info);
        } catch (Exception ex) {
            return ApiResult.of(Status.ERROR, ex.getMessage());
        }
    }

    private ApiResult<List<CommitInfo>> mapCommits(HttpResponse<String> response) {
        Status status = resolveStatus(response);
        if (status != Status.OK) {
            return ApiResult.of(status, "HTTP " + response.statusCode());
        }
        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray()) {
                return ApiResult.of(Status.ERROR, "Unexpected commits payload");
            }
            List<CommitInfo> commits = new ArrayList<>();
            for (JsonNode item : root) {
                JsonNode commit = item.path("commit");
                JsonNode authorUser = item.path("author");
                String authorName = textOrNull(commit.path("author"), "name");
                if (authorName == null || authorName.isBlank()) {
                    authorName = textOrNull(authorUser, "login");
                }
                String avatar = textOrNull(authorUser, "avatar_url");
                String message = text(commit, "message");
                // Keep first line only for timeline
                int nl = message.indexOf('\n');
                if (nl > 0) {
                    message = message.substring(0, nl).trim();
                }
                commits.add(new CommitInfo(
                        text(item, "sha"),
                        message,
                        authorName,
                        avatar,
                        parseInstant(textOrNull(commit.path("author"), "date")),
                        text(item, "html_url")
                ));
            }
            return ApiResult.ok(commits);
        } catch (Exception ex) {
            return ApiResult.of(Status.ERROR, ex.getMessage());
        }
    }

    private static Status resolveStatus(HttpResponse<String> response) {
        int code = response.statusCode();
        if (code == 200) {
            return Status.OK;
        }
        if (code == 404) {
            return Status.NOT_FOUND;
        }
        if (code == 429) {
            return Status.RATE_LIMITED;
        }
        if (code == 403 || code == 401) {
            String remaining = response.headers().firstValue("X-RateLimit-Remaining").orElse(null);
            if ("0".equals(remaining)) {
                return Status.RATE_LIMITED;
            }
            String body = response.body() == null ? "" : response.body().toLowerCase();
            if (body.contains("rate limit")) {
                return Status.RATE_LIMITED;
            }
            return Status.FORBIDDEN;
        }
        return Status.ERROR;
    }

    private static String text(JsonNode node, String field) {
        String v = node.path(field).asText("");
        return v == null ? "" : v;
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String v = node.path(field).asText(null);
        return v == null || v.isBlank() ? null : v;
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(iso);
        } catch (Exception ex) {
            return null;
        }
    }

    private static LocalDateTime parseLocalDateTime(String iso) {
        Instant instant = parseInstant(iso);
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
