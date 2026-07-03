package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PresentationTimerPauseTest {

    private final PresentationDurationResolver durationResolver = new PresentationDurationResolver();

    @Test
    void pausedSlot_freezesRemainingSeconds() {
        Round round = Round.builder().defaultPresentationMinutes(10).defaultQaMinutes(5).build();
        Track track = Track.builder().build();
        LocalDateTime started = LocalDateTime.now().minusMinutes(2);
        PresentationSlot slot = PresentationSlot.builder()
                .timerPhase(PresentationTimerPhase.PAUSED)
                .timerPhaseBeforePause(PresentationTimerPhase.PRESENTING)
                .presentationStartedAt(started)
                .pausedAt(LocalDateTime.now())
                .pausedAccumulatedSeconds(0)
                .build();

        int remaining = PresentationTimerCalculator.remainingSeconds(slot, track, round, durationResolver);
        assertThat(remaining).isBetween(470, 480);
    }

    @Test
    void idleReturnsFullPresentationDuration() {
        Round round = Round.builder().defaultPresentationMinutes(8).build();
        PresentationSlot slot = PresentationSlot.builder().timerPhase(PresentationTimerPhase.IDLE).build();
        int remaining = PresentationTimerCalculator.remainingSeconds(slot, null, round, durationResolver);
        assertThat(remaining).isEqualTo(8 * 60);
    }

    @Test
    void setupReturnsFullPresentationDuration() {
        Round round = Round.builder().defaultPresentationMinutes(10).build();
        PresentationSlot slot = PresentationSlot.builder().timerPhase(PresentationTimerPhase.SETUP).build();
        int remaining = PresentationTimerCalculator.remainingSeconds(slot, null, round, durationResolver);
        assertThat(remaining).isEqualTo(10 * 60);
    }
}
