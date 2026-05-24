package com.sealhackathon.api.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 401 / 403 authentication & authorization failures (MF-02).
 */
public class AuthException extends BaseException {

    public AuthException(String code, String message, HttpStatus status) {
        super(code, message, status);
    }

    public AuthException(String code, String message, HttpStatus status, Map<String, Object> details) {
        super(code, message, status, details);
    }
}
