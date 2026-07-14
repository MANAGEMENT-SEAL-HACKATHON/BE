package com.sealhackathon.api.calibration_sessions.service;

import com.sealhackathon.api.calibration_sessions.dto.request.CreateCalibrationSessionRequest;
import com.sealhackathon.api.calibration_sessions.dto.request.UpdateCalibrationSessionRequest;
import com.sealhackathon.api.calibration_sessions.dto.response.CalibrationSessionResponse;

import java.util.List;

public interface CalibrationSessionService {

    CalibrationSessionResponse create(CreateCalibrationSessionRequest req);

    CalibrationSessionResponse update(Integer sessionId, UpdateCalibrationSessionRequest req);

    List<CalibrationSessionResponse> listByRound(Integer roundId);

    List<CalibrationSessionResponse> listByRound(Integer roundId, Integer trackId);
}
