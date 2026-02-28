package com.portal.universe.shoppingsettlementservice.common.config;

import com.portal.universe.commonlibrary.security.filter.GatewayAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
                        .requestMatchers("/actuator/health/**", "/actuator/health", "/actuator/info").permitAll()
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
                        "/swagger-ui.html", "/swagger-ui/**",
                        "/api-docs", "/api-docs/**",
                        "/v3/api-docs", "/v3/api-docs/**"
                )
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 정산 조회/관리 (관리자 전용 — SUPER_ADMIN은 effectiveRoles에 포함)
                        .requestMatchers("/periods/**", "/sellers/**")
                            .hasAuthority("ROLE_SHOPPING_ADMIN")
                        // 배치 실행 (관리자 전용)
                        .requestMatchers("/batch/**")
                            .hasAuthority("ROLE_SHOPPING_ADMIN")
                        // 시드 데이터 (관리자 전용)
                        .requestMatchers("/seed/**")
                            .hasAuthority("ROLE_SHOPPING_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new GatewayAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
