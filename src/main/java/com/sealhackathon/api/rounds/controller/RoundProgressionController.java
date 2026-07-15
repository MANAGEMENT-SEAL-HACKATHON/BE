package com.sealhackathon.api.rounds.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.rounds.dto.request.AdvanceTeamsRequest;
import com.sealhackathon.api.rounds.dto.request.AssignFinalJudgesRequest;
import com.sealhackathon.api.rounds.dto.request.LockScoringRequest;
import com.sealhackathon.api.rounds.dto.request.ResolveTiebreakRequest;
import com.sealhackathon.api.rounds.dto.request.UnlockScoringRequest;
import com.sealhackathon.api.rounds.dto.response.AdvanceRosterItemResponse;
import com.sealhackathon.api.rounds.dto.response.AdvanceTeamsResponse;
import com.sealhackathon.api.rounds.dto.response.AssignFinalJudgesResult;
import com.sealhackathon.api.rounds.dto.response.CloseSubmissionEarlyResponse;
import com.sealhackathon.api.rounds.dto.response.FinalJudgeAssignmentResponse;
import com.sealhackathon.api.rounds.dto.response.LockScoringResult;
import com.sealhackathon.api.rounds.dto.response.RoundRankingItemResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoreboardResponse;
import com.sealhackathon.api.rounds.dto.response.RoundScoringProgressResponse;
import com.sealhackathon.api.rounds.dto.response.RoundSummaryResponse;
import com.sealhackathon.api.rounds.dto.response.ScoreBreakdownResponse;
import com.sealhackathon.api.rounds.dto.response.TiebreakItemResponse;
import com.sealhackathon.api.rounds.dto.response.WildcardCandidatesResponse;
import com.sealhackathon.api.common.response.PageResponse;
import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.common.response.WarningCode;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.service.RoundProgressionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Round Progression (GĐ3-GĐ5)", description = "FR-15A..30A — Sơ loại, thăng vòng, Chung kết")
@RestController
@RequestMapping("/api/v1/rounds")
@RequiredArgsConstructor
public class RoundProgressionController {

    private final RoundProgressionService progressionService;
    private final RoundRankingQueryService roundRankingQueryService;

