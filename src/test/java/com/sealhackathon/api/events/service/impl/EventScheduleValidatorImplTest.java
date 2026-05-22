package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.dto.request.CreateEventRequest;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.support.EventTimeline;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventScheduleValidatorImplTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventScheduleValidatorImpl validator;

    private Hackathon hackathon;

    @BeforeEach
    void setUp() {
        hackathon = Hackathon.builder()
                .id(1)
                .registrationStart(LocalDate.of(2026, 4, 1))
                .eventStart(LocalDate.of(2026, 4, 11))
                .eventEnd(LocalDate.of(2026, 4, 12))
                .build();
    }

    @Test
    void blocksWhenLocationAndMeetUrlBothMissing() {
        CreateEventRequest req = CreateEventRequest.builder()
                .title("T")
                .type(EventType.WORKSHOP)
                .startsAt(LocalDateTime.of(2026, 4, 9, 20, 0))
                .endsAt(LocalDateTime.of(2026, 4, 9, 21, 0))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_LOCATION_REQUIRED, ex.getCode());
    }

    @Test
    void blocksWorkshopWithoutEndsAt() {
        CreateEventRequest req = CreateEventRequest.builder()
                .title("WS")
                .type(EventType.WORKSHOP)
                .location("Online")
                .startsAt(LocalDateTime.of(2026, 4, 9, 20, 0))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_END_REQUIRED, ex.getCode());
    }

    @Test
    void blocksWorkshopStartAfterKickoff() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff(10, LocalDateTime.of(2026, 4, 11, 14, 0),
                        LocalDateTime.of(2026, 4, 11, 17, 0))));
        stubMilestoneStubs(EventType.KICKOFF);

        CreateEventRequest req = workshop(
                LocalDateTime.of(2026, 4, 11, 20, 0),
                LocalDateTime.of(2026, 4, 11, 21, 0));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_ORDER_VIOLATION, ex.getCode());
    }

    @Test
    void blocksWorkshopOverlappingKickoff() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff(10, LocalDateTime.of(2026, 4, 11, 14, 0),
                        LocalDateTime.of(2026, 4, 11, 17, 0))));
        stubMilestoneStubs(EventType.KICKOFF);

        CreateEventRequest req = workshop(
                LocalDateTime.of(2026, 4, 9, 20, 0),
                LocalDateTime.of(2026, 4, 11, 16, 0));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_ORDER_VIOLATION, ex.getCode());
    }

    @Test
    void allowsSpring2026WorkshopBeforeEventStart() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.WORKSHOP))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByHackathonIdAndType(1, EventType.PRESENTATION))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByHackathonIdAndType(1, EventType.AWARDS))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findOverlapping(eq(1), eq(EventType.WORKSHOP), any(), any(), eq(0)))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findOtherOverlapping(eq(1), any(), any(), eq(0)))
                .thenReturn(Collections.emptyList());

        CreateEventRequest req = workshop(
                LocalDateTime.of(2026, 4, 9, 20, 0),
                LocalDateTime.of(2026, 4, 9, 21, 30));

        assertDoesNotThrow(() -> validator.validateBlocking(hackathon, req, 0));
    }

    @Test
    void blocksDuplicateKickoff() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff(10, LocalDateTime.of(2026, 4, 11, 14, 0),
                        LocalDateTime.of(2026, 4, 11, 17, 0))));

        CreateEventRequest req = CreateEventRequest.builder()
                .title("K2")
                .type(EventType.KICKOFF)
                .location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 12, 14, 0))
                .endsAt(LocalDateTime.of(2026, 4, 12, 17, 0))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_MILESTONE_DUPLICATE, ex.getCode());
    }

    @Test
    void blocksOtherEventOverlappingMilestone() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.WORKSHOP))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByHackathonIdAndType(1, EventType.PRESENTATION))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByHackathonIdAndType(1, EventType.AWARDS))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findOverlapping(eq(1), eq(EventType.KICKOFF), any(), any(), eq(0)))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findOtherOverlapping(eq(1), any(), any(), eq(0)))
                .thenReturn(List.of(Event.builder()
                        .id(99)
                        .type(EventType.OTHER)
                        .startsAt(LocalDateTime.of(2026, 4, 11, 15, 0))
                        .endsAt(LocalDateTime.of(2026, 4, 11, 16, 0))
                        .build()));

        CreateEventRequest req = CreateEventRequest.builder()
                .title("KO")
                .type(EventType.KICKOFF)
                .location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 11, 14, 0))
                .endsAt(LocalDateTime.of(2026, 4, 11, 17, 0))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_CONFLICTS_WITH_MILESTONE, ex.getCode());
    }

    @Test
    void blocksOtherEventWhenOverlappingExistingKickoff() {
        stubMilestoneStubs(EventType.OTHER);
        when(eventRepository.findMilestoneOverlapping(
                eq(1), eq(EventTimeline.MILESTONE_TYPES), any(), any(), eq(0)))
                .thenReturn(List.of(kickoff(10, LocalDateTime.of(2026, 4, 11, 14, 0),
                        LocalDateTime.of(2026, 4, 11, 17, 0))));

        CreateEventRequest req = CreateEventRequest.builder()
                .title("Họp phụ")
                .type(EventType.OTHER)
                .location("Room")
                .startsAt(LocalDateTime.of(2026, 4, 11, 15, 0))
                .endsAt(LocalDateTime.of(2026, 4, 11, 16, 0))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_CONFLICTS_WITH_MILESTONE, ex.getCode());
    }

    @Test
    void blocksPresentationBeforeKickoffEnds() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff(10, LocalDateTime.of(2026, 4, 11, 14, 0),
                        LocalDateTime.of(2026, 4, 11, 17, 0))));
        when(eventRepository.findByHackathonIdAndType(1, EventType.WORKSHOP))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByHackathonIdAndType(1, EventType.PRESENTATION))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByHackathonIdAndType(1, EventType.AWARDS))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findOverlapping(eq(1), eq(EventType.PRESENTATION), any(), any(), eq(0)))
                .thenReturn(Collections.emptyList());

        CreateEventRequest req = CreateEventRequest.builder()
                .title("Thi")
                .type(EventType.PRESENTATION)
                .location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 11, 16, 0))
                .endsAt(LocalDateTime.of(2026, 4, 11, 19, 0))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_ORDER_VIOLATION, ex.getCode());
    }

    @Test
    void blocksAwardsBeforePresentationEnds() {
        Event presentation = Event.builder()
                .id(20)
                .type(EventType.PRESENTATION)
                .startsAt(LocalDateTime.of(2026, 4, 12, 8, 0))
                .endsAt(LocalDateTime.of(2026, 4, 12, 19, 0))
                .build();
        when(eventRepository.findByHackathonIdAndType(1, EventType.WORKSHOP))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByHackathonIdAndType(1, EventType.PRESENTATION))
                .thenReturn(List.of(presentation));
        when(eventRepository.findByHackathonIdAndType(1, EventType.AWARDS))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findOverlapping(eq(1), eq(EventType.AWARDS), any(), any(), eq(0)))
                .thenReturn(Collections.emptyList());

        CreateEventRequest req = CreateEventRequest.builder()
                .title("Trao giải")
                .type(EventType.AWARDS)
                .location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 12, 18, 0))
                .endsAt(LocalDateTime.of(2026, 4, 12, 20, 0))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_ORDER_VIOLATION, ex.getCode());
    }

    private void stubMilestoneStubs(EventType except) {
        for (EventType type : EventType.values()) {
            if (type == except || type == EventType.OTHER) {
                continue;
            }
            if (type == EventType.WORKSHOP || type == EventType.KICKOFF
                    || type == EventType.PRESENTATION || type == EventType.AWARDS) {
                when(eventRepository.findByHackathonIdAndType(1, type))
                        .thenReturn(Collections.emptyList());
            }
        }
        when(eventRepository.findOtherOverlapping(eq(1), any(), any(), eq(0)))
                .thenReturn(Collections.emptyList());
    }

    private static Event kickoff(int id, LocalDateTime start, LocalDateTime end) {
        return Event.builder()
                .id(id)
                .type(EventType.KICKOFF)
                .startsAt(start)
                .endsAt(end)
                .build();
    }

    private static CreateEventRequest workshop(LocalDateTime start, LocalDateTime end) {
        return CreateEventRequest.builder()
                .title("WS")
                .type(EventType.WORKSHOP)
                .location("Online")
                .startsAt(start)
                .endsAt(end)
                .build();
    }
}
