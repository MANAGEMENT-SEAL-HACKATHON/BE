package com.se194093.be.users.controller;

import com.se194093.be.common.response.ApiResponse;
import com.se194093.be.common.security.CoordinatorOnly;
import com.se194093.be.users.dto.request.PatchUserRequest;
import com.se194093.be.users.dto.response.UserResponse;
import com.se194093.be.users.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-05a v3.1 [FIX-R9] — PATCH user flags (is_dept_head).
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CoordinatorOnly
public class UserController {

    private final UserAdminService userAdminService;

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> patch(
            @PathVariable Integer userId,
            @RequestBody PatchUserRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userAdminService.patchUser(userId, req)));
    }
}
