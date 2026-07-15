package com.sealhackathon.api.live_scoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControllerChangedEvent {
    public static final String TYPE = "CONTROLLER_CHANGED";

    private String type;
    private Integer roundId;
    private Integer trackId;
    private Integer controllerJudgeId;
    private Integer previousControllerJudgeId;
    private String timestamp;
}
