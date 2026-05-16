package com.se194093.be.common.security;

import com.se194093.be.users.value_object.UserRole;
import com.se194093.be.users.value_object.UserStatus;
import com.se194093.be.users.value_object.UserType;
import org.springframework.stereotype.Component;

/**
 * Stub impl cho {@link CurrentUserAccessor} trong scope MF-01 — TRƯỚC khi module Auth được wire.
 *
 * <p>Luôn trả về Coordinator (id=1) giả để các service mutation có giá trị {@code created_by} /
 * {@code assigned_by} hợp lệ khi dev test thủ công.
 *
 * <p><b>QUAN TRỌNG:</b> impl này phải bị REPLACE bằng impl đọc JWT/SecurityContext khi module
 * Auth ra mắt. Đánh dấu bằng {@code @Primary} không cần thiết — chỉ cần xóa class này hoặc thay
 * bằng impl Auth là Spring tự pick.
 */
@Component
public class StubCurrentUserAccessor implements CurrentUserAccessor {

    private static final Integer STUB_COORDINATOR_ID = 1;

    @Override
    public Integer currentUserId() {
        return STUB_COORDINATOR_ID;
    }

    @Override
    public CurrentUserStub currentUser() {
        return CurrentUserStub.builder()
                .userId(STUB_COORDINATOR_ID)
                .email("coordinator@stub.local")
                .fullName("Stub Coordinator")
                .role(UserRole.COORDINATOR)
                .status(UserStatus.APPROVED)
                .userType(UserType.INTERNAL)
                .isTempAccount(false)
                .build();
    }
}
