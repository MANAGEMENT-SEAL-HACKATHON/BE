package com.sealhackathon.api.teams.job;

import com.sealhackathon.api.teams.service.FormationGraceExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Tự động loại đội hết hạn 24h grace mà leader chưa xác nhận thành lập. */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.formation-grace.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class FormationGraceExpiryScheduler {

    private final FormationGraceExpiryService formationGraceExpiryService;

    @Scheduled(cron = "${app.formation-grace.scheduler.cron:0 */5 * * * *}")
    public void runGraceExpiryJob() {
        int expired = formationGraceExpiryService.expireOverdueGraceTeams();
        if (expired > 0) {
            log.info("Formation grace expiry job: removed {} teams", expired);
        }
    }
}
