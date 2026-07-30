package com.sealhackathon.api.showcase.listener;

import com.sealhackathon.api.hackathons.event.HackathonFinishedEvent;
import com.sealhackathon.api.showcase.service.HallOfFameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Snapshot Hall of Fame after FINISHED — does not call HackathonArchiveGuard.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.showcase.enabled", havingValue = "true", matchIfMissing = true)
public class HallOfFameFinishedListener {

    private final HallOfFameService hallOfFameService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onHackathonFinished(HackathonFinishedEvent event) {
        Integer hackathonId = event.getHackathonId();
        log.info("[showcase] HackathonFinishedEvent → HallOfFame snapshot hackathonId={}", hackathonId);
        hallOfFameService.snapshotFromFinishedHackathon(hackathonId);
    }
}
