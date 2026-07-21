package com.sealhackathon.api.hackathons.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.hackathons.entity.HackathonRegistration;
import com.sealhackathon.api.hackathons.entity.HackathonRegistrationWithdrawal;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationWithdrawalRepository;
import com.sealhackathon.api.hackathons.dto.request.CloseRegistrationEarlyRequest;
import com.sealhackathon.api.hackathons.dto.response.CloseRegistrationEarlyResponse;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.service.CompetitionScheduleAdjustService;
import com.sealhackathon.api.hackathons.service.HackathonRegistrationCloseService;
import com.sealhackathon.api.hackathons.support.HackathonRegistrationSupport;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.support.HackathonTeamSizeResolver;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HackathonRegistrationCloseServiceImpl implements HackathonRegistrationCloseService {

    private static final int FORMATION_GRACE_HOURS = 24;

    private final HackathonRepository hackathonRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final HackathonRegistrationWithdrawalRepository hackathonRegistrationWithdrawalRepository;
    private final HackathonTeamSizeResolver teamSizeResolver;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final RoundRepository roundRepository;
    private final CompetitionScheduleAdjustService competitionScheduleAdjustService;

    @Override
    @Transactional
    public CloseRegistrationEarlyResponse closeRegistrationEarly(Integer hackathonId,
                                                                 CloseRegistrationEarlyRequest request) {
        if (request == null || request.getNewPrelimExamAt() == null) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_FAILED,
                    "Cần chọn giờ thi Sơ loại (newPrelimExamAt) khi kết thúc đăng ký sớm — hệ thống sẽ cascade Workshop/Kickoff/Chung kết/Awards.");
        }

        Hackathon hackathon = hackathonRepository.findByIdForUpdate(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

        if (hackathon.getStatus() != HackathonStatus.ONGOING) {
            throw new BusinessRuleException(ErrorCode.HACKATHON_NOT_ONGOING,
                    "Chỉ kết thúc đăng ký sớm khi Hackathon đang ONGOING.");
        }
        if (HackathonRegistrationSupport.isRegistrationClosed(hackathon)) {
            throw new BusinessRuleException(ErrorCode.REGISTRATION_ALREADY_CLOSED,
                    "Đăng ký đã kết thúc (hết hạn hoặc đã đóng sớm) — không thể kết thúc sớm nữa.");
        }
        if (hackathon.getScheduleAdjustedAt() != null) {
            throw new BusinessRuleException(ErrorCode.SCHEDULE_ALREADY_ADJUSTED,
                    "Lịch thi đã được dời — không thể đóng ĐK sớm kèm dời lịch lần nữa.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        hackathon.setRegistrationClosedEarlyAt(now);
        if (hackathon.getRegistrationEnd() == null || hackathon.getRegistrationEnd().isAfter(today)) {
            hackathon.setRegistrationEnd(today);
        }
        hackathonRepository.save(hackathon);

        int lockedActiveTeams = 0;
        int rejectedIncompleteTeams = 0;
        int withdrawnOrphans = 0;
        List<CloseRegistrationEarlyResponse.TeamAwaitingCoordinatorApprovalItem> awaitingApproval = new ArrayList<>();
        List<CloseRegistrationEarlyResponse.TeamInFormationGraceItem> inGracePeriod = new ArrayList<>();

        List<Team> allTeams = teamRepository.findByHackathon_Id(hackathonId);

        for (Team team : allTeams) {
            if (team.getStatus() == TeamStatus.ACTIVE && !Boolean.TRUE.equals(team.getIsLocked())) {
                team.setIsLocked(true);
                team.setLockedAt(now);
                teamRepository.save(team);
                lockedActiveTeams++;
                auditService.log(AuditAction.TEAM_LOCKED, "teams", team.getId(),
                        Map.of("hackathonId", hackathonId, "reason", "REGISTRATION_CLOSED_EARLY"));
            }
        }

        for (Team team : allTeams) {
            if (team.getStatus() != TeamStatus.PENDING) {
                continue;
            }

            long acceptedCount = teamMemberRepository.countByTeam_IdAndStatus(team.getId(), TeamMemberStatus.ACCEPTED);
            HackathonTeamSizeResolver.TeamSizeLimits limits = teamSizeResolver.forTeam(team);
            boolean inRange = acceptedCount >= limits.minTeamSize() && acceptedCount <= limits.maxTeamSize();

            if (!inRange) {
                rejectTeamAndWithdrawMembers(team,
                        "Kết thúc đăng ký sớm: đội không đủ điều kiện (%d thành viên, yêu cầu %d-%d)."
                                .formatted(acceptedCount, limits.minTeamSize(), limits.maxTeamSize()));
                rejectedIncompleteTeams++;
                continue;
            }

            if (team.getFormationSubmittedAt() != null) {
                long pendingInvites = teamMemberRepository.countByTeam_IdAndStatus(
                        team.getId(), TeamMemberStatus.PENDING);
                if (pendingInvites > 0) {
                    continue;
                }
                boolean hasUnapprovedMember = teamMemberRepository.findByTeam_Id(team.getId()).stream()
                        .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED)
                        .anyMatch(m -> m.getUser().getStatus() != UserStatus.APPROVED);
                if (hasUnapprovedMember) {
                    continue;
                }
                awaitingApproval.add(CloseRegistrationEarlyResponse.TeamAwaitingCoordinatorApprovalItem.builder()
                        .teamId(team.getId())
                        .teamName(team.getTeamName())
                        .leaderName(team.getLeader().getFullName())
                        .acceptedMemberCount((int) acceptedCount)
                        .build());
                continue;
            }

            LocalDateTime graceDeadline = now.plusHours(FORMATION_GRACE_HOURS);
            team.setFormationGraceDeadlineAt(graceDeadline);
            teamRepository.save(team);

            inGracePeriod.add(CloseRegistrationEarlyResponse.TeamInFormationGraceItem.builder()
                    .teamId(team.getId())
                    .teamName(team.getTeamName())
                    .leaderName(team.getLeader().getFullName())
                    .acceptedMemberCount((int) acceptedCount)
                    .formationGraceDeadlineAt(graceDeadline)
                    .build());

            notifyTeamMembersOfGracePeriod(team, graceDeadline);
        }

        List<HackathonRegistration> registrations =
                hackathonRegistrationRepository.findAllByHackathon_Id(hackathonId);
        for (HackathonRegistration registration : registrations) {
            User user = registration.getUser();
            if (!teamMemberRepository.isUserInAnyActiveTeamForHackathon(user.getId(), hackathonId)) {
                withdrawRegistration(hackathon, user);
                withdrawnOrphans++;
            }
        }

        if (!awaitingApproval.isEmpty()) {
            notifyCoordinatorsOfPendingApproval(hackathon, awaitingApproval.size());
        }

        // Dời lịch theo ngày Coord chọn + cascade WS/KO/CK/Awards (1 lần); không đụng lottery
        Map<String, Object> timelineMeta =
                competitionScheduleAdjustService.apply(
                        hackathon, request.getNewPrelimExamAt(), true, request.getOverrides());

        Map<String, Object> auditPayload = new java.util.LinkedHashMap<>();
        auditPayload.put("lockedActiveTeams", lockedActiveTeams);
        auditPayload.put("rejectedIncompleteTeams", rejectedIncompleteTeams);
        auditPayload.put("withdrawnOrphans", withdrawnOrphans);
        auditPayload.put("teamsAwaitingApproval", awaitingApproval.size());
        auditPayload.put("teamsInFormationGrace", inGracePeriod.size());
        auditPayload.putAll(timelineMeta);
        auditService.log(AuditAction.HACKATHON_REGISTRATION_CLOSED_EARLY, "hackathons", hackathonId, auditPayload);

        LocalDateTime prelimExamAt = roundRepository.findByHackathon_IdOrderByExamAtAsc(hackathonId).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsFinal()))
                .map(Round::getExamAt)
                .filter(at -> at != null)
                .findFirst()
                .orElse(null);
        Long hoursUntilPrelimExam = null;
        if (prelimExamAt != null && prelimExamAt.isAfter(now)) {
            hoursUntilPrelimExam = ChronoUnit.HOURS.between(now, prelimExamAt);
        }

        Boolean timelineCompressed = timelineMeta.containsKey("timelineCompressed")
                ? Boolean.TRUE.equals(timelineMeta.get("timelineCompressed"))
                : null;

        return CloseRegistrationEarlyResponse.builder()
                .hackathonId(hackathonId)
                .closedAt(now)
                .lockedActiveTeams(lockedActiveTeams)
                .withdrawnOrphans(withdrawnOrphans)
                .rejectedIncompleteTeams(rejectedIncompleteTeams)
                .teamsAwaitingCoordinatorApproval(awaitingApproval)
                .teamsInFormationGracePeriod(inGracePeriod)
                .prelimExamAt(prelimExamAt)
                .hoursUntilPrelimExam(hoursUntilPrelimExam)
                .timelineCompressed(timelineCompressed)
                .build();
    }

    private void notifyCoordinatorsOfPendingApproval(Hackathon hackathon, int teamCount) {
        List<User> coordinators = userRepository
                .findByRoleAndStatus(UserRole.COORDINATOR, UserStatus.APPROVED, Pageable.unpaged())
                .getContent();
        if (coordinators.isEmpty()) {
            return;
        }
        notificationService.sendBatch(
                coordinators,
                "TEAM_AWAITING_APPROVAL",
                "Có đội chờ duyệt tham gia",
                "Hackathon \"" + hackathon.getName() + "\" đã kết thúc đăng ký sớm. "
                        + "Có " + teamCount + " đội đã xác nhận thành lập và đang chờ bạn duyệt.",
                "hackathons",
                hackathon.getId());
    }

    private void notifyTeamMembersOfGracePeriod(Team team, LocalDateTime graceDeadline) {
        List<User> recipients = teamMemberRepository.findByTeam_Id(team.getId()).stream()
                .filter(m -> m.getStatus() == TeamMemberStatus.ACCEPTED)
                .map(TeamMember::getUser)
                .toList();
        if (recipients.isEmpty()) {
            return;
        }
        notificationService.sendBatch(
                recipients,
                "TEAM_FORMATION_GRACE",
                "Khẩn: 24h để xác nhận thành lập đội",
                "Hackathon đã kết thúc đăng ký sớm. Đội " + team.getTeamName()
                        + " có 24 giờ (đến " + graceDeadline.toLocalDate() + " "
                        + String.format("%02d:%02d", graceDeadline.getHour(), graceDeadline.getMinute())
                        + ") để trưởng nhóm xác nhận thành lập. "
                        + "Nếu không xác nhận, CẢ ĐỘI sẽ tự động bị loại và mất suất thi. "
                        + "Thành viên hãy nhắc trưởng nhóm xác nhận ngay trên trang Quản lý đội.",
                "teams",
                team.getId());
    }

    private void rejectTeamAndWithdrawMembers(Team team, String reason) {
        team.setStatus(TeamStatus.REJECTED);
        team.setRejectionReason(reason);
        team.setFormationGraceDeadlineAt(null);
        teamRepository.save(team);
        auditService.log(AuditAction.TEAM_REJECT, "teams", team.getId(), Map.of("reason", reason));

        List<TeamMember> members = teamMemberRepository.findByTeam_Id(team.getId());
        LocalDateTime now = LocalDateTime.now();
        for (TeamMember member : members) {
            if (member.getStatus() == TeamMemberStatus.ACCEPTED) {
                withdrawRegistration(team.getHackathon(), member.getUser());
            }
            member.setStatus(TeamMemberStatus.LEFT);
            member.setLeftAt(now);
        }
        teamMemberRepository.saveAll(members);
    }

    private void withdrawRegistration(Hackathon hackathon, User user) {
        if (!hackathonRegistrationWithdrawalRepository.existsByHackathon_IdAndUser_Id(
                hackathon.getId(), user.getId())) {
            hackathonRegistrationWithdrawalRepository.save(HackathonRegistrationWithdrawal.builder()
                    .hackathon(hackathon)
                    .user(user)
                    .build());
        }
        if (hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(hackathon.getId(), user.getId())) {
            hackathonRegistrationRepository.deleteByHackathon_IdAndUser_Id(hackathon.getId(), user.getId());
        }
    }
}
