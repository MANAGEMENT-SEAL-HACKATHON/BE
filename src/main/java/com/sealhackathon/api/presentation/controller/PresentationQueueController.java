package com.sealhackathon.api.presentation.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.ApprovedOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.presentation.dto.request.PresentationQueueNextRequest;
import com.sealhackathon.api.presentation.dto.request.PresentationShuffleRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueNextResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationQueueResponse;
import com.sealhackathon.api.presentation.dto.response.PresentationShuffleResponse;
import com.sealhackathon.api.presentation.service.PresentationQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Presentation Queue (GĐ3)", description = "FR-GĐ3 — Hàng đợi thuyết trình")
@RestController
@RequestMapping("/api/v1/presentation/queue")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class PresentationQueueController {

    private final PresentationQueueService presentationQueueService;

    @GetMapping
    @ApprovedOnly
    @Operation(summary = "GĐ3 — Danh sách thứ tự thuyết trình theo track")
    public ResponseEntity<ApiResponse<PresentationQueueResponse>> getQueue(
            @RequestParam(required = false) Integer roundId,
            @RequestParam(required = false) Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(presentationQueueService.getQueue(roundId, trackId)));
    }

    @PostMapping("/shuffle")
    @ApprovedOnly
    @Operation(summary = "GĐ3 — Xáo trộn hàng đợi thuyết trình")
    public ResponseEntity<ApiResponse<PresentationShuffleResponse>> shuffle(
            @RequestBody PresentationShuffleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(presentationQueueService.shuffle(request)));
    }

    @PatchMapping("/next")
    @ApprovedOnly
    @Operation(summary = "GĐ3 — Chuyển submission tiếp theo (presentation controller)")
    public ResponseEntity<ApiResponse<PresentationQueueNextResponse>> advanceNext(
            @RequestParam(required = false) Integer roundId,
            @RequestParam(required = false) Integer trackId,
            @RequestBody(required = false) PresentationQueueNextRequest request) {
        Integer currentSubmissionId = null;
        Integer currentTeamId = null;
        boolean acknowledgeIncompleteScoring = false;
        if (request != null) {
            currentSubmissionId = request.getCurrentSubmissionId();
            currentTeamId = request.getCurrentTeamId();
            if (trackId == null) {
                trackId = request.getTrackId();
            }
            if (Boolean.TRUE.equals(request.getAcknowledgeIncompleteScoring())) {
                acknowledgeIncompleteScoring = true;
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(
                presentationQueueService.advanceNext(
                        roundId, trackId, currentSubmissionId, currentTeamId, acknowledgeIncompleteScoring)));
    }
}