    @PatchMapping(value = "/{id}/release-problem", consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-15A — Phát đề (Sau khi kích hoạt vòng; Sơ loại: xác nhận sau khi upload từng track)")
    public ResponseEntity<ApiResponse<RoundSummaryResponse>> releaseProblem(
            @PathVariable Integer id,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.releaseProblem(id, file)));
    }

    @PostMapping("/{id}/close-submission-early")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Kết thúc thời gian thi sớm — đóng hạn nộp + examAt → JUDGING (GĐ3/GĐ5)")
    public ResponseEntity<ApiResponse<CloseSubmissionEarlyResponse>> closeSubmissionEarly(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.closeSubmissionEarly(id)));
    }

    @PatchMapping("/{id}/lock-scoring")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-20A — Khóa chấm điểm round")
    public ResponseEntity<ApiResponse<RoundSummaryResponse>> lockScoring(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) LockScoringRequest req) {
        LockScoringRequest body = req == null ? LockScoringRequest.builder().build() : req;
        LockScoringResult result = progressionService.lockScoring(id, body);
        if (result.getWarnings() == null || result.getWarnings().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(result.getRound()));
        }
        return ResponseEntity.ok(ApiResponse.okWithWarnings(result.getRound(), result.getWarnings()));
    }

    @PatchMapping("/{id}/unlock-scoring")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Mở khóa chấm điểm — broadcast SCORING_UNLOCKED")
    public ResponseEntity<ApiResponse<RoundSummaryResponse>> unlockScoring(
            @PathVariable Integer id,
            @Valid @RequestBody UnlockScoringRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.unlockScoring(id, req)));
    }

    @PatchMapping("/{id}/publish")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-24 — Công bố kết quả Sơ loại")
    public ResponseEntity<ApiResponse<RoundSummaryResponse>> publish(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.publish(id)));
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
    @Operation(summary = "FR-20/22 — Xếp hạng chính thức")
    public ResponseEntity<ApiResponse<List<RoundRankingItemResponse>>> ranking(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.ranking(id)));
    }

    @GetMapping("/{id}/ranking/preview")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-20 — Preview xếp hạng")
    public ResponseEntity<ApiResponse<List<RoundRankingItemResponse>>> rankingPreview(@PathVariable Integer id) {
        List<RoundRankingItemResponse> ranking = progressionService.rankingPreview(id);
        if (roundRankingQueryService.hasIncompleteScoring(id, true)) {
            return ResponseEntity.ok(ApiResponse.okWithWarnings(ranking, List.of(
                    Warning.of(WarningCode.INCOMPLETE_SCORING_IN_RANKING,
                            "Một số tiêu chí chưa được chấm — điểm preview có thể thiếu (COALESCE=0)"))));
        }
        return ResponseEntity.ok(ApiResponse.ok(ranking));
    }

    @GetMapping("/{id}/tiebreak")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-22B — Danh sách tiebreak cần xử lý")
    public ResponseEntity<ApiResponse<List<TiebreakItemResponse>>> tiebreak(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.tiebreak(id)));
    }

    @PostMapping("/{id}/tiebreak/resolve")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-22B — Resolve tiebreak")
    public ResponseEntity<ApiResponse<List<RoundRankingItemResponse>>> resolveTiebreak(
            @PathVariable Integer id,
            @Valid @RequestBody ResolveTiebreakRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.resolveTiebreak(id, req)));
    }

    @GetMapping("/{id}/wildcard-candidates")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-22A — Danh sách ứng viên wildcard")
    public ResponseEntity<ApiResponse<WildcardCandidatesResponse>> wildcardCandidates(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.wildcardCandidates(id)));
    }

    @PostMapping("/{id}/advance")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-22/23 — Chốt danh sách ADVANCE/ELIMINATE")
    public ResponseEntity<ApiResponse<AdvanceTeamsResponse>> advance(
            @PathVariable Integer id,
            @Valid @RequestBody AdvanceTeamsRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.advance(id, req)));
    }

    @PostMapping("/{id}/judge-assignments")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "FR-27 — Phân Judge cho vòng Chung kết")
    public ResponseEntity<ApiResponse<FinalJudgeAssignmentResponse>> assignFinalJudges(
            @PathVariable Integer id,
            @Valid @RequestBody AssignFinalJudgesRequest req) {
        AssignFinalJudgesResult result = progressionService.assignFinalJudges(id, req);
        if (result.getWarnings() == null || result.getWarnings().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(result.getAssignment()));
        }
        return ResponseEntity.ok(ApiResponse.okWithWarnings(result.getAssignment(), result.getWarnings()));
    }

    @GetMapping("/{id}/scoreboard")
    @Operation(summary = "FR-20 — Bảng điểm công khai theo round")
    public ResponseEntity<ApiResponse<RoundScoreboardResponse>> scoreboard(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.scoreboard(id)));
    }

    @GetMapping("/{id}/advance-roster")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Danh sách CK & loại (preview sau publish / outcome sau advance)")
    public ResponseEntity<ApiResponse<PageResponse<AdvanceRosterItemResponse>>> advanceRoster(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.advanceRoster(id, page, size)));
    }

    @GetMapping("/{id}/score-breakdown")
    @CoordinatorOnly
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @Operation(summary = "Ma trận điểm judges × criteria theo submission (null = chưa chấm)")
    public ResponseEntity<ApiResponse<ScoreBreakdownResponse>> scoreBreakdown(
            @PathVariable Integer id,
            @RequestParam Integer submissionId) {
        return ResponseEntity.ok(ApiResponse.ok(progressionService.scoreBreakdown(id, submissionId)));
    }
}
