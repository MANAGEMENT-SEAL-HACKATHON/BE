package com.sealhackathon.api.presentation.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.presentation.dto.request.PresentationDurationSetupRequest;
import com.sealhackathon.api.presentation.dto.response.PresentationDurationResponse;
import com.sealhackathon.api.presentation.service.PresentationDurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Presentation Duration (GĐ3/GĐ5)", description = "Coordinator cấu hình thời lượng thuyết trình & Q&A")
@RestController
@RequestMapping("/api/v1/presentation/duration")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@CoordinatorOnly
public class PresentationDurationController {

    private final PresentationDurationService presentationDurationService;

    @GetMapping
    @Operation(summary = "Xem thời lượng timer",
            description = "Không có trackId → cấu hình round (GĐ5 / default GĐ3). Có trackId → override track GĐ3.")
    public ResponseEntity<ApiResponse<PresentationDurationResponse>> getDuration(
            @RequestParam Integer roundId,
            @RequestParam(required = false) Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(presentationDurationService.getDuration(roundId, trackId)));
    }

    @PutMapping
    @Operation(summary = "Cập nhật thời lượng timer",
            description = "Chỉ trước khi start timer (chưa có slot DONE / timer PRESENTING|QA). "
                    + "Sau khi đổi, BE tự cập nhật khung giờ presentation_slots. "
                    + "GĐ5: chỉ roundId. GĐ3 track: thêm trackId.")
    public ResponseEntity<ApiResponse<PresentationDurationResponse>> updateDuration(
            @Valid @RequestBody PresentationDurationSetupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(presentationDurationService.updateDuration(request)));
    }

    @DeleteMapping
    @Operation(summary = "Gỡ override track (GĐ3)",
            description = "Track quay lại dùng defaultPresentationMinutes/defaultQaMinutes của round.")
    public ResponseEntity<ApiResponse<PresentationDurationResponse>> clearTrackOverride(
            @RequestParam Integer roundId,
            @RequestParam Integer trackId) {
        return ResponseEntity.ok(
                ApiResponse.ok(presentationDurationService.clearTrackOverride(roundId, trackId)));
    }
}
