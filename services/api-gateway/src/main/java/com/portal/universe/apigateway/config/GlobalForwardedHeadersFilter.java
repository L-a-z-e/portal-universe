package com.portal.universe.apigateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 전역 X-Forwarded 헤더 필터
 * 추가 헤더:
 * - X-Forwarded-Host: portal-universe:30000
 * - X-Forwarded-Proto: https
 * - X-Forwarded-Port: 30000
 * - X-Forwarded-For: 클라이언트 IP
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalForwardedHeadersFilter implements GlobalFilter, Ordered {

    private final FrontendProperties frontendProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String forwardedHost = frontendProperties.getHost();
        String forwardedScheme = frontendProperties.getScheme();
        String forwardedPort = String.valueOf(frontendProperties.getPort());
        String forwardedFor = extractClientIp(request);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Forwarded-Host", forwardedHost)
                .header("X-Forwarded-Proto", forwardedScheme)
                .header("X-Forwarded-Port", forwardedPort)
                .header("X-Forwarded-For", forwardedFor)
                .build();

        log.debug("[Global Forwarded] Host={}, Proto={}, Port={}, For={}, Path={}",
                forwardedHost, forwardedScheme, forwardedPort, forwardedFor, request.getPath());

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 클라이언트 IP 추출
     * 1. 기존 X-Forwarded-For 헤더 확인 (프록시 체인)
     * 2. X-Real-IP 헤더 확인
     * 3. Remote Address 사용
     */
    private String extractClientIp(ServerHttpRequest request) {
        String existingForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (existingForwardedFor != null && !existingForwardedFor.isEmpty()) {
            return existingForwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    @Override
    public int getOrder() {
        // GlobalLoggingFilter보다 먼저 실행되도록 설정
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
