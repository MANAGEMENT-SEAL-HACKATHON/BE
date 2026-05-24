package com.sealhackathon.api.users.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.security.ApprovedOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.users.dto.request.PatchMeRequest;
import com.sealhackathon.api.users.dto.response.UserDetailResponse;
import com.sealhackathon.api.users.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users — Profile", description = "MF-02 — profile người dùng hiện tại")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@ApprovedOnly
public class UserMeController {

    private final UserAdminService userAdminService;

    @GetMapping("/me")
    @Operation(summary = "Profile user đang đăng nhập")
    public ResponseEntity<ApiResponse<UserDetailResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok(userAdminService.getMe()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Cập nhật phone / avatar (không đổi userType, chapter, mã SV)")
    public ResponseEntity<ApiResponse<UserDetailResponse>> patchMe(
            @Valid @RequestBody PatchMeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userAdminService.patchMe(req)));
    }
}
