package com.sealhackathon.api.events.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.events.dto.request.BuffetMenuItemRequest;
import com.sealhackathon.api.events.dto.response.BuffetMenuItemResponse;
import com.sealhackathon.api.events.service.BuffetMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Events", description = "FR-06 — Lịch sự kiện (WORKSHOP, KICKOFF, BUFFET, …)")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/events/{id}/buffet-menu")
@RequiredArgsConstructor
public class BuffetMenuController {

    private final BuffetMenuService buffetMenuService;

    @GetMapping
    @PreAuthorize("(hasAnyRole('COORDINATOR', 'STUDENT')) and authentication.principal.status.name() == 'APPROVED'")
    @Operation(summary = "Xem thực đơn buffet",
            description = "Coordinator luôn xem được; Student chỉ khi event isPublic=true.")
    public ResponseEntity<ApiResponse<List<BuffetMenuItemResponse>>> list(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(buffetMenuService.listByEvent(id)));
    }

    @PutMapping
    @CoordinatorOnly
    @Operation(summary = "Thay thế toàn bộ thực đơn buffet",
            description = "Replace-all. Khóa sau khi sơ loại đã publish (BUFFET_LOCKED_AFTER_PUBLISH).")
    public ResponseEntity<ApiResponse<List<BuffetMenuItemResponse>>> replace(
            @PathVariable Integer id,
            @Valid @RequestBody List<BuffetMenuItemRequest> items) {
        return ResponseEntity.ok(ApiResponse.ok(buffetMenuService.replaceMenu(id, items)));
    }
}
