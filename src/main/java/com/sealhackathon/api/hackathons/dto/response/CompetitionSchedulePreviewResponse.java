package com.sealhackathon.api.hackathons.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CompetitionSchedulePreviewResponse {

    private final LocalDateTime newPrelimExamAt;
    private final boolean alreadyAdjusted;
    private final boolean canAdjust;
    private final String blockReason;
    private final List<ScheduleChangeItem> changes;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ScheduleChangeItem {
        /** WORKSHOP | KICKOFF | PRELIM | FINAL | AWARDS | EVENT_START | EVENT_END | TRACK_SLOTS */
        private final String key;
        private final String label;
        private final String oldValue;
        private final String newValue;
    }
}
