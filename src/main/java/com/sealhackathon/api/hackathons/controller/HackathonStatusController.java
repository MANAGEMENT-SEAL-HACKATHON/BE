package com.sealhackathon.api.hackathons.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.sealhackathon.api.hackathons.dto.request.ChangeHackathonStatusRequest;
import com.sealhackathon.api.hackathons.dto.response.HackathonReadinessResponse;
import com.sealhackathon.api.hackathons.dto.response.HackathonResponse;
import com.sealhackathon.api.hackathons.service.HackathonReadinessService;
import com.sealhackathon.api.hackathons.service.HackathonStatusService;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-06 — Readiness dry-run + Change status.
 *
 * <p>Tách thành controller riêng (không gộp với {@link HackathonController}) để rõ phân quyền
 * mutation trạng thái có thể cần audit/lock chặt hơn.
 */
@Tag(name = "Status", description = "FR-07 — Readiness gate + PATCH hackathon status")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/hackathons")
@RequiredArgsConstructor
@CoordinatorOnly
public class HackathonStatusController {

    private final HackathonReadinessService readinessService;
    private final HackathonStatusService statusService;

    @GetMapping("/{id}/readiness")
    public ResponseEntity<ApiResponse<HackathonReadinessResponse>> readiness(
            @PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "ONGOING") HackathonStatus target
    ) {
        return ResponseEntity.ok(ApiResponse.ok(readinessService.check(id, target)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<HackathonResponse>> changeStatus(
            @PathVariable Integer id,
            @Valid @RequestBody ChangeHackathonStatusRequest req
    ) {
        HackathonResponse data = statusService.changeStatus(id, req);
        String msg = "Status changed to " + data.getStatus();
        return ResponseEntity.ok(ApiResponse.ok(data, msg));
    }
}
