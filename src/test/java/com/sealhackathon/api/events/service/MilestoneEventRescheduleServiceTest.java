package com.sealhackathon.api.events.service;

import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.rounds.entity.Round;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MilestoneEventRescheduleServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private HackathonRepository hackathonRepository;

    @InjectMocks private MilestoneEventRescheduleService service;

    @Test
    void repositionWorkshopKickoff_updatesExisting() {
        LocalDate regEnd = LocalDate.of(2026, 7, 20);
        Hackathon h = Hackathon.builder()
                .id(1)
                .registrationEnd(regEnd)
                .eventStart(regEnd.plusDays(3))
                .eventEnd(regEnd.plusDays(4))
                .build();
        Event ws = Event.builder()
                .id(10)
                .type(EventType.WORKSHOP)
                .startsAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .endsAt(LocalDateTime.of(2026, 8, 1, 11, 0))
                .build();
        Event ko = Event.builder()
                .id(11)
                .type(EventType.KICKOFF)
                .startsAt(LocalDateTime.of(2026, 8, 2, 10, 0))
                .endsAt(LocalDateTime.of(2026, 8, 2, 12, 0))
                .build();
        when(eventRepository.findByHackathonIdAndType(1, EventType.WORKSHOP)).thenReturn(List.of(ws));
        when(eventRepository.findByHackathonIdAndType(1, EventType.KICKOFF)).thenReturn(List.of(ko));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int n = service.repositionWorkshopKickoff(h);

        assertThat(n).isEqualTo(2);
        assertThat(ws.getStartsAt()).isEqualTo(LocalDateTime.of(2026, 7, 21, 20, 0));
        assertThat(ko.getStartsAt()).isEqualTo(LocalDateTime.of(2026, 7, 22, 14, 0));
        assertThat(ws.getReminderSentAt()).isNull();
    }

    @Test
    void repositionAwards_pushesAfterFinalDeadline() {
        Hackathon h = Hackathon.builder()
                .id(1)
                .eventEnd(LocalDate.of(2026, 7, 24))
                .build();
        Round finalR = Round.builder()
                .id(4)
                .isFinal(true)
                .examAt(LocalDateTime.of(2026, 7, 24, 22, 0))
                .submissionDeadline(LocalDateTime.of(2026, 7, 24, 23, 30))
                .codingDurationHours(2)
                .build();
        Event awards = Event.builder()
                .id(12)
                .type(EventType.AWARDS)
                .startsAt(LocalDateTime.of(2026, 7, 24, 17, 30))
                .endsAt(LocalDateTime.of(2026, 7, 24, 19, 0))
                .build();
        when(eventRepository.findByHackathonIdAndType(1, EventType.AWARDS)).thenReturn(List.of(awards));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(hackathonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int n = service.repositionAwardsAfterFinal(h, finalR);

        assertThat(n).isEqualTo(1);
        assertThat(awards.getStartsAt()).isEqualTo(LocalDateTime.of(2026, 7, 25, 0, 0));
        assertThat(awards.getEndsAt()).isEqualTo(LocalDateTime.of(2026, 7, 25, 1, 30));
        assertThat(h.getEventEnd()).isEqualTo(LocalDate.of(2026, 7, 25));
        verify(hackathonRepository).save(h);
    }
}
