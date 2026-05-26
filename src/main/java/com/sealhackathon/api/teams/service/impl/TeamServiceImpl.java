package com.sealhackathon.api.teams.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.*;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.Hackathon;
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
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.mapper.TeamMapper;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.service.TeamService;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.entity.User;
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
    private final TrackRepository trackRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final AuditService auditService;
    private final TeamMapper teamMapper;

    @Override
    public TeamResponse createTeam(CreateTeamRequest req) {
        // 1. Kiểm tra User đang đăng nhập
        Integer currentUserId = currentUserAccessor.currentUserId();
        User leader = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED, "User không tồn tại", org.springframework.http.HttpStatus.UNAUTHORIZED));

        if (leader.getRole() != com.sealhackathon.api.users.value_object.UserRole.STUDENT) {
            throw new BusinessRuleException(ErrorCode.TEAM_LEADER_INVALID_ROLE, "Chỉ sinh viên mới được tạo đội");
        }

        // 2. Kiểm tra Hackathon
        Hackathon hackathon = hackathonRepository.findById(req.getHackathonId())
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", req.getHackathonId()));

        if (hackathon.getStatus() != com.sealhackathon.api.hackathons.value_object.HackathonStatus.ONGOING) {
            throw new BusinessRuleException(ErrorCode.HACKATHON_NOT_ONGOING, "Hackathon chưa mở đăng ký (không phải ONGOING)");
        }

        if (hackathon.getRegistrationEnd() != null && java.time.LocalDate.now().isAfter(hackathon.getRegistrationEnd())) {
            throw new BusinessRuleException(ErrorCode.REGISTRATION_CLOSED, "Đã quá hạn đăng ký tham gia Hackathon");
        }

        // 3. Kiểm tra tính hợp lệ của Đội và Leader
        if (teamRepository.existsByHackathon_IdAndTeamNameIgnoreCase(hackathon.getId(), req.getTeamName().trim())) {
            throw new ConflictException(ErrorCode.TEAM_NAME_DUPLICATE, "Tên đội đã tồn tại trong Hackathon này");
        }

        boolean alreadyInTeam = teamMemberRepository.existsAcceptedInActiveOrPendingTeam(
                leader.getId(),
                hackathon.getId(),
                java.util.List.of(TeamStatus.ACTIVE, TeamStatus.PENDING),
                com.sealhackathon.api.team_members.value_object.TeamMemberStatus.ACCEPTED
        );

        if (alreadyInTeam) {
            throw new ConflictException(ErrorCode.USER_IN_ANOTHER_TEAM, "Bạn đã tham gia một đội khác trong Hackathon này");
        }

        // 4. Tạo Team (Lấy Chapter của Leader làm Chapter của Team)
        Team team = Team.builder()
                .hackathon(hackathon)
                .teamName(req.getTeamName().trim())
                .leader(leader)
                .chapter(leader.getChapter())
                .status(TeamStatus.PENDING)
                .isLocked(false)
                .build();
        Team savedTeam = teamRepository.save(team);

        // 5. Thêm Leader vào danh sách thành viên
        com.sealhackathon.api.team_members.entity.TeamMember leaderMember = com.sealhackathon.api.team_members.entity.TeamMember.builder()
                .id(new com.sealhackathon.api.team_members.entity.TeamMemberId(savedTeam.getId(), leader.getId()))
                .team(savedTeam)
                .user(leader)
                .roleInTeam(com.sealhackathon.api.team_members.value_object.TeamMemberRole.LEADER)
                .status(com.sealhackathon.api.team_members.value_object.TeamMemberStatus.ACCEPTED)
                .joinedAt(java.time.LocalDateTime.now())
                .build();
        teamMemberRepository.save(leaderMember);

        // 6. Trả kết quả & Ghi Audit
        TeamResponse response = teamMapper.toResponse(savedTeam);
        auditService.log(com.sealhackathon.api.common.audit.AuditAction.TEAM_CREATE, "teams", savedTeam.getId(), java.util.Map.of("snapshot", response));

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TeamDetailResponse getTeam(Integer teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));

        java.util.List<com.sealhackathon.api.team_members.entity.TeamMember> members = teamMemberRepository.findByTeam_Id(teamId);

        int acceptedCount = (int) members.stream()
                .filter(m -> m.getStatus() == com.sealhackathon.api.team_members.value_object.TeamMemberStatus.ACCEPTED)
                .count();
        int pendingCount = (int) members.stream()
                .filter(m -> m.getStatus() == com.sealhackathon.api.team_members.value_object.TeamMemberStatus.PENDING)
                .count();

        java.util.List<com.sealhackathon.api.teams.dto.response.TeamMemberResponse> memberResponses = members.stream()
                .map(m -> com.sealhackathon.api.teams.dto.response.TeamMemberResponse.builder()
                        .userId(m.getUser().getId())
                        .fullName(m.getUser().getFullName())
                        .email(m.getUser().getEmail())
                        .roleInTeam(m.getRoleInTeam())
                        .status(m.getStatus())
                        .build())
                .toList();

        return TeamDetailResponse.builder()
                .id(team.getId())
                .hackathonId(team.getHackathon().getId())
                .hackathonName(team.getHackathon().getName())
                .teamName(team.getTeamName())
                .leaderId(team.getLeader().getId())
                .leaderName(team.getLeader().getFullName())
                .chapterId(team.getChapter() != null ? team.getChapter().getId() : null)
                .status(team.getStatus())
                .isLocked(team.getIsLocked())
                .lockedAt(team.getLockedAt())
                .rejectionReason(team.getRejectionReason())
                .createdAt(team.getCreatedAt())
                .acceptedMemberCount(acceptedCount)
                .pendingInviteCount(pendingCount)
                .members(memberResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<TeamDetailResponse> listTeams(Integer hackathonId, TeamStatus status) {
        Integer currentUserId = currentUserAccessor.currentUserId();
        com.sealhackathon.api.common.security.CurrentUserStub currentUser = currentUserAccessor.currentUser();

        java.util.List<Team> teams;

        // Nếu là Coordinator -> Thấy toàn bộ đội của Hackathon
        if (currentUser.getRole() == com.sealhackathon.api.users.value_object.UserRole.COORDINATOR) {
            if (status != null) {
                teams = teamRepository.findByHackathon_IdAndStatus(hackathonId, status);
            } else {
                teams = teamRepository.findByHackathon_Id(hackathonId);
            }
        }
        // Nếu là Student -> Chỉ thấy đội do mình làm Leader (Có thể mở rộng thấy đội mình làm Member sau)
        else {
            teams = teamRepository.findByLeader_Id(currentUserId).stream()
                    .filter(t -> t.getHackathon().getId().equals(hackathonId))
                    .toList();
        }

        // Map sang Detail Response (Có thể tối ưu N+1 query sau, tạm thời vòng lặp cho logic chuẩn)
        return teams.stream().map(t -> getTeam(t.getId())).toList();
    }

    @Override
    public TeamResponse patchTeamStatus(Integer teamId, PatchTeamStatusRequest req) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));

        if (team.getStatus() == req.getStatus()) {
            return teamMapper.toResponse(team);
        }

        if (req.getStatus() == TeamStatus.ACTIVE) {
            if (team.getHackathon().getStatus() != com.sealhackathon.api.hackathons.value_object.HackathonStatus.ONGOING) {
                throw new BusinessRuleException(ErrorCode.HACKATHON_NOT_ONGOING, "Chỉ duyệt đội khi Hackathon đang ONGOING");
            }
            long acceptedCount = teamMemberRepository.countByTeam_IdAndStatus(teamId, com.sealhackathon.api.team_members.value_object.TeamMemberStatus.ACCEPTED);
            if (acceptedCount < 3 || acceptedCount > 5) {
                throw new BusinessRuleException(ErrorCode.TEAM_INVALID_MEMBER_COUNT,
                        "Đội cần từ 3 đến 5 thành viên đã chấp nhận (hiện tại: " + acceptedCount + ")",
                        java.util.Map.of("accepted", acceptedCount, "min", 3, "max", 5));
            }
            team.setStatus(TeamStatus.ACTIVE);
            team.setRejectionReason(null);
            auditService.log(com.sealhackathon.api.common.audit.AuditAction.TEAM_APPROVE, "teams", teamId);

        } else if (req.getStatus() == TeamStatus.REJECTED) {
            if (req.getRejectionReason() == null || req.getRejectionReason().isBlank()) {
                throw new BusinessRuleException(ErrorCode.REJECTION_REASON_REQUIRED, "Từ chối đội bắt buộc phải có lý do");
            }
            team.setStatus(TeamStatus.REJECTED);
            team.setRejectionReason(req.getRejectionReason().trim());
            auditService.log(com.sealhackathon.api.common.audit.AuditAction.TEAM_REJECT, "teams", teamId,
                    java.util.Map.of("reason", req.getRejectionReason()));
        } else {
            throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION, "Trạng thái đội không hợp lệ");
        }

        return teamMapper.toResponse(teamRepository.save(team));
    }

    @Override
    public BulkApproveTeamsResponse bulkApproveTeams(BulkApproveTeamsRequest req) {
        Hackathon hackathon = hackathonRepository.findById(req.getHackathonId())
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", req.getHackathonId()));

        if (hackathon.getStatus() != com.sealhackathon.api.hackathons.value_object.HackathonStatus.ONGOING) {
            throw new BusinessRuleException(ErrorCode.HACKATHON_NOT_ONGOING, "Hackathon chưa ONGOING");
        }

        int approvedCount = 0;
        java.util.List<Integer> approvedIds = new java.util.ArrayList<>();
        java.util.List<String> errors = new java.util.ArrayList<>();

        for (Integer teamId : req.getTeamIds()) {
            try {
                Team team = teamRepository.findById(teamId).orElse(null);
                if (team == null || !team.getHackathon().getId().equals(hackathon.getId())) {
                    errors.add("Team ID " + teamId + " không tồn tại trong Hackathon này");
                    continue;
                }
                if (team.getStatus() == TeamStatus.ACTIVE) {
                    continue; // Đã duyệt rồi thì bỏ qua
                }
                long acceptedCount = teamMemberRepository.countByTeam_IdAndStatus(teamId, com.sealhackathon.api.team_members.value_object.TeamMemberStatus.ACCEPTED);
                if (acceptedCount < 3 || acceptedCount > 5) {
                    errors.add("Team ID " + teamId + " (" + team.getTeamName() + ") có " + acceptedCount + " thành viên (yêu cầu 3-5)");
                    continue;
                }

                team.setStatus(TeamStatus.ACTIVE);
                team.setRejectionReason(null);
                teamRepository.save(team);
                approvedCount++;
                approvedIds.add(teamId);
                auditService.log(com.sealhackathon.api.common.audit.AuditAction.TEAM_APPROVE, "teams", teamId);
            } catch (Exception e) {
                errors.add("Team ID " + teamId + " lỗi: " + e.getMessage());
            }
        }

        return BulkApproveTeamsResponse.builder()
                .approvedCount(approvedCount)
                .approvedTeamIds(approvedIds)
                .errors(errors)
                .build();
    }

    @Override
    public TeamResponse transferLeader(Integer teamId, TransferLeaderRequest req) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));

        if (!team.getLeader().getId().equals(currentUserAccessor.currentUserId())) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN, "Chỉ nhóm trưởng hiện tại mới được chuyển quyền");
        }
        if (Boolean.TRUE.equals(team.getIsLocked())) {
            throw new BusinessRuleException(ErrorCode.TEAM_LOCKED, "Đội đã bị khóa");
        }

        Integer newLeaderId = req.getNewLeaderId();
        com.sealhackathon.api.team_members.entity.TeamMember newLeaderMember = teamMemberRepository.findById(new com.sealhackathon.api.team_members.entity.TeamMemberId(teamId, newLeaderId))
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.NEW_LEADER_NOT_MEMBER, "Người được chọn không có trong đội"));

        if (newLeaderMember.getStatus() != com.sealhackathon.api.team_members.value_object.TeamMemberStatus.ACCEPTED) {
            throw new BusinessRuleException(ErrorCode.NEW_LEADER_NOT_APPROVED, "Người được chọn phải đã ACCEPTED vào đội");
        }

        // 1. Hạ quyền Leader cũ xuống Member
        com.sealhackathon.api.team_members.entity.TeamMember oldLeaderMember = teamMemberRepository.findById(new com.sealhackathon.api.team_members.entity.TeamMemberId(teamId, team.getLeader().getId())).orElseThrow();
        oldLeaderMember.setRoleInTeam(com.sealhackathon.api.team_members.value_object.TeamMemberRole.MEMBER);

        // 2. Nâng quyền Member mới lên Leader
        newLeaderMember.setRoleInTeam(com.sealhackathon.api.team_members.value_object.TeamMemberRole.LEADER);

        teamMemberRepository.save(oldLeaderMember);
        teamMemberRepository.save(newLeaderMember);

        // 3. Cập nhật bảng Team
        User newLeaderUser = userRepository.findById(newLeaderId).orElseThrow();
        team.setLeader(newLeaderUser);
        Team savedTeam = teamRepository.save(team);

        auditService.log(com.sealhackathon.api.common.audit.AuditAction.LEADER_TRANSFERRED, "teams", teamId,
                java.util.Map.of("oldLeaderId", oldLeaderMember.getUser().getId(), "newLeaderId", newLeaderId));

        return teamMapper.toResponse(savedTeam);
    }

    @Override
    public void disbandTeam(Integer teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));

        Integer currentUserId = currentUserAccessor.currentUserId();
        com.sealhackathon.api.common.security.CurrentUserStub currentUser = currentUserAccessor.currentUser();

        boolean isLeader = team.getLeader().getId().equals(currentUserId);
        boolean isCoordinator = currentUser.getRole() == com.sealhackathon.api.users.value_object.UserRole.COORDINATOR;

        if (!isLeader && !isCoordinator) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN, "Chỉ nhóm trưởng hoặc Coordinator mới có quyền giải tán đội");
        }

        if (mentorTeamAssignmentRepository.existsByTeam_Id(teamId)) {
            throw new ConflictException(ErrorCode.TEAM_HAS_MENTOR_CANNOT_DISBAND, "Đội đã được phân công Mentor, không thể giải tán");
        }

        team.setStatus(TeamStatus.REJECTED);
        team.setRejectionReason("Đội đã giải tán");
        teamRepository.save(team);

        java.util.List<com.sealhackathon.api.team_members.entity.TeamMember> members = teamMemberRepository.findByTeam_Id(teamId);
        for(com.sealhackathon.api.team_members.entity.TeamMember m : members) {
            m.setStatus(com.sealhackathon.api.team_members.value_object.TeamMemberStatus.LEFT);
            m.setLeftAt(java.time.LocalDateTime.now());
        }
        teamMemberRepository.saveAll(members);

        java.util.List<com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack> trts = teamRoundTrackRepository.findByTeam_Id(teamId);
        teamRoundTrackRepository.deleteAll(trts);

        java.util.List<com.sealhackathon.api.team_round_participation.entity.TeamRoundParticipation> trps = teamRoundParticipationRepository.findByTeam_Id(teamId);
        teamRoundParticipationRepository.deleteAll(trps);

        auditService.log(com.sealhackathon.api.common.audit.AuditAction.TEAM_DISBAND, "teams", teamId);
    }

    @Override
    public void inviteMember(Integer teamId, InviteTeamMemberRequest req) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));

        if (!team.getLeader().getId().equals(currentUserAccessor.currentUserId())) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN, "Chỉ nhóm trưởng mới có quyền mời thành viên");
        }
        if (Boolean.TRUE.equals(team.getIsLocked())) {
            throw new BusinessRuleException(ErrorCode.TEAM_LOCKED, "Đội đã bị khóa, không thể mời thêm thành viên");
        }

        User invitee = userRepository.findByEmail(req.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getEmail()));

        if (invitee.getRole() != com.sealhackathon.api.users.value_object.UserRole.STUDENT) {
            throw new BusinessRuleException(ErrorCode.INVITEE_INVALID_ROLE, "Người được mời phải là Sinh viên");
        }
        if (invitee.getStatus() != com.sealhackathon.api.users.value_object.UserStatus.APPROVED) {
            throw new BusinessRuleException(ErrorCode.INVITEE_NOT_APPROVED, "Tài khoản của người được mời chưa được BTC duyệt");
        }

        java.util.List<com.sealhackathon.api.team_members.entity.TeamMember> currentMembers = teamMemberRepository.findByTeam_Id(teamId);
        long activeOrPendingCount = currentMembers.stream()
                .filter(m -> m.getStatus() == com.sealhackathon.api.team_members.value_object.TeamMemberStatus.ACCEPTED
                        || m.getStatus() == com.sealhackathon.api.team_members.value_object.TeamMemberStatus.PENDING)
                .count();
        if (activeOrPendingCount >= 5) {
            throw new BusinessRuleException(ErrorCode.TEAM_MEMBER_FULL, "Đội đã đạt tối đa 5 thành viên (bao gồm cả lời mời đang chờ)");
        }

        boolean alreadyInTeam = teamMemberRepository.existsAcceptedInActiveOrPendingTeam(
                invitee.getId(),
                team.getHackathon().getId(),
                java.util.List.of(TeamStatus.ACTIVE, TeamStatus.PENDING),
                com.sealhackathon.api.team_members.value_object.TeamMemberStatus.ACCEPTED
        );
        if (alreadyInTeam) {
            throw new ConflictException(ErrorCode.USER_IN_ANOTHER_TEAM, "Người này đã tham gia một đội khác trong kỳ Hackathon này");
        }

        boolean alreadyInvited = currentMembers.stream()
                .anyMatch(m -> m.getUser().getId().equals(invitee.getId()) &&
                        (m.getStatus() == com.sealhackathon.api.team_members.value_object.TeamMemberStatus.PENDING
                                || m.getStatus() == com.sealhackathon.api.team_members.value_object.TeamMemberStatus.ACCEPTED));
        if (alreadyInvited) {
            throw new ConflictException(ErrorCode.DUPLICATE_PENDING_INVITATION, "Người này đã ở trong đội hoặc đang có lời mời chờ xác nhận");
        }

        com.sealhackathon.api.team_members.entity.TeamMember newMember = com.sealhackathon.api.team_members.entity.TeamMember.builder()
                .id(new com.sealhackathon.api.team_members.entity.TeamMemberId(teamId, invitee.getId()))
                .team(team)
                .user(invitee)
                .roleInTeam(com.sealhackathon.api.team_members.value_object.TeamMemberRole.MEMBER)
                .status(com.sealhackathon.api.team_members.value_object.TeamMemberStatus.PENDING)
                .build();
        teamMemberRepository.save(newMember);

        auditService.log(com.sealhackathon.api.common.audit.AuditAction.MEMBER_INVITED, "team_members", invitee.getId(),
                java.util.Map.of("teamId", teamId, "email", invitee.getEmail()));
    }

    @Override
    public void patchTeamMember(Integer teamId, Integer userId, PatchTeamMemberRequest req) {
        if (!currentUserAccessor.currentUserId().equals(userId)) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN, "Bạn chỉ có thể phản hồi lời mời của chính mình");
        }

        com.sealhackathon.api.team_members.entity.TeamMember member = teamMemberRepository.findById(new com.sealhackathon.api.team_members.entity.TeamMemberId(teamId, userId))
                .orElseThrow(() -> new ResourceNotFoundException("TeamMember", userId));

        com.sealhackathon.api.teams.value_object.TeamMemberAction action = req.getAction();
        String auditActionLog = "";

        if (action == com.sealhackathon.api.teams.value_object.TeamMemberAction.ACCEPT) {
            if (member.getStatus() != com.sealhackathon.api.team_members.value_object.TeamMemberStatus.PENDING) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION, "Chỉ có thể ACCEPT lời mời đang ở trạng thái PENDING");
            }
            long acceptedCount = teamMemberRepository.countByTeam_IdAndStatus(teamId, com.sealhackathon.api.team_members.value_object.TeamMemberStatus.ACCEPTED);
            if (acceptedCount >= 5) {
                throw new BusinessRuleException(ErrorCode.TEAM_MEMBER_FULL, "Đội đã đủ 5 thành viên chính thức");
            }
            member.setStatus(com.sealhackathon.api.team_members.value_object.TeamMemberStatus.ACCEPTED);
            member.setJoinedAt(java.time.LocalDateTime.now());
            auditActionLog = com.sealhackathon.api.common.audit.AuditAction.MEMBER_ACCEPTED;

        } else if (action == com.sealhackathon.api.teams.value_object.TeamMemberAction.REJECT) {
            if (member.getStatus() != com.sealhackathon.api.team_members.value_object.TeamMemberStatus.PENDING) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION, "Chỉ có thể REJECT lời mời đang PENDING");
            }
            member.setStatus(com.sealhackathon.api.team_members.value_object.TeamMemberStatus.REJECTED);
            auditActionLog = com.sealhackathon.api.common.audit.AuditAction.MEMBER_REJECTED;

        } else if (action == com.sealhackathon.api.teams.value_object.TeamMemberAction.LEFT) {
            if (member.getRoleInTeam() == com.sealhackathon.api.team_members.value_object.TeamMemberRole.LEADER) {
                throw new BusinessRuleException(ErrorCode.LEADER_CANNOT_LEAVE_TEAM, "Leader không thể rời đội. Hãy chuyển quyền (Transfer) trước.");
            }
            member.setStatus(com.sealhackathon.api.team_members.value_object.TeamMemberStatus.LEFT);
            member.setLeftAt(java.time.LocalDateTime.now());
            auditActionLog = com.sealhackathon.api.common.audit.AuditAction.MEMBER_LEFT;
        }

        teamMemberRepository.save(member);
        auditService.log(auditActionLog, "team_members", userId, java.util.Map.of("teamId", teamId));
    }

    @Override
    public void removePendingMember(Integer teamId, Integer userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
        if (!team.getLeader().getId().equals(currentUserAccessor.currentUserId())) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN, "Chỉ nhóm trưởng mới được hủy lời mời");
        }
        if (Boolean.TRUE.equals(team.getIsLocked())) {
            throw new BusinessRuleException(ErrorCode.TEAM_LOCKED, "Đội đã bị khóa");
        }
        com.sealhackathon.api.team_members.entity.TeamMember member = teamMemberRepository.findById(new com.sealhackathon.api.team_members.entity.TeamMemberId(teamId, userId))
                .orElseThrow(() -> new ResourceNotFoundException("TeamMember", userId));
        if (member.getStatus() != com.sealhackathon.api.team_members.value_object.TeamMemberStatus.PENDING) {
            throw new BusinessRuleException(ErrorCode.CANNOT_DELETE_ACCEPTED_MEMBER, "Chỉ được xóa lời mời đang chờ (PENDING)");
        }

        teamMemberRepository.delete(member);
        auditService.log(com.sealhackathon.api.common.audit.AuditAction.MEMBER_INVITE_CANCELLED, "team_members", userId, java.util.Map.of("teamId", teamId));
    }

    @Override
    public TeamResponse reassignTrack(Integer teamId, Integer roundId, ReassignTeamTrackRequest req) {
        com.sealhackathon.api.team_round_tracks.entity.TeamRoundTrack trt = teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(teamId, roundId)
                .orElseThrow(() -> new ResourceNotFoundException("TeamRoundTrack", "teamId=" + teamId + ", roundId=" + roundId));

        com.sealhackathon.api.rounds.entity.Round round = roundRepository.findById(roundId).orElseThrow();
        if (Boolean.TRUE.equals(round.getIsActive()) || Boolean.TRUE.equals(round.getScoringLocked())) {
            throw new BusinessRuleException(ErrorCode.ROUND_ALREADY_ACTIVE, "Vòng thi đã kích hoạt hoặc đã khóa điểm");
        }

        com.sealhackathon.api.tracks.entity.Track newTrack = trackRepository.findById(req.getTrackId())
                .orElseThrow(() -> new ResourceNotFoundException("Track", req.getTrackId()));

        if (!newTrack.getRound().getId().equals(roundId)) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Track mới không thuộc Round này");
        }
        if (newTrack.getStatus() != com.sealhackathon.api.tracks.value_object.TrackStatus.OPEN) {
            throw new BusinessRuleException(ErrorCode.TRACK_CLOSED, "Track mới đã đóng");
        }

        Integer oldTrackId = trt.getTrack().getId();
        trt.setTrack(newTrack);
        trt.setAssignedGroup(req.getAssignedGroup());
        trt.setAssignedAt(java.time.LocalDateTime.now());
        if (currentUserAccessor.currentUserId() != null) {
            trt.setAssignedBy(userRepository.findById(currentUserAccessor.currentUserId()).orElse(null));
        }

        teamRoundTrackRepository.save(trt);

        auditService.log(com.sealhackathon.api.common.audit.AuditAction.TEAM_TRACK_CHANGED, "team_round_tracks", trt.getId(),
                java.util.Map.of("teamId", teamId, "oldTrackId", oldTrackId, "newTrackId", newTrack.getId()));

        return teamMapper.toResponse(trt.getTeam());
    }

    @Override
    public void assignMentor(Integer teamId, Integer roundId, AssignTeamMentorRequest req) {
        com.sealhackathon.api.team_round_participation.entity.TeamRoundParticipation trp = teamRoundParticipationRepository.findByTeam_IdAndRound_Id(teamId, roundId)
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.TEAM_NOT_IN_ROUND, "Đội chưa tham gia vào vòng này"));

        com.sealhackathon.api.rounds.entity.Round round = trp.getRound();
        if (Boolean.TRUE.equals(round.getIsFinal())) {
            throw new BusinessRuleException(ErrorCode.MENTOR_ASSIGNMENT_NOT_FOR_FINAL_ROUND, "Không gán Mentor cho Vòng Chung kết");
        }

        if (mentorTeamAssignmentRepository.findByTeam_IdAndRound_Id(teamId, roundId).isPresent()) {
            throw new ConflictException(ErrorCode.TEAM_ALREADY_HAS_MENTOR_IN_ROUND, "Đội đã có Mentor trong vòng này");
        }

        User mentor = userRepository.findById(req.getMentorId())
                .orElseThrow(() -> new ResourceNotFoundException("User (Mentor)", req.getMentorId()));

        if (mentor.getRole() != com.sealhackathon.api.users.value_object.UserRole.MENTOR) {
            throw new BusinessRuleException(ErrorCode.USER_INVALID_ROLE, "User được gán phải có role là MENTOR");
        }

        com.sealhackathon.api.mentor_team_assignments.entity.MentorTeamAssignment mta = com.sealhackathon.api.mentor_team_assignments.entity.MentorTeamAssignment.builder()
                .mentor(mentor)
                .team(trp.getTeam())
                .round(round)
                .hackathon(trp.getHackathon())
                .assignedAt(java.time.LocalDateTime.now())
                .build();
        if (currentUserAccessor.currentUserId() != null) {
            mta.setAssignedBy(userRepository.findById(currentUserAccessor.currentUserId()).orElse(null));
        }

        com.sealhackathon.api.mentor_team_assignments.entity.MentorTeamAssignment saved = mentorTeamAssignmentRepository.save(mta);

        auditService.log(com.sealhackathon.api.common.audit.AuditAction.MENTOR_TEAM_ASSIGNED, "mentor_team_assignments", saved.getId(),
                java.util.Map.of("teamId", teamId, "roundId", roundId, "mentorId", mentor.getId()));
    }

    @Override
    public void removeMentor(Integer teamId, Integer roundId) {
        com.sealhackathon.api.mentor_team_assignments.entity.MentorTeamAssignment mta = mentorTeamAssignmentRepository.findByTeam_IdAndRound_Id(teamId, roundId)
                .orElseThrow(() -> new ResourceNotFoundException("MentorTeamAssignment", "teamId=" + teamId + ", roundId=" + roundId));

        mentorTeamAssignmentRepository.delete(mta);

        auditService.log(com.sealhackathon.api.common.audit.AuditAction.MENTOR_TEAM_UNASSIGNED, "mentor_team_assignments", mta.getId(),
                java.util.Map.of("teamId", teamId, "roundId", roundId, "mentorId", mta.getMentor().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public TeamMentorHistoryResponse listMentorHistory(Integer teamId) {
        java.util.List<com.sealhackathon.api.mentor_team_assignments.entity.MentorTeamAssignment> assignments = mentorTeamAssignmentRepository.findByTeam_IdOrderByRound_IdAsc(teamId);

        java.util.List<TeamMentorHistoryResponse.Item> items = assignments.stream()
                .map(a -> TeamMentorHistoryResponse.Item.builder()
                        .roundId(a.getRound().getId())
                        .roundName(a.getRound().getName())
                        .mentorId(a.getMentor().getId())
                        .mentorName(a.getMentor().getFullName())
                        .assignedAt(a.getAssignedAt())
                        .build())
                .toList();

        return TeamMentorHistoryResponse.builder()
                .teamId(teamId)
                .items(items)
                .build();
    }
}