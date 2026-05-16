package com.se194093.be.events.service.impl;

import com.se194093.be.events.dto.request.CreateEventRequest;
import com.se194093.be.events.dto.request.UpdateEventRequest;
import com.se194093.be.events.dto.response.EventResponse;
import com.se194093.be.events.service.EventService;
import com.se194093.be.events.value_object.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skeleton — TODO Dev implement theo {@code docs/api/mf-01/fr-06a-events.md}.
 *
 * <p>Inject: EventRepository, HackathonRepository, EventMapper, AuditService, EventScheduleValidator,
 * UserRepository (lấy danh sách APPROVED cho REMINDER fan-out), NotificationRepository,
 * CurrentUserAccessor.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    @Override
    public CreateResult create(Integer hackathonId, CreateEventRequest req) {
        // TODO Dev:
        //  1. h = hackathonRepo.findById(hackathonId) or 404
        //  2. scheduleValidator.validateBlocking(h, req, 0)
        //  3. warnings = scheduleValidator.computeLayer3Warnings(h, req)
        //  4. entity = mapper.toEntity(req, h); entity.createdBy = currentUserRef
        //  5. saved = eventRepo.save(entity)
        //  6. audit.log(EVENT_CREATE, "events", saved.id, snapshot(saved))
        //  7. for w in warnings: audit.log(WARNING_EVENT_ORDER, "events", saved.id, w.details)
        //  8. if saved.isPublic: enqueueRemindersAsync(saved)
        //  9. return CreateResult(mapper.toResponse(saved), warnings)
        throw new UnsupportedOperationException("FR-06A POST /events - to be implemented");
    }

    @Override
    public List<EventResponse> listByHackathon(Integer hackathonId, EventType type,
                                               LocalDateTime from, LocalDateTime to, Boolean isPublic) {
        // TODO Dev:
        //   - if type != null: repo.findByHackathonIdAndType(hackathonId, type)
        //   - elif from != null && to != null: repo.findInRange(hackathonId, from, to)
        //   - else: repo.findByHackathonIdOrderByStartsAtAsc(hackathonId)
        //   - filter isPublic nếu truyền
        //   - map → response
        throw new UnsupportedOperationException("FR-06A GET /events - to be implemented");
    }

    @Override
    public EventResponse getById(Integer id) {
        throw new UnsupportedOperationException("FR-06A GET /events/{id} - to be implemented");
    }

    @Override
    public UpdateResult update(Integer id, UpdateEventRequest req) {
        // TODO Dev:
        //  - findById → 404
        //  - scheduleValidator.validateBlocking(h, req, id) (exclude id từ overlap check)
        //  - warnings = scheduleValidator.computeLayer3Warnings(h, req)
        //  - applyUpdate; save; audit EVENT_UPDATE {before, after}
        //  - notification: optionally re-send REMINDER nếu đổi startsAt
        throw new UnsupportedOperationException("FR-06A PUT /events/{id} - to be implemented");
    }

    @Override
    public Integer delete(Integer id) {
        // TODO Dev:
        //  - findById → 404
        //  - delete; audit EVENT_DELETE snapshot
        //  - delete notifications referencing event id (best-effort)
        throw new UnsupportedOperationException("FR-06A DELETE /events/{id} - to be implemented");
    }
}
