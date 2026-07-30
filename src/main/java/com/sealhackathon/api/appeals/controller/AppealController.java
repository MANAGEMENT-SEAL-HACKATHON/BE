package com.sealhackathon.api.appeals.controller;

import com.sealhackathon.api.appeals.dto.request.AppealDelayRequest;
import com.sealhackathon.api.appeals.dto.request.ReviewAppealRequest;
import com.sealhackathon.api.appeals.dto.response.AppealDelayPreviewResponse;
import com.sealhackathon.api.appeals.dto.response.AppealWindowStatusResponse;
import com.sealhackathon.api.appeals.dto.response.PublishPreflightResponse;
import com.sealhackathon.api.appeals.service.AppealReviewService;
import com.sealhackathon.api.appeals.service.AppealWindowService;
import com.sealhackathon.api.appeals.value_object.AppealStatus;
import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.me.student.dto.response.AppealResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Appeals (GĐ4 DQ)", description = "Cửa sổ khiếu nại DQ sau công bố sơ loại")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CoordinatorOnly
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AppealController {

    private final AppealReviewService appealReviewService;
    private final AppealWindowService appealWindowService;

    @GetMapping("/rounds/{roundId}/appeals")
    @Operation(summary = "Danh sách đơn khiếu nại theo vòng")
    public ResponseEntity<ApiResponse<List<AppealResponse>>> listByRound(
            @PathVariable Integer roundId,
            @RequestParam(required = false) AppealStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(appealReviewService.listByRound(roundId, status)));
    }

    @GetMapping("/appeals/{id}")
    @Operation(summary = "Chi tiết đơn khiếu nại")
    public ResponseEntity<ApiResponse<AppealResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(appealReviewService.getById(id)));
    }

    @PatchMapping("/appeals/{id}/claim")
    @Operation(summary = "Nhận đơn để duyệt (PENDING → UNDER_REVIEW)")
    public ResponseEntity<ApiResponse<AppealResponse>> claim(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(appealReviewService.claim(id)));
    }

    @PatchMapping("/appeals/{id}/review")
    @Operation(summary = "Duyệt hoặc từ chối đơn")
    public ResponseEntity<ApiResponse<AppealResponse>> review(
            @PathVariable Integer id,
            @Valid @RequestBody ReviewAppealRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(appealReviewService.review(id, request)));
    }

    @PostMapping("/rounds/{roundId}/appeal-window/close")
    @Operation(summary = "Đóng cửa sổ khiếu nại sớm")
    public ResponseEntity<ApiResponse<AppealWindowStatusResponse>> closeEarly(@PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(appealWindowService.closeEarly(roundId)));
    }

    @PostMapping("/rounds/{roundId}/republish")
    @Operation(summary = "Công bố lại kết quả sau khi duyệt khiếu nại")
    public ResponseEntity<ApiResponse<AppealWindowStatusResponse>> republish(@PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(appealWindowService.republish(roundId)));
    }

    @PostMapping("/rounds/{roundId}/appeal-delay/preview")
    @Operation(summary = "Xem trước dời giờ Chung kết (T-5)")
    public ResponseEntity<ApiResponse<AppealDelayPreviewResponse>> previewDelay(
            @PathVariable Integer roundId,
            @Valid @RequestBody AppealDelayRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(appealWindowService.previewDelay(roundId, request)));
    }

    @PostMapping("/rounds/{roundId}/appeal-delay")
    @Operation(summary = "Dời giờ Chung kết cho kháng cáo (T-5)")
    public ResponseEntity<ApiResponse<AppealDelayPreviewResponse>> applyDelay(
            @PathVariable Integer roundId,
            @Valid @RequestBody AppealDelayRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(appealWindowService.applyDelay(roundId, request)));
    }

    @GetMapping("/rounds/{roundId}/appeal-window")
    @Operation(summary = "Trạng thái cửa sổ khiếu nại + serverNow")
    public ResponseEntity<ApiResponse<AppealWindowStatusResponse>> windowStatus(
            @PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(appealWindowService.getWindowStatus(roundId)));
    }

    @PostMapping("/rounds/{roundId}/publish/preflight")
    @Operation(summary = "Kiểm tra trước khi công bố — cửa sổ khiếu nại có vừa không")
    public ResponseEntity<ApiResponse<PublishPreflightResponse>> publishPreflight(
            @PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(appealWindowService.preflight(roundId)));
    }

    @GetMapping("/rounds/{roundId}/publish/preflight")
    @Operation(summary = "Kiểm tra trước khi công bố (GET)")
    public ResponseEntity<ApiResponse<PublishPreflightResponse>> publishPreflightGet(
            @PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(appealWindowService.preflight(roundId)));
    }
}
