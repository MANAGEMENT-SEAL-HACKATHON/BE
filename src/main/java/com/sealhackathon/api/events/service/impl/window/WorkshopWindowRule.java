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
 * FR-06A WORKSHOP window — phải nằm trong gap (registrationEnd, eventStart),
 * exclusive cả hai đầu. Workshop trùng ngày đóng đăng ký hoặc trùng ngày khai mạc đều fail.
 */
@Component
public class WorkshopWindowRule implements EventWindowRule {

    @Override
    public void check(Hackathon h, LocalDateTime startsAt, LocalDateTime effectiveEnd,
                      Integer excludeEventId) {
        if (h == null || startsAt == null) {
            return;
        }
        LocalDate startDate = startsAt.toLocalDate();
        LocalDate regEnd = h.getRegistrationEnd();
        LocalDate eventStart = h.getEventStart();

        if (regEnd != null && !startDate.isAfter(regEnd)) {
            throw fail("WORKSHOP phải sau ngày đóng đăng ký (%s)".formatted(regEnd),
                    h, startsAt, effectiveEnd);
        }
        if (eventStart != null && !startDate.isBefore(eventStart)) {
            throw fail("WORKSHOP phải trước ngày khai mạc (Kick-off, %s)".formatted(eventStart),
                    h, startsAt, effectiveEnd);
        }
        if (effectiveEnd != null && eventStart != null
                && !effectiveEnd.toLocalDate().isBefore(eventStart)) {
            throw fail("WORKSHOP phải kết thúc trước ngày khai mạc (%s)".formatted(eventStart),
                    h, startsAt, effectiveEnd);
        }
    }

    private static BusinessRuleException fail(String message, Hackathon h,
                                              LocalDateTime startsAt, LocalDateTime effectiveEnd) {
        Map<String, Object> details = new HashMap<>();
        details.put("type", "WORKSHOP");
        details.put("registrationEnd", h.getRegistrationEnd());
        details.put("eventStart", h.getEventStart());
        details.put("startsAt", startsAt);
        details.put("effectiveEnd", effectiveEnd);
        return new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON, message, details);
    }
}
