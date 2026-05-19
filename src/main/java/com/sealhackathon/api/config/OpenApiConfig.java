package com.sealhackathon.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Swagger UI — MF-01 GĐ1 (SpringDoc).
 *
 * <p>Auth: scheme {@code bearerAuth} mô tả JWT Coordinator (GĐ2). Hiện tại BE dùng stub
 * {@code StubCurrentUserAccessor} — Try it out không bắt buộc token.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI sealOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SEAL Hackathon API")
                        .version("0.0.1")
                        .description("""
                                SEAL Hackathon Management System — Backend (MF-01 GĐ1).

                                Kiến trúc: Hackathon → Round → Track.
                                Runbook: docs/workflow/mf01-gd1-quy-trinh-api.md
                                """)
                        .contact(new Contact().name("FPT SE Dept").email("coord@fpt.edu.vn"))
                        .license(new License().name("Internal")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT — role COORDINATOR, status APPROVED (module Auth GĐ2)")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
