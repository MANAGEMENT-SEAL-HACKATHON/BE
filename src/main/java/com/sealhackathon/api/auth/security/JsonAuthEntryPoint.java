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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class JsonAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorResponse body = ErrorResponse.of(
                ErrorCode.UNAUTHORIZED,
                "Yêu cầu xác thực (Bearer JWT)",
                HttpStatus.UNAUTHORIZED.value());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
