package com.sealhackathon.api.criteria.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Criteria", description = "FR-04 — Criteria (track XOR round FINAL)")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@CoordinatorOnly
public class CriteriaController {

    private final CriteriaService criteriaService;
    private final WeightSummaryService weightSummaryService;

    // ---------- Track (Sơ loại) ----------
    @PostMapping("/api/v1/tracks/{trackId}/criteria")
    @Operation(summary = "Tạo criterion mới cho track", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
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
    @Operation(summary = "Tạo nhiều criterion cho track", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<BatchCreateResponse>> batchCreateForTrack(
            @PathVariable Integer trackId,
            @Valid @RequestBody BatchCreateCriteriaRequest req
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                criteriaService.batchCreateForTrack(trackId, req)));
    }

    @GetMapping("/api/v1/tracks/{trackId}/criteria")
    @Operation(summary = "Lấy danh sách criteria của track", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<CriteriaListResponse>> listByTrack(@PathVariable Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(criteriaService.listByTrack(trackId)));
    }

    @GetMapping("/api/v1/tracks/{trackId}/criteria/weight-summary")
    @Operation(summary = "Tổng hợp trọng số của criteria trong track", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<WeightSummaryResponse>> weightSummaryForTrack(
            @PathVariable Integer trackId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(weightSummaryService.summaryForTrack(trackId)));
    }

    @PostMapping("/api/v1/tracks/{trackId}/criteria/clone")
    @Operation(summary = "Clone criteria từ track khác", description = "Clone criteria từ track nguồn sang track đích.")
    public ResponseEntity<ApiResponse<CloneResponse>> cloneForTrack(
            @PathVariable Integer trackId,
            @Valid @RequestBody CloneCriteriaRequest req
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                criteriaService.cloneFromSourceForTrack(trackId, req)));
    }

    // ---------- Round FINAL (Chung kết) ----------
    @PostMapping("/api/v1/rounds/{roundId}/criteria")
    @Operation(summary = "Tạo criterion mới cho round FINAL", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
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
    @Operation(summary = "Tạo hàng loạt criteria cho track", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<BatchCreateResponse>> batchCreateForFinalRound(
            @PathVariable Integer roundId,
            @Valid @RequestBody BatchCreateCriteriaRequest req
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                criteriaService.batchCreateForFinalRound(roundId, req)));
    }

    @GetMapping("/api/v1/rounds/{roundId}/criteria")
    @Operation(summary = "Lấy danh sách criteria của round FINAL", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<CriteriaListResponse>> listByFinalRound(@PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(criteriaService.listByFinalRound(roundId)));
    }

    @GetMapping("/api/v1/rounds/{roundId}/criteria/weight-summary")
    @Operation(summary = "Tổng hợp trọng số của criteria trong round FINAL", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<WeightSummaryResponse>> weightSummaryForFinalRound(
            @PathVariable Integer roundId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(weightSummaryService.summaryForFinalRound(roundId)));
    }

    @PostMapping("/api/v1/rounds/{roundId}/criteria/clone")
    @Operation(summary = "Clone criteria từ round FINAL khác", description = "Clone criteria từ round Chung kết nguồn sang round Chung kết đích.")
    public ResponseEntity<ApiResponse<CloneResponse>> cloneForFinalRound(
            @PathVariable Integer roundId,
            @Valid @RequestBody CloneCriteriaRequest req
    ) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                criteriaService.cloneFromSourceForFinalRound(roundId, req)));
    }

    @GetMapping("/api/v1/criteria/{id}")
    @Operation(summary = "Lấy thông tin chi tiết criterion theo ID", description = "Chỉ coordinator mới có quyền thực hiện hành động này. Trả về lỗi 404 nếu không tìm thấy criterion với ID đã cho.")
    public ResponseEntity<ApiResponse<CriterionResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(criteriaService.getById(id)));
    }

    @PutMapping("/api/v1/criteria/{id}")
    @Operation(summary = "Cập nhật thông tin criterion", description = "Chỉ coordinator mới có quyền thực hiện hành động này. Trả về lỗi 404 nếu không tìm thấy criterion với ID đã cho.")
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
    @Operation(summary = "Xóa criterion", description = "Chỉ coordinator mới có quyền thực hiện hành động này. Trả về lỗi 404 nếu không tìm thấy criterion với ID đã cho.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer id) {
        Integer deletedId = criteriaService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", deletedId), "Deleted"));
    }
}
