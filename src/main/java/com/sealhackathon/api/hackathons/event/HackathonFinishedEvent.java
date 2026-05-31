package com.sealhackathon.api.hackathons.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** FR-33 — sau commit PENDING_CONFIRM → FINISHED. */
@Getter
public class HackathonFinishedEvent extends ApplicationEvent {

    private final Integer hackathonId;

    public HackathonFinishedEvent(Object source, Integer hackathonId) {
        super(source);
        this.hackathonId = hackathonId;
    }
}
