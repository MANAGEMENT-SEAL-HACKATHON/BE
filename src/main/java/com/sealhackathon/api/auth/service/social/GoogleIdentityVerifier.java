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
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GoogleIdentityVerifier {

    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final OAuthProperties oAuthProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public SocialIdentity verifyIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw invalidToken("Google idToken rỗng");
        }
        try {
            String encoded = URLEncoder.encode(idToken, StandardCharsets.UTF_8);
            URI uri = URI.create(oAuthProperties.getGoogleTokenInfoUrl() + "?id_token=" + encoded);
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw invalidToken("Google tokeninfo trả về status " + response.statusCode());
            }

            JsonNode node = objectMapper.readTree(response.body());
            String issuer = node.path("iss").asText("");
            String audience = node.path("aud").asText("");
            String providerUid = node.path("sub").asText("");
            String email = node.path("email").asText("").trim().toLowerCase();
            boolean emailVerified = node.path("email_verified").asBoolean(false);
            String name = node.path("name").asText(email);

            if (providerUid.isBlank() || email.isBlank() || !emailVerified || !ISSUERS.contains(issuer)) {
                throw invalidToken("Google token thiếu claim bắt buộc");
            }
            String configuredAud = oAuthProperties.getGoogleClientId();
            if (configuredAud != null && !configuredAud.isBlank() && !configuredAud.equals(audience)) {
                throw invalidToken("Google token không đúng client id");
            }

            return new SocialIdentity("GOOGLE", providerUid, email, name);
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException(ErrorCode.OAUTH_TOKEN_INVALID,
                    "Google token không hợp lệ",
                    HttpStatus.BAD_REQUEST,
                    Map.of("reason", ex.getMessage()));
        }
    }

    private static AuthException invalidToken(String reason) {
        return new AuthException(
                ErrorCode.OAUTH_TOKEN_INVALID,
                "Google token không hợp lệ",
                HttpStatus.BAD_REQUEST,
                Map.of("reason", reason));
    }
}
