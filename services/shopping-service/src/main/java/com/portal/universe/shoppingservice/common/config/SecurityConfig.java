package com.portal.universe.shoppingservice.common.config;

import com.portal.universe.commonlibrary.security.filter.GatewayAuthenticationFilter;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
public class SecurityConfig {

    /**
     * Actuator 엔드포인트(/actuator/**)에 대한 보안 필터 체인을 설정합니다.
     */
    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
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
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // ========================================
                        // [공개] 누구나 접근 가능
                        // ========================================
                        .requestMatchers(HttpMethod.GET, "/api/shopping/products", "/api/shopping/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/shopping/categories", "/api/shopping/categories/**").permitAll()

                        // ========================================
                        // [인증된 사용자] USER 또는 ADMIN 역할 필요
                        // ========================================
                        // 장바구니
                        .requestMatchers("/api/shopping/cart/**").hasAnyRole("USER", "ADMIN")

                        // 주문 (사용자 본인 주문)
                        .requestMatchers(HttpMethod.POST, "/api/shopping/orders").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/shopping/orders", "/api/shopping/orders/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/shopping/orders/*/cancel").hasAnyRole("USER", "ADMIN")

                        // 결제
                        .requestMatchers(HttpMethod.POST, "/api/shopping/payments").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/shopping/payments/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/shopping/payments/*/cancel").hasAnyRole("USER", "ADMIN")

                        // 배송 조회
                        .requestMatchers(HttpMethod.GET, "/api/shopping/deliveries/**").hasAnyRole("USER", "ADMIN")

                        // 재고 조회
                        .requestMatchers(HttpMethod.GET, "/api/shopping/inventory/**").hasAnyRole("USER", "ADMIN")

                        // ========================================
                        // [관리자] ADMIN 역할 필요
                        // ========================================
                        // 상품 관리
                        .requestMatchers(HttpMethod.POST, "/api/shopping/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/shopping/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/shopping/products/**").hasRole("ADMIN")

                        // 재고 관리
                        .requestMatchers(HttpMethod.POST, "/api/shopping/inventory/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/shopping/inventory/**").hasRole("ADMIN")

                        // 배송 상태 관리
                        .requestMatchers(HttpMethod.PUT, "/api/shopping/deliveries/**").hasRole("ADMIN")

                        // 결제 환불 (관리자)
                        .requestMatchers(HttpMethod.POST, "/api/shopping/payments/*/refund").hasRole("ADMIN")

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
