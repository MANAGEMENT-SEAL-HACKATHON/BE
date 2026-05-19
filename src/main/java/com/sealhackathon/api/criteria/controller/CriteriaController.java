package com.sealhackathon.api.criteria.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.criteria.dto.request.BatchCreateCriteriaRequest;
import com.sealhackathon.api.criteria.dto.request.CloneCriteriaRequest;
import com.sealhackathon.api.criteria.dto.request.CreateCriterionRequest;
import com.sealhackathon.api.criteria.dto.request.UpdateCriterionRequest;
import com.sealhackathon.api.criteria.dto.response.BatchCreateResponse;
import com.sealhackathon.api.criteria.dto.response.CloneResponse;
import com.sealhackathon.api.criteria.dto.response.CriteriaListResponse;
import com.sealhackathon.api.criteria.dto.response.CriterionResponse;
import com.sealhackathon.api.criteria.dto.response.WeightSummaryResponse;
import com.sealhackathon.api.criteria.service.CriteriaService;
import com.sealhackathon.api.criteria.service.WeightSummaryService;
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
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CoordinatorOnly
public class CriteriaController {

    private final CriteriaService criteriaService;
    private final WeightSummaryService weightSummaryService;

    // ---------- Track (Sơ loại) ----------
    @PostMapping("/api/v1/tracks/{trackId}/criteria")
    public ResponseEntity<ApiResponse<CriterionResponse>> createForTrack(
            @PathVariable Integer trackId,
            @Valid @RequestBody CreateCriterionRequest req
    ) {
        CriteriaService.CreateResult result = criteriaService.createForTrack(trackId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/criteria/{id}")
                .buildAndExpand(result.criterion().getId())
                .toUri();
        return ResponseEntity.created(location).body(
                ApiResponse.createdWithWarnings(result.criterion(), criteriaService.wrap(result.weightWarning()))
        );
    }

    @PostMapping("/api/v1/tracks/{trackId}/criteria/batch")
    public ResponseEntity<ApiResponse<BatchCreateResponse>> batchCreateForTrack(
            @PathVariable Integer trackId,
            @Valid @RequestBody BatchCreateCriteriaRequest req
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                criteriaService.batchCreateForTrack(trackId, req)));
    }

    @GetMapping("/api/v1/tracks/{trackId}/criteria")
    public ResponseEntity<ApiResponse<CriteriaListResponse>> listByTrack(@PathVariable Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(criteriaService.listByTrack(trackId)));
    }

    @GetMapping("/api/v1/tracks/{trackId}/criteria/weight-summary")
    public ResponseEntity<ApiResponse<WeightSummaryResponse>> weightSummaryForTrack(
            @PathVariable Integer trackId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(weightSummaryService.summaryForTrack(trackId)));
    }

    @PostMapping("/api/v1/tracks/{trackId}/criteria/clone")
    public ResponseEntity<ApiResponse<CloneResponse>> cloneForTrack(
            @PathVariable Integer trackId,
            @Valid @RequestBody CloneCriteriaRequest req
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                criteriaService.cloneFromSourceForTrack(trackId, req)));
    }

    // ---------- Round FINAL (Chung kết) ----------
    @PostMapping("/api/v1/rounds/{roundId}/criteria")
    public ResponseEntity<ApiResponse<CriterionResponse>> createForFinalRound(
            @PathVariable Integer roundId,
            @Valid @RequestBody CreateCriterionRequest req
    ) {
        CriteriaService.CreateResult result = criteriaService.createForFinalRound(roundId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/criteria/{id}")
                .buildAndExpand(result.criterion().getId())
                .toUri();
        return ResponseEntity.created(location).body(
                ApiResponse.createdWithWarnings(result.criterion(), criteriaService.wrap(result.weightWarning()))
        );
    }

    @PostMapping("/api/v1/rounds/{roundId}/criteria/batch")
    public ResponseEntity<ApiResponse<BatchCreateResponse>> batchCreateForFinalRound(
            @PathVariable Integer roundId,
            @Valid @RequestBody BatchCreateCriteriaRequest req
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                criteriaService.batchCreateForFinalRound(roundId, req)));
    }

    @GetMapping("/api/v1/rounds/{roundId}/criteria")
    public ResponseEntity<ApiResponse<CriteriaListResponse>> listByFinalRound(@PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(criteriaService.listByFinalRound(roundId)));
    }

    @GetMapping("/api/v1/rounds/{roundId}/criteria/weight-summary")
    public ResponseEntity<ApiResponse<WeightSummaryResponse>> weightSummaryForFinalRound(
            @PathVariable Integer roundId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(weightSummaryService.summaryForFinalRound(roundId)));
    }

    @PostMapping("/api/v1/rounds/{roundId}/criteria/clone")
    public ResponseEntity<ApiResponse<CloneResponse>> cloneForFinalRound(
            @PathVariable Integer roundId,
            @Valid @RequestBody CloneCriteriaRequest req
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                criteriaService.cloneFromSourceForFinalRound(roundId, req)));
    }

    @GetMapping("/api/v1/criteria/{id}")
    public ResponseEntity<ApiResponse<CriterionResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(criteriaService.getById(id)));
    }

    @PutMapping("/api/v1/criteria/{id}")
    public ResponseEntity<ApiResponse<CriterionResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateCriterionRequest req
    ) {
        CriteriaService.UpdateResult result = criteriaService.update(id, req);
        return ResponseEntity.ok(
                ApiResponse.okWithWarnings(result.criterion(), criteriaService.wrap(result.weightWarning()))
        );
    }

    @DeleteMapping("/api/v1/criteria/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer id) {
        Integer deletedId = criteriaService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", deletedId), "Deleted"));
    }
}
