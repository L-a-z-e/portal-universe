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
     * 인증이 필요 없는 공개 경로(Public Endpoints)에 대한 보안 설정을 담당합니다.
     * 해당 경로들은 JWT 토큰 검증 없이 접근이 허용됩니다.
     * @param http ServerHttpSecurity 객체
     * @return SecurityWebFilterChain 공개 경로용 보안 필터 체인
     */
    @Bean
    @Order(1)
    public SecurityWebFilterChain publicEndpointsFilterChain(ServerHttpSecurity http) {
        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                        "/auth-service/**",
                        "/api/users/**",
                        "/actuator/**"
                ))
                .authorizeExchange(authorize -> authorize.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // Stateless한 REST API이므로 CSRF 비활성화
                .build();
    }

    /**
     * JWT 토큰으로 보호되는 비공개 경로(Private Endpoints)에 대한 보안 설정을 담당합니다.
     * Gateway에서는 인증된 사용자인지만 확인하고, 세부적인 권한 제어는 각 마이크로서비스에 위임하는 것을 기본 전략으로 합니다.
     * @param http ServerHttpSecurity 객체
     * @return SecurityWebFilterChain 비공개 경로용 보안 필터 체인
     */
    @Bean
    @Order(2)
    public SecurityWebFilterChain privateEndpointsFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(authorize -> authorize
                        .anyExchange().authenticated() // 위에서 정의한 공개 경로 외 모든 경로는 인증 필요
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        // 인증 실패 시 401 Unauthorized 응답 대신, 후속 필터 체인으로 넘어갈 수 있도록 함
                        // 이는 여러 SecurityWebFilterChain을 연쇄적으로 처리하기 위함
                        .authenticationFailureHandler((exchange, ex) -> Mono.empty())
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