package com.sealhackathon.api.config.seed;

import com.sealhackathon.api.rounds.entity.Round;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class E2eDevFlowGuardTest {

    @Test
    void prelimActiveMeansProgress() {
        Round prelim = Round.builder().isActive(true).build();
        assertTrue(E2eDevFlowGuard.hasPrelimProgress(prelim));
    }

    @Test
    void prelimActivatedAtMeansProgress() {
        Round prelim = Round.builder()
                .isActive(false)
                .activatedAt(LocalDateTime.now())
                .build();
        assertTrue(E2eDevFlowGuard.hasPrelimProgress(prelim));
    }

    @Test
    void baselinePrelimNotProgress() {
        Round prelim = Round.builder()
                .isActive(false)
                .scoringLocked(false)
                .isPublished(false)
                .build();
        assertFalse(E2eDevFlowGuard.hasPrelimProgress(prelim));
    }

    @Test
    void finalActiveMeansProgress() {
        Round finalRound = Round.builder().isActive(true).isFinal(true).build();
        assertTrue(E2eDevFlowGuard.hasFinalProgress(finalRound));
    }

    @Test
    void closedEarlyMeansProgress() {
        Round prelim = Round.builder()
                .isActive(true)
                .submissionClosedEarlyAt(LocalDateTime.now())
                .build();
        assertTrue(E2eDevFlowGuard.hasPrelimProgress(prelim));
    }
}
