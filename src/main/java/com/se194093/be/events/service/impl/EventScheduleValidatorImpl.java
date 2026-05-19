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
 * FR-06 validator 3 lớp (MF-01 v3.1): Lớp 3a–3c BLOCK CỨNG; Lớp 3d warn mềm.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventScheduleValidatorImpl implements EventScheduleValidator {

    private static final Set<EventType> OVERLAP_BLOCKING_TYPES =
            EnumSet.of(EventType.KICKOFF, EventType.AWARDS);

    private static final int END_BUFFER_DAYS = 1;

    private final EventRepository eventRepository;

    @Override
    public void validateBlocking(Hackathon hackathon, CreateEventRequest req, Integer excludeEventId) {
        validateBlockingCommon(hackathon, req.getType(), req.getStartsAt(), req.getEndsAt(),
                req.getLocation(), req.getMeetUrl(), excludeEventId);
    }

    @Override
    public void validateBlocking(Hackathon hackathon, UpdateEventRequest req, Integer excludeEventId) {
        validateBlockingCommon(hackathon, req.getType(), req.getStartsAt(), req.getEndsAt(),
                req.getLocation(), req.getMeetUrl(), excludeEventId);
    }

    private void validateBlockingCommon(Hackathon h, EventType type,
                                        LocalDateTime startsAt, LocalDateTime endsAt,
                                        String location, String meetUrl,
                                        Integer excludeEventId) {
        if (h == null || type == null || startsAt == null) {
            return;
        }

        validateLocationOrMeetUrl(location, meetUrl);

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
            int ex = (excludeEventId == null) ? 0 : excludeEventId;
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

        validateLayer3Ordering(h.getId(), type, startsAt, endsAt, excludeEventId);
    }

    private void validateLocationOrMeetUrl(String location, String meetUrl) {
        boolean hasLocation = location != null && !location.isBlank();
        boolean hasMeetUrl = meetUrl != null && !meetUrl.isBlank();
        if (!hasLocation && !hasMeetUrl) {
            throw new BusinessRuleException(ErrorCode.EVENT_LOCATION_REQUIRED,
                    "Phải cung cấp địa điểm (offline) hoặc link họp (online)",
                    Map.of());
        }
    }

    /**
     * Lớp 3a–3c — BLOCK khi vi phạm thứ tự WORKSHOP &lt; KICKOFF &lt; PRESENTATION &lt; AWARDS.
     */
    private void validateLayer3Ordering(Integer hackathonId, EventType type,
                                      LocalDateTime startsAt, LocalDateTime endsAt,
                                      Integer excludeEventId) {
        int ex = (excludeEventId == null) ? 0 : excludeEventId;

        switch (type) {
            case WORKSHOP -> {
                for (Event kickoff : eventRepository.findByHackathonIdAndType(hackathonId, EventType.KICKOFF)) {
                    if (kickoff.getId().equals(ex)) {
                        continue;
                    }
                    if (!startsAt.isBefore(kickoff.getStartsAt())) {
                        throw orderViolation("Workshop phải diễn ra trước Khai mạc",
                                Map.of("type", "WORKSHOP", "kickoffId", kickoff.getId(),
                                        "kickoffStartsAt", kickoff.getStartsAt(), "startsAt", startsAt));
                    }
                }
            }
            case KICKOFF -> {
                for (Event workshop : eventRepository.findByHackathonIdAndType(hackathonId, EventType.WORKSHOP)) {
                    if (workshop.getId().equals(ex)) {
                        continue;
                    }
                    if (!workshop.getStartsAt().isBefore(startsAt)) {
                        throw orderViolation("Workshop phải diễn ra trước Khai mạc",
                                Map.of("type", "KICKOFF", "workshopId", workshop.getId(),
                                        "workshopStartsAt", workshop.getStartsAt(), "startsAt", startsAt));
                    }
                }
            }
            case PRESENTATION -> {
                List<Event> kickoffs = eventRepository.findLatestByType(hackathonId, EventType.KICKOFF);
                for (Event kickoff : kickoffs) {
                    if (kickoff.getId().equals(ex)) {
                        continue;
                    }
                    LocalDateTime kickoffEnd = kickoff.getEndsAt() != null
                            ? kickoff.getEndsAt() : kickoff.getStartsAt();
                    if (kickoffEnd != null && !kickoffEnd.isBefore(startsAt)) {
                        throw orderViolation("Khai mạc phải kết thúc trước Ngày thi",
                                Map.of("type", "PRESENTATION", "kickoffId", kickoff.getId(),
                                        "kickoffEnd", kickoffEnd, "startsAt", startsAt));
                    }
                }
            }
            case AWARDS -> {
                List<Event> presentations = eventRepository.findByHackathonIdAndType(
                        hackathonId, EventType.PRESENTATION);
                for (Event pres : presentations) {
                    if (pres.getId().equals(ex)) {
                        continue;
                    }
                    LocalDateTime presStart = pres.getStartsAt();
                    if (presStart != null && !presStart.isBefore(startsAt)) {
                        throw orderViolation("Ngày thi phải trước Lễ trao giải",
                                Map.of("type", "AWARDS", "presentationId", pres.getId(),
                                        "presentationStartsAt", presStart, "startsAt", startsAt));
                    }
                }
            }
            default -> { /* OTHER — no ordering */ }
        }
        if (endsAt != null) {
            /* endsAt used in layer 1; ordering uses startsAt per spec */
        }
    }

    private static BusinessRuleException orderViolation(String message, Map<String, Object> details) {
        return new BusinessRuleException(ErrorCode.EVENT_ORDER_VIOLATION, message, details);
    }

    @Override
    public List<Warning> computeLayer3Warnings(Hackathon hackathon, CreateEventRequest req) {
        return computeWarningsLayer3dOnly(hackathon, req.getType(), req.getStartsAt());
    }

    @Override
    public List<Warning> computeLayer3Warnings(Hackathon hackathon, UpdateEventRequest req) {
        return computeWarningsLayer3dOnly(hackathon, req.getType(), req.getStartsAt());
    }

    /** Lớp 3d — KICKOFF trong [event_start, event_start+1d]. */
    private List<Warning> computeWarningsLayer3dOnly(Hackathon h, EventType type,
                                                     LocalDateTime startsAt) {
        List<Warning> warnings = new ArrayList<>();
        if (h == null || type != EventType.KICKOFF || startsAt == null) {
            return warnings;
        }
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
        return warnings;
    }
}
