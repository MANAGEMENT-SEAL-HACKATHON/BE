package com.sealhackathon.api.judge_assignments.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * FR-05c — Judge Assignment controller.
 */
@Tag(name = "Personnel — Judge", description = "FR-05c — Phân công judge vào track")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CoordinatorOnly
public class JudgeAssignmentController {

    private final JudgeAssignmentService judgeAssignmentService;

    @PostMapping("/judge-assignments")
    @Operation(summary = "Phân công judge vào track", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<JudgeAssignmentResponse>> assign(
            @Valid @RequestBody CreateJudgeAssignmentRequest req
    ) {
        JudgeAssignmentService.CreateResult result = judgeAssignmentService.assign(req);
        return ResponseEntity.status(201).body(
                ApiResponse.createdWithWarnings(result.assignment(), result.warnings())
        );
    }

    @GetMapping("/tracks/{trackId}/judges")
    @Operation(summary = "Liệt kê judge của track", description = "Không dùng ở GĐ1 — xem runbook Bước 5.")
    public ResponseEntity<ApiResponse<List<JudgeAssignmentResponse>>> listByTrack(
            @PathVariable Integer trackId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(judgeAssignmentService.listByTrack(trackId)));
    }

    /** List Judge Chung kết (round_id). Không dùng ở GĐ1 — xem runbook Bước 5. */
    @GetMapping("/rounds/{roundId}/judges")
    @Operation(summary = "Liệt kê judge của vòng chung kết", description = "Không dùng ở GĐ1 — xem runbook Bước 5.")
    public ResponseEntity<ApiResponse<List<JudgeAssignmentResponse>>> listByRound(
            @PathVariable Integer roundId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(judgeAssignmentService.listByRound(roundId)));
    }

    @GetMapping("/users/{judgeId}/round-assignments")
    @Operation(summary = "Liệt kê track/vòng mà judge được phân công", description = "Không dùng ở GĐ1 — xem runbook Bước 5.")
    public ResponseEntity<ApiResponse<List<JudgeAssignmentResponse>>> listByJudge(@PathVariable Integer judgeId) {
        return ResponseEntity.ok(ApiResponse.ok(judgeAssignmentService.listByJudge(judgeId)));
    }

    @DeleteMapping("/judge-assignments/{id}")
    @Operation(summary = "Hủy phân công judge khỏi track", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unassign(@PathVariable Integer id) {
        Integer deletedId = judgeAssignmentService.unassign(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", deletedId), "Unassigned"));
    }
}
