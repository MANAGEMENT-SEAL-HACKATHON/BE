package com.sealhackathon.api.rounds.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.sealhackathon.api.rounds.dto.request.ActivateRoundRequest;
import com.sealhackathon.api.rounds.dto.response.RoundResponse;
import com.sealhackathon.api.rounds.service.RoundActivationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-06B — Activate Round (safety net validate weight = 1.0).
 *
 * <p>Tách thành controller riêng để dễ áp guard / rate-limit nếu cần ở phase sau.
 */
@Tag(name = "Round — Activate", description = "FR-07B — Kích hoạt round (GĐ3)")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/rounds")
@RequiredArgsConstructor
@CoordinatorOnly
public class RoundActivationController {

    private final RoundActivationService activationService;

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Kích hoạt round", description = "Chỉ coordinator mới có quyền thực hiện hành động này. Endpoint này sẽ validate round có weight = 1.0 hay không, nếu không sẽ trả về lỗi.")
    public ResponseEntity<ApiResponse<RoundResponse>> activate(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) ActivateRoundRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                activationService.activate(id, req), "Round activated"));
    }
}
