package com.sealhackathon.api.auth.security;

import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Khi JWT tắt ({@code security.jwt.enabled=false}): gắn Coordinator stub vào context
 * để {@link com.sealhackathon.api.common.security.CoordinatorOnly} vẫn hoạt động trong test.
 */
@Component
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "false")
public class DevStubAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            CurrentUserStub stub = CurrentUserStub.builder()
                    .userId(1)
                    .email("coord@fpt.edu.vn")
                    .fullName("Stub Coordinator")
                    .role(UserRole.COORDINATOR)
                    .status(UserStatus.APPROVED)
                    .userType(UserType.INTERNAL)
                    .isTempAccount(false)
                    .build();
            SecurityContextHolder.getContext().setAuthentication(new SealAuthentication(stub));
        }
        filterChain.doFilter(request, response);
    }
}
