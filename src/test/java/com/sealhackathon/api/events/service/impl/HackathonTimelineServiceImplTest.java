package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.support.EventTimeline;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.rounds.value_object.RoundType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * examAt chỉ ràng buộc với KICKOFF và [eventStart, eventEnd].
 * Không còn ràng buộc với PRESENTATION hay AWARDS.
 * Circular-delete AWARDS↔Final round đã được fix.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class HackathonTimelineServiceImplTest {

    @Mock EventRepository eventRepository;
    @Mock RoundRepository roundRepository;
    @Mock HackathonRepository hackathonRepository;

    HackathonTimelineServiceImpl service;

    Hackathon hackathon;
    static final int HACKATHON_ID = 1;

    @BeforeEach
    void setUp() {
        service = new HackathonTimelineServiceImpl(eventRepository, roundRepository, hackathonRepository);
        hackathon = Hackathon.builder()
                .id(HACKATHON_ID)
                .eventStart(LocalDate.of(2026, 4, 11))
                .eventEnd(LocalDate.of(2026, 4, 12))
                .build();

        when(hackathonRepository.findById(HACKATHON_ID)).thenReturn(Optional.of(hackathon));
        when(eventRepository.findByHackathonIdAndType(HACKATHON_ID, EventType.KICKOFF))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByHackathonIdAndType(HACKATHON_ID, EventType.AWARDS))
                .thenReturn(Collections.emptyList());
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(HACKATHON_ID))
                .thenReturn(Collections.emptyList());
    }

    // ------------ examAt before kickoff (ROUND_EXAM_BEFORE_KICKOFF) ------------

    @Test
    void examAt_afterKickoff_passes() {
        mockKickoff(LocalDateTime.of(2026, 4, 11, 14, 0), LocalDateTime.of(2026, 4, 11, 17, 0));

        assertDoesNotThrow(() ->
                service.validateRoundExamAt(HACKATHON_ID, false, LocalDateTime.of(2026, 4, 12, 9, 0)));
    }

    @Test
    void examAt_beforeKickoffEnd_blocked() {
        mockKickoff(LocalDateTime.of(2026, 4, 11, 14, 0), LocalDateTime.of(2026, 4, 11, 17, 0));

        assertEquals(ErrorCode.ROUND_EXAM_BEFORE_KICKOFF,
                assertThrows(BusinessRuleException.class, () ->
                        service.validateRoundExamAt(HACKATHON_ID, false,
                                LocalDateTime.of(2026, 4, 11, 16, 0))).getCode());
    }

    @Test
    void examAt_exactlyKickoffEnd_blocked() {
        mockKickoff(LocalDateTime.of(2026, 4, 11, 14, 0), LocalDateTime.of(2026, 4, 11, 17, 0));

        assertEquals(ErrorCode.ROUND_EXAM_BEFORE_KICKOFF,
                assertThrows(BusinessRuleException.class, () ->
                        service.validateRoundExamAt(HACKATHON_ID, false,
                                LocalDateTime.of(2026, 4, 11, 17, 0))).getCode());
    }

    // ------------ examAt outside [eventStart, eventEnd] ------------

    @Test
    void examAt_beforeEventStart_blocked() {
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON,
                assertThrows(BusinessRuleException.class, () ->
                        service.validateRoundExamAt(HACKATHON_ID, false,
                                LocalDateTime.of(2026, 4, 10, 10, 0))).getCode());
    }

    @Test
    void examAt_afterEventEnd_blocked() {
        assertEquals(ErrorCode.EVENT_OUT_OF_HACKATHON,
                assertThrows(BusinessRuleException.class, () ->
                        service.validateRoundExamAt(HACKATHON_ID, false,
                                LocalDateTime.of(2026, 4, 13, 10, 0))).getCode());
    }

    @Test
    void examAt_withinEventWindow_noKickoff_passes() {
        assertDoesNotThrow(() ->
                service.validateRoundExamAt(HACKATHON_ID, false, LocalDateTime.of(2026, 4, 11, 10, 0)));
    }

    // ------------ AWARDS does NOT block examAt (circular-delete fix) ------------

    @Test
    void finalRound_noAwards_passes() {
        // Xóa AWARDS → Awards list empty → không throw EVENT_AWARDS_MISSING
        assertDoesNotThrow(() ->
                service.validateRoundExamAt(HACKATHON_ID, true, LocalDateTime.of(2026, 4, 12, 9, 0)));
    }

    @Test
    void finalRound_examBeforeAwards_passes() {
        mockAwards(LocalDateTime.of(2026, 4, 12, 17, 30), LocalDateTime.of(2026, 4, 12, 19, 0));

        assertDoesNotThrow(() ->
                service.validateRoundExamAt(HACKATHON_ID, true, LocalDateTime.of(2026, 4, 12, 10, 0)));
    }

    @Test
    void finalRound_examAfterAwardsStart_blocked() {
        mockAwards(LocalDateTime.of(2026, 4, 12, 17, 30), LocalDateTime.of(2026, 4, 12, 19, 0));

        assertEquals(ErrorCode.ROUND_EXAM_OUTSIDE_AWARDS,
                assertThrows(BusinessRuleException.class, () ->
                        service.validateRoundExamAt(HACKATHON_ID, true,
                                LocalDateTime.of(2026, 4, 12, 18, 0))).getCode());
    }

    // ------------ collectViolations ------------

    @Test
    void collectViolations_noRounds_empty() {
        assertTrue(service.collectRoundExamAtViolations(HACKATHON_ID).isEmpty());
    }

    @Test
    void collectViolations_roundBeforeKickoff_collectedNotThrown() {
        mockKickoff(LocalDateTime.of(2026, 4, 11, 14, 0), LocalDateTime.of(2026, 4, 11, 17, 0));

        Round round = Round.builder().id(1).name("Sơ loại").roundType(RoundType.PRELIMINARY)
                .examAt(LocalDateTime.of(2026, 4, 11, 16, 0)).isFinal(false).build();
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(HACKATHON_ID))
                .thenReturn(List.of(round));

        List<BusinessRuleException> violations = service.collectRoundExamAtViolations(HACKATHON_ID);
        assertEquals(1, violations.size());
        assertEquals(ErrorCode.ROUND_EXAM_BEFORE_KICKOFF, violations.get(0).getCode());
    }

    @Test
    void assertAllRoundsExamAtValid_withViolation_throws() {
        mockKickoff(LocalDateTime.of(2026, 4, 11, 14, 0), LocalDateTime.of(2026, 4, 11, 17, 0));

        Round round = Round.builder().id(1).name("Sơ loại").roundType(RoundType.PRELIMINARY)
                .examAt(LocalDateTime.of(2026, 4, 11, 16, 0)).isFinal(false).build();
        when(roundRepository.findByHackathon_IdOrderByExamAtAsc(HACKATHON_ID))
                .thenReturn(List.of(round));

        assertThrows(BusinessRuleException.class,
                () -> service.assertAllRoundsExamAtValid(HACKATHON_ID));
    }

    // ------------ helpers ------------

    private void mockKickoff(LocalDateTime start, LocalDateTime end) {
        when(eventRepository.findLatestByType(HACKATHON_ID, EventType.KICKOFF))
                .thenReturn(List.of(Event.builder().id(1).type(EventType.KICKOFF)
                        .startsAt(start).endsAt(end).build()));
    }

    private void mockAwards(LocalDateTime start, LocalDateTime end) {
        when(eventRepository.findByHackathonIdAndType(HACKATHON_ID, EventType.AWARDS))
                .thenReturn(List.of(Event.builder().id(2).type(EventType.AWARDS)
                        .startsAt(start).endsAt(end).build()));
    }
}
