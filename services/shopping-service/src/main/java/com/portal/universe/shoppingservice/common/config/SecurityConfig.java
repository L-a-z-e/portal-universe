package com.portal.universe.shoppingservice.common.config;

import com.portal.universe.commonlibrary.security.filter.GatewayAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 쇼핑 서비스의 웹 보안 설정을 담당하는 클래스입니다.
 *
 * API Gateway에서 JWT를 검증하고 X-User-Id, X-User-Roles 헤더를 전달합니다.
 * 이 서비스는 해당 헤더를 읽어 SecurityContext를 설정합니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Actuator 엔드포인트(/actuator/**)에 대한 보안 필터 체인을 설정합니다.
     * - /actuator/health, /actuator/info: 공개 (상태 확인용)
     * - /actuator/prometheus, /actuator/metrics: 내부망 전용 (Prometheus 스크래핑)
     * - 나머지: 차단
     */
    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()  // 공개
                        .requestMatchers("/actuator/prometheus", "/actuator/metrics/**").permitAll()  // 내부망 전용 (Gateway에서 차단)
                        .anyRequest().denyAll())  // 나머지는 차단
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Swagger UI 및 OpenAPI 문서 엔드포인트에 대한 보안 필터 체인을 설정합니다.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/api-docs",
                        "/api-docs/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**"
                )
                .authorizeHttpRequests(requests -> requests
                        .anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * 쇼핑 서비스의 API 엔드포인트에 대한 보안 필터 체인을 설정합니다.
     *
     * ### 권한 부여 전략
     * - **공개 (Permit All)**: `GET` 요청 (상품, 카테고리 조회).
     * - **인증된 사용자 (USER, ADMIN)**: 장바구니, 주문, 결제 관련 기능.
     * - **관리자 전용 (ADMIN)**: 상품, 재고, 배송 상태 관리 기능.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // ========================================
                        // [공개] 누구나 접근 가능
                        // Gateway StripPrefix=2 적용 후 경로 (/api/shopping 제거됨)
                        // ========================================
                        // Actuator 엔드포인트 (EndpointRequest와 별개로 경로 기반 허용)
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories", "/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/search/**").permitAll()

                        // ========================================
                        // [인증된 사용자] USER 또는 ADMIN 역할 필요
                        // 주의: 구체적 경로를 와일드카드보다 먼저 선언
                        // ========================================

                        // 쿠폰 - 인증 필요 (my, issue는 와일드카드 매칭 전에 선언)
                        .requestMatchers(HttpMethod.GET, "/coupons/my", "/coupons/my/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/coupons/*/issue").hasAnyRole("USER", "ADMIN")
                        // 쿠폰 - 공개 조회
                        .requestMatchers(HttpMethod.GET, "/coupons", "/coupons/*").permitAll()

                        // 타임딜 - 인증 필요 (my, purchase는 와일드카드 매칭 전에 선언)
                        .requestMatchers(HttpMethod.GET, "/time-deals/my/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/time-deals/purchase").hasAnyRole("USER", "ADMIN")
                        // 타임딜 - 공개 조회
                        .requestMatchers(HttpMethod.GET, "/time-deals", "/time-deals/*").permitAll()

                        // 대기열
                        .requestMatchers("/queue/**").hasAnyRole("USER", "ADMIN")

                        // 장바구니
                        .requestMatchers("/cart/**").hasAnyRole("USER", "ADMIN")

                        // 주문 (사용자 본인 주문)
                        .requestMatchers(HttpMethod.POST, "/orders").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/orders", "/orders/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/orders/*/cancel").hasAnyRole("USER", "ADMIN")

                        // 결제
                        .requestMatchers(HttpMethod.POST, "/payments").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/payments/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/payments/*/cancel").hasAnyRole("USER", "ADMIN")

                        // 배송 조회
                        .requestMatchers(HttpMethod.GET, "/deliveries/**").hasAnyRole("USER", "ADMIN")

                        // 재고 조회 (상품 목록/상세에서 비인증 사용자도 확인 필요)
                        .requestMatchers(HttpMethod.POST, "/inventory/batch").permitAll()
                        .requestMatchers(HttpMethod.GET, "/inventory/stream").permitAll()
                        .requestMatchers(HttpMethod.GET, "/inventory/*/movements").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/inventory/*").permitAll()

                        // ========================================
                        // [관리자] SHOPPING_ADMIN / SUPER_ADMIN 역할 필요
                        // ========================================
                        // Admin 전용 경로
                        .requestMatchers("/admin/**")
                            .hasAnyAuthority("ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        // 상품 관리 (Seller도 가능)
                        .requestMatchers(HttpMethod.POST, "/products")
                            .hasAnyAuthority("ROLE_SELLER", "ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/**")
                            .hasAnyAuthority("ROLE_SELLER", "ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/**")
                            .hasAnyAuthority("ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        // 재고 관리
                        .requestMatchers(HttpMethod.POST, "/inventory/**")
                            .hasAnyAuthority("ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/inventory/**")
                            .hasAnyAuthority("ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        // 배송 상태 관리
                        .requestMatchers(HttpMethod.PUT, "/deliveries/**")
                            .hasAnyAuthority("ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        // 결제 환불 (관리자)
                        .requestMatchers(HttpMethod.POST, "/payments/*/refund")
                            .hasAnyAuthority("ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        // 그 외 모든 요청은 인증만 되면 허용
                        .anyRequest().authenticated()
                )
                // Gateway에서 전달한 헤더로 인증 정보 설정
                .addFilterBefore(new GatewayAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // Stateless REST API
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
