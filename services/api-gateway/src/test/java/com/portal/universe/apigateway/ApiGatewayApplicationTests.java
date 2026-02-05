package com.portal.universe.apigateway;

import com.portal.universe.apigateway.health.ServiceHealthAggregator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * API Gateway 애플리케이션의 기본 통합 테스트 클래스입니다.
 * WebFlux 관련 컴포넌트를 테스트하기 위한 설정을 포함합니다.
 */
@WebFluxTest
@Import(TestSecurityConfig.class)
class ApiGatewayApplicationTests {

    @MockitoBean
    private ServiceHealthAggregator serviceHealthAggregator;

    /**
     * Spring 컨텍스트가 성공적으로 로드되는지 확인하는 기본 테스트입니다.
     */
    @Test
    void contextLoads() {
    }

}