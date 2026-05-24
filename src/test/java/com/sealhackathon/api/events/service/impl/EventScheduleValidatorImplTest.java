package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.dto.request.CreateEventRequest;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.impl.window.AwardsWindowRule;
import com.sealhackathon.api.events.service.impl.window.KickoffWindowRule;
import com.sealhackathon.api.events.service.impl.window.PresentationWindowRule;
import com.sealhackathon.api.events.service.impl.window.WorkshopWindowRule;
import com.sealhackathon.api.events.support.EventTimeline;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit-tests cho 28 TC (TC-01 → TC-28) — kiểm tra rule mới ở Lớp 1 + giữ Lớp 2/3.
 * Validator được lắp tay trong {@code @BeforeEach} với các rule thật (không stub) để
 * cover dispatcher + rule logic.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventScheduleValidatorImplTest {

    @Mock private EventRepository eventRepository;
    @Mock private RoundRepository roundRepository;

    private EventScheduleValidatorImpl validator;
    private Hackathon hackathon;

    @BeforeEach
    void setUp() {
        WorkshopWindowRule workshop = new WorkshopWindowRule();
        KickoffWindowRule kickoff = new KickoffWindowRule();
        PresentationWindowRule presentation = new PresentationWindowRule(roundRepository);
        AwardsWindowRule awards = new AwardsWindowRule(eventRepository, roundRepository);
        validator = new EventScheduleValidatorImpl(
                eventRepository, workshop, kickoff, presentation, awards);
        validator.initRules();

        hackathon = Hackathon.builder()
                .id(1)
                .registrationStart(LocalDate.of(2026, 4, 1))
                .registrationEnd(LocalDate.of(2026, 4, 8))
                .eventStart(LocalDate.of(2026, 4, 11))
                .eventEnd(LocalDate.of(2026, 4, 12))
                .build();

        when(roundRepository.findByHackathon_IdAndIsFinalTrue(1)).thenReturn(Optional.empty());
        for (EventType t : EventType.values()) {
            when(eventRepository.findByHackathonIdAndType(1, t)).thenReturn(Collections.emptyList());
        }
        when(eventRepository.findOverlapping(eq(1), any(), any(), any(), eq(0)))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findOtherOverlapping(eq(1), any(), any(), eq(0)))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findMilestoneOverlapping(eq(1), any(), any(), any(), eq(0)))
                .thenReturn(Collections.emptyList());
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(1))
                .thenReturn(Collections.emptyList());
    }

    // ------------ COMMON ------------

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
    void blocksMilestoneWithoutEndsAt() {
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

    // ------------ WORKSHOP ------------

    @Test
    void workshop_inGapAfterRegEndBeforeEventStart_isAllowed() {
        CreateEventRequest req = workshop(
                LocalDateTime.of(2026, 4, 9, 20, 0),
                LocalDateTime.of(2026, 4, 9, 21, 30));
        assertDoesNotThrow(() -> validator.validateBlocking(hackathon, req, 0));
    }

    @Test
    void workshop_onRegistrationEndDay_isBlocked() {
        CreateEventRequest req = workshop(
                LocalDateTime.of(2026, 4, 8, 20, 0),
                LocalDateTime.of(2026, 4, 8, 21, 30));
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON, ex.getCode());
    }

    @Test
    void workshop_onEventStartDay_isBlocked() {
        CreateEventRequest req = workshop(
                LocalDateTime.of(2026, 4, 11, 20, 0),
                LocalDateTime.of(2026, 4, 11, 21, 30));
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON, ex.getCode());
    }

    // ------------ KICKOFF ------------

    @Test
    void kickoff_onEventStartDay_isAllowed() {
        CreateEventRequest req = CreateEventRequest.builder()
                .title("Khai mạc")
                .type(EventType.KICKOFF)
                .location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 11, 14, 0))
                .endsAt(LocalDateTime.of(2026, 4, 11, 17, 0))
                .build();
        assertDoesNotThrow(() -> validator.validateBlocking(hackathon, req, 0));
    }

    @Test
    void kickoff_notOnEventStart_isBlocked() {
        CreateEventRequest req = CreateEventRequest.builder()
                .title("Khai mạc")
                .type(EventType.KICKOFF)
                .location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 12, 9, 0))
                .endsAt(LocalDateTime.of(2026, 4, 12, 11, 0))
                .build();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON, ex.getCode());
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
                .startsAt(LocalDateTime.of(2026, 4, 11, 14, 0))
                .endsAt(LocalDateTime.of(2026, 4, 11, 17, 0))
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_MILESTONE_DUPLICATE, ex.getCode());
    }

    // ------------ PRESENTATION ------------

    @Test
    void presentation_withinEventWindow_isAllowed() {
        CreateEventRequest req = CreateEventRequest.builder()
                .title("Thuyết trình")
                .type(EventType.PRESENTATION)
                .location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 12, 13, 0))
                .endsAt(LocalDateTime.of(2026, 4, 12, 16, 0))
                .build();
        assertDoesNotThrow(() -> validator.validateBlocking(hackathon, req, 0));
    }

    @Test
    void presentation_beforeEventStart_isBlocked() {
        CreateEventRequest req = CreateEventRequest.builder()
                .title("Thuyết trình")
                .type(EventType.PRESENTATION)
                .location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 10, 13, 0))
                .endsAt(LocalDateTime.of(2026, 4, 10, 16, 0))
                .build();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON, ex.getCode());
    }

    @Test
    void presentation_beforeFinalExamAt_isBlocked() {
        Round finalRound = Round.builder()
                .id(99)
                .isFinal(true)
                .examAt(LocalDateTime.of(2026, 4, 12, 14, 0))
                .build();
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(1)).thenReturn(Optional.of(finalRound));

        CreateEventRequest req = CreateEventRequest.builder()
                .title("Thuyết trình")
                .type(EventType.PRESENTATION)
                .location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 12, 13, 0))
                .endsAt(LocalDateTime.of(2026, 4, 12, 16, 0))
                .build();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.PRESENTATION_BEFORE_FINAL_EXAM, ex.getCode());
    }

    @Test
    void presentation_afterFinalExamAt_isAllowed() {
        Round finalRound = Round.builder()
                .id(99)
                .isFinal(true)
                .examAt(LocalDateTime.of(2026, 4, 12, 12, 0))
                .build();
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(1)).thenReturn(Optional.of(finalRound));

        CreateEventRequest req = CreateEventRequest.builder()
                .title("Thuyết trình")
                .type(EventType.PRESENTATION)
                .location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 12, 13, 0))
                .endsAt(LocalDateTime.of(2026, 4, 12, 16, 0))
                .build();
        assertDoesNotThrow(() -> validator.validateBlocking(hackathon, req, 0));
    }

    // ------------ AWARDS ------------

    @Test
    void awards_notOnEventEnd_isBlocked() {
        CreateEventRequest req = awards(
                LocalDateTime.of(2026, 4, 11, 17, 30),
                LocalDateTime.of(2026, 4, 11, 19, 0));
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON, ex.getCode());
    }

    @Test
    void awards_withoutCompetition_isBlocked() {
        CreateEventRequest req = awards(
                LocalDateTime.of(2026, 4, 12, 17, 30),
                LocalDateTime.of(2026, 4, 12, 19, 0));
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.AWARDS_NEEDS_COMPETITION, ex.getCode());
    }

    @Test
    void awards_beforeRoundExamAt_isBlocked() {
        Round prelim = Round.builder()
                .id(7)
                .isFinal(false)
                .examAt(LocalDateTime.of(2026, 4, 12, 18, 0))
                .build();
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(1)).thenReturn(List.of(prelim));

        CreateEventRequest req = awards(
                LocalDateTime.of(2026, 4, 12, 17, 30),
                LocalDateTime.of(2026, 4, 12, 19, 0));
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        assertEquals(ErrorCode.AWARDS_BEFORE_COMPETITION_END, ex.getCode());
    }

    @Test
    void awards_afterAllExamAt_isAllowed() {
        Round prelim = Round.builder()
                .id(7)
                .isFinal(false)
                .examAt(LocalDateTime.of(2026, 4, 12, 12, 0))
                .build();
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(1)).thenReturn(List.of(prelim));

        CreateEventRequest req = awards(
                LocalDateTime.of(2026, 4, 12, 17, 30),
                LocalDateTime.of(2026, 4, 12, 19, 0));
        assertDoesNotThrow(() -> validator.validateBlocking(hackathon, req, 0));
    }

    @Test
    void awards_afterPresentation_isAllowed() {
        Event presentation = Event.builder()
                .id(20)
                .type(EventType.PRESENTATION)
                .startsAt(LocalDateTime.of(2026, 4, 12, 13, 0))
                .endsAt(LocalDateTime.of(2026, 4, 12, 16, 0))
                .build();
        when(eventRepository.findByHackathonIdAndType(1, EventType.PRESENTATION))
                .thenReturn(List.of(presentation));

        CreateEventRequest req = awards(
                LocalDateTime.of(2026, 4, 12, 17, 30),
                LocalDateTime.of(2026, 4, 12, 19, 0));
        assertDoesNotThrow(() -> validator.validateBlocking(hackathon, req, 0));
    }

    // ------------ LAYER 2 / 3 (giữ behavior cũ) ------------

    @Test
    void blocksWorkshopOverlappingKickoff_layer3Order() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff(10, LocalDateTime.of(2026, 4, 11, 14, 0),
                        LocalDateTime.of(2026, 4, 11, 17, 0))));

        CreateEventRequest req = workshop(
                LocalDateTime.of(2026, 4, 11, 13, 0),
                LocalDateTime.of(2026, 4, 11, 16, 0));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> validator.validateBlocking(hackathon, req, 0));
        // ngày 11 vi phạm WORKSHOP window (=eventStart) trước khi đến layer 3
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON, ex.getCode());
    }

    @Test
    void blocksOtherEventOverlappingMilestone() {
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

    // ------------ helpers ------------

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

    private static CreateEventRequest awards(LocalDateTime start, LocalDateTime end) {
        return CreateEventRequest.builder()
                .title("Trao giải")
                .type(EventType.AWARDS)
                .location("Hall")
                .startsAt(start)
                .endsAt(end)
                .build();
    }
}
