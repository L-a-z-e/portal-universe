package com.portal.universe.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;

@WebFluxTest
@Import(TestSecurityConfig.class)
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
