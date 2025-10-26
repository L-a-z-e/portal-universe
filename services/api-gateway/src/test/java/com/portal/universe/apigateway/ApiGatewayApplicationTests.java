package com.portal.universe.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;

/**
 * API Gateway 애플리케이션의 기본 통합 테스트 클래스입니다.
 * WebFlux 관련 컴포넌트를 테스트하기 위한 설정을 포함합니다.
 */
@WebFluxTest
@Import(TestSecurityConfig.class) // 테스트용 보안 설정을 가져옵니다.
class ApiGatewayApplicationTests {

    /**
     * Spring 컨텍스트가 성공적으로 로드되는지 확인하는 기본 테스트입니다.
     */
    @Test
    void contextLoads() {
    }

}