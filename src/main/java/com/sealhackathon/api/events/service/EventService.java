package com.sealhackathon.api.events.service;

import com.sealhackathon.api.common.response.Warning;
import com.sealhackathon.api.events.dto.request.CreateEventRequest;
import com.sealhackathon.api.events.dto.request.UpdateEventRequest;
import com.sealhackathon.api.events.dto.response.EventResponse;
import com.sealhackathon.api.events.value_object.EventType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FR-06A — CRUD Event + REMINDER fan-out.
 *
 * <p>Mọi mutation gọi {@link EventScheduleValidator} trước khi save. Sau khi save thành công:
 * enqueue notification {@code REMINDER} cho mọi user APPROVED nếu event {@code isPublic = true}.
 *
 * <p>Audit: {@code EVENT_CREATE}, {@code EVENT_UPDATE}, {@code EVENT_DELETE},
 * {@code WARNING_EVENT_ORDER}.
 */
public interface EventService {

    record CreateResult(EventResponse event, List<Warning> warnings) {}

    record UpdateResult(EventResponse event, List<Warning> warnings) {}

    CreateResult create(Integer hackathonId, CreateEventRequest req);

    List<EventResponse> listByHackathon(Integer hackathonId, EventType type,
                                        LocalDateTime from, LocalDateTime to, Boolean isPublic);

    EventResponse getById(Integer id);

    UpdateResult update(Integer id, UpdateEventRequest req);

    Integer delete(Integer id);
}
