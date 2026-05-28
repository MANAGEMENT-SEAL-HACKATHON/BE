package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_PASSWORD_RESET = "password_reset";

    private final JwtProperties jwtProperties;

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.getAccessTtlMinutes() * 60L);
        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("status", user.getStatus().name())
                .claim("userType", user.getUserType() != null ? user.getUserType().name() : UserType.UNSPECIFIED.name())
                .claim("isTempAccount", Boolean.TRUE.equals(user.getIsTempAccount()))
                .signWith(signingKey())
                .compact();
    }

    public CurrentUserStub parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        String typ = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!TYPE_ACCESS.equals(typ)) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Token không hợp lệ",
                    HttpStatus.UNAUTHORIZED);
        }
        return toPrincipal(claims);
    }

    public String createPasswordResetToken(Integer userId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.getPasswordResetTtlHours() * 3600L);
        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim(CLAIM_TOKEN_TYPE, TYPE_PASSWORD_RESET)
                .signWith(signingKey())
                .compact();
    }

    public Integer parsePasswordResetUserId(String token) {
        Claims claims = parseClaimsForPasswordReset(token);
        if (!TYPE_PASSWORD_RESET.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new AuthException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID,
                    "Link đặt lại mật khẩu không hợp lệ", HttpStatus.BAD_REQUEST);
        }
        return Integer.parseInt(claims.getSubject());
    }

    private Claims parseClaimsForPasswordReset(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(jwtProperties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AuthException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID,
                    "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn",
                    HttpStatus.BAD_REQUEST, Map.of("reason", ex.getMessage()));
        }
    }

    private CurrentUserStub toPrincipal(Claims claims) {
        String userTypeClaim = claims.get("userType", String.class);
        return CurrentUserStub.builder()
                .userId(Integer.parseInt(claims.getSubject()))
                .email(claims.get("email", String.class))
                .role(UserRole.valueOf(claims.get("role", String.class)))
                .status(UserStatus.valueOf(claims.get("status", String.class)))
                .userType(userTypeClaim != null ? UserType.valueOf(userTypeClaim) : UserType.UNSPECIFIED)
                .isTempAccount(Boolean.TRUE.equals(claims.get("isTempAccount", Boolean.class)))
                .build();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey())
                    .requireIssuer(jwtProperties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn",
                    HttpStatus.UNAUTHORIZED, Map.of("reason", ex.getMessage()));
        }
    }

    private SecretKey signingKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("security.jwt.secret phải >= 32 bytes cho HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
