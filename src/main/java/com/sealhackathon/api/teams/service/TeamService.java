package com.sealhackathon.api.teams.service;

import com.sealhackathon.api.teams.dto.request.AssignTeamMentorRequest;
import com.sealhackathon.api.teams.dto.request.BulkApproveTeamsRequest;
import com.sealhackathon.api.teams.dto.request.CreateTeamRequest;
import com.sealhackathon.api.teams.dto.request.InviteTeamMemberRequest;
import com.sealhackathon.api.teams.dto.request.PatchTeamMemberRequest;
import com.sealhackathon.api.teams.dto.request.PatchTeamStatusRequest;
import com.sealhackathon.api.teams.dto.request.ReassignTeamTrackRequest;
import com.sealhackathon.api.teams.dto.request.TransferLeaderRequest;
import com.sealhackathon.api.teams.dto.response.BulkApproveTeamsResponse;
import com.sealhackathon.api.teams.dto.response.TeamDetailResponse;
import com.sealhackathon.api.teams.dto.response.TeamMentorHistoryResponse;
import com.sealhackathon.api.teams.dto.response.TeamResponse;
import com.sealhackathon.api.teams.value_object.TeamStatus;

import java.util.List;

public interface TeamService {

    TeamResponse createTeam(CreateTeamRequest req);

    TeamDetailResponse getTeam(Integer teamId);

    List<TeamDetailResponse> listTeams(Integer hackathonId, TeamStatus status);

    TeamResponse patchTeamStatus(Integer teamId, PatchTeamStatusRequest req);

    BulkApproveTeamsResponse bulkApproveTeams(BulkApproveTeamsRequest req);

    TeamResponse transferLeader(Integer teamId, TransferLeaderRequest req);

    void disbandTeam(Integer teamId);

    void inviteMember(Integer teamId, InviteTeamMemberRequest req);

    void patchTeamMember(Integer teamId, Integer userId, PatchTeamMemberRequest req);

    void removePendingMember(Integer teamId, Integer userId);

    TeamResponse reassignTrack(Integer teamId, Integer roundId, ReassignTeamTrackRequest req);

    void assignMentor(Integer teamId, Integer roundId, AssignTeamMentorRequest req);

    void removeMentor(Integer teamId, Integer roundId);

    TeamMentorHistoryResponse listMentorHistory(Integer teamId);
}
