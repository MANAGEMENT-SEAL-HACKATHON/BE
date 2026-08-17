package com.sealhackathon.api.me.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.MentorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.me.mentor.dto.response.*;
import com.sealhackathon.api.me.mentor.service.MentorPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Mentor Portal", description = "FR-M — Portal mentor /api/v1/me/*")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@MentorOnly
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class MentorMeController {

    private final MentorPortalService mentorPortalService;

    @GetMapping("/mentor-track-assignments")
    @Operation(summary = "FR-M-05 — Phân công track")
    public ResponseEntity<ApiResponse<List<MentorTrackAssignmentResponse>>> trackAssignments() {
        return ResponseEntity.ok(ApiResponse.ok(mentorPortalService.listTrackAssignments()));
    }

    @GetMapping("/mentor/rounds")
    @Operation(summary = "GĐ3 — Danh sách vòng thi của mentor")
    public ResponseEntity<ApiResponse<List<MentorRoundResponse>>> mentorRounds() {
        return ResponseEntity.ok(ApiResponse.ok(mentorPortalService.getMentorRounds()));
    }

    @GetMapping("/mentor/rounds/{roundId}/assigned-teams")
    @Operation(summary = "GĐ3 — Đội được phân công theo vòng (enriched)")
    public ResponseEntity<ApiResponse<MentorAssignedTeamsResponse>> assignedTeams(
            @PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(mentorPortalService.getAssignedTeamsForRound(roundId)));
    }

    @GetMapping("/mentor-team-assignments")
    @Operation(summary = "FR-M-06 — Phân công đội")
    public ResponseEntity<ApiResponse<List<MentorTeamAssignmentResponse>>> teamAssignments(
            @RequestParam(required = false) Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(mentorPortalService.listTeamAssignments(roundId)));
    }

    @GetMapping("/mentor-team-assignments/{teamId}/presentation-slot")
    @Operation(summary = "FR-M-12 — Slot thuyết trình")
    public ResponseEntity<ApiResponse<MentorPresentationSlotResponse>> presentationSlot(
            @PathVariable Integer teamId) {
        return ResponseEntity.ok(ApiResponse.ok(mentorPortalService.getPresentationSlot(teamId)));
    }

    @GetMapping("/mentor/teams/{teamId}/submissions")
    @Operation(summary = "FR-M-10 — Bài nộp của đội (mentor)")
    public ResponseEntity<ApiResponse<List<MentorSubmissionViewResponse>>> submissions(
            @PathVariable Integer teamId,
            @RequestParam(required = false) Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(mentorPortalService.listTeamSubmissions(teamId, roundId)));
    }

    @GetMapping("/mentor/teams/{teamId}/scores")
    @Operation(summary = "FR-M-13 — Điểm đội sau scoring_locked")
    public ResponseEntity<ApiResponse<List<MentorTeamScoreResponse>>> scores(
            @PathVariable Integer teamId,
            @RequestParam(required = false) Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(mentorPortalService.listTeamScores(teamId, roundId)));
    }

    @GetMapping("/mentor/rounds/{roundId}/schedule")
    @Operation(summary = "FR-M-16 — Lịch Chung kết (passive)")
    public ResponseEntity<ApiResponse<MentorRoundScheduleResponse>> finalRoundSchedule(
            @PathVariable Integer roundId) {
        return ResponseEntity.ok(ApiResponse.ok(mentorPortalService.getFinalRoundSchedule(roundId)));
    }

    @GetMapping("/mentor/hackathons/{hackathonId}/rankings")
    @Operation(summary = "FR-M-18 — Xếp hạng (mentor read-only)")
    public ResponseEntity<ApiResponse<MentorRankingResponse>> rankings(@PathVariable Integer hackathonId) {
        return ResponseEntity.ok(ApiResponse.ok(mentorPortalService.getHackathonRankings(hackathonId)));
    }

    @GetMapping("/mentor-history")
    @Operation(summary = "FR-M-19 — Lịch sử mentor")
    public ResponseEntity<ApiResponse<MentorHistoryResponse>> history(
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(mentorPortalService.getHistory(year)));
    }
}
