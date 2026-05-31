package com.sealhackathon.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Một mục cảnh báo mềm (soft warning) gắn kèm response 2xx.
 *
 * <p>Không phải lỗi — endpoint vẫn thành công. Client (UI) hiển thị toast vàng hoặc
 * yêu cầu Coordinator xác nhận trước khi đi tiếp.
 *
 * <p>Convention {@code code}:
 * <ul>
 *   <li><b>WEIGHT_NOT_ONE</b> — tổng weight Criteria của Round chưa = 1.0 (FR-04)</li>
 *   <li><b>MENTOR_JUDGE_CONFLICT</b> — user đang là Mentor của Track / Judge của Round trong cùng Track (FR-05)</li>
 *   <li><b>CONFLICT_CHECK_SKIPPED</b> — bảng đối chiếu rỗng, không thể check 2 chiều (FR-05)</li>
 *   <li><b>JUDGE_FINAL_ROUND_AT_PHASE1</b> — phân công Judge cho Round Chung kết tại GĐ1 (FR-05c)</li>
 *   <li><b>EVENT_ORDER_INVALID</b> — Lớp 3 thứ tự sự kiện (FR-06A)</li>
 *   <li><b>READINESS_WARNING</b> — cảnh báo mềm tổng hợp tại {@code GET /hackathons/{id}/readiness}</li>
 *   <li>MF-03 — xem {@link WarningCode}</li>
 * </ul>
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Warning {

    private final String code;
    private final String message;
    private final Map<String, Object> details;

    public static Warning of(String code, String message) {
        return Warning.builder().code(code).message(message).build();
    }

    public static Warning of(String code, String message, Map<String, Object> details) {
        return Warning.builder().code(code).message(message).details(details).build();
    }
}
