package com.sealhackathon.api.showcase.job;

import com.sealhackathon.api.showcase.service.HallOfFameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Optional one-shot backfill: set {@code app.showcase.backfill-on-startup=true}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(HallOfFameService.class)
@ConditionalOnProperty(name = "app.showcase.backfill-on-startup", havingValue = "true")
public class HallOfFameBackfillRunner implements ApplicationRunner {

    private final HallOfFameService hallOfFameService;

    @Override
    public void run(ApplicationArguments args) {
        int created = hallOfFameService.backfillFinishedHackathons();
        log.info("[showcase] Startup HallOfFame backfill created={}", created);
    }
}
