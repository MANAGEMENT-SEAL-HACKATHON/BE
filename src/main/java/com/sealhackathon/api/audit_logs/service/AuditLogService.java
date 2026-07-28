package com.sealhackathon.api.audit_logs.service;

import java.util.Map;

/**
 * AUDIT-RO — coordinator đọc audit log (hackathon mình / action / own user).
 */
public interface AuditLogService {

    /**
     * @param userId      current coordinator id
     * @param hackathonId optional filter; when set, ownership is enforced
     * @param action      optional action filter
     * @param page        zero-based page
     * @param size        page size
     * @return map with keys {@code items}, {@code page}, {@code size}, {@code total}
     */
    Map<String, Object> list(Integer userId, Integer hackathonId, String action, int page, int size);
}
