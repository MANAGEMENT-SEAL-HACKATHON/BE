package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HackathonTimelineServiceImplTest {

    @Mock private EventRepository eventRepository;
    @Mock private RoundRepository roundRepository;

    @InjectMocks
    private HackathonTimelineServiceImpl timelineService;

    @Test
    void prelimExamAt_mustBeInsidePresentationWindow() {
        when(eventRepository.findLatestByType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff()));
        when(eventRepository.findByHackathonIdAndType(1, EventType.PRESENTATION))
                .thenReturn(List.of(presentation()));

        LocalDateTime examAt = LocalDateTime.of(2026, 4, 13, 10, 0);
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> timelineService.validateRoundExamAt(1, false, examAt));
        assertEquals(ErrorCode.ROUND_EXAM_OUTSIDE_PRESENTATION, ex.getCode());
    }

    @Test
    void finalExamAt_atAwardsStart_isAllowed() {
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

        timelineService.validateRoundExamAt(1, true, awardsStart);
    }

    @Test
    void finalExamAt_afterAwardsEnd_isBlocked() {
        when(eventRepository.findLatestByType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff()));
        when(eventRepository.findByHackathonIdAndType(1, EventType.AWARDS))
                .thenReturn(List.of(Event.builder()
                        .id(3)
                        .type(EventType.AWARDS)
                        .startsAt(LocalDateTime.of(2026, 4, 12, 17, 30))
                        .endsAt(LocalDateTime.of(2026, 4, 12, 19, 0))
                        .build()));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> timelineService.validateRoundExamAt(1, true,
                        LocalDateTime.of(2026, 4, 12, 20, 0)));
        assertEquals(ErrorCode.ROUND_EXAM_OUTSIDE_AWARDS, ex.getCode());
    }

    @Test
    void prelimExamAt_withoutPresentationEvent_isBlocked() {
        when(eventRepository.findLatestByType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff()));
        when(eventRepository.findByHackathonIdAndType(1, EventType.PRESENTATION))
                .thenReturn(List.of());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> timelineService.validateRoundExamAt(1, false,
                        LocalDateTime.of(2026, 4, 12, 8, 0)));
        assertEquals(ErrorCode.EVENT_PRESENTATION_MISSING, ex.getCode());
    }

    @Test
    void finalExamAt_withoutAwardsEvent_isBlocked() {
        when(eventRepository.findLatestByType(1, EventType.KICKOFF))
                .thenReturn(List.of(kickoff()));
        when(eventRepository.findByHackathonIdAndType(1, EventType.AWARDS))
                .thenReturn(List.of());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> timelineService.validateRoundExamAt(1, true,
                        LocalDateTime.of(2026, 4, 12, 17, 30)));
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
                        .examAt(LocalDateTime.of(2026, 4, 13, 10, 0))
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
