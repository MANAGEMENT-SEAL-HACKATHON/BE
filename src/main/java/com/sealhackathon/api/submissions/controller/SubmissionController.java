package com.sealhackathon.api.submissions.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.common.security.StudentOnly;
import com.sealhackathon.api.common.security.SubmissionListAccess;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.submissions.dto.request.RejectLateSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.ResubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.ReviewLateSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.ReviewSubmissionRequest;
import com.sealhackathon.api.submissions.dto.request.SubmitSubmissionRequest;
import com.sealhackathon.api.submissions.dto.response.SubmissionResponse;
import com.sealhackathon.api.submissions.service.SubmissionService;
import com.sealhackathon.api.submissions.value_object.LateReviewDecision;
import com.sealhackathon.api.submissions.value_object.SubmissionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Submissions (GĐ3-GĐ5)", description = "FR-16/16A/26 — Nộp và duyệt bài")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @StudentOnly
    @Operation(summary = "FR-16/26 — Nộp bài multipart (repoUrl + slide PDF)")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submitMultipart(
            @RequestParam Integer teamId,
            @RequestParam(required = false) Integer trackId,
            @RequestParam(required = false) Integer roundId,
            @RequestParam String repoUrl,
            @RequestParam(required = false) String lateReason,
            @RequestPart(required = false) MultipartFile slideFile) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                submissionService.submitMultipart(teamId, trackId, roundId, repoUrl, lateReason, slideFile)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @StudentOnly
    @Operation(summary = "FR-16/26 — Nộp bài JSON (legacy)")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submit(@Valid @RequestBody SubmitSubmissionRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(submissionService.submit(req)));
    }

    @GetMapping("/{id}/slide")
    @SubmissionListAccess
    @Operation(summary = "GĐ3 — Xem hoặc tải slide PDF",
            description = "Mặc định `inline` (xem trên trình duyệt). `?download=true` → `attachment` (tải file).")
    public ResponseEntity<byte[]> getSlide(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean download) throws java.io.IOException {
        var slide = submissionService.getSlideDownload(id);
        byte[] bytes = slide.content().stream().readAllBytes();
        slide.content().stream().close();
        String disposition = download ? "attachment" : "inline";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"" + slide.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    @GetMapping
    @SubmissionListAccess
    @Operation(summary = "Danh sách bài nộp theo team/round")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> list(
            @RequestParam(required = false) Integer teamId,
            @RequestParam(required = false) Integer roundId,
            @RequestParam(required = false) SubmissionStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.list(teamId, roundId, status)));
    }

    @Deprecated
    @PatchMapping("/{id}/resubmit")
    @StudentOnly
    @Operation(summary = "Deprecated — dùng POST /submissions upsert (FR-16)")
    public ResponseEntity<ApiResponse<SubmissionResponse>> resubmit(
            @PathVariable Integer id,
            @Valid @RequestBody ResubmitSubmissionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.resubmit(id, req)));
    }

    @PatchMapping("/{id}/review-late")
    @CoordinatorOnly
    @Operation(summary = "FR-16A — Coordinator duyệt LATE_PENDING")
    public ResponseEntity<ApiResponse<SubmissionResponse>> reviewLate(
            @PathVariable Integer id,
            @Valid @RequestBody ReviewLateSubmissionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.reviewLate(id, req)));
    }

    @PatchMapping("/{id}/approve")
    @CoordinatorOnly
    @Operation(summary = "GĐ3 — Alias duyệt bài nộp trễ")
    public ResponseEntity<ApiResponse<SubmissionResponse>> approveLate(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.reviewLate(id,
                ReviewLateSubmissionRequest.builder().decision(LateReviewDecision.APPROVE).build())));
    }

    @PatchMapping("/{id}/reject")
    @CoordinatorOnly
    @Operation(summary = "GĐ3 — Alias từ chối bài nộp trễ")
    public ResponseEntity<ApiResponse<SubmissionResponse>> rejectLate(
            @PathVariable Integer id,
            @Valid @RequestBody RejectLateSubmissionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.reviewLate(id,
                ReviewLateSubmissionRequest.builder()
                        .decision(LateReviewDecision.REJECT)
                        .note(req.getReason())
                        .build())));
    }

    @Deprecated
    @PatchMapping("/{id}/review")
    @CoordinatorOnly
    @Operation(summary = "Deprecated — dùng PATCH /{id}/review-late")
    public ResponseEntity<ApiResponse<SubmissionResponse>> review(
            @PathVariable Integer id,
            @Valid @RequestBody ReviewSubmissionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(submissionService.review(id, req)));
    }
}
