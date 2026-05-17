package com.se194093.be.events.service.impl;

import com.se194093.be.common.audit.AuditAction;
import com.se194093.be.common.audit.AuditService;
import com.se194093.be.common.exception.ResourceNotFoundException;
import com.se194093.be.common.response.Warning;
import com.se194093.be.common.security.CurrentUserAccessor;
import com.se194093.be.events.dto.request.CreateEventRequest;
import com.se194093.be.events.dto.request.UpdateEventRequest;
import com.se194093.be.events.dto.response.EventResponse;
import com.se194093.be.events.entity.Event;
import com.se194093.be.events.mapper.EventMapper;
import com.se194093.be.events.repository.EventRepository;
import com.se194093.be.events.service.EventScheduleValidator;
import com.se194093.be.events.service.EventService;
import com.se194093.be.events.value_object.EventType;
import com.se194093.be.hackathons.entity.Hackathon;
import com.se194093.be.hackathons.repository.HackathonRepository;
import com.se194093.be.notifications.entity.Notification;
import com.se194093.be.notifications.repository.NotificationRepository;
import com.se194093.be.notifications.service.NotificationService;
import com.se194093.be.users.entity.User;
import com.se194093.be.users.repository.UserRepository;
import com.se194093.be.users.value_object.UserStatus;
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
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserAccessor currentUserAccessor;

    @Override
    public CreateResult create(Integer hackathonId, CreateEventRequest req) {
        Hackathon h = hackathonRepository.findById(hackathonId)
                .orElseThrow(() -> new ResourceNotFoundException("Hackathon", hackathonId));

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
        return new UpdateResult(after, warnings);
    }

    @Override
    public Integer delete(Integer id) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        EventResponse snapshot = eventMapper.toResponse(e);

        List<Notification> stale = notificationRepository.findByReferenceTypeAndReferenceId("events", id);
        if (!stale.isEmpty()) {
            notificationRepository.deleteAll(stale);
        }
        eventRepository.delete(e);

        auditService.log(AuditAction.EVENT_DELETE, "events", id,
                Map.of("snapshot", snapshot, "notificationCleanup", stale.size()));
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
