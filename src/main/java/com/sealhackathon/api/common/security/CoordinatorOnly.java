package com.sealhackathon.api.common.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Endpoint chỉ Coordinator (role=COORDINATOR &amp; status=APPROVED) — MF-02 JWT.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('COORDINATOR') and authentication.principal.status.name() == 'APPROVED'")
public @interface CoordinatorOnly {
}
