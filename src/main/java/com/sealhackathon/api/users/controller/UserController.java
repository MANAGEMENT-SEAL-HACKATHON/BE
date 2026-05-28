package com.sealhackathon.api.users.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.common.response.PageResponse;
import com.sealhackathon.api.common.security.CoordinatorOnly;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.users.dto.request.PatchUserRequest;
import com.sealhackathon.api.users.dto.request.PatchUserStatusRequest;
import com.sealhackathon.api.users.dto.response.UserDetailResponse;
import com.sealhackathon.api.users.dto.response.UserResponse;
import com.sealhackathon.api.users.dto.response.UserSummaryResponse;
import com.sealhackathon.api.users.service.UserAdminService;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-05a / MF-02 FR-09 — quản lý user (coordinator).
 */
@Tag(name = "Personnel — Users", description = "FR-05a / MF-02 FR-09 — Users admin")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CoordinatorOnly
public class UserController {

    private final UserAdminService userAdminService;

    @GetMapping
    @Operation(summary = "Danh sách user (filter status/role/userType/q)",
            description = "role=MENTOR hoặc JUDGE (mặc định) → trả cả pool MENTOR+JUDGE cho dropdown phân công. "
                    + "accountRoleExact=true → lọc đúng users.role. Phân công Judge/Mentor lưu ở bảng assignment, "
                    + "không thêm cột role thứ hai trên users.")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> list(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean personnelOnly,
            @RequestParam(required = false) Boolean accountRoleExact,
            @RequestParam(required = false) UserType userType,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                userAdminService.listUsers(status, role, personnelOnly, accountRoleExact, userType, q, pageable)));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Chi tiết user (form duyệt)")
    public ResponseEntity<ApiResponse<UserDetailResponse>> get(@PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.ok(userAdminService.getUser(userId)));
    }

    @GetMapping("/{userId}/student-card")
    @Operation(summary = "Coordinator tải ảnh thẻ sinh viên")
    public ResponseEntity<Resource> getStudentCard(@PathVariable Integer userId) {
        Resource resource = userAdminService.getUserStudentCard(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"student-card\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Duyệt / từ chối / override trạng thái tài khoản")
    public ResponseEntity<ApiResponse<UserDetailResponse>> patchStatus(
            @PathVariable Integer userId,
            @Valid @RequestBody PatchUserStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userAdminService.patchStatus(userId, req)));
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "Cập nhật user (is_dept_head)")
    public ResponseEntity<ApiResponse<UserResponse>> patch(
            @PathVariable Integer userId,
            @RequestBody PatchUserRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userAdminService.patchUser(userId, req)));
    }
}
