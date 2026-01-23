package com.portal.universe.apigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 보안 헤더 설정을 관리하는 Properties 클래스입니다.
 * application.yml의 security.headers 섹션과 매핑됩니다.
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.headers")
public class SecurityHeadersProperties {

    /**
     * 보안 헤더 적용 여부
     */
    private boolean enabled = true;

    /**
     * X-Frame-Options 헤더 값 (DENY, SAMEORIGIN)
     */
    private String frameOptions = "DENY";

    /**
     * X-Content-Type-Options 헤더 적용 여부
     */
    private boolean contentTypeOptions = true;

    /**
     * X-XSS-Protection 헤더 적용 여부
     */
    private boolean xssProtection = true;

    /**
     * Referrer-Policy 헤더 값
     */
    private String referrerPolicy = "strict-origin-when-cross-origin";

    /**
     * Permissions-Policy 헤더 값
     */
    private String permissionsPolicy = "geolocation=(), microphone=(), camera=()";

    /**
     * Content Security Policy 설정
     */
    private CspProperties csp = new CspProperties();

    /**
     * HSTS (HTTP Strict Transport Security) 설정
     */
    private HstsProperties hsts = new HstsProperties();

    /**
     * Cache-Control 설정
     */
    private CacheControlProperties cacheControl = new CacheControlProperties();

    @Data
    public static class CspProperties {
        /**
         * CSP 적용 여부
         */
        private boolean enabled = true;

        /**
         * CSP 정책 문자열
         */
        private String policy = "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self' https://fonts.gstatic.com; " +
                "connect-src 'self'";

        /**
         * Report-Only 모드 (위반 보고만 하고 차단하지 않음)
         */
        private boolean reportOnly = false;
    }

    @Data
    public static class HstsProperties {
        /**
         * HSTS 적용 여부
         */
        private boolean enabled = true;

        /**
         * max-age 값 (초 단위, 기본 1년)
         */
        private long maxAge = 31536000;

        /**
         * includeSubDomains 옵션
         */
        private boolean includeSubDomains = true;

        /**
         * preload 옵션
         */
        private boolean preload = false;

        /**
         * HTTPS 환경에서만 적용 여부
         */
        private boolean httpsOnly = true;
    }

    @Data
    public static class CacheControlProperties {
        /**
         * 인증 경로에 no-cache 헤더 적용 여부
         */
        private boolean authPaths = true;

        /**
         * no-cache 적용 경로 패턴 목록
         */
        private String[] noCachePaths = {
                "/api/auth/**",
                "/auth-service/**",
                "/api/users/**",
                "/api/*/profile/**"
        };
    }
}
