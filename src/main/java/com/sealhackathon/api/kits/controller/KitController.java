package com.sealhackathon.api.kits.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.kits.dto.request.*;
import com.sealhackathon.api.kits.dto.response.*;
import com.sealhackathon.api.kits.service.KitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Tag(name = "Kits", description = "Phase 4 — tồn kho & phát kit")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CoordinatorOnly
@ConditionalOnProperty(name = "app.kits.enabled", havingValue = "true", matchIfMissing = true)
public class KitController {

    private final KitService kitService;

    @GetMapping("/hackathons/{hackathonId}/kit-items")
    @Operation(summary = "Danh sách món kit + tồn kho theo size")
    public ResponseEntity<ApiResponse<List<KitItemResponse>>> listItems(@PathVariable Integer hackathonId) {
        return ResponseEntity.ok(ApiResponse.ok(kitService.listItems(hackathonId)));
    }

    @PostMapping("/hackathons/{hackathonId}/kit-items")
    @Operation(summary = "Tạo món kit")
    public ResponseEntity<ApiResponse<KitItemResponse>> createItem(
            @PathVariable Integer hackathonId,
            @Valid @RequestBody CreateKitItemRequest req) {
        KitItemResponse created = kitService.createItem(hackathonId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/kit-items/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.created(created));
    }

    @PutMapping("/kit-items/{id}")
    @Operation(summary = "Cập nhật món kit")
    public ResponseEntity<ApiResponse<KitItemResponse>> updateItem(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateKitItemRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(kitService.updateItem(id, req)));
    }

    @DeleteMapping("/kit-items/{id}")
    @Operation(summary = "Xóa món kit (kèm stock + allocation)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteItem(@PathVariable Integer id) {
        kitService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", id), "Deleted"));
    }

    @PutMapping("/kit-items/{id}/stock")
    @Operation(summary = "Upsert tồn kho theo size")
    public ResponseEntity<ApiResponse<KitStockResponse>> upsertStock(
            @PathVariable Integer id,
            @Valid @RequestBody UpsertKitStockRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(kitService.upsertStock(id, req)));
    }

    @GetMapping("/hackathons/{hackathonId}/kits/recipients")
    @Operation(summary = "Danh sách người nhận kit (ACCEPTED + ACTIVE) kèm allocation")
    public ResponseEntity<ApiResponse<List<KitRecipientResponse>>> listRecipients(
            @PathVariable Integer hackathonId,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.ok(kitService.listRecipients(hackathonId, q)));
    }

    @PostMapping("/hackathons/{hackathonId}/kits/issue")
    @Operation(summary = "Phát một món kit cho sinh viên")
    public ResponseEntity<ApiResponse<KitAllocationResponse>> issue(
            @PathVariable Integer hackathonId,
            @Valid @RequestBody IssueKitRequest req) {
        KitService.IssueResult result = kitService.issue(hackathonId, req);
        return ResponseEntity.ok(ApiResponse.okWithWarnings(result.allocation(), result.warnings()));
    }

    @PostMapping("/kit-allocations/{id}/revoke")
    @Operation(summary = "Thu hồi kit đã phát (bắt buộc lý do)")
    public ResponseEntity<ApiResponse<KitAllocationResponse>> revoke(
            @PathVariable Integer id,
            @Valid @RequestBody RevokeKitRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(kitService.revoke(id, req)));
    }

    @GetMapping("/hackathons/{hackathonId}/kits/reconciliation")
    @Operation(summary = "Đối soát cuối buổi: tổng / đã phát / còn / eligible / chênh lệch")
    public ResponseEntity<ApiResponse<List<KitReconciliationLineResponse>>> reconciliation(
            @PathVariable Integer hackathonId) {
        return ResponseEntity.ok(ApiResponse.ok(kitService.reconciliation(hackathonId)));
    }
}
