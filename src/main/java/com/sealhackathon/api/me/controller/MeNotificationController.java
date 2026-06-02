package com.sealhackathon.api.me.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.ApprovedOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.me.dto.request.MarkNotificationsReadRequest;
import com.sealhackathon.api.me.dto.response.MeNotificationResponse;
import com.sealhackathon.api.me.service.MeNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Me Notifications", description = "Thông báo — mọi role đã APPROVED")
@RestController
@RequestMapping("/api/v1/me/notifications")
@RequiredArgsConstructor
@ApprovedOnly
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class MeNotificationController {

    private final MeNotificationService meNotificationService;

    @GetMapping
    @Operation(summary = "Danh sách thông báo của user hiện tại")
    public ResponseEntity<ApiResponse<List<MeNotificationResponse>>> list(
            @RequestParam(required = false) Boolean unreadOnly) {
        return ResponseEntity.ok(ApiResponse.ok(meNotificationService.listForCurrentUser(unreadOnly)));
    }

    @PatchMapping("/read")
    @Operation(summary = "Đánh dấu đã đọc")
    public ResponseEntity<ApiResponse<Void>> markRead(@Valid @RequestBody MarkNotificationsReadRequest request) {
        meNotificationService.markRead(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Marked read"));
    }
}
