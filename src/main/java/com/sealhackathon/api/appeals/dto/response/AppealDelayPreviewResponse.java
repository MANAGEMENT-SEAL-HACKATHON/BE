package com.sealhackathon.api.appeals.dto.response;

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
public class AppealDelayPreviewResponse {

    private LocalDateTime currentFinalExamAt;
    private LocalDateTime newFinalExamAt;
    private int requestedMinutes;
    private int delayMinutesRemaining;
    private int delayMinutesApplied;
    private List<String> consequences;
}
