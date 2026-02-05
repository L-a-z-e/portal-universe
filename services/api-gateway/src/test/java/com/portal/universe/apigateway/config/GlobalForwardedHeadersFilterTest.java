package com.portal.universe.apigateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GlobalForwardedHeadersFilter Test")
class GlobalForwardedHeadersFilterTest {

    @Mock
    private FrontendProperties frontendProperties;

    @Mock
    private GatewayFilterChain chain;

    private GlobalForwardedHeadersFilter filter;

    @BeforeEach
    void setUp() {
        when(frontendProperties.getHost()).thenReturn("portal-universe");
        when(frontendProperties.getScheme()).thenReturn("https");
        when(frontendProperties.getPort()).thenReturn(30000);
        filter = new GlobalForwardedHeadersFilter(frontendProperties);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("X-Forwarded 헤더를 추가한다")
    void should_addForwardedHeaders_when_normalRequest() {
        var request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        var exchange = MockServerWebExchange.from(request);

        var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        filter.filter(exchange, chain).block();

        verify(chain).filter(captor.capture());
        var mutated = captor.getValue().getRequest();
        assertThat(mutated.getHeaders().getFirst("X-Forwarded-Host")).isEqualTo("portal-universe");
        assertThat(mutated.getHeaders().getFirst("X-Forwarded-Proto")).isEqualTo("https");
        assertThat(mutated.getHeaders().getFirst("X-Forwarded-Port")).isEqualTo("30000");
    }

    @Test
    @DisplayName("X-Forwarded-For 체인에서 첫 번째 IP를 추출한다")
    void should_extractClientIp_from_XForwardedFor() {
        var request = MockServerHttpRequest.get("/api/test")
                .header("X-Forwarded-For", "10.0.0.1, 10.0.0.2")
                .build();
        var exchange = MockServerWebExchange.from(request);

        var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        filter.filter(exchange, chain).block();

        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders().getFirst("X-Forwarded-For"))
                .isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("X-Real-IP 헤더에서 IP를 추출한다")
    void should_extractClientIp_from_XRealIp() {
        var request = MockServerHttpRequest.get("/api/test")
                .header("X-Real-IP", "10.0.0.5")
                .build();
        var exchange = MockServerWebExchange.from(request);

        var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        filter.filter(exchange, chain).block();

        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders().getFirst("X-Forwarded-For"))
                .isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("RemoteAddress에서 IP를 추출한다")
    void should_extractClientIp_from_remoteAddress() {
        var request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("192.168.1.1", 8080))
                .build();
        var exchange = MockServerWebExchange.from(request);

        var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        filter.filter(exchange, chain).block();

        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders().getFirst("X-Forwarded-For"))
                .isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("IP를 얻을 수 없으면 unknown을 반환한다")
    void should_returnUnknown_when_noIpAvailable() {
        var request = MockServerHttpRequest.get("/api/test").build();
        var exchange = MockServerWebExchange.from(request);

        var captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        filter.filter(exchange, chain).block();

        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders().getFirst("X-Forwarded-For"))
                .isEqualTo("unknown");
    }

    @Test
    @DisplayName("Order는 HIGHEST_PRECEDENCE + 1이다")
    void should_haveCorrectOrder() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
    }
}
