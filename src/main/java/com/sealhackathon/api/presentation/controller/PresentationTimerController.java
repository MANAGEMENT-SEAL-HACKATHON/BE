package com.sealhackathon.api.presentation.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.ApprovedOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.presentation.dto.response.PresentationTimerActionResponse;
import com.sealhackathon.api.presentation.service.PresentationTimerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Presentation Timer (GĐ3)", description = "FR-GĐ3 — Đồng hồ thuyết trình")
@RestController
@RequestMapping("/api/v1/presentation/timer")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class PresentationTimerController {

    private final PresentationTimerService presentationTimerService;

    @PostMapping("/start")
    @ApprovedOnly
    @Operation(summary = "Bắt đầu timer thuyết trình")
    public ResponseEntity<ApiResponse<PresentationTimerActionResponse>> start(
            @RequestParam Integer roundId,
            @RequestParam(required = false) Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(presentationTimerService.start(roundId, trackId)));
    }

    @PostMapping("/pause")
    @ApprovedOnly
    @Operation(summary = "Tạm dừng timer")
    public ResponseEntity<ApiResponse<PresentationTimerActionResponse>> pause(
            @RequestParam Integer roundId,
            @RequestParam(required = false) Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(presentationTimerService.pause(roundId, trackId)));
    }

    @PostMapping("/resume")
    @ApprovedOnly
    @Operation(summary = "Tiếp tục timer")
    public ResponseEntity<ApiResponse<PresentationTimerActionResponse>> resume(
            @RequestParam Integer roundId,
            @RequestParam(required = false) Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(presentationTimerService.resume(roundId, trackId)));
    }

    @PostMapping("/qa")
    @ApprovedOnly
    @Operation(summary = "Chuyển sang pha Q&A")
    public ResponseEntity<ApiResponse<PresentationTimerActionResponse>> qa(
            @RequestParam Integer roundId,
            @RequestParam(required = false) Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(presentationTimerService.qa(roundId, trackId)));
    }

    @PostMapping("/reset")
    @ApprovedOnly
    @Operation(summary = "Reset timer slot hiện tại")
    public ResponseEntity<ApiResponse<PresentationTimerActionResponse>> reset(
            @RequestParam Integer roundId,
            @RequestParam(required = false) Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(presentationTimerService.reset(roundId, trackId)));
    }
}
