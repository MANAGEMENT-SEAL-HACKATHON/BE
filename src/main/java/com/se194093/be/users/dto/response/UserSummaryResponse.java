package com.se194093.be.users.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.se194093.be.users.value_object.UserRole;
import com.se194093.be.users.value_object.UserStatus;
import com.se194093.be.users.value_object.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSummaryResponse {

    private final Integer id;
    private final String fullName;
    private final String email;
    private final UserRole role;
    private final UserStatus status;
    private final UserType userType;
    private final Boolean isTempAccount;
    private final String institution;
}
