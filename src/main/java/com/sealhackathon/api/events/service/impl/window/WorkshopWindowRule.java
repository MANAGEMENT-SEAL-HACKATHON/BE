package com.sealhackathon.api.events.service.impl.window;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * FR-06A WORKSHOP window — gap (registrationEnd, eventStart), giống code cũ.
 *
 * <p>POST order: bắt buộc đã có bản ghi KICKOFF (gốc) trước khi tạo WORKSHOP.
 * Trên lịch thực tế WORKSHOP vẫn diễn ra <strong>trước</strong> KICKOFF (Lớp 3 phaseOrder).
 */
@Component
@RequiredArgsConstructor
public class WorkshopWindowRule implements EventWindowRule {

    private final EventRepository eventRepository;

    @Override
    public void check(Hackathon h, LocalDateTime startsAt, LocalDateTime effectiveEnd,
                      Integer excludeEventId) {
        if (h == null || startsAt == null) {
            return;
        }
        Integer hackathonId = h.getId();
        int ex = (excludeEventId == null) ? 0 : excludeEventId;

        boolean hasKickoff = eventRepository.findByHackathonIdAndType(hackathonId, EventType.KICKOFF).stream()
                .anyMatch(e -> !e.getId().equals(ex));
        if (!hasKickoff) {
            throw fail("WORKSHOP yêu cầu đã tạo KICKOFF trước (POST order — KICKOFF làm gốc)",
                    h, startsAt, effectiveEnd, Map.of("requiredType", "KICKOFF"));
        }

        LocalDate startDate = startsAt.toLocalDate();
        LocalDate regEnd = h.getRegistrationEnd();
        LocalDate eventStart = h.getEventStart();

        if (regEnd != null && !startDate.isAfter(regEnd)) {
            throw fail("WORKSHOP phải sau ngày đóng đăng ký (%s)".formatted(regEnd),
                    h, startsAt, effectiveEnd, Map.of());
        }
        if (eventStart != null && !startDate.isBefore(eventStart)) {
            throw fail("WORKSHOP phải trước ngày khai mạc (Kick-off, %s)".formatted(eventStart),
                    h, startsAt, effectiveEnd, Map.of());
        }
        if (effectiveEnd != null && eventStart != null
                && !effectiveEnd.toLocalDate().isBefore(eventStart)) {
            throw fail("WORKSHOP phải kết thúc trước ngày khai mạc (%s)".formatted(eventStart),
                    h, startsAt, effectiveEnd, Map.of());
        }
    }

    private static BusinessRuleException fail(String message, Hackathon h,
                                              LocalDateTime startsAt, LocalDateTime effectiveEnd,
                                              Map<String, Object> extra) {
        Map<String, Object> details = new HashMap<>();
        details.put("type", "WORKSHOP");
        details.put("registrationEnd", h.getRegistrationEnd());
        details.put("eventStart", h.getEventStart());
        details.put("startsAt", startsAt);
        details.put("effectiveEnd", effectiveEnd);
        if (extra != null) {
            details.putAll(extra);
        }
        return new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON, message, details);
    }
}
