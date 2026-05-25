package com.sealhackathon.api.auth.security;

import tools.jackson.databind.ObjectMapper;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ErrorResponse body = ErrorResponse.of(
                ErrorCode.FORBIDDEN,
                "Yêu cầu role/quyền phù hợp (COORDINATOR đã APPROVED hoặc tài khoản APPROVED)",
                HttpStatus.FORBIDDEN.value());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
