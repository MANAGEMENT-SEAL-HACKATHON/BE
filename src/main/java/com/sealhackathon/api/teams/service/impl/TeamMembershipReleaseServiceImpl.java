package com.sealhackathon.api.teams.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.entity.HackathonRegistrationWithdrawal;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationWithdrawalRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.service.TeamMembershipReleaseService;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamMembershipReleaseServiceImpl implements TeamMembershipReleaseService {

    private final TeamMemberRepository teamMemberRepository;
    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final HackathonRegistrationWithdrawalRepository hackathonRegistrationWithdrawalRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Override
    public void releaseMembers(Team team, String reason, boolean withdrawRegistration) {
        List<TeamMember> members = teamMemberRepository.findByTeam_Id(team.getId());
        LocalDateTime now = LocalDateTime.now();
        List<User> releasedUsers = new ArrayList<>();

        for (TeamMember member : members) {
            if (member.getStatus() == TeamMemberStatus.PENDING) {
                member.setStatus(TeamMemberStatus.REJECTED);
                continue;
            }
            if (member.getStatus() == TeamMemberStatus.ACCEPTED) {
                if (withdrawRegistration) {
                    withdrawRegistration(team.getHackathon(), member.getUser());
                }
                member.setStatus(TeamMemberStatus.LEFT);
                member.setLeftAt(now);
                releasedUsers.add(member.getUser());
            }
        }
        teamMemberRepository.saveAll(members);

        if (!releasedUsers.isEmpty()) {
            String body = "Đội \"" + team.getTeamName() + "\" đã bị "
                    + (reason != null && reason.contains("giải tán") ? "giải tán" : "từ chối")
                    + (reason != null && !reason.isBlank() ? ": " + reason : ".")
                    + " Bạn có thể tạo đội mới hoặc tham gia đội khác.";
            notificationService.sendBatch(
                    releasedUsers,
                    "TEAM_RELEASED",
                    "Đội của bạn đã kết thúc",
                    body,
                    "teams",
                    team.getId());
        }

        auditService.log(
                AuditAction.TEAM_MEMBERS_RELEASED,
                "teams",
                team.getId(),
                Map.of("reason", reason != null ? reason : "", "releasedCount", releasedUsers.size()));
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
