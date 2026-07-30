package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.rounds.entity.Round;

import java.time.LocalDateTime;

/**
 * Single source of truth for "submission window closed" (past deadline or closed early).
 * Used by RoundPhaseResolver, lock-scoring, and presentation shuffle gates.
 */
public final class RoundSubmissionWindow {

    private RoundSubmissionWindow() {
    }

    public static boolean isClosed(Round round, LocalDateTime now) {
        if (round == null) {
            return false;
        }
        if (round.getSubmissionClosedEarlyAt() != null) {
            return true;
        }
        LocalDateTime deadline = round.getSubmissionDeadline();
        return deadline != null && !now.isBefore(deadline);
    }
}
