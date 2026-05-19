package com.sealhackathon.api.common.security;

/**
 * Cho phép tầng service truy cập principal đang đăng nhập mà không bind cứng vào Spring Security.
 *
 * <p>Module Auth (làm sau) sẽ cung cấp impl đọc {@code SecurityContextHolder}.
 *
 * <p>Trong scope MF-01, dùng impl {@link StubCurrentUserAccessor} trả về 1 Coordinator giả
 * (id=1) để code blueprint chạy được khi dev test.
 */
public interface CurrentUserAccessor {

    /**
     * @return user id đang đăng nhập; {@code null} nếu anonymous.
     */
    Integer currentUserId();

    /**
     * @return full principal stub; {@code null} nếu anonymous.
     */
    CurrentUserStub currentUser();
}
