package com.sealhackathon.api.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 404 — tài nguyên không tồn tại. Ví dụ Hackathon/Track/Round/Criteria id sai.
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super(
                ErrorCode.RESOURCE_NOT_FOUND,
                "%s with id=%s not found".formatted(resourceName, id),
                HttpStatus.NOT_FOUND,
                Map.of("resource", resourceName, "id", id == null ? "null" : id.toString())
        );
    }

    public ResourceNotFoundException(String code, String message) {
        super(code, message, HttpStatus.NOT_FOUND);
    }
}
