package com.portal.universe.apigateway.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * OIDC 전용 Prefix 필터
 */
@Component
@Slf4j
public class OidcPrefixGatewayFilterFactory
        extends AbstractGatewayFilterFactory<OidcPrefixGatewayFilterFactory.Config> {

    public OidcPrefixGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {
            if (!config.isEnabled()) {
                return chain.filter(exchange);
            }

            ServerHttpRequest request = exchange.getRequest();

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Forwarded-Prefix", "/auth-service")
                    .build();

            log.debug("🔐 [OIDC Prefix] Added X-Forwarded-Prefix=/auth-service for path: {}",
                    request.getPath());

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("enabled");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private boolean enabled = true;
    }
}