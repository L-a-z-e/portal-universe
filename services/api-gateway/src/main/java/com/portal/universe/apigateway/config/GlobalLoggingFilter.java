package com.portal.universe.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class GlobalLoggingFilter implements GlobalFilter {

    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "cookie");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String requestPath = request.getPath().value();
        String method = request.getMethod().name();
        String clientIp = getClientIp(request, exchange);

        log.info("API_REQUEST - Method: {}, Path: {}, IP: {}, Headers: {}",
                method, requestPath, clientIp, maskSensitiveHeaders(request.getHeaders().toSingleValueMap()));

        return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    ServerHttpResponse response = exchange.getResponse();
                    long duration = System.currentTimeMillis() - startTime;

                    log.info("API_RESPONSE - Path: {}, Status: {}, Duration: {}ms",
                            requestPath, response.getStatusCode(), duration);
                })
        );
    }

    private Map<String, String> maskSensitiveHeaders(Map<String, String> headers) {
        Map<String, String> masked = new LinkedHashMap<>(headers);
        masked.replaceAll((key, value) ->
                SENSITIVE_HEADERS.contains(key.toLowerCase()) ? "***" : value);
        return masked;
    }

    private String getClientIp(ServerHttpRequest request, ServerWebExchange exchange) {
        XForwardedRemoteAddressResolver resolver = XForwardedRemoteAddressResolver.maxTrustedIndex(1);
        InetSocketAddress remoteAddress = resolver.resolve(exchange);

        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "Unknown";
    }
}