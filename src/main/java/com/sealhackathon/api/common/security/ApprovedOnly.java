package com.sealhackathon.api.common.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Mọi role nhưng {@code status=APPROVED}. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("authentication.principal.status.name() == 'APPROVED'")
public @interface ApprovedOnly {
}
