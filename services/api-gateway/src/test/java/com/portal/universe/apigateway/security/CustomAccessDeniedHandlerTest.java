package com.portal.universe.apigateway.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomAccessDeniedHandler Test")
class CustomAccessDeniedHandlerTest {

    private final CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler();

    @Test
    @DisplayName("403 상태 코드를 반환한다")
    void should_return403_when_accessDenied() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/admin"));
        var denied = new AccessDeniedException("Access denied");

        StepVerifier.create(handler.handle(exchange, denied))
                .expectComplete()
                .verify();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GW-A002 에러 코드가 포함된 JSON body를 반환한다")
    void should_returnJsonBody_when_accessDenied() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/admin"));
        var denied = new AccessDeniedException("Access denied");

        StepVerifier.create(handler.handle(exchange, denied))
                .expectComplete()
                .verify();

        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).contains("\"code\":\"GW-A002\"");
        assertThat(body).contains("\"success\":false");
    }

    @Test
    @DisplayName("Content-Type이 application/json이다")
    void should_setContentTypeJson_when_accessDenied() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/admin"));
        var denied = new AccessDeniedException("Access denied");

        StepVerifier.create(handler.handle(exchange, denied))
                .expectComplete()
                .verify();

        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);
    }
}
