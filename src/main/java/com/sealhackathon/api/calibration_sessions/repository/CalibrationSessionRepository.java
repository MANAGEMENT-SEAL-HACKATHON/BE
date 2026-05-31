package com.sealhackathon.api.calibration_sessions.repository;

import com.sealhackathon.api.calibration_sessions.entity.CalibrationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalibrationSessionRepository extends JpaRepository<CalibrationSession, Integer> {

    List<CalibrationSession> findByRound_IdOrderByStartedAtDesc(Integer roundId);
}
