package com.sealhackathon.api.appeals.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppealWindowStatusResponse {

    private LocalDateTime serverNow;
    private LocalDateTime appealWindowEndsAt;
    private LocalDateTime finalExamAt;
    private long pendingCount;
    private long underReviewCount;
    private int delayMinutesRemaining;
    /** OPEN | CLOSED | SKIPPED | NOT_CONFIGURED | EXPIRED */
    private String windowState;
    private Integer publishRevision;
    private LocalDateTime resultsRevisedAt;
}
