package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.presentation.value_object.RoundPhase;
import com.sealhackathon.api.rounds.entity.Round;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoundPhaseResolverTest {

    private final RoundPhaseResolver resolver = new RoundPhaseResolver();

    @Test
    void resolve_published() {
        Round round = Round.builder().isPublished(true).build();
        assertEquals(RoundPhase.PUBLISHED, resolver.resolve(round));
    }

    @Test
    void resolve_scoringLocked() {
        Round round = Round.builder().isPublished(false).scoringLocked(true).build();
        assertEquals(RoundPhase.SCORING_LOCKED, resolver.resolve(round));
    }

    @Test
    void resolve_setupWhenInactive() {
        Round round = Round.builder().isActive(false).build();
        assertEquals(RoundPhase.SETUP, resolver.resolve(round));
    }

    @Test
    void resolve_codingBeforeExamAt_whenNoDeadline() {
        Round round = Round.builder()
                .isActive(true)
                .examAt(LocalDateTime.now().plusHours(2))
                .build();
        assertEquals(RoundPhase.CODING, resolver.resolve(round));
    }

    @Test
    void resolve_codingWhileExamStartedButDeadlineStillFuture() {
        Round round = Round.builder()
                .isActive(true)
                .examAt(LocalDateTime.now().minusHours(1))
                .submissionDeadline(LocalDateTime.now().plusHours(3))
                .build();
        assertEquals(RoundPhase.CODING, resolver.resolve(round));
    }

    @Test
    void resolve_judgingWhenDeadlinePassed() {
        Round round = Round.builder()
                .isActive(true)
                .examAt(LocalDateTime.now().minusHours(4))
                .submissionDeadline(LocalDateTime.now().minusMinutes(5))
                .build();
        assertEquals(RoundPhase.JUDGING, resolver.resolve(round));
    }

    @Test
    void resolve_judgingWhenActiveAndExamStarted_noDeadlineFallback() {
        Round round = Round.builder()
                .isActive(true)
                .examAt(LocalDateTime.now().minusHours(1))
                .build();
        assertEquals(RoundPhase.JUDGING, resolver.resolve(round));
    }
}
