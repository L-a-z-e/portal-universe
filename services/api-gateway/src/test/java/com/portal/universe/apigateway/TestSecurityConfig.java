package com.portal.universe.apigateway;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import static org.mockito.Mockito.mock;

/**
 * Spring Boot 테스트를 위한 보안 관련 테스트 설정 클래스입니다.
 * 실제 JWT 검증 로직 대신 Mock 객체를 주입하여 테스트 환경을 제어합니다.
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * 실제 JWT Decoder를 Mockito를 사용한 Mock 객체로 대체합니다.
     * 이를 통해 외부 인증 서버에 의존하지 않고 보안 관련 로직을 테스트할 수 있습니다.
     * @return Mock으로 만들어진 ReactiveJwtDecoder Bean
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return mock(ReactiveJwtDecoder.class);
    }
}