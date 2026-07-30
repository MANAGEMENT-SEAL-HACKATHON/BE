package com.sealhackathon.api.showcase.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.showcase.dto.request.CreateShowcaseArticleRequest;
import com.sealhackathon.api.showcase.dto.request.UpdateShowcaseArticleRequest;
import com.sealhackathon.api.showcase.dto.response.HallOfFameEntryResponse;
import com.sealhackathon.api.showcase.dto.response.ShowcaseArticleResponse;
import com.sealhackathon.api.showcase.service.HallOfFameService;
import com.sealhackathon.api.showcase.service.ShowcaseArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Tag(name = "Showcase (Coordinator)", description = "Phase 8 — bảng vàng & bài viết vinh danh")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CoordinatorOnly
@ConditionalOnProperty(name = "app.showcase.enabled", havingValue = "true", matchIfMissing = true)
public class ShowcaseCoordinatorController {

    private final ShowcaseArticleService showcaseArticleService;
    private final HallOfFameService hallOfFameService;

    @GetMapping("/hackathons/{hackathonId}/hall-of-fame")
    @Operation(summary = "Bảng vàng theo hackathon")
    public ResponseEntity<ApiResponse<List<HallOfFameEntryResponse>>> listHof(
            @PathVariable Integer hackathonId) {
        return ResponseEntity.ok(ApiResponse.ok(hallOfFameService.listByHackathon(hackathonId)));
    }

    @PostMapping("/showcase/hall-of-fame/backfill")
    @Operation(summary = "Backfill bảng vàng từ hackathon FINISHED (one-shot)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> backfill() {
        int created = hallOfFameService.backfillFinishedHackathons();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("created", created)));
    }

    @GetMapping("/hackathons/{hackathonId}/showcase/articles")
    @Operation(summary = "Danh sách bài viết (mọi trạng thái) theo hackathon")
    public ResponseEntity<ApiResponse<List<ShowcaseArticleResponse>>> listArticles(
            @PathVariable Integer hackathonId) {
        return ResponseEntity.ok(ApiResponse.ok(showcaseArticleService.listByHackathon(hackathonId)));
    }

    @PostMapping("/hackathons/{hackathonId}/showcase/articles")
    @Operation(summary = "Tạo bài viết nháp")
    public ResponseEntity<ApiResponse<ShowcaseArticleResponse>> create(
            @PathVariable Integer hackathonId,
            @Valid @RequestBody CreateShowcaseArticleRequest req) {
        ShowcaseArticleResponse created = showcaseArticleService.create(hackathonId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/showcase/articles/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.created(created));
    }

    @PostMapping("/hackathons/{hackathonId}/showcase/articles/generate-draft")
    @Operation(summary = "Sinh nháp từ dữ liệu quán quân (Hall of Fame)")
    public ResponseEntity<ApiResponse<ShowcaseArticleResponse>> generateDraft(
            @PathVariable Integer hackathonId) {
        ShowcaseArticleResponse created = showcaseArticleService.generateDraftFromChampions(hackathonId);
        return ResponseEntity.status(201).body(ApiResponse.created(created));
    }

    @GetMapping("/showcase/articles/{id}")
    @Operation(summary = "Chi tiết bài viết (coordinator)")
    public ResponseEntity<ApiResponse<ShowcaseArticleResponse>> get(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(showcaseArticleService.getById(id)));
    }

    @PutMapping("/showcase/articles/{id}")
    @Operation(summary = "Cập nhật bài viết + khối nội dung")
    public ResponseEntity<ApiResponse<ShowcaseArticleResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateShowcaseArticleRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(showcaseArticleService.update(id, req)));
    }

    @DeleteMapping("/showcase/articles/{id}")
    @Operation(summary = "Xóa bài viết")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer id) {
        showcaseArticleService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", id), "Deleted"));
    }

    @PostMapping("/showcase/articles/{id}/publish")
    @Operation(summary = "Xuất bản bài viết")
    public ResponseEntity<ApiResponse<ShowcaseArticleResponse>> publish(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(showcaseArticleService.publish(id)));
    }

    @PostMapping("/showcase/articles/{id}/unpublish")
    @Operation(summary = "Gỡ xuất bản (về DRAFT)")
    public ResponseEntity<ApiResponse<ShowcaseArticleResponse>> unpublish(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(showcaseArticleService.unpublish(id)));
    }

    @PostMapping(value = "/showcase/articles/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload ảnh bìa")
    public ResponseEntity<ApiResponse<ShowcaseArticleResponse>> uploadCover(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(showcaseArticleService.uploadCover(id, file)));
    }

    @PostMapping(value = "/showcase/articles/{id}/block-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload ảnh cho khối IMAGE")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadBlockImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(showcaseArticleService.uploadBlockImage(id, file)));
    }

    @GetMapping("/showcase/articles/{id}/cover")
    @Operation(summary = "Tải ảnh bìa (coordinator preview)")
    public ResponseEntity<Resource> getCover(@PathVariable Integer id) {
        ShowcaseArticleResponse article = showcaseArticleService.getById(id);
        Resource resource = showcaseArticleService.loadCoverBySlug(article.getSlug());
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cover\"")
                .body(resource);
    }
}
