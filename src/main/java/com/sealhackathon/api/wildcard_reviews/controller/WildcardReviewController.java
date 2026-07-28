package com.sealhackathon.api.wildcard_reviews.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardOverrideRequest;
import com.sealhackathon.api.wildcard_reviews.dto.response.WildcardReviewResponse;
import com.sealhackathon.api.wildcard_reviews.service.WildcardReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wildcard Reviews (GĐ4)", description = "Plan C Override — per-row PATCH removed; use confirm proposal + override")
@RestController
@RequestMapping("/api/v1/wildcard-reviews")
@RequiredArgsConstructor
public class WildcardReviewController {

    private final WildcardReviewService wildcardReviewService;

    @PostMapping("/{id}/override")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Plan C — Override quyết định vé vớt sau khi LOCKED")
    public ResponseEntity<ApiResponse<WildcardReviewResponse>> override(
            @PathVariable Integer id,
            @Valid @RequestBody WildcardOverrideRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(wildcardReviewService.overrideReview(id, req)));
    }
}
