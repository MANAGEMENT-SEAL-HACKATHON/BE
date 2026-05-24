package com.sealhackathon.api.auth.security;

import com.sealhackathon.api.common.exception.AuthException;
import com.sealhackathon.api.common.exception.ErrorCode;
import com.sealhackathon.api.common.security.CurrentUserAccessor;
import com.sealhackathon.api.common.security.CurrentUserStub;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Primary
@ConditionalOnProperty(name = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
public class JwtCurrentUserAccessor implements CurrentUserAccessor {

    @Override
    public Integer currentUserId() {
        return currentUser().getUserId();
    }

    @Override
    public CurrentUserStub currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CurrentUserStub principal)) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "Chưa đăng nhập",
                    HttpStatus.UNAUTHORIZED);
        }
        return principal;
    }
}
