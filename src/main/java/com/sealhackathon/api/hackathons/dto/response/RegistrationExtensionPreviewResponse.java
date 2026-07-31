package com.sealhackathon.api.hackathons.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RegistrationExtensionPreviewResponse {

    private final LocalDateTime currentEnd;
    private final LocalDateTime newEnd;
    private final int extensionCount;
    private final int maxExtensions;
    private final TeamStats teamStats;
    private final List<MilestoneItem> milestones;
    private final boolean canExtend;
    private final String blockReason;
    private final List<String> suggestedAdjustments;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TeamStats {
        private final long activeCount;
        private final long lockedCount;
        private final long pendingCount;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MilestoneItem {
        /** WORKSHOP | KICKOFF | PRELIM | EVENT_START */
        private final String key;
        private final String label;
        private final LocalDate date;
        private final Long daysFromNewRegEnd;
        /** OK | TIGHT | VIOLATION */
        private final String status;
    }
}
