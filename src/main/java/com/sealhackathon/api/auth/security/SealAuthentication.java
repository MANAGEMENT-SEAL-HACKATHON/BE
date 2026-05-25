package com.sealhackathon.api.auth.security;

import com.sealhackathon.api.common.security.CurrentUserStub;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Authentication token với principal {@link CurrentUserStub} (JWT claims).
 */
@Getter
public class SealAuthentication extends AbstractAuthenticationToken {

    private final CurrentUserStub principal;

    public SealAuthentication(CurrentUserStub principal) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole().name())));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public CurrentUserStub getPrincipal() {
        return principal;
    }
}
