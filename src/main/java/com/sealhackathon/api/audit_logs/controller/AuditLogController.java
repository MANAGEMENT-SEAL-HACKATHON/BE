package com.sealhackathon.api.audit_logs.controller;

import com.sealhackathon.api.audit_logs.entity.AuditLog;
import com.sealhackathon.api.audit_logs.repository.AuditLogRepository;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
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

    private final AuditLogRepository auditLogRepository;
    private final HackathonRepository hackathonRepository;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping
    @Operation(summary = "AUDIT-RO — đọc audit log (coordinator hackathon mình)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(required = false) Integer hackathonId,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Integer userId = currentUserAccessor.currentUserId();
        if (hackathonId != null) {
            Hackathon h = hackathonRepository.findById(hackathonId).orElse(null);
            if (h == null || h.getCreatedBy() == null || !h.getCreatedBy().getId().equals(userId)) {
                throw new AuthException(ErrorCode.FORBIDDEN, "Không có quyền đọc audit hackathon này",
                        HttpStatus.FORBIDDEN);
            }
        }

        Page<AuditLog> result;
        if (StringUtils.hasText(action)) {
            result = auditLogRepository.findByActionOrderByCreatedAtDesc(action, PageRequest.of(page, size));
        } else if (hackathonId != null) {
            result = auditLogRepository.findByTargetTableAndTargetIdOrderByCreatedAtDesc(
                    "hackathons", hackathonId, PageRequest.of(page, size));
        } else {
            result = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("items", result.getContent());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        body.put("total", result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.ok(body));
    }
}
