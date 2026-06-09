package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.tracks.entity.Track;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PresentationDurationResolverTest {

    private final PresentationDurationResolver resolver = new PresentationDurationResolver();

    @Test
    void usesTrackOverrideWhenPresent() {
        Track track = Track.builder().presentationMinutes(12).qaMinutes(3).build();
        Round round = Round.builder().defaultPresentationMinutes(10).defaultQaMinutes(5).build();
        assertEquals(12, resolver.presentationMinutes(track, round));
        assertEquals(3, resolver.qaMinutes(track, round));
        assertEquals(15, resolver.slotMinutes(track, round));
    }

    @Test
    void fallsBackToRoundDefaults() {
        Round round = Round.builder().defaultPresentationMinutes(8).defaultQaMinutes(4).build();
        assertEquals(8, resolver.presentationMinutes(null, round));
        assertEquals(4, resolver.qaMinutes(null, round));
        assertEquals(12, resolver.slotMinutes(null, round));
    }

    @Test
    void fallsBackToHardcodedDefaults() {
        Round round = Round.builder().build();
        assertEquals(10, resolver.presentationMinutes(null, round));
        assertEquals(5, resolver.qaMinutes(null, round));
        assertEquals(15, resolver.slotMinutes(null, round));
    }
}
