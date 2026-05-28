package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.dto.request.ChangePasswordRequest;
import com.sealhackathon.api.auth.dto.request.LoginRequest;
import com.sealhackathon.api.auth.dto.response.AuthTokenResponse;
import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.invitations.entity.Invitation;
import com.sealhackathon.api.invitations.repository.InvitationRepository;
import com.sealhackathon.api.invitations.service.GuestJudgeLifecycleService;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final UserSessionService userSessionService;
    private final JwtProperties jwtProperties;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;
    private final GuestJudgeLifecycleService guestJudgeLifecycleService;

    @Transactional
    public AuthTokenResponse login(LoginRequest req, HttpServletRequest httpRequest) {
        String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> invalidCredentials());

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        assertApproved(user);
        assertGuestJudgeInvitationValid(user);
        guestJudgeLifecycleService.assertHackathonNotEndedForTempJudge(user);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String access = jwtTokenService.createAccessToken(user);
        UserSessionService.RefreshTokenPair refresh = userSessionService.createSession(
                user,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));

        auditService.log(AuditAction.ACCOUNT_LOGIN, "users", user.getId(),
                Map.of("email", email));

        return AuthTokenResponse.builder()
                .accessToken(access)
                .refreshToken(refresh.rawToken())
                .tokenType("Bearer")
                .expiresInSeconds(jwtProperties.getAccessTtlMinutes() * 60L)
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        Integer userId = currentUserAccessor.currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED,
                        "User không tồn tại", HttpStatus.UNAUTHORIZED));

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new AuthException(ErrorCode.PASSWORD_MISMATCH,
                    "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST);
        }

        if (passwordEncoder.matches(req.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException(ErrorCode.NEW_PASSWORD_SAME_AS_CURRENT,
                    "Mật khẩu mới phải khác mật khẩu hiện tại");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setMustChangePassword(false);
        user.setUpdatedAt(now);
        userRepository.save(user);

        invitationRepository
                .findFirstByEmailAndRoleAndAcceptedAtIsNullOrderByCreatedAtDesc(
                        user.getEmail(), UserRole.JUDGE)
                .ifPresent(inv -> {
                    inv.setAcceptedAt(now);
                    invitationRepository.save(inv);
                });

        userSessionService.revokeAllForUser(userId);

        auditService.log(AuditAction.ACCOUNT_PASSWORD_CHANGED, "users", user.getId(),
                Map.of("email", user.getEmail()));
    }

    @Transactional
    public AuthTokenResponse refresh(String rawRefreshToken, HttpServletRequest httpRequest) {
        UserSessionService.RefreshTokenPair rotated = userSessionService.rotateRefreshToken(
                rawRefreshToken,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
        User user = rotated.session().getUser();
        assertApproved(user);
        guestJudgeLifecycleService.assertHackathonNotEndedForTempJudge(user);
        String access = jwtTokenService.createAccessToken(user);
        return AuthTokenResponse.builder()
                .accessToken(access)
                .refreshToken(rotated.rawToken())
                .tokenType("Bearer")
                .expiresInSeconds(jwtProperties.getAccessTtlMinutes() * 60L)
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .build();
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        userSessionService.revokeByRawToken(rawRefreshToken);
        auditService.log(AuditAction.ACCOUNT_LOGOUT, "users", null, Map.of());
    }

    @Transactional
    public void logoutAll() {
        Integer userId = currentUserAccessor.currentUserId();
        userSessionService.revokeAllForUser(userId);
        auditService.log(AuditAction.ACCOUNT_LOGOUT_ALL, "users", userId, Map.of());
    }

    private void assertGuestJudgeInvitationValid(User user) {
        if (!Boolean.TRUE.equals(user.getIsTempAccount())) {
            return;
        }
        Invitation inv = invitationRepository
                .findFirstByEmailAndRoleAndAcceptedAtIsNullOrderByCreatedAtDesc(
                        user.getEmail(), UserRole.JUDGE)
                .orElse(null);
        if (inv == null) {
            return;
        }
        if (inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthException(ErrorCode.INVITATION_EXPIRED,
                    "Lời mời judge đã hết hạn — liên hệ Coordinator để gửi lại",
                    HttpStatus.UNAUTHORIZED,
                    Map.of("expiresAt", inv.getExpiresAt().toString()));
        }
    }

    private void assertApproved(User user) {
        if (user.getRole() == UserRole.STUDENT && user.getStatus() == UserStatus.PENDING) {
            return;
        }
        if (user.getStatus() == UserStatus.PENDING) {
            throw new AuthException(ErrorCode.ACCOUNT_PENDING_NOT_ALLOWED_LOGIN,
                    "Tài khoản đang chờ duyệt", HttpStatus.UNAUTHORIZED,
                    Map.of("status", UserStatus.PENDING.name()));
        }
        if (user.getStatus() == UserStatus.REJECTED) {
            throw new AuthException(ErrorCode.ACCOUNT_REJECTED_NOT_ALLOWED_LOGIN,
                    "Tài khoản đã bị từ chối", HttpStatus.UNAUTHORIZED,
                    Map.of("status", UserStatus.REJECTED.name()));
        }
        if (user.getStatus() != UserStatus.APPROVED) {
            throw new AuthException(ErrorCode.UNAUTHORIZED,
                    "Trạng thái tài khoản không cho phép đăng nhập",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    private static AuthException invalidCredentials() {
        return new AuthException(ErrorCode.INVALID_CREDENTIALS,
                "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED);
    }
}
