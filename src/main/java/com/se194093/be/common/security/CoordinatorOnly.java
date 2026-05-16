package com.se194093.be.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation: endpoint chỉ Coordinator (role=COORDINATOR &amp; status=APPROVED) được gọi.
 *
 * <p><b>Trạng thái hiện tại (MF-01 v2.2):</b> annotation thuần để document intent. KHÔNG đính kèm
 * {@code @PreAuthorize} vì plan KHÔNG cấu hình {@code SecurityFilterChain} (để module Auth làm sau).
 *
 * <p><b>Khi module Auth được wire up:</b> chuyển annotation này thành meta-annotation:
 * <pre>{@code
 * @Target({ElementType.METHOD, ElementType.TYPE})
 * @Retention(RetentionPolicy.RUNTIME)
 * @PreAuthorize("hasRole('COORDINATOR') and authentication.principal.status == 'APPROVED'")
 * public @interface CoordinatorOnly { }
 * }</pre>
 *
 * <p>Hoặc thay thế trực tiếp bằng {@code @PreAuthorize} ở từng method.
 *
 * <p>SpEL contract bắt buộc:
 * <ul>
 *   <li>{@code hasRole('COORDINATOR')} — JWT claim {@code role = COORDINATOR}</li>
 *   <li>{@code principal.status == 'APPROVED'} — JWT claim {@code status = APPROVED}</li>
 * </ul>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CoordinatorOnly {
}
