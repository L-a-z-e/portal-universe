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
        // 보안 헤더가 비활성화되어 있으면 스킵
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

    /**
     * 응답에 보안 헤더를 추가합니다.
     */
    private void addSecurityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();
        String path = request.getPath().value();

        // X-Content-Type-Options: nosniff
        if (properties.isContentTypeOptions()) {
            headers.add("X-Content-Type-Options", "nosniff");
        }

        // X-Frame-Options
        if (properties.getFrameOptions() != null && !properties.getFrameOptions().isEmpty()) {
            headers.add("X-Frame-Options", properties.getFrameOptions());
        }

        // X-XSS-Protection
        if (properties.isXssProtection()) {
            headers.add("X-XSS-Protection", "1; mode=block");
        }

        // Referrer-Policy
        if (properties.getReferrerPolicy() != null && !properties.getReferrerPolicy().isEmpty()) {
            headers.add("Referrer-Policy", properties.getReferrerPolicy());
        }

        // Permissions-Policy
        if (properties.getPermissionsPolicy() != null && !properties.getPermissionsPolicy().isEmpty()) {
            headers.add("Permissions-Policy", properties.getPermissionsPolicy());
        }

        // Content-Security-Policy
        addContentSecurityPolicy(headers);

        // HSTS (Strict-Transport-Security)
        addHstsHeader(request, headers);

        // Cache-Control (인증 관련 경로)
        addCacheControlHeader(path, headers);

        log.debug("Security headers added for path: {}", path);
    }

    /**
     * Content-Security-Policy 헤더를 추가합니다.
     */
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

    /**
     * HSTS (HTTP Strict Transport Security) 헤더를 추가합니다.
     * HTTPS 요청인 경우에만 적용됩니다.
     */
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

    /**
     * Cache-Control 헤더를 추가합니다.
     * 인증 관련 경로에는 no-cache 정책을 적용합니다.
     */
    private void addCacheControlHeader(String path, HttpHeaders headers) {
        SecurityHeadersProperties.CacheControlProperties cacheControl = properties.getCacheControl();

        if (!cacheControl.isAuthPaths()) {
            return;
        }

        // 설정된 경로 패턴과 매칭되는지 확인
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

    /**
     * HTTPS 요청인지 확인합니다.
     * X-Forwarded-Proto 헤더를 우선 확인하고, 없으면 scheme을 확인합니다.
     */
    private boolean isHttpsRequest(ServerHttpRequest request) {
        // X-Forwarded-Proto 헤더 확인 (프록시 환경)
        String forwardedProto = request.getHeaders().getFirst("X-Forwarded-Proto");
        if (forwardedProto != null) {
            return "https".equalsIgnoreCase(forwardedProto);
        }

        // 직접 연결인 경우 scheme 확인
        String scheme = request.getURI().getScheme();
        return "https".equalsIgnoreCase(scheme);
    }
}
