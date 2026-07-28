package com.sealhackathon.api.criteria.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.criteria.dto.request.ApplyCriteriaTemplateRequest;
import com.sealhackathon.api.criteria.dto.request.CriteriaTemplateRequest;
import com.sealhackathon.api.criteria.dto.response.CriteriaTemplateResponse;
import com.sealhackathon.api.criteria.service.CriteriaTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CoordinatorOnly
@RequestMapping("/api/v1")
public class CriteriaTemplateController {
    private final CriteriaTemplateService service;

    @GetMapping("/criteria-templates")
    public ResponseEntity<ApiResponse<List<CriteriaTemplateResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.list()));
    }

    @GetMapping("/criteria-templates/{id}")
    public ResponseEntity<ApiResponse<CriteriaTemplateResponse>> get(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(id)));
    }

    @PostMapping("/criteria-templates")
    public ResponseEntity<ApiResponse<CriteriaTemplateResponse>> create(
            @Valid @RequestBody CriteriaTemplateRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(service.create(request)));
    }

    @PutMapping("/criteria-templates/{id}")
    public ResponseEntity<ApiResponse<CriteriaTemplateResponse>> update(
            @PathVariable Integer id, @Valid @RequestBody CriteriaTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, request)));
    }

    @DeleteMapping("/criteria-templates/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", id), "Deleted"));
    }

    @PostMapping("/tracks/{trackId}/criteria/templates/{templateId}/apply")
    public ResponseEntity<ApiResponse<CriteriaTemplateService.ApplyResult>> applyToTrack(
            @PathVariable Integer trackId,
            @PathVariable Integer templateId,
            @RequestBody(required = false) ApplyCriteriaTemplateRequest request) {
        boolean replace = request != null && request.replace();
        return ResponseEntity.status(201).body(
                ApiResponse.created(service.applyToTrack(templateId, trackId, replace)));
    }

    @PostMapping("/rounds/{roundId}/criteria/templates/{templateId}/apply")
    public ResponseEntity<ApiResponse<CriteriaTemplateService.ApplyResult>> applyToRound(
            @PathVariable Integer roundId,
            @PathVariable Integer templateId,
            @RequestBody(required = false) ApplyCriteriaTemplateRequest request) {
        boolean replace = request != null && request.replace();
        return ResponseEntity.status(201).body(
                ApiResponse.created(service.applyToFinalRound(templateId, roundId, replace)));
    }
}
