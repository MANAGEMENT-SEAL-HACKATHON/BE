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
 * FR-06A KICKOFF window — phải đúng ngày khai mạc {@code eventStart} (= Ngày 1 hackathon,
 * tức "trước ngày thi 1 ngày" trong hackathon 2-day, hoặc cùng ngày trong 1-day).
 * Tham chiếu Spring 2026: Khai mạc 11/4 (eventStart) → Cuộc thi 12/4 (eventEnd).
 */
@Component
public class KickoffWindowRule implements EventWindowRule {

    @Override
    public void check(Hackathon h, LocalDateTime startsAt, LocalDateTime effectiveEnd,
                      Integer excludeEventId) {
        if (h == null || startsAt == null) {
            return;
        }
        LocalDate eventStart = h.getEventStart();
        LocalDate eventEnd = h.getEventEnd();
        LocalDate startDate = startsAt.toLocalDate();

        if (eventStart != null && !startDate.equals(eventStart)) {
            throw fail("Kick-off phải đúng ngày khai mạc Hackathon (%s)".formatted(eventStart),
                    h, startsAt, effectiveEnd);
        }
        if (effectiveEnd != null && eventEnd != null
                && effectiveEnd.toLocalDate().isAfter(eventEnd)) {
            throw fail("Kick-off phải kết thúc trong khung Hackathon (eventEnd %s)".formatted(eventEnd),
                    h, startsAt, effectiveEnd);
        }
    }

    private static BusinessRuleException fail(String message, Hackathon h,
                                              LocalDateTime startsAt, LocalDateTime effectiveEnd) {
        Map<String, Object> details = new HashMap<>();
        details.put("type", "KICKOFF");
        details.put("eventStart", h.getEventStart());
        details.put("eventEnd", h.getEventEnd());
        details.put("startsAt", startsAt);
        details.put("effectiveEnd", effectiveEnd);
        return new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON, message, details);
    }
}
