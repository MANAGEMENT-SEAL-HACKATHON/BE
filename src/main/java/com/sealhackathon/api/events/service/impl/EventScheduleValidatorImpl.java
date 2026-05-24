package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.events.dto.request.CreateEventRequest;
import com.sealhackathon.api.events.dto.request.UpdateEventRequest;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.EventScheduleValidator;
import com.sealhackathon.api.events.service.impl.window.AwardsWindowRule;
import com.sealhackathon.api.events.service.impl.window.EventWindowRule;
import com.sealhackathon.api.events.service.impl.window.KickoffWindowRule;
import com.sealhackathon.api.events.service.impl.window.PresentationWindowRule;
import com.sealhackathon.api.events.service.impl.window.WorkshopWindowRule;
import com.sealhackathon.api.events.support.EventTimeline;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * FR-06A validator 3 lớp.
 *
 * <p>Lớp 1 (window theo từng loại) tách thành các rule trong subpackage
 * {@code events.service.impl.window} để tránh gom logic vào một method khổng lồ —
 * vốn dễ gây sai timeline khi sửa (xem các TC đã FAIL: TC-02, TC-06, TC-12, TC-13,
 * TC-20, TC-26, TC-28).
 *
 * <p>Lớp 2 (overlap milestone vs OTHER) và Lớp 3 (ordering theo phase) giữ nguyên
 * trong file này.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventScheduleValidatorImpl implements EventScheduleValidator {

    private final EventRepository eventRepository;
    private final WorkshopWindowRule workshopRule;
    private final KickoffWindowRule kickoffRule;
    private final PresentationWindowRule presentationRule;
    private final AwardsWindowRule awardsRule;

    private final EnumMap<EventType, EventWindowRule> windowRules =
            new EnumMap<>(EventType.class);

    @PostConstruct
    void initRules() {
        windowRules.put(EventType.WORKSHOP, workshopRule);
        windowRules.put(EventType.KICKOFF, kickoffRule);
        windowRules.put(EventType.PRESENTATION, presentationRule);
        windowRules.put(EventType.AWARDS, awardsRule);
    }

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

        LocalDateTime effectiveEnd = EventTimeline.effectiveEnd(startsAt, endsAt);

        // Lớp 1 — window theo từng loại (dispatcher)
        EventWindowRule rule = windowRules.get(type);
        if (rule != null) {
            rule.check(h, startsAt, effectiveEnd, excludeEventId);
        } else if (type == EventType.OTHER) {
            validateOtherWithinHackathon(h, startsAt, effectiveEnd);
        }

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
     * OTHER không có rule riêng — chỉ giữ guard "trong khung Hackathon"
     * ([eventStart, eventEnd]) cũ, không buffer.
     */
    private void validateOtherWithinHackathon(Hackathon h, LocalDateTime startsAt,
                                              LocalDateTime effectiveEnd) {
        LocalDate eventStart = h.getEventStart();
        LocalDate eventEnd = h.getEventEnd();
        if (eventStart != null && startsAt.toLocalDate().isBefore(eventStart)) {
            throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "Event OTHER startsAt (%s) trước eventStart (%s)"
                            .formatted(startsAt.toLocalDate(), eventStart),
                    Map.of("eventStart", eventStart, "startsAt", startsAt));
        }
        if (eventEnd != null && effectiveEnd != null
                && effectiveEnd.toLocalDate().isAfter(eventEnd)) {
            throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "Event OTHER kết thúc (%s) sau eventEnd (%s)"
                            .formatted(effectiveEnd, eventEnd),
                    Map.of("eventEnd", eventEnd, "effectiveEnd", effectiveEnd));
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
                case PRESENTATION -> "Workshop phải kết thúc trước Ngày thuyết trình";
                case AWARDS -> "Workshop phải kết thúc trước Lễ trao giải";
                default -> "Workshop phải kết thúc trước giai đoạn sau";
            };
            case KICKOFF -> switch (later) {
                case PRESENTATION -> "Khai mạc phải kết thúc trước Ngày thuyết trình";
                case AWARDS -> "Khai mạc phải kết thúc trước Lễ trao giải";
                default -> "Khai mạc phải kết thúc trước giai đoạn sau";
            };
            case PRESENTATION -> "Ngày thuyết trình phải kết thúc trước Lễ trao giải";
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
        if (h.getEventStart() != null && !startsAt.toLocalDate().equals(h.getEventStart())) {
            warnings.add(Warning.of("EVENT_ORDER_INVALID",
                    "KICKOFF nên đúng ngày eventStart",
                    Map.of("type", "KICKOFF", "eventStart", h.getEventStart(),
                            "startsAt", startsAt)));
        }
        return warnings;
    }
}
