package com.sealhackathon.api.events.job;

import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventReminderSchedulerTest {

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private EventReminderScheduler scheduler;

    @BeforeEach
    void setLeadHours() {
        ReflectionTestUtils.setField(scheduler, "leadHours", 24);
    }

    @Test
    void runUpcomingEventReminders_sendsAndMarksReminderSentAt() {
        LocalDateTime now = LocalDateTime.now();
        Event event = Event.builder()
                .id(5)
                .title("Workshop AI")
                .startsAt(now.plusHours(12))
                .isPublic(true)
                .hackathon(Hackathon.builder().id(1).build())
                .build();
        when(eventRepository.findPublicUpcomingWithoutReminder(any(), any())).thenReturn(List.of(event));
        when(userRepository.findAllByStatus(UserStatus.APPROVED))
                .thenReturn(List.of(User.builder().id(1).build()));

        scheduler.runUpcomingEventReminders();

        verify(notificationService).sendBatch(
                anyList(),
                eq("EVENT_UPCOMING"),
                eq("Sự kiện sắp diễn ra: Workshop AI"),
                anyString(),
                eq("events"),
                eq(5));
        verify(eventRepository).save(event);
        org.junit.jupiter.api.Assertions.assertNotNull(event.getReminderSentAt());
    }

    @Test
    void runUpcomingEventReminders_noDueEvents_skipsSend() {
        when(eventRepository.findPublicUpcomingWithoutReminder(any(), any())).thenReturn(List.of());

        scheduler.runUpcomingEventReminders();

        verify(notificationService, never()).sendBatch(any(), any(), any(), any(), any(), any());
        verify(eventRepository, never()).save(any());
    }
}
