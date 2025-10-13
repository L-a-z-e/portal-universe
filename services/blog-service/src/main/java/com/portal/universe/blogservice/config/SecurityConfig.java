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

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Actuator 엔드포인트에 대한 보안 필터 체인
     *
     * 우선순위: 0 (가장 먼저 평가)
     * 대상: /actuator/**
     * 보안: 인증 없이 접근 허용 (Prometheus, Health Check 등)
     */
    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // Actuator 관련 모든 엔드포인트에 대해서만 이 필터 체인을 적용
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(requests -> requests
                        // 모든 Actuator 엔드포인트는 인증 없이 접근 허용
                        .anyRequest().permitAll())
                // Actuator는 보통 외부 시스템이 호출하므로 CSRF 보호가 불필요
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Blog Service API에 대한 보안 필터 체인
     *
     * 우선순위: 1 (Actuator 다음 평가)
     * 대상: /actuator를 제외한 모든 요청
     *
     * 권한 전략:
     * - 공개: GET 요청 (블로그 글, 카테고리, 태그 조회)
     * - 인증 필요: 댓글 작성/수정/삭제
     * - ADMIN 전용: 블로그 글/카테고리/태그 관리
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        //  공개 엔드포인트 (누구나 접근 가능)
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                        // 블로그 글 조회
                        .requestMatchers(HttpMethod.GET, "/articles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/articles/**").permitAll()

                        // 카테고리 조회
                        .requestMatchers(HttpMethod.GET, "/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()

                        // 태그 조회
                        .requestMatchers(HttpMethod.GET, "/tags").permitAll()
                        .requestMatchers(HttpMethod.GET, "/tags/**").permitAll()

                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        //  인증 필요 (로그인한 사용자면 누구나)
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                        // 댓글 작성/수정/삭제
                        .requestMatchers(HttpMethod.POST, "/comments").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/comments/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/comments/**").authenticated()

                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        //  ADMIN 전용
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                        // 블로그 글 작성/수정/삭제
                        .requestMatchers(HttpMethod.POST, "/articles").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/articles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/articles/**").hasRole("ADMIN")

                        // 카테고리 관리
                        .requestMatchers(HttpMethod.POST, "/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/categories/**").hasRole("ADMIN")

                        // 태그 관리
                        .requestMatchers(HttpMethod.POST, "/tags").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/tags/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/tags/**").hasRole("ADMIN")

                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        //  기타 모든 요청은 인증 필요
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        .anyRequest().authenticated()
                )
                // OAuth2 리소스 서버로 설정하여 JWT 토큰을 검증
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())  // ✅ common-library의 JwtAuthenticationConverter 자동 주입!
                )
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
