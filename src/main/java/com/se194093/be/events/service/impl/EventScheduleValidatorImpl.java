package com.se194093.be.events.service.impl;

import com.se194093.be.common.exception.BusinessRuleException;
import com.se194093.be.common.exception.ErrorCode;
import com.se194093.be.common.response.Warning;
import com.se194093.be.events.dto.request.CreateEventRequest;
import com.se194093.be.events.dto.request.UpdateEventRequest;
import com.se194093.be.events.entity.Event;
import com.se194093.be.events.repository.EventRepository;
import com.se194093.be.events.service.EventScheduleValidator;
import com.se194093.be.events.value_object.EventType;
import com.se194093.be.hackathons.entity.Hackathon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FR-06A validator 3 lớp. Lớp 1 (in-range hackathon + buffer 1 ngày), Lớp 2 (overlap cùng type
 * KICKOFF/AWARDS), Lớp 3 (warn mềm thứ tự sự kiện).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventScheduleValidatorImpl implements EventScheduleValidator {

    /** Type bắt buộc unique-overlap. PRESENTATION được phép parallel. */
    private static final Set<EventType> OVERLAP_BLOCKING_TYPES =
            EnumSet.of(EventType.KICKOFF, EventType.AWARDS);

    /** Số ngày đệm sau {@code hackathon.event_end} cho phép tổ chức trao giải / dọn dẹp. */
    private static final int END_BUFFER_DAYS = 1;

    private final EventRepository eventRepository;

    @Override
    public void validateBlocking(Hackathon hackathon, CreateEventRequest req, Integer excludeEventId) {
        validateBlockingCommon(hackathon, req.getType(), req.getStartsAt(), req.getEndsAt(),
                Boolean.TRUE.equals(req.getIsPublic()), excludeEventId);
    }

    @Override
    public void validateBlocking(Hackathon hackathon, UpdateEventRequest req, Integer excludeEventId) {
        validateBlockingCommon(hackathon, req.getType(), req.getStartsAt(), req.getEndsAt(),
                Boolean.TRUE.equals(req.getIsPublic()), excludeEventId);
    }

    private void validateBlockingCommon(Hackathon h, EventType type,
                                        LocalDateTime startsAt, LocalDateTime endsAt,
                                        boolean isPublic, Integer excludeEventId) {
        if (h == null) {
            return;
        }
        if (endsAt != null && endsAt.isBefore(startsAt)) {
            throw new BusinessRuleException(ErrorCode.EVENT_END_BEFORE_START,
                    "endsAt (%s) phải >= startsAt (%s)".formatted(endsAt, startsAt),
                    Map.of("startsAt", startsAt, "endsAt", endsAt));
        }

        LocalDate eventStart = h.getEventStart();
        LocalDate eventEnd = h.getEventEnd();
        LocalDateTime effectiveEnd = (endsAt != null) ? endsAt : startsAt;
        if (eventStart != null && startsAt.toLocalDate().isBefore(eventStart)) {
            throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "Event startsAt (%s) trước Hackathon eventStart (%s)"
                            .formatted(startsAt.toLocalDate(), eventStart),
                    Map.of("eventStart", eventStart, "eventEnd", eventEnd,
                           "startsAt", startsAt, "endsAt", endsAt));
        }
        if (eventEnd != null) {
            LocalDate cap = eventEnd.plusDays(END_BUFFER_DAYS);
            if (effectiveEnd.toLocalDate().isAfter(cap)) {
                throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                        "Event %s (%s) sau khung Hackathon eventEnd+%dd (%s)"
                                .formatted(endsAt != null ? "endsAt" : "startsAt",
                                          effectiveEnd, END_BUFFER_DAYS, cap),
                        Map.of("eventStart", eventStart, "eventEnd", eventEnd,
                               "bufferDays", END_BUFFER_DAYS, "effectiveEnd", effectiveEnd));
            }
        }

        if (OVERLAP_BLOCKING_TYPES.contains(type)) {
            Integer ex = (excludeEventId == null) ? 0 : excludeEventId;
            List<Event> overlaps = eventRepository.findOverlapping(
                    h.getId(), type, startsAt, effectiveEnd, ex);
            if (!overlaps.isEmpty()) {
                List<Integer> ids = overlaps.stream().map(Event::getId).toList();
                throw new BusinessRuleException(ErrorCode.EVENT_OVERLAP,
                        "Event type %s đã tồn tại trong khung giờ này (%d trùng)"
                                .formatted(type, ids.size()),
                        Map.of("conflictIds", ids, "type", type.name(),
                               "startsAt", startsAt, "endsAt", effectiveEnd));
            }
        }
        // hidden var to silence unused warning if needed in IDE
        if (isPublic) { /* no-op — sẽ dùng ở fan-out reminder */ }
    }

    @Override
    public List<Warning> computeLayer3Warnings(Hackathon hackathon, CreateEventRequest req) {
        return computeWarningsCommon(hackathon, req.getType(), req.getStartsAt(), req.getEndsAt());
    }

    @Override
    public List<Warning> computeLayer3Warnings(Hackathon hackathon, UpdateEventRequest req) {
        return computeWarningsCommon(hackathon, req.getType(), req.getStartsAt(), req.getEndsAt());
    }

    private List<Warning> computeWarningsCommon(Hackathon h, EventType type,
                                                LocalDateTime startsAt, LocalDateTime endsAt) {
        List<Warning> warnings = new ArrayList<>();
        if (h == null || type == null || startsAt == null) {
            return warnings;
        }
        switch (type) {
            case WORKSHOP -> {
                if (h.getRegistrationEnd() != null
                        && startsAt.toLocalDate().isAfter(h.getRegistrationEnd())) {
                    warnings.add(Warning.of("EVENT_ORDER_INVALID",
                            "WORKSHOP nên trước registrationEnd (%s)".formatted(h.getRegistrationEnd()),
                            Map.of("type", "WORKSHOP", "registrationEnd", h.getRegistrationEnd(),
                                   "startsAt", startsAt)));
                }
            }
            case KICKOFF -> {
                if (h.getEventStart() != null) {
                    LocalDate cap = h.getEventStart().plusDays(1);
                    if (startsAt.toLocalDate().isBefore(h.getEventStart())
                            || startsAt.toLocalDate().isAfter(cap)) {
                        warnings.add(Warning.of("EVENT_ORDER_INVALID",
                                "KICKOFF nên trong [eventStart, eventStart+1d]",
                                Map.of("type", "KICKOFF", "eventStart", h.getEventStart(),
                                       "startsAt", startsAt)));
                    }
                }
            }
            case PRESENTATION -> {
                List<Event> kickoffs = eventRepository.findLatestByType(h.getId(), EventType.KICKOFF);
                if (!kickoffs.isEmpty()) {
                    Event latest = kickoffs.get(0);
                    LocalDateTime kickoffEnd = (latest.getEndsAt() != null)
                            ? latest.getEndsAt() : latest.getStartsAt();
                    if (kickoffEnd != null && startsAt.isBefore(kickoffEnd)) {
                        warnings.add(Warning.of("EVENT_ORDER_INVALID",
                                "PRESENTATION nên sau khi KICKOFF (id=%d) kết thúc"
                                        .formatted(latest.getId()),
                                Map.of("type", "PRESENTATION", "kickoffId", latest.getId(),
                                       "kickoffEnd", kickoffEnd, "startsAt", startsAt)));
                    }
                }
            }
            case AWARDS -> {
                List<Event> presentations = eventRepository.findLatestByType(h.getId(), EventType.PRESENTATION);
                if (!presentations.isEmpty()) {
                    Event latest = presentations.get(0);
                    LocalDateTime maxStart = latest.getStartsAt();
                    if (maxStart != null && startsAt.isBefore(maxStart)) {
                        warnings.add(Warning.of("EVENT_ORDER_INVALID",
                                "AWARDS nên sau PRESENTATION muộn nhất (id=%d, startsAt=%s)"
                                        .formatted(latest.getId(), maxStart),
                                Map.of("type", "AWARDS", "lastPresentationId", latest.getId(),
                                       "lastPresentationStart", maxStart, "startsAt", startsAt)));
                    }
                }
            }
            default -> { /* TEAM_MEETING, OTHER — không kiểm tra thứ tự */ }
        }
        // endsAt dùng để rộng API nhưng không trigger warning ở MF-01 — silence unused
        if (endsAt != null) { /* no-op */ }
        return warnings;
    }
}
