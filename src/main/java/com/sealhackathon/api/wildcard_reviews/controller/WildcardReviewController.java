package com.sealhackathon.api.wildcard_reviews.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.rounds.service.RoundProgressionService;
import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardOverrideRequest;
import com.sealhackathon.api.wildcard_reviews.dto.request.WildcardReviewDecisionRequest;
import com.sealhackathon.api.wildcard_reviews.dto.response.WildcardReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wildcard Reviews (GĐ4)", description = "Plan C Override — legacy per-row decide deprecated")
@RestController
@RequestMapping("/api/v1/wildcard-reviews")
@RequiredArgsConstructor
@Slf4j
public class WildcardReviewController {

    private final RoundProgressionService progressionService;
    private final CurrentUserAccessor currentUserAccessor;

    /**
     * @deprecated Plan C: dùng {@code POST /rounds/{id}/wildcard-proposal/confirm} + Override.
     * Endpoint còn để tương thích; sẽ bị gỡ. Mỗi lần gọi ghi WARN để phát hiện caller ẩn.
     */
    @Deprecated
    @PatchMapping("/{id}")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(
            summary = "[DEPRECATED] Legacy FR-22A per-row approve/reject — dùng Confirm proposal + Override",
            deprecated = true)
    public ResponseEntity<ApiResponse<WildcardReviewResponse>> decide(
            @PathVariable Integer id,
            @Valid @RequestBody WildcardReviewDecisionRequest req) {
        Integer userId = currentUserAccessor.currentUserId();
        log.warn(
                "DEPRECATED API called: PATCH /api/v1/wildcard-reviews/{} by userId={} — migrate to Plan C confirm/override",
                id, userId);
        return ResponseEntity.ok(ApiResponse.ok(progressionService.decideWildcardReview(id, req)));
    }

    @PostMapping("/{id}/override")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Plan C — Override quyết định vé vớt sau khi LOCKED")
    public ResponseEntity<ApiResponse<WildcardReviewResponse>> override(
            @PathVariable Integer id,
            @Valid @RequestBody WildcardOverrideRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.overrideWildcardReview(id, req)));
    }
}
