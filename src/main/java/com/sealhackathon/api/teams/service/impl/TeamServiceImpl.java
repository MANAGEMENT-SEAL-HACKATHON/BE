package com.sealhackathon.api.teams.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.*;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.hackathons.entity.HackathonRegistration;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.mentors.repository.MentorAssignmentRepository;
import com.sealhackathon.api.mentors.repository.MentorTeamAssignmentRepository;
import com.sealhackathon.api.rounds.query.RoundRankingQueryService;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.entity.TeamMemberId;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberRole;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.repository.TeamRoundParticipationRepository;
import com.sealhackathon.api.teams.repository.TeamRoundTrackRepository;
import com.sealhackathon.api.teams.dto.request.*;
import com.sealhackathon.api.teams.dto.response.BulkApproveTeamsResponse;
import com.sealhackathon.api.teams.dto.response.TeamDetailResponse;
import com.sealhackathon.api.teams.dto.response.TeamMentorHistoryResponse;
import com.sealhackathon.api.teams.dto.response.TeamResponse;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.mapper.TeamMapper;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.service.TeamDqBackfillService;
import com.sealhackathon.api.teams.service.TeamMembershipReleaseService;
import com.sealhackathon.api.teams.service.TeamService;
import com.sealhackathon.api.hackathons.support.HackathonRegistrationSupport;
import com.sealhackathon.api.teams.support.HackathonTeamSizeResolver;
import com.sealhackathon.api.teams.support.TeamAccessGuard;
import com.sealhackathon.api.teams.value_object.ParticipationStatus;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.tracks.repository.TrackRepository;
import com.sealhackathon.api.users.dto.response.UserSummaryResponse;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final HackathonRepository hackathonRepository;
    private final RoundRepository roundRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final AuditService auditService;
    private final TeamMapper teamMapper;
    private final TeamAccessGuard teamAccessGuard;
    private final HackathonTeamSizeResolver teamSizeResolver;
    private final NotificationService notificationService;
    private final TeamMembershipReleaseService teamMembershipReleaseService;
    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final TeamDqBackfillService teamDqBackfillService;
    private final RoundRankingQueryService roundRankingQueryService;

    private HackathonTeamSizeResolver.TeamSizeLimits limitsFor(Team team) {
        return teamSizeResolver.forTeam(team);
    }

    private HackathonTeamSizeResolver.TeamSizeLimits limitsForHackathon(Integer hackathonId) {
        return teamSizeResolver.forHackathon(hackathonId);
    }

    /** Chặn tạo/gộp đội sau khi vòng Sơ loại đã kích hoạt — tránh chen cohort đã lottery. */
    private void assertPrelimRoundNotActive(Integer hackathonId) {
        boolean prelimActive = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .anyMatch(r -> !Boolean.TRUE.equals(r.getIsFinal()) && Boolean.TRUE.equals(r.getIsActive()));
        if (prelimActive) {
            throw new BusinessRuleException(ErrorCode.ROUND_ALREADY_ACTIVE,
                    "Vòng Sơ loại đã kích hoạt — không thể tạo/gộp đội mới vào cohort thi.");
        }
    }

    private void assertAcceptedCountInRange(long acceptedCount, HackathonTeamSizeResolver.TeamSizeLimits limits) {
        if (acceptedCount < limits.minTeamSize() || acceptedCount > limits.maxTeamSize()) {
            throw new BusinessRuleException(ErrorCode.TEAM_INVALID_MEMBER_COUNT,
                    "Đội cần từ %d đến %d thành viên đã chấp nhận (hiện tại: %d)"
                            .formatted(limits.minTeamSize(), limits.maxTeamSize(), acceptedCount),
                    java.util.Map.of(
                            "accepted", acceptedCount,
                            "min", limits.minTeamSize(),
                            "max", limits.maxTeamSize()));
        }
    }

    /** Coordinator chỉ duyệt khi leader đã xác nhận thành lập, đủ thành viên, không còn lời mời chờ, mọi TK đã duyệt. */
    private void assertCoordinatorCanApproveTeam(Team team, Integer teamId) {
        if (team.getStatus() != TeamStatus.PENDING) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Chỉ duyệt đội đang chờ duyệt (PENDING).");
        }
        if (team.getFormationSubmittedAt() == null) {
            throw new BusinessRuleException(ErrorCode.TEAM_FORMATION_NOT_SUBMITTED,
                    "Trưởng nhóm chưa xác nhận thành lập đội. Chỉ duyệt sau khi leader bấm xác nhận.");
        }
        long acceptedCount = teamMemberRepository.countByTeam_IdAndStatus(
                teamId, com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED);
        assertAcceptedCountInRange(acceptedCount, limitsFor(team));
        long pendingCount = teamMemberRepository.countByTeam_IdAndStatus(
                teamId, com.sealhackathon.api.teams.value_object.TeamMemberStatus.PENDING);
        if (pendingCount > 0) {
            throw new BusinessRuleException(ErrorCode.TEAM_HAS_PENDING_MEMBERS,
                    "Đội vẫn còn " + pendingCount + " lời mời chờ phản hồi, cần xử lý trước khi duyệt.");
        }
        java.util.List<TeamMember> acceptedMembers = teamMemberRepository.findByTeam_Id(teamId).stream()
                .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED)
                .toList();
        for (TeamMember member : acceptedMembers) {
            if (member.getUser().getStatus() != com.sealhackathon.api.users.value_object.UserStatus.APPROVED) {
                throw new BusinessRuleException(ErrorCode.TEAM_HAS_UNAPPROVED_MEMBERS,
                        "Đội có thành viên chưa được duyệt tài khoản: " + member.getUser().getFullName());
            }
        }
    }

    /** Sau khi đăng ký đóng: duyệt đội ACTIVE phải khóa ngay (không chờ cron 1 phút). */
    private void lockIfRegistrationClosed(Team team) {
        Hackathon hackathon = team.getHackathon();
        if (hackathon == null) {
            return;
        }
        if (!HackathonRegistrationSupport.isRegistrationClosed(hackathon)) {
            return;
        }
        if (Boolean.TRUE.equals(team.getIsLocked())) {
            return;
        }
        team.setIsLocked(true);
        team.setLockedAt(LocalDateTime.now());
        auditService.log(AuditAction.TEAM_LOCKED, "teams", team.getId(),
                java.util.Map.of(
                        "hackathonId", hackathon.getId(),
                        "reason", "APPROVED_AFTER_REGISTRATION_CLOSED"));
    }

    /** Leader chỉ chỉnh roster khi đội PENDING, chưa xác nhận thành lập và chưa bị khóa. */
    private void assertLeaderCanChangeMembership(Team team) {
        if (Boolean.TRUE.equals(team.getIsLocked())) {
            throw new BusinessRuleException(ErrorCode.TEAM_LOCKED, "Đội đã bị khóa, không thể thay đổi thành viên.");
        }
        if (team.getFormationSubmittedAt() != null) {
            throw new BusinessRuleException(ErrorCode.TEAM_FORMATION_ALREADY_SUBMITTED,
                    "Đội đã xác nhận thành lập, không thể thay đổi thành viên.");
        }
        if (team.getStatus() != TeamStatus.PENDING) {
            throw new BusinessRuleException(ErrorCode.TEAM_ALREADY_ACTIVE,
                    "Đội đã được Coordinator duyệt, không thể thay đổi thành viên.");
        }
    }

    @Override
    public TeamResponse createTeam(CreateTeamRequest req) {
        // 1. Kiểm tra User đang đăng nhập
        Integer currentUserId = currentUserAccessor.currentUserId();
        User leader = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED, "User không tồn tại", org.springframework.http.HttpStatus.UNAUTHORIZED));

        if (leader.getRole() != com.sealhackathon.api.users.value_object.UserRole.STUDENT) {
            throw new BusinessRuleException(ErrorCode.TEAM_LEADER_INVALID_ROLE, "Chỉ sinh viên mới được tạo đội");
        }

        // 2. Kiểm tra Hackathon (PESSIMISTIC_WRITE — serialize với lottery / close-early)
        Hackathon hackathon = hackathonRepository.findByIdForUpdate(req.getHackathonId())
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", req.getHackathonId()));

        if (hackathon.getStatus() != com.sealhackathon.api.hackathons.value_object.HackathonStatus.ONGOING) {
            throw new BusinessRuleException(ErrorCode.HACKATHON_NOT_ONGOING, "Hackathon chưa mở đăng ký (không phải ONGOING)");
        }

        if (HackathonRegistrationSupport.isRegistrationClosed(hackathon)) {
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
                com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED
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
        com.sealhackathon.api.teams.entity.TeamMember leaderMember = com.sealhackathon.api.teams.entity.TeamMember.builder()
                .id(new com.sealhackathon.api.teams.entity.TeamMemberId(savedTeam.getId(), leader.getId()))
                .team(savedTeam)
                .user(leader)
                .roleInTeam(com.sealhackathon.api.teams.value_object.TeamMemberRole.LEADER)
                .status(com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED)
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
        teamAccessGuard.assertCanViewTeamDetails(teamId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));

        java.util.List<com.sealhackathon.api.teams.entity.TeamMember> members = teamMemberRepository.findByTeam_Id(teamId);

        int acceptedCount = (int) members.stream()
                .filter(m -> m.getStatus() == com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED)
                .count();
        int pendingCount = (int) members.stream()
                .filter(m -> m.getStatus() == com.sealhackathon.api.teams.value_object.TeamMemberStatus.PENDING)
                .count();

        java.util.List<com.sealhackathon.api.teams.dto.response.TeamMemberResponse> memberResponses = members.stream()
                .map(m -> com.sealhackathon.api.teams.dto.response.TeamMemberResponse.builder()
                        .userId(m.getUser().getId())
                        .fullName(m.getUser().getFullName())
                        .email(m.getUser().getEmail())
                        .roleInTeam(m.getRoleInTeam())
                        .status(m.getStatus())
                        .userAccountStatus(m.getUser().getStatus())
                        .build())
                .toList();

        // Lấy thông tin Track (bảng đấu) của đội — ưu tiên TRT Sơ loại
        java.util.List<com.sealhackathon.api.teams.entity.TeamRoundTrack> trackAssignments =
                teamRoundTrackRepository.findByTeamIdWithTrackAndRound(teamId);
        Integer trackId = null;
        String trackName = null;
        String assignedGroup = null;
        String lotteryStatus = null;

        com.sealhackathon.api.teams.entity.TeamRoundTrack prelimTrt = trackAssignments.stream()
                .filter(trt -> trt.getTrack() != null
                        && trt.getTrack().getRound() != null
                        && !Boolean.TRUE.equals(trt.getTrack().getRound().getIsFinal()))
                .findFirst()
                .orElse(trackAssignments.isEmpty() ? null : trackAssignments.get(trackAssignments.size() - 1));

        if (prelimTrt != null) {
            trackId = prelimTrt.getTrack().getId();
            trackName = prelimTrt.getTrack().getName();
            assignedGroup = prelimTrt.getAssignedGroup();
            lotteryStatus = prelimTrt.getParticipationStatus() != null
                    ? prelimTrt.getParticipationStatus().name()
                    : null;
        }

        HackathonTeamSizeResolver.TeamSizeLimits sizeLimits = limitsFor(team);

        boolean hasMentor = mentorTeamAssignmentRepository.existsByTeam_Id(teamId);

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
                .formationSubmittedAt(team.getFormationSubmittedAt())
                .formationGraceDeadlineAt(team.getFormationGraceDeadlineAt())
                .acceptedMemberCount(acceptedCount)
                .pendingInviteCount(pendingCount)
                .members(memberResponses)
                .trackId(trackId)
                .trackName(trackName)
                .assignedGroup(assignedGroup)
                .lotteryStatus(lotteryStatus)
                .minTeamSize(sizeLimits.minTeamSize())
                .maxTeamSize(sizeLimits.maxTeamSize())
                .hasMentor(hasMentor)
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
        // Student: đội mình là thành viên (leader cũng nằm trong team_members)
        else {
            teams = teamMemberRepository.findTeamsByUserIdAndHackathonIdAndMemberStatusIn(
                    currentUserId,
                    hackathonId,
                    java.util.List.of(
                            com.sealhackathon.api.teams.value_object.TeamMemberStatus.PENDING,
                            com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED));
            if (status != null) {
                teams = teams.stream().filter(t -> t.getStatus() == status).toList();
            }
        }

        // Map sang Detail Response (Có thể tối ưu N+1 query sau, tạm thời vòng lặp cho logic chuẩn)
        return teams.stream().map(t -> getTeam(t.getId())).toList();
    }

    @Override
    public TeamResponse patchTeamStatus(Integer teamId, PatchTeamStatusRequest req) {
        Team team = teamRepository.findByIdForUpdate(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));

        if (team.getStatus() == req.getStatus()) {
            if (req.getStatus() == TeamStatus.ACTIVE) {
                throw new BusinessRuleException(ErrorCode.TEAM_ALREADY_ACTIVE,
                        "Đội đã được duyệt (ACTIVE)");
            }
            return teamMapper.toResponse(team);
        }

        if (req.getStatus() == TeamStatus.ACTIVE) {
            if (team.getHackathon().getStatus() != com.sealhackathon.api.hackathons.value_object.HackathonStatus.ONGOING) {
                throw new BusinessRuleException(ErrorCode.HACKATHON_NOT_ONGOING, "Chỉ duyệt đội khi Hackathon đang ONGOING");
            }
            assertCoordinatorCanApproveTeam(team, teamId);

            team.setStatus(TeamStatus.ACTIVE);
            team.setRejectionReason(null);
            team.setFormationGraceDeadlineAt(null);
            lockIfRegistrationClosed(team);
            auditService.log(com.sealhackathon.api.common.audit.AuditAction.TEAM_APPROVE, "teams", teamId);

        } else if (req.getStatus() == TeamStatus.REJECTED) {
            if (req.getRejectionReason() == null || req.getRejectionReason().isBlank()) {
                throw new BusinessRuleException(ErrorCode.REJECTION_REASON_REQUIRED, "Từ chối đội bắt buộc phải có lý do");
            }
            team.setStatus(TeamStatus.REJECTED);
            team.setRejectionReason(req.getRejectionReason().trim());
            teamRepository.save(team);
            auditService.log(com.sealhackathon.api.common.audit.AuditAction.TEAM_REJECT, "teams", teamId,
                    java.util.Map.of("reason", req.getRejectionReason()));
            teamMembershipReleaseService.releaseMembers(team, req.getRejectionReason().trim(), false);
            return teamMapper.toResponse(team);
        } else {
            throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION, "Trạng thái đội không hợp lệ");
        }

        return teamMapper.toResponse(teamRepository.save(team));
    }

    @Override
    public TeamDetailResponse confirmFormation(Integer teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));

        if (!team.getLeader().getId().equals(currentUserAccessor.currentUserId())) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN, "Chỉ trưởng nhóm mới được xác nhận thành lập đội.");
        }
        if (Boolean.TRUE.equals(team.getIsLocked())) {
            throw new BusinessRuleException(ErrorCode.TEAM_LOCKED, "Đội đã bị khóa, không thể xác nhận thành lập.");
        }
        if (team.getStatus() != TeamStatus.PENDING) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Chỉ xác nhận thành lập khi đội đang chờ duyệt.");
        }
        if (team.getFormationSubmittedAt() != null) {
            throw new BusinessRuleException(ErrorCode.TEAM_FORMATION_ALREADY_SUBMITTED,
                    "Bạn đã xác nhận thành lập đội trước đó.");
        }

        long acceptedCount = teamMemberRepository.countByTeam_IdAndStatus(
                teamId, com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED);
        assertAcceptedCountInRange(acceptedCount, limitsFor(team));

        long pendingCount = teamMemberRepository.countByTeam_IdAndStatus(
                teamId, com.sealhackathon.api.teams.value_object.TeamMemberStatus.PENDING);
        if (pendingCount > 0) {
            throw new BusinessRuleException(ErrorCode.TEAM_FORMATION_PENDING_INVITES,
                    "Đội đang còn " + pendingCount + " lời mời chờ phản hồi. "
                            + "Hãy chờ thành viên chấp nhận hoặc hủy lời mời trước khi xác nhận.");
        }

        team.setFormationSubmittedAt(LocalDateTime.now());
        team.setFormationGraceDeadlineAt(null);
        teamRepository.save(team);
        auditService.log(com.sealhackathon.api.common.audit.AuditAction.TEAM_FORMATION_CONFIRMED, "teams", teamId,
                java.util.Map.of("acceptedMembers", acceptedCount));

        notifyCoordinatorsFormationSubmitted(team);

        return getTeam(teamId);
    }

    private void notifyCoordinatorsFormationSubmitted(Team team) {
        java.util.List<User> coordinators = userRepository
                .findByRoleAndStatus(
                        com.sealhackathon.api.users.value_object.UserRole.COORDINATOR,
                        com.sealhackathon.api.users.value_object.UserStatus.APPROVED,
                        org.springframework.data.domain.Pageable.unpaged())
                .getContent();
        if (coordinators.isEmpty()) {
            return;
        }
        notificationService.sendBatch(
                coordinators,
                "TEAM_AWAITING_APPROVAL",
                "Đội mới chờ duyệt tham gia",
                "Đội \"" + team.getTeamName() + "\" vừa xác nhận thành lập và đang chờ Coordinator duyệt.",
                "teams",
                team.getId());
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
                Team team = teamRepository.findByIdForUpdate(teamId).orElse(null);
                if (team == null || !team.getHackathon().getId().equals(hackathon.getId())) {
                    errors.add("Team ID " + teamId + " không tồn tại trong Hackathon này");
                    continue;
                }
                if (team.getStatus() == TeamStatus.ACTIVE) {
                    continue;
                }
                if (team.getStatus() != TeamStatus.PENDING) {
                    errors.add("Team ID " + teamId + " không ở trạng thái chờ duyệt (PENDING)");
                    continue;
                }
                assertCoordinatorCanApproveTeam(team, teamId);

                team.setStatus(TeamStatus.ACTIVE);
                team.setRejectionReason(null);
                team.setFormationGraceDeadlineAt(null);
                lockIfRegistrationClosed(team);
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
        assertLeaderCanChangeMembership(team);

        Integer newLeaderId = req.getNewLeaderId();
        com.sealhackathon.api.teams.entity.TeamMember newLeaderMember = teamMemberRepository.findById(new com.sealhackathon.api.teams.entity.TeamMemberId(teamId, newLeaderId))
                .orElseThrow(() -> new BusinessRuleException(ErrorCode.NEW_LEADER_NOT_MEMBER, "Người được chọn không có trong đội"));

        if (newLeaderMember.getStatus() != com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED) {
            throw new BusinessRuleException(ErrorCode.NEW_LEADER_NOT_APPROVED, "Người được chọn phải đã ACCEPTED vào đội");
        }

        // 1. Hạ quyền Leader cũ xuống Member
        com.sealhackathon.api.teams.entity.TeamMember oldLeaderMember = teamMemberRepository.findById(new com.sealhackathon.api.teams.entity.TeamMemberId(teamId, team.getLeader().getId())).orElseThrow();
        oldLeaderMember.setRoleInTeam(com.sealhackathon.api.teams.value_object.TeamMemberRole.MEMBER);

        // 2. Nâng quyền Member mới lên Leader
        newLeaderMember.setRoleInTeam(com.sealhackathon.api.teams.value_object.TeamMemberRole.LEADER);

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

        if (isLeader) {
            if (Boolean.TRUE.equals(team.getIsLocked())) {
                throw new BusinessRuleException(ErrorCode.TEAM_LOCKED, "Đội đã bị khóa, không thể giải tán.");
            }
            if (team.getFormationSubmittedAt() != null) {
                throw new BusinessRuleException(ErrorCode.TEAM_FORMATION_ALREADY_SUBMITTED,
                        "Đã xác nhận thành lập đội, không thể giải tán.");
            }
            if (team.getStatus() == TeamStatus.ACTIVE) {
                throw new BusinessRuleException(ErrorCode.TEAM_ALREADY_ACTIVE,
                        "Đội đã được Coordinator duyệt. Chỉ Coordinator mới có thể giải tán.");
            }
        }

        team.setStatus(TeamStatus.REJECTED);
        team.setRejectionReason("Đội đã giải tán");
        teamRepository.save(team);

        teamMembershipReleaseService.releaseMembers(team, "Đội đã giải tán", false);

        java.util.List<com.sealhackathon.api.teams.entity.TeamRoundTrack> trts = teamRoundTrackRepository.findByTeam_Id(teamId);
        teamRoundTrackRepository.deleteAll(trts);

        java.util.List<com.sealhackathon.api.teams.entity.TeamRoundParticipation> trps = teamRoundParticipationRepository.findByTeam_Id(teamId);
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
        if (HackathonRegistrationSupport.isRegistrationClosed(team.getHackathon())) {
            throw new BusinessRuleException(ErrorCode.REGISTRATION_CLOSED,
                    "Đã hết hạn đăng ký, không thể mời thêm thành viên.");
        }
        assertLeaderCanChangeMembership(team);

        User invitee = userRepository.findByEmail(req.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getEmail()));

        if (invitee.getRole() != com.sealhackathon.api.users.value_object.UserRole.STUDENT) {
            throw new BusinessRuleException(ErrorCode.INVITEE_INVALID_ROLE, "Người được mời phải là Sinh viên");
        }
        if (invitee.getStatus() != com.sealhackathon.api.users.value_object.UserStatus.APPROVED) {
            throw new BusinessRuleException(ErrorCode.INVITEE_NOT_APPROVED, "Tài khoản của người được mời chưa được BTC duyệt");
        }

        java.util.List<com.sealhackathon.api.teams.entity.TeamMember> currentMembers = teamMemberRepository.findByTeam_Id(teamId);
        HackathonTeamSizeResolver.TeamSizeLimits inviteLimits = limitsFor(team);
        long activeOrPendingCount = currentMembers.stream()
                .filter(m -> m.getStatus() == com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED
                        || m.getStatus() == com.sealhackathon.api.teams.value_object.TeamMemberStatus.PENDING)
                .count();
        if (activeOrPendingCount >= inviteLimits.maxTeamSize()) {
            throw new BusinessRuleException(ErrorCode.TEAM_MEMBER_FULL,
                    "Đội đã đạt tối đa %d thành viên (bao gồm cả lời mời đang chờ)"
                            .formatted(inviteLimits.maxTeamSize()));
        }

        boolean alreadyInTeam = teamMemberRepository.existsAcceptedInActiveOrPendingTeam(
                invitee.getId(),
                team.getHackathon().getId(),
                java.util.List.of(TeamStatus.ACTIVE, TeamStatus.PENDING),
                com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED
        );
        if (alreadyInTeam) {
            throw new ConflictException(ErrorCode.USER_IN_ANOTHER_TEAM, "Người này đã tham gia một đội khác trong kỳ Hackathon này");
        }

        boolean alreadyInvited = currentMembers.stream()
                .anyMatch(m -> m.getUser().getId().equals(invitee.getId()) &&
                        (m.getStatus() == com.sealhackathon.api.teams.value_object.TeamMemberStatus.PENDING
                                || m.getStatus() == com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED));
        if (alreadyInvited) {
            throw new ConflictException(ErrorCode.DUPLICATE_PENDING_INVITATION, "Người này đã ở trong đội hoặc đang có lời mời chờ xác nhận");
        }

        com.sealhackathon.api.teams.entity.TeamMember newMember = com.sealhackathon.api.teams.entity.TeamMember.builder()
                .id(new com.sealhackathon.api.teams.entity.TeamMemberId(teamId, invitee.getId()))
                .team(team)
                .user(invitee)
                .roleInTeam(com.sealhackathon.api.teams.value_object.TeamMemberRole.MEMBER)
                .status(com.sealhackathon.api.teams.value_object.TeamMemberStatus.PENDING)
                .build();
        teamMemberRepository.save(newMember);

        auditService.log(com.sealhackathon.api.common.audit.AuditAction.MEMBER_INVITED, "team_members", invitee.getId(),
                java.util.Map.of("teamId", teamId, "email", invitee.getEmail()));
    }

    @Override
    public void patchTeamMember(Integer teamId, Integer userId, PatchTeamMemberRequest req) {
        Integer callerId = currentUserAccessor.currentUserId();
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));

        com.sealhackathon.api.teams.entity.TeamMember member = teamMemberRepository.findById(
                        new com.sealhackathon.api.teams.entity.TeamMemberId(teamId, userId))
                .orElseThrow(() -> new ResourceNotFoundException("TeamMember", userId));

        com.sealhackathon.api.teams.value_object.TeamMemberAction action = req.getAction();
        boolean isSelf = callerId.equals(userId);
        boolean isLeader = team.getLeader().getId().equals(callerId);
        String auditActionLog = "";
        java.util.Map<String, Object> auditMeta = new java.util.HashMap<>(java.util.Map.of("teamId", teamId));

        if (action == com.sealhackathon.api.teams.value_object.TeamMemberAction.ACCEPT
                || action == com.sealhackathon.api.teams.value_object.TeamMemberAction.REJECT) {
            if (!isSelf) {
                throw new BusinessRuleException(ErrorCode.FORBIDDEN,
                        "Bạn chỉ có thể phản hồi lời mời của chính mình");
            }
            if (Boolean.TRUE.equals(team.getIsLocked())) {
                throw new BusinessRuleException(ErrorCode.TEAM_LOCKED, "Đội đã bị khóa, không thể thay đổi thành viên");
            }
        } else if (action == com.sealhackathon.api.teams.value_object.TeamMemberAction.LEFT) {
            if (!isSelf && !isLeader) {
                throw new BusinessRuleException(ErrorCode.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
            }
            if (!isSelf && isLeader) {
                assertLeaderCanChangeMembership(team);
            } else if (Boolean.TRUE.equals(team.getIsLocked())) {
                throw new BusinessRuleException(ErrorCode.TEAM_LOCKED, "Đội đã bị khóa, không thể thay đổi thành viên");
            }
        } else {
            throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION, "Hành động thành viên không hợp lệ");
        }

        if (action == com.sealhackathon.api.teams.value_object.TeamMemberAction.ACCEPT) {
            if (member.getStatus() != com.sealhackathon.api.teams.value_object.TeamMemberStatus.PENDING) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION, "Chỉ có thể ACCEPT lời mời đang ở trạng thái PENDING");
            }
            // Re-check trong transaction: chặn race accept khi đội đang merge/disband
            Team freshTeam = teamRepository.findById(teamId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
            if (freshTeam.getStatus() != TeamStatus.PENDING && freshTeam.getStatus() != TeamStatus.ACTIVE) {
                throw new ConflictException(ErrorCode.TEAM_NOT_ACCEPTING_INVITES,
                        "Đội không còn nhận lời mời (đã bị từ chối/giải tán hoặc không còn PENDING/ACTIVE)");
            }
            boolean alreadyInTeam = teamMemberRepository.existsAcceptedInActiveOrPendingTeam(
                    userId,
                    freshTeam.getHackathon().getId(),
                    java.util.List.of(TeamStatus.ACTIVE, TeamStatus.PENDING),
                    TeamMemberStatus.ACCEPTED);
            if (alreadyInTeam) {
                throw new ConflictException(ErrorCode.USER_IN_ANOTHER_TEAM,
                        "Bạn đã tham gia một đội khác trong Hackathon này");
            }
            long acceptedCount = teamMemberRepository.countByTeam_IdAndStatus(teamId, com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED);
            HackathonTeamSizeResolver.TeamSizeLimits acceptLimits = limitsFor(freshTeam);
            if (acceptedCount >= acceptLimits.maxTeamSize()) {
                throw new BusinessRuleException(ErrorCode.TEAM_MEMBER_FULL,
                        "Đội đã đủ %d thành viên chính thức".formatted(acceptLimits.maxTeamSize()));
            }
            member.setStatus(com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED);
            member.setJoinedAt(java.time.LocalDateTime.now());
            auditActionLog = com.sealhackathon.api.common.audit.AuditAction.MEMBER_ACCEPTED;

        } else if (action == com.sealhackathon.api.teams.value_object.TeamMemberAction.REJECT) {
            if (member.getStatus() != com.sealhackathon.api.teams.value_object.TeamMemberStatus.PENDING) {
                throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION, "Chỉ có thể REJECT lời mời đang PENDING");
            }
            member.setStatus(com.sealhackathon.api.teams.value_object.TeamMemberStatus.REJECTED);
            auditActionLog = com.sealhackathon.api.common.audit.AuditAction.MEMBER_REJECTED;

        } else if (action == com.sealhackathon.api.teams.value_object.TeamMemberAction.LEFT) {
            if (member.getRoleInTeam() == com.sealhackathon.api.teams.value_object.TeamMemberRole.LEADER) {
                throw new BusinessRuleException(ErrorCode.LEADER_CANNOT_LEAVE_TEAM,
                        "Leader không thể rời đội. Hãy chuyển quyền (Transfer) trước.");
            }
            if (!isSelf) {
                if (member.getStatus() != com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED) {
                    throw new BusinessRuleException(ErrorCode.INVALID_STATUS_TRANSITION,
                            "Chỉ có thể mời rời thành viên đã tham gia. Lời mời đang chờ hãy dùng Hủy lời mời.");
                }
                auditMeta.put("removedByLeader", true);
            }
            member.setStatus(com.sealhackathon.api.teams.value_object.TeamMemberStatus.LEFT);
            member.setLeftAt(java.time.LocalDateTime.now());
            auditActionLog = com.sealhackathon.api.common.audit.AuditAction.MEMBER_LEFT;
        }

        teamMemberRepository.save(member);
        auditService.log(auditActionLog, "team_members", userId, auditMeta);
    }

    @Override
    public void removePendingMember(Integer teamId, Integer userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
        if (!team.getLeader().getId().equals(currentUserAccessor.currentUserId())) {
            throw new BusinessRuleException(ErrorCode.FORBIDDEN, "Chỉ nhóm trưởng mới được hủy lời mời");
        }
        assertLeaderCanChangeMembership(team);
        com.sealhackathon.api.teams.entity.TeamMember member = teamMemberRepository.findById(new com.sealhackathon.api.teams.entity.TeamMemberId(teamId, userId))
                .orElseThrow(() -> new ResourceNotFoundException("TeamMember", userId));
        if (member.getStatus() != com.sealhackathon.api.teams.value_object.TeamMemberStatus.PENDING) {
            throw new BusinessRuleException(ErrorCode.CANNOT_DELETE_ACCEPTED_MEMBER, "Chỉ được xóa lời mời đang chờ (PENDING)");
        }

        teamMemberRepository.delete(member);
        auditService.log(com.sealhackathon.api.common.audit.AuditAction.MEMBER_INVITE_CANCELLED, "team_members", userId, java.util.Map.of("teamId", teamId));
    }

    @Override
    public TeamResponse reassignTrack(Integer teamId, Integer roundId, ReassignTeamTrackRequest req) {
        com.sealhackathon.api.teams.entity.TeamRoundTrack trt = teamRoundTrackRepository.findByTeam_IdAndTrack_Round_Id(teamId, roundId)
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
        com.sealhackathon.api.teams.entity.TeamRoundParticipation trp = teamRoundParticipationRepository.findByTeam_IdAndRound_Id(teamId, roundId)
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

        com.sealhackathon.api.mentors.entity.MentorTeamAssignment mta = com.sealhackathon.api.mentors.entity.MentorTeamAssignment.builder()
                .mentor(mentor)
                .team(trp.getTeam())
                .round(round)
                .hackathon(trp.getHackathon())
                .assignedAt(java.time.LocalDateTime.now())
                .build();
        if (currentUserAccessor.currentUserId() != null) {
            mta.setAssignedBy(userRepository.findById(currentUserAccessor.currentUserId()).orElse(null));
        }

        com.sealhackathon.api.mentors.entity.MentorTeamAssignment saved = mentorTeamAssignmentRepository.save(mta);

        auditService.log(com.sealhackathon.api.common.audit.AuditAction.MENTOR_TEAM_ASSIGNED, "mentor_team_assignments", saved.getId(),
                java.util.Map.of("teamId", teamId, "roundId", roundId, "mentorId", mentor.getId()));
    }

    @Override
    public void removeMentor(Integer teamId, Integer roundId) {
        com.sealhackathon.api.mentors.entity.MentorTeamAssignment mta = mentorTeamAssignmentRepository.findByTeam_IdAndRound_Id(teamId, roundId)
                .orElseThrow(() -> new ResourceNotFoundException("MentorTeamAssignment", "teamId=" + teamId + ", roundId=" + roundId));

        mentorTeamAssignmentRepository.delete(mta);

        auditService.log(com.sealhackathon.api.common.audit.AuditAction.MENTOR_TEAM_UNASSIGNED, "mentor_team_assignments", mta.getId(),
                java.util.Map.of("teamId", teamId, "roundId", roundId, "mentorId", mta.getMentor().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public TeamMentorHistoryResponse listMentorHistory(Integer teamId) {
        teamAccessGuard.assertCanViewTeamDetails(teamId);

        // 1) Gán mentor theo đội (FR-13C explicit) — ưu tiên khi trùng round+mentor.
        java.util.LinkedHashMap<String, TeamMentorHistoryResponse.Item> byKey = new java.util.LinkedHashMap<>();
        for (var a : mentorTeamAssignmentRepository.findByTeam_IdOrderByRound_IdAsc(teamId)) {
            String key = a.getRound().getId() + ":" + a.getMentor().getId();
            byKey.put(key, TeamMentorHistoryResponse.Item.builder()
                    .roundId(a.getRound().getId())
                    .roundName(a.getRound().getName())
                    .mentorId(a.getMentor().getId())
                    .mentorName(a.getMentor().getFullName())
                    .assignedAt(a.getAssignedAt())
                    .build());
        }

        // 2) Mentor theo bảng đấu (GĐ1) — sau lottery đội có track thì hiện mentor bảng.
        for (var trt : teamRoundTrackRepository.findByTeamIdWithTrackAndRound(teamId)) {
            var track = trt.getTrack();
            if (track == null || track.getRound() == null) {
                continue;
            }
            Integer roundId = track.getRound().getId();
            String roundName = track.getRound().getName();
            for (var ma : mentorAssignmentRepository.findByTrackId(track.getId())) {
                String key = roundId + ":" + ma.getMentor().getId();
                byKey.putIfAbsent(key, TeamMentorHistoryResponse.Item.builder()
                        .roundId(roundId)
                        .roundName(roundName)
                        .mentorId(ma.getMentor().getId())
                        .mentorName(ma.getMentor().getFullName())
                        .assignedAt(ma.getAssignedAt())
                        .build());
            }
        }

        return TeamMentorHistoryResponse.builder()
                .teamId(teamId)
                .items(new ArrayList<>(byKey.values()))
                .build();
    }

    @Override
    public TeamResponse eliminateTeam(Integer teamId, EliminateTeamRequest req) {
        if (req == null || req.getReason() == null || req.getReason().isBlank()) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Lý do loại đội (DQ) bắt buộc không được để trống");
        }
        String reason = req.getReason().trim();

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
        if (team.getStatus() == TeamStatus.ELIMINATED) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Đội đã bị loại");
        }

        // Capture ADVANCED prelim seats BEFORE mutating TRT → ELIMINATED
        var teamTracks = teamRoundTrackRepository.findByTeamIdWithTrackAndRound(teamId);
        List<TeamDqBackfillService.AdvancedPrelimSeat> previouslyAdvanced = new ArrayList<>();
        for (com.sealhackathon.api.teams.entity.TeamRoundTrack trt : teamTracks) {
            if (trt.getParticipationStatus() != ParticipationStatus.ADVANCED) {
                continue;
            }
            if (trt.getTrack() == null || trt.getTrack().getRound() == null
                    || Boolean.TRUE.equals(trt.getTrack().getRound().getIsFinal())) {
                continue;
            }
            previouslyAdvanced.add(new TeamDqBackfillService.AdvancedPrelimSeat(trt));
        }

        team.setStatus(TeamStatus.ELIMINATED);
        team.setEliminatedAt(java.time.LocalDateTime.now());
        team.setEliminationReason(reason);
        Team saved = teamRepository.save(team);

        for (com.sealhackathon.api.teams.entity.TeamRoundTrack trt : teamTracks) {
            trt.setParticipationStatus(ParticipationStatus.ELIMINATED);
        }
        teamRoundTrackRepository.saveAll(teamTracks);

        auditService.log(AuditAction.TEAM_ELIMINATE_DQ,
                "teams", teamId, java.util.Map.of(
                        "reason", reason,
                        "previouslyAdvancedCount", previouslyAdvanced.size()));

        teamDqBackfillService.afterEliminate(saved, previouslyAdvanced, reason);
        return teamMapper.toResponse(saved);
    }

    // XỬ LÝ GOM ĐỘI CHO NGƯỜI CHƠ VƠ (GOD MODE CỦA COORDINATOR)
    @Override
    public TeamDetailResponse adminCreateTeam(AdminCreateTeamRequest req) {
        Hackathon hackathon = hackathonRepository.findByIdForUpdate(req.getHackathonId())
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", req.getHackathonId()));

        assertPrelimRoundNotActive(hackathon.getId());

        // RÀO CHẮN: quy mô đội theo cấu hình track của hackathon
        HackathonTeamSizeResolver.TeamSizeLimits createLimits = limitsForHackathon(hackathon.getId());
        int totalMembers = 1 + req.getMemberIds().size(); // 1 Leader + số lượng Members
        if (totalMembers < createLimits.minTeamSize() || totalMembers > createLimits.maxTeamSize()) {
            throw new BusinessRuleException(ErrorCode.TEAM_INVALID_MEMBER_COUNT,
                    "Vi phạm quy tắc: Ban Tổ Chức chỉ được phép tạo đội ép buộc khi gom đủ từ %d đến %d thành viên. Số lượng bạn đang chọn là: %d"
                            .formatted(createLimits.minTeamSize(), createLimits.maxTeamSize(), totalMembers));
        }

        if (teamRepository.existsByHackathon_IdAndTeamNameIgnoreCase(hackathon.getId(), req.getTeamName().trim())) {
            throw new ConflictException(ErrorCode.TEAM_NAME_DUPLICATE, "Tên đội đã tồn tại trong Hackathon này");
        }

        User leader = userRepository.findById(req.getLeaderId()).orElseThrow();
        if (teamMemberRepository.isUserInAnyActiveTeamForHackathon(leader.getId(), hackathon.getId())) {
            throw new ConflictException(ErrorCode.USER_IN_ANOTHER_TEAM, "Trưởng nhóm đã thuộc một đội khác.");
        }
        for (Integer memberId : req.getMemberIds()) {
            if (teamMemberRepository.isUserInAnyActiveTeamForHackathon(memberId, hackathon.getId())) {
                throw new ConflictException(ErrorCode.USER_IN_ANOTHER_TEAM,
                        "Thành viên ID " + memberId + " đã thuộc một đội khác.");
            }
        }

        // Sau đóng đăng ký: tạo ACTIVE đã khóa để không để ACTIVE unlocked chặn lottery
        boolean registrationClosed = HackathonRegistrationSupport.isRegistrationClosed(hackathon);
        Team team = Team.builder()
                .hackathon(hackathon)
                .teamName(req.getTeamName().trim())
                .leader(leader)
                .chapter(leader.getChapter())
                .status(TeamStatus.ACTIVE)
                .isLocked(registrationClosed)
                .lockedAt(registrationClosed ? LocalDateTime.now() : null)
                .build();
        Team savedTeam = teamRepository.save(team);

        // Chuẩn bị danh sách thành viên (bao gồm Leader và các Members)
        List<com.sealhackathon.api.teams.entity.TeamMember> newMembers = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. Thêm Leader
        newMembers.add(com.sealhackathon.api.teams.entity.TeamMember.builder()
                .id(new com.sealhackathon.api.teams.entity.TeamMemberId(savedTeam.getId(), leader.getId()))
                .team(savedTeam)
                .user(leader)
                .roleInTeam(com.sealhackathon.api.teams.value_object.TeamMemberRole.LEADER)
                .status(com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED) // ACCEPTED thẳng
                .joinedAt(now)
                .build());

        // 2. Thêm Members
        for (Integer memberId : req.getMemberIds()) {
            User memberUser = userRepository.findById(memberId).orElseThrow();
            newMembers.add(com.sealhackathon.api.teams.entity.TeamMember.builder()
                    .id(new com.sealhackathon.api.teams.entity.TeamMemberId(savedTeam.getId(), memberUser.getId()))
                    .team(savedTeam)
                    .user(memberUser)
                    .roleInTeam(com.sealhackathon.api.teams.value_object.TeamMemberRole.MEMBER)
                    .status(com.sealhackathon.api.teams.value_object.TeamMemberStatus.ACCEPTED) // ACCEPTED thẳng
                    .joinedAt(now)
                    .build());
        }

        // Re-validate ngay trước saveAll (race: 2 create team cùng orphan)
        if (teamMemberRepository.isUserInAnyActiveTeamForHackathon(leader.getId(), hackathon.getId())) {
            throw new ConflictException(ErrorCode.USER_IN_ANOTHER_TEAM, "Trưởng nhóm đã thuộc một đội khác.");
        }
        for (Integer memberId : req.getMemberIds()) {
            if (teamMemberRepository.isUserInAnyActiveTeamForHackathon(memberId, hackathon.getId())) {
                throw new ConflictException(ErrorCode.USER_IN_ANOTHER_TEAM,
                        "Thành viên ID " + memberId + " đã thuộc một đội khác.");
            }
        }

        try {
            teamMemberRepository.saveAll(newMembers);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException(ErrorCode.USER_IN_ANOTHER_TEAM,
                    "Thành viên đã thuộc một đội khác (ràng buộc DB).",
                    java.util.Map.of("hackathonId", hackathon.getId()));
        }

        auditService.log(com.sealhackathon.api.common.audit.AuditAction.TEAM_CREATE, "teams", savedTeam.getId(),
                java.util.Map.of("note", "Created by Administrator", "memberCount", newMembers.size()));

        return getTeam(savedTeam.getId());
    }

    // =========================================================================
    // MATCHMAKING VÀ GOD MODE (TÌM KIẾM & ÉP GỘP ĐỘI)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.sealhackathon.api.users.dto.response.UserSummaryResponse> getOrphanUsers(Integer hackathonId) {
        hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        List<HackathonRegistration> regs = hackathonRegistrationRepository.findAllByHackathon_Id(hackathonId);

        List<UserSummaryResponse> orphans = new java.util.ArrayList<>();

        for (HackathonRegistration reg : regs) {
            User u = reg.getUser();
            boolean hasTeam = teamMemberRepository.isUserInAnyActiveTeamForHackathon(u.getId(), hackathonId);
            if (!hasTeam) {
                orphans.add(UserSummaryResponse.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .role(u.getRole())
                        .status(u.getStatus())
                        .userType(u.getUserType())
                        .institution(u.getInstitution())
                        .build());
            }
        }
        return orphans;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamDetailResponse> getIncompleteTeams(Integer hackathonId) {
        // Lấy các đội PENDING
        List<Team> pendingTeams = teamRepository.findByHackathon_IdAndStatus(hackathonId, TeamStatus.PENDING);
        List<TeamDetailResponse> incompleteTeams = new ArrayList<>();

        for (Team t : pendingTeams) {
            long acceptedCount = teamMemberRepository.countByTeam_IdAndStatus(t.getId(), TeamMemberStatus.ACCEPTED);
            HackathonTeamSizeResolver.TeamSizeLimits limits = limitsFor(t);
            // Under-min OR over-max — both need Coordinator rescue (radar / merge / adjust).
            if (acceptedCount < limits.minTeamSize() || acceptedCount > limits.maxTeamSize()) {
                incompleteTeams.add(getTeam(t.getId()));
            }
        }
        return incompleteTeams;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamDetailResponse> getMatchmakingTeams(Integer hackathonId) {
        // Sinh viên cũng gọi hàm này để xem danh sách đội thiếu người và chủ động liên hệ Leader
        return getIncompleteTeams(hackathonId);
    }

    @Override
    public TeamDetailResponse adminAddMember(Integer teamId, AdminAddMemberRequest req) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));

        if (Boolean.TRUE.equals(team.getIsLocked())) {
            throw new BusinessRuleException(ErrorCode.TEAM_LOCKED, "Không thể thêm người vì đội đã bị khóa.");
        }

        User newMember = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getUserId()));

        // Rào chắn: Kiểm tra user có đang ở đội khác không
        boolean alreadyInTeam = teamMemberRepository.isUserInAnyActiveTeamForHackathon(newMember.getId(), team.getHackathon().getId());
        if (alreadyInTeam) {
            throw new ConflictException(ErrorCode.USER_IN_ANOTHER_TEAM, "Sinh viên này đã thuộc về một đội khác.");
        }

        // Rào chắn: Đội đã full chưa
        long acceptedCount = teamMemberRepository.countByTeam_IdAndStatus(teamId, TeamMemberStatus.ACCEPTED);
        HackathonTeamSizeResolver.TeamSizeLimits addLimits = limitsFor(team);
        if (acceptedCount >= addLimits.maxTeamSize()) {
            throw new BusinessRuleException(ErrorCode.TEAM_MEMBER_FULL,
                    "Đội này đã đủ %d thành viên.".formatted(addLimits.maxTeamSize()));
        }

        // Ép vào đội với trạng thái ACCEPTED (Bỏ qua luồng gửi Mail Invite)
        TeamMember member = TeamMember.builder()
                .id(new TeamMemberId(teamId, newMember.getId()))
                .team(team)
                .user(newMember)
                .roleInTeam(TeamMemberRole.MEMBER)
                .status(TeamMemberStatus.ACCEPTED)
                .joinedAt(LocalDateTime.now())
                .build();
        teamMemberRepository.save(member);

        auditService.log(AuditAction.TEAM_UPDATE, "teams", teamId,
                java.util.Map.of("note", "Admin ép thêm thành viên ID: " + req.getUserId()));

        return getTeam(teamId);
    }

    @Override
    public TeamDetailResponse adminMergeTeams(Integer targetTeamId, AdminMergeTeamsRequest req) {
        // 1. Lấy thông tin 2 Đội
        Team targetTeam = teamRepository.findById(targetTeamId)
                .orElseThrow(() -> new ResourceNotFoundException("Target Team", targetTeamId));
        Team sourceTeam = teamRepository.findById(req.getSourceTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Source Team", req.getSourceTeamId()));

        Hackathon hackathon = hackathonRepository.findByIdForUpdate(targetTeam.getHackathon().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", targetTeam.getHackathon().getId()));
        assertPrelimRoundNotActive(hackathon.getId());

        if (targetTeam.getId().equals(sourceTeam.getId())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Không thể tự gộp đội vào chính nó.");
        }
        if (!targetTeam.getHackathon().getId().equals(sourceTeam.getHackathon().getId())) {
            throw new BusinessRuleException(ErrorCode.INVALID_STATE, "Hai đội không thuộc cùng một Giải đấu.");
        }
        if (Boolean.TRUE.equals(targetTeam.getIsLocked()) || Boolean.TRUE.equals(sourceTeam.getIsLocked())) {
            throw new BusinessRuleException(ErrorCode.TEAM_LOCKED, "Một trong hai đội đã bị khóa, không thể gộp.");
        }

        // 2. Đếm số lượng thành viên thực tế của 2 đội
        List<TeamMember> targetMembers = teamMemberRepository.findByTeam_Id(targetTeam.getId());
        List<TeamMember> sourceMembers = teamMemberRepository.findByTeam_Id(sourceTeam.getId());

        long targetCount = targetMembers.stream().filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED).count();
        long sourceCount = sourceMembers.stream().filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED).count();

        HackathonTeamSizeResolver.TeamSizeLimits mergeLimits = limitsFor(targetTeam);

        // 3. RÀO CHẮN: Đảm bảo tổng số người sau khi gộp không vượt quá max
        long totalAfterMerge = targetCount + sourceCount;
        if (totalAfterMerge > mergeLimits.maxTeamSize()) {
            throw new BusinessRuleException(ErrorCode.TEAM_MEMBER_FULL,
                    "Gộp thất bại: Tổng số thành viên của 2 đội là " + totalAfterMerge
                            + " (Vượt quá quy định tối đa " + mergeLimits.maxTeamSize() + " thành viên).");
        }

        // 4. Bế thành viên từ Source Team sang Target Team
        for (TeamMember sm : sourceMembers) {
            if (sm.getStatus() == TeamMemberStatus.ACCEPTED) {
                // Xóa tư cách thành viên ở đội cũ
                teamMemberRepository.delete(sm);
                teamMemberRepository.flush(); // Đẩy lệnh xóa xuống DB ngay lập tức để tránh lỗi Trùng lặp Khóa chính

                // Cấp tư cách thành viên ở đội mới (Tất cả những người bị chuyển sang đều mang role MEMBER)
                TeamMember newMember = TeamMember.builder()
                        .id(new TeamMemberId(targetTeam.getId(), sm.getUser().getId()))
                        .team(targetTeam)
                        .user(sm.getUser())
                        .roleInTeam(TeamMemberRole.MEMBER)
                        .status(TeamMemberStatus.ACCEPTED)
                        .joinedAt(LocalDateTime.now())
                        .build();
                teamMemberRepository.save(newMember);
            }
        }

        // 5. Đánh giá lại Đội Đích (Target Team): đủ tối thiểu thì kích hoạt ACTIVE
        if (totalAfterMerge >= mergeLimits.minTeamSize()) {
            targetTeam.setStatus(TeamStatus.ACTIVE);
            lockIfRegistrationClosed(targetTeam);
            teamRepository.save(targetTeam);
        }

        // 6. Xóa sổ/Giải tán Đội Nguồn (Source Team)
        sourceTeam.setStatus(TeamStatus.REJECTED);
        sourceTeam.setRejectionReason("Đã được BTC gộp vào đội: " + targetTeam.getTeamName());
        teamRepository.save(sourceTeam);
        teamMembershipReleaseService.releaseMembers(sourceTeam,
                "Đã được BTC gộp vào đội: " + targetTeam.getTeamName(), false);

        auditService.log(AuditAction.TEAM_UPDATE, "teams", targetTeam.getId(),
                java.util.Map.of("note", "Đã gộp đội ID " + sourceTeam.getId() + " vào đội này."));

        return getTeam(targetTeam.getId());
    }
}