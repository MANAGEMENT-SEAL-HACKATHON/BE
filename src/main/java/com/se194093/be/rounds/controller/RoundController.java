package com.se194093.be.rounds.controller;

import com.se194093.be.common.response.ApiResponse;
import com.se194093.be.common.security.CoordinatorOnly;
import com.se194093.be.rounds.dto.request.CreateRoundRequest;
import com.se194093.be.rounds.dto.request.UpdateRoundRequest;
import com.se194093.be.rounds.dto.response.RoundResponse;
import com.se194093.be.rounds.dto.response.RoundSummaryResponse;
import com.se194093.be.rounds.service.RoundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * FR-03 — Round CRUD controller. Activate (FR-06B) ở {@link RoundActivationController}.
 */
@RestController
@RequiredArgsConstructor
@CoordinatorOnly
public class RoundController {

    private final RoundService roundService;

    @PostMapping("/api/v1/tracks/{trackId}/rounds")
    public ResponseEntity<ApiResponse<RoundResponse>> create(
            @PathVariable Integer trackId,
            @Valid @RequestBody CreateRoundRequest req
    ) {
        RoundResponse data = roundService.create(trackId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/rounds/{id}")
                .buildAndExpand(data.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.created(data));
    }

    @GetMapping("/api/v1/tracks/{trackId}/rounds")
    public ResponseEntity<ApiResponse<List<RoundSummaryResponse>>> listByTrack(@PathVariable Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(roundService.listByTrack(trackId)));
    }

    @GetMapping("/api/v1/rounds/{id}")
    public ResponseEntity<ApiResponse<RoundResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(roundService.getById(id)));
    }

    @PutMapping("/api/v1/rounds/{id}")
    public ResponseEntity<ApiResponse<RoundResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateRoundRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(roundService.update(id, req)));
    }

    @DeleteMapping("/api/v1/rounds/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer id) {
        Integer deletedId = roundService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", deletedId), "Deleted"));
    }
}
