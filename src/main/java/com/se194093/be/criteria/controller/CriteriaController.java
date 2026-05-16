package com.se194093.be.criteria.controller;

import com.se194093.be.common.response.ApiResponse;
import com.se194093.be.common.security.CoordinatorOnly;
import com.se194093.be.criteria.dto.request.BatchCreateCriteriaRequest;
import com.se194093.be.criteria.dto.request.CloneCriteriaRequest;
import com.se194093.be.criteria.dto.request.CreateCriterionRequest;
import com.se194093.be.criteria.dto.request.UpdateCriterionRequest;
import com.se194093.be.criteria.dto.response.BatchCreateResponse;
import com.se194093.be.criteria.dto.response.CloneResponse;
import com.se194093.be.criteria.dto.response.CriteriaListResponse;
import com.se194093.be.criteria.dto.response.CriterionResponse;
import com.se194093.be.criteria.dto.response.WeightSummaryResponse;
import com.se194093.be.criteria.service.CriteriaService;
import com.se194093.be.criteria.service.WeightSummaryService;
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

/**
 * FR-04 — Criteria CRUD + batch + weight-summary + clone.
 *
 * <p>WARN mềm weight được gắn vào field {@code warnings} của {@link ApiResponse} thông qua
 * {@link CriteriaService.CreateResult#weightWarning()} / {@link CriteriaService.UpdateResult#weightWarning()}.
 */
@RestController
@RequiredArgsConstructor
@CoordinatorOnly
public class CriteriaController {

    private final CriteriaService criteriaService;
    private final WeightSummaryService weightSummaryService;

    @PostMapping("/api/v1/rounds/{roundId}/criteria")
    public ResponseEntity<ApiResponse<CriterionResponse>> create(
            @PathVariable Integer roundId,
            @Valid @RequestBody CreateCriterionRequest req
    ) {
        CriteriaService.CreateResult result = criteriaService.create(roundId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/criteria/{id}")
                .buildAndExpand(result.criterion().getId())
                .toUri();
        return ResponseEntity.created(location).body(
                ApiResponse.createdWithWarnings(result.criterion(), criteriaService.wrap(result.weightWarning()))
        );
    }

    @PostMapping("/api/v1/rounds/{roundId}/criteria/batch")
    public ResponseEntity<ApiResponse<BatchCreateResponse>> batchCreate(
            @PathVariable Integer roundId,
            @Valid @RequestBody BatchCreateCriteriaRequest req
    ) {
        BatchCreateResponse data = criteriaService.batchCreate(roundId, req);
        return ResponseEntity.status(201).body(ApiResponse.created(data));
    }

    @GetMapping("/api/v1/rounds/{roundId}/criteria")
    public ResponseEntity<ApiResponse<CriteriaListResponse>> listByRound(@PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(criteriaService.listByRound(roundId)));
    }

    @GetMapping("/api/v1/rounds/{roundId}/criteria/weight-summary")
    public ResponseEntity<ApiResponse<WeightSummaryResponse>> weightSummary(@PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(weightSummaryService.summary(roundId)));
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

    @PostMapping("/api/v1/rounds/{roundId}/criteria/clone")
    public ResponseEntity<ApiResponse<CloneResponse>> cloneFromSource(
            @PathVariable Integer roundId,
            @Valid @RequestBody CloneCriteriaRequest req
    ) {
        CloneResponse data = criteriaService.cloneFromSource(roundId, req);
        return ResponseEntity.status(201).body(ApiResponse.created(data));
    }
}
