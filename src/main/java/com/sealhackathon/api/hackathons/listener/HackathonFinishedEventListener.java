package com.sealhackathon.api.hackathons.listener;

import com.sealhackathon.api.chapters.service.ChapterRankingService;
import com.sealhackathon.api.hackathons.event.HackathonFinishedEvent;
import com.sealhackathon.api.individual_rankings.service.IndividualRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** FR-33 async — tính XH Chapter/Cá nhân sau confirm FINISHED. */
@Component
@Slf4j
@RequiredArgsConstructor
public class HackathonFinishedEventListener {

    private final ChapterRankingService chapterRankingService;
    private final IndividualRankingService individualRankingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onHackathonFinished(HackathonFinishedEvent event) {
        Integer hackathonId = event.getHackathonId();
        log.info("[GĐ6] HackathonFinishedEvent hackathonId={} — tính chapter/individual rankings", hackathonId);
        chapterRankingService.calculateAsync(hackathonId);
        individualRankingService.calculateAsync(hackathonId);
        // TODO: FR-33 — RESULT_PUBLISHED notification batch
    }
}
