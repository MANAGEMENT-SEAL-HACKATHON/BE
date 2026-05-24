package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HackathonTimelineServiceImplTest {

    @Mock private EventRepository eventRepository;
    @Mock private RoundRepository roundRepository;
    @Mock private HackathonRepository hackathonRepository;

    @InjectMocks
    private HackathonTimelineServiceImpl timelineService;

    @BeforeEach
    void setUpHackathon() {
        Hackathon h = Hackathon.builder()
                .id(1)
                .eventStart(LocalDate.of(2026, 4, 11))
                .eventEnd(LocalDate.of(2026, 4, 12))
                .build();
        when(hackathonRepository.findById(1)).thenReturn(Optional.of(h));
    }

    @Test
    void prelimExamAt_outsidePresentationWindow_isBlocked() {
        when(eventRepository.findLatestByType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff()));
        when(eventRepository.findByHackathonIdAndType(1, EventType.PRESENTATION))
                .thenReturn(List.of(presentation()));

        LocalDateTime examAt = LocalDateTime.of(2026, 4, 12, 5, 0);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> timelineService.validateRoundExamAt(1, false, examAt));
        assertEquals(ErrorCode.ROUND_EXAM_OUTSIDE_PRESENTATION, ex.getCode());
    }

    @Test
    void prelimExamAt_withoutPresentation_isAllowedWithinEventWindow() {
        when(eventRepository.findLatestByType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff()));
        when(eventRepository.findByHackathonIdAndType(1, EventType.PRESENTATION))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> timelineService.validateRoundExamAt(1, false,
                LocalDateTime.of(2026, 4, 12, 8, 0)));
    }

    @Test
    void examAt_outsideEventWindow_isBlocked() {
        when(eventRepository.findLatestByType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff()));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> timelineService.validateRoundExamAt(1, false,
                        LocalDateTime.of(2026, 4, 13, 8, 0)));
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON, ex.getCode());
    }

    @Test
    void finalExamAt_beforeAwardsStart_isAllowed() {
        when(eventRepository.findLatestByType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff()));
        when(eventRepository.findByHackathonIdAndType(1, EventType.AWARDS))
                .thenReturn(List.of(Event.builder()
                        .id(3)
                        .type(EventType.AWARDS)
                        .startsAt(LocalDateTime.of(2026, 4, 12, 17, 30))
                        .endsAt(LocalDateTime.of(2026, 4, 12, 19, 0))
                        .build()));

        assertDoesNotThrow(() -> timelineService.validateRoundExamAt(1, true,
                LocalDateTime.of(2026, 4, 12, 12, 0)));
    }

    @Test
    void finalExamAt_atOrAfterAwardsStart_isBlocked() {
        when(eventRepository.findLatestByType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff()));
        LocalDateTime awardsStart = LocalDateTime.of(2026, 4, 12, 17, 30);
        when(eventRepository.findByHackathonIdAndType(1, EventType.AWARDS))
                .thenReturn(List.of(Event.builder()
                        .id(3)
                        .type(EventType.AWARDS)
                        .startsAt(awardsStart)
                        .endsAt(LocalDateTime.of(2026, 4, 12, 19, 0))
                        .build()));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> timelineService.validateRoundExamAt(1, true, awardsStart));
        assertEquals(ErrorCode.ROUND_EXAM_OUTSIDE_AWARDS, ex.getCode());
    }

    @Test
    void finalExamAt_withoutAwardsEvent_isBlocked() {
        when(eventRepository.findLatestByType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff()));
        when(eventRepository.findByHackathonIdAndType(1, EventType.AWARDS))
                .thenReturn(List.of());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> timelineService.validateRoundExamAt(1, true,
                        LocalDateTime.of(2026, 4, 12, 12, 0)));
        assertEquals(ErrorCode.EVENT_AWARDS_MISSING, ex.getCode());
    }

    @Test
    void collectRoundExamAtViolations_includesRoundContext() {
        when(eventRepository.findLatestByType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff()));
        when(eventRepository.findByHackathonIdAndType(1, EventType.PRESENTATION))
                .thenReturn(List.of(presentation()));
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(1))
                .thenReturn(List.of(Round.builder()
                        .id(5)
                        .name("Sơ loại")
                        .isFinal(false)
                        .examAt(LocalDateTime.of(2026, 4, 12, 5, 0))
                        .build()));

        List<BusinessRuleException> violations = timelineService.collectRoundExamAtViolations(1);
        assertEquals(1, violations.size());
        assertEquals(ErrorCode.ROUND_EXAM_OUTSIDE_PRESENTATION, violations.get(0).getCode());
    }

    private static Event kickoff() {
        return Event.builder()
                .id(1)
                .type(EventType.KICKOFF)
                .startsAt(LocalDateTime.of(2026, 4, 11, 14, 0))
                .endsAt(LocalDateTime.of(2026, 4, 11, 17, 0))
                .build();
    }

    private static Event presentation() {
        return Event.builder()
                .id(2)
                .type(EventType.PRESENTATION)
                .startsAt(LocalDateTime.of(2026, 4, 12, 6, 0))
                .endsAt(LocalDateTime.of(2026, 4, 12, 17, 0))
                .build();
    }
}
