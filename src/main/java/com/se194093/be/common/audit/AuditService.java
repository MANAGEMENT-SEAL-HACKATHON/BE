package com.se194093.be.common.audit;

import tools.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Service ghi {@code audit_logs} từ tầng service nghiệp vụ.
 *
 * <p>Mọi mutation MF-01 (CREATE/UPDATE/DELETE/STATUS_CHANGE) PHẢI gọi 1 trong các overload
 * dưới đây trong cùng {@code @Transactional} với mutation chính, để rollback nguyên khối khi lỗi.
 *
 * <p>Action code lấy từ {@link AuditAction}. {@code targetTable} = tên bảng (vd "hackathons"),
 * {@code targetId} = id record bị tác động. {@code detail} là snapshot JSON tuỳ chọn.
 */
public interface AuditService {

    void log(String action, String targetTable, Integer targetId);

    void log(String action, String targetTable, Integer targetId, JsonNode detail);

    void log(String action, String targetTable, Integer targetId, Map<String, Object> detail);

    /**
     * Ghi audit với user thực hiện explicit (vd khi worker async ghi log thay user).
     */
    void logAs(Integer userId, String action, String targetTable, Integer targetId,
               Map<String, Object> detail);
}
