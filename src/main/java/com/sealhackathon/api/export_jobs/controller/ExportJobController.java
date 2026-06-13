package com.sealhackathon.api.export_jobs.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.export_jobs.dto.response.ExportFileDownload;
import com.sealhackathon.api.export_jobs.dto.response.ExportJobResponse;
import com.sealhackathon.api.export_jobs.service.ExportJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Export Jobs (GĐ6)", description = "MF-06 FR-34/35 — Xuất CSV/RBL")
@RestController
@RequestMapping("/api/v1/export-jobs")
@RequiredArgsConstructor
@CoordinatorOnly
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ExportJobController {

    private final ExportJobService exportJobService;

    @GetMapping("/{id}")
    @Operation(summary = "FR-34 — Trạng thái export job")
    public ResponseEntity<ApiResponse<ExportJobResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(exportJobService.getById(id)));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "FR-34/35 — Tải file CSV export (attachment)")
    public ResponseEntity<byte[]> download(@PathVariable Integer id) throws java.io.IOException {
        ExportFileDownload file = exportJobService.downloadFile(id);
        String contentType = file.content().contentType() != null
                ? file.content().contentType()
                : "text/csv";
        byte[] bytes;
        try (var stream = file.content().stream()) {
            bytes = stream.readAllBytes();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }
}
