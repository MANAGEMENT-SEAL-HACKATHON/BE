package com.sealhackathon.api.hackathons.job;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HackathonDraftReminderSchedulerTest {

    @Mock private HackathonRepository hackathonRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private HackathonDraftReminderScheduler scheduler;

    @BeforeEach
    void setLeadDays() {
        ReflectionTestUtils.setField(scheduler, "leadDays", 3);
    }

    @Test
    void runDraftReminders_draftWithinWindow_sendsAndMarksSent() {
        Hackathon draft = Hackathon.builder()
                .id(7)
                .name("SEAL 2026")
                .status(HackathonStatus.DRAFT)
                .registrationStart(LocalDate.now().plusDays(1))
                .build();
        when(hackathonRepository.findByStatusAndDraftReminderSentAtIsNull(HackathonStatus.DRAFT))
                .thenReturn(List.of(draft));
        when(userRepository.findAllByRoleAndStatus(UserRole.COORDINATOR, UserStatus.APPROVED))
                .thenReturn(List.of(User.builder().id(1).build()));

        scheduler.runDraftReminders();

        verify(notificationService).sendBatch(
                anyList(),
                eq("HACKATHON_DRAFT_REMINDER"),
                anyString(),
                anyString(),
                eq("hackathons"),
                eq(7));
        verify(hackathonRepository).save(draft);
        Assertions.assertNotNull(draft.getDraftReminderSentAt());
    }

    @Test
    void runDraftReminders_registrationStartFarAway_skips() {
        Hackathon draft = Hackathon.builder()
                .id(8)
                .name("SEAL later")
                .status(HackathonStatus.DRAFT)
                .registrationStart(LocalDate.now().plusDays(30))
                .build();
        when(hackathonRepository.findByStatusAndDraftReminderSentAtIsNull(HackathonStatus.DRAFT))
                .thenReturn(List.of(draft));

        scheduler.runDraftReminders();

        verify(notificationService, never()).sendBatch(any(), any(), any(), any(), any(), any());
        verify(hackathonRepository, never()).save(any());
    }

    @Test
    void runDraftReminders_noDrafts_skips() {
        when(hackathonRepository.findByStatusAndDraftReminderSentAtIsNull(HackathonStatus.DRAFT))
                .thenReturn(List.of());

        scheduler.runDraftReminders();

        verify(notificationService, never()).sendBatch(any(), any(), any(), any(), any(), any());
    }
}
