package com.se194093.be.users.service;

import com.se194093.be.users.dto.request.PatchUserRequest;
import com.se194093.be.users.dto.response.UserResponse;

public interface UserAdminService {

    UserResponse patchUser(Integer userId, PatchUserRequest req);
}
