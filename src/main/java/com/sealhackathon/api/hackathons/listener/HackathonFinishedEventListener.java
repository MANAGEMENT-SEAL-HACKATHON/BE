package com.sealhackathon.api.hackathons.listener;

import com.sealhackathon.api.chapter_rankings.service.ChapterRankingService;
import com.sealhackathon.api.hackathons.event.HackathonFinishedEvent;
import com.sealhackathon.api.individual_rankings.service.IndividualRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** FR-33 async — enqueue workers sau confirm FINISHED. */
@Component
@Slf4j
@RequiredArgsConstructor
public class HackathonFinishedEventListener {

    private final ChapterRankingService chapterRankingService;
    private final IndividualRankingService individualRankingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onHackathonFinished(HackathonFinishedEvent event) {
        // TODO: FR-33 — RESULT_PUBLISHED notification batch
        // TODO: FR-33B — chapterRankingService.calculateAsync(event.getHackathonId())
        // TODO: FR-33C — individualRankingService.calculateAsync(...) if individual_ranking_enabled
        log.debug("[GĐ6 stub] HackathonFinishedEvent hackathonId={}", event.getHackathonId());
    }
}
