package com.sealhackathon.api.teams.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.hackathons.entity.HackathonRegistration;
import com.sealhackathon.api.hackathons.entity.HackathonRegistrationWithdrawal;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationRepository;
import com.sealhackathon.api.hackathons.repository.HackathonRegistrationWithdrawalRepository;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.support.HackathonRegistrationSupport;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.teams.entity.TeamMember;
import com.sealhackathon.api.teams.repository.TeamMemberRepository;
import com.sealhackathon.api.teams.value_object.TeamMemberStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.service.TeamLockService;
import com.sealhackathon.api.teams.support.HackathonTeamSizeResolver;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import com.sealhackathon.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeamLockServiceImpl implements TeamLockService {

    private final HackathonRepository hackathonRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final HackathonRegistrationRepository hackathonRegistrationRepository;
    private final HackathonRegistrationWithdrawalRepository hackathonRegistrationWithdrawalRepository;
    private final HackathonTeamSizeResolver teamSizeResolver;
    private final AuditService auditService;

    @Override
    @Transactional
    public int lockTeamsAfterRegistrationEnd() {
        int lockedCount = 0;

        List<Hackathon> ongoingHackathons = hackathonRepository.findAll().stream()
                .filter(h -> h.getStatus() == HackathonStatus.ONGOING)
                .filter(HackathonRegistrationSupport::isRegistrationPeriodEnded)
                .toList();

        for (Hackathon h : ongoingHackathons) {
            List<Team> allTeams = teamRepository.findByHackathon_Id(h.getId());

            for (Team team : allTeams) {
                if (!Boolean.TRUE.equals(team.getIsLocked()) && team.getStatus() == TeamStatus.ACTIVE) {
                    team.setIsLocked(true);
                    team.setLockedAt(LocalDateTime.now());
                    teamRepository.save(team);
                    lockedCount++;
                    auditService.logAs(1, AuditAction.TEAM_LOCKED, "teams", team.getId(),
                            Map.of("hackathonId", h.getId(), "reason", "REGISTRATION_ENDED"));
                }
            }

            rejectPendingOutOfRangeTeams(h, allTeams);
        }

        return lockedCount;
    }

    private void rejectPendingOutOfRangeTeams(Hackathon hackathon, List<Team> allTeams) {
        for (Team team : allTeams) {
            if (team.getStatus() != TeamStatus.PENDING) {
                continue;
            }
            long acceptedCount = teamMemberRepository.countByTeam_IdAndStatus(team.getId(), TeamMemberStatus.ACCEPTED);
            HackathonTeamSizeResolver.TeamSizeLimits limits = teamSizeResolver.forTeam(team);
            boolean inRange = acceptedCount >= limits.minTeamSize() && acceptedCount <= limits.maxTeamSize();
            if (!inRange) {
                rejectTeamAndWithdrawMembers(team,
                        "Hết hạn đăng ký: đội không đủ điều kiện (%d thành viên, yêu cầu %d-%d)."
                                .formatted(acceptedCount, limits.minTeamSize(), limits.maxTeamSize()));
            }
        }

        List<HackathonRegistration> registrations =
                hackathonRegistrationRepository.findAllByHackathon_Id(hackathon.getId());
        for (HackathonRegistration registration : registrations) {
            User user = registration.getUser();
            if (!teamMemberRepository.isUserInAnyActiveTeamForHackathon(user.getId(), hackathon.getId())) {
                withdrawRegistration(hackathon, user);
            }
        }
    }

    private void rejectTeamAndWithdrawMembers(Team team, String reason) {
        team.setStatus(TeamStatus.REJECTED);
        team.setRejectionReason(reason);
        team.setFormationGraceDeadlineAt(null);
        teamRepository.save(team);
        auditService.logAs(1, AuditAction.TEAM_REJECT, "teams", team.getId(), Map.of("reason", reason));

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
