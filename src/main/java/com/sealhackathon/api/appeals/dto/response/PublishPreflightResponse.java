package com.sealhackathon.api.appeals.dto.response;

import com.sealhackathon.api.appeals.value_object.AppealWindowMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishPreflightResponse {

    private LocalDateTime serverNow;
    private LocalDateTime finalExamAt;
    private Integer configuredWindowMinutes;
    private Long remainingMinutes;
    private boolean fits;
    private List<ModeAvailability> availableModes;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModeAvailability {
        private AppealWindowMode mode;
        private boolean available;
        private String blockedReason;
        private Integer suggestedDelayMinutes;
    }
}
