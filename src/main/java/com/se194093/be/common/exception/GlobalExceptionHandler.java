package com.se194093.be.common.exception;

import com.se194093.be.common.response.ErrorResponse;
import com.se194093.be.common.response.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Bắt mọi exception phát sinh trong controller và ánh xạ về response envelope chuẩn.
 *
 * <p>Quy ước:
 * <ul>
 *   <li>{@link BaseException} → status từ exception, code từ {@link ErrorCode}</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 với danh sách field errors</li>
 *   <li>{@link ConstraintViolationException} → 400 với danh sách violation</li>
 *   <li>{@link DataIntegrityViolationException} → 409 (UNIQUE/FK violation từ DB)</li>
 *   <li>Bất kỳ {@link Exception} khác → 500 INTERNAL_ERROR</li>
 * </ul>
 *
 * <p><b>traceId</b>: sinh UUID mỗi request — sau này có thể inject từ MDC/RequestId filter.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBase(BaseException ex, HttpServletRequest req) {
        String traceId = traceId();
        log.warn("[{}] {} {} -> {} {}: {}",
                traceId, req.getMethod(), req.getRequestURI(),
                ex.getStatus().value(), ex.getCode(), ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .success(false)
                .error(ErrorResponse.ErrorPayload.builder()
                        .code(ex.getCode())
                        .message(ex.getMessage())
                        .status(ex.getStatus().value())
                        .details(ex.getDetails().isEmpty() ? null : ex.getDetails())
                        .build())
                .traceId(traceId)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleBeanValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        String traceId = traceId();
        List<ValidationErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ValidationErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .rejectedValue(fe.getRejectedValue())
                        .build())
                .toList();
        log.warn("[{}] {} {} -> 400 VALIDATION_FAILED ({} fields)",
                traceId, req.getMethod(), req.getRequestURI(), fields.size());

        ValidationErrorResponse body = ValidationErrorResponse.builder()
                .success(false)
                .error(ValidationErrorResponse.ValidationErrorPayload.builder()
                        .code(ErrorCode.VALIDATION_FAILED)
                        .message("Yêu cầu không hợp lệ")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .fields(fields)
                        .build())
                .traceId(traceId)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {
        String traceId = traceId();
        List<ValidationErrorResponse.FieldError> fields = ex.getConstraintViolations().stream()
                .map(v -> ValidationErrorResponse.FieldError.builder()
                        .field(v.getPropertyPath().toString())
                        .message(v.getMessage())
                        .rejectedValue(v.getInvalidValue())
                        .build())
                .toList();
        log.warn("[{}] {} {} -> 400 VALIDATION_FAILED (constraint)",
                traceId, req.getMethod(), req.getRequestURI());

        ValidationErrorResponse body = ValidationErrorResponse.builder()
                .success(false)
                .error(ValidationErrorResponse.ValidationErrorPayload.builder()
                        .code(ErrorCode.VALIDATION_FAILED)
                        .message("Yêu cầu không hợp lệ")
                        .status(HttpStatus.BAD_REQUEST.value())
                        .fields(fields)
                        .build())
                .traceId(traceId)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest req) {
        String traceId = traceId();
        log.warn("[{}] {} {} -> 409 DB integrity: {}",
                traceId, req.getMethod(), req.getRequestURI(), rootMessage(ex));
        ErrorResponse body = ErrorResponse.of(
                "DB_INTEGRITY_VIOLATION",
                "Vi phạm ràng buộc dữ liệu (UNIQUE / FK / NOT NULL)",
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMalformed(Exception ex, HttpServletRequest req) {
        String traceId = traceId();
        log.warn("[{}] {} {} -> 400 malformed: {}", traceId, req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(
                ErrorResponse.of("MALFORMED_REQUEST", "Body/Param không đọc được", HttpStatus.BAD_REQUEST.value())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        String traceId = traceId();
        log.error("[{}] {} {} -> 500 unhandled", traceId, req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Đã xảy ra lỗi hệ thống",
                        HttpStatus.INTERNAL_SERVER_ERROR.value())
        );
    }

    private static String traceId() {
        return UUID.randomUUID().toString();
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getMessage();
    }
}
