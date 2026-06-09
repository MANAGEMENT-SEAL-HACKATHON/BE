package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.presentation.value_object.PresentationTimerPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationScoringGateTest {

    @Test
    void setupAndIdle_blockScoring() {
        assertFalse(PresentationScoringGate.isTimerOpenForScoring(PresentationTimerPhase.IDLE));
        assertFalse(PresentationScoringGate.isTimerOpenForScoring(PresentationTimerPhase.SETUP));
    }

    @Test
    void activePhases_allowScoring() {
        assertTrue(PresentationScoringGate.isTimerOpenForScoring(PresentationTimerPhase.PRESENTING));
        assertTrue(PresentationScoringGate.isTimerOpenForScoring(PresentationTimerPhase.PAUSED));
        assertTrue(PresentationScoringGate.isTimerOpenForScoring(PresentationTimerPhase.QA));
        assertTrue(PresentationScoringGate.isTimerOpenForScoring(PresentationTimerPhase.ENDED));
    }
}
