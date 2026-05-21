package com.sealhackathon.api.users.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.response.PageResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.sealhackathon.api.users.dto.request.CreateTempJudgeRequest;
import com.sealhackathon.api.users.dto.response.TempJudgeResponse;
import com.sealhackathon.api.users.dto.response.UserSummaryResponse;
import com.sealhackathon.api.users.service.TempJudgeService;
import com.sealhackathon.api.invitations.controller.InvitationController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-05a — Tạo / list Judge khách mời. Resend invitation tách ở {@link InvitationController}.
 */
@Tag(name = "Personnel — Temp judge", description = "FR-05a — Tạo judge khách mời")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/users/temp-judges")
@RequiredArgsConstructor
@CoordinatorOnly
public class TempJudgeController {

    private final TempJudgeService tempJudgeService;

    @PostMapping
    @Operation(summary = "Tạo judge khách mời", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<TempJudgeResponse>> create(
            @Valid @RequestBody CreateTempJudgeRequest req
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(tempJudgeService.createTempJudge(req)));
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm judge khách mời", description = "Chỉ coordinator mới có quyền thực hiện hành động này. Có thể tìm kiếm theo institution hoặc tên/email (q).")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> search(
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tempJudgeService.search(institution, q, pageable)));
    }
}
