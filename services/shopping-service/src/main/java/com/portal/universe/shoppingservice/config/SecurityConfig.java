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

/**
 * 쇼핑 서비스의 웹 보안 설정을 담당하는 클래스입니다.
 * Actuator와 API 엔드포인트에 대한 별도의 보안 필터 체인을 구성하여 역할을 분리합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Actuator 엔드포인트(/actuator/**)에 대한 보안 필터 체인을 설정합니다.
     * 가장 높은 우선순위(@Order(0))를 부여하여 다른 API 필터 체인보다 먼저 평가되도록 합니다.
     *
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain Actuator용 보안 필터 체인
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
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
     * Actuator 필터 체인 다음 순서(@Order(1))로 평가됩니다.
     *
     * ### 권한 부여 전략
     * - **공개 (Permit All)**: `GET` 요청 (상품, 카테고리 조회).
     * - **인증된 사용자 (USER, ADMIN)**: 주문, 장바구니 관련 기능.
     * - **관리자 전용 (ADMIN)**: 상품, 카테고리 등 리소스 관리 기능.
     *
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain API용 보안 필터 체인
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // [공개] 누구나 접근 가능
                        .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories", "/categories/**").permitAll()

                        // [인증된 사용자] USER 또는 ADMIN 역할 필요
                        .requestMatchers(HttpMethod.POST, "/orders").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/orders/my", "/orders/my/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/cart/**").hasAnyRole("USER", "ADMIN")

                        // [관리자] ADMIN 역할 필요
                        .requestMatchers(HttpMethod.POST, "/products", "/categories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/**", "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/**", "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/orders", "/orders/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/orders/**").hasRole("ADMIN")

                        // 그 외 모든 요청은 인증만 되면 허용
                        .anyRequest().authenticated()
                )
                // 이 서비스를 OAuth2 리소스 서버로 설정하여 API Gateway로부터 전달받은 JWT를 검증합니다.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()) // common-library의 JwtAuthenticationConverter가 자동으로 JWT의 roles를 GrantedAuthority로 변환합니다.
                )
                // Stateless한 REST API이므로 CSRF 보호를 비활성화합니다.
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
