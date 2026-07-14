package com.sealhackathon.api.calibration_sessions.controller;

import com.sealhackathon.api.calibration_sessions.dto.request.CreateCalibrationSessionRequest;
import com.sealhackathon.api.calibration_sessions.dto.request.UpdateCalibrationSessionRequest;
import com.sealhackathon.api.calibration_sessions.dto.response.CalibrationSessionResponse;
import com.sealhackathon.api.calibration_sessions.service.CalibrationSessionService;
import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Calibration Sessions (GĐ5)", description = "FR-29 — Hiệu chuẩn điểm RBL")
@RestController
@RequestMapping("/api/v1/calibration-sessions")
@RequiredArgsConstructor
public class CalibrationSessionController {

    private final CalibrationSessionService calibrationSessionService;

    @PostMapping
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-29 — Tạo phiên calibration")
    public ResponseEntity<ApiResponse<CalibrationSessionResponse>> create(
            @Valid @RequestBody CreateCalibrationSessionRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(calibrationSessionService.create(req)));
    }

    @PatchMapping("/{id}")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-29 — Cập nhật/đóng phiên calibration")
    public ResponseEntity<ApiResponse<CalibrationSessionResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateCalibrationSessionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(calibrationSessionService.update(id, req)));
    }

    @GetMapping
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-29 — Danh sách phiên theo round (optional trackId)")
    public ResponseEntity<ApiResponse<List<CalibrationSessionResponse>>> list(
            @RequestParam Integer roundId,
            @RequestParam(required = false) Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(calibrationSessionService.listByRound(roundId, trackId)));
    }
}
