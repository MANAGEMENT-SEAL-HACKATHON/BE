package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.events.dto.request.CreateEventRequest;
import com.sealhackathon.api.events.dto.request.UpdateEventRequest;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.EventScheduleValidator;
import com.sealhackathon.api.events.support.EventTimeline;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FR-06 validator 3 lớp (MF-01): WORKSHOP có thể trước {@code event_start} (Fall/Spring PDF);
 * milestone nối tiếp WORKSHOP → KICKOFF → PRESENTATION → AWARDS.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventScheduleValidatorImpl implements EventScheduleValidator {

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

        if (EventTimeline.isMilestone(type) && endsAt == null) {
            throw new BusinessRuleException(ErrorCode.EVENT_END_REQUIRED,
                    "Sự kiện %s bắt buộc có endsAt — giai đoạn phải có thời điểm kết thúc"
                            .formatted(type.name()),
                    Map.of("type", type.name(), "startsAt", startsAt));
        }

        if (endsAt != null && endsAt.isBefore(startsAt)) {
            throw new BusinessRuleException(ErrorCode.EVENT_END_BEFORE_START,
                    "endsAt (%s) phải >= startsAt (%s)".formatted(endsAt, startsAt),
                    Map.of("startsAt", startsAt, "endsAt", endsAt));
        }

        LocalDate eventStart = h.getEventStart();
        LocalDate eventEnd = h.getEventEnd();
        LocalDate registrationStart = h.getRegistrationStart();
        LocalDateTime effectiveEnd = EventTimeline.effectiveEnd(startsAt, endsAt);

        validateHackathonDateWindow(type, startsAt, effectiveEnd, eventStart, eventEnd,
                registrationStart, endsAt);

        int ex = (excludeEventId == null) ? 0 : excludeEventId;
        if (EventTimeline.isMilestone(type)) {
            validateSingleMilestonePerType(h.getId(), type, excludeEventId);
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
            validateOtherDoesNotOverlapMilestone(h.getId(), startsAt, effectiveEnd, ex);
        } else if (type == EventType.OTHER) {
            validateMilestoneDoesNotOverlapOther(h.getId(), startsAt, effectiveEnd, ex);
        }

        validateLayer3Ordering(h.getId(), type, startsAt, effectiveEnd, excludeEventId);
    }

    /**
     * Lớp 1 — WORKSHOP: từ {@code registrationStart}, có thể trước {@code event_start} (9/4 trước 11/4).
     * Các milestone khác: {@code startsAt >= event_start}.
     */
    private void validateHackathonDateWindow(EventType type, LocalDateTime startsAt,
                                             LocalDateTime effectiveEnd,
                                             LocalDate eventStart, LocalDate eventEnd,
                                             LocalDate registrationStart, LocalDateTime endsAt) {
        if (type == EventType.WORKSHOP) {
            if (registrationStart != null && startsAt.toLocalDate().isBefore(registrationStart)) {
                throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                        "Workshop startsAt (%s) trước registrationStart (%s)"
                                .formatted(startsAt.toLocalDate(), registrationStart),
                        Map.of("registrationStart", registrationStart,
                                "startsAt", startsAt, "endsAt", endsAt));
            }
        } else if (eventStart != null && startsAt.toLocalDate().isBefore(eventStart)) {
            throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "Event startsAt (%s) trước Hackathon eventStart (%s)"
                            .formatted(startsAt.toLocalDate(), eventStart),
                    Map.of("eventStart", eventStart, "eventEnd", eventEnd,
                            "startsAt", startsAt, "endsAt", endsAt, "type", type.name()));
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
    }

    private void validateSingleMilestonePerType(Integer hackathonId, EventType type, Integer excludeEventId) {
        int ex = (excludeEventId == null) ? 0 : excludeEventId;
        for (Event existing : eventRepository.findByHackathonIdAndType(hackathonId, type)) {
            if (!existing.getId().equals(ex)) {
                throw new BusinessRuleException(ErrorCode.EVENT_MILESTONE_DUPLICATE,
                        "Mỗi hackathon chỉ có một sự kiện type=%s (đã có id=%d)"
                                .formatted(type.name(), existing.getId()),
                        Map.of("type", type.name(), "existingEventId", existing.getId(),
                                "hackathonId", hackathonId));
            }
        }
    }

    private void validateOtherDoesNotOverlapMilestone(Integer hackathonId,
                                                      LocalDateTime startsAt,
                                                      LocalDateTime effectiveEnd,
                                                      int excludeId) {
        List<Event> others = eventRepository.findOtherOverlapping(
                hackathonId, startsAt, effectiveEnd, excludeId);
        if (!others.isEmpty()) {
            throw milestoneConflictException("Khung giờ milestone trùng sự kiện OTHER (id=%d)"
                            .formatted(others.get(0).getId()),
                    others.stream().map(Event::getId).toList(),
                    startsAt, effectiveEnd);
        }
    }

    private void validateMilestoneDoesNotOverlapOther(Integer hackathonId,
                                                        LocalDateTime startsAt,
                                                        LocalDateTime effectiveEnd,
                                                        int excludeId) {
        List<Event> milestones = eventRepository.findMilestoneOverlapping(
                hackathonId, EventTimeline.MILESTONE_TYPES, startsAt, effectiveEnd, excludeId);
        if (!milestones.isEmpty()) {
            Event hit = milestones.get(0);
            throw milestoneConflictException(
                    "Sự kiện OTHER trùng khung milestone %s (id=%d)"
                            .formatted(hit.getType().name(), hit.getId()),
                    List.of(hit.getId()),
                    startsAt, effectiveEnd);
        }
    }

    private static BusinessRuleException milestoneConflictException(String message,
                                                                    List<Integer> conflictIds,
                                                                    LocalDateTime startsAt,
                                                                    LocalDateTime effectiveEnd) {
        return new BusinessRuleException(ErrorCode.EVENT_CONFLICTS_WITH_MILESTONE,
                message,
                Map.of("conflictIds", conflictIds,
                        "startsAt", startsAt,
                        "endsAt", effectiveEnd));
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

    private void validateLayer3Ordering(Integer hackathonId, EventType newType,
                                      LocalDateTime newStartsAt, LocalDateTime newEffectiveEnd,
                                      Integer excludeEventId) {
        if (!EventTimeline.isMilestone(newType)) {
            return;
        }
        int ex = (excludeEventId == null) ? 0 : excludeEventId;
        int newOrder = EventTimeline.phaseOrder(newType);

        for (EventType existingType : EventTimeline.MILESTONE_TYPES) {
            for (Event existing : eventRepository.findByHackathonIdAndType(hackathonId, existingType)) {
                if (existing.getId().equals(ex) || existing.getStartsAt() == null) {
                    continue;
                }
                LocalDateTime existingEnd = EventTimeline.effectiveEnd(existing);
                int existingOrder = EventTimeline.phaseOrder(existingType);

                if (newOrder < existingOrder) {
                    if (!newEffectiveEnd.isBefore(existing.getStartsAt())) {
                        throw orderViolation(earlierMustEndBeforeLater(newType, existingType),
                                Map.of("type", newType.name(),
                                        "effectiveEnd", newEffectiveEnd,
                                        "laterType", existingType.name(),
                                        "laterStartsAt", existing.getStartsAt(),
                                        "existingEventId", existing.getId()));
                    }
                } else if (newOrder > existingOrder) {
                    if (!existingEnd.isBefore(newStartsAt)) {
                        throw orderViolation(earlierMustEndBeforeLater(existingType, newType),
                                Map.of("type", newType.name(),
                                        "startsAt", newStartsAt,
                                        "earlierType", existingType.name(),
                                        "earlierEffectiveEnd", existingEnd,
                                        "existingEventId", existing.getId()));
                    }
                }
            }
        }
    }

    private static String earlierMustEndBeforeLater(EventType earlier, EventType later) {
        return switch (earlier) {
            case WORKSHOP -> switch (later) {
                case KICKOFF -> "Workshop phải kết thúc trước Khai mạc";
                case PRESENTATION -> "Workshop phải kết thúc trước Ngày thi";
                case AWARDS -> "Workshop phải kết thúc trước Lễ trao giải";
                default -> "Workshop phải kết thúc trước giai đoạn sau";
            };
            case KICKOFF -> switch (later) {
                case PRESENTATION -> "Khai mạc phải kết thúc trước Ngày thi";
                case AWARDS -> "Khai mạc phải kết thúc trước Lễ trao giải";
                default -> "Khai mạc phải kết thúc trước giai đoạn sau";
            };
            case PRESENTATION -> "Ngày thi phải kết thúc trước Lễ trao giải";
            default -> "%s phải kết thúc trước %s".formatted(earlier, later);
        };
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
