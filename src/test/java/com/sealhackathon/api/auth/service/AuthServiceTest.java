package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.auth.dto.request.ChangePasswordRequest;
import com.sealhackathon.api.auth.dto.request.LoginRequest;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.invitations.entity.Invitation;
import com.sealhackathon.api.invitations.repository.InvitationRepository;
import com.sealhackathon.api.invitations.service.GuestJudgeLifecycleService;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import com.sealhackathon.api.user_sessions.entity.UserSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private InvitationRepository invitationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private UserSessionService userSessionService;
    @Mock private JwtProperties jwtProperties;
    @Mock private AuditService auditService;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private GuestJudgeLifecycleService guestJudgeLifecycleService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_tempJudge_returnsMustChangePassword() {
        User user = tempJudgeUser();
        when(userRepository.findByEmail("guest@company.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("tempPass12", "hash")).thenReturn(true);
        when(jwtTokenService.createAccessToken(user)).thenReturn("access");
        when(userSessionService.createSession(any(), any(), any()))
                .thenReturn(new UserSessionService.RefreshTokenPair("refresh", new UserSession()));
        when(jwtProperties.getAccessTtlMinutes()).thenReturn(30);

        LoginRequest req = new LoginRequest();
        req.setEmail("guest@company.com");
        req.setPassword("tempPass12");

        var response = authService.login(req, new MockHttpServletRequest());

        assertThat(response.isMustChangePassword()).isTrue();
    }

    @Test
    void login_tempJudge_expiredInvitation_throws() {
        User user = tempJudgeUser();
        when(userRepository.findByEmail("guest@company.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("tempPass12", "hash")).thenReturn(true);
        Invitation expired = Invitation.builder()
                .id(1)
                .email(user.getEmail())
                .role(UserRole.JUDGE)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        when(invitationRepository.findFirstByEmailAndRoleAndAcceptedAtIsNullOrderByCreatedAtDesc(
                user.getEmail(), UserRole.JUDGE))
                .thenReturn(Optional.of(expired));

        LoginRequest req = new LoginRequest();
        req.setEmail("guest@company.com");
        req.setPassword("tempPass12");

        assertThatThrownBy(() -> authService.login(req, new MockHttpServletRequest()))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getCode())
                .isEqualTo(ErrorCode.INVITATION_EXPIRED);
    }

    @Test
    void login_tempJudge_hackathonEnded_throws() {
        User user = tempJudgeUser();
        when(userRepository.findByEmail("guest@company.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("tempPass12", "hash")).thenReturn(true);
        when(invitationRepository.findFirstByEmailAndRoleAndAcceptedAtIsNullOrderByCreatedAtDesc(
                user.getEmail(), UserRole.JUDGE))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.doThrow(new AuthException(ErrorCode.TEMP_JUDGE_HACKATHON_ENDED,
                        "ended", org.springframework.http.HttpStatus.UNAUTHORIZED))
                .when(guestJudgeLifecycleService).assertHackathonNotEndedForTempJudge(user);

        LoginRequest req = new LoginRequest();
        req.setEmail("guest@company.com");
        req.setPassword("tempPass12");

        assertThatThrownBy(() -> authService.login(req, new MockHttpServletRequest()))
                .isInstanceOf(AuthException.class)
                .extracting(ex -> ((AuthException) ex).getCode())
                .isEqualTo(ErrorCode.TEMP_JUDGE_HACKATHON_ENDED);
    }

    @Test
    void changePassword_clearsFlagAndAcceptsInvitation() {
        User user = tempJudgeUser();
        user.setId(10);
        user.setMustChangePassword(true);
        when(currentUserAccessor.currentUserId()).thenReturn(10);
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass12", "hash")).thenReturn(true);
        when(passwordEncoder.matches("newPass123", "hash")).thenReturn(false);
        when(passwordEncoder.encode("newPass123")).thenReturn("new-hash");
        Invitation inv = Invitation.builder()
                .id(5)
                .email(user.getEmail())
                .role(UserRole.JUDGE)
                .build();
        when(invitationRepository.findFirstByEmailAndRoleAndAcceptedAtIsNullOrderByCreatedAtDesc(
                user.getEmail(), UserRole.JUDGE))
                .thenReturn(Optional.of(inv));

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldPass12");
        req.setNewPassword("newPass123");

        authService.changePassword(req);

        assertThat(user.getMustChangePassword()).isFalse();
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(inv.getAcceptedAt()).isNotNull();
        verify(invitationRepository).save(inv);
    }

    private static User tempJudgeUser() {
        return User.builder()
                .email("guest@company.com")
                .passwordHash("hash")
                .role(UserRole.JUDGE)
                .userType(UserType.EXTERNAL)
                .isTempAccount(true)
                .mustChangePassword(true)
                .status(UserStatus.APPROVED)
                .build();
    }
}
