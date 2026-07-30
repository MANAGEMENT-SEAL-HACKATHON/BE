package com.sealhackathon.api.rounds.service;

import com.sealhackathon.api.rounds.dto.request.ActivateRoundRequest;
import com.sealhackathon.api.rounds.dto.response.RoundResponse;

/**
 * FR-06B — Activate Round (weight + schedule mode KEEP; RESCHEDULE rejected on activate).
 */
public interface RoundActivationService {

    double WEIGHT_TARGET    = 1.0;
    double WEIGHT_TOLERANCE = 0.001;

    /**
     * @param roundId Round id
     * @param request optional note + scheduleMode / newExamAt
     */
    RoundResponse activate(Integer roundId, ActivateRoundRequest request);
}
