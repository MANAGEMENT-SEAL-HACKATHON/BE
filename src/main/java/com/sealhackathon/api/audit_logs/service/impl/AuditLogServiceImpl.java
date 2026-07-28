package com.sealhackathon.api.audit_logs.service.impl;

import com.sealhackathon.api.audit_logs.entity.AuditLog;
import com.sealhackathon.api.audit_logs.repository.AuditLogRepository;
import com.sealhackathon.api.audit_logs.service.AuditLogService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.hackathons.entity.Hackathon;
import com.sealhackathon.api.hackathons.repository.HackathonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final HackathonRepository hackathonRepository;

    @Override
    public Map<String, Object> list(Integer userId, Integer hackathonId, String action, int page, int size) {
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
        return body;
    }
}
