package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.auth.dto.request.RegisterRequest;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
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
import java.util.Optional;

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
    @Mock
    private ChapterRepository chapterRepository;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void registerInternal_savesPendingStudentWithChapter() {
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
        req.setUserType(UserType.INTERNAL);
        req.setStudentCode("SE123456");
        req.setChapterId(1);
        Chapter chapter = Chapter.builder().id(1).name("FPT HCM").code("FPT-HCM").build();
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));

        var response = registrationService.register(req);

        assertThat(response.getStatus()).isEqualTo(UserStatus.PENDING.name());
        assertThat(response.getMessage()).contains("xác thực");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("sv@fpt.edu.vn");
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(captor.getValue().getUserType()).isEqualTo(UserType.INTERNAL);
        assertThat(captor.getValue().getStudentCode()).isEqualTo("SE123456");
        assertThat(captor.getValue().getChapter()).isSameAs(chapter);
        assertThat(captor.getValue().getInstitution()).isNull();
        assertThat(captor.getValue().getEmailVerifiedAt()).isNull();
        verify(emailVerificationService).sendVerificationEmail(any(User.class));
    }

    @Test
    void registerExternal_savesInstitutionWithoutChapter() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.existsByEmail("ext@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        RegisterRequest req = validRequest(UserType.EXTERNAL);
        req.setInstitution("Đại học Bách Khoa");

        registrationService.register(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUserType()).isEqualTo(UserType.EXTERNAL);
        assertThat(captor.getValue().getInstitution()).isEqualTo("Đại học Bách Khoa");
        assertThat(captor.getValue().getChapter()).isNull();
    }

    @Test
    void registerInternalWithoutChapter_throwsValidation() {
        when(userRepository.existsByEmail("ext@example.com")).thenReturn(false);
        RegisterRequest req = validRequest(UserType.INTERNAL);

        assertThatThrownBy(() -> registrationService.register(req))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static RegisterRequest validRequest(UserType userType) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("ext@example.com");
        req.setPassword("password12");
        req.setConfirmPassword("password12");
        req.setUserType(userType);
        req.setStudentCode("SV001");
        return req;
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
