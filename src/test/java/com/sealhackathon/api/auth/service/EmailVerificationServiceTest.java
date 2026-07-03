package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.invitations.service.EmailService;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private JwtProperties jwtProperties;
    @Mock private AppProperties appProperties;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    void verify_setsEmailVerifiedAtWhenNotYetVerified() {
        User user = User.builder()
                .id(5)
                .email("sv@fpt.edu.vn")
                .role(UserRole.STUDENT)
                .status(UserStatus.PENDING)
                .emailVerifiedAt(null)
                .build();
        when(jwtTokenService.parseEmailVerificationUserId("token")).thenReturn(5);
        when(userRepository.findById(5)).thenReturn(Optional.of(user));

        emailVerificationService.verify("token");

        assertThat(user.getEmailVerifiedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void verify_idempotentWhenAlreadyVerified() {
        LocalDateTime verifiedAt = LocalDateTime.now().minusDays(1);
        User user = User.builder()
                .id(5)
                .email("sv@fpt.edu.vn")
                .emailVerifiedAt(verifiedAt)
                .build();
        when(jwtTokenService.parseEmailVerificationUserId("token")).thenReturn(5);
        when(userRepository.findById(5)).thenReturn(Optional.of(user));

        emailVerificationService.verify("token");

        assertThat(user.getEmailVerifiedAt()).isEqualTo(verifiedAt);
        verify(userRepository, never()).save(any());
    }
}
