package com.sealhackathon.api.users.dto.response;

import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private final Integer id;
    private final String fullName;
    private final String email;
    private final UserRole role;
    private final UserType userType;
    private final Boolean isTempAccount;
    private final Boolean isDeptHead;
    private final UserStatus status;
    private final String institution;
}
