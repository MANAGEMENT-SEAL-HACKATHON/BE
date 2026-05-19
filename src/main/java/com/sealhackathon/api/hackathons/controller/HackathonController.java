package com.sealhackathon.api.hackathons.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.response.PageResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.hackathons.dto.request.CreateHackathonRequest;
import com.sealhackathon.api.hackathons.dto.request.UpdateHackathonRequest;
import com.sealhackathon.api.hackathons.dto.response.HackathonResponse;
import com.sealhackathon.api.hackathons.dto.response.HackathonSummaryResponse;
import com.sealhackathon.api.hackathons.service.HackathonService;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.hackathons.value_object.Season;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * FR-01 — REST controller cho Hackathon CRUD.
 *
 * <p>Spec: {@code docs/api/mf-01/fr-01-hackathons.md}.
 * <p>State transition (FR-06) ở {@link HackathonStatusController}.
 */
@RestController
@RequestMapping("/api/v1/hackathons")
@RequiredArgsConstructor
@CoordinatorOnly
public class HackathonController {

    private final HackathonService hackathonService;

    @PostMapping
    public ResponseEntity<ApiResponse<HackathonResponse>> create(@Valid @RequestBody CreateHackathonRequest req) {
        HackathonResponse data = hackathonService.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(data.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.created(data));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<HackathonSummaryResponse>>> search(
            @RequestParam(required = false) HackathonStatus status,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Season season,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hackathonService.search(status, year, season, q, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HackathonResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(hackathonService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HackathonResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateHackathonRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hackathonService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer id) {
        Integer deletedId = hackathonService.delete(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.ok(Map.of("deletedId", deletedId), "Deleted"));
    }
}
