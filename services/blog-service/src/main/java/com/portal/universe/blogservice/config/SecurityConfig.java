package com.portal.universe.blogservice.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 블로그 서비스의 웹 보안 설정을 담당하는 클래스입니다.
 * Actuator와 API 엔드포인트에 대한 별도의 보안 필터 체인을 구성합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Actuator 엔드포인트(/actuator/**)에 대한 보안 필터 체인을 설정합니다.
     * 가장 높은 우선순위(@Order(0))를 부여하여 다른 API 필터 체인보다 먼저 평가되도록 합니다.
     * Prometheus, Kubernetes Liveness/Readiness Probe 등 외부 모니터링 시스템의 접근을 허용하기 위함입니다.
     *
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain Actuator용 보안 필터 체인
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // Actuator 관련 모든 엔드포인트에 대해서만 이 필터 체인을 적용합니다.
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(requests -> requests
                        // 모든 Actuator 엔드포인트는 인증 없이 접근을 허용합니다.
                        .anyRequest().permitAll())
                // Actuator는 보통 시스템 간 호출이므로 CSRF 보호가 불필요합니다.
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Swagger UI 및 OpenAPI 문서 엔드포인트에 대한 보안 필터 체인을 설정합니다.
     *
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain Swagger용 보안 필터 체인
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
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
     * Actuator 필터 체인 다음 순서(@Order(1))로 평가됩니다.
     *
     * ### 권한 부여 전략
     * - **공개 (Permit All)**: `GET` 요청은 누구나 가능 (게시글/카테고리/태그 조회 등).
     * - **인증 필요 (Authenticated)**: 댓글 작성/수정/삭제 등 인증된 사용자만 가능한 기능.
     * - **관리자 전용 (Admin Role)**: `POST`, `PUT`, `DELETE` 요청은 ADMIN 역할이 있는 사용자만 가능 (게시글 생성/수정/삭제 등).
     *
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain API용 보안 필터 체인
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // --- 공개 엔드포인트 (조회) ---
                        .requestMatchers(HttpMethod.GET, "/api/blog").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blog/**").permitAll()

                        // --- 관리자 전용 엔드포인트 (생성, 수정, 삭제) ---
                        .requestMatchers(HttpMethod.POST, "/api/blog").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/blog/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/blog/** ").hasRole("ADMIN")

                        // --- 위에서 지정하지 않은 나머지 모든 요청은 인증만 되면 허용 ---
                        .anyRequest().authenticated()
                )
                // 이 서비스를 OAuth2 리소스 서버로 설정하여 API Gateway로부터 전달받은 JWT를 검증합니다.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()) // common-library에 정의된 JwtAuthenticationConverter가 자동으로 주입되어 JWT의 roles를 GrantedAuthority로 변환합니다.
                )
                // Stateless한 REST API이므로 CSRF 보호를 비활성화합니다.
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}