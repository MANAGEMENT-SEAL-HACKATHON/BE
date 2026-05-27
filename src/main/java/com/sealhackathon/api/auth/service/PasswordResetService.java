package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.auth.dto.request.ResetPasswordRequest;
import com.sealhackathon.api.auth.dto.response.ForgotPasswordResponse;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.config.FrontendUrls;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String GENERIC_MESSAGE =
            "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu sẽ được gửi.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final AppProperties appProperties;
    private final UserSessionService userSessionService;
    private final AuditService auditService;

    @Transactional
    public ForgotPasswordResponse requestReset(String email) {
        String normalized = normalizeEmail(email);
        Optional<User> userOpt = userRepository.findByEmail(normalized);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getStatus() == UserStatus.APPROVED && user.getPasswordHash() != null) {
                String token = jwtTokenService.createPasswordResetToken(user.getId());
                String resetUrl = FrontendUrls.resetPasswordUrl(appProperties, token);
                log.info("[Auth] Forgot password {} — reset token (dev/log): {}", normalized, token);
                log.info("[Auth] Forgot password {} — reset URL: {}", normalized, resetUrl);
                auditService.log(AuditAction.ACCOUNT_PASSWORD_RESET_REQUESTED, "users", user.getId(),
                        Map.of("email", normalized));

                ForgotPasswordResponse.ForgotPasswordResponseBuilder builder =
                        ForgotPasswordResponse.builder().message(GENERIC_MESSAGE);
                if (jwtProperties.isDevExposePasswordResetToken()) {
                    builder.devResetToken(token).devResetUrl(resetUrl);
                }
                return builder.build();
            }
        }

        return ForgotPasswordResponse.builder().message(GENERIC_MESSAGE).build();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        Integer userId = jwtTokenService.parsePasswordResetUserId(req.getToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID,
                        "User không tồn tại", HttpStatus.BAD_REQUEST));

        if (user.getPasswordHash() != null
                && passwordEncoder.matches(req.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException(ErrorCode.NEW_PASSWORD_SAME_AS_CURRENT,
                    "Mật khẩu mới phải khác mật khẩu hiện tại");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setMustChangePassword(false);
        user.setUpdatedAt(now);
        userRepository.save(user);

        userSessionService.revokeAllForUser(user.getId());

        auditService.log(AuditAction.ACCOUNT_PASSWORD_RESET, "users", user.getId(),
                Map.of("email", user.getEmail()));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
