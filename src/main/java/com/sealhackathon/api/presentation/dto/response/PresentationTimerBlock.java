package com.sealhackathon.api.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationTimerBlock {

    private String phase;
    private Integer presentationMinutes;
    private Integer qaMinutes;
    private LocalDateTime presentationStartedAt;
    private LocalDateTime qaStartedAt;
    private LocalDateTime pausedAt;
    private Integer pausedAccumulatedSeconds;
    private Integer remainingSeconds;
    /** true = kết thúc sớm Q&A; false = hết giờ tự nhiên; null = chưa ENDED. */
    private Boolean qaEndedEarly;
}
