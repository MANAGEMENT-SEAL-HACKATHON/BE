package com.sealhackathon.api.calibration_sessions.service.impl;

import com.sealhackathon.api.calibration_sessions.dto.request.CreateCalibrationSessionRequest;
import com.sealhackathon.api.calibration_sessions.dto.request.UpdateCalibrationSessionRequest;
import com.sealhackathon.api.calibration_sessions.dto.response.CalibrationSessionResponse;
import com.sealhackathon.api.calibration_sessions.service.CalibrationSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CalibrationSessionServiceImpl implements CalibrationSessionService {

    @Override
    public CalibrationSessionResponse create(CreateCalibrationSessionRequest req) {
        // TODO: FR-29 create OPEN session + notify judges.
        return CalibrationSessionResponse.builder()
                .roundId(req.getRoundId())
                .sampleSubmissionId(req.getSampleSubmissionId())
                .targetScore(req.getTargetScore())
                .instructions(req.getInstructions())
                .build();
    }

    @Override
    public CalibrationSessionResponse update(Integer sessionId, UpdateCalibrationSessionRequest req) {
        // TODO: FR-29 close session — block CALIBRATION scores when CLOSED.
        return CalibrationSessionResponse.builder().id(sessionId).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalibrationSessionResponse> listByRound(Integer roundId) {
        // TODO: FR-29 list sessions for round.
        return Collections.emptyList();
    }
}
