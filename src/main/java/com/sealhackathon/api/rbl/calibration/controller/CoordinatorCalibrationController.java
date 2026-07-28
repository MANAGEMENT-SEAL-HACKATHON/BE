package com.sealhackathon.api.rbl.calibration.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.rbl.calibration.dto.CalibrationDtos.*;
import com.sealhackathon.api.rbl.calibration.service.CalibrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rbl/calibration/coordinator")
@RequiredArgsConstructor
public class CoordinatorCalibrationController {
    private final CalibrationService calibrationService;

    @PostMapping("/prompts")
    @CoordinatorOnly
    public ResponseEntity<ApiResponse<PromptView>> create(
            @Valid @RequestBody CreatePromptRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(calibrationService.create(request)));
    }

    @PatchMapping("/prompts/{promptId}/close")
    @CoordinatorOnly
    public ResponseEntity<ApiResponse<PromptView>> close(@PathVariable Integer promptId) {
        return ResponseEntity.ok(ApiResponse.ok(calibrationService.close(promptId)));
    }

    @GetMapping("/rounds/{roundId}/prompts")
    @CoordinatorOnly
    public ResponseEntity<ApiResponse<List<PromptView>>> list(@PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(calibrationService.listByRound(roundId)));
    }

    @GetMapping("/prompts/{promptId}/distribution")
    @CoordinatorOnly
    public ResponseEntity<ApiResponse<DistributionView>> distribution(@PathVariable Integer promptId) {
        return ResponseEntity.ok(ApiResponse.ok(calibrationService.distribution(promptId, false)));
    }
}
