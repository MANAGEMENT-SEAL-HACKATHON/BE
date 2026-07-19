package com.sealhackathon.api.auth.controller;

import tools.jackson.databind.ObjectMapper;
import com.sealhackathon.api.auth.dto.request.ForgotPasswordRequest;
import com.sealhackathon.api.auth.dto.request.LoginRequest;
import com.sealhackathon.api.auth.dto.request.OAuthGithubCodeRequest;
import com.sealhackathon.api.auth.dto.request.OAuthGoogleRequest;
import com.sealhackathon.api.auth.dto.request.RegisterRequest;
import com.sealhackathon.api.auth.dto.request.ResetPasswordRequest;
import com.sealhackathon.api.auth.dto.response.AuthTokenResponse;
import com.sealhackathon.api.auth.dto.response.ForgotPasswordResponse;
import com.sealhackathon.api.auth.dto.response.OAuthLinkStatusResponse;
import com.sealhackathon.api.auth.dto.response.RegisterResponse;
import com.sealhackathon.api.auth.service.AuthService;
import com.sealhackathon.api.auth.service.EmailVerificationService;
import com.sealhackathon.api.auth.service.PasswordResetService;
import com.sealhackathon.api.auth.service.RegistrationService;
import com.sealhackathon.api.auth.service.SocialAuthService;
import com.sealhackathon.api.users.value_object.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "security.jwt.enabled=false")
@Import(JacksonAutoConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private SocialAuthService socialAuthService;

    @Test
    void register_minimalPayload_returnsCreated() throws Exception {
        when(registrationService.register(any())).thenReturn(RegisterResponse.builder()
                .userId(100)
                .email("user@gmail.com")
                .status("PENDING")
                .message("Đăng ký thành công")
                .build());

        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@gmail.com");
        req.setPassword("password12");
        req.setConfirmPassword("password12");
        req.setUserType(UserType.INTERNAL);
        req.setStudentCode("SE123456");
        req.setChapterId(1);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("user@gmail.com"));
    }

    @Test
    void login_returnsTokens() throws Exception {
        when(authService.login(any(), any())).thenReturn(AuthTokenResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .tokenType("Bearer")
                .expiresInSeconds(1800L)
                .mustChangePassword(false)
                .build());

        LoginRequest req = new LoginRequest();
        req.setEmail("coord@fpt.edu.vn");
        req.setPassword("Coordinator@dev1");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"));
    }

    @Test
    void forgotPassword_returnsOk() throws Exception {
        when(passwordResetService.requestReset("user@fpt.edu.vn"))
                .thenReturn(ForgotPasswordResponse.builder()
                        .message("Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu sẽ được gửi.")
                        .build());

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("user@fpt.edu.vn");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").exists());
    }

    @Test
    void resetPassword_returnsOk() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("jwt-token");
        req.setNewPassword("newPass123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void logoutAll_returnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout-all"))
                .andExpect(status().isOk());
    }

    @Test
    void oauthGoogleLogin_returnsTokens() throws Exception {
        when(socialAuthService.loginWithGoogle(any(), any(), any())).thenReturn(AuthTokenResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .tokenType("Bearer")
                .expiresInSeconds(1800L)
                .mustChangePassword(false)
                .build());
        OAuthGoogleRequest req = new OAuthGoogleRequest();
        req.setIdToken("google-id-token");
        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").value("refresh"));
    }

    @Test
    void oauthGithubCodeLogin_returnsTokens() throws Exception {
        when(socialAuthService.loginWithGithubCode(any(), any(), any(), any())).thenReturn(AuthTokenResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .tokenType("Bearer")
                .expiresInSeconds(1800L)
                .mustChangePassword(false)
                .build());
        OAuthGithubCodeRequest req = new OAuthGithubCodeRequest();
        req.setCode("gh-code");
        req.setRedirectUri("http://localhost:5173/auth/github/callback");
        mockMvc.perform(post("/api/v1/auth/oauth/github/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"));
    }

    @Test
    void oauthGithubLinkCode_returnsOk() throws Exception {
        when(socialAuthService.linkGithubCodeForCurrentUser(any(), any())).thenReturn(OAuthLinkStatusResponse.builder()
                .provider("GITHUB")
                .email("dev@gmail.com")
                .linked(true)
                .message("ok")
                .build());
        OAuthGithubCodeRequest req = new OAuthGithubCodeRequest();
        req.setCode("gh-code");
        req.setRedirectUri("http://localhost:5173/auth/github/callback");
        mockMvc.perform(post("/api/v1/auth/oauth/github/link/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("GITHUB"));
    }

    @Test
    void oauthGoogleUnlink_returnsOk() throws Exception {
        when(socialAuthService.unlinkGoogleForCurrentUser()).thenReturn(OAuthLinkStatusResponse.builder()
                .provider("GOOGLE")
                .email("dev@gmail.com")
                .linked(false)
                .message("unlinked")
                .build());
        mockMvc.perform(post("/api/v1/auth/oauth/google/unlink"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.linked").value(false));
    }
}
