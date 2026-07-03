package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.auth.dto.request.ResendVerificationRequest;
import com.sealhackathon.api.auth.dto.response.ResendVerificationResponse;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.config.FrontendUrls;
import com.sealhackathon.api.invitations.service.EmailService;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String GENERIC_RESEND_MESSAGE =
            "Nếu email tồn tại và chưa xác thực, chúng tôi đã gửi lại link xác thực.";

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final AppProperties appProperties;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional
    public void verify(String token) {
        Integer userId = jwtTokenService.parseEmailVerificationUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID,
                        "Tài khoản không tồn tại", HttpStatus.BAD_REQUEST));

        if (user.getEmailVerifiedAt() != null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        user.setEmailVerifiedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        auditService.log(AuditAction.ACCOUNT_EMAIL_VERIFIED, "users", user.getId(),
                Map.of("email", user.getEmail()));
    }

    @Transactional
    public ResendVerificationResponse resend(ResendVerificationRequest req) {
        String normalized = normalizeEmail(req.getEmail());
        Optional<User> userOpt = userRepository.findByEmail(normalized);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getRole() == UserRole.STUDENT
                    && user.getEmailVerifiedAt() == null
                    && user.getPasswordHash() != null
                    && !Boolean.TRUE.equals(user.getIsTempAccount())) {
                sendVerificationEmail(user);
                auditService.log(AuditAction.ACCOUNT_EMAIL_VERIFICATION_RESENT, "users", user.getId(),
                        Map.of("email", normalized));
            }
        }

        return ResendVerificationResponse.builder().message(GENERIC_RESEND_MESSAGE).build();
    }

    public void sendVerificationEmail(User user) {
        String token = jwtTokenService.createEmailVerificationToken(user.getId());
        String verifyUrl = FrontendUrls.verifyEmailUrl(appProperties, token);
        LocalDateTime expiresAt =
                LocalDateTime.now().plusHours(jwtProperties.getEmailVerificationTtlHours());
        log.info("[Auth] Email verification {} — token (dev/log): {}", user.getEmail(), token);
        log.info("[Auth] Email verification {} — URL: {}", user.getEmail(), verifyUrl);
        try {
            emailService.sendEmailVerification(user.getEmail(), user.getFullName(), verifyUrl, expiresAt);
        } catch (RuntimeException ex) {
            log.warn("[Auth] Verification email failed for {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
