package com.portal.universe.driveservice.common.config;

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

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:drive-service}")
    private String applicationName;

    @Value("${server.port:8087}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(apiServers())
                .components(securityComponents())
                .addSecurityItem(securityRequirement());
    }

    private Info apiInfo() {
        return new Info()
                .title("Portal Universe - Drive Service API")
                .description("""
                        # Drive Service API Documentation

                        파일 관리 서비스 API

                        ## 주요 기능
                        - 📁 **File**: 파일 업로드/다운로드/삭제
                        - 📂 **Folder**: 폴더 생성/관리

                        ## API Gateway 라우팅
                        ```
                        Client Request: /api/v1/drive/{endpoint}
                                ↓ (StripPrefix=3)
                        Drive Service:   /{endpoint}
                        ```

                        ## 인증
                        - API Gateway에서 JWT 검증 후 X-User-Id 헤더 전달
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("L-a-z-e")
                        .email("yysi8771@gmail.com")
                        .url("https://github.com/L-a-z-e/portal-universe"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> apiServers() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("API Gateway (development)"),
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Drive Service Direct (testing)")
        );
    }

    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token (without Bearer prefix)")
                );
    }

    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList("bearerAuth");
    }
}
