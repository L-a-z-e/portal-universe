package com.portal.universe.apigateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("GlobalLoggingFilter")
class GlobalLoggingFilterTest {

    private final GlobalLoggingFilter filter = new GlobalLoggingFilter();

    @Nested
    @DisplayName("filter")
    class Filter {

        @Test
        @DisplayName("should_callChainFilter_when_getRequest")
        void should_callChainFilter_when_getRequest() {
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
        @DisplayName("should_completeSuccessfully_when_requestWithHeaders")
        void should_completeSuccessfully_when_requestWithHeaders() {
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
        @DisplayName("should_handlePostRequest")
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
}
