package com.sealhackathon.api.invitations.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.invitations.service.InvitationService;
import com.sealhackathon.api.users.dto.response.TempJudgeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-05a — Resend invitation judge khách mời.
 */
@Tag(name = "Personnel — Invitations", description = "FR-05a — Resend invitation judge khách")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
@CoordinatorOnly
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/{id}/resend")
    @Operation(summary = "Resend email judge khách mời",
            description = "Chỉ role JUDGE; token hết hạn mới resend. Gửi MK tạm mới + link đăng nhập.")
    public ResponseEntity<ApiResponse<TempJudgeResponse.InvitationInfo>> resend(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.resend(id), "Invitation resent"));
    }

    @PostMapping("/{id}/revoke")
    @Operation(summary = "Thu hồi lời mời judge khách mời",
            description = "Chỉ thu hồi lời mời PENDING chưa activate. Giữ nguyên tài khoản temp để tránh tái sử dụng email qua UI.")
    public ResponseEntity<ApiResponse<TempJudgeResponse.InvitationInfo>> revoke(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.revoke(id), "Invitation revoked"));
    }
}
