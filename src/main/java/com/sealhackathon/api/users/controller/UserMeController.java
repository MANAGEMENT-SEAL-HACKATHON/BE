package com.sealhackathon.api.users.controller;

import com.sealhackathon.api.common.response.ApiResponse;
import com.sealhackathon.api.config.OpenApiConfig;
import com.sealhackathon.api.users.dto.request.PatchMeRequest;
import com.sealhackathon.api.users.dto.response.UserDetailResponse;
import com.sealhackathon.api.users.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Users — Profile", description = "MF-02 — profile người dùng hiện tại")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserMeController {

    private final UserAdminService userAdminService;

    @GetMapping("/me")
    @Operation(summary = "Profile user đang đăng nhập")
    public ResponseEntity<ApiResponse<UserDetailResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok(userAdminService.getMe()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Cập nhật hồ sơ cá nhân để hoàn thiện thông tin xét duyệt")
    public ResponseEntity<ApiResponse<UserDetailResponse>> patchMe(
            @Valid @RequestBody PatchMeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userAdminService.patchMe(req)));
    }

    @PostMapping(value = "/me/student-card", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload ảnh thẻ sinh viên của user hiện tại")
    public ResponseEntity<ApiResponse<UserDetailResponse>> uploadStudentCard(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(userAdminService.uploadMyStudentCard(file)));
    }

    @GetMapping("/me/student-card")
    @Operation(summary = "Tải ảnh thẻ sinh viên của user hiện tại")
    public ResponseEntity<Resource> downloadMyStudentCard() {
        Resource resource = userAdminService.getMyStudentCard();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"student-card\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
