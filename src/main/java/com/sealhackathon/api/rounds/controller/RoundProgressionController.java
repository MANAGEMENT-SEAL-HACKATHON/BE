package com.sealhackathon.api.rounds.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.rounds.dto.request.AdvanceTeamsRequest;
import com.sealhackathon.api.rounds.dto.request.AssignFinalJudgesRequest;
import com.sealhackathon.api.rounds.dto.request.LockScoringRequest;
import com.sealhackathon.api.rounds.dto.request.ReleaseProblemRequest;
import com.sealhackathon.api.rounds.dto.request.ResolveTiebreakRequest;
import com.sealhackathon.api.rounds.dto.request.WildcardDecisionRequest;
import com.sealhackathon.api.rounds.dto.response.AdvanceTeamsResponse;
import com.sealhackathon.api.rounds.dto.response.FinalJudgeAssignmentResponse;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoreboardResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoringProgressResponse;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;
import com.sealhackathon.api.rounds.dto.response.TiebreakItemResponse;
import com.sealhackathon.api.rounds.dto.response.WildcardCandidateResponse;
import com.sealhackathon.api.rounds.service.RoundProgressionService;
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

@Tag(name = "Round Progression (GĐ3-GĐ5)", description = "FR-21/26/27/28/29/30/31/36")
@RestController
@RequestMapping("/api/v1/rounds")
@RequiredArgsConstructor
public class RoundProgressionController {

    private final RoundProgressionService progressionService;

    @PatchMapping("/{id}/release-problem")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-21 — Phát đề theo round")
    public ResponseEntity<ApiResponse<RoundSummaryResponse>> releaseProblem(
            @PathVariable Integer id,
            @Valid @RequestBody ReleaseProblemRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.releaseProblem(id, req)));
    }

    @PatchMapping("/{id}/lock-scoring")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-26/36 — Khóa chấm điểm round")
    public ResponseEntity<ApiResponse<RoundSummaryResponse>> lockScoring(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) LockScoringRequest req) {
        LockScoringRequest body = req == null ? LockScoringRequest.builder().build() : req;
        return ResponseEntity.ok(ApiResponse.ok(progressionService.lockScoring(id, body)));
    }

    @GetMapping("/{id}/scoring-progress")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Tiến độ chấm điểm round")
    public ResponseEntity<ApiResponse<RoundScoringProgressResponse>> scoringProgress(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.scoringProgress(id)));
    }

    @GetMapping("/{id}/ranking")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-27 — Xếp hạng chính thức")
    public ResponseEntity<ApiResponse<List<RoundRankingItemResponse>>> ranking(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.ranking(id)));
    }

    @GetMapping("/{id}/ranking/preview")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-27 — Preview xếp hạng")
    public ResponseEntity<ApiResponse<List<RoundRankingItemResponse>>> rankingPreview(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.rankingPreview(id)));
    }

    @GetMapping("/{id}/tiebreak")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-28 — Danh sách tiebreak cần xử lý")
    public ResponseEntity<ApiResponse<List<TiebreakItemResponse>>> tiebreak(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.tiebreak(id)));
    }

    @PostMapping("/{id}/tiebreak/resolve")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-28 — Resolve tiebreak")
    public ResponseEntity<ApiResponse<List<RoundRankingItemResponse>>> resolveTiebreak(
            @PathVariable Integer id,
            @Valid @RequestBody ResolveTiebreakRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.resolveTiebreak(id, req)));
    }

    @GetMapping("/{id}/wildcard/candidates")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-29 — Danh sách ứng viên wildcard")
    public ResponseEntity<ApiResponse<List<WildcardCandidateResponse>>> wildcardCandidates(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.wildcardCandidates(id)));
    }

    @PostMapping("/{id}/wildcard/approve")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-29 — Duyệt wildcard")
    public ResponseEntity<ApiResponse<List<WildcardCandidateResponse>>> wildcardApprove(
            @PathVariable Integer id,
            @Valid @RequestBody WildcardDecisionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.wildcardApprove(id, req)));
    }

    @PostMapping("/{id}/wildcard/reject")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-29 — Từ chối wildcard")
    public ResponseEntity<ApiResponse<List<WildcardCandidateResponse>>> wildcardReject(
            @PathVariable Integer id,
            @Valid @RequestBody WildcardDecisionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.wildcardReject(id, req)));
    }

    @PostMapping("/{id}/advance-teams")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-30 — Chốt danh sách ADVANCE/ELIMINATE")
    public ResponseEntity<ApiResponse<AdvanceTeamsResponse>> advanceTeams(
            @PathVariable Integer id,
            @Valid @RequestBody AdvanceTeamsRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.advanceTeams(id, req)));
    }

    @PostMapping("/{id}/judge-assignments")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-31 — Phân Judge cho vòng Chung kết")
    public ResponseEntity<ApiResponse<FinalJudgeAssignmentResponse>> assignFinalJudges(
            @PathVariable Integer id,
            @Valid @RequestBody AssignFinalJudgesRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.assignFinalJudges(id, req)));
    }

    @GetMapping("/{id}/scoreboard")
    @Operation(summary = "Bảng điểm công khai theo round")
    public ResponseEntity<ApiResponse<RoundScoreboardResponse>> scoreboard(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.scoreboard(id)));
    }
}
