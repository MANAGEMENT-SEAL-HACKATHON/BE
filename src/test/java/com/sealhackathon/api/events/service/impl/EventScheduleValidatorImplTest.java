package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.dto.request.CreateEventRequest;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
    void blocksWorkshopAfterKickoff() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(List.of(Event.builder()
                        .id(10)
                        .type(EventType.KICKOFF)
                        .startsAt(LocalDateTime.of(2026, 4, 11, 14, 0))
                        .build()));

        CreateEventRequest req = CreateEventRequest.builder()
                .title("WS")
                .type(EventType.WORKSHOP)
                .location("Online")
                .startsAt(LocalDateTime.of(2026, 4, 11, 20, 0))
                .endsAt(LocalDateTime.of(2026, 4, 11, 21, 0))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_ORDER_VIOLATION, ex.getCode());
    }
}
