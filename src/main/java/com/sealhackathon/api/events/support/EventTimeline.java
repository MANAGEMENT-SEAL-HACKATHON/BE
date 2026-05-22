package com.sealhackathon.api.events.support;

import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.value_object.EventType;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/** Thời gian hiệu lực sự kiện milestone — dùng chung validator & round.examAt. */
public final class EventTimeline {

    public static final Set<EventType> MILESTONE_TYPES = EnumSet.of(
            EventType.WORKSHOP,
            EventType.KICKOFF,
            EventType.PRESENTATION,
            EventType.AWARDS);

    private EventTimeline() {
    }

    public static LocalDateTime effectiveEnd(Event event) {
        if (event == null || event.getStartsAt() == null) {
            return null;
        }
        return effectiveEnd(event.getStartsAt(), event.getEndsAt());
    }

    public static LocalDateTime effectiveEnd(LocalDateTime startsAt, LocalDateTime endsAt) {
        return endsAt != null ? endsAt : startsAt;
    }

    public static int phaseOrder(EventType type) {
        return switch (type) {
            case WORKSHOP -> 0;
            case KICKOFF -> 1;
            case PRESENTATION -> 2;
            case AWARDS -> 3;
            default -> -1;
        };
    }

    public static boolean isMilestone(EventType type) {
        return type != null && MILESTONE_TYPES.contains(type);
    }
}
