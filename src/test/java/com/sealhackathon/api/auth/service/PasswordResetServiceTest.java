package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.auth.dto.request.ResetPasswordRequest;
import com.sealhackathon.api.auth.dto.response.ForgotPasswordResponse;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private JwtProperties jwtProperties;
    @Mock private AppProperties appProperties;
    @Mock private UserSessionService userSessionService;
    @Mock private AuditService auditService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void requestReset_unknownEmail_returnsGenericMessage() {
        when(userRepository.findByEmail("nobody@fpt.edu.vn")).thenReturn(Optional.empty());

        ForgotPasswordResponse response = passwordResetService.requestReset("nobody@fpt.edu.vn");

        assertThat(response.getMessage()).contains("Nếu email tồn tại");
        assertThat(response.getDevResetToken()).isNull();
    }

    @Test
    void requestReset_approvedUser_logsAndOptionalDevFields() {
        User user = User.builder()
                .id(1)
                .email("coord@fpt.edu.vn")
                .passwordHash("hash")
                .status(UserStatus.APPROVED)
                .build();
        when(userRepository.findByEmail("coord@fpt.edu.vn")).thenReturn(Optional.of(user));
        when(jwtTokenService.createPasswordResetToken(1)).thenReturn("reset-jwt");
        when(appProperties.getFrontendUrl()).thenReturn("http://localhost:5173");
        when(jwtProperties.isDevExposePasswordResetToken()).thenReturn(true);

        ForgotPasswordResponse response = passwordResetService.requestReset("coord@fpt.edu.vn");

        assertThat(response.getDevResetToken()).isEqualTo("reset-jwt");
        assertThat(response.getDevResetUrl()).contains("reset-password?token=reset-jwt");
    }

    @Test
    void resetPassword_validToken_revokesAllSessions() {
        User user = User.builder()
                .id(5)
                .email("user@fpt.edu.vn")
                .passwordHash("old-hash")
                .build();
        when(jwtTokenService.parsePasswordResetUserId("token")).thenReturn(5);
        when(userRepository.findById(5)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("newPass123", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("newPass123")).thenReturn("new-hash");

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("token");
        req.setNewPassword("newPass123");

        passwordResetService.resetPassword(req);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getMustChangePassword()).isFalse();
        verify(userSessionService).revokeAllForUser(5);
    }

    @Test
    void resetPassword_invalidToken_throws() {
        when(jwtTokenService.parsePasswordResetUserId("bad"))
                .thenThrow(new AuthException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID,
                        "invalid", org.springframework.http.HttpStatus.BAD_REQUEST));

        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("bad");
        req.setNewPassword("newPass123");

        assertThatThrownBy(() -> passwordResetService.resetPassword(req))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getCode())
                .isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
    }
}
