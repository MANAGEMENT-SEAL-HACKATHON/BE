package com.sealhackathon.api.users.service;

import com.sealhackathon.api.common.response.PageResponse;
import com.sealhackathon.api.users.dto.request.PatchMeRequest;
import com.sealhackathon.api.users.dto.request.PatchUserRequest;
import com.sealhackathon.api.users.dto.request.PatchUserStatusRequest;
import com.sealhackathon.api.users.dto.response.UserDetailResponse;
import com.sealhackathon.api.users.dto.response.UserResponse;
import com.sealhackathon.api.users.dto.response.UserSummaryResponse;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import org.springframework.data.domain.Pageable;

public interface UserAdminService {

    UserDetailResponse getMe();

    UserDetailResponse patchMe(PatchMeRequest req);

    PageResponse<UserSummaryResponse> listUsers(UserStatus status, UserRole role, UserType userType,
                                                  String q, Pageable pageable);

    UserDetailResponse getUser(Integer userId);

    UserResponse patchUser(Integer userId, PatchUserRequest req);

    UserDetailResponse patchStatus(Integer userId, PatchUserStatusRequest req);
}
