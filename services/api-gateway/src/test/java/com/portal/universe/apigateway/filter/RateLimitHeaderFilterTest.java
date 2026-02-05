package com.portal.universe.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitHeaderFilter Test")
class RateLimitHeaderFilterTest {

    private RateLimitHeaderFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        filter = new RateLimitHeaderFilter(objectMapper);
    }

    @Test
    @DisplayName("정상 요청은 chain.filter를 호출한다")
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
    @DisplayName("Order는 LOWEST_PRECEDENCE이다")
    void should_returnLowestPrecedenceOrder() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    @DisplayName("낮은 rate에서 올바른 retryAfter를 계산한다")
    void should_calculateRetryAfter_when_lowRate() throws Exception {
        Method method = RateLimitHeaderFilter.class.getDeclaredMethod("calculateRetryAfter", String.class);
        method.setAccessible(true);

        int result = (int) method.invoke(filter, "0.083");

        // ceil(1/0.083) = ceil(12.048) = 13
        assertThat(result).isEqualTo(13);
    }

    @Test
    @DisplayName("높은 rate에서는 기본 60초를 반환한다")
    void should_returnDefaultRetryAfter_when_highRate() throws Exception {
        Method method = RateLimitHeaderFilter.class.getDeclaredMethod("calculateRetryAfter", String.class);
        method.setAccessible(true);

        int result = (int) method.invoke(filter, "10");

        assertThat(result).isEqualTo(60);
    }

    @Test
    @DisplayName("null rate에서 기본 60초를 반환한다")
    void should_returnDefaultRetryAfter_when_nullRate() throws Exception {
        Method method = RateLimitHeaderFilter.class.getDeclaredMethod("calculateRetryAfter", String.class);
        method.setAccessible(true);

        int result = (int) method.invoke(filter, (String) null);

        assertThat(result).isEqualTo(60);
    }

    @Test
    @DisplayName("유효하지 않은 rate에서 기본 60초를 반환한다")
    void should_returnDefaultRetryAfter_when_invalidRate() throws Exception {
        Method method = RateLimitHeaderFilter.class.getDeclaredMethod("calculateRetryAfter", String.class);
        method.setAccessible(true);

        int result = (int) method.invoke(filter, "abc");

        assertThat(result).isEqualTo(60);
    }

    @Test
    @DisplayName("에러 응답은 올바른 구조를 가진다")
    @SuppressWarnings("unchecked")
    void should_createErrorResponse_with_correctStructure() throws Exception {
        Method method = RateLimitHeaderFilter.class.getDeclaredMethod("createErrorResponse", int.class);
        method.setAccessible(true);

        var result = (java.util.Map<String, Object>) method.invoke(filter, 60);

        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("data")).isNull();
        var error = (java.util.Map<String, Object>) result.get("error");
        assertThat(error.get("code")).isEqualTo("TOO_MANY_REQUESTS");
        assertThat((String) error.get("message")).contains("60");
    }
}
