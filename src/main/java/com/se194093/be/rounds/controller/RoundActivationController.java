package com.se194093.be.rounds.controller;

import com.se194093.be.common.response.ApiResponse;
import com.se194093.be.common.security.CoordinatorOnly;
import com.se194093.be.rounds.dto.request.ActivateRoundRequest;
import com.se194093.be.rounds.dto.response.RoundResponse;
import com.se194093.be.rounds.service.RoundActivationService;
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
@RestController
@RequestMapping("/api/v1/rounds")
@RequiredArgsConstructor
@CoordinatorOnly
public class RoundActivationController {

    private final RoundActivationService activationService;

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<RoundResponse>> activate(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) ActivateRoundRequest req
    ) {
        String note = req == null ? null : req.getNote();
        return ResponseEntity.ok(ApiResponse.ok(activationService.activate(id, note), "Round activated"));
    }
}
