package com.sealhackathon.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF: Rất quan trọng khi làm API Restful, nếu không tắt sẽ bị 403 khi gọi POST/PUT/DELETE
                .csrf(AbstractHttpConfigurer::disable)

                // Bật CORS: Lệnh này giúp Spring Security kết hợp với file CorsConfig.java
                .cors(Customizer.withDefaults())

                // Cấu hình phân quyền: Mở toang cửa cho Giai đoạn 1 (GĐ1)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}