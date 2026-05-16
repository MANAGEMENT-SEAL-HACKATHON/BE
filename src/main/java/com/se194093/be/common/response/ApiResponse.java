package com.se194093.be.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Chuẩn response envelope cho mọi endpoint 2xx trong MF-01.
 *
 * <p>Format:
 * <pre>
 * {
 *   "success": true,
 *   "data":    {...} | [...],
 *   "message": "Optional human-readable message",
 *   "warnings": [ { code, message, ... } ],
 *   "traceId": "uuid",
 *   "timestamp": "2026-05-16T09:57:00Z"
 * }
 * </pre>
 *
 * <p>Trường {@code warnings} là cảnh báo mềm — KHÔNG block luồng nghiệp vụ. Áp dụng cho:
 * <ul>
 *   <li>FR-04: tổng weight Criteria chưa đủ 1.0 (Bước 4)</li>
 *   <li>FR-05: conflict Mentor ↔ Judge (2 chiều)</li>
 *   <li>FR-06A: Lớp 3 — thứ tự sự kiện sai logic</li>
 *   <li>FR-05c: phân công Judge vào Round Chung kết tại GĐ1</li>
 * </ul>
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final List<Warning> warnings;
    private final String traceId;
    private final Instant timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> okWithWarnings(T data, List<Warning> warnings) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .warnings(warnings == null || warnings.isEmpty() ? null : warnings)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message("Created")
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> createdWithWarnings(T data, List<Warning> warnings) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message("Created")
                .warnings(warnings == null || warnings.isEmpty() ? null : warnings)
                .timestamp(Instant.now())
                .build();
    }
}
