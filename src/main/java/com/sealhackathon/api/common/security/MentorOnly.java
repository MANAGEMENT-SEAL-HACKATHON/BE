package com.sealhackathon.api.common.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Mentor đã duyệt — portal /api/v1/me/mentor*. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('MENTOR') and authentication.principal.status.name() == 'APPROVED'")
public @interface MentorOnly {
}
