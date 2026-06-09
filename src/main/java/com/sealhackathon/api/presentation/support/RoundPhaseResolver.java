package com.sealhackathon.api.presentation.support;

import com.sealhackathon.api.presentation.value_object.RoundPhase;
import com.sealhackathon.api.rounds.entity.Round;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RoundPhaseResolver {

    public RoundPhase resolve(Round round) {
        if (Boolean.TRUE.equals(round.getIsPublished())) {
            return RoundPhase.PUBLISHED;
        }
        if (Boolean.TRUE.equals(round.getScoringLocked())) {
            return RoundPhase.SCORING_LOCKED;
        }
        if (!Boolean.TRUE.equals(round.getIsActive())) {
            return RoundPhase.SETUP;
        }
        LocalDateTime examAt = round.getExamAt();
        if (examAt != null && LocalDateTime.now().isBefore(examAt)) {
            return RoundPhase.CODING;
        }
        return RoundPhase.JUDGING;
    }
}
