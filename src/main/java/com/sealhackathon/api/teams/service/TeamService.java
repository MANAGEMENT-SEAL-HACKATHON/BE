package com.sealhackathon.api.teams.service;

import com.sealhackathon.api.teams.dto.request.*;
import com.sealhackathon.api.teams.dto.response.BulkApproveTeamsResponse;
import com.sealhackathon.api.teams.dto.response.TeamDetailResponse;
import com.sealhackathon.api.teams.dto.response.TeamMentorHistoryResponse;
import com.sealhackathon.api.teams.dto.response.TeamResponse;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.users.dto.response.UserSummaryResponse;

import java.util.List;

public interface TeamService {

    TeamResponse createTeam(CreateTeamRequest req);

    TeamDetailResponse getTeam(Integer teamId);

    List<TeamDetailResponse> listTeams(Integer hackathonId, TeamStatus status);

    TeamResponse patchTeamStatus(Integer teamId, PatchTeamStatusRequest req);

    TeamDetailResponse confirmFormation(Integer teamId);

    BulkApproveTeamsResponse bulkApproveTeams(BulkApproveTeamsRequest req);

    TeamResponse transferLeader(Integer teamId, TransferLeaderRequest req);

    void disbandTeam(Integer teamId);

    TeamResponse eliminateTeam(Integer teamId, EliminateTeamRequest req);

    void inviteMember(Integer teamId, InviteTeamMemberRequest req);

    void patchTeamMember(Integer teamId, Integer userId, PatchTeamMemberRequest req);

    void removePendingMember(Integer teamId, Integer userId);

    TeamResponse reassignTrack(Integer teamId, Integer roundId, ReassignTeamTrackRequest req);

    void assignMentor(Integer teamId, Integer roundId, AssignTeamMentorRequest req);

    void removeMentor(Integer teamId, Integer roundId);

    TeamMentorHistoryResponse listMentorHistory(Integer teamId);

    TeamDetailResponse adminCreateTeam(AdminCreateTeamRequest req);

    // Lấy danh sách các sinh viên đã đăng ký nhưng chưa có đội (Orphan Users)
    List<UserSummaryResponse> getOrphanUsers(Integer hackathonId);

    // Lấy danh sách các đội chưa đủ 3 người (Incomplete Teams)
    List<TeamDetailResponse> getIncompleteTeams(Integer hackathonId);

    // Bảng tin ghép đội cho Sinh viên (Chỉ xem các đội PENDING đang thiếu người)
    List<TeamDetailResponse> getMatchmakingTeams(Integer hackathonId);

    // BTC ép thêm 1 sinh viên vào đội bất kỳ
    TeamDetailResponse adminAddMember(Integer teamId, AdminAddMemberRequest req);

    // BTC gộp 2 đội thiếu người
    TeamDetailResponse adminMergeTeams(Integer targetTeamId, com.sealhackathon.api.teams.dto.request.AdminMergeTeamsRequest req);
}
