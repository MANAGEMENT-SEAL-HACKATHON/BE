package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentationQaTimeoutMaterializerTest {

    @Mock private PresentationSlotRepository slotRepository;

    private final PresentationDurationResolver durationResolver = new PresentationDurationResolver();

    @Test
    void qaExpired_materializesEndedWithoutScoringGuard() {
        Round round = Round.builder().defaultPresentationMinutes(10).defaultQaMinutes(5).build();
        Track track = Track.builder().build();
        PresentationSlot slot = PresentationSlot.builder()
                .timerPhase(PresentationTimerPhase.QA)
                .qaStartedAt(LocalDateTime.now().minusMinutes(10))
                .pausedAccumulatedSeconds(0)
                .build();
        when(slotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean changed = PresentationQaTimeoutMaterializer.materializeIfExpired(
                slot, track, round, durationResolver, slotRepository);

        assertThat(changed).isTrue();
        assertThat(slot.getTimerPhase()).isEqualTo(PresentationTimerPhase.ENDED);
        verify(slotRepository).save(slot);
    }

    @Test
    void qaStillRunning_doesNotChangePhase() {
        Round round = Round.builder().defaultPresentationMinutes(10).defaultQaMinutes(5).build();
        PresentationSlot slot = PresentationSlot.builder()
                .timerPhase(PresentationTimerPhase.QA)
                .qaStartedAt(LocalDateTime.now().minusSeconds(30))
                .pausedAccumulatedSeconds(0)
                .build();

        boolean changed = PresentationQaTimeoutMaterializer.materializeIfExpired(
                slot, null, round, durationResolver, slotRepository);

        assertThat(changed).isFalse();
        assertThat(slot.getTimerPhase()).isEqualTo(PresentationTimerPhase.QA);
        verify(slotRepository, never()).save(any());
    }

    @Test
    void presentingPhase_ignoredEvenIfClockWouldBeZero() {
        Round round = Round.builder().defaultPresentationMinutes(1).defaultQaMinutes(5).build();
        PresentationSlot slot = PresentationSlot.builder()
                .timerPhase(PresentationTimerPhase.PRESENTING)
                .presentationStartedAt(LocalDateTime.now().minusMinutes(30))
                .pausedAccumulatedSeconds(0)
                .build();

        boolean changed = PresentationQaTimeoutMaterializer.materializeIfExpired(
                slot, null, round, durationResolver, slotRepository);

        assertThat(changed).isFalse();
        assertThat(slot.getTimerPhase()).isEqualTo(PresentationTimerPhase.PRESENTING);
        verify(slotRepository, never()).save(any());
    }
}
