package com.portal.universe.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitHeaderFilter")
class RateLimitHeaderFilterTest {

    private RateLimitHeaderFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitHeaderFilter(new ObjectMapper());
    }

    @Nested
    @DisplayName("filter")
    class Filter {

        @Test
        @DisplayName("should_callChainFilter_when_normalRequest")
        void should_callChainFilter_when_normalRequest() {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());
            var chain = mock(GatewayFilterChain.class);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("should_returnLowestPrecedenceOrder")
        void should_returnLowestPrecedenceOrder() {
            assertThat(filter.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
        }
    }

    @Nested
    @DisplayName("calculateRetryAfter")
    class CalculateRetryAfter {

        @Test
        @DisplayName("should_calculateRetryAfter_when_lowRate")
        void should_calculateRetryAfter_when_lowRate() throws Exception {
            int result = invokeCalculateRetryAfter("0.083");

            assertThat(result).isEqualTo(13);
        }

        @Test
        @DisplayName("should_returnDefault60_when_highRate")
        void should_returnDefault60_when_highRate() throws Exception {
            int result = invokeCalculateRetryAfter("10");

            assertThat(result).isEqualTo(60);
        }

        @Test
        @DisplayName("should_returnDefault60_when_nullRate")
        void should_returnDefault60_when_nullRate() throws Exception {
            int result = invokeCalculateRetryAfter(null);

            assertThat(result).isEqualTo(60);
        }

        @Test
        @DisplayName("should_returnDefault60_when_invalidRate")
        void should_returnDefault60_when_invalidRate() throws Exception {
            int result = invokeCalculateRetryAfter("abc");

            assertThat(result).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("createErrorResponse")
    class CreateErrorResponse {

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("should_createCorrectStructure_when_called")
        void should_createCorrectStructure_when_called() throws Exception {
            Method method = RateLimitHeaderFilter.class.getDeclaredMethod("createErrorResponse", int.class);
            method.setAccessible(true);

            var result = (Map<String, Object>) method.invoke(filter, 60);

            assertThat(result.get("success")).isEqualTo(false);
            assertThat(result.get("data")).isNull();
            @SuppressWarnings("unchecked")
            var error = (Map<String, Object>) result.get("error");
            assertThat(error.get("code")).isEqualTo("GW-R001");
            assertThat((String) error.get("message")).contains("60");
        }
    }

    // ── Helper ──

    private int invokeCalculateRetryAfter(String rate) throws Exception {
        Method method = RateLimitHeaderFilter.class.getDeclaredMethod("calculateRetryAfter", String.class);
        method.setAccessible(true);
        return (int) method.invoke(filter, rate);
    }
}
