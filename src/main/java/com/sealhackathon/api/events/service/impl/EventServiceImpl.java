package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
import com.sealhackathon.api.common.exception.BusinessRuleException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.exception.ResourceNotFoundException;
import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.events.dto.request.CreateEventRequest;
import com.sealhackathon.api.events.dto.request.UpdateEventRequest;
import com.sealhackathon.api.events.dto.response.EventResponse;
import com.sealhackathon.api.events.entity.Event;
import com.sealhackathon.api.events.mapper.EventMapper;
import com.sealhackathon.api.events.repository.EventRepository;
import com.sealhackathon.api.events.service.EventScheduleValidator;
import com.sealhackathon.api.events.service.EventService;
import com.sealhackathon.api.events.service.HackathonTimelineService;
import com.sealhackathon.api.events.support.EventTimeline;
import com.sealhackathon.api.events.value_object.EventType;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import com.sealhackathon.api.hackathons.support.HackathonArchiveGuard;
import com.sealhackathon.api.notifications.entity.Notification;
import com.sealhackathon.api.notifications.repository.NotificationRepository;
import com.sealhackathon.api.notifications.service.NotificationService;
import com.sealhackathon.api.notifications.service.StakeholderBroadcastService;
import com.sealhackathon.api.notifications.value_object.NotificationType;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.repository.UserRepository;
import com.sealhackathon.api.users.value_object.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * FR-06A Event CRUD impl. Gọi {@link EventScheduleValidator} mọi mutation.
 * REMINDER fan-out tới user APPROVED khi {@code isPublic=true}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final HackathonRepository hackathonRepository;
    private final EventMapper eventMapper;
    private final AuditService auditService;
    private final EventScheduleValidator scheduleValidator;
    private final HackathonTimelineService hackathonTimelineService;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final HackathonArchiveGuard archiveGuard;
    private final StakeholderBroadcastService stakeholderBroadcastService;

    @Override
    public CreateResult create(Integer hackathonId, CreateEventRequest req) {
        Hackathon h = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));
        archiveGuard.assertNotArchived(h);
        assertCreateDependencies(hackathonId, req.getType());

        scheduleValidator.validateBlocking(h, req, 0);
        List<Warning> warnings = scheduleValidator.computeLayer3Warnings(h, req);

        Event entity = eventMapper.toEntity(req, h);
        Integer uid = currentUserAccessor.currentUserId();
        if (uid != null) {
            entity.setCreatedBy(User.builder().id(uid).build());
        }
        Event saved = eventRepository.save(entity);
        EventResponse response = eventMapper.toResponse(saved);

        auditService.log(AuditAction.EVENT_CREATE, "events", saved.getId(),
                Map.of("hackathonId", hackathonId, "snapshot", response));
        for (Warning w : warnings) {
            auditService.log(AuditAction.WARNING_EVENT_ORDER, "events", saved.getId(),
                    Map.of("code", w.getCode(), "message", w.getMessage(),
                           "details", w.getDetails() == null ? Map.of() : w.getDetails()));
        }
        if (Boolean.TRUE.equals(saved.getIsPublic())) {
            fanoutReminder(saved, true);
        }
        if (saved.getType() == EventType.KICKOFF) {
            hackathonTimelineService.assertAllRoundsExamAtValid(hackathonId);
        }
        return new CreateResult(response, warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> listByHackathon(Integer hackathonId, EventType type,
                                               LocalDateTime from, LocalDateTime to, Boolean isPublic) {
        List<Event> events;
        if (type != null) {
            events = eventRepository.findByHackathonIdAndType(hackathonId, type);
        } else if (from != null && to != null) {
            events = eventRepository.findInRange(hackathonId, from, to);
        } else {
            events = eventRepository.findByHackathonIdOrderByStartsAtAsc(hackathonId);
        }
        return events.stream()
                .filter(e -> isPublic == null || isPublic.equals(e.getIsPublic()))
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getById(Integer id) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        return eventMapper.toResponse(e);
    }

    @Override
    public UpdateResult update(Integer id, UpdateEventRequest req) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        Hackathon h = e.getHackathon();
        archiveGuard.assertNotArchived(h);
        scheduleValidator.validateBlocking(h, req, id);
        List<Warning> warnings = scheduleValidator.computeLayer3Warnings(h, req);

        EventResponse before = eventMapper.toResponse(e);
        LocalDateTime prevStart = e.getStartsAt();
        String prevLocation = e.getLocation();
        String prevBuffetLocation = e.getBuffetLocation();
        LocalDateTime prevBuffetStartsAt = e.getBuffetStartsAt();
        LocalDateTime prevBuffetEndsAt = e.getBuffetEndsAt();
        eventMapper.applyUpdate(e, req);
        if (!e.getStartsAt().equals(prevStart)) {
            e.setReminderSentAt(null);
        }
        Event saved = eventRepository.save(e);
        EventResponse after = eventMapper.toResponse(saved);

        auditService.logBeforeAfter(AuditAction.EVENT_UPDATE, "events", saved.getId(), before, after);
        for (Warning w : warnings) {
            auditService.log(AuditAction.WARNING_EVENT_ORDER, "events", saved.getId(),
                    Map.of("code", w.getCode(), "message", w.getMessage(),
                           "details", w.getDetails() == null ? Map.of() : w.getDetails()));
        }
        boolean startsChanged = !saved.getStartsAt().equals(prevStart);
        boolean locationChanged = !Objects.equals(saved.getLocation(), prevLocation);
        boolean buffetChanged = !Objects.equals(saved.getBuffetLocation(), prevBuffetLocation)
                || !Objects.equals(saved.getBuffetStartsAt(), prevBuffetStartsAt)
                || !Objects.equals(saved.getBuffetEndsAt(), prevBuffetEndsAt);
        if (Boolean.TRUE.equals(saved.getIsPublic())) {
            if (isStakeholderMilestone(saved.getType())) {
                fanoutReminder(saved, startsChanged || locationChanged || buffetChanged);
            } else if (startsChanged) {
                fanoutReminder(saved, false);
            }
        }
        if (saved.getType() == EventType.KICKOFF) {
            hackathonTimelineService.assertAllRoundsExamAtValid(h.getId());
        }
        return new UpdateResult(after, warnings);
    }

    @Override
    public Integer delete(Integer id) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        archiveGuard.assertNotArchived(e.getHackathon());
        assertDeleteDependencies(e);
        EventResponse snapshot = eventMapper.toResponse(e);
        Integer hackathonId = e.getHackathon().getId();

        List<Notification> stale = notificationRepository.findByReferenceTypeAndReferenceId("events", id);
        if (!stale.isEmpty()) {
            notificationRepository.deleteAll(stale);
        }
        eventRepository.delete(e);

        auditService.log(AuditAction.EVENT_DELETE, "events", id,
                Map.of("snapshot", snapshot, "notificationCleanup", stale.size()));
        // Chỉ revalidate round khi xóa KICKOFF — đây là event duy nhất còn ràng buộc với round.examAt.
        // AWARDS và WORKSHOP không có ràng buộc chéo với round.examAt nên không cần revalidate.
        if (e.getType() == EventType.KICKOFF) {
            hackathonTimelineService.assertAllRoundsExamAtValid(hackathonId);
        }
        return id;
    }

    private void assertCreateDependencies(Integer hackathonId, EventType type) {
        if (type == EventType.WORKSHOP
                && !eventRepository.existsByHackathonIdAndType(hackathonId, EventType.KICKOFF)) {
            throw new BusinessRuleException(ErrorCode.EVENT_KICKOFF_MISSING,
                    "Phải tạo KICKOFF trước khi tạo WORKSHOP",
                    Map.of("hackathonId", hackathonId, "requiredType", "KICKOFF"));
        }
        if (type == EventType.AWARDS
                && !eventRepository.existsByHackathonIdAndType(hackathonId, EventType.WORKSHOP)) {
            throw new BusinessRuleException(ErrorCode.EVENT_ORDER_VIOLATION,
                    "Phải tạo WORKSHOP trước khi tạo AWARDS",
                    Map.of("hackathonId", hackathonId, "requiredType", "WORKSHOP"));
        }
    }

    private void assertDeleteDependencies(Event event) {
        Integer hackathonId = event.getHackathon().getId();
        Integer eventId = event.getId();
        switch (event.getType()) {
            case KICKOFF -> {
                if (hasOtherMilestone(hackathonId, EventType.WORKSHOP, eventId)
                        || hasOtherMilestone(hackathonId, EventType.AWARDS, eventId)) {
                    throw new BusinessRuleException(ErrorCode.EVENT_ORDER_VIOLATION,
                            "Không thể xóa KICKOFF khi còn WORKSHOP hoặc AWARDS",
                            Map.of("eventId", eventId, "hackathonId", hackathonId));
                }
            }
            case WORKSHOP -> {
                if (hasOtherMilestone(hackathonId, EventType.AWARDS, eventId)) {
                    throw new BusinessRuleException(ErrorCode.EVENT_ORDER_VIOLATION,
                            "Không thể xóa WORKSHOP khi còn AWARDS",
                            Map.of("eventId", eventId, "hackathonId", hackathonId));
                }
            }
            default -> {
                // AWARDS và các loại khác — cho phép xóa
            }
        }
    }

    private boolean hasOtherMilestone(Integer hackathonId, EventType type, Integer excludeEventId) {
        return eventRepository.findByHackathonIdAndType(hackathonId, type).stream()
                .anyMatch(e -> !e.getId().equals(excludeEventId));
    }

    private void fanoutReminder(Event event, boolean sendEmail) {
        String title = "Sự kiện sắp diễn ra: %s".formatted(event.getTitle());
        String body = buildReminderBody(event);
        if (isStakeholderMilestone(event.getType())) {
            Integer hackathonId = event.getHackathon() != null ? event.getHackathon().getId() : null;
            stakeholderBroadcastService.broadcast(
                    hackathonId,
                    NotificationType.EVENT_REMINDER,
                    title,
                    body,
                    "events",
                    event.getId(),
                    sendEmail);
            return;
        }
        List<User> users = userRepository.findAllByStatus(UserStatus.APPROVED);
        notificationService.sendBatch(
                users,
                NotificationType.EVENT_REMINDER,
                title,
                body,
                "events", event.getId());
    }

    static String buildReminderBody(Event event) {
        StringBuilder body = new StringBuilder("Thời gian: %s%s".formatted(
                event.getStartsAt(),
                event.getLocation() == null || event.getLocation().isBlank()
                        ? "" : " — " + event.getLocation()));
        if (hasBuffetInfo(event)) {
            body.append("\nBuffet");
            if (event.getBuffetLocation() != null && !event.getBuffetLocation().isBlank()) {
                body.append(": ").append(event.getBuffetLocation());
            }
            if (event.getBuffetStartsAt() != null || event.getBuffetEndsAt() != null) {
                body.append(" (")
                        .append(event.getBuffetStartsAt() != null ? event.getBuffetStartsAt() : "…")
                        .append(" – ")
                        .append(event.getBuffetEndsAt() != null ? event.getBuffetEndsAt() : "…")
                        .append(")");
            }
        }
        return body.toString();
    }

    private static boolean hasBuffetInfo(Event event) {
        return (event.getBuffetLocation() != null && !event.getBuffetLocation().isBlank())
                || event.getBuffetStartsAt() != null
                || event.getBuffetEndsAt() != null;
    }

    private static boolean isStakeholderMilestone(EventType type) {
        return type == EventType.KICKOFF
                || type == EventType.WORKSHOP
                || type == EventType.AWARDS;
    }
}
