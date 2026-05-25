package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.auth.dto.request.RegisterRequest;
import com.sealhackathon.api.auth.dto.response.RegisterResponse;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
import com.sealhackathon.api.chapters.value_object.ChapterStatus;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final ChapterRepository chapterRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final AuditService auditService;

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(ErrorCode.ACCOUNT_DUPLICATE_EMAIL,
                    "Email đã được đăng ký: " + email);
        }

        UserType userType = req.getUserType();
        Chapter chapter = null;
        String institution = null;

        String studentCode = req.getStudentCode() != null ? req.getStudentCode().trim() : null;
        if (studentCode != null && !studentCode.isBlank()
                && userRepository.existsByStudentCode(studentCode)) {
            throw new ConflictException(ErrorCode.STUDENT_CODE_DUPLICATE,
                    "Mã sinh viên đã được sử dụng: " + studentCode,
                    Map.of("studentCode", studentCode));
        }

        if (userType == UserType.INTERNAL) {
            if (req.getStudentCode() == null || req.getStudentCode().isBlank()) {
                throw new BusinessRuleException(ErrorCode.STUDENT_CODE_REQUIRED,
                        "INTERNAL bắt buộc studentCode");
            }
            if (req.getChapterId() == null) {
                throw new BusinessRuleException(ErrorCode.INVALID_CHAPTER,
                        "INTERNAL bắt buộc chapterId");
            }
            chapter = chapterRepository.findById(req.getChapterId())
                    .orElseThrow(() -> new BusinessRuleException(ErrorCode.INVALID_CHAPTER,
                            "chapterId không tồn tại", Map.of("chapterId", req.getChapterId())));
            if (chapter.getStatus() != ChapterStatus.ACTIVE) {
                throw new BusinessRuleException(ErrorCode.INVALID_CHAPTER,
                        "Chapter không ACTIVE", Map.of("chapterId", req.getChapterId()));
            }
        } else if (userType == UserType.EXTERNAL) {
            if (req.getInstitution() == null || req.getInstitution().isBlank()) {
                throw new BusinessRuleException(ErrorCode.INSTITUTION_REQUIRED,
                        "EXTERNAL bắt buộc institution");
            }
            if (req.getStudentCode() == null || req.getStudentCode().isBlank()) {
                throw new BusinessRuleException(ErrorCode.STUDENT_CODE_REQUIRED,
                        "EXTERNAL bắt buộc studentCode");
            }
            institution = req.getInstitution().trim();
        } else {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "userType không hợp lệ");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .fullName(req.getFullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(UserRole.STUDENT)
                .userType(userType)
                .studentCode(studentCode)
                .chapter(chapter)
                .institution(institution)
                .status(UserStatus.PENDING)
                .isTempAccount(false)
                .isDeptHead(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        User saved = userRepository.save(user);

        auditService.log(AuditAction.ACCOUNT_REGISTER, "users", saved.getId(), Map.of(
                "email", email,
                "userType", userType.name(),
                "status", UserStatus.PENDING.name()));

        String verifyToken = jwtTokenService.createEmailVerifyToken(saved.getId());
        log.info("[Auth] Đăng ký {} — verify token (dev/log): {}", email, verifyToken);

        RegisterResponse.RegisterResponseBuilder builder = RegisterResponse.builder()
                .userId(saved.getId())
                .email(email)
                .status(UserStatus.PENDING.name())
                .message("Đăng ký thành công. Vui lòng xác thực email và chờ duyệt tài khoản.");

        if (jwtProperties.isDevExposeVerifyToken()) {
            builder.devVerifyToken(verifyToken)
                    .devVerifyUrl("/api/v1/auth/verify-email?token=" + verifyToken);
        }
        return builder.build();
    }

    @Transactional
    public void verifyEmail(String token) {
        Integer userId = jwtTokenService.parseEmailVerifyUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.EMAIL_VERIFY_TOKEN_INVALID,
                        "User không tồn tại", HttpStatus.BAD_REQUEST));
        if (user.getEmailVerifiedAt() != null) {
            return;
        }
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.log(AuditAction.ACCOUNT_EMAIL_VERIFIED, "users", userId,
                Map.of("email", user.getEmail()));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
