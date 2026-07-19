package com.sealhackathon.api.invitations.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.invitations.entity.Invitation;
import com.sealhackathon.api.invitations.repository.InvitationRepository;
import com.sealhackathon.api.invitations.service.EmailService;
import com.sealhackathon.api.invitations.service.GuestJudgeLifecycleService;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceImplRevokeTest {

    @Mock private InvitationRepository invitationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AppProperties appProperties;
    @Mock private GuestJudgeLifecycleService guestJudgeLifecycleService;

    @InjectMocks
    private InvitationServiceImpl service;

    @Test
    void revoke_pendingInvitation_setsRevokedAtAndAudits() {
        Invitation inv = Invitation.builder()
                .id(5)
                .email("guest@test.com")
                .role(UserRole.JUDGE)
                .build();
        User user = User.builder().email("guest@test.com").mustChangePassword(true).build();

        when(invitationRepository.findById(5)).thenReturn(Optional.of(inv));
        when(userRepository.findByEmail("guest@test.com")).thenReturn(Optional.of(user));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.revoke(5);

        assertThat(result.getRevokedAt()).isNotNull();
        verify(auditService).log(eq(AuditAction.INVITATION_REVOKE), eq("invitations"), eq(5), any(Map.class));
    }

    @Test
    void revoke_activatedUser_rejects() {
        Invitation inv = Invitation.builder()
                .id(6)
                .email("active@test.com")
                .role(UserRole.JUDGE)
                .build();
        User user = User.builder().email("active@test.com").mustChangePassword(false).build();

        when(invitationRepository.findById(6)).thenReturn(Optional.of(inv));
        when(userRepository.findByEmail("active@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.revoke(6))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVITATION_ALREADY_ACCEPTED);
    }
}
