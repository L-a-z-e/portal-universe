package com.portal.universe.apigateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 보안 헤더를 응답에 추가하는 GlobalFilter입니다.
 * X-Content-Type-Options, X-Frame-Options, CSP, HSTS 등을 설정합니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements GlobalFilter {

    private final SecurityHeadersProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        // 응답 커밋 직전에 보안 헤더 추가 (beforeCommit 콜백 사용)
        exchange.getResponse().beforeCommit(() -> {
            addSecurityHeaders(exchange);
            return Mono.empty();
        });

        return chain.filter(exchange);
    }

    private void addSecurityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();
        String path = request.getPath().value();

        if (properties.isContentTypeOptions()) {
            headers.add("X-Content-Type-Options", "nosniff");
        }

        if (properties.getFrameOptions() != null && !properties.getFrameOptions().isEmpty()) {
            headers.add("X-Frame-Options", properties.getFrameOptions());
        }

        if (properties.isXssProtection()) {
            headers.add("X-XSS-Protection", "1; mode=block");
        }

        if (properties.getReferrerPolicy() != null && !properties.getReferrerPolicy().isEmpty()) {
            headers.add("Referrer-Policy", properties.getReferrerPolicy());
        }

        if (properties.getPermissionsPolicy() != null && !properties.getPermissionsPolicy().isEmpty()) {
            headers.add("Permissions-Policy", properties.getPermissionsPolicy());
        }

        addContentSecurityPolicy(headers);
        addHstsHeader(request, headers);
        addCacheControlHeader(path, headers);

        log.debug("Security headers added for path: {}", path);
    }

    private void addContentSecurityPolicy(HttpHeaders headers) {
        SecurityHeadersProperties.CspProperties csp = properties.getCsp();

        if (!csp.isEnabled() || csp.getPolicy() == null || csp.getPolicy().isEmpty()) {
            return;
        }

        String headerName = csp.isReportOnly()
                ? "Content-Security-Policy-Report-Only"
                : "Content-Security-Policy";

        headers.add(headerName, csp.getPolicy());
    }

    /** HTTPS 요청인 경우에만 HSTS 헤더를 적용한다. */
    private void addHstsHeader(ServerHttpRequest request, HttpHeaders headers) {
        SecurityHeadersProperties.HstsProperties hsts = properties.getHsts();

        if (!hsts.isEnabled()) {
            return;
        }

        // HTTPS 요청인지 확인
        boolean isHttps = isHttpsRequest(request);
        if (hsts.isHttpsOnly() && !isHttps) {
            return;
        }

        StringBuilder hstsValue = new StringBuilder();
        hstsValue.append("max-age=").append(hsts.getMaxAge());

        if (hsts.isIncludeSubDomains()) {
            hstsValue.append("; includeSubDomains");
        }

        if (hsts.isPreload()) {
            hstsValue.append("; preload");
        }

        headers.add("Strict-Transport-Security", hstsValue.toString());
    }

    /** 인증 관련 경로에 no-cache 정책을 적용한다. */
    private void addCacheControlHeader(String path, HttpHeaders headers) {
        SecurityHeadersProperties.CacheControlProperties cacheControl = properties.getCacheControl();

        if (!cacheControl.isAuthPaths()) {
            return;
        }

        for (String pattern : cacheControl.getNoCachePaths()) {
            if (pathMatcher.match(pattern, path)) {
                headers.add("Cache-Control", "no-store, no-cache, must-revalidate");
                headers.add("Pragma", "no-cache");
                headers.add("Expires", "0");
                log.debug("Cache-Control no-cache applied for auth path: {}", path);
                return;
            }
        }
    }

    /** X-Forwarded-Proto 우선, 없으면 scheme으로 판별한다. */
    private boolean isHttpsRequest(ServerHttpRequest request) {
        String forwardedProto = request.getHeaders().getFirst("X-Forwarded-Proto");
        if (forwardedProto != null) {
            return "https".equalsIgnoreCase(forwardedProto);
        }

        String scheme = request.getURI().getScheme();
        return "https".equalsIgnoreCase(scheme);
    }
}
