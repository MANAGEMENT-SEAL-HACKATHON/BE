package com.sealhackathon.api.common.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nhân sự chấm điểm đã duyệt (role JUDGE hoặc MENTOR).
 *
 * <p>MF-02 cross-track: mentor track A có thể được phân công judge track B qua
 * {@code judge_assignments}; quyền chấm thực tế kiểm tra ở service ({@code JudgeAssignmentGuard}).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("""
        (hasRole('JUDGE') or hasRole('MENTOR'))
        and authentication.principal.status.name() == 'APPROVED'
        """)
public @interface JudgeOnly {
}
