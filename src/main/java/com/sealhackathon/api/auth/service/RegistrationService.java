package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.dto.request.RegisterRequest;
import com.sealhackathon.api.auth.dto.response.RegisterResponse;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(ErrorCode.ACCOUNT_DUPLICATE_EMAIL,
                    "Email đã được đăng ký: " + email);
        }
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessRuleException(
                    ErrorCode.VALIDATION_FAILED,
                    "Mật khẩu nhập lại không khớp");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .fullName(defaultFullName(req.getFullName(), email))
                .email(email)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(UserRole.STUDENT)
                .userType(UserType.UNSPECIFIED)
                .studentCode(null)
                .chapter(null)
                .institution(null)
                .studentCardImagePath(null)
                .status(UserStatus.PENDING)
                .isTempAccount(false)
                .isDeptHead(false)
                .emailVerifiedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        User saved = userRepository.save(user);

        auditService.log(AuditAction.ACCOUNT_REGISTER, "users", saved.getId(), Map.of(
                "email", email,
                "userType", UserType.UNSPECIFIED.name(),
                "status", UserStatus.PENDING.name()));

        return RegisterResponse.builder()
                .userId(saved.getId())
                .email(email)
                .status(UserStatus.PENDING.name())
                .message("Đăng ký thành công. Vui lòng hoàn thiện hồ sơ.")
                .build();
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String defaultFullName(String fullName, String email) {
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
