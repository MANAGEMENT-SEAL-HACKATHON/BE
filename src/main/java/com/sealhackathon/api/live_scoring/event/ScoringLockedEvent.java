package com.sealhackathon.api.live_scoring.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** FR-20A — round đã khóa chấm. */
@Getter
public class ScoringLockedEvent extends ApplicationEvent {

    private final Integer roundId;

    public ScoringLockedEvent(Object source, Integer roundId) {
        super(source);
        this.roundId = roundId;
    }
}
