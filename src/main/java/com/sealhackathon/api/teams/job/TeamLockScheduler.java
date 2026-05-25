package com.sealhackathon.api.teams.job;

import com.sealhackathon.api.teams.service.TeamLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FR-13A — Cron khóa thành viên sau deadline đăng ký.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.team-lock.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class TeamLockScheduler {

    private final TeamLockService teamLockService;

    /** Mỗi phút — idempotent. */
    @Scheduled(cron = "${app.team-lock.scheduler.cron:0 * * * * *}")
    public void runLockJob() {
        int updated = teamLockService.lockTeamsAfterRegistrationEnd();
        if (updated > 0) {
            log.info("Team lock job: locked {} teams", updated);
        }
    }
}
