package com.sealhackathon.api.scores.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @deprecated Backward-compatible adapter. Use {@link ScoreRepository}.
 */
@Deprecated
@Component
@RequiredArgsConstructor
public class ScorePlaceholderRepository {

    private final ScoreRepository scoreRepository;

    public long countByCriteriaId(Integer criteriaId) {
        return scoreRepository.countByCriteriaId(criteriaId);
    }

    public long countByRoundId(Integer roundId) {
        return scoreRepository.countByRoundId(roundId);
    }
}
