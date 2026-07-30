package com.sealhackathon.api.me.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.JudgeOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.me.dto.request.AssignmentDeclineRequest;
import com.sealhackathon.api.me.dto.response.AssignmentResponseStatusResponse;
import com.sealhackathon.api.me.judge.dto.request.JudgeScoreCommentRequest;
import com.sealhackathon.api.me.judge.dto.request.JudgeScoringCompletionRequest;
import com.sealhackathon.api.me.judge.dto.request.TiebreakVoteRequest;
import com.sealhackathon.api.me.judge.dto.response.*;
import com.sealhackathon.api.me.judge.service.JudgePortalService;
import com.sealhackathon.api.me.service.AssignmentResponseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Judge Portal", description = "FR-J — Portal giám khảo /api/v1/me/*")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@JudgeOnly
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class JudgeMeController {

    private final JudgePortalService judgePortalService;
    private final AssignmentResponseService assignmentResponseService;

    @GetMapping("/judge-track-assignments")
    @Operation(summary = "FR-J-05 — Phân công track")
    public ResponseEntity<ApiResponse<List<JudgeTrackAssignmentResponse>>> trackAssignments() {
        return ResponseEntity.ok(ApiResponse.ok(judgePortalService.listTrackAssignments()));
    }

    @PatchMapping("/judge/assignments/{id}/decline")
    @Operation(summary = "Pha 6 — Giám khảo từ chối phân công")
    public ResponseEntity<ApiResponse<AssignmentResponseStatusResponse>> declineAssignment(
            @PathVariable Integer id,
            @Valid @RequestBody AssignmentDeclineRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                assignmentResponseService.declineJudgeAssignment(id, request), "Đã từ chối phân công"));
    }

    @PatchMapping("/judge/assignments/{id}/accept")
    @Operation(summary = "Pha 6 — Giám khảo chấp nhận lại phân công")
    public ResponseEntity<ApiResponse<AssignmentResponseStatusResponse>> acceptAssignment(
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(
                assignmentResponseService.acceptJudgeAssignment(id), "Đã chấp nhận phân công"));
    }

    @GetMapping("/judge-final-assignments")
    @Operation(summary = "FR-J-06 — Phân công chung kết")
    public ResponseEntity<ApiResponse<List<JudgeFinalAssignmentResponse>>> finalAssignments() {
        return ResponseEntity.ok(ApiResponse.ok(judgePortalService.listFinalAssignments()));
    }

    @GetMapping("/scoring-schedule")
    @Operation(summary = "FR-J-12 — Lịch chấm")
    public ResponseEntity<ApiResponse<List<JudgeScoringScheduleItemResponse>>> scoringSchedule(
            @RequestParam(required = false) Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(judgePortalService.getScoringSchedule(roundId)));
    }

    @PatchMapping("/scoring-completion")
    @Operation(summary = "FR-J-16/20/21 — Cập nhật hoàn thành chấm")
    public ResponseEntity<ApiResponse<Void>> scoringCompletion(
            @Valid @RequestBody JudgeScoringCompletionRequest request) {
        judgePortalService.updateScoringCompletion(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Updated"));
    }

    @GetMapping("/scores")
    @Operation(summary = "FR-J-24 — Điểm đã chấm (judge view)")
    public ResponseEntity<ApiResponse<List<JudgeScoreSummaryResponse>>> myScores(
            @RequestParam(required = false) Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(judgePortalService.listMyScores(roundId)));
    }

    @GetMapping("/judge/submissions")
    @Operation(summary = "GĐ3 — Danh sách bài nộp ẩn danh theo mã submissionId")
    public ResponseEntity<ApiResponse<List<JudgeSubmissionListItemResponse>>> judgeSubmissions(
            @RequestParam Integer roundId,
            @RequestParam(required = false) Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(judgePortalService.listSubmissions(roundId, trackId)));
    }

    @PostMapping("/judge/submissions/{submissionId}/confirm-scoring")
    @Operation(summary = "GĐ3 — Judge xác nhận đã chấm xong bài đang PRESENTING")
    public ResponseEntity<ApiResponse<Void>> confirmSubmissionScoring(@PathVariable Integer submissionId) {
        judgePortalService.confirmSubmissionScoring(submissionId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã xác nhận chấm xong"));
    }

    @GetMapping("/judge/presentation-scoring-status")
    @Operation(summary = "GĐ3 — Tiến độ xác nhận chấm của track (multi-judge)")
    public ResponseEntity<ApiResponse<JudgePresentationScoringStatusResponse>> presentationScoringStatus(
            @RequestParam Integer roundId,
            @RequestParam(required = false) Integer trackId) {
        return ResponseEntity.ok(ApiResponse.ok(
                judgePortalService.getPresentationScoringStatus(roundId, trackId)));
    }

    @PatchMapping("/scores/{id}/comment")
    @Operation(summary = "FR-J-15 — Sửa comment điểm")
    public ResponseEntity<ApiResponse<JudgeScoreSummaryResponse>> updateComment(
            @PathVariable Integer id,
            @Valid @RequestBody JudgeScoreCommentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(judgePortalService.updateScoreComment(id, request)));
    }

    @PostMapping("/tiebreak-evaluations")
    @Operation(summary = "FR-J-22/23 — HEAD vote tiebreak")
    public ResponseEntity<ApiResponse<TiebreakVoteResponse>> tiebreakVote(
            @Valid @RequestBody TiebreakVoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(judgePortalService.submitTiebreakVote(request)));
    }

    @GetMapping("/judge-history")
    @Operation(summary = "FR-J-26 — Lịch sử chấm")
    public ResponseEntity<ApiResponse<JudgeHistoryResponse>> history(
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(judgePortalService.getHistory(year)));
    }
}
