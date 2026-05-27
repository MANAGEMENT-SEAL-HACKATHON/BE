package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.ConflictException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.events.dto.request.CreateEventRequest;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.mapper.EventMapper;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.EventScheduleValidator;
import com.sealhackathon.api.events.service.HackathonTimelineService;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.notifications.repository.NotificationRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplArchiveGuardTest {

    @Mock HackathonRepository hackathonRepository;
    @Mock EventRepository eventRepository;
    @Mock EventMapper eventMapper;
    @Mock AuditService auditService;
    @Mock EventScheduleValidator scheduleValidator;
    @Mock HackathonTimelineService hackathonTimelineService;
    @Mock NotificationService notificationService;
    @Mock NotificationRepository notificationRepository;
    @Mock UserRepository userRepository;
    @Mock CurrentUserAccessor currentUserAccessor;
    @Spy HackathonArchiveGuard archiveGuard = new HackathonArchiveGuard();

    @InjectMocks EventServiceImpl eventService;

    @Test
    void create_onFinishedHackathon_throwsArchived() {
        Hackathon finished = Hackathon.builder().id(1).status(HackathonStatus.FINISHED).build();
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(finished));

        CreateEventRequest req = CreateEventRequest.builder()
                .type(EventType.WORKSHOP)
                .title("WS")
                .startsAt(LocalDateTime.of(2026, 6, 6, 20, 0))
                .endsAt(LocalDateTime.of(2026, 6, 6, 21, 0))
                .build();

        assertThatThrownBy(() -> eventService.create(1, req))
                .isInstanceOf(ConflictException.class)
                .matches(ex -> ErrorCode.HACKATHON_ARCHIVED.equals(((ConflictException) ex).getCode()));
    }

    @Test
    void delete_onFinishedHackathon_throwsArchived() {
        Hackathon finished = Hackathon.builder().id(1).status(HackathonStatus.FINISHED).build();
        Event event = Event.builder().id(10).hackathon(finished).type(EventType.KICKOFF).build();
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.delete(10))
                .isInstanceOf(ConflictException.class)
                .matches(ex -> ErrorCode.HACKATHON_ARCHIVED.equals(((ConflictException) ex).getCode()));
    }
}
