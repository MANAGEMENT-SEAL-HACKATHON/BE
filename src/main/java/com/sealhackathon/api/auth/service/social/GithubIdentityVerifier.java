package com.sealhackathon.api.auth.service.social;

import com.sealhackathon.api.auth.config.OAuthProperties;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GithubIdentityVerifier {

    private final OAuthProperties oAuthProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public SocialIdentity verifyAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw invalidToken("GitHub access token rỗng");
        }
        try {
            JsonNode user = getJson("/user", accessToken);
            String providerUid = user.path("id").asText("");
            String email = user.path("email").asText("");
            if (email == null || email.isBlank()) {
                email = resolvePrimaryVerifiedEmail(accessToken);
            }
            email = email.trim().toLowerCase();
            String login = user.path("login").asText("");
            String name = user.path("name").asText(login.isBlank() ? email : login);
            if (providerUid.isBlank() || email.isBlank()) {
                throw invalidToken("GitHub account không có id/email hợp lệ");
            }
            return new SocialIdentity("GITHUB", providerUid, email, name);
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException(ErrorCode.OAUTH_TOKEN_INVALID,
                    "GitHub token không hợp lệ",
                    HttpStatus.BAD_REQUEST,
                    Map.of("reason", ex.getMessage()));
        }
    }

    public SocialIdentity verifyCode(String code, String redirectUri) {
        return verifyAccessToken(exchangeCodeForAccessToken(code, redirectUri));
    }

    public String exchangeCodeForAccessToken(String code, String redirectUri) {
        if (code == null || code.isBlank()) {
            throw invalidToken("GitHub oauth code rỗng");
        }
        String clientId = oAuthProperties.getGithubClientId();
        String clientSecret = oAuthProperties.getGithubClientSecret();
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new AuthException(
                    ErrorCode.OAUTH_TOKEN_INVALID,
                    "Thiếu cấu hình GitHub OAuth clientId/clientSecret",
                    HttpStatus.BAD_REQUEST);
        }
        try {
            String form = "client_id=" + encode(clientId)
                    + "&client_secret=" + encode(clientSecret)
                    + "&code=" + encode(code)
                    + "&redirect_uri=" + encode(redirectUri);
            HttpRequest request = HttpRequest.newBuilder(URI.create(oAuthProperties.getGithubTokenUrl()))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw invalidToken("GitHub token exchange trả về status " + response.statusCode());
            }
            JsonNode node = objectMapper.readTree(response.body());
            String token = node.path("access_token").asText("");
            if (token.isBlank()) {
                throw invalidToken("Không lấy được access_token từ GitHub");
            }
            return token;
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException(
                    ErrorCode.OAUTH_TOKEN_INVALID,
                    "Không thể đổi GitHub code sang access token",
                    HttpStatus.BAD_REQUEST,
                    Map.of("reason", ex.getMessage()));
        }
    }

    private String resolvePrimaryVerifiedEmail(String accessToken) throws Exception {
        JsonNode emails = getJson("/user/emails", accessToken);
        if (!emails.isArray()) {
            return "";
        }
        for (JsonNode emailNode : emails) {
            boolean primary = emailNode.path("primary").asBoolean(false);
            boolean verified = emailNode.path("verified").asBoolean(false);
            String email = emailNode.path("email").asText("");
            if (primary && verified && !email.isBlank()) {
                return email;
            }
        }
        for (JsonNode emailNode : emails) {
            boolean verified = emailNode.path("verified").asBoolean(false);
            String email = emailNode.path("email").asText("");
            if (verified && !email.isBlank()) {
                return email;
            }
        }
        throw new AuthException(ErrorCode.OAUTH_EMAIL_NOT_VERIFIED,
                "GitHub account chưa có email verified",
                HttpStatus.BAD_REQUEST);
    }

    private JsonNode getJson(String path, String accessToken) throws Exception {
        String normalizedToken = accessToken == null ? "" : accessToken.trim();
        URI uri = URI.create(oAuthProperties.getGithubApiBaseUrl() + path);
        HttpRequest request = HttpRequest.newBuilder(uri)
                // GitHub OAuth app token works reliably with "token" scheme.
                .header("Authorization", "token " + normalizedToken)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "seal-hackathon-be")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw invalidToken("GitHub API trả về status " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private static AuthException invalidToken(String reason) {
        return new AuthException(
                ErrorCode.OAUTH_TOKEN_INVALID,
                "GitHub token không hợp lệ",
                HttpStatus.BAD_REQUEST,
                Map.of("reason", reason));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
