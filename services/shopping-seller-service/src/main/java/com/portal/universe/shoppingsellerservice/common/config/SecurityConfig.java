package com.portal.universe.shoppingsellerservice.common.config;

import com.portal.universe.commonlibrary.security.filter.GatewayAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/prometheus", "/actuator/metrics/**").permitAll()
                        .anyRequest().denyAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

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

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()

                        // Internal API (서비스 간 통신)
                        .requestMatchers("/internal/**").permitAll()

                        // 상품 조회 (공개)
                        .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()

                        // 판매자 관리 (SELLER, SHOPPING_ADMIN, SUPER_ADMIN)
                        .requestMatchers("/sellers/**")
                            .hasAnyAuthority("ROLE_SHOPPING_SELLER", "ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        // 상품 관리 (SELLER 본인 상품만)
                        .requestMatchers(HttpMethod.POST, "/products")
                            .hasAnyAuthority("ROLE_SHOPPING_SELLER", "ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/**")
                            .hasAnyAuthority("ROLE_SHOPPING_SELLER", "ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/**")
                            .hasAnyAuthority("ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        // 재고 관리
                        .requestMatchers("/inventory/**")
                            .hasAnyAuthority("ROLE_SHOPPING_SELLER", "ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        // 주문 관리 (판매자 본인 주문)
                        .requestMatchers("/orders/**")
                            .hasAnyAuthority("ROLE_SHOPPING_SELLER", "ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        // 배송 관리
                        .requestMatchers("/deliveries/**")
                            .hasAnyAuthority("ROLE_SHOPPING_SELLER", "ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        // 쿠폰 관리
                        .requestMatchers("/coupons/**")
                            .hasAnyAuthority("ROLE_SHOPPING_SELLER", "ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        // 타임딜 관리
                        .requestMatchers("/time-deals/**")
                            .hasAnyAuthority("ROLE_SHOPPING_SELLER", "ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        // 대기열 관리
                        .requestMatchers("/queue/**")
                            .hasAnyAuthority("ROLE_SHOPPING_SELLER", "ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        // 대시보드
                        .requestMatchers("/dashboard/**")
                            .hasAnyAuthority("ROLE_SHOPPING_SELLER", "ROLE_SHOPPING_ADMIN", "ROLE_SUPER_ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(new GatewayAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
