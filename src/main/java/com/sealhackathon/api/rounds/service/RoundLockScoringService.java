package com.sealhackathon.api.rounds.service;

import com.sealhackathon.api.rounds.dto.request.LockScoringRequest;
import com.sealhackathon.api.rounds.dto.request.UnlockScoringRequest;
import com.sealhackathon.api.rounds.dto.response.LockScoringResult;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;

/**
 * Lock / unlock scoring for a round (extracted from {@link RoundProgressionService}).
 */
public interface RoundLockScoringService {

    LockScoringResult lockScoring(Integer roundId, LockScoringRequest req);

    RoundSummaryResponse unlockScoring(Integer roundId, UnlockScoringRequest req);
}
