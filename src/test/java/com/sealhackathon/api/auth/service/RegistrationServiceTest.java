package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.auth.dto.request.RegisterRequest;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void registerMinimal_savesPendingStudentWithUnspecifiedProfile() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.existsByEmail("sv@fpt.edu.vn")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10);
            return u;
        });
        when(jwtProperties.isDevExposeEmailVerificationToken()).thenReturn(false);

        RegisterRequest req = new RegisterRequest();
        req.setFullName("SV FPT");
        req.setEmail("SV@fpt.edu.vn");
        req.setPassword("password12");
        req.setConfirmPassword("password12");

        var response = registrationService.register(req);

        assertThat(response.getStatus()).isEqualTo(UserStatus.PENDING.name());
        assertThat(response.getMessage()).contains("xác thực");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("sv@fpt.edu.vn");
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(captor.getValue().getUserType()).isEqualTo(UserType.UNSPECIFIED);
        assertThat(captor.getValue().getStudentCode()).isNull();
        assertThat(captor.getValue().getChapter()).isNull();
        assertThat(captor.getValue().getInstitution()).isNull();
        assertThat(captor.getValue().getEmailVerifiedAt()).isNull();
        verify(emailVerificationService).sendVerificationEmail(any(User.class));
    }

    @Test
    void registerWhenConfirmPasswordMismatch_throwsValidation() {
        when(userRepository.existsByEmail("ext@gmail.com")).thenReturn(false);
        RegisterRequest req = new RegisterRequest();
        req.setEmail("ext@gmail.com");
        req.setPassword("password12");
        req.setConfirmPassword("password13");
        assertThatThrownBy(() -> registrationService.register(req))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void registerDuplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("taken@fpt.edu.vn")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setEmail("taken@fpt.edu.vn");
        req.setFullName("X");
        req.setPassword("password12");
        req.setConfirmPassword("password12");

        assertThatThrownBy(() -> registrationService.register(req))
                .isInstanceOf(ConflictException.class);
    }
}
