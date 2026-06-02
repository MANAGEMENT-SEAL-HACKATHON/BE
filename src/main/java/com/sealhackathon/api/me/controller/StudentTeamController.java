package com.sealhackathon.api.me.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.StudentOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.me.student.dto.request.RelotteryTrackRequest;
import com.sealhackathon.api.me.student.dto.response.StudentSubmissionStatusResponse;
import com.sealhackathon.api.me.student.service.StudentPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Student Portal", description = "FR-U — Team portal routes")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@StudentOnly
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class StudentTeamController {

    private final StudentPortalService studentPortalService;

    @GetMapping("/teams/{teamId}/submissions")
    @Operation(summary = "FR-U-20 — Trạng thái nộp bài theo đội")
    public ResponseEntity<ApiResponse<List<StudentSubmissionStatusResponse>>> listSubmissions(
            @PathVariable Integer teamId) {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.listTeamSubmissions(teamId)));
    }

    @PatchMapping("/teams/{teamId}/rounds/{roundId}/track")
    @Operation(summary = "FR-U-16 — Re-lottery track")
    public ResponseEntity<ApiResponse<Void>> relotteryTrack(
            @PathVariable Integer teamId,
            @PathVariable Integer roundId,
            @Valid @RequestBody RelotteryTrackRequest request) {
        studentPortalService.relotteryTrack(teamId, roundId, request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Track updated"));
    }

    @PostMapping("/tracks/{trackId}/select")
    @Operation(summary = "FR-U-15-F — Chọn track (fall)")
    public ResponseEntity<ApiResponse<Void>> selectFallTrack(@PathVariable Integer trackId) {
        studentPortalService.selectFallTrack(trackId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Track selected"));
    }
}
