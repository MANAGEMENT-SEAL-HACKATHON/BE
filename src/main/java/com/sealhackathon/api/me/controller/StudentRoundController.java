package com.sealhackathon.api.me.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.StudentOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.me.student.dto.response.StudentLeaderboardItemResponse;
import com.sealhackathon.api.me.student.dto.response.StudentProblemResponse;
import com.sealhackathon.api.me.student.dto.response.StudentRoundDeadlineResponse;
import com.sealhackathon.api.me.student.service.StudentPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Student Portal", description = "FR-U — Round problem & leaderboard")
@RestController
@RequestMapping("/api/v1/me/rounds")
@RequiredArgsConstructor
@StudentOnly
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class StudentRoundController {

    private final StudentPortalService studentPortalService;

    @GetMapping("/current/deadline")
    @Operation(summary = "GĐ3 — Deadline nộp bài vòng đang active")
    public ResponseEntity<ApiResponse<StudentRoundDeadlineResponse>> currentDeadline(
            @RequestParam(required = false) Integer hackathonId) {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.getCurrentDeadline(hackathonId)));
    }

    @GetMapping("/{roundId}/problem")
    @Operation(summary = "FR-U-17 — Đề bài (student view)")
    public ResponseEntity<ApiResponse<StudentProblemResponse>> getProblem(@PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.getRoundProblem(roundId)));
    }

    @GetMapping("/{roundId}/problem-statement")
    @Operation(summary = "Tải PDF đề bài (theo bảng đấu nếu Sơ loại)")
    public ResponseEntity<Resource> downloadProblemStatement(@PathVariable Integer roundId) {
        Resource resource = studentPortalService.downloadRoundProblemStatement(roundId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"de-bai.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/{roundId}/leaderboard")
    @Operation(summary = "FR-U-21 — Bảng xếp hạng vòng (đã publish)")
    public ResponseEntity<ApiResponse<List<StudentLeaderboardItemResponse>>> getLeaderboard(
            @PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.getRoundLeaderboard(roundId)));
    }
}
