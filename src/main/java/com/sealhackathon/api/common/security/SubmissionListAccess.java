package com.sealhackathon.api.common.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GET /submissions — Coordinator, Judge hoặc Student đã duyệt (GD03 §8).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("""
        (hasRole('COORDINATOR') or hasRole('JUDGE') or hasRole('STUDENT'))
        and authentication.principal.status.name() == 'APPROVED'
        """)
public @interface SubmissionListAccess {
}
