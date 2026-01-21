package com.portal.universe.blogservice.config;

import com.portal.universe.commonlibrary.security.filter.GatewayAuthenticationFilter;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 블로그 서비스의 웹 보안 설정을 담당하는 클래스입니다.
 *
 * API Gateway에서 JWT를 검증하고 X-User-Id, X-User-Roles 헤더를 전달합니다.
 * 이 서비스는 해당 헤더를 읽어 SecurityContext를 설정합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Actuator 엔드포인트(/actuator/**)에 대한 보안 필터 체인을 설정합니다.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(requests -> requests
                        .anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Swagger UI 및 OpenAPI 문서 엔드포인트에 대한 보안 필터 체인을 설정합니다.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/api-docs",
                        "/api-docs/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**"
                )
                .authorizeHttpRequests(requests -> requests
                        .anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * 블로그 서비스의 API 엔드포인트에 대한 보안 필터 체인을 설정합니다.
     *
     * ### 권한 부여 전략
     * - **공개 (Permit All)**: `GET` 요청은 누구나 가능 (게시글/카테고리/태그 조회 등).
     * - **인증 필요 (Authenticated)**: 댓글 작성/수정/삭제 등 인증된 사용자만 가능한 기능.
     * - **관리자 전용 (Admin Role)**: `POST`, `PUT`, `DELETE` 요청은 ADMIN 역할이 있는 사용자만 가능.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // ========================================
                        // [공개] 누구나 접근 가능
                        // Gateway StripPrefix=2 적용 후 경로 (/api/blog 제거됨)
                        // ========================================
                        .requestMatchers(HttpMethod.GET, "/posts", "/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/tags", "/tags/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories", "/categories/**").permitAll()

                        // ========================================
                        // [인증된 사용자] 로그인 필요
                        // ========================================
                        // 파일 업로드
                        .requestMatchers(HttpMethod.POST, "/file/upload").authenticated()
                        // 게시글 작성 (일반 사용자도 가능)
                        .requestMatchers(HttpMethod.POST, "/posts").authenticated()
                        // 게시글 수정/삭제 (본인 게시글)
                        .requestMatchers(HttpMethod.PUT, "/posts/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/posts/**").authenticated()
                        // 좋아요, 팔로우 등
                        .requestMatchers(HttpMethod.POST, "/posts/*/like").authenticated()
                        .requestMatchers(HttpMethod.POST, "/follows/**").authenticated()

                        // ========================================
                        // [관리자] ADMIN 역할 필요
                        // ========================================
                        .requestMatchers(HttpMethod.DELETE, "/file/delete").hasRole("ADMIN")

                        // --- 위에서 지정하지 않은 나머지 모든 요청은 인증만 되면 허용 ---
                        .anyRequest().authenticated()
                )
                // Gateway에서 전달한 헤더로 인증 정보 설정
                .addFilterBefore(new GatewayAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // Stateless REST API
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
