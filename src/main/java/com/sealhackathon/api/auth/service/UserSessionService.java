package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.user_sessions.entity.UserSession;
import com.sealhackathon.api.user_sessions.repository.UserSessionRepository;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserSessionRepository userSessionRepository;
    private final JwtProperties jwtProperties;

    public record RefreshTokenPair(String rawToken, UserSession session) {}

    @Transactional
    public RefreshTokenPair createSession(User user, String ipAddress, String userAgent) {
        String raw = generateRawRefreshToken();
        UserSession session = UserSession.builder()
                .user(user)
                .tokenHash(hashToken(raw))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(LocalDateTime.now().plusDays(jwtProperties.getRefreshTtlDays()))
                .build();
        return new RefreshTokenPair(raw, userSessionRepository.save(session));
    }

    @Transactional
    public RefreshTokenPair rotateRefreshToken(String rawToken, String ipAddress, String userAgent) {
        UserSession active = resolveActiveSession(rawToken);
        User user = active.getUser();
        active.setRevokedAt(LocalDateTime.now());
        userSessionRepository.save(active);
        return createSession(user, ipAddress, userAgent);
    }

    @Transactional
    public User validateRefreshToken(String rawToken) {
        return resolveActiveSession(rawToken).getUser();
    }

    @Transactional
    public void revokeByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String hash = hashToken(rawToken);
        userSessionRepository.findByTokenHashAndRevokedAtIsNull(hash).ifPresent(session -> {
            session.setRevokedAt(LocalDateTime.now());
            userSessionRepository.save(session);
        });
    }

    @Transactional
    public void revokeAllForUser(Integer userId) {
        if (userId == null) {
            return;
        }
        userSessionRepository.revokeAllActiveByUserId(userId, LocalDateTime.now());
    }

    private UserSession resolveActiveSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw refreshInvalid();
        }
        String hash = hashToken(rawToken);
        return userSessionRepository.findByTokenHashAndRevokedAtIsNull(hash)
                .map(session -> {
                    if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
                        throw refreshInvalid();
                    }
                    return session;
                })
                .orElseGet(() -> handleMissingOrReusedToken(hash));
    }

    private UserSession handleMissingOrReusedToken(String hash) {
        userSessionRepository.findByTokenHash(hash).ifPresent(revoked -> {
            if (revoked.getRevokedAt() != null && revoked.getUser() != null) {
                revokeAllForUser(revoked.getUser().getId());
            }
        });
        throw refreshInvalid();
    }

    private static AuthException refreshInvalid() {
        return new AuthException(ErrorCode.REFRESH_TOKEN_INVALID,
                "Refresh token không hợp lệ", HttpStatus.UNAUTHORIZED);
    }

    public static String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String generateRawRefreshToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
