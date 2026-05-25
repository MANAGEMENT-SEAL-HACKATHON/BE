package com.sealhackathon.api.teams.service.impl;

import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.teams.repository.TeamRepository;
import com.sealhackathon.api.teams.service.TeamLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // TODO FR-13A: foreach hackathon ONGOING where registration_end < today
        // TODO: UPDATE teams SET is_locked=TRUE WHERE status=ACTIVE AND is_locked=FALSE
        // TODO: audit TEAM_LOCKED per team or batch; return count updated
        log.debug("TODO: lockTeamsAfterRegistrationEnd not implemented");
        return 0;
    }
}
