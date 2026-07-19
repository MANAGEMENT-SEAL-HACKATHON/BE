package com.sealhackathon.api.rbl.calibration.repository;

import com.sealhackathon.api.rbl.calibration.entity.CalibrationPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CalibrationPromptRepository extends JpaRepository<CalibrationPrompt, Integer> {
    List<CalibrationPrompt> findByRoundIdOrderByCreatedAtDesc(Integer roundId);
    List<CalibrationPrompt> findByRoundIdAndStatusOrderByCreatedAtDesc(
            Integer roundId, CalibrationPrompt.Status status);
}
