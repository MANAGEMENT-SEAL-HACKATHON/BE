package com.sealhackathon.api.users.service;

import com.sealhackathon.api.users.dto.request.PatchUserRequest;
import com.sealhackathon.api.users.dto.response.UserResponse;

public interface UserAdminService {

    UserResponse patchUser(Integer userId, PatchUserRequest req);
}
