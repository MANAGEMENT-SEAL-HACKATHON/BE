package com.sealhackathon.api.auth.service;

import com.sealhackathon.api.auth.config.JwtProperties;
import com.sealhackathon.api.common.security.CurrentUserStub;
import com.sealhackathon.api.users.entity.User;
import com.sealhackathon.api.users.value_object.UserRole;
import com.sealhackathon.api.users.value_object.UserStatus;
import com.sealhackathon.api.users.value_object.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-at-least-32-bytes-long!!");
        props.setIssuer("test-issuer");
        props.setAccessTtlMinutes(15);
        jwtTokenService = new JwtTokenService(props);
    }

    @Test
    void createAndParseAccessToken() {
        User user = User.builder()
                .id(42)
                .email("alice@fpt.edu.vn")
                .role(UserRole.COORDINATOR)
                .status(UserStatus.APPROVED)
                .userType(UserType.INTERNAL)
                .isTempAccount(false)
                .build();

        String token = jwtTokenService.createAccessToken(user);
        CurrentUserStub principal = jwtTokenService.parseAccessToken(token);

        assertThat(principal.getUserId()).isEqualTo(42);
        assertThat(principal.getEmail()).isEqualTo("alice@fpt.edu.vn");
        assertThat(principal.getRole()).isEqualTo(UserRole.COORDINATOR);
        assertThat(principal.getStatus()).isEqualTo(UserStatus.APPROVED);
    }

}
