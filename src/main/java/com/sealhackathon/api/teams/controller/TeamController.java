package com.sealhackathon.api.teams.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.ApprovedOnly;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.common.security.StudentOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.teams.dto.request.*;
import com.sealhackathon.api.teams.dto.response.BulkApproveTeamsResponse;
import com.sealhackathon.api.teams.dto.response.TeamDetailResponse;
import com.sealhackathon.api.teams.dto.response.TeamMentorHistoryResponse;
import com.sealhackathon.api.teams.dto.response.TeamResponse;
import com.sealhackathon.api.teams.service.TeamService;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MF-02 GĐ2 — Teams (FR-11 … FR-13C).
 *
 * <p>Spec: {@code docs/mf02/03-api-reference-gd2.md}.
 */
@Tag(name = "Teams (GĐ2)", description = "MF-02 — Đội, thành viên, bốc thăm, mentor per-round")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    @ApprovedOnly
    @Operation(summary = "Danh sách đội theo hackathon (Coordinator: tất cả; Student: đội mình tham gia / được mời)")
    public ResponseEntity<ApiResponse<List<TeamDetailResponse>>> list(
            @RequestParam Integer hackathonId,
            @RequestParam(required = false) TeamStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.listTeams(hackathonId, status)));
    }

    @GetMapping("/{teamId}")
    @ApprovedOnly
    @Operation(summary = "Chi tiết đội + danh sách thành viên (leader/member PENDING|ACCEPTED, mentor được gán, coordinator)")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> get(@PathVariable Integer teamId) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.getTeam(teamId)));
    }

    @PatchMapping("/{teamId}/status")
    @CoordinatorOnly
    @Operation(summary = "FR-13 — Duyệt (ACTIVE) hoặc từ chối (REJECTED) đội")
    public ResponseEntity<ApiResponse<TeamResponse>> patchStatus(
            @PathVariable Integer teamId,
            @Valid @RequestBody PatchTeamStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.patchTeamStatus(teamId, req)));
    }

    @PatchMapping("/{teamId}/approve")
    @CoordinatorOnly
    @Operation(summary = "FR-13 — Shortcut duyệt → ACTIVE (tương đương status=ACTIVE)")
    public ResponseEntity<ApiResponse<TeamResponse>> approve(@PathVariable Integer teamId) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.patchTeamStatus(teamId,
                PatchTeamStatusRequest.builder().status(TeamStatus.ACTIVE).build())));
    }

    @PostMapping("/bulk-approve")
    @CoordinatorOnly
    @Operation(summary = "FR-13 — Duyệt hàng loạt đội đủ điều kiện (3–5 thành viên)")
    public ResponseEntity<ApiResponse<BulkApproveTeamsResponse>> bulkApprove(
            @Valid @RequestBody BulkApproveTeamsRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.bulkApproveTeams(req)));
    }

    @PatchMapping("/{teamId}/transfer-leader")
    @ApprovedOnly
    @Operation(summary = "FR-11C — Chuyển quyền Leader (caller phải là leader hiện tại)")
    public ResponseEntity<ApiResponse<TeamResponse>> transferLeader(
            @PathVariable Integer teamId,
            @Valid @RequestBody TransferLeaderRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.transferLeader(teamId, req)));
    }

    @PostMapping("/{teamId}/confirm-formation")
    @StudentOnly
    @Operation(summary = "Leader xác nhận roster — gửi Coordinator duyệt sớm (một lần)")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> confirmFormation(@PathVariable Integer teamId) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.confirmFormation(teamId)));
    }

    @DeleteMapping("/{teamId}")
    @ApprovedOnly
    @Operation(summary = "FR-11D — Giải tán đội (Leader hoặc Coordinator)")
    public ResponseEntity<ApiResponse<Void>> disband(@PathVariable Integer teamId) {
        teamService.disbandTeam(teamId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đội đã giải tán"));
    }

    @PatchMapping("/{teamId}/eliminate")
    @CoordinatorOnly
    @Operation(summary = "FR-21 — Loại đội vi phạm (ELIMINATE thủ công)")
    public ResponseEntity<ApiResponse<TeamResponse>> eliminate(
            @PathVariable Integer teamId,
            @Valid @RequestBody EliminateTeamRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.eliminateTeam(teamId, req)));
    }

    @PostMapping("/{teamId}/members/invite")
    @ApprovedOnly
    @Operation(summary = "FR-12 — Leader mời thành viên bằng email")
    public ResponseEntity<ApiResponse<Void>> inviteMember(
            @PathVariable Integer teamId,
            @Valid @RequestBody InviteTeamMemberRequest req) {
        teamService.inviteMember(teamId, req);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(null, "Đã gửi lời mời (TODO)"));
    }

    @PatchMapping("/{teamId}/members/{userId}")
    @ApprovedOnly
    @Operation(summary = "FR-12 — Accept / Reject / Left lời mời")
    public ResponseEntity<ApiResponse<Void>> patchMember(
            @PathVariable Integer teamId,
            @PathVariable Integer userId,
            @Valid @RequestBody PatchTeamMemberRequest req) {
        teamService.patchTeamMember(teamId, userId, req);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã cập nhật thành viên"));
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @ApprovedOnly
    @Operation(summary = "FR-12 — Leader hủy lời mời PENDING")
    public ResponseEntity<ApiResponse<Void>> removePendingMember(
            @PathVariable Integer teamId,
            @PathVariable Integer userId) {
        teamService.removePendingMember(teamId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{teamId}/rounds/{roundId}/track")
    @CoordinatorOnly
    @Operation(summary = "FR-13B-R — Re-lottery đổi Track")
    public ResponseEntity<ApiResponse<TeamResponse>> reassignTrack(
            @PathVariable Integer teamId,
            @PathVariable Integer roundId,
            @Valid @RequestBody ReassignTeamTrackRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.reassignTrack(teamId, roundId, req)));
    }

    @PostMapping("/{teamId}/rounds/{roundId}/mentor")
    @CoordinatorOnly
    @Operation(summary = "FR-13C — Phân Mentor theo vòng")
    public ResponseEntity<ApiResponse<Void>> assignMentor(
            @PathVariable Integer teamId,
            @PathVariable Integer roundId,
            @Valid @RequestBody AssignTeamMentorRequest req) {
        teamService.assignMentor(teamId, roundId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(null));
    }

    @DeleteMapping("/{teamId}/rounds/{roundId}/mentor")
    @CoordinatorOnly
    @Operation(summary = "FR-13C — Gỡ Mentor per-round (trước khi có điểm)")
    public ResponseEntity<ApiResponse<Void>> removeMentor(
            @PathVariable Integer teamId,
            @PathVariable Integer roundId) {
        teamService.removeMentor(teamId, roundId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{teamId}/mentors")
    @ApprovedOnly
    @Operation(summary = "FR-13C — Lịch sử Mentor theo vòng")
    public ResponseEntity<ApiResponse<TeamMentorHistoryResponse>> listMentors(
            @PathVariable Integer teamId) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.listMentorHistory(teamId)));
    }

    @PostMapping("/admin-create")
    @CoordinatorOnly
    @Operation(summary = "Coordinator gom nhóm thủ công", description = "BTC gom những sinh viên lẻ tạo thành 1 đội thi")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> adminCreateTeam(@Valid @RequestBody AdminCreateTeamRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(teamService.adminCreateTeam(req)));
    }

    @GetMapping("/hackathons/{hackathonId}/orphans")
    @CoordinatorOnly
    @Operation(summary = "Danh sách sinh viên chưa có đội", description = "BTC lấy danh sách SV đã đăng ký nhưng chưa có đội")
    public ResponseEntity<ApiResponse<List<com.sealhackathon.api.users.dto.response.UserSummaryResponse>>> getOrphans(@PathVariable Integer hackathonId) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.getOrphanUsers(hackathonId)));
    }

    @GetMapping("/hackathons/{hackathonId}/incomplete-teams")
    @CoordinatorOnly
    @Operation(summary = "Danh sách đội thiếu người (BTC)", description = "BTC lấy danh sách các đội đang có dưới 3 người")
    public ResponseEntity<ApiResponse<List<TeamDetailResponse>>> getIncompleteTeamsAdmin(@PathVariable Integer hackathonId) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.getIncompleteTeams(hackathonId)));
    }

    @GetMapping("/hackathons/{hackathonId}/matchmaking")
    @ApprovedOnly
    @Operation(summary = "Bảng tin ghép đội (Student)", description = "Sinh viên xem danh sách các đội đang thiếu người để chủ động xin vào")
    public ResponseEntity<ApiResponse<List<TeamDetailResponse>>> getMatchmakingTeams(@PathVariable Integer hackathonId) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.getMatchmakingTeams(hackathonId)));
    }

    @PostMapping("/{teamId}/admin-add-member")
    @CoordinatorOnly
    @Operation(summary = "Ép thêm sinh viên vào đội", description = "BTC chủ động nhét 1 sinh viên lẻ vào 1 đội")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> adminAddMember(
            @PathVariable Integer teamId,
            @Valid @RequestBody com.sealhackathon.api.teams.dto.request.AdminAddMemberRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.adminAddMember(teamId, req)));
    }

    @PostMapping("/{teamId}/admin-merge")
    @CoordinatorOnly
    @Operation(summary = "BTC Gộp 2 đội", description = "BTC gộp 2 đội thiếu người thành 1 đội hợp lệ")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> adminMergeTeams(
            @PathVariable Integer teamId,
            @Valid @RequestBody com.sealhackathon.api.teams.dto.request.AdminMergeTeamsRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.adminMergeTeams(teamId, req)));
    }
}
