package com.se194093.be.common.audit;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.se194093.be.audit_logs.entity.AuditLog;
import com.se194093.be.audit_logs.repository.AuditLogRepository;
import com.se194093.be.common.security.CurrentUserAccessor;
import com.se194093.be.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Skeleton impl của {@link AuditService}.
 *
 * <p>TODO (sẽ hoàn thiện ở phase implement nghiệp vụ):
 * <ul>
 *   <li>Lấy {@code ipAddress} từ {@code HttpServletRequest} (cần inject HttpServletRequest holder)</li>
 *   <li>Đặt propagation REQUIRES_NEW cho action nhạy cảm (vd security failure)</li>
 *   <li>Bắn audit event qua Spring Events nếu muốn tách thành async listener</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUserAccessor currentUserAccessor;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void log(String action, String targetTable, Integer targetId) {
        persist(currentUserId(), action, targetTable, targetId, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void log(String action, String targetTable, Integer targetId, JsonNode detail) {
        persist(currentUserId(), action, targetTable, targetId, detail);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void log(String action, String targetTable, Integer targetId, Map<String, Object> detail) {
        JsonNode node = detail == null ? null : objectMapper.valueToTree(detail);
        persist(currentUserId(), action, targetTable, targetId, node);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void logAs(Integer userId, String action, String targetTable, Integer targetId,
                      Map<String, Object> detail) {
        JsonNode node = detail == null ? null : objectMapper.valueToTree(detail);
        persist(userId, action, targetTable, targetId, node);
    }

    private void persist(Integer userId, String action, String targetTable, Integer targetId, JsonNode detail) {
        try {
            AuditLog entry = AuditLog.builder()
                    .user(userId == null ? null : User.builder().id(userId).build())
                    .action(action)
                    .targetTable(targetTable)
                    .targetId(targetId)
                    .detail(detail)
                    .createdAt(LocalDateTime.now())
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("[AUDIT] Failed to persist audit_log action={} target={}#{}: {}",
                    action, targetTable, targetId, ex.getMessage());
        }
    }

    private Integer currentUserId() {
        try {
            return currentUserAccessor.currentUserId();
        } catch (Exception ex) {
            return null;
        }
    }
}
