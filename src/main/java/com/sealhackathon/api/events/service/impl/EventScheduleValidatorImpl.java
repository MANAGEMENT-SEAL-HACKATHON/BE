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
import com.sealhackathon.api.events.service.impl.window.WorkshopWindowRule;
import com.sealhackathon.api.events.support.EventTimeline;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.rounds.entity.Round;
import com.sealhackathon.api.rounds.repository.RoundRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * FR-06A validator 3 lớp.
 *
 * <p>POST order: KICKOFF → WORKSHOP → AWARDS (KICKOFF làm gốc). Trên lịch: WORKSHOP → KICKOFF → AWARDS.
 * PRESENTATION không còn là milestone —
 * validate như event phụ (OTHER) trong [eventStart, eventEnd].
 *
 * <p>Hackathon → Round → Track/Criteria. Event thuộc Hackathon, không ràng buộc chéo validation với Round qua validator này.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventScheduleValidatorImpl implements EventScheduleValidator {

    private final EventRepository eventRepository;
    private final RoundRepository roundRepository;
    private final WorkshopWindowRule workshopRule;
    private final KickoffWindowRule kickoffRule;
    private final AwardsWindowRule awardsRule;

    private final EnumMap<EventType, EventWindowRule> windowRules =
            new EnumMap<>(EventType.class);

    @PostConstruct
    void initRules() {
        windowRules.put(EventType.WORKSHOP, workshopRule);
        windowRules.put(EventType.KICKOFF, kickoffRule);
        windowRules.put(EventType.AWARDS, awardsRule);
    }

    @Override
    public void validateBlocking(Hackathon hackathon, CreateEventRequest req, Integer excludeEventId) {
        validateBuffet(req.getType(), req.getStartsAt(), req.getEndsAt(),
                req.getBuffetLocation(), req.getBuffetStartsAt(), req.getBuffetEndsAt());
        validateBlockingCommon(hackathon, req.getType(), req.getStartsAt(), req.getEndsAt(),
                req.getLocation(), req.getMeetUrl(), excludeEventId);
    }

    @Override
    public void validateBlocking(Hackathon hackathon, UpdateEventRequest req, Integer excludeEventId) {
        validateBuffet(req.getType(), req.getStartsAt(), req.getEndsAt(),
                req.getBuffetLocation(), req.getBuffetStartsAt(), req.getBuffetEndsAt());
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

        // PRESENTATION: không còn trong milestone chain và không cần validate thêm.
        // Coordinator tự chịu trách nhiệm quản lý loại sự kiện này.
        if (type == EventType.PRESENTATION) {
            return;
        }

        // Lớp 1 — window theo từng loại (dispatcher)
        EventWindowRule rule = windowRules.get(type);
        if (rule != null) {
            rule.check(h, startsAt, effectiveEnd, excludeEventId);
        } else {
            // OTHER: chỉ check nằm trong [eventStart, eventEnd]
            validateWithinHackathon(h, startsAt, effectiveEnd, type);
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
        } else {
            validateMilestoneDoesNotOverlapOther(h.getId(), startsAt, effectiveEnd, ex);
        }

        validateLayer3Ordering(h.getId(), type, startsAt, effectiveEnd, excludeEventId);
        validateWorkshopKickoffDifferentDays(h.getId(), type, startsAt, effectiveEnd, excludeEventId);
        validateAwardsAfterFinalSubmissionDeadline(h, type, startsAt);
    }

    /**
     * Lễ trao giải bắt đầu sau hạn nộp bài vòng Chung kết (khi round CK đã có deadline).
     */
    private void validateAwardsAfterFinalSubmissionDeadline(Hackathon h, EventType type,
                                                           LocalDateTime awardsStartsAt) {
        if (h == null || type != EventType.AWARDS || awardsStartsAt == null) {
            return;
        }
        roundRepository.findByHackathon_IdAndIsFinalTrue(h.getId()).ifPresent(finalRound -> {
            LocalDateTime minMoment = resolveAwardsMinMoment(finalRound);
            if (minMoment != null && !awardsStartsAt.isAfter(minMoment)) {
                throw new BusinessRuleException(ErrorCode.AWARDS_BEFORE_FINAL_DEADLINE,
                        "Lễ trao giải (%s) phải bắt đầu sau khi vòng Chung kết công bố kết quả (%s)"
                                .formatted(awardsStartsAt, minMoment),
                        Map.of("hackathonId", h.getId(),
                                "awardsStartsAt", awardsStartsAt,
                                "minMoment", minMoment,
                                "finalRoundId", finalRound.getId()));
            }
        });
    }

    private static LocalDateTime resolveAwardsMinMoment(Round finalRound) {
        if (finalRound.getPublishedAt() != null) {
            return finalRound.getPublishedAt();
        }
        if (finalRound.getScoringLockedAt() != null) {
            return finalRound.getScoringLockedAt();
        }
        if (finalRound.getExamAt() == null) {
            return null;
        }
        int codingHours = finalRound.getCodingDurationHours() != null
                ? finalRound.getCodingDurationHours().intValue() : 0;
        int pres = finalRound.getDefaultPresentationMinutes() != null
                ? finalRound.getDefaultPresentationMinutes() : 10;
        int qa = finalRound.getDefaultQaMinutes() != null ? finalRound.getDefaultQaMinutes() : 5;
        int bufferMinutes = (pres + qa) * 4;
        return finalRound.getExamAt().plusHours(codingHours).plusMinutes(bufferMinutes);
    }

    /**
     * Check sự kiện nằm trong khung Hackathon [eventStart, eventEnd].
     * Dùng cho PRESENTATION và OTHER (không có rule riêng).
     */
    private void validateWithinHackathon(Hackathon h, LocalDateTime startsAt,
                                         LocalDateTime effectiveEnd, EventType type) {
        LocalDate eventStart = h.getEventStart();
        LocalDate eventEnd = h.getEventEnd();
        if (eventStart != null && startsAt.toLocalDate().isBefore(eventStart)) {
            throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "Event %s startsAt (%s) trước eventStart (%s)"
                            .formatted(type.name(), startsAt.toLocalDate(), eventStart),
                    Map.of("eventStart", eventStart, "startsAt", startsAt, "type", type.name()));
        }
        if (eventEnd != null && effectiveEnd != null
                && effectiveEnd.toLocalDate().isAfter(eventEnd)) {
            throw new BusinessRuleException(ErrorCode.EVENT_OUT_OF_HACKATHON,
                    "Event %s kết thúc (%s) sau eventEnd (%s)"
                            .formatted(type.name(), effectiveEnd, eventEnd),
                    Map.of("eventEnd", eventEnd, "effectiveEnd", effectiveEnd, "type", type.name()));
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
                    "Sự kiện trùng khung milestone %s (id=%d)"
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

    /**
     * Buffet fields are Kickoff-only and must lie within the event window [startsAt, endsAt].
     */
    private void validateBuffet(EventType type, LocalDateTime startsAt, LocalDateTime endsAt,
                                String buffetLocation, LocalDateTime buffetStartsAt,
                                LocalDateTime buffetEndsAt) {
        boolean hasLocation = buffetLocation != null && !buffetLocation.isBlank();
        boolean anyBuffet = hasLocation || buffetStartsAt != null || buffetEndsAt != null;
        if (!anyBuffet) {
            return;
        }
        if (type != EventType.KICKOFF) {
            throw new BusinessRuleException(ErrorCode.EVENT_BUFFET_NOT_KICKOFF,
                    "Thông tin buffet chỉ áp dụng cho sự kiện Khai mạc (KICKOFF)",
                    Map.of("type", type == null ? "null" : type.name()));
        }
        if (startsAt == null) {
            return;
        }
        LocalDateTime windowEnd = EventTimeline.effectiveEnd(startsAt, endsAt);
        if (buffetStartsAt != null
                && (buffetStartsAt.isBefore(startsAt) || buffetStartsAt.isAfter(windowEnd))) {
            throw buffetOutOfWindow(startsAt, windowEnd, buffetStartsAt, buffetEndsAt);
        }
        if (buffetEndsAt != null
                && (buffetEndsAt.isBefore(startsAt) || buffetEndsAt.isAfter(windowEnd))) {
            throw buffetOutOfWindow(startsAt, windowEnd, buffetStartsAt, buffetEndsAt);
        }
        if (buffetStartsAt != null && buffetEndsAt != null && buffetEndsAt.isBefore(buffetStartsAt)) {
            throw new BusinessRuleException(ErrorCode.EVENT_BUFFET_OUT_OF_WINDOW,
                    "buffetEndsAt (%s) phải >= buffetStartsAt (%s)"
                            .formatted(buffetEndsAt, buffetStartsAt),
                    Map.of("buffetStartsAt", buffetStartsAt, "buffetEndsAt", buffetEndsAt,
                            "startsAt", startsAt, "endsAt", windowEnd));
        }
    }

    private static BusinessRuleException buffetOutOfWindow(LocalDateTime startsAt,
                                                           LocalDateTime windowEnd,
                                                           LocalDateTime buffetStartsAt,
                                                           LocalDateTime buffetEndsAt) {
        return new BusinessRuleException(ErrorCode.EVENT_BUFFET_OUT_OF_WINDOW,
                "Khung giờ buffet phải nằm trong [%s, %s]".formatted(startsAt, windowEnd),
                Map.of("startsAt", startsAt, "endsAt", windowEnd,
                        "buffetStartsAt", buffetStartsAt == null ? "null" : buffetStartsAt,
                        "buffetEndsAt", buffetEndsAt == null ? "null" : buffetEndsAt));
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
                case AWARDS -> "Workshop phải kết thúc trước Lễ trao giải";
                default -> "Workshop phải kết thúc trước giai đoạn sau";
            };
            case KICKOFF -> "Khai mạc phải kết thúc trước Lễ trao giải";
            default -> "%s phải kết thúc trước %s".formatted(earlier, later);
        };
    }

    private static BusinessRuleException orderViolation(String message, Map<String, Object> details) {
        return new BusinessRuleException(ErrorCode.EVENT_ORDER_VIOLATION, message, details);
    }

    /**
     * WORKSHOP và KICKOFF phải ở hai ngày lịch khác nhau (không cùng calendar day).
     */
    private void validateWorkshopKickoffDifferentDays(Integer hackathonId, EventType newType,
                                                      LocalDateTime newStartsAt,
                                                      LocalDateTime newEffectiveEnd,
                                                      Integer excludeEventId) {
        if (newType != EventType.WORKSHOP && newType != EventType.KICKOFF) {
            return;
        }
        int ex = (excludeEventId == null) ? 0 : excludeEventId;

        if (newType == EventType.KICKOFF) {
            for (Event ws : eventRepository.findByHackathonIdAndType(hackathonId, EventType.WORKSHOP)) {
                if (ws.getId().equals(ex) || ws.getStartsAt() == null) {
                    continue;
                }
                LocalDate wsEndDay = EventTimeline.effectiveEnd(ws).toLocalDate();
                if (!newStartsAt.toLocalDate().isAfter(wsEndDay)) {
                    throw orderViolation(
                            "Workshop và Khai mạc phải diễn ra ở hai ngày khác nhau",
                            Map.of("type", "KICKOFF",
                                    "startsAt", newStartsAt,
                                    "workshopEffectiveEnd", EventTimeline.effectiveEnd(ws),
                                    "workshopEventId", ws.getId()));
                }
            }
        } else {
            for (Event ko : eventRepository.findByHackathonIdAndType(hackathonId, EventType.KICKOFF)) {
                if (ko.getId().equals(ex) || ko.getStartsAt() == null) {
                    continue;
                }
                if (!newEffectiveEnd.toLocalDate().isBefore(ko.getStartsAt().toLocalDate())) {
                    throw orderViolation(
                            "Workshop và Khai mạc phải diễn ra ở hai ngày khác nhau",
                            Map.of("type", "WORKSHOP",
                                    "effectiveEnd", newEffectiveEnd,
                                    "kickoffStartsAt", ko.getStartsAt(),
                                    "kickoffEventId", ko.getId()));
                }
            }
        }
    }

    @Override
    public List<Warning> computeLayer3Warnings(Hackathon hackathon, CreateEventRequest req) {
        return List.of();
    }

    @Override
    public List<Warning> computeLayer3Warnings(Hackathon hackathon, UpdateEventRequest req) {
        return List.of();
    }
}
