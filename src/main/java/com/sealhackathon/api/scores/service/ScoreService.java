package com.sealhackathon.api.scores.service;

import com.sealhackathon.api.scores.dto.request.SubmitCalibrationScoreRequest;
import com.sealhackathon.api.scores.dto.request.SubmitScoreRequest;
import com.sealhackathon.api.scores.dto.response.ScoreResponse;

public interface ScoreService {

    ScoreResponse submitScore(SubmitScoreRequest req);

    ScoreResponse submitCalibrationScore(SubmitCalibrationScoreRequest req);
}
