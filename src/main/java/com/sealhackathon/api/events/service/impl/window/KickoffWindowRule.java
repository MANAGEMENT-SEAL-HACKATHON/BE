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
 * FR-06A KICKOFF window — trong gap (registrationEnd, eventStart), exclusive cả hai đầu.
 * Phải sau ngày đóng đăng ký và trước ngày thi (eventStart).
 *
 * <p>Cho phép tạo KICKOFF trước WORKSHOP (POST order) dù trên lịch KICKOFF diễn ra sau WORKSHOP.
 */
@Component
public class KickoffWindowRule implements EventWindowRule {

    @Override
    public void check(Hackathon h, LocalDateTime startsAt, LocalDateTime effectiveEnd,
                      Integer excludeEventId) {
        if (h == null || startsAt == null) {
            return;
        }
        LocalDate regEnd = h.getRegistrationEnd() != null ? h.getRegistrationEnd().toLocalDate() : null;
        LocalDate eventStart = h.getEventStart();
        LocalDate startDate = startsAt.toLocalDate();

        if (regEnd != null && !startDate.isAfter(regEnd)) {
            throw fail("KICKOFF phải sau ngày đóng đăng ký (%s)".formatted(regEnd),
                    h, startsAt, effectiveEnd);
        }
        if (eventStart != null && !startDate.isBefore(eventStart)) {
            throw fail("KICKOFF phải trước ngày thi Hackathon (eventStart %s)".formatted(eventStart),
                    h, startsAt, effectiveEnd);
        }
        if (effectiveEnd != null && eventStart != null
                && !effectiveEnd.toLocalDate().isBefore(eventStart)) {
            throw fail("KICKOFF phải kết thúc trước ngày thi (eventStart %s)".formatted(eventStart),
                    h, startsAt, effectiveEnd);
        }
    }

    private static BusinessRuleException fail(String message, Hackathon h,
                                              LocalDateTime startsAt, LocalDateTime effectiveEnd) {
        Map<String, Object> details = new HashMap<>();
        details.put("type", "KICKOFF");
        details.put("registrationEnd", h.getRegistrationEnd());
        details.put("eventStart", h.getEventStart());
        details.put("startsAt", startsAt);
        details.put("effectiveEnd", effectiveEnd);
        return new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON, message, details);
    }
}
