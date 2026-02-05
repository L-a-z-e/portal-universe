package com.portal.universe.apigateway.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomAuthenticationEntryPoint Test")
class CustomAuthenticationEntryPointTest {

    private final CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();

    @Test
    @DisplayName("401 상태 코드를 반환한다")
    void should_return401_when_authFailed() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/protected"));
        var ex = new BadCredentialsException("Bad credentials");

        StepVerifier.create(entryPoint.commence(exchange, ex))
                .expectComplete()
                .verify();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("A001 에러 코드가 포함된 JSON body를 반환한다")
    void should_returnJsonBody_when_authFailed() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/protected"));
        var ex = new BadCredentialsException("Bad credentials");

        StepVerifier.create(entryPoint.commence(exchange, ex))
                .expectComplete()
                .verify();

        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).contains("\"code\":\"A001\"");
        assertThat(body).contains("\"success\":false");
    }

    @Test
    @DisplayName("Content-Type이 application/json이다")
    void should_setContentTypeJson_when_authFailed() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/protected"));
        var ex = new BadCredentialsException("Bad credentials");

        StepVerifier.create(entryPoint.commence(exchange, ex))
                .expectComplete()
                .verify();

        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("WWW-Authenticate 헤더가 포함되지 않는다")
    void should_notContainWwwAuthenticate_when_authFailed() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/protected"));
        var ex = new BadCredentialsException("Bad credentials");

        StepVerifier.create(entryPoint.commence(exchange, ex))
                .expectComplete()
                .verify();

        assertThat(exchange.getResponse().getHeaders().get("WWW-Authenticate")).isNull();
    }
}
