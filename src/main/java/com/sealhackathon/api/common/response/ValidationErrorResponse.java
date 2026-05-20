package com.sealhackathon.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Response 400 / 422 khi Bean Validation thất bại.
 *
 * <p>Format:
 * <pre>
 * {
 *   "success": false,
 *   "error": {
 *     "code":    "VALIDATION_FAILED",
 *     "message": "Yêu cầu không hợp lệ",
 *     "status":  400,
 *     "fields": [
 *        { "field": "name", "message": "must not be blank", "rejectedValue": null }
 *     ]
 *   }
 * }
 * </pre>
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationErrorResponse {

    private final boolean success;
    private final ValidationErrorPayload error;
    private final String traceId;
    private final Instant timestamp;

    @Getter
    @Builder
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationErrorPayload {
        private final String code;
        private final String message;
        private final int status;
        private final List<FieldError> fields;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        private final String field;
        private final String message;
        private final Object rejectedValue;
    }
}
