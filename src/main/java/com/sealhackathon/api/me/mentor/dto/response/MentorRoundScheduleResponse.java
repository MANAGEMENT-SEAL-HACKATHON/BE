package com.sealhackathon.api.me.mentor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** FR-M-16 — Lịch Chung kết (passive, read-only). */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorRoundScheduleResponse {

    private Integer roundId;
    private String roundName;
    private List<ScheduleSlot> slots;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScheduleSlot {
        private Integer teamId;
        private String teamName;
        private LocalDateTime startAt;
        private LocalDateTime endAt;
    }
}
