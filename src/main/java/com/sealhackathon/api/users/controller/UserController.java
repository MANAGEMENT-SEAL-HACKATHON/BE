package com.sealhackathon.api.users.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.sealhackathon.api.users.dto.request.PatchUserRequest;
import com.sealhackathon.api.users.dto.response.UserResponse;
import com.sealhackathon.api.users.service.UserAdminService;
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
@Tag(name = "Personnel — Users", description = "FR-05a — PATCH user (is_dept_head)")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
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
