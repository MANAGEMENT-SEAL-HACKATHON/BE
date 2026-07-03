package com.sealhackathon.api.invitations.service.impl;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.invitations.entity.Invitation;
import com.sealhackathon.api.invitations.repository.InvitationRepository;
import com.sealhackathon.api.judge_assignments.repository.JudgeAssignmentRepository;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestJudgeLifecycleServiceImplTest {

    @Mock private InvitationRepository invitationRepository;
    @Mock private EventRepository eventRepository;
    @Mock private JudgeAssignmentRepository judgeAssignmentRepository;

    @InjectMocks
    private GuestJudgeLifecycleServiceImpl lifecycleService;

    @Test
    void assertResendAllowed_within48hBeforeKickoff_throws() {
        Hackathon h = Hackathon.builder().id(1).eventEnd(LocalDate.now().plusDays(30)).build();
        Invitation inv = Invitation.builder()
                .id(10)
                .role(UserRole.JUDGE)
                .hackathon(h)
                .build();
        LocalDateTime kickoffStart = LocalDateTime.now().plusHours(24);
        when(eventRepository.findLatestByType(1, EventType.KICKOFF))
                .thenReturn(List.of(Event.builder().id(5).startsAt(kickoffStart).build()));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> lifecycleService.assertResendAllowed(inv));
        assertEquals(ErrorCode.INVITATION_RESEND_AFTER_KICKOFF_CUTOFF, ex.getCode());
    }

    @Test
    void assertResendAllowed_moreThan48hBeforeKickoff_passes() {
        Hackathon h = Hackathon.builder().id(1).eventEnd(LocalDate.now().plusDays(30)).build();
        Invitation inv = Invitation.builder()
                .id(10)
                .role(UserRole.JUDGE)
                .hackathon(h)
                .build();
        LocalDateTime kickoffStart = LocalDateTime.now().plusHours(72);
        when(eventRepository.findLatestByType(1, EventType.KICKOFF))
                .thenReturn(List.of(Event.builder().id(5).startsAt(kickoffStart).build()));

        lifecycleService.assertResendAllowed(inv);
    }

    @Test
    void assertHackathonNotEndedForTempJudge_afterEventEnd_throws() {
        User user = User.builder()
                .id(1)
                .email("g@test.com")
                .isTempAccount(true)
                .role(UserRole.JUDGE)
                .userType(UserType.EXTERNAL)
                .status(UserStatus.APPROVED)
                .build();
        Hackathon ended = Hackathon.builder()
                .id(2)
                .eventEnd(LocalDate.now().minusDays(1))
                .build();
        Invitation inv = Invitation.builder()
                .email(user.getEmail())
                .role(UserRole.JUDGE)
                .hackathon(ended)
                .build();
        when(invitationRepository.findByEmail(user.getEmail()))
                .thenReturn(List.of(inv));
        when(judgeAssignmentRepository.findByJudgeId(user.getId()))
                .thenReturn(List.of());

        AuthException ex = assertThrows(AuthException.class,
                () -> lifecycleService.assertHackathonNotEndedForTempJudge(user));
        assertEquals(ErrorCode.TEMP_JUDGE_HACKATHON_ENDED, ex.getCode());
    }

    @Test
    void requireHackathonOnInvitation_missingHackathon_throws() {
        Invitation inv = Invitation.builder().id(1).role(UserRole.JUDGE).build();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> lifecycleService.requireHackathonOnInvitationEntity(inv));
        assertEquals(ErrorCode.INVITATION_HACKATHON_REQUIRED, ex.getCode());
    }
}
