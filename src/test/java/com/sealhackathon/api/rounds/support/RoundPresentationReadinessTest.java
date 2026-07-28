package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.events.entity.PresentationSlot;
import com.sealhackathon.api.events.repository.PresentationSlotRepository;
import com.sealhackathon.api.presentation.value_object.PresentationQueueStatus;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundPresentationReadinessTest {

    @Mock private TrackRepository trackRepository;
    @Mock private PresentationSlotRepository presentationSlotRepository;

    @InjectMocks
    private RoundPresentationReadiness readiness;

    private Round prelim;
    private Round finalRound;

    @BeforeEach
    void setUp() {
        prelim = Round.builder().id(10).isFinal(false).build();
        finalRound = Round.builder().id(20).isFinal(true).presentationShuffled(false).build();
    }

    @Test
    void prelim_notShuffled_flagsFalse() {
        Track t = Track.builder().id(1).status(TrackStatus.OPEN).presentationShuffled(false).build();
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(10)).thenReturn(List.of(t));

        RoundPresentationReadiness.Flags flags = readiness.evaluate(prelim);

        assertThat(flags.isPresentationShuffled()).isFalse();
        assertThat(flags.isPresentationsComplete()).isFalse();
    }

    @Test
    void prelim_shuffledZeroSlots_complete() {
        Track t = Track.builder().id(1).status(TrackStatus.OPEN).presentationShuffled(true).build();
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(10)).thenReturn(List.of(t));
        when(presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(10, 1))
                .thenReturn(List.of());

        RoundPresentationReadiness.Flags flags = readiness.evaluate(prelim);

        assertThat(flags.isPresentationShuffled()).isTrue();
        assertThat(flags.isPresentationsComplete()).isTrue();
    }

    @Test
    void prelim_shuffledWithWaiting_incomplete() {
        Track t = Track.builder().id(1).status(TrackStatus.OPEN).presentationShuffled(true).build();
        when(trackRepository.findByRoundIdOrderBySequenceOrderAsc(10)).thenReturn(List.of(t));
        when(presentationSlotRepository.findByRound_IdAndTrack_IdOrderBySequenceOrderAsc(10, 1))
                .thenReturn(List.of(PresentationSlot.builder()
                        .queueStatus(PresentationQueueStatus.WAITING)
                        .build()));

        RoundPresentationReadiness.Flags flags = readiness.evaluate(prelim);

        assertThat(flags.isPresentationShuffled()).isTrue();
        assertThat(flags.isPresentationsComplete()).isFalse();
    }

    @Test
    void final_zeroSlotAfterShuffle_complete() {
        finalRound.setPresentationShuffled(true);
        when(presentationSlotRepository.findByRound_IdAndTrackIsNullOrderBySequenceOrderAsc(20))
                .thenReturn(List.of());

        RoundPresentationReadiness.Flags flags = readiness.evaluate(finalRound);

        assertThat(flags.isPresentationShuffled()).isTrue();
        assertThat(flags.isPresentationsComplete()).isTrue();
    }

    /** Null wrapper (legacy / migration lag) must not NPE — treat as false. */
    @Test
    void final_nullPresentationShuffled_isNotShuffled() {
        Round round = Round.builder().id(21).isFinal(true).presentationShuffled(null).build();

        assertThat(readiness.isShuffled(round)).isFalse();
    }
}
