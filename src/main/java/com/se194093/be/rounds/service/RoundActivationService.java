package com.se194093.be.rounds.service;

import com.se194093.be.rounds.dto.response.RoundResponse;

/**
 * FR-06B — Safety net validate weight = 1.0 khi activate Round.
 *
 * <p>Pseudocode:
 * <pre>
 * total = criteriaRepo.sumWeightExcludingPenalty(roundId)
 * if total == null         → throw BusinessRuleException(ROUND_NO_CRITERIA)
 * if abs(total - 1.0) &gt; 0.001 → throw BusinessRuleException(ROUND_WEIGHT_NOT_ONE)
 * roundRepo.deactivateOtherRoundsInTrack(round.track.id, roundId)
 * round.isActive = TRUE; save
 * audit.log(ROUND_ACTIVATE, ...)
 * </pre>
 *
 * <p>Constants:
 * <ul>
 *   <li>{@link #WEIGHT_TARGET} = 1.0</li>
 *   <li>{@link #WEIGHT_TOLERANCE} = 0.001 — chấp nhận sai số float</li>
 * </ul>
 */
public interface RoundActivationService {

    double WEIGHT_TARGET    = 1.0;
    double WEIGHT_TOLERANCE = 0.001;

    /**
     * @param roundId Round id
     * @param note    optional, ghi vào audit detail
     */
    RoundResponse activate(Integer roundId, String note);
}
