package com.sealhackathon.api.submissions.job;

import com.sealhackathon.api.submissions.service.SubmissionMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** FR-17 — process PENDING submission_metadata rows (GitHub API). */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.github.enabled", havingValue = "true")
public class SubmissionMetadataScheduler {

    private final SubmissionMetadataService submissionMetadataService;

    @Scheduled(cron = "${app.github.scheduler.cron:0 */1 * * * *}")
    public void processPending() {
        int n = submissionMetadataService.processPendingBatch(20);
        if (n > 0) {
            log.info("[FR-17] Processed {} pending GitHub metadata rows", n);
        }
    }
}
