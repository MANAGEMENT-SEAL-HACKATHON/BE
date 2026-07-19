package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.auth.dto.request.RegisterRequest;
import com.sealhackathon.api.auth.dto.response.RegisterResponse;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.config.FrontendUrls;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final EmailVerificationService emailVerificationService;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final AppProperties appProperties;
    private final ChapterRepository chapterRepository;

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
        UserType userType = req.getUserType();
        if (userType == null || userType == UserType.UNSPECIFIED) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "userType phải là INTERNAL hoặc EXTERNAL");
        }
        String studentCode = requireStudentCode(req.getStudentCode());
        Chapter chapter = null;
        String institution = null;
        if (userType == UserType.INTERNAL) {
            if (req.getChapterId() == null) {
                throw new BusinessRuleException(ErrorCode.INVALID_CHAPTER, "Vui lòng chọn cơ sở");
            }
            chapter = chapterRepository.findById(req.getChapterId())
                    .orElseThrow(() -> new BusinessRuleException(
                            ErrorCode.INVALID_CHAPTER, "Cơ sở không hợp lệ"));
        } else {
            if (req.getInstitution() == null || req.getInstitution().isBlank()) {
                throw new BusinessRuleException(
                        ErrorCode.INSTITUTION_REQUIRED, "Vui lòng nhập trường / tổ chức");
            }
            institution = req.getInstitution().trim();
        }

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .fullName(defaultFullName(req.getFullName(), email))
                .email(email)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(UserRole.STUDENT)
                .userType(userType)
                .studentCode(studentCode)
                .chapter(chapter)
                .institution(institution)
                .studentCardImagePath(null)
                .status(UserStatus.PENDING)
                .isTempAccount(false)
                .isDeptHead(false)
                .emailVerifiedAt(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
        User saved = userRepository.save(user);

        auditService.log(AuditAction.ACCOUNT_REGISTER, "users", saved.getId(), Map.of(
                "email", email,
                "userType", userType.name(),
                "status", UserStatus.PENDING.name()));

        emailVerificationService.sendVerificationEmail(saved);

        RegisterResponse.RegisterResponseBuilder builder = RegisterResponse.builder()
                .userId(saved.getId())
                .email(email)
                .status(UserStatus.PENDING.name())
                .message("Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản trước khi đăng nhập.");

        if (jwtProperties.isDevExposeEmailVerificationToken()) {
            String token = jwtTokenService.createEmailVerificationToken(saved.getId());
            builder.devVerificationToken(token)
                    .devVerificationUrl(FrontendUrls.verifyEmailUrl(appProperties, token));
        }

        return builder.build();
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

    private static String requireStudentCode(String studentCode) {
        if (studentCode == null || studentCode.isBlank()) {
            throw new BusinessRuleException(
                    ErrorCode.STUDENT_CODE_REQUIRED, "Vui lòng nhập mã sinh viên");
        }
        return studentCode.trim();
    }
}
