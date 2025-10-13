package com.portal.universe.shoppingservice.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Actuator 엔드포인트에 대한 보안 필터 체인
     *
     * 우선순위: 0 (가장 먼저 평가)
     * 대상: /actuator/**
     * 보안: 인증 없이 접근 허용 (Prometheus, Health Check 등)
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
     * Shopping Service API에 대한 보안 필터 체인
     *
     * 우선순위: 1 (Actuator 다음 평가)
     * 대상: /actuator를 제외한 모든 요청
     *
     * 권한 전략:
     * - 공개: GET 요청 (상품, 카테고리 조회)
     * - 인증 필요: 주문, 장바구니 관리 (USER + ADMIN)
     * - ADMIN 전용: 상품, 카테고리 관리, 전체 주문 조회
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        //  공개 엔드포인트 (누구나 접근 가능)
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                        // 상품 조회
                        .requestMatchers(HttpMethod.GET, "/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()

                        // 카테고리 조회
                        .requestMatchers(HttpMethod.GET, "/categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()

                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        //  인증 필요 (USER + ADMIN)
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                        // 주문 생성 및 본인 주문 조회
                        .requestMatchers(HttpMethod.POST, "/orders").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/orders/my").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/orders/my/**").hasAnyRole("USER", "ADMIN")

                        // 장바구니 관리
                        .requestMatchers(HttpMethod.POST, "/cart/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/cart").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/cart/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/cart/**").hasAnyRole("USER", "ADMIN")

                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        //  ADMIN 전용
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                        // 상품 관리
                        .requestMatchers(HttpMethod.POST, "/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")

                        // 카테고리 관리
                        .requestMatchers(HttpMethod.POST, "/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/categories/**").hasRole("ADMIN")

                        // 전체 주문 조회 및 관리
                        .requestMatchers(HttpMethod.GET, "/orders").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/orders/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/orders/**").hasRole("ADMIN")

                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        //  기타 모든 요청은 인증 필요
                        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())  // ✅ common-library의 JwtAuthenticationConverter 자동 주입!
                )
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}