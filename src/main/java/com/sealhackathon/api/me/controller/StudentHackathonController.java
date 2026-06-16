package com.sealhackathon.api.me.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.StudentOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.hackathon_registrations.service.HackathonRegistrationService;
import com.sealhackathon.api.me.student.dto.response.StudentHackathonBrowseItemResponse;
import com.sealhackathon.api.me.student.service.StudentPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Student Portal", description = "FR-U — Hackathon browse & register")
@RestController
@RequestMapping("/api/v1/me/hackathons")
@RequiredArgsConstructor
@StudentOnly
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class StudentHackathonController {

    private final StudentPortalService studentPortalService;
    private final HackathonRegistrationService hackathonRegistrationService;

    @GetMapping("/browse")
    @Operation(summary = "FR-U-05 — Danh sách hackathon (student)")
    public ResponseEntity<ApiResponse<List<StudentHackathonBrowseItemResponse>>> browse(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.browseHackathons(status)));
    }

    @PostMapping("/{hackathonId}/register")
    @Operation(summary = "FR-U-06 — Đăng ký hackathon")
    public ResponseEntity<ApiResponse<Void>> register(@PathVariable Integer hackathonId) {
        hackathonRegistrationService.register(hackathonId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Registered"));
    }

    @DeleteMapping("/{hackathonId}/register")
    @Operation(summary = "FR-U-06 — Hủy đăng ký")
    public ResponseEntity<ApiResponse<Void>> unregister(@PathVariable Integer hackathonId) {
        hackathonRegistrationService.unregister(hackathonId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Unregistered"));
    }

    @GetMapping("/{hackathonId}/rankings")
    @Operation(summary = "FR-U-27 — Bảng xếp hạng hackathon (student view)")
    public ResponseEntity<ApiResponse<com.sealhackathon.api.me.student.dto.response.StudentRankingResponse>> rankings(
            @PathVariable Integer hackathonId) {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.getHackathonRankings(hackathonId)));
    }

    @GetMapping("/{hackathonId}/final-round")
    @Operation(summary = "GĐ5 — Thông tin vòng Chung kết (student, đội ADVANCED)")
    public ResponseEntity<ApiResponse<com.sealhackathon.api.me.student.dto.response.StudentFinalRoundResponse>> finalRound(
            @PathVariable Integer hackathonId) {
        return ResponseEntity.ok(ApiResponse.ok(studentPortalService.getFinalRoundForHackathon(hackathonId)));
    }
}
