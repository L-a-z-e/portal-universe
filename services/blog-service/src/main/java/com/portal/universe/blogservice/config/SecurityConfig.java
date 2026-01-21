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
                        // --- 공개 엔드포인트 (조회) ---
                        .requestMatchers(HttpMethod.GET, "/api/blog").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blog/**").permitAll()
                        // GET 메서드로 시작하는 모든 경로 공개 (posts, tags, categories, trending, feed 등)
                        .requestMatchers(HttpMethod.GET, "/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/tags/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()

                        // --- 파일 업로드 API 권한 설정 ---
                        .requestMatchers(HttpMethod.POST, "/file/upload").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/file/delete").hasRole("ADMIN")

                        // --- 관리자 전용 엔드포인트 (생성, 수정, 삭제) ---
                        .requestMatchers(HttpMethod.POST, "/api/blog").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/blog/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/blog/**").hasRole("ADMIN")

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
