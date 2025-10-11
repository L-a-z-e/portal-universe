package com.portal.universe.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
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

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
@Slf4j
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    // --- 디버깅을 위한 요청 경로 로깅 필터 ---
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // 모든 필터 중 가장 먼저 실행되도록 설정
    public WebFilter requestPathLoggingFilter() {
        return (exchange, chain) -> {
            // Spring Security가 보기 직전의 요청 경로를 로그로 출력
            log.info(">>>>> INCOMING REQUEST PATH: {}", exchange.getRequest().getPath().value());
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null) {
                log.info(">>>> AUTHORIZATION HEADER: {}", authHeader);
            } else {
                log.warn(">>>> AUTHORIZATION HEADER MISSING");
            }
            return chain.filter(exchange);
        };
    }

    /**
     * Spring Cloud Gateway의 전역 CORS 설정을 담당합니다.
     * SecurityWebFilterChain의 .cors() 설정보다 우선하며, Gateway 단계에서 Preflight 요청을 처리합니다.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:50000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return new CorsWebFilter(source);
    }

    // 완전 공개 경로 처리 (JWT 검증 안 함)
    @Bean
    @Order(1)
    public SecurityWebFilterChain publicEndpointsFilterChain(ServerHttpSecurity http) {
        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                        "/auth-service/.well-known/**",
                        "/auth-service/oauth2/**",
                        "/auth-service/login",
                        "/auth-service/logout",
                        "/auth-service/connect/**",
                        "/api/users/signup"
                ))
                .authorizeExchange(authorize -> authorize.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    // JWT 보호 경로 처리
    @Bean
    @Order(2)
    public SecurityWebFilterChain privateEndpointsFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(authorize -> authorize
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/blog/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/shopping/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    // --- 이 Bean을 아래 내용으로 교체합니다 ---
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        // 1. 내부 주소(jwk-set-uri)를 사용해 키를 가져오는 Decoder를 먼저 생성
        // 시작 시 순환 참조가 발생 방지
        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(this.jwkSetUri).build();

        // 2. 외부 주소(issuer-uri)를 사용해 토큰의 iss 필드를 검증하는 Validator를 추가
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(this.issuerUri);
        jwtDecoder.setJwtValidator(issuerValidator);

        return jwtDecoder;
    }

}
