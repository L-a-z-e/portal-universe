package com.portal.universe.apigateway;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Boot 테스트를 위한 보안 관련 테스트 설정 클래스입니다.
 * 테스트 환경에서는 JWT 검증을 비활성화하고 모든 요청을 허용합니다.
 *
 * 참고: 프로덕션에서는 HMAC JWT 방식(JwtAuthenticationFilter)을 사용합니다.
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * 테스트용 보안 필터 체인
     * 모든 요청을 허용하여 JWT 검증 없이 테스트 가능하도록 합니다.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityWebFilterChain testSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}
