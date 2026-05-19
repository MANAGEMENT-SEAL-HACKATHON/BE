package com.sealhackathon.api.common.security;

import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Đại diện cho principal hiện tại sau khi module Auth verify JWT.
 *
 * <p>Là contract tối thiểu mà module Auth phải cung cấp. Trong scope MF-01 (chưa có Auth),
 * {@link CurrentUserAccessor} sẽ trả về 1 stub Coordinator để dev/test luồng nghiệp vụ.
 *
 * <p>Claim mapping (JWT &rarr; field):
 * <ul>
 *   <li>{@code sub} hoặc {@code userId} → {@link #userId}</li>
 *   <li>{@code email} → {@link #email}</li>
 *   <li>{@code role} → {@link #role}</li>
 *   <li>{@code status} → {@link #status}</li>
 *   <li>{@code userType} → {@link #userType}</li>
 *   <li>{@code isTempAccount} → {@link #isTempAccount}</li>
 * </ul>
 */
@Getter
@Builder
@AllArgsConstructor
public class CurrentUserStub {

    private final Integer userId;
    private final String email;
    private final String fullName;
    private final UserRole role;
    private final UserStatus status;
    private final UserType userType;
    private final boolean isTempAccount;

    public boolean isCoordinatorApproved() {
        return role == UserRole.COORDINATOR && status == UserStatus.APPROVED;
    }
}
