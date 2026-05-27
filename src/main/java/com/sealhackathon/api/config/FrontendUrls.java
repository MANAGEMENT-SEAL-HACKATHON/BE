package com.sealhackathon.api.config;

/**
 * URL frontend dùng trong email (judge khách, v.v.).
 */
public final class FrontendUrls {

    private FrontendUrls() {
    }

    public static String loginUrl(AppProperties appProperties) {
        return trimTrailingSlash(appProperties.getFrontendUrl()) + "/login";
    }

    public static String resetPasswordUrl(AppProperties appProperties, String token) {
        return trimTrailingSlash(appProperties.getFrontendUrl()) + "/reset-password?token=" + token;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5173";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
