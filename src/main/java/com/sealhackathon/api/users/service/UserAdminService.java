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
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UserAdminService {

    UserDetailResponse getMe();

    UserDetailResponse patchMe(PatchMeRequest req);

    UserDetailResponse uploadMyStudentCard(MultipartFile file);

    Resource getMyStudentCard();

    PageResponse<UserSummaryResponse> listUsers(UserStatus status, UserRole role, Boolean personnelOnly,
                                                       Boolean accountRoleExact, UserType userType, String q,
                                                       Pageable pageable);

    UserDetailResponse getUser(Integer userId);

    UserResponse patchUser(Integer userId, PatchUserRequest req);

    UserDetailResponse patchStatus(Integer userId, PatchUserStatusRequest req);

    Resource getUserStudentCard(Integer userId);
}
