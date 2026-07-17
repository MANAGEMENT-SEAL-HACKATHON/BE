package com.sealhackathon.api.users.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.config.AppProperties;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.invitations.service.GuestJudgeLifecycleService;
import com.sealhackathon.api.invitations.InvitationConstants;
import com.sealhackathon.api.invitations.entity.Invitation;
import com.sealhackathon.api.invitations.repository.InvitationRepository;
import com.sealhackathon.api.invitations.service.EmailService;
import com.sealhackathon.api.users.dto.request.CreateTempJudgeRequest;
import com.sealhackathon.api.users.dto.response.TempJudgeResponse;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.mapper.UserMapper;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TempJudgeServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private InvitationRepository invitationRepository;
    @Mock private EmailService emailService;
    @Mock private UserMapper userMapper;
    @Mock private AuditService auditService;
    @Mock private CurrentUserAccessor currentUserAccessor;
    @Mock private HackathonRepository hackathonRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AppProperties appProperties;
    @Mock private GuestJudgeLifecycleService guestJudgeLifecycleService;

    @InjectMocks
    private TempJudgeServiceImpl tempJudgeService;

    @Test
    void createTempJudge_setsPasswordMustChangeAndInvitation72h() {
        when(userRepository.existsByEmail("guest@company.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("bcrypt-hash");
        when(currentUserAccessor.currentUserId()).thenReturn(1);
        Hackathon hackathon = Hackathon.builder().id(1).eventEnd(LocalDate.now().plusDays(30)).build();
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(hackathon));
        when(appProperties.getFrontendUrl()).thenReturn("https://seal-hackathon-fe.vercel.app");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10);
            return u;
        });
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> {
            Invitation i = inv.getArgument(0);
            i.setId(20);
            return i;
        });
        when(userMapper.toTempJudgeResponse(any(), any(), any(Boolean.class)))
                .thenReturn(TempJudgeResponse.builder().build());

        CreateTempJudgeRequest req = new CreateTempJudgeRequest();
        req.setFullName("Guest Judge");
        req.setEmail("guest@company.com");
        req.setInstitution("Corp");
        req.setPhone("+84");
        req.setHackathonId(1);

        tempJudgeService.createTempJudge(req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getMustChangePassword()).isTrue();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(saved.getEmailVerifiedAt()).isNotNull();

        ArgumentCaptor<Invitation> invCaptor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository, org.mockito.Mockito.atLeastOnce()).save(invCaptor.capture());
        Invitation inv = invCaptor.getValue();
        assertThat(inv.getRole()).isEqualTo(UserRole.JUDGE);
        assertThat(inv.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(InvitationConstants.INVITATION_EXPIRY_HOURS - 1));
        assertThat(inv.getLastTokenSent()).isTrue();

        verify(emailService).sendGuestJudgeInvitation(
                anyString(), anyString(), anyString(), anyString(), any(LocalDateTime.class));
    }
}
