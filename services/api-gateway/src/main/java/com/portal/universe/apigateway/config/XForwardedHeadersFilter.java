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

/**
 * Kubernetes 환경에서 Ingress를 통해 들어온 요청의 원본 정보를 보존하기 위한 글로벌 필터입니다.
 * Ingress Controller가 추가하는 'X-Forwarded-*' 헤더들을 기반으로, 백엔드 서비스가 클라이언트의 실제 요청 정보를
 * (Host, Protocol, Port 등) 인식할 수 있도록 새로운 'X-Forwarded-*' 헤더를 추가/수정합니다.
 * 이 필터는 'kubernetes' 프로파일이 활성화될 때만 동작합니다.
 */
@Slf4j
@Profile("kubernetes")
@Component
public class XForwardedHeadersFilter implements GlobalFilter, Ordered {

    /**
     * 들어오는 모든 요청을 가로채 'X-Forwarded-*' 헤더를 추가하는 필터 로직을 수행합니다.
     * @param exchange 현재 요청-응답 컨텍스트
     * @param chain 다음 필터로 요청을 전달하기 위한 체인
     * @return 필터 체인의 다음 단계로 진행하기 위한 Mono<Void>
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();

        // Host 헤더에서 원본 호스트와 포트를 추출합니다.
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
        } else {
            int portNumber = uri.getPort();
            if (portNumber == -1) {
                portNumber = originalScheme.equals("https") ? 443 : 80;
            }
            originalPort = String.valueOf(portNumber);
        }

        // 경로에서 prefix 추출 (예: /auth-service)
        String path = uri.getPath();
        String prefix = "";
        if (path.startsWith("/auth-service")) {
            prefix = "/auth-service";
        }

        log.debug("X-Forwarded Headers added: Host={}, Proto={}, Port={}, Prefix={}",
                originalHost, originalScheme, originalPort, prefix);

        // 요청 객체를 변경하여 X-Forwarded-* 헤더를 추가합니다.
        ServerHttpRequest.Builder mutatedRequest = request.mutate()
                .header("X-Forwarded-Host", originalHost)
                .header("X-Forwarded-Proto", originalScheme)
                .header("X-Forwarded-Port", originalPort);

        if (!prefix.isEmpty()) {
            mutatedRequest.header("X-Forwarded-Prefix", prefix);
        }

        // 클라이언트의 실제 IP 주소를 X-Forwarded-For 헤더에 추가합니다.
        if (request.getRemoteAddress() != null) {
            String clientIp = request.getRemoteAddress().getAddress().getHostAddress();
            mutatedRequest.header("X-Forwarded-For", clientIp);
        }

        return chain.filter(exchange.mutate().request(mutatedRequest.build()).build());
    }

    /**
     * 필터의 실행 순서를 지정합니다.
     * SecurityConfig에 정의된 다른 필터들 이후에 실행되도록 설정합니다.
     * @return 필터 순서 값
     */
    @Override
    public int getOrder() {
        // requestPathLoggingFilter -> corsWebFilter -> XForwardedHeadersFilter 순서로 실행
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
