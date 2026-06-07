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
    public ResponseEntity<ApiResponse<StudentRoundDeadlineResponse>> currentDeadline() {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.getCurrentDeadline()));
    }

    @GetMapping("/{roundId}/problem")
    @Operation(summary = "FR-U-17 — Đề bài (student view)")
    public ResponseEntity<ApiResponse<StudentProblemResponse>> getProblem(@PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.getRoundProblem(roundId)));
    }

    @GetMapping("/{roundId}/leaderboard")
    @Operation(summary = "FR-U-21 — Bảng xếp hạng vòng (đã publish)")
    public ResponseEntity<ApiResponse<List<StudentLeaderboardItemResponse>>> getLeaderboard(
            @PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.getRoundLeaderboard(roundId)));
    }
}
