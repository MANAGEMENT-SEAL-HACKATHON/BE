package com.se194093.be.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 422 Unprocessable Entity — payload đúng cấu trúc nhưng vi phạm business rule.
 *
 * <p>Ví dụ áp dụng:
 * <ul>
 *   <li>FR-01: event_start &lt; registration_end</li>
 *   <li>FR-03: submission_deadline &lt;= submission_open / &lt; NOW(); force_locked=TRUE mà thiếu reason</li>
 *   <li>FR-06: Gate cứng — tổng weight Criteria ≠ 1.0 ở một hoặc nhiều Round (kèm details liệt kê Round vi phạm)</li>
 *   <li>FR-06A: Lớp 1+2 — event ngoài khung Hackathon hoặc trùng giờ</li>
 *   <li>FR-06B: ABS(SUM(weight) - 1.0) &gt; 0.001 khi activate Round</li>
 * </ul>
 */
public class BusinessRuleException extends BaseException {

    public BusinessRuleException(String code, String message) {
        super(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessRuleException(String code, String message, Map<String, Object> details) {
        super(code, message, HttpStatus.UNPROCESSABLE_ENTITY, details);
    }
}
