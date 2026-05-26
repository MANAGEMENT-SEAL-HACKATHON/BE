package com.sealhackathon.api.events.service.impl;

import com.sealhackathon.api.common.audit.AuditAction;
import com.sealhackathon.api.common.audit.AuditService;
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

    @Override
    public CreateResult create(Integer hackathonId, CreateEventRequest req) {
        Hackathon h = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));
        archiveGuard.assertNotArchived(h);

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
            fanoutReminder(saved);
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
        eventMapper.applyUpdate(e, req);
        Event saved = eventRepository.save(e);
        EventResponse after = eventMapper.toResponse(saved);

        auditService.logBeforeAfter(AuditAction.EVENT_UPDATE, "events", saved.getId(), before, after);
        for (Warning w : warnings) {
            auditService.log(AuditAction.WARNING_EVENT_ORDER, "events", saved.getId(),
                    Map.of("code", w.getCode(), "message", w.getMessage(),
                           "details", w.getDetails() == null ? Map.of() : w.getDetails()));
        }
        if (Boolean.TRUE.equals(saved.getIsPublic()) && !saved.getStartsAt().equals(prevStart)) {
            fanoutReminder(saved);
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

    private void fanoutReminder(Event event) {
        List<User> users = userRepository.findAllByStatus(UserStatus.APPROVED);
        notificationService.sendBatch(
                users,
                "EVENT_REMINDER",
                "Sự kiện sắp diễn ra: %s".formatted(event.getTitle()),
                "Thời gian: %s%s".formatted(
                        event.getStartsAt(),
                        event.getLocation() == null ? "" : " — " + event.getLocation()),
                "events", event.getId());
    }
}
