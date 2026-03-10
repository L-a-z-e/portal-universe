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

    private boolean enabled = true;

    /** DENY, SAMEORIGIN */
    private String frameOptions = "DENY";

    private boolean contentTypeOptions = true;

    private boolean xssProtection = true;

    private String referrerPolicy = "strict-origin-when-cross-origin";

    private String permissionsPolicy = "geolocation=(), microphone=(), camera=()";

    private CspProperties csp = new CspProperties();

    private HstsProperties hsts = new HstsProperties();

    private CacheControlProperties cacheControl = new CacheControlProperties();

    @Data
    public static class CspProperties {
        private boolean enabled = true;

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
        private boolean enabled = true;

        /** 초 단위, 기본 1년 */
        private long maxAge = 31536000;

        private boolean includeSubDomains = true;

        private boolean preload = false;

        private boolean httpsOnly = true;
    }

    @Data
    public static class CacheControlProperties {
        /** 인증 경로에 no-cache 적용 여부 */
        private boolean authPaths = true;

        private String[] noCachePaths = {
                "/api/auth/**",
                "/auth-service/**",
                "/api/users/**",
                "/api/*/profile/**"
        };
    }
}
