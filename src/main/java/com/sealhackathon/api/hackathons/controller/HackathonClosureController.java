package com.sealhackathon.api.hackathons.controller;

import com.sealhackathon.api.chapters.dto.response.ChapterRankingItemResponse;
import com.sealhackathon.api.chapters.service.ChapterRankingService;
import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.export_jobs.dto.request.CreateExportJobRequest;
import com.sealhackathon.api.export_jobs.dto.response.ExportJobResponse;
import com.sealhackathon.api.export_jobs.service.ExportJobService;
import com.sealhackathon.api.hackathons.dto.request.ConfirmHackathonRequest;
import com.sealhackathon.api.hackathons.dto.response.FinalTeamRankingItemResponse;
import com.sealhackathon.api.hackathons.dto.response.HackathonResponse;
import com.sealhackathon.api.hackathons.service.HackathonClosureService;
import com.sealhackathon.api.individual_rankings.dto.response.IndividualRankingItemResponse;
import com.sealhackathon.api.individual_rankings.service.IndividualRankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Hackathon Closure (GĐ6)", description = "MF-06 FR-31/33/33A — XH CK, confirm FINISHED")
@RestController
@RequestMapping("/api/v1/hackathons")
@RequiredArgsConstructor
@CoordinatorOnly
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class HackathonClosureController {

    private final HackathonClosureService closureService;
    private final ChapterRankingService chapterRankingService;
    private final IndividualRankingService individualRankingService;
    private final ExportJobService exportJobService;

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "FR-33 — Xác nhận PENDING_CONFIRM → FINISHED")
    public ResponseEntity<ApiResponse<HackathonResponse>> confirm(
            @PathVariable Integer id,
            @Valid @RequestBody ConfirmHackathonRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(closureService.confirm(id, req)));
    }

    @GetMapping("/{id}/team-rankings")
    @Operation(summary = "FR-31/33A — Bảng XH Team (round FINAL, không persist)")
    public ResponseEntity<ApiResponse<List<FinalTeamRankingItemResponse>>> teamRankings(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(closureService.teamRankings(id)));
    }

    @GetMapping("/{id}/chapter-rankings")
    @Operation(summary = "FR-33B — Bảng XH Chapter (cumulative)")
    public ResponseEntity<ApiResponse<List<ChapterRankingItemResponse>>> chapterRankings(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(chapterRankingService.listByHackathon(id)));
    }

    @GetMapping("/{id}/individual-rankings")
    @Operation(summary = "FR-33C — Bảng XH Cá nhân (nếu individual_ranking_enabled)")
    public ResponseEntity<ApiResponse<List<IndividualRankingItemResponse>>> individualRankings(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(individualRankingService.listByHackathon(id)));
    }

    @GetMapping("/{id}/export-jobs")
    @Operation(summary = "FR-34 — Danh sách job xuất báo cáo (mới nhất trước)")
    public ResponseEntity<ApiResponse<List<ExportJobResponse>>> listExportJobs(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(exportJobService.listByHackathon(id)));
    }

    @PostMapping("/{id}/export-jobs")
    @Operation(summary = "FR-34 — Tạo job xuất báo cáo / RBL")
    public ResponseEntity<ApiResponse<ExportJobResponse>> createExportJob(
            @PathVariable Integer id,
            @Valid @RequestBody CreateExportJobRequest req) {
        return ResponseEntity.status(202).body(ApiResponse.created(exportJobService.create(id, req)));
    }
}
