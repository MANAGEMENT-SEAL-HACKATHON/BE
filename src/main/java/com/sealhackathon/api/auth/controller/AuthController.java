package com.sealhackathon.api.auth.controller;

import com.sealhackathon.api.auth.dto.request.ChangePasswordRequest;
import com.sealhackathon.api.auth.dto.request.ForgotPasswordRequest;
import com.sealhackathon.api.auth.dto.request.LoginRequest;
import com.sealhackathon.api.auth.dto.request.LogoutRequest;
import com.sealhackathon.api.auth.dto.request.RefreshTokenRequest;
import com.sealhackathon.api.auth.dto.request.RegisterRequest;
import com.sealhackathon.api.auth.dto.request.ResetPasswordRequest;
import com.sealhackathon.api.common.security.ApprovedOnly;
import com.sealhackathon.api.auth.dto.response.AuthTokenResponse;
import com.sealhackathon.api.auth.dto.response.ForgotPasswordResponse;
import com.sealhackathon.api.auth.dto.response.RegisterResponse;
import com.sealhackathon.api.auth.service.AuthService;
import com.sealhackathon.api.auth.service.PasswordResetService;
import com.sealhackathon.api.auth.service.RegistrationService;
import com.sealhackathon.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "MF-02 FR-07 — Đăng ký, đăng nhập, JWT")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản STUDENT (mở — không bắt buộc lời mời)")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(registrationService.register(req)));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Xác thực email (link từ mail stub / dev)")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        registrationService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.ok(null, "Email đã xác thực"));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập — trả access + refresh JWT")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req, httpRequest)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token — trả refresh token mới (rotation)")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest req,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.ok(
                authService.refresh(req.getRefreshToken(), httpRequest)));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu đặt lại mật khẩu (luôn trả message chung)")
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                passwordResetService.requestReset(req.getEmail())));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu bằng token từ email/link dev")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok(null, "Mật khẩu đã được đặt lại"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Thu hồi refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) LogoutRequest req) {
        String token = req != null ? req.getRefreshToken() : null;
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã đăng xuất"));
    }

    @PostMapping("/logout-all")
    @ApprovedOnly
    @Operation(summary = "Thu hồi mọi phiên đăng nhập của user hiện tại")
    public ResponseEntity<ApiResponse<Void>> logoutAll() {
        authService.logoutAll();
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã đăng xuất tất cả thiết bị"));
    }

    @PostMapping("/change-password")
    @ApprovedOnly
    @Operation(summary = "Đổi mật khẩu (bắt buộc judge khách lần đầu)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(req);
        return ResponseEntity.ok(ApiResponse.ok(null, "Mật khẩu đã được cập nhật"));
    }
}
