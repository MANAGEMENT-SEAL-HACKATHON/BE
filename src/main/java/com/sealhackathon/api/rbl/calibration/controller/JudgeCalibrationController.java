package com.sealhackathon.api.rbl.calibration.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.JudgeOnly;
import com.sealhackathon.api.rbl.calibration.dto.CalibrationDtos.*;
import com.sealhackathon.api.rbl.calibration.service.CalibrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rbl/calibration/judge")
@RequiredArgsConstructor
public class JudgeCalibrationController {
    private final CalibrationService calibrationService;

    @GetMapping("/rounds/{roundId}/prompts")
    @JudgeOnly
    public ResponseEntity<ApiResponse<List<PromptView>>> openPrompts(@PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(calibrationService.listOpenForJudge(roundId)));
    }

    @PostMapping("/prompts/{promptId}/scores")
    @JudgeOnly
    public ResponseEntity<ApiResponse<DistributionView>> submit(
            @PathVariable Integer promptId, @Valid @RequestBody SubmitScoresRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(calibrationService.submit(promptId, request)));
    }

    @GetMapping("/prompts/{promptId}/distribution")
    @JudgeOnly
    public ResponseEntity<ApiResponse<DistributionView>> distribution(@PathVariable Integer promptId) {
        return ResponseEntity.ok(ApiResponse.ok(calibrationService.distribution(promptId, true)));
    }
}
