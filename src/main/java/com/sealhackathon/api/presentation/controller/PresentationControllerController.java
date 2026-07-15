package com.sealhackathon.api.presentation.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.ApprovedOnly;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.presentation.dto.request.PresentationControllerGrantRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationControllerResponse;
import com.sealhackathon.api.presentation.service.PresentationControllerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Presentation Controller (GĐ3)", description = "Ủy quyền điều khiển hàng đợi thuyết trình")
@RestController
@RequestMapping("/api/v1/presentation")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class PresentationControllerController {

    private final PresentationControllerService presentationControllerService;

    @PostMapping("/controller/heartbeat")
    @ApprovedOnly
    @Operation(summary = "Heartbeat 30s — cập nhật lastSeenAt cho judge presence")
    public ResponseEntity<ApiResponse<Void>> heartbeat(
            @RequestParam(required = false) Integer roundId,
            @RequestParam(required = false) Integer trackId) {
        presentationControllerService.heartbeat(roundId, trackId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/tracks/{trackId}/controller")
    @CoordinatorOnly
    @Operation(summary = "Xem presentation controller của track")
    public ResponseEntity<ApiResponse<PresentationControllerResponse>> getTrackController(
            @PathVariable Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(presentationControllerService.getTrackController(trackId)));
    }

    @PutMapping("/tracks/{trackId}/controller")
    @CoordinatorOnly
    @Operation(summary = "Gán / transfer / takeover presentation controller cho track")
    public ResponseEntity<ApiResponse<PresentationControllerResponse>> grantTrackController(
            @PathVariable Integer trackId,
            @Valid @RequestBody PresentationControllerGrantRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                presentationControllerService.grantTrackController(trackId, request)));
    }

    @DeleteMapping("/tracks/{trackId}/controller")
    @CoordinatorOnly
    @Operation(summary = "Thu hồi presentation controller của track")
    public ResponseEntity<ApiResponse<Void>> revokeTrackController(@PathVariable Integer trackId) {
        presentationControllerService.revokeTrackController(trackId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/rounds/{roundId}/controller")
    @CoordinatorOnly
    @Operation(summary = "Xem presentation controller của vòng chung kết")
    public ResponseEntity<ApiResponse<PresentationControllerResponse>> getRoundController(
            @PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(presentationControllerService.getRoundController(roundId)));
    }

    @PutMapping("/rounds/{roundId}/controller")
    @CoordinatorOnly
    @Operation(summary = "Gán / transfer / takeover presentation controller cho vòng chung kết")
    public ResponseEntity<ApiResponse<PresentationControllerResponse>> grantRoundController(
            @PathVariable Integer roundId,
            @Valid @RequestBody PresentationControllerGrantRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                presentationControllerService.grantRoundController(roundId, request)));
    }

    @DeleteMapping("/rounds/{roundId}/controller")
    @CoordinatorOnly
    @Operation(summary = "Thu hồi presentation controller của vòng chung kết")
    public ResponseEntity<ApiResponse<Void>> revokeRoundController(@PathVariable Integer roundId) {
        presentationControllerService.revokeRoundController(roundId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
