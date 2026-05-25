package com.sealhackathon.api.teams.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.mentor_team_assignments.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.team_members.repository.TeamMemberRepository;
import com.sealhackathon.api.team_round_participation.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.team_round_tracks.repository.TeamRoundTrackRepository;
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
import com.sealhackathon.api.teams.mapper.TeamMapper;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.service.TeamService;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * MF-02 GĐ2 — Teams (FR-11 … FR-13C). Khung xử lý; logic nghiệp vụ TODO.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRoundTrackRepository teamRoundTrackRepository;
    private final TeamRoundParticipationRepository teamRoundParticipationRepository;
    private final MentorTeamAssignmentRepository mentorTeamAssignmentRepository;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final UserRepository userRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final AuditService auditService;
    private final TeamMapper teamMapper;

    @Override
    public TeamResponse createTeam(CreateTeamRequest req) {
        // TODO FR-11: hackathon ONGOING; registration_end > NOW(); leader = current user STUDENT APPROVED
        // TODO: USER_IN_ANOTHER_TEAM; UNIQUE(team_name, hackathon_id) → TEAM_NAME_DUPLICATE
        // TODO: INSERT teams PENDING; INSERT team_members LEADER status=ACCEPTED; audit TEAM_CREATE
        throw todo("createTeam");
    }

    @Override
    @Transactional(readOnly = true)
    public TeamDetailResponse getTeam(Integer teamId) {
        // TODO: load team + members + counts; 404 if not found; leader/coordinator access rules
        throw todo("getTeam");
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamDetailResponse> listTeams(Integer hackathonId, TeamStatus status) {
        // TODO: coordinator list all; student list own teams; filter by status optional
        throw todo("listTeams");
    }

    @Override
    public TeamResponse patchTeamStatus(Integer teamId, PatchTeamStatusRequest req) {
        // TODO FR-13: ACTIVE — 3-5 ACCEPTED members, all APPROVED, hackathon ONGOING
        // TODO FR-13: REJECTED — rejectionReason required; status REJECTED
        // TODO: idempotent if already target status; audit TEAM_APPROVE / TEAM_REJECT
        throw todo("patchTeamStatus");
    }

    @Override
    public BulkApproveTeamsResponse bulkApproveTeams(BulkApproveTeamsRequest req) {
        // TODO FR-13: loop teamIds, collect errors[], partial success
        throw todo("bulkApproveTeams");
    }

    @Override
    public TeamResponse transferLeader(Integer teamId, TransferLeaderRequest req) {
        // TODO FR-11C: caller must be current leader; team PENDING; is_locked=false
        // TODO: NEW_LEADER_NOT_MEMBER; swap team_members roles; audit LEADER_TRANSFERRED
        throw todo("transferLeader");
    }

    @Override
    public void disbandTeam(Integer teamId) {
        // TODO FR-11D: leader OR coordinator; phases per doc (mentor assigned → block)
        // TODO: soft REJECTED; cleanup trt/trp if đã bốc thăm; audit TEAM_DISBANDED
        throw todo("disbandTeam");
    }

    @Override
    public void inviteMember(Integer teamId, InviteTeamMemberRequest req) {
        // TODO FR-12: resolve user by email APPROVED STUDENT; TEAM_LOCKED; TEAM_MEMBER_FULL
        // TODO: INSERT team_members PENDING; audit MEMBER_INVITED
        throw todo("inviteMember");
    }

    @Override
    public void patchTeamMember(Integer teamId, Integer userId, PatchTeamMemberRequest req) {
        // TODO FR-12: invitee self only; ACCEPT/REJECT/LEFT; LEADER_CANNOT_LEAVE_TEAM on LEFT as leader
        // TODO: audit MEMBER_ACCEPTED / MEMBER_REJECTED / MEMBER_LEFT
        throw todo("patchTeamMember");
    }

    @Override
    public void removePendingMember(Integer teamId, Integer userId) {
        // TODO DELETE §B: leader only; member status=PENDING; is_locked=false
        // TODO: hard DELETE team_members row; audit MEMBER_INVITE_CANCELLED
        throw todo("removePendingMember");
    }

    @Override
    public TeamResponse reassignTrack(Integer teamId, Integer roundId, ReassignTeamTrackRequest req) {
        // TODO FR-13B-R: ROUND_ALREADY_ACTIVE; UPDATE team_round_tracks; audit TEAM_TRACK_CHANGED
        throw todo("reassignTrack");
    }

    @Override
    public void assignMentor(Integer teamId, Integer roundId, AssignTeamMentorRequest req) {
        // TODO FR-13C: team_round_participation exists; UNIQUE(team, round); audit MENTOR_TEAM_ASSIGNED
        throw todo("assignMentor");
    }

    @Override
    public void removeMentor(Integer teamId, Integer roundId) {
        // TODO DELETE §B: no scores in round for team; DELETE mentor_team_assignments
        throw todo("removeMentor");
    }

    @Override
    @Transactional(readOnly = true)
    public TeamMentorHistoryResponse listMentorHistory(Integer teamId) {
        // TODO FR-13C: join rounds for names; mentor/coordinator read access
        throw todo("listMentorHistory");
    }

    private static UnsupportedOperationException todo(String method) {
        return new UnsupportedOperationException("TODO: implement " + method);
    }
}
