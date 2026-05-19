package com.sealhackathon.api.judge_assignments.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.judge_assignments.dto.request.CreateJudgeAssignmentRequest;
import com.sealhackathon.api.judge_assignments.dto.response.JudgeAssignmentResponse;
import com.sealhackathon.api.judge_assignments.service.JudgeAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * FR-05c — Judge Assignment controller.
 */
@RestController
@RequiredArgsConstructor
@CoordinatorOnly
public class JudgeAssignmentController {

    private final JudgeAssignmentService judgeAssignmentService;

    @PostMapping("/api/v1/judge-assignments")
    public ResponseEntity<ApiResponse<JudgeAssignmentResponse>> assign(
            @Valid @RequestBody CreateJudgeAssignmentRequest req
    ) {
        JudgeAssignmentService.CreateResult result = judgeAssignmentService.assign(req);
        return ResponseEntity.status(201).body(
                ApiResponse.createdWithWarnings(result.assignment(), result.warnings())
        );
    }

    @GetMapping("/api/v1/tracks/{trackId}/judges")
    public ResponseEntity<ApiResponse<List<JudgeAssignmentResponse>>> listByTrack(
            @PathVariable Integer trackId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(judgeAssignmentService.listByTrack(trackId)));
    }

    /** List Judge Chung kết (round_id). Không dùng ở GĐ1 — xem runbook Bước 5. */
    @GetMapping("/api/v1/rounds/{roundId}/judges")
    public ResponseEntity<ApiResponse<List<JudgeAssignmentResponse>>> listByRound(
            @PathVariable Integer roundId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(judgeAssignmentService.listByRound(roundId)));
    }

    @GetMapping("/api/v1/users/{judgeId}/round-assignments")
    public ResponseEntity<ApiResponse<List<JudgeAssignmentResponse>>> listByJudge(@PathVariable Integer judgeId) {
        return ResponseEntity.ok(ApiResponse.ok(judgeAssignmentService.listByJudge(judgeId)));
    }

    @DeleteMapping("/api/v1/judge-assignments/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unassign(@PathVariable Integer id) {
        Integer deletedId = judgeAssignmentService.unassign(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", deletedId), "Unassigned"));
    }
}
