package com.portal.universe.apigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 공개 경로 설정을 관리하는 Properties 클래스입니다.
 * application.yml의 gateway.public-paths 섹션과 매핑됩니다.
 *
 * <p>SecurityConfig와 JwtAuthenticationFilter에서 공유하며,
 * 경로 추가 시 YAML만 수정하면 됩니다.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.public-paths")
public class PublicPathProperties {

    /**
     * 모든 HTTP method에 대해 인증 없이 접근 가능한 경로 (SecurityConfig permitAll)
     */
    private List<String> permitAll = List.of(
            "/auth-service/**",
            "/api/auth/**",
            "/api/users/**",
            "/api/shopping/products",
            "/api/shopping/products/**",
            "/api/shopping/categories",
            "/api/shopping/categories/**",
            "/api/shopping/coupons",
            "/api/shopping/time-deals",
            "/api/shopping/time-deals/**",
            "/api/prism/sse/**",
            "/api/prism/health",
            "/api/prism/ready",
            "/actuator/**",
            "/api/*/actuator/**"
    );

    /**
     * GET method에 대해서만 인증 없이 접근 가능한 경로 (SecurityConfig permitAll GET)
     */
    private List<String> permitAllGet = List.of(
            "/api/blog/**",
            "/api/memberships/tiers/**"
    );

    /**
     * JWT 파싱 자체를 skip하는 경로 prefix (JwtAuthenticationFilter용).
     * SecurityConfig의 permitAll과는 별도로 관리됩니다.
     * 예: /api/blog/** GET은 permitAll이지만 JWT가 있으면 파싱하여 사용자 정보를 추출합니다.
     */
    private List<String> skipJwtParsing = List.of(
            "/auth-service/",
            "/api/auth/",
            "/api/users/",
            "/actuator/",
            "/api/shopping/products",
            "/api/shopping/categories",
            "/api/prism/health",
            "/api/prism/ready",
            "/api/prism/sse/"
    );
}
