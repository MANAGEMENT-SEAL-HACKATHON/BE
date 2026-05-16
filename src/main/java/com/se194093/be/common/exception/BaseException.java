package com.se194093.be.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * Cha của mọi business exception trong MF-01. Mỗi exception ràng buộc:
 * <ul>
 *   <li>{@link #code}     — mã định danh (xem {@link ErrorCode}), client mapping i18n.</li>
 *   <li>{@link #status}   — HTTP status mặc định của exception.</li>
 *   <li>{@link #details}  — payload tuỳ chọn (Map) để client hiển thị chi tiết (vd: danh sách Round vi phạm weight).</li>
 * </ul>
 *
 * <p>{@code GlobalExceptionHandler} sẽ ánh xạ exception → {@code ErrorResponse}.
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final Map<String, Object> details;

    protected BaseException(String code, String message, HttpStatus status) {
        this(code, message, status, Collections.emptyMap());
    }

    protected BaseException(String code, String message, HttpStatus status, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details == null ? Collections.emptyMap() : details;
    }
}
