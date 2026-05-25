package com.sealhackathon.api.auth.controller;

import tools.jackson.databind.ObjectMapper;
import com.sealhackathon.api.auth.dto.request.LoginRequest;
import com.sealhackathon.api.auth.dto.response.AuthTokenResponse;
import com.sealhackathon.api.auth.service.AuthService;
import com.sealhackathon.api.auth.service.RegistrationService;
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
}
