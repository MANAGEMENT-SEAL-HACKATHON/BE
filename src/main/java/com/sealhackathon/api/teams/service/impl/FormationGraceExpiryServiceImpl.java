package com.sealhackathon.api.teams.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationWithdrawalRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.service.FormationGraceExpiryService;
import com.sealhackathon.api.teams.value_object.TeamStatus;
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
public class FormationGraceExpiryServiceImpl implements FormationGraceExpiryService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final HackathonRegistrationWithdrawalRepository hackathonRegistrationWithdrawalRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Override
    @Transactional
    public int expireOverdueGraceTeams() {
        LocalDateTime now = LocalDateTime.now();
        List<Team> overdueTeams = teamRepository
                .findByStatusAndFormationSubmittedAtIsNullAndFormationGraceDeadlineAtIsNotNullAndFormationGraceDeadlineAtBefore(
                        TeamStatus.PENDING, now);

        int expired = 0;
        for (Team team : overdueTeams) {
            expireTeam(team, now);
            expired++;
        }
        return expired;
    }

    private void expireTeam(Team team, LocalDateTime now) {
        String reason = "Hết thời hạn 24h xác nhận thành lập đội sau khi kết thúc đăng ký sớm.";
        team.setStatus(TeamStatus.REJECTED);
        team.setRejectionReason(reason);
        team.setFormationGraceDeadlineAt(null);
        teamRepository.save(team);
        auditService.log(AuditAction.TEAM_REJECT, "teams", team.getId(), Map.of("reason", reason));

        List<User> membersToNotify = new ArrayList<>();
        List<TeamMember> members = teamMemberRepository.findByTeam_Id(team.getId());
        for (TeamMember member : members) {
            if (member.getStatus() == TeamMemberStatus.ACCEPTED) {
                withdrawRegistration(team.getHackathon(), member.getUser());
                membersToNotify.add(member.getUser());
            }
            member.setStatus(TeamMemberStatus.LEFT);
            member.setLeftAt(now);
        }
        teamMemberRepository.saveAll(members);

        if (!membersToNotify.isEmpty()) {
            notificationService.sendBatch(
                    membersToNotify,
                    "TEAM_FORMATION_GRACE_EXPIRED",
                    "Đội không còn tham gia sự kiện",
                    "Đội " + team.getTeamName() + " không xác nhận thành lập trong 24h. "
                            + "Bạn đã bị loại khỏi hackathon này.",
                    "teams",
                    team.getId());
        }
    }

    private void withdrawRegistration(Hackathon hackathon, User user) {
        if (!hackathonRegistrationWithdrawalRepository.existsByHackathon_IdAndUser_Id(
                hackathon.getId(), user.getId())) {
            hackathonRegistrationWithdrawalRepository.save(
                    com.sealhackathon.api.hackathons.entity.HackathonRegistrationWithdrawal.builder()
                            .hackathon(hackathon)
                            .user(user)
                            .build());
        }
        if (hackathonRegistrationRepository.existsByHackathon_IdAndUser_Id(hackathon.getId(), user.getId())) {
            hackathonRegistrationRepository.deleteByHackathon_IdAndUser_Id(hackathon.getId(), user.getId());
        }
    }
}
