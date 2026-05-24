package com.sealhackathon.api.auth.security;

import tools.jackson.databind.ObjectMapper;
import com.sealhackathon.api.auth.service.JwtTokenService;
import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.response.ErrorResponse;
import com.sealhackathon.api.common.security.CurrentUserStub;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    CurrentUserStub principal = jwtTokenService.parseAccessToken(token);
                    SecurityContextHolder.getContext().setAuthentication(new SealAuthentication(principal));
                } catch (AuthException ex) {
                    writeAuthError(response, ex);
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeAuthError(HttpServletResponse response, AuthException ex) throws IOException {
        ErrorResponse body = ErrorResponse.of(
                ex.getCode(), ex.getMessage(), ex.getStatus().value());
        response.setStatus(ex.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
