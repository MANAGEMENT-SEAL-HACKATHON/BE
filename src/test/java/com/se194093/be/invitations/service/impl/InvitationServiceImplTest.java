package com.se194093.be.invitations.service.impl;

import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.BusinessRuleException;
import com.se194093.be.common.exception.ErrorCode;
import com.se194093.be.invitations.entity.Invitation;
import com.se194093.be.invitations.repository.InvitationRepository;
import com.se194093.be.invitations.service.EmailService;
import com.se194093.be.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationServiceImplTest {

    @Mock private InvitationRepository invitationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    @Test
    void resendRejectedWhenTokenStillValid() {
        Invitation inv = Invitation.builder()
                .id(1)
                .email("j@test.com")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        when(invitationRepository.findById(1)).thenReturn(Optional.of(inv));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> invitationService.resend(1));
        assertEquals(ErrorCode.INVITATION_STILL_VALID, ex.getCode());
    }
}
