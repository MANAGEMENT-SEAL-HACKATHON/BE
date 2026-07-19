package com.sealhackathon.api.rbl.calibration.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.JudgeOnly;
import com.sealhackathon.api.rbl.calibration.dto.CalibrationDtos.*;
import com.sealhackathon.api.rbl.calibration.service.CalibrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rbl/calibration/judge")
@RequiredArgsConstructor
public class JudgeCalibrationController {
    private final CalibrationService calibrationService;

    @GetMapping("/rounds/{roundId}/prompts")
    @JudgeOnly
    public ApiResponse<List<PromptView>> openPrompts(@PathVariable Integer roundId) {
        return ApiResponse.ok(calibrationService.listOpenForJudge(roundId));
    }

    @PostMapping("/prompts/{promptId}/scores")
    @JudgeOnly
    public ApiResponse<DistributionView> submit(
            @PathVariable Integer promptId, @Valid @RequestBody SubmitScoresRequest request) {
        return ApiResponse.ok(calibrationService.submit(promptId, request));
    }

    @GetMapping("/prompts/{promptId}/distribution")
    @JudgeOnly
    public ApiResponse<DistributionView> distribution(@PathVariable Integer promptId) {
        return ApiResponse.ok(calibrationService.distribution(promptId, true));
    }
}
