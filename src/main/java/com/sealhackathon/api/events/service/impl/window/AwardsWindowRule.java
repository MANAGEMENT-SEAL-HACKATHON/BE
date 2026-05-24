package com.sealhackathon.api.events.service.impl.window;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * FR-06A AWARDS window — Lễ trao giải đứng cuối timeline.
 *
 * <p>Rule duy nhất: {@code date == eventEnd} (đúng ngày kết thúc Hackathon).
 * Event và Round là đồng cấp — không check Round.examAt hay PRESENTATION ở đây.
 */
@Component
public class AwardsWindowRule implements EventWindowRule {

    @Override
    public void check(Hackathon h, LocalDateTime startsAt, LocalDateTime effectiveEnd,
                      Integer excludeEventId) {
        if (h == null || startsAt == null) {
            return;
        }
        LocalDate eventEnd = h.getEventEnd();
        LocalDate startDate = startsAt.toLocalDate();

        if (eventEnd != null && !startDate.equals(eventEnd)) {
            Map<String, Object> details = new HashMap<>();
            details.put("type", "AWARDS");
            details.put("eventEnd", eventEnd);
            details.put("startsAt", startsAt);
            throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "AWARDS phải đúng ngày kết thúc Hackathon (%s)".formatted(eventEnd),
                    details);
        }
        if (effectiveEnd != null && eventEnd != null
                && effectiveEnd.toLocalDate().isAfter(eventEnd)) {
            Map<String, Object> details = new HashMap<>();
            details.put("type", "AWARDS");
            details.put("eventEnd", eventEnd);
            details.put("effectiveEnd", effectiveEnd);
            throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "AWARDS phải kết thúc trong ngày eventEnd (%s)".formatted(eventEnd),
                    details);
        }
    }
}
