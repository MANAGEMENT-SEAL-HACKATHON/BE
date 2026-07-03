package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.users.entity.UserSession;
import com.sealhackathon.api.users.repository.UserSessionRepository;
import com.sealhackathon.api.users.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceTest {

    @Mock private UserSessionRepository userSessionRepository;
    @Mock private JwtProperties jwtProperties;

    @InjectMocks
    private UserSessionService userSessionService;

    @Test
    void rotateRefreshToken_revokesOldAndCreatesNew() {
        String raw = "raw-refresh-token";
        String hash = UserSessionService.hashToken(raw);
        User user = User.builder().id(7).build();
        UserSession active = UserSession.builder()
                .id(1)
                .user(user)
                .tokenHash(hash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(userSessionRepository.findByTokenHashAndRevokedAtIsNull(hash))
                .thenReturn(Optional.of(active));
        when(jwtProperties.getRefreshTtlDays()).thenReturn(14);
        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSessionService.RefreshTokenPair rotated = userSessionService.rotateRefreshToken(
                raw, "127.0.0.1", "TestAgent");

        assertThat(active.getRevokedAt()).isNotNull();
        assertThat(rotated.rawToken()).isNotBlank();
        assertThat(rotated.rawToken()).isNotEqualTo(raw);
        ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(s -> s == active && s.getRevokedAt() != null);
        UserSession inserted = captor.getAllValues().stream()
                .filter(s -> s != active)
                .findFirst()
                .orElseThrow();
        assertThat(inserted.getUser()).isEqualTo(user);
        assertThat(inserted.getTokenHash()).isEqualTo(UserSessionService.hashToken(rotated.rawToken()));
    }

    @Test
    void revokeAllForUser_callsRepository() {
        userSessionService.revokeAllForUser(42);
        verify(userSessionRepository).revokeAllActiveByUserId(eq(42), any(LocalDateTime.class));
    }

    @Test
    void validateRefreshToken_reusedRevokedToken_revokesAllSessions() {
        String raw = "reused-token";
        String hash = UserSessionService.hashToken(raw);
        User user = User.builder().id(3).build();
        UserSession revoked = UserSession.builder()
                .user(user)
                .tokenHash(hash)
                .revokedAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(userSessionRepository.findByTokenHashAndRevokedAtIsNull(hash))
                .thenReturn(Optional.empty());
        when(userSessionRepository.findByTokenHash(hash)).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> userSessionService.validateRefreshToken(raw))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getCode())
                .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);

        verify(userSessionRepository).revokeAllActiveByUserId(eq(3), any(LocalDateTime.class));
    }

    @Test
    void revokeAllForUser_nullUserId_noOp() {
        userSessionService.revokeAllForUser(null);
        verify(userSessionRepository, never()).revokeAllActiveByUserId(any(), any());
    }
}
