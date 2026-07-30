package com.sealhackathon.api.showcase.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.showcase.dto.response.HallOfFameEntryResponse;
import com.sealhackathon.api.showcase.dto.response.ShowcaseArticleResponse;
import com.sealhackathon.api.showcase.dto.response.ShowcaseArticleSummaryResponse;
import com.sealhackathon.api.showcase.service.HallOfFameService;
import com.sealhackathon.api.showcase.service.ShowcaseArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Showcase (Public)", description = "Phase 8 — bảng vàng & bài viết công khai")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.showcase.enabled", havingValue = "true", matchIfMissing = true)
public class PublicShowcaseController {

    private final HallOfFameService hallOfFameService;
    private final ShowcaseArticleService showcaseArticleService;

    @GetMapping("/hall-of-fame")
    @Operation(summary = "Danh sách bảng vàng (lọc năm tùy chọn)")
    public ResponseEntity<ApiResponse<List<HallOfFameEntryResponse>>> hallOfFame(
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(hallOfFameService.listPublic(year)));
    }

    @GetMapping("/articles")
    @Operation(summary = "Danh sách bài viết đã xuất bản")
    public ResponseEntity<ApiResponse<List<ShowcaseArticleSummaryResponse>>> listArticles() {
        return ResponseEntity.ok(ApiResponse.ok(showcaseArticleService.listPublished()));
    }

    @GetMapping("/articles/{slug}")
    @Operation(summary = "Chi tiết bài viết theo slug (chỉ PUBLISHED)")
    public ResponseEntity<ApiResponse<ShowcaseArticleResponse>> getArticle(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(showcaseArticleService.getPublishedBySlug(slug)));
    }

    @GetMapping("/articles/{slug}/cover")
    @Operation(summary = "Ảnh bìa bài viết công khai")
    public ResponseEntity<Resource> getCover(@PathVariable String slug) {
        Resource resource = showcaseArticleService.loadCoverBySlug(slug);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"cover\"")
                .body(resource);
    }

    @GetMapping("/articles/{slug}/blocks/{blockId}/image")
    @Operation(summary = "Ảnh khối IMAGE trong bài viết công khai")
    public ResponseEntity<Resource> getBlockImage(
            @PathVariable String slug,
            @PathVariable Integer blockId) {
        Resource resource = showcaseArticleService.loadBlockImageBySlug(slug, blockId);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"block\"")
                .body(resource);
    }
}
