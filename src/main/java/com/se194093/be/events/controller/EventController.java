package com.se194093.be.events.controller;

import com.se194093.be.common.response.ApiResponse;
import com.se194093.be.common.security.CoordinatorOnly;
import com.se194093.be.events.dto.request.CreateEventRequest;
import com.se194093.be.events.dto.request.UpdateEventRequest;
import com.se194093.be.events.dto.response.EventResponse;
import com.se194093.be.events.service.EventService;
import com.se194093.be.events.value_object.EventType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * FR-06A — Event controller. Validate 3 lớp ở {@link com.se194093.be.events.service.EventScheduleValidator}.
 */
@RestController
@RequiredArgsConstructor
@CoordinatorOnly
public class EventController {

    private final EventService eventService;

    @PostMapping("/api/v1/hackathons/{hackathonId}/events")
    public ResponseEntity<ApiResponse<EventResponse>> create(
            @PathVariable Integer hackathonId,
            @Valid @RequestBody CreateEventRequest req
    ) {
        EventService.CreateResult result = eventService.create(hackathonId, req);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/events/{id}")
                .buildAndExpand(result.event().getId())
                .toUri();
        return ResponseEntity.created(location).body(
                ApiResponse.createdWithWarnings(result.event(), result.warnings())
        );
    }

    @GetMapping("/api/v1/hackathons/{hackathonId}/events")
    public ResponseEntity<ApiResponse<List<EventResponse>>> listByHackathon(
            @PathVariable Integer hackathonId,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Boolean isPublic
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                eventService.listByHackathon(hackathonId, type, from, to, isPublic)
        ));
    }

    @GetMapping("/api/v1/events/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getById(id)));
    }

    @PutMapping("/api/v1/events/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateEventRequest req
    ) {
        EventService.UpdateResult result = eventService.update(id, req);
        return ResponseEntity.ok(ApiResponse.okWithWarnings(result.event(), result.warnings()));
    }

    @DeleteMapping("/api/v1/events/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer id) {
        Integer deletedId = eventService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", deletedId), "Deleted"));
    }
}
