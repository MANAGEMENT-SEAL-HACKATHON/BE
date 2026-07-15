package com.sealhackathon.api.prizes.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.prizes.dto.request.UpdateAwardedPrizeRequest;
import com.sealhackathon.api.prizes.dto.response.PrizeResponse;
import com.sealhackathon.api.prizes.service.PrizeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Prizes (GĐ6)", description = "FR-32 — Thu hồi / sửa giải đã trao")
@RestController
@RequestMapping("/api/v1/prizes")
@RequiredArgsConstructor
@CoordinatorOnly
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class PrizeController {

    private final PrizeService prizeService;

    @PatchMapping("/{id}")
    @Operation(summary = "PRIZE-02 — Sửa giải đã trao (typo / đổi đội cùng loại)")
    public ResponseEntity<ApiResponse<PrizeResponse>> updateAwarded(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateAwardedPrizeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(prizeService.updateAwarded(id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "FR-32 — Thu hồi giải")
    public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable Integer id) {
        prizeService.revoke(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
