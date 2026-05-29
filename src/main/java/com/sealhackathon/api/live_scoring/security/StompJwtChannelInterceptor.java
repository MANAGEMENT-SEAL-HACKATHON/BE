package com.sealhackathon.api.live_scoring.security;

import com.sealhackathon.api.auth.security.SealAuthentication;
import com.sealhackathon.api.auth.service.JwtTokenService;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** JWT Bearer trên STOMP CONNECT. */
@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenService jwtTokenService;

    @Value("${security.jwt.enabled:true}")
    private boolean jwtEnabled;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        if (!jwtEnabled) {
            CurrentUserStub stub = CurrentUserStub.builder()
                    .userId(1)
                    .email("coord@fpt.edu.vn")
                    .fullName("Stub Coordinator")
                    .role(UserRole.COORDINATOR)
                    .status(UserStatus.APPROVED)
                    .userType(UserType.INTERNAL)
                    .isTempAccount(false)
                    .build();
            accessor.setUser(new SealAuthentication(stub));
            SecurityContextHolder.getContext().setAuthentication(new SealAuthentication(stub));
            return message;
        }

        String auth = accessor.getFirstNativeHeader("Authorization");
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing Authorization Bearer token on STOMP CONNECT");
        }

        String token = auth.substring(7).trim();
        CurrentUserStub principal = jwtTokenService.parseAccessToken(token);
        accessor.setUser(new SealAuthentication(principal));
        SecurityContextHolder.getContext().setAuthentication(new SealAuthentication(principal));
        return message;
    }
}
