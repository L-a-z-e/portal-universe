package com.portal.universe.blogservice.common.config;

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
 * Swagger/OpenAPI 3.0 설정
 * API 문서 자동 생성 및 테스트 UI 제공
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:blog-service}")
    private String applicationName;

    @Value("${server.port:8082}")
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
                .title("Portal Universe - Blog Service API")
                .description("""
                        # Blog Service API Documentation
                        
                        Velog 스타일의 블로그 플랫폼 API
                        
                        ## 주요 기능
                        - 📝 **Post**: 블로그 포스트 작성/수정/삭제
                        - 💬 **Comment**: 댓글 및 대댓글 관리
                        - 📚 **Series**: 포스트 시리즈(연재) 관리
                        - 🏷️ **Tag**: 태그 기반 분류 및 검색
                        
                        ## API Gateway 라우팅
                        ```
                        Client Request: /api/blog/{endpoint}
                                ↓ (StripPrefix=2)
                        Blog Service:   /{endpoint}
                        ```
                        
                        ## 인증
                        - OAuth2 JWT 토큰 기반 인증
                        - Bearer Token 방식
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
                        .description("API Gateway (개발 환경)"),
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Blog Service Direct (테스트용)")
        );
    }

    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력하세요 (Bearer 제외)")
                );
    }

    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList("bearerAuth");
    }
}