package com.portal.universe.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * API Gateway의 보안 관련 설정을 담당하는 클래스입니다.
 * Spring Security의 WebFlux 지원을 활성화하고, CORS, 경로별 접근 제어, JWT 검증 등을 설정합니다.
 */
@Configuration
@EnableWebFluxSecurity
@Profile("!test")
@Slf4j
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * 모든 들어오는 요청의 경로와 HTTP 메서드를 디버그 레벨로 로깅하는 필터입니다.
     * 다른 필터들보다 가장 먼저 실행되어 모든 요청을 추적할 수 있도록 합니다.
     * @return WebFilter 요청 로깅 필터 Bean
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter requestPathLoggingFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();
            log.debug("Request: {} {}", method, path);

            return chain.filter(exchange);
        };
    }

    /**
     * Spring Cloud Gateway의 전역 CORS(Cross-Origin Resource Sharing) 설정을 담당합니다.
     * Gateway 단계에서 Preflight 요청(OPTIONS)을 처리하여 각 마이크로서비스의 CORS 부담을 덜어줍니다.
     * SecurityWebFilterChain의 .cors() 설정보다 우선적으로 적용됩니다.
     * @return CorsWebFilter CORS 처리 필터 Bean
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:30000",
                "http://localhost:8080",
                "https://portal-universe:30000"
                ));
        configuration.addAllowedOrigin("null"); // 로컬 개발 환경 등에서 Origin이 'null'인 경우 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Preflight 요청 결과를 3600초(1시간) 동안 캐시

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return new CorsWebFilter(source);
    }

    /**
     * 전체 API에 대한 통합 보안 설정을 담당합니다.
     * @param http ServerHttpSecurity 객체
     * @return SecurityWebFilterChain 보안 필터 체인
     */
    @Bean
    @Order(1)
    public SecurityWebFilterChain apiSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(authorize -> authorize
                        // ========================================
                        // [공개] 인증 없이 접근 가능
                        // ========================================
                        .pathMatchers("/auth-service/**").permitAll()
                        .pathMatchers("/api/users/**").permitAll()
                        // Shopping Service - 상품/카테고리 조회는 공개
                        .pathMatchers("/api/shopping/products", "/api/shopping/products/**").permitAll()
                        .pathMatchers("/api/shopping/categories", "/api/shopping/categories/**").permitAll()
                        // 쿠폰/타임딜 목록 조회도 공개
                        .pathMatchers("/api/shopping/coupons", "/api/shopping/time-deals").permitAll()
                        .pathMatchers("/api/shopping/time-deals/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()

                        // ========================================
                        // [관리자] ADMIN 권한 필요
                        // ========================================
                        .pathMatchers("/api/shopping/admin/**").hasRole("ADMIN")

                        // ========================================
                        // [비공개] 인증 필요
                        // ========================================
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    /**
     * JWT 토큰의 서명을 검증하고 디코딩하는 역할을 담당합니다.
     * 내부망(Docker Network)에서는 jwk-set-uri로 키를 가져오고,
     * 외부에서 발급된 토큰의 issuer(iss) 필드는 issuer-uri로 검증하여 보안을 강화합니다.
     * @return ReactiveJwtDecoder JWT 디코더 Bean
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        // 1. 내부 주소(jwk-set-uri)를 사용해 키 세트를 가져오는 Decoder를 먼저 생성합니다.
        //    (애플리케이션 시작 시 순환 참조 문제 방지)
        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(this.jwkSetUri).build();

        // 2. 외부 주소(issuer-uri)를 기준으로 토큰의 'iss' 필드를 검증하는 Validator를 추가합니다.
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(this.issuerUri);
        jwtDecoder.setJwtValidator(issuerValidator);

        return jwtDecoder;
    }

}