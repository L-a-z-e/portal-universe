package com.portal.universe.paymentservice.common.config;

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
                        // Internal API — 서비스 간 호출 (토큰 검증은 컨트롤러에서)
                        .requestMatchers("/internal/**").permitAll()

                        // Intent 조회
                        .requestMatchers(HttpMethod.GET, "/intents/**").hasAnyRole("USER", "ADMIN")

                        // 결제 확인
                        .requestMatchers(HttpMethod.POST, "/intents/*/confirm").hasAnyRole("USER", "ADMIN")

                        // 결제 조회/취소
                        .requestMatchers(HttpMethod.GET, "/payments/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/payments/*/cancel").hasAnyRole("USER", "ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(new GatewayAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
