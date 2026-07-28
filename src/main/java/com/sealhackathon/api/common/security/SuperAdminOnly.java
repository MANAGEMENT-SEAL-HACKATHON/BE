package com.sealhackathon.api.common.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Chỉ SUPERADMIN — dùng cho thao tác cực kỳ nhạy cảm (vd. unlock scoring sau khi khóa).
 * Không dành cho Coordinator thường.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('SUPERADMIN') and authentication.principal.status.name() == 'APPROVED'")
public @interface SuperAdminOnly {
}
