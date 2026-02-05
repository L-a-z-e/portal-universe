package com.portal.universe.apigateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("GlobalLoggingFilter Test")
class GlobalLoggingFilterTest {

    private final GlobalLoggingFilter filter = new GlobalLoggingFilter();

    @Test
    @DisplayName("chain.filter를 호출한다")
    void should_callChainFilter_when_requestReceived() {
        var request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        var exchange = MockServerWebExchange.from(request);
        var chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("필터 실행 후 정상 완료된다")
    void should_completeSuccessfully_when_normalRequest() {
        var request = MockServerHttpRequest.get("/api/test")
                .header("Authorization", "Bearer some-token")
                .header("Content-Type", "application/json")
                .remoteAddress(new InetSocketAddress("10.0.0.1", 8080))
                .build();
        var exchange = MockServerWebExchange.from(request);
        var chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("POST 요청도 정상 처리된다")
    void should_handlePostRequest() {
        var request = MockServerHttpRequest.post("/api/auth/login")
                .remoteAddress(new InetSocketAddress("10.0.0.1", 8080))
                .build();
        var exchange = MockServerWebExchange.from(request);
        var chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .expectComplete()
                .verify();

        verify(chain).filter(exchange);
    }
}
