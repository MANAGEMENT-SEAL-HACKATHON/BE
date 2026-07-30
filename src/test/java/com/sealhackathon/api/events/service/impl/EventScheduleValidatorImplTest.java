package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.dto.request.CreateEventRequest;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.impl.window.AwardsWindowRule;
import com.sealhackathon.api.events.service.impl.window.KickoffWindowRule;
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
 * POST order: KICKOFF → WORKSHOP → AWARDS. Trên lịch: WORKSHOP → KICKOFF → AWARDS.
 * PRESENTATION không còn là milestone — validate như event phụ trong [eventStart, eventEnd].
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
        WorkshopWindowRule workshop = new WorkshopWindowRule(eventRepository);
        KickoffWindowRule kickoff = new KickoffWindowRule();
        AwardsWindowRule awards = new AwardsWindowRule();
        validator = new EventScheduleValidatorImpl(eventRepository, roundRepository, workshop, kickoff, awards);
        validator.initRules();
        when(roundRepository.findPreliminaryLikeByHackathonId(1)).thenReturn(Collections.emptyList());

        hackathon = Hackathon.builder()
                .id(1)
                .registrationStart(LocalDate.of(2026, 4, 1))
                .registrationEnd(LocalDate.of(2026, 4, 8))
                .eventStart(LocalDate.of(2026, 4, 11))
                .eventEnd(LocalDate.of(2026, 4, 12))
                .build();

        for (EventType t : EventType.values()) {
            when(eventRepository.findByHackathonIdAndType(1, t)).thenReturn(Collections.emptyList());
        }
        when(eventRepository.findOverlapping(eq(1), any(), any(), any(), eq(0)))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findOtherOverlapping(eq(1), any(), any(), eq(0)))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findMilestoneOverlapping(eq(1), any(), any(), any(), eq(0)))
                .thenReturn(Collections.emptyList());
    }

    // ------------ COMMON ------------

    @Test
    void blocksWhenLocationAndMeetUrlBothMissing() {
        CreateEventRequest req = CreateEventRequest.builder()
                .title("T").type(EventType.WORKSHOP)
                .startsAt(LocalDateTime.of(2026, 4, 9, 20, 0))
                .endsAt(LocalDateTime.of(2026, 4, 9, 21, 0))
                .build();

        assertEquals(ErrorCode.EVENT_LOCATION_REQUIRED,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon, req, 0)).getCode());
    }

    @Test
    void blocksMilestoneWithoutEndsAt() {
        CreateEventRequest req = CreateEventRequest.builder()
                .title("KO").type(EventType.KICKOFF).location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 10, 14, 0))
                .build();

        assertEquals(ErrorCode.EVENT_END_REQUIRED,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon, req, 0)).getCode());
    }

    // ------------ WORKSHOP ------------

    @Test
    void workshop_inGapAfterRegEndBeforeEventStart_isAllowed() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoffEvent(5,
                        LocalDateTime.of(2026, 4, 11, 14, 0),
                        LocalDateTime.of(2026, 4, 11, 17, 0))));
        assertDoesNotThrow(() -> validator.validateBlocking(hackathon,
                workshop(LocalDateTime.of(2026, 4, 9, 20, 0),
                        LocalDateTime.of(2026, 4, 9, 21, 30)), 0));
    }

    @Test
    void workshop_withoutKickoff_isBlocked() {
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon,
                                workshop(LocalDateTime.of(2026, 4, 10, 20, 0),
                                        LocalDateTime.of(2026, 4, 10, 21, 30)), 0)).getCode());
    }

    @Test
    void workshop_onRegistrationEndDay_isBlocked() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoffEvent(5,
                        LocalDateTime.of(2026, 4, 11, 14, 0),
                        LocalDateTime.of(2026, 4, 11, 17, 0))));
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon,
                                workshop(LocalDateTime.of(2026, 4, 8, 20, 0),
                                        LocalDateTime.of(2026, 4, 8, 21, 30)), 0)).getCode());
    }

    @Test
    void workshop_onEventStartDay_isBlocked() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoffEvent(5,
                        LocalDateTime.of(2026, 4, 11, 14, 0),
                        LocalDateTime.of(2026, 4, 11, 17, 0))));
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon,
                                workshop(LocalDateTime.of(2026, 4, 11, 20, 0),
                                        LocalDateTime.of(2026, 4, 11, 21, 30)), 0)).getCode());
    }

    // ------------ KICKOFF ------------

    @Test
    void kickoff_inGapBeforeEventStart_isAllowed() {
        assertDoesNotThrow(() -> validator.validateBlocking(hackathon,
                kickoff(LocalDateTime.of(2026, 4, 10, 14, 0),
                        LocalDateTime.of(2026, 4, 10, 17, 0)), 0));
    }

    @Test
    void kickoff_onEventStartDay_isBlocked() {
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon,
                                kickoff(LocalDateTime.of(2026, 4, 11, 14, 0),
                                        LocalDateTime.of(2026, 4, 11, 17, 0)), 0)).getCode());
    }

    @Test
    void kickoff_onOrBeforeRegistrationEnd_isBlocked() {
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon,
                                kickoff(LocalDateTime.of(2026, 4, 8, 14, 0),
                                        LocalDateTime.of(2026, 4, 8, 17, 0)), 0)).getCode());
    }

    @Test
    void blocksDuplicateKickoff() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoffEvent(10,
                        LocalDateTime.of(2026, 4, 10, 14, 0),
                        LocalDateTime.of(2026, 4, 10, 17, 0))));

        assertEquals(ErrorCode.EVENT_MILESTONE_DUPLICATE,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon,
                                kickoff(LocalDateTime.of(2026, 4, 10, 14, 0),
                                        LocalDateTime.of(2026, 4, 10, 17, 0)), 0)).getCode());
    }

    // ------------ AWARDS ------------

    @Test
    void awards_onEventEnd_isAllowed() {
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(1))
                .thenReturn(Optional.of(Round.builder()
                        .id(99)
                        .submissionDeadline(LocalDateTime.of(2026, 4, 12, 16, 30))
                        .build()));
        assertDoesNotThrow(() -> validator.validateBlocking(hackathon,
                awards(LocalDateTime.of(2026, 4, 12, 17, 30),
                        LocalDateTime.of(2026, 4, 12, 19, 0)), 0));
    }

    @Test
    void awards_beforeFinalSubmissionDeadline_isBlocked() {
        when(roundRepository.findByHackathon_IdAndIsFinalTrue(1))
                .thenReturn(Optional.of(Round.builder()
                        .id(99)
                        .submissionDeadline(LocalDateTime.of(2026, 6, 10, 16, 30))
                        .publishedAt(LocalDateTime.of(2026, 6, 10, 16, 30))
                        .build()));
        Hackathon h = Hackathon.builder()
                .id(1)
                .registrationStart(LocalDate.of(2026, 5, 24))
                .registrationEnd(LocalDate.of(2026, 6, 5))
                .eventStart(LocalDate.of(2026, 6, 10))
                .eventEnd(LocalDate.of(2026, 6, 10))
                .build();

        assertEquals(ErrorCode.AWARDS_BEFORE_FINAL_DEADLINE,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(h,
                                awards(LocalDateTime.of(2026, 6, 10, 10, 0),
                                        LocalDateTime.of(2026, 6, 10, 11, 0)), 0)).getCode());
    }

    @Test
    void awards_notOnEventEnd_isBlocked() {
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon,
                                awards(LocalDateTime.of(2026, 4, 11, 17, 30),
                                        LocalDateTime.of(2026, 4, 11, 19, 0)), 0)).getCode());
    }

    @Test
    void awards_noRoundRequiredToCreate() {
        // Round không còn là điều kiện tiên quyết tạo AWARDS
        assertDoesNotThrow(() -> validator.validateBlocking(hackathon,
                awards(LocalDateTime.of(2026, 4, 12, 17, 30),
                        LocalDateTime.of(2026, 4, 12, 19, 0)), 0));
    }

    // ------------ PRESENTATION (như OTHER) ------------

    @Test
    void presentation_withinEventWindow_isAllowed() {
        // PRESENTATION không còn là milestone — không cần endsAt bắt buộc, validate như event phụ
        CreateEventRequest req = CreateEventRequest.builder()
                .title("Thuyết trình").type(EventType.PRESENTATION).location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 12, 8, 0))
                .endsAt(LocalDateTime.of(2026, 4, 12, 12, 0))
                .build();
        assertDoesNotThrow(() -> validator.validateBlocking(hackathon, req, 0));
    }

    @Test
    void presentation_beforeEventStart_isAllowed() {
        // PRESENTATION hoàn toàn không có validation — coordinator tự quản lý
        CreateEventRequest req = CreateEventRequest.builder()
                .title("Thuyết trình").type(EventType.PRESENTATION).location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 10, 8, 0))
                .endsAt(LocalDateTime.of(2026, 4, 10, 12, 0))
                .build();
        assertDoesNotThrow(() -> validator.validateBlocking(hackathon, req, 0));
    }

    @Test
    void presentation_doesNotNeedEndsAt() {
        // PRESENTATION không còn là milestone → EVENT_END_REQUIRED không áp dụng
        CreateEventRequest req = CreateEventRequest.builder()
                .title("Thuyết trình").type(EventType.PRESENTATION).location("Hall")
                .startsAt(LocalDateTime.of(2026, 4, 12, 8, 0))
                .build();
        assertDoesNotThrow(() -> validator.validateBlocking(hackathon, req, 0));
    }

    // ------------ LAYER 3 (lịch: WORKSHOP → KICKOFF → AWARDS) ------------

    @Test
    void kickoff_beforeAwardsStart_layer3_passes() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.AWARDS))
                .thenReturn(List.of(Event.builder().id(20).type(EventType.AWARDS)
                        .startsAt(LocalDateTime.of(2026, 4, 12, 17, 30))
                        .endsAt(LocalDateTime.of(2026, 4, 12, 19, 0)).build()));

        assertDoesNotThrow(() -> validator.validateBlocking(hackathon,
                kickoff(LocalDateTime.of(2026, 4, 10, 14, 0),
                        LocalDateTime.of(2026, 4, 10, 17, 0)), 0));
    }

    @Test
    void workshopAndKickoff_sameCalendarDay_blocked() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoffEvent(5,
                        LocalDateTime.of(2026, 4, 10, 14, 0),
                        LocalDateTime.of(2026, 4, 10, 17, 0))));

        assertEquals(ErrorCode.EVENT_ORDER_VIOLATION,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon,
                                workshop(LocalDateTime.of(2026, 4, 10, 20, 0),
                                        LocalDateTime.of(2026, 4, 10, 21, 30)), 0)).getCode());
    }

    @Test
    void workshopAndKickoff_differentDays_passes() {
        when(eventRepository.findByHackathonIdAndType(1, EventType.WORKSHOP))
                .thenReturn(List.of(Event.builder().id(5).type(EventType.WORKSHOP)
                        .startsAt(LocalDateTime.of(2026, 4, 9, 20, 0))
                        .endsAt(LocalDateTime.of(2026, 4, 9, 21, 30)).build()));

        assertDoesNotThrow(() -> validator.validateBlocking(hackathon,
                kickoff(LocalDateTime.of(2026, 4, 10, 14, 0),
                        LocalDateTime.of(2026, 4, 10, 17, 0)), 0));
    }

    // ------------ LAYER 2 overlap OTHER ↔ milestone ------------

    @Test
    void blocksOtherEventOverlappingMilestone() {
        when(eventRepository.findOtherOverlapping(eq(1), any(), any(), eq(0)))
                .thenReturn(List.of(Event.builder().id(99).type(EventType.OTHER)
                        .startsAt(LocalDateTime.of(2026, 4, 11, 15, 0))
                        .endsAt(LocalDateTime.of(2026, 4, 11, 16, 0)).build()));

        assertEquals(ErrorCode.EVENT_CONFLICTS_WITH_MILESTONE,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon,
                                kickoff(LocalDateTime.of(2026, 4, 10, 14, 0),
                                        LocalDateTime.of(2026, 4, 10, 17, 0)), 0)).getCode());
    }

    @Test
    void blocksOtherWhenOverlappingExistingKickoff() {
        when(eventRepository.findMilestoneOverlapping(
                eq(1), eq(EventTimeline.MILESTONE_TYPES), any(), any(), eq(0)))
                .thenReturn(List.of(kickoffEvent(10,
                        LocalDateTime.of(2026, 4, 10, 14, 0),
                        LocalDateTime.of(2026, 4, 10, 17, 0))));

        CreateEventRequest req = CreateEventRequest.builder()
                .title("Họp phụ").type(EventType.OTHER).location("Room")
                .startsAt(LocalDateTime.of(2026, 4, 11, 15, 0))
                .endsAt(LocalDateTime.of(2026, 4, 11, 16, 0)).build();

        assertEquals(ErrorCode.EVENT_CONFLICTS_WITH_MILESTONE,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon, req, 0)).getCode());
    }

    // ------------ BUFFET (KICKOFF) ------------

    @Test
    void allowsKickoffBuffetInsideEventWindow() {
        CreateEventRequest req = kickoff(
                LocalDateTime.of(2026, 4, 10, 8, 0),
                LocalDateTime.of(2026, 4, 10, 17, 0));
        req.setBuffetLocation("Canteen");
        req.setBuffetStartsAt(LocalDateTime.of(2026, 4, 10, 11, 0));
        req.setBuffetEndsAt(LocalDateTime.of(2026, 4, 10, 12, 30));

        assertDoesNotThrow(() -> validator.validateBlocking(hackathon, req, 0));
    }

    @Test
    void blocksBuffetOnNonKickoff() {
        CreateEventRequest req = workshop(
                LocalDateTime.of(2026, 4, 9, 20, 0),
                LocalDateTime.of(2026, 4, 9, 21, 0));
        req.setBuffetLocation("Canteen");

        assertEquals(ErrorCode.EVENT_BUFFET_NOT_KICKOFF,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon, req, 0)).getCode());
    }

    @Test
    void blocksBuffetOutsideEventWindow() {
        CreateEventRequest req = kickoff(
                LocalDateTime.of(2026, 4, 10, 8, 0),
                LocalDateTime.of(2026, 4, 10, 17, 0));
        req.setBuffetStartsAt(LocalDateTime.of(2026, 4, 10, 18, 0));
        req.setBuffetEndsAt(LocalDateTime.of(2026, 4, 10, 19, 0));

        assertEquals(ErrorCode.EVENT_BUFFET_OUT_OF_WINDOW,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon, req, 0)).getCode());
    }

    @Test
    void blocksBuffetEndsBeforeBuffetStarts() {
        CreateEventRequest req = kickoff(
                LocalDateTime.of(2026, 4, 10, 8, 0),
                LocalDateTime.of(2026, 4, 10, 17, 0));
        req.setBuffetStartsAt(LocalDateTime.of(2026, 4, 10, 12, 0));
        req.setBuffetEndsAt(LocalDateTime.of(2026, 4, 10, 11, 0));

        assertEquals(ErrorCode.EVENT_BUFFET_OUT_OF_WINDOW,
                assertThrows(BusinessRuleException.class,
                        () -> validator.validateBlocking(hackathon, req, 0)).getCode());
    }

    // ------------ helpers ------------

    private static Event kickoffEvent(int id, LocalDateTime start, LocalDateTime end) {
        return Event.builder().id(id).type(EventType.KICKOFF).startsAt(start).endsAt(end).build();
    }

    private static CreateEventRequest workshop(LocalDateTime start, LocalDateTime end) {
        return CreateEventRequest.builder().title("WS").type(EventType.WORKSHOP).location("Online")
                .startsAt(start).endsAt(end).build();
    }

    private static CreateEventRequest kickoff(LocalDateTime start, LocalDateTime end) {
        return CreateEventRequest.builder().title("KO").type(EventType.KICKOFF).location("Hall")
                .startsAt(start).endsAt(end).build();
    }

    private static CreateEventRequest awards(LocalDateTime start, LocalDateTime end) {
        return CreateEventRequest.builder().title("Trao giải").type(EventType.AWARDS).location("Hall")
                .startsAt(start).endsAt(end).build();
    }
}
