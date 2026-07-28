package com.sealhackathon.api.users.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sealhackathon.api.judge_assignments.dto.response.JudgeAssignmentResponse;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    private final String studentCardImagePath;
    /** Populated when patchUser syncs JudgeAssignment types (B5). */
    private final List<JudgeAssignmentResponse> assignments;
}
