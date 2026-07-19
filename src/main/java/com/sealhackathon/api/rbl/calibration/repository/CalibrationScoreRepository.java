package com.sealhackathon.api.rbl.calibration.repository;

import com.sealhackathon.api.rbl.calibration.entity.CalibrationScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalibrationScoreRepository extends JpaRepository<CalibrationScore, Integer> {
    List<CalibrationScore> findByPromptIdOrderByJudgeIdAscCriterionIdAsc(Integer promptId);
    Optional<CalibrationScore> findByPromptIdAndJudgeIdAndCriterionId(
            Integer promptId, Integer judgeId, Integer criterionId);
}
