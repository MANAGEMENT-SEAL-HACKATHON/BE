package com.sealhackathon.api.events.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.sealhackathon.api.events.dto.request.CreateEventRequest;
import com.sealhackathon.api.events.dto.request.UpdateEventRequest;
import com.sealhackathon.api.events.dto.response.EventResponse;
import com.sealhackathon.api.events.service.EventService;
import com.sealhackathon.api.events.service.EventScheduleValidator;
import com.sealhackathon.api.events.value_object.EventType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * FR-06A — Event controller. Validate 3 lớp ở {@link EventScheduleValidator}.
 * GET list/detail: Coordinator hoặc Student (APPROVED). Mutations: Coordinator only.
 */
@Tag(name = "Events", description = "FR-06 — Lịch sự kiện (WORKSHOP, KICKOFF, …)")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/api/v1/hackathons/{hackathonId}/events")
    @CoordinatorOnly
    @Operation(summary = "Tạo event mới cho hackathon", description = "Chỉ coordinator mới có quyền thực hiện hành động này.")
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
    @PreAuthorize("(hasAnyRole('COORDINATOR', 'STUDENT')) and authentication.principal.status.name() == 'APPROVED'")
    @Operation(summary = "Danh sách event của hackathon", description = "Coordinator hoặc Student APPROVED. Có thể lọc theo type, khoảng thời gian (from-to), và isPublic.")
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
    @PreAuthorize("(hasAnyRole('COORDINATOR', 'STUDENT')) and authentication.principal.status.name() == 'APPROVED'")
    @Operation(summary = "Lấy thông tin chi tiết event theo ID", description = "Coordinator hoặc Student APPROVED. Trả về lỗi 404 nếu không tìm thấy.")
    public ResponseEntity<ApiResponse<EventResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(eventService.getById(id)));
    }

    @PutMapping("/api/v1/events/{id}")
    @CoordinatorOnly
    @Operation(summary = "Cập nhật thông tin event", description = "Chỉ coordinator mới có quyền thực hiện hành động này. Trả về lỗi 404 nếu không tìm thấy event với ID đã cho.")
    public ResponseEntity<ApiResponse<EventResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateEventRequest req
    ) {
        EventService.UpdateResult result = eventService.update(id, req);
        return ResponseEntity.ok(ApiResponse.okWithWarnings(result.event(), result.warnings()));
    }

    @DeleteMapping("/api/v1/events/{id}")
    @CoordinatorOnly
    @Operation(summary = "Xóa event", description = "Chỉ coordinator mới có quyền thực hiện hành động này. Trả về lỗi 404 nếu không tìm thấy event với ID đã cho.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Integer id) {
        Integer deletedId = eventService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("deletedId", deletedId), "Deleted"));
    }
}
