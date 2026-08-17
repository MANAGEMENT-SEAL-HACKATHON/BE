package com.sealhackathon.api.rounds.support;

import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.rounds.entity.Round;

/**
 * Visibility gates for round results.
 * <ul>
 *   <li>Preliminary: always requires {@code round.isPublished}.</li>
 *   <li>Final: publish() is blocked; results become visible after lock-scoring / confirm.</li>
 * </ul>
 * Public scoreboard is {@code permitAll} — must not open at PENDING_CONFIRM.
 */
public final class RoundResultVisibility {

    private RoundResultVisibility() {
    }

    /**
     * Authenticated participants (students on the portal).
     * Final results visible from PENDING_CONFIRM once scoring is locked — same as getHackathonRankings.
     */
    public static boolean visibleToParticipants(Round round, Hackathon hackathon) {
        if (round == null) {
            return false;
        }
        if (Boolean.TRUE.equals(round.getIsPublished())) {
            return true;
        }
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            return false;
        }
        if (!Boolean.TRUE.equals(round.getScoringLocked())) {
            return false;
        }
        HackathonStatus status = hackathon != null ? hackathon.getStatus() : null;
        return status == HackathonStatus.PENDING_CONFIRM || status == HackathonStatus.FINISHED;
    }

    /**
     * Anonymous / public scoreboard ({@code GET /rounds/{id}/scoreboard}).
     * Final results only after hackathon is FINISHED (post confirm + prize ceremony).
     */
    public static boolean visibleToPublic(Round round, Hackathon hackathon) {
        if (round == null) {
            return false;
        }
        if (Boolean.TRUE.equals(round.getIsPublished())) {
            return true;
        }
        if (!Boolean.TRUE.equals(round.getIsFinal())) {
            return false;
        }
        HackathonStatus status = hackathon != null ? hackathon.getStatus() : null;
        return status == HackathonStatus.FINISHED;
    }
}
