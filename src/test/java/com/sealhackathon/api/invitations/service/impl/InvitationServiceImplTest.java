package com.sealhackathon.api.invitations.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.invitations.entity.Invitation;
import com.sealhackathon.api.invitations.repository.InvitationRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceImplTest {

    @Mock private InvitationRepository invitationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AppProperties appProperties;
    @Mock private GuestJudgeLifecycleService guestJudgeLifecycleService;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    @Test
    void resendRejectedWhenTokenStillValid() {
        Invitation inv = Invitation.builder()
                .id(1)
                .email("j@test.com")
                .role(UserRole.JUDGE)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        when(invitationRepository.findById(1)).thenReturn(Optional.of(inv));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> invitationService.resend(1));
        assertEquals(ErrorCode.INVITATION_STILL_VALID, ex.getCode());
    }

    @Test
    void resendExpired_regeneratesPasswordAndEmails() {
        Hackathon hackathon = Hackathon.builder().id(1).build();
        Invitation inv = Invitation.builder()
                .id(2)
                .email("j@test.com")
                .role(UserRole.JUDGE)
                .hackathon(hackathon)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
        User user = User.builder().id(9).email("j@test.com").fullName("Judge").build();
        when(invitationRepository.findById(2)).thenReturn(Optional.of(inv));
        when(userRepository.findByEmail("j@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");
        when(appProperties.getFrontendUrl()).thenReturn("https://seal-hackathon-fe.vercel.app");
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        invitationService.resend(2);

        assertEquals("new-hash", user.getPasswordHash());
        verify(emailService).resendGuestJudgeInvitation(
                anyString(), anyString(), anyString(), anyString(), any(LocalDateTime.class));
    }
}
