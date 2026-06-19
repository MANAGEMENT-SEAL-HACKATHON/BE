package com.sealhackathon.api.tracks.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.sealhackathon.api.tracks.dto.request.CreateTrackRequest;
import com.sealhackathon.api.tracks.dto.request.UpdateTrackRequest;
import com.sealhackathon.api.tracks.dto.response.TrackResponse;
import com.sealhackathon.api.tracks.dto.response.TrackSummaryResponse;
import com.sealhackathon.api.tracks.service.TrackService;
import com.sealhackathon.api.tracks.value_object.TrackStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * FR-02 — Track controller.
 *
 * <p>Routes:
 * <ul>
 *   <li>POST/GET nested theo Round: {@code /rounds/{roundId}/tracks}</li>
 *   <li>GET nested theo Hackathon: {@code /hackathons/{hackathonId}/tracks}</li>
 *   <li>GET/PUT/DELETE single: {@code /tracks/{id}}</li>
 * </ul>
 */
@Tag(name = "Track", description = "FR-03 — Track CRUD (trong round sơ loại)")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
@CoordinatorOnly
public class TrackController {

    private final TrackService trackService;

    // ------- nested ROUTES -------

    @PostMapping("/api/v1/rounds/{roundId}/tracks")
    @Operation(summary = "Tạo track mới cho round", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<TrackResponse>> createByRound(
            @PathVariable Integer roundId,
            @Valid @RequestBody CreateTrackRequest req
    ) {
        TrackResponse data = trackService.createByRound(roundId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/tracks/{id}")
                .buildAndExpand(data.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.created(data));
    }

    @GetMapping("/api/v1/rounds/{roundId}/tracks")
    @Operation(summary = "Liệt kê các track của round", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<List<TrackSummaryResponse>>> listByRound(
            @PathVariable Integer roundId,
            @RequestParam(required = false) TrackStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(trackService.listByRound(roundId, status)));
    }

    @GetMapping("/api/v1/hackathons/{hackathonId}/tracks")
    @Operation(summary = "Liệt kê các track của hackathon", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<List<TrackSummaryResponse>>> listByHackathon(
            @PathVariable Integer hackathonId,
            @RequestParam(required = false) TrackStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(trackService.listByHackathon(hackathonId, status)));
    }

    // ------- single resource ROUTES -------

    @GetMapping("/api/v1/tracks/{id}")
    @Operation(summary = "Xem chi tiết track", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<TrackResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(trackService.getById(id)));
    }

    @PutMapping("/api/v1/tracks/{id}")
    @Operation(summary = "Cập nhật track", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<TrackResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateTrackRequest req
    ) {
        TrackService.UpdateResult result = trackService.update(id, req);
        return ResponseEntity.ok(ApiResponse.okWithWarnings(result.track(), result.warnings()));
    }

    @DeleteMapping("/api/v1/tracks/{id}")
    @Operation(summary = "Xóa track", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer id) {
        Integer deletedId = trackService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", deletedId), "Deleted"));
    }

    @PostMapping(value = "/api/v1/tracks/{id}/problem-statement", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file PDF đề bài cho bảng đấu (sơ loại)")
    public ResponseEntity<ApiResponse<TrackResponse>> uploadProblemStatement(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(trackService.uploadProblemStatement(id, file)));
    }

    @GetMapping("/api/v1/tracks/{id}/problem-statement")
    @Operation(summary = "Tải file PDF đề bài của bảng đấu")
    public ResponseEntity<Resource> downloadProblemStatement(@PathVariable Integer id) {
        Resource resource = trackService.downloadProblemStatement(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"de-bai-track.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
