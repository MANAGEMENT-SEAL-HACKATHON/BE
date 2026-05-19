package com.sealhackathon.api.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 409 Conflict — vi phạm ràng buộc UNIQUE, cố xóa khi còn child, state machine sai.
 *
 * <p>Ví dụ áp dụng:
 * <ul>
 *   <li>FR-01: tạo Hackathon trùng (name, season, year)</li>
 *   <li>FR-02: xóa Track còn team ACTIVE/PENDING hoặc Round đang active</li>
 *   <li>FR-03: set is_active=TRUE khi đã có Round khác active trong cùng Track</li>
 *   <li>FR-04: sửa/xóa Criteria khi đã có scores</li>
 *   <li>FR-05: phân công Mentor/Judge trùng (mentor_id, track_id) / (judge_id, round_id)</li>
 *   <li>FR-06: transition status sai chiều (vd ONGOING → DRAFT)</li>
 * </ul>
 */
public class ConflictException extends BaseException {

    public ConflictException(String code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }

    public ConflictException(String code, String message, Map<String, Object> details) {
        super(code, message, HttpStatus.CONFLICT, details);
    }
}
