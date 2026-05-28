package com.sealhackathon.api.scores.service.impl;

import com.sealhackathon.api.scores.dto.request.SubmitCalibrationScoreRequest;
import com.sealhackathon.api.scores.dto.request.SubmitScoreRequest;
import com.sealhackathon.api.scores.dto.response.ScoreResponse;
import com.sealhackathon.api.scores.repository.ScoreRepository;
import com.sealhackathon.api.scores.service.ScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ScoreServiceImpl implements ScoreService {

    private final ScoreRepository scoreRepository;

    @Override
    public ScoreResponse submitScore(SubmitScoreRequest req) {
        // TODO: FR-24/35 judge scoring with assignment + lock checks.
        return ScoreResponse.builder().build();
    }

    @Override
    public ScoreResponse submitCalibrationScore(SubmitCalibrationScoreRequest req) {
        // TODO: FR-34 calibration scoring flow.
        return ScoreResponse.builder().build();
    }
}
