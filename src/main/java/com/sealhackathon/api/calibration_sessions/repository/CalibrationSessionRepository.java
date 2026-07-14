package com.sealhackathon.api.calibration_sessions.repository;

import com.sealhackathon.api.calibration_sessions.entity.CalibrationSession;
import com.sealhackathon.api.calibration_sessions.value_object.CalibrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalibrationSessionRepository extends JpaRepository<CalibrationSession, Integer> {

    List<CalibrationSession> findByRound_IdOrderByStartedAtDesc(Integer roundId);

    List<CalibrationSession> findByRound_IdAndTrack_IdOrderByStartedAtDesc(Integer roundId, Integer trackId);

    boolean existsByRound_IdAndTrack_IdAndStatus(Integer roundId, Integer trackId, CalibrationStatus status);

    boolean existsByRound_IdAndTrackIsNullAndStatus(Integer roundId, CalibrationStatus status);
}
