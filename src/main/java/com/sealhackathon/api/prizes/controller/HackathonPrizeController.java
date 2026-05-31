package com.sealhackathon.api.prizes.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.prizes.dto.request.AwardPrizeRequest;
import com.sealhackathon.api.prizes.dto.response.PrizeResponse;
import com.sealhackathon.api.prizes.service.PrizeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Prizes (GĐ6)", description = "FR-GĐ6 — Trao giải hackathon")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/hackathons")
@RequiredArgsConstructor
@CoordinatorOnly
public class HackathonPrizeController {

    private final PrizeService prizeService;

    @PostMapping("/{hackathonId}/prizes")
    @Operation(summary = "Trao giải cho đội", description = "Chỉ khi hackathon PENDING_CONFIRM; chặn trùng đội/loại giải.")
    public ResponseEntity<ApiResponse<PrizeResponse>> award(
            @PathVariable Integer hackathonId,
            @Valid @RequestBody AwardPrizeRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(prizeService.award(hackathonId, req)));
    }

    @GetMapping("/{hackathonId}/prizes")
    @Operation(summary = "FR-32 — Danh sách giải đã trao (stub gates)")
    public ResponseEntity<ApiResponse<java.util.List<PrizeResponse>>> list(
            @PathVariable Integer hackathonId) {
        return ResponseEntity.ok(ApiResponse.ok(prizeService.listByHackathon(hackathonId)));
    }
}
