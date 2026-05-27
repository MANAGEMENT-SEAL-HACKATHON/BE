package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.auth.config.OAuthProperties;
import com.sealhackathon.api.auth.dto.response.AuthTokenResponse;
import com.sealhackathon.api.auth.dto.response.OAuthLinkStatusResponse;
import com.sealhackathon.api.auth.service.social.GithubIdentityVerifier;
import com.sealhackathon.api.auth.service.social.GoogleIdentityVerifier;
import com.sealhackathon.api.auth.service.social.SocialIdentity;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.invitations.service.GuestJudgeLifecycleService;
import com.sealhackathon.api.oauth_accounts.entity.OAuthAccount;
import com.sealhackathon.api.oauth_accounts.repository.OAuthAccountRepository;
import com.sealhackathon.api.user_sessions.entity.UserSession;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialAuthServiceTest {

    @Mock private OAuthAccountRepository oAuthAccountRepository;
    @Mock private UserRepository userRepository;
    @Mock private GoogleIdentityVerifier googleIdentityVerifier;
    @Mock private GithubIdentityVerifier githubIdentityVerifier;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private UserSessionService userSessionService;
    @Mock private JwtProperties jwtProperties;
    @Mock private OAuthProperties oAuthProperties;
    @Mock private AuditService auditService;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private GuestJudgeLifecycleService guestJudgeLifecycleService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SocialAuthService socialAuthService;

    @Test
    void loginWithGoogle_linkedAccount_returnsTokens() {
        SocialIdentity identity = new SocialIdentity("GOOGLE", "g-sub", "user@fpt.edu.vn", "User");
        User user = approvedUser();
        user.setId(8);
        OAuthAccount account = OAuthAccount.builder()
                .user(user)
                .provider("GOOGLE")
                .providerUid("g-sub")
                .build();
        when(googleIdentityVerifier.verifyIdToken("id-token")).thenReturn(identity);
        when(oAuthAccountRepository.findByProviderAndProviderUid("GOOGLE", "g-sub"))
                .thenReturn(Optional.of(account));
        when(jwtTokenService.createAccessToken(user)).thenReturn("access");
        when(userSessionService.createSession(any(), any(), any()))
                .thenReturn(new UserSessionService.RefreshTokenPair("refresh", new UserSession()));
        when(jwtProperties.getAccessTtlMinutes()).thenReturn(30);

        AuthTokenResponse response = socialAuthService.loginWithGoogle("id-token", null, new MockHttpServletRequest());

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
    }

    @Test
    void loginWithGithub_withoutLink_throws() {
        SocialIdentity identity = new SocialIdentity("GITHUB", "123", "user@fpt.edu.vn", "User");
        when(githubIdentityVerifier.verifyAccessToken("gh-token")).thenReturn(identity);
        when(oAuthAccountRepository.findByProviderAndProviderUid("GITHUB", "123")).thenReturn(Optional.empty());
        when(oAuthProperties.isAutoCreateUserOnLogin()).thenReturn(false);

        assertThatThrownBy(() -> socialAuthService.loginWithGithub("gh-token", null, new MockHttpServletRequest()))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getCode())
                .isEqualTo(ErrorCode.OAUTH_ACCOUNT_NOT_LINKED);
    }

    @Test
    void loginWithGithub_emailMatch_autoLinkAndLogin() {
        SocialIdentity identity = new SocialIdentity("GITHUB", "123", "user@fpt.edu.vn", "User");
        User user = approvedUser();
        user.setId(9);
        when(githubIdentityVerifier.verifyAccessToken("gh-token")).thenReturn(identity);
        when(oAuthAccountRepository.findByProviderAndProviderUid("GITHUB", "123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@fpt.edu.vn")).thenReturn(Optional.of(user));
        when(oAuthProperties.isAutoLinkByEmail()).thenReturn(true);
        when(oAuthProperties.isRequirePasswordForAutoLink()).thenReturn(false);
        when(jwtTokenService.createAccessToken(user)).thenReturn("access");
        when(userSessionService.createSession(any(), any(), any()))
                .thenReturn(new UserSessionService.RefreshTokenPair("refresh", new UserSession()));
        when(jwtProperties.getAccessTtlMinutes()).thenReturn(30);

        AuthTokenResponse response = socialAuthService.loginWithGithub("gh-token", null, new MockHttpServletRequest());

        assertThat(response.getAccessToken()).isEqualTo("access");
        verify(oAuthAccountRepository).save(any(OAuthAccount.class));
    }

    @Test
    void loginWithGoogle_emailMatch_requirePasswordButMissing_throws() {
        SocialIdentity identity = new SocialIdentity("GOOGLE", "g-sub-new", "user@fpt.edu.vn", "User");
        User user = approvedUser();
        user.setId(9);
        user.setPasswordHash("hash");
        when(googleIdentityVerifier.verifyIdToken("id-token")).thenReturn(identity);
        when(oAuthAccountRepository.findByProviderAndProviderUid("GOOGLE", "g-sub-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@fpt.edu.vn")).thenReturn(Optional.of(user));
        when(oAuthProperties.isAutoLinkByEmail()).thenReturn(true);
        when(oAuthProperties.isRequirePasswordForAutoLink()).thenReturn(true);

        assertThatThrownBy(() -> socialAuthService.loginWithGoogle("id-token", null, new MockHttpServletRequest()))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getCode())
                .isEqualTo(ErrorCode.OAUTH_PASSWORD_CONFIRM_REQUIRED);
    }

    @Test
    void loginWithGoogle_unknownEmail_autoCreateUserAndLogin() {
        SocialIdentity identity = new SocialIdentity("GOOGLE", "g-sub-999", "newuser@gmail.com", "New User");
        User created = approvedUser();
        created.setId(77);
        created.setEmail("newuser@gmail.com");
        created.setFullName("New User");

        when(googleIdentityVerifier.verifyIdToken("id-token")).thenReturn(identity);
        when(oAuthAccountRepository.findByProviderAndProviderUid("GOOGLE", "g-sub-999"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.empty());
        when(oAuthProperties.isAutoCreateUserOnLogin()).thenReturn(true);
        when(oAuthProperties.isAutoLinkByEmail()).thenReturn(true);
        when(oAuthProperties.isRequirePasswordForAutoLink()).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(created);
        when(jwtTokenService.createAccessToken(created)).thenReturn("access");
        when(userSessionService.createSession(any(), any(), any()))
                .thenReturn(new UserSessionService.RefreshTokenPair("refresh", new UserSession()));
        when(jwtProperties.getAccessTtlMinutes()).thenReturn(30);

        AuthTokenResponse response = socialAuthService.loginWithGoogle("id-token", null, new MockHttpServletRequest());

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        verify(userRepository, org.mockito.Mockito.atLeast(2)).save(any(User.class));
        verify(oAuthAccountRepository).save(any(OAuthAccount.class));
    }

    @Test
    void linkGoogleForCurrentUser_savesWhenEmailMatches() {
        User user = approvedUser();
        user.setId(11);
        SocialIdentity identity = new SocialIdentity("GOOGLE", "sub-111", "user@fpt.edu.vn", "User");
        when(currentUserAccessor.currentUserId()).thenReturn(11);
        when(userRepository.findById(11)).thenReturn(Optional.of(user));
        when(googleIdentityVerifier.verifyIdToken("id-token")).thenReturn(identity);
        when(oAuthAccountRepository.findByProviderAndProviderUid("GOOGLE", "sub-111"))
                .thenReturn(Optional.empty());

        OAuthLinkStatusResponse response = socialAuthService.linkGoogleForCurrentUser("id-token");

        assertThat(response.isLinked()).isTrue();
        assertThat(response.getProvider()).isEqualTo("GOOGLE");
        verify(oAuthAccountRepository).save(any(OAuthAccount.class));
    }

    @Test
    void linkGoogleForCurrentUser_emailMismatch_throws() {
        User user = approvedUser();
        user.setId(11);
        user.setEmail("other@fpt.edu.vn");
        SocialIdentity identity = new SocialIdentity("GOOGLE", "sub-111", "user@fpt.edu.vn", "User");
        when(currentUserAccessor.currentUserId()).thenReturn(11);
        when(userRepository.findById(11)).thenReturn(Optional.of(user));
        when(googleIdentityVerifier.verifyIdToken("id-token")).thenReturn(identity);

        assertThatThrownBy(() -> socialAuthService.linkGoogleForCurrentUser("id-token"))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getCode())
                .isEqualTo(ErrorCode.OAUTH_EMAIL_MISMATCH);
    }

    @Test
    void linkGithubForCurrentUser_linkedToAnotherUser_throws() {
        User current = approvedUser();
        current.setId(11);
        User other = approvedUser();
        other.setId(22);
        SocialIdentity identity = new SocialIdentity("GITHUB", "gh-uid", "user@fpt.edu.vn", "User");
        OAuthAccount existing = OAuthAccount.builder()
                .user(other)
                .provider("GITHUB")
                .providerUid("gh-uid")
                .build();
        when(currentUserAccessor.currentUserId()).thenReturn(11);
        when(userRepository.findById(11)).thenReturn(Optional.of(current));
        when(githubIdentityVerifier.verifyAccessToken("gh-token")).thenReturn(identity);
        when(oAuthAccountRepository.findByProviderAndProviderUid("GITHUB", "gh-uid"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> socialAuthService.linkGithubForCurrentUser("gh-token"))
                .isInstanceOf(ConflictException.class)
                .extracting(ex -> ((ConflictException) ex).getCode())
                .isEqualTo(ErrorCode.OAUTH_ACCOUNT_ALREADY_LINKED);
    }

    @Test
    void unlinkGoogle_noPasswordAndLastSocial_throws() {
        User user = approvedUser();
        user.setId(11);
        user.setPasswordHash(null);
        OAuthAccount existing = OAuthAccount.builder().user(user).provider("GOOGLE").providerUid("uid").build();
        when(currentUserAccessor.currentUserId()).thenReturn(11);
        when(userRepository.findById(11)).thenReturn(Optional.of(user));
        when(oAuthAccountRepository.findByUserIdAndProvider(11, "GOOGLE")).thenReturn(Optional.of(existing));
        when(oAuthAccountRepository.countByUserId(11)).thenReturn(1L);

        assertThatThrownBy(() -> socialAuthService.unlinkGoogleForCurrentUser())
                .isInstanceOf(ConflictException.class)
                .extracting(ex -> ((ConflictException) ex).getCode())
                .isEqualTo(ErrorCode.OAUTH_UNLINK_FORBIDDEN);
    }

    @Test
    void unlinkGithub_success() {
        User user = approvedUser();
        user.setId(11);
        user.setPasswordHash("hash");
        OAuthAccount existing = OAuthAccount.builder().user(user).provider("GITHUB").providerUid("uid").build();
        when(currentUserAccessor.currentUserId()).thenReturn(11);
        when(userRepository.findById(11)).thenReturn(Optional.of(user));
        when(oAuthAccountRepository.findByUserIdAndProvider(11, "GITHUB")).thenReturn(Optional.of(existing));
        when(oAuthAccountRepository.countByUserId(11)).thenReturn(1L);

        OAuthLinkStatusResponse response = socialAuthService.unlinkGithubForCurrentUser();

        assertThat(response.isLinked()).isFalse();
        assertThat(response.getProvider()).isEqualTo("GITHUB");
        verify(oAuthAccountRepository).delete(existing);
    }

    @Test
    void loginWithGithubCode_usesVerifierCodeFlow() {
        SocialIdentity identity = new SocialIdentity("GITHUB", "code-uid", "dev@gmail.com", "Dev");
        User user = approvedUser();
        user.setId(33);
        OAuthAccount linked = OAuthAccount.builder()
                .user(user)
                .provider("GITHUB")
                .providerUid("code-uid")
                .build();
        when(githubIdentityVerifier.verifyCode("code", "http://localhost:5173/auth/github/callback"))
                .thenReturn(identity);
        when(oAuthAccountRepository.findByProviderAndProviderUid("GITHUB", "code-uid"))
                .thenReturn(Optional.of(linked));
        when(jwtTokenService.createAccessToken(user)).thenReturn("access");
        when(userSessionService.createSession(any(), any(), any()))
                .thenReturn(new UserSessionService.RefreshTokenPair("refresh", new UserSession()));
        when(jwtProperties.getAccessTtlMinutes()).thenReturn(30);

        AuthTokenResponse response = socialAuthService.loginWithGithubCode(
                "code",
                "http://localhost:5173/auth/github/callback",
                null,
                new MockHttpServletRequest());

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
    }

    @Test
    void linkGithubCodeForCurrentUser_usesVerifierCodeFlow() {
        User user = approvedUser();
        user.setId(44);
        SocialIdentity identity = new SocialIdentity("GITHUB", "ghc-uid", "user@fpt.edu.vn", "User");
        when(currentUserAccessor.currentUserId()).thenReturn(44);
        when(userRepository.findById(44)).thenReturn(Optional.of(user));
        when(githubIdentityVerifier.verifyCode("code", "http://localhost:5173/auth/github/callback"))
                .thenReturn(identity);
        when(oAuthAccountRepository.findByProviderAndProviderUid("GITHUB", "ghc-uid"))
                .thenReturn(Optional.empty());

        OAuthLinkStatusResponse response = socialAuthService.linkGithubCodeForCurrentUser(
                "code",
                "http://localhost:5173/auth/github/callback");

        assertThat(response.getProvider()).isEqualTo("GITHUB");
        assertThat(response.isLinked()).isTrue();
    }

    private static User approvedUser() {
        return User.builder()
                .email("user@fpt.edu.vn")
                .status(UserStatus.APPROVED)
                .userType(UserType.EXTERNAL)
                .build();
    }
}
