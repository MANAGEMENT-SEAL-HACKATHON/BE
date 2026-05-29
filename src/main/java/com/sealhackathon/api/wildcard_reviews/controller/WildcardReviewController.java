package com.sealhackathon.api.wildcard_reviews.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.rounds.service.RoundProgressionService;
import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardReviewDecisionRequest;
import com.sealhackathon.api.wildcard_reviews.dto.response.WildcardReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wildcard Reviews (GĐ4)", description = "FR-22A — Duyệt Wild Card")
@RestController
@RequestMapping("/api/v1/wildcard-reviews")
@RequiredArgsConstructor
public class WildcardReviewController {

    private final RoundProgressionService progressionService;

    @PatchMapping("/{id}")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-22A — Approve/reject wildcard candidate")
    public ResponseEntity<ApiResponse<WildcardReviewResponse>> decide(
            @PathVariable Integer id,
            @Valid @RequestBody WildcardReviewDecisionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.decideWildcardReview(id, req)));
    }
}
