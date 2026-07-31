package com.sealhackathon.api.events.service;

import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.rounds.entity.Round;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MilestoneEventRescheduleServiceBuffetTest {

    @Mock private EventRepository eventRepository;
    @Mock private HackathonRepository hackathonRepository;
    @InjectMocks private MilestoneEventRescheduleService service;

    @Test
    void repositionBuffetBetweenRounds_clampsIntoNewWindow() {
        Hackathon h = Hackathon.builder().id(1).build();
        Round prelim = Round.builder().id(10).examAt(LocalDateTime.of(2026, 4, 11, 8, 0))
                .codingDurationHours(7).build();
        // prelimEnd = 15:00; final = 16:30 → window [15:00, 16:30]
        Round finalRound = Round.builder().id(11)
                .examAt(LocalDateTime.of(2026, 4, 11, 16, 30)).build();

        Event buffet = Event.builder().id(50).type(EventType.BUFFET)
                .startsAt(LocalDateTime.of(2026, 4, 11, 16, 0))
                .endsAt(LocalDateTime.of(2026, 4, 11, 16, 45))
                .build();
        when(eventRepository.findByHackathonIdAndType(1, EventType.BUFFET))
                .thenReturn(List.of(buffet));

        int updated = service.repositionBuffetBetweenRounds(h, prelim, finalRound);

        assertEquals(1, updated);
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        Event saved = captor.getValue();
        assertEquals(LocalDateTime.of(2026, 4, 11, 15, 45), saved.getStartsAt());
        assertEquals(LocalDateTime.of(2026, 4, 11, 16, 30), saved.getEndsAt());
    }
}
