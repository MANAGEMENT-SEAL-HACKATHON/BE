package com.sealhackathon.api.live_scoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * STOMP payload for timer phase sync (G5-F).
 * topic: /topic/rounds/{roundId}/presentation-queue (same as queue invalidate).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresentationTimerPhaseEvent {

    public static final String TYPE = "TIMER_PHASE";

    private String type;
    /** Round-scoped queue id (roundId as string). */
    private String queueId;
    /** Current presenting submission — required for multi-track FE filtering. */
    private Integer submissionId;
    private String phase;
    private Integer remainingSeconds;
    private String timestamp;
}
