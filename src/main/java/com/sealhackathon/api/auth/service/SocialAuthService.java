package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.auth.config.OAuthProperties;
import com.sealhackathon.api.auth.dto.response.AuthTokenResponse;
import com.sealhackathon.api.auth.dto.response.OAuthLinkStatusResponse;
import com.sealhackathon.api.auth.service.social.GithubIdentityVerifier;
import com.sealhackathon.api.auth.service.social.GoogleIdentityVerifier;
import com.sealhackathon.api.auth.service.social.SocialIdentity;
import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.invitations.service.GuestJudgeLifecycleService;
import com.sealhackathon.api.oauth_accounts.entity.OAuthAccount;
import com.sealhackathon.api.oauth_accounts.repository.OAuthAccountRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final OAuthAccountRepository oAuthAccountRepository;
    private final UserRepository userRepository;
    private final GoogleIdentityVerifier googleIdentityVerifier;
    private final GithubIdentityVerifier githubIdentityVerifier;
    private final JwtTokenService jwtTokenService;
    private final UserSessionService userSessionService;
    private final JwtProperties jwtProperties;
    private final OAuthProperties oAuthProperties;
    private final AuditService auditService;
    private final CurrentUserAccessor currentUserAccessor;
    private final GuestJudgeLifecycleService guestJudgeLifecycleService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthTokenResponse loginWithGoogle(String idToken, String existingAccountPassword, HttpServletRequest httpRequest) {
        SocialIdentity identity = googleIdentityVerifier.verifyIdToken(idToken);
        return loginWithIdentity(identity, existingAccountPassword, httpRequest);
    }

    @Transactional
    public AuthTokenResponse loginWithGithub(String accessToken, String existingAccountPassword, HttpServletRequest httpRequest) {
        SocialIdentity identity = githubIdentityVerifier.verifyAccessToken(accessToken);
        return loginWithIdentity(identity, existingAccountPassword, httpRequest);
    }

    @Transactional
    public AuthTokenResponse loginWithGithubCode(
            String code,
            String redirectUri,
            String existingAccountPassword,
            HttpServletRequest httpRequest) {
        SocialIdentity identity = githubIdentityVerifier.verifyCode(code, redirectUri);
        return loginWithIdentity(identity, existingAccountPassword, httpRequest);
    }

    @Transactional
    public OAuthLinkStatusResponse linkGoogleForCurrentUser(String idToken) {
        SocialIdentity identity = googleIdentityVerifier.verifyIdToken(idToken);
        return linkIdentityForCurrentUser(identity);
    }

    @Transactional
    public OAuthLinkStatusResponse linkGithubForCurrentUser(String accessToken) {
        SocialIdentity identity = githubIdentityVerifier.verifyAccessToken(accessToken);
        return linkIdentityForCurrentUser(identity);
    }

    @Transactional
    public OAuthLinkStatusResponse linkGithubCodeForCurrentUser(String code, String redirectUri) {
        SocialIdentity identity = githubIdentityVerifier.verifyCode(code, redirectUri);
        return linkIdentityForCurrentUser(identity);
    }

    public OAuthLinkStatusResponse unlinkGoogleForCurrentUser() {
        return unlinkProviderForCurrentUser("GOOGLE");
    }

    @Transactional
    public OAuthLinkStatusResponse unlinkGithubForCurrentUser() {
        return unlinkProviderForCurrentUser("GITHUB");
    }

    private AuthTokenResponse loginWithIdentity(
            SocialIdentity identity,
            String existingAccountPassword,
            HttpServletRequest httpRequest) {
        User user = resolveLoginUser(identity, existingAccountPassword);
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(LocalDateTime.now());
        }
        assertApproved(user);
        guestJudgeLifecycleService.assertHackathonNotEndedForTempJudge(user);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String access = jwtTokenService.createAccessToken(user);
        UserSessionService.RefreshTokenPair refresh = userSessionService.createSession(
                user, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        auditService.log(AuditAction.ACCOUNT_LOGIN, "users", user.getId(), Map.of(
                "email", user.getEmail(),
                "loginMethod", "oauth_" + identity.provider().toLowerCase(Locale.ROOT)
        ));
        return AuthTokenResponse.builder()
                .accessToken(access)
                .refreshToken(refresh.rawToken())
                .tokenType("Bearer")
                .expiresInSeconds(jwtProperties.getAccessTtlMinutes() * 60L)
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .build();
    }

    private User resolveLoginUser(SocialIdentity identity, String existingAccountPassword) {
        OAuthAccount linked = oAuthAccountRepository.findByProviderAndProviderUid(identity.provider(), identity.providerUid())
                .orElse(null);
        if (linked != null) {
            return linked.getUser();
        }
        String normalizedEmail = identity.email().trim().toLowerCase(Locale.ROOT);
        User userByEmail = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (userByEmail == null && oAuthProperties.isAutoCreateUserOnLogin()) {
            userByEmail = createUserFromSocial(identity, normalizedEmail);
        }
        if (userByEmail == null || !oAuthProperties.isAutoLinkByEmail()) {
            throw new AuthException(
                    ErrorCode.OAUTH_ACCOUNT_NOT_LINKED,
                    "Tài khoản " + identity.provider() + " chưa liên kết với tài khoản hệ thống",
                    HttpStatus.UNAUTHORIZED,
                    Map.of("provider", identity.provider(), "email", normalizedEmail));
        }
        if (oAuthProperties.isRequirePasswordForAutoLink()) {
            requirePasswordConfirmation(userByEmail, existingAccountPassword, identity.provider());
        }
        ensureProviderNotLinkedToOtherUser(identity, userByEmail);
        oAuthAccountRepository.save(OAuthAccount.builder()
                .user(userByEmail)
                .provider(identity.provider())
                .providerUid(identity.providerUid())
                .build());
        auditService.log(AuditAction.ACCOUNT_OAUTH_LINKED, "users", userByEmail.getId(), Map.of(
                "provider", identity.provider(),
                "email", normalizedEmail,
                "autoLinkedByEmail", true
        ));
        return userByEmail;
    }

    private User createUserFromSocial(SocialIdentity identity, String normalizedEmail) {
        LocalDateTime now = LocalDateTime.now();
        User created = User.builder()
                .fullName(defaultFullName(identity, normalizedEmail))
                .email(normalizedEmail)
                .passwordHash(null)
                .role(UserRole.STUDENT)
                .userType(UserType.UNSPECIFIED)
                .studentCode(null)
                .institution(null)
                .studentCardImagePath(null)
                .status(UserStatus.PENDING)
                .isTempAccount(false)
                .isDeptHead(false)
                .mustChangePassword(false)
                .emailVerifiedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        User saved = userRepository.save(created);
        auditService.log(AuditAction.ACCOUNT_REGISTER, "users", saved.getId(), Map.of(
                "email", normalizedEmail,
                "signupMethod", "oauth_" + identity.provider().toLowerCase(Locale.ROOT),
                "autoCreated", true
        ));
        return saved;
    }

    private static String defaultFullName(SocialIdentity identity, String email) {
        String displayName = identity.displayName();
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private OAuthLinkStatusResponse linkIdentityForCurrentUser(SocialIdentity identity) {
        Integer userId = currentUserAccessor.currentUserId();
        User current = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        ErrorCode.UNAUTHORIZED, "User không tồn tại", HttpStatus.UNAUTHORIZED));
        String normalizedCurrentEmail = current.getEmail().trim().toLowerCase(Locale.ROOT);
        String normalizedIdentityEmail = identity.email().trim().toLowerCase(Locale.ROOT);
        if (!normalizedCurrentEmail.equals(normalizedIdentityEmail)
                && !oAuthProperties.isAllowLinkDifferentEmail()) {
            throw new AuthException(
                    ErrorCode.OAUTH_EMAIL_MISMATCH,
                    "Email " + identity.provider() + " phải trùng email tài khoản hiện tại để liên kết",
                    HttpStatus.BAD_REQUEST,
                    Map.of("accountEmail", normalizedCurrentEmail, "oauthEmail", normalizedIdentityEmail));
        }

        OAuthAccount existing = oAuthAccountRepository.findByProviderAndProviderUid(identity.provider(), identity.providerUid())
                .orElse(null);
        if (existing != null) {
            if (!existing.getUser().getId().equals(current.getId())) {
                throw new ConflictException(
                        ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED,
                        "Tài khoản " + identity.provider() + " đã liên kết với user khác");
            }
            return OAuthLinkStatusResponse.builder()
                    .provider(identity.provider())
                    .email(normalizedIdentityEmail)
                    .linked(true)
                    .message("Tài khoản đã được liên kết trước đó")
                    .build();
        }

        OAuthAccount oauth = OAuthAccount.builder()
                .user(current)
                .provider(identity.provider())
                .providerUid(identity.providerUid())
                .build();
        oAuthAccountRepository.save(oauth);
        auditService.log(AuditAction.ACCOUNT_OAUTH_LINKED, "users", current.getId(), Map.of(
                "provider", identity.provider(),
                "email", normalizedIdentityEmail
        ));
        return OAuthLinkStatusResponse.builder()
                .provider(identity.provider())
                .email(normalizedIdentityEmail)
                .linked(true)
                .message("Liên kết tài khoản thành công")
                .build();
    }

    private OAuthLinkStatusResponse unlinkProviderForCurrentUser(String provider) {
        Integer userId = currentUserAccessor.currentUserId();
        User current = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(
                        ErrorCode.UNAUTHORIZED, "User không tồn tại", HttpStatus.UNAUTHORIZED));
        OAuthAccount oauth = oAuthAccountRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new AuthException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Chưa liên kết " + provider,
                        HttpStatus.NOT_FOUND));
        long linkedProviders = oAuthAccountRepository.countByUserId(userId);
        boolean noPassword = current.getPasswordHash() == null || current.getPasswordHash().isBlank();
        if (linkedProviders <= 1 && noPassword) {
            throw new ConflictException(
                    ErrorCode.OAUTH_UNLINK_FORBIDDEN,
                    "Không thể gỡ liên kết social cuối cùng khi tài khoản chưa có mật khẩu");
        }
        oAuthAccountRepository.delete(oauth);
        auditService.log(AuditAction.ACCOUNT_OAUTH_UNLINKED, "users", userId, Map.of("provider", provider));
        return OAuthLinkStatusResponse.builder()
                .provider(provider)
                .email(current.getEmail())
                .linked(false)
                .message("Đã gỡ liên kết " + provider)
                .build();
    }

    private void ensureProviderNotLinkedToOtherUser(SocialIdentity identity, User expectedUser) {
        OAuthAccount existing = oAuthAccountRepository.findByProviderAndProviderUid(identity.provider(), identity.providerUid())
                .orElse(null);
        if (existing != null && !existing.getUser().getId().equals(expectedUser.getId())) {
            throw new ConflictException(
                    ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED,
                    "Tài khoản " + identity.provider() + " đã liên kết với user khác");
        }
    }

    private void requirePasswordConfirmation(User user, String password, String provider) {
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()
                || password == null || password.isBlank()
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException(
                    ErrorCode.OAUTH_PASSWORD_CONFIRM_REQUIRED,
                    "Cần xác nhận mật khẩu tài khoản hiện có trước khi liên kết " + provider,
                    HttpStatus.UNAUTHORIZED);
        }
    }

    private static void assertApproved(User user) {
        if (user.getRole() == UserRole.STUDENT && user.getStatus() == UserStatus.PENDING) {
            return;
        }
        if (Boolean.TRUE.equals(user.getIsTempAccount())
                && user.getUserType() == UserType.EXTERNAL
                && user.getRole() == UserRole.JUDGE
                && user.getStatus() == UserStatus.PENDING
                && Boolean.TRUE.equals(user.getMustChangePassword())) {
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
}
