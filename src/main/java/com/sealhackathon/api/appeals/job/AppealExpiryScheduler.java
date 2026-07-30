package com.sealhackathon.api.appeals.job;

import com.sealhackathon.api.appeals.service.AppealWindowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mỗi phút: chuyển đơn PENDING/UNDER_REVIEW sang EXPIRED khi cửa sổ đã đóng.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.appeal-expiry.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class AppealExpiryScheduler {

    private final AppealWindowService appealWindowService;

    @Scheduled(cron = "${app.appeal-expiry.scheduler.cron:0 * * * * *}")
    @Transactional
    public void expireAppeals() {
        int expired = appealWindowService.expireAllDueAppeals();
        if (expired > 0) {
            log.info("[AppealExpiry] expired {} appeal(s)", expired);
        }
    }
}
