package com.sealhackathon.api.teams.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.value_object.HackathonStatus;
import com.sealhackathon.api.teams.entity.Team;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.service.TeamLockService;
import com.sealhackathon.api.teams.value_object.TeamStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sealhackathon.api.hackathons.support.HackathonRegistrationSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TeamLockServiceImpl implements TeamLockService {

    private final HackathonRepository hackathonRepository;
    private final TeamRepository teamRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public int lockTeamsAfterRegistrationEnd() {
        int lockedCount = 0;

        // 1. Tìm các Hackathon đang ONGOING và đã kết thúc giai đoạn đăng ký
        List<Hackathon> ongoingHackathons = hackathonRepository.findAll().stream()
                .filter(h -> h.getStatus() == HackathonStatus.ONGOING)
                .filter(HackathonRegistrationSupport::isRegistrationPeriodEnded)
                .toList();

        // 2. Khóa các đội ACTIVE chưa bị khóa
        for (Hackathon h : ongoingHackathons) {
            List<Team> teamsToLock = teamRepository.findByHackathon_Id(h.getId()).stream()
                    .filter(t -> !Boolean.TRUE.equals(t.getIsLocked()))
                    .filter(t -> t.getStatus() == TeamStatus.ACTIVE)
                    .toList();

            for (Team team : teamsToLock) {
                team.setIsLocked(true);
                team.setLockedAt(LocalDateTime.now());
                teamRepository.save(team);
                lockedCount++;

                // Actor ID = 1 (SYSTEM)
                auditService.logAs(1, AuditAction.TEAM_LOCKED, "teams", team.getId(),
                        Map.of("hackathonId", h.getId()));
            }
        }

        return lockedCount;
    }
}
