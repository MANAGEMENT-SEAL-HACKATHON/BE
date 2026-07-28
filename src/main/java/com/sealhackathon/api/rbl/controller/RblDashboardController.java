package com.sealhackathon.api.rbl.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.rbl.dto.response.RblScoringProgressResponse;
import com.sealhackathon.api.rbl.dto.response.RblVarianceResponse;
import com.sealhackathon.api.rbl.service.RblDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RBL Dashboard (GĐ5)", description = "FR-30 — Độ tin cậy chấm điểm")
@RestController
@RequestMapping("/api/v1/rounds")
@RequiredArgsConstructor
public class RblDashboardController {

    private final RblDashboardService rblDashboardService;

    @GetMapping("/{id}/rbl/variance")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-30 — Phương sai chấm điểm (per-judge + inter-rater by criterion)")
    public ResponseEntity<ApiResponse<RblVarianceResponse>> variance(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(rblDashboardService.varianceByRound(id)));
    }

    @GetMapping("/{id}/rbl/progress")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-30 — Tiến độ chấm (RBL view)")
    public ResponseEntity<ApiResponse<RblScoringProgressResponse>> progress(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(rblDashboardService.scoringProgress(id)));
    }
}
