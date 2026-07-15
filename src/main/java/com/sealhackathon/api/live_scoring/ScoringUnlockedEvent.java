package com.sealhackathon.api.live_scoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoringUnlockedEvent {
    public static final String TYPE = "SCORING_UNLOCKED";

    private String type;
    private Integer roundId;
    private Integer trackId;
    private String reason;
    private String timestamp;
}
