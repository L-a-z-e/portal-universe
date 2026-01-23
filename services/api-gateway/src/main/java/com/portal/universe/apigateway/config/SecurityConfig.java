package com.portal.universe.apigateway.config;

import com.portal.universe.apigateway.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;

import java.util.Arrays;
import java.util.List;

/**
 * API Gateway의 보안 관련 설정을 담당하는 클래스입니다.
 * Spring Security의 WebFlux 지원을 활성화하고, CORS, 경로별 접근 제어 등을 설정합니다.
 *
 * JWT 검증은 JwtAuthenticationFilter에서 HMAC 방식으로 수행합니다.
 * (Auth Service와 동일한 secret key 사용)
 */
@Configuration
@EnableWebFluxSecurity
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProperties jwtProperties;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProperties);
    }

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
            String origin = exchange.getRequest().getHeaders().getOrigin();
            String host = exchange.getRequest().getHeaders().getHost() != null
                ? exchange.getRequest().getHeaders().getHost().toString() : "null";
            log.debug("Request: {} {} | Origin: {} | Host: {}", method, path, origin, host);

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
     * JWT 검증은 JwtAuthenticationFilter에서 HMAC 방식으로 수행하므로,
     * 여기서는 경로별 접근 제어만 설정합니다.
     *
     * @param http ServerHttpSecurity 객체
     * @return SecurityWebFilterChain 보안 필터 체인
     */
    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(authorize -> authorize
                        // ========================================
                        // [공개] 인증 없이 접근 가능
                        // ========================================
                        // Auth Service 경로 (자체 검증)
                        .pathMatchers("/auth-service/**", "/api/auth/**", "/api/users/**").permitAll()

                        // Blog Service - GET 요청은 공개 (조회)
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/api/blog/**").permitAll()

                        // Shopping Service - 상품/카테고리 조회는 공개
                        .pathMatchers("/api/shopping/products", "/api/shopping/products/**").permitAll()
                        .pathMatchers("/api/shopping/categories", "/api/shopping/categories/**").permitAll()
                        // 쿠폰/타임딜 목록 조회도 공개
                        .pathMatchers("/api/shopping/coupons", "/api/shopping/time-deals").permitAll()
                        .pathMatchers("/api/shopping/time-deals/**").permitAll()
                        // Actuator Health Check (Status Page용)
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/api/*/actuator/**").permitAll()

                        // ========================================
                        // [관리자] ADMIN 권한 필요
                        // ========================================
                        .pathMatchers("/api/shopping/admin/**").hasRole("ADMIN")

                        // ========================================
                        // [비공개] 인증 필요
                        // ========================================
                        .anyExchange().authenticated()
                )
                // JWT 인증 필터 추가 (AUTHENTICATION 단계에서 실행)
                .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
                // 기본 인증 비활성화 (401 시 브라우저 프롬프트 방지)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }
}
