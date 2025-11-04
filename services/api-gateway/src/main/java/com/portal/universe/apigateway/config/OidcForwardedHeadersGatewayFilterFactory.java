package com.portal.universe.apigateway.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * OIDC 인증 관련 라우트에서 프론트엔드 호스트 정보를 X-Forwarded-* 헤더로 설정하는 필터
 */
@Component
@Slf4j
public class OidcForwardedHeadersGatewayFilterFactory
        extends AbstractGatewayFilterFactory<OidcForwardedHeadersGatewayFilterFactory.Config> {

    private final FrontendProperties frontendProperties;

    // 명시적 생성자 작성
    public OidcForwardedHeadersGatewayFilterFactory(FrontendProperties frontendProperties) {
        super(Config.class);
        this.frontendProperties = frontendProperties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OidcForwardedHeadersFilter(config, frontendProperties);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("enabled");
    }

    /**
     * 설정 클래스
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private boolean enabled = true;
    }

    /**
     * 실제 필터 구현
     */
    private static class OidcForwardedHeadersFilter implements GatewayFilter, Ordered {

        private final Config config;
        private final FrontendProperties frontendProperties;

        public OidcForwardedHeadersFilter(Config config, FrontendProperties frontendProperties) {
            this.config = config;
            this.frontendProperties = frontendProperties;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            if (!config.isEnabled()) {
                return chain.filter(exchange);
            }

            ServerHttpRequest request = exchange.getRequest();

            // 프론트엔드 정보로 X-Forwarded 헤더 설정
            String forwardedHost = frontendProperties.getHost();
            String forwardedScheme = frontendProperties.getScheme();
            String forwardedPort = String.valueOf(frontendProperties.getPort());
            String forwardedFor = getClientIp(request);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Forwarded-Host", forwardedHost)
                    .header("X-Forwarded-Proto", forwardedScheme)
                    .header("X-Forwarded-Port", forwardedPort)
                    .header("X-Forwarded-Prefix", "/auth-service")  // 이 라인 추가
                    .header("X-Forwarded-For", forwardedFor)
                    .build();

            log.debug("🔐 [OIDC Headers] Host: {}, Proto: {}, Port: {}",
                    forwardedHost, forwardedScheme, forwardedPort);

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        private String getClientIp(ServerHttpRequest request) {
            // 기존 X-Forwarded-For가 있으면 체인 유지
            String existingForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
            String clientIp = request.getRemoteAddress() != null ?
                    request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

            return existingForwardedFor != null ?
                    existingForwardedFor + ", " + clientIp : clientIp;
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE + 10; // X-Forwarded 헤더는 일찍 처리
        }
    }
}