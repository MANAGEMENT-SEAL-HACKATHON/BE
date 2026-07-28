package com.sealhackathon.api.audit_logs.controller;

import com.sealhackathon.api.audit_logs.service.AuditLogService;
import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Audit RO for coordinator (own hackathons). Mentor/Student/Judge → 403 via CoordinatorOnly.
 */
@Tag(name = "Audit Logs (RO)")
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@CoordinatorOnly
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping
    @Operation(summary = "AUDIT-RO — đọc audit log (coordinator hackathon mình)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(required = false) Integer hackathonId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Integer userId = currentUserAccessor.currentUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                auditLogService.list(userId, hackathonId, action, page, size)));
    }
}
