package com.portal.universe.blogservice.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    /**
     * Actuator 엔드포인트에 대한 보안 필터 체인.
     * Prometheus, Health Check 등 모니터링 도구의 접근을 허용합니다.
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
     * 애플리케이션의 주요 API에 대한 보안 필터 체인.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        // Actuator를 제외한 모든 요청은 인증된 사용자만 접근 가능
                        .anyRequest().authenticated())
                // OAuth2 리소스 서버로 설정하여 JWT 토큰을 검증
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));

        return http.build();
    }
}
