package com.sealhackathon.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Chuẩn response cho mọi response 4xx / 5xx.
 *
 * <p>Format:
 * <pre>
 * {
 *   "success": false,
 *   "error": {
 *     "code":    "HACKATHON_DUPLICATE",
 *     "message": "Kỳ thi đã tồn tại",
 *     "status":  409,
 *     "details": { ... }
 *   },
 *   "traceId":   "uuid",
 *   "timestamp": "2026-05-16T09:57:00Z"
 * }
 * </pre>
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final boolean success;
    private final ErrorPayload error;
    /** Gate ONGOING fail (mf01 §8.5) — song song với {@link ErrorPayload#details}. */
    private final List<GateErrorItem> errors;
    private final String traceId;
    private final Instant timestamp;

    public static ErrorResponse of(String code, String message, int status) {
        return ErrorResponse.builder()
                .success(false)
                .error(ErrorPayload.builder().code(code).message(message).status(status).build())
                .timestamp(Instant.now())
                .build();
    }

    public static ErrorResponse of(String code, String message, int status, Map<String, Object> details) {
        return ErrorResponse.builder()
                .success(false)
                .error(ErrorPayload.builder().code(code).message(message).status(status).details(details).build())
                .timestamp(Instant.now())
                .build();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorPayload {
        private final String code;
        private final String message;
        private final int status;
        private final Map<String, Object> details;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GateErrorItem {
        private final String code;
        private final String message;
        private final Map<String, Object> details;
    }
}
