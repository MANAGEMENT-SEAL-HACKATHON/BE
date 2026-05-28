package com.sealhackathon.api.submissions.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.common.security.StudentOnly;
import com.sealhackathon.api.common.security.SubmissionListAccess;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.submissions.dto.request.ResubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.ReviewSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.SubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.response.SubmissionResponse;
import com.sealhackathon.api.submissions.service.SubmissionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Submissions (GĐ3-GĐ5)", description = "FR-22/25/33 — Nộp và duyệt bài")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    @StudentOnly
    @Operation(summary = "FR-22/33 — Nộp bài (Sơ loại/Chung kết)")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submit(@Valid @RequestBody SubmitSubmissionRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(submissionService.submit(req)));
    }

    @GetMapping
    @SubmissionListAccess
    @Operation(summary = "Danh sách bài nộp theo team/round")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> list(
            @RequestParam(required = false) Integer teamId,
            @RequestParam(required = false) Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.list(teamId, roundId)));
    }

    @PatchMapping("/{id}/resubmit")
    @StudentOnly
    @Operation(summary = "FR-22 — Nộp lại bài")
    public ResponseEntity<ApiResponse<SubmissionResponse>> resubmit(
            @PathVariable Integer id,
            @Valid @RequestBody ResubmitSubmissionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.resubmit(id, req)));
    }

    @PatchMapping("/{id}/review")
    @CoordinatorOnly
    @Operation(summary = "FR-25 — Coordinator duyệt LATE_PENDING")
    public ResponseEntity<ApiResponse<SubmissionResponse>> review(
            @PathVariable Integer id,
            @Valid @RequestBody ReviewSubmissionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.review(id, req)));
    }
}
