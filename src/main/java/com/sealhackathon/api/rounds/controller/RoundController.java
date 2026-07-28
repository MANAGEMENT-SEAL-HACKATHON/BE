package com.sealhackathon.api.rounds.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.sealhackathon.api.rounds.dto.request.CreateRoundRequest;
import com.sealhackathon.api.rounds.dto.request.UpdateRoundRequest;
import com.sealhackathon.api.rounds.dto.response.RoundResponse;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;
import com.sealhackathon.api.rounds.service.RoundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * FR-02 Round CRUD. Activate ở {@link RoundActivationController}.
 */
@Tag(name = "Round", description = "FR-02 — Round CRUD (dưới hackathon)")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@CoordinatorOnly
public class RoundController {

    private final RoundService roundService;

    @PostMapping("/api/v1/hackathons/{hackathonId}/rounds")
    @Operation(summary = "Tạo round mới cho hackathon", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<RoundResponse>> createByHackathon(
            @PathVariable Integer hackathonId,
            @Valid @RequestBody CreateRoundRequest req
    ) {
        RoundResponse data = roundService.createByHackathon(hackathonId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/rounds/{id}")
                .buildAndExpand(data.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.created(data));
    }

    @GetMapping("/api/v1/hackathons/{hackathonId}/rounds")
    @Operation(summary = "Liệt kê các round của hackathon", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<List<RoundSummaryResponse>>> listByHackathon(
            @PathVariable Integer hackathonId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(roundService.listByHackathon(hackathonId)));
    }

    @GetMapping("/api/v1/rounds/{id}")
    @Operation(summary = "Xem chi tiết round", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<RoundResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(roundService.getById(id)));
    }

    @PutMapping("/api/v1/rounds/{id}")
    @Operation(summary = "Cập nhật round", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<RoundResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateRoundRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(roundService.update(id, req)));
    }

    @DeleteMapping("/api/v1/rounds/{id}")
    @Operation(summary = "Xóa round", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer id) {
        Integer deletedId = roundService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", deletedId), "Deleted"));
    }

    @PostMapping(value = "/api/v1/rounds/{id}/problem-statement", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file PDF đề bài (trước khi phát)")
    public ResponseEntity<ApiResponse<RoundResponse>> uploadProblemStatement(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(roundService.uploadProblemStatement(id, file)));
    }

    @PostMapping("/api/v1/rounds/{id}/dismiss-final-problem-migration-banner")
    @Operation(summary = "Dismiss banner migration đề CK (một lần theo round)")
    public ResponseEntity<ApiResponse<RoundResponse>> dismissFinalProblemMigrationBanner(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(roundService.dismissFinalProblemMigrationBanner(id)));
    }

    @GetMapping("/api/v1/rounds/{id}/problem-statement")
    @Operation(summary = "Tải file PDF đề bài")
    public ResponseEntity<Resource> downloadProblemStatement(@PathVariable Integer id) {
        Resource resource = roundService.downloadProblemStatement(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"de-bai.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
