package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.auth.dto.request.RegisterRequest;
import com.sealhackathon.api.chapters.entity.Chapter;
import com.sealhackathon.api.chapters.repository.ChapterRepository;
import com.sealhackathon.api.chapters.value_object.ChapterStatus;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.ConflictException;
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

import java.util.Optional;

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
    private ChapterRepository chapterRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void registerInternal_savesPendingStudent() {
        when(jwtProperties.isDevExposeVerifyToken()).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.existsByEmail("sv@fpt.edu.vn")).thenReturn(false);
        when(userRepository.existsByStudentCode("SE123")).thenReturn(false);
        Chapter chapter = Chapter.builder().id(1).status(ChapterStatus.ACTIVE).build();
        when(chapterRepository.findById(1)).thenReturn(Optional.of(chapter));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10);
            return u;
        });
        when(jwtTokenService.createEmailVerifyToken(10)).thenReturn("verify-jwt");

        RegisterRequest req = new RegisterRequest();
        req.setFullName("SV FPT");
        req.setEmail("SV@fpt.edu.vn");
        req.setPassword("password12");
        req.setUserType(UserType.INTERNAL);
        req.setStudentCode("SE123");
        req.setChapterId(1);

        var response = registrationService.register(req);

        assertThat(response.getStatus()).isEqualTo(UserStatus.PENDING.name());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("sv@fpt.edu.vn");
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    void registerExternal_withoutInvitationToken_savesPendingStudent() {
        when(jwtProperties.isDevExposeVerifyToken()).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.existsByEmail("ext@gmail.com")).thenReturn(false);
        when(userRepository.existsByStudentCode("EXT001")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(11);
            return u;
        });
        when(jwtTokenService.createEmailVerifyToken(11)).thenReturn("verify-jwt");

        RegisterRequest req = new RegisterRequest();
        req.setFullName("SV Ngoai");
        req.setEmail("ext@gmail.com");
        req.setPassword("password12");
        req.setUserType(UserType.EXTERNAL);
        req.setStudentCode("EXT001");
        req.setInstitution("Đại học Bách Khoa");

        var response = registrationService.register(req);

        assertThat(response.getStatus()).isEqualTo(UserStatus.PENDING.name());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getInstitution()).isEqualTo("Đại học Bách Khoa");
        assertThat(captor.getValue().getUserType()).isEqualTo(UserType.EXTERNAL);
    }

    @Test
    void registerDuplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("taken@fpt.edu.vn")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setEmail("taken@fpt.edu.vn");
        req.setFullName("X");
        req.setPassword("password12");
        req.setUserType(UserType.INTERNAL);
        req.setStudentCode("SE1");
        req.setChapterId(1);

        assertThatThrownBy(() -> registrationService.register(req))
                .isInstanceOf(ConflictException.class);
    }
}
