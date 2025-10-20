package com.portal.universe.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.context.annotation.Profile;

import java.net.URI;

@Slf4j
@Profile("kubernetes")
@Component
public class XForwardedHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.error("!!!!!!!!!! XForwardedHeadersFilter IS RUNNING !!!!!!!!!!");
        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();

        // Host 헤더에서 원본 호스트:포트 추출
        String originalHost = request.getHeaders().getFirst("Host");
        if (originalHost == null) {
            originalHost = uri.getHost();
            int port = uri.getPort();
            if (port != -1 && port != 80 && port != 443) {
                originalHost += ":" + port;
            }
        }
        String originalScheme = uri.getScheme();
        String originalPort;
        if (originalHost != null && originalHost.contains(":")) {
            originalPort = originalHost.split(":")[1];
        }
        else {
            // Fallback to original logic if Host header has no port
            int portNumber = uri.getPort();
            if (portNumber == -1) {
                portNumber = originalScheme.equals("https") ? 443 : 80;
            }
            originalPort = String.valueOf(portNumber);
        }

//        // 스키마 (http/https)
//        String originalScheme = uri.getScheme();
//
//        // 포트
//        int portNumber = uri.getPort();
//        if (portNumber == -1) {
//            portNumber = originalScheme.equals("https") ? 443 : 80;
//        }
//        String originalPort = String.valueOf(portNumber);

        // 경로에서 prefix 추출
        String path = uri.getPath();
        String prefix = "";
        if (path.startsWith("/auth-service")) {
            prefix = "/auth-service";
        }

        log.debug("X-Forwarded: Host={}, Proto={}, Port={}, Prefix={}",
                originalHost, originalScheme, originalPort, prefix);

        // X-Forwarded-* 헤더 추가
        ServerHttpRequest.Builder mutatedRequest = request.mutate()
                .header("X-Forwarded-Host", originalHost)
                .header("X-Forwarded-Proto", originalScheme)
                .header("X-Forwarded-Port", originalPort);

        if (!prefix.isEmpty()) {
            mutatedRequest.header("X-Forwarded-Prefix", prefix);
        }

        // 클라이언트 IP
        if (request.getRemoteAddress() != null) {
            String clientIp = request.getRemoteAddress().getAddress().getHostAddress();
            mutatedRequest.header("X-Forwarded-For", clientIp);
        }

        return chain.filter(exchange.mutate().request(mutatedRequest.build()).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2; // requestPathLoggingFilter -> corsWebFilter -> XForwardedHeadersFilter 순서로 실행
    }
}