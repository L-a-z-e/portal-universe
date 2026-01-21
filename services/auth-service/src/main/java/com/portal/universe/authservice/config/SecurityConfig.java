package com.portal.universe.authservice.config;

import com.portal.universe.authservice.oauth2.CustomOAuth2UserService;
import com.portal.universe.authservice.oauth2.OAuth2AuthenticationFailureHandler;
import com.portal.universe.authservice.oauth2.OAuth2AuthenticationSuccessHandler;
import com.portal.universe.authservice.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * 애플리케이션의 전반적인 웹 보안 설정을 담당하는 클래스입니다.
 * Spring Security의 필터 체인을 구성하여 요청에 대한 인증 및 인가를 처리합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * X-Forwarded-* 헤더를 처리하는 필터를 등록합니다.
     * API Gateway를 통해 들어온 요청의 원본 Host, Proto, Port 정보를 보존합니다.
     *
     * @return ForwardedHeaderFilter
     */

    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        FilterRegistrationBean<ForwardedHeaderFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ForwardedHeaderFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    /**
     * 애플리케이션의 모든 엔드포인트(API, 웹 페이지 등)에 대한 보안을 설정합니다.
     * Direct JWT 인증 방식을 사용하며, 세션은 사용하지 않습니다(STATELESS).
     *
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain 일반용 보안 필터 체인
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // 인증 API는 누구나 접근 가능해야 합니다.
                        .requestMatchers("/api/auth/**").permitAll()
                        // 회원가입 API는 누구나 접근 가능해야 합니다.
                        .requestMatchers(HttpMethod.POST, "/api/users/signup").permitAll()
                        // OIDC Discovery, 로그인/로그아웃, Actuator 등 인증이 필요 없는 경로들을 지정합니다.
                        .requestMatchers(
                                "/css/**",           // CSS 파일
                                "/js/**",            // JavaScript 파일
                                "/images/**",        // 이미지 파일
                                "/favicon.ico",   // 파비콘
                                "/webjars/**"        // WebJars 라이브러리
                        ).permitAll()
                        .requestMatchers(
                                "/.well-known/**",
                                "/login",
                                "/logout",
                                "/default-ui.css",   // Spring Security 기본 로그인 페이지 CSS
                                "/actuator/**",
                                "/ping",
                                "/oauth2/**",        // OAuth2 소셜 로그인 엔드포인트
                                "/login/oauth2/**"   // OAuth2 콜백 엔드포인트
                        ).permitAll()
                        // /api/admin 경로는 ADMIN 역할을 가진 사용자만 접근 가능합니다.
                        .requestMatchers("/api/admin").hasRole("ADMIN")
                        // /api/profile 경로는 인증된 사용자만 접근 가능합니다.
                        .requestMatchers("/api/profile/**").authenticated()
                        // 위에서 지정한 경로 외의 모든 요청은 인증이 필요합니다.
                        .anyRequest().authenticated()
                )
                // STATELESS 세션 정책 - 세션을 사용하지 않음
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // JWT 인증 필터 추가 (UsernamePasswordAuthenticationFilter 앞에 위치)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // FormLogin 비활성화 (JWT 기반 인증 사용)
                .formLogin(AbstractHttpConfigurer::disable)
                // OAuth2 소셜 로그인 유지 (JWT 토큰 발급 방식으로 전환)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
                // HttpBasic 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                // 로그아웃은 JWT 기반으로 처리하므로 기본 로그아웃 비활성화
                .logout(AbstractHttpConfigurer::disable)
                // CSRF 보호 비활성화 (Stateless 환경)
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
                )
                .cors(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * Spring Security의 인증을 총괄하는 AuthenticationManager를 Bean으로 등록합니다.
     * @param authenticationConfiguration 인증 구성 객체
     * @return AuthenticationManager
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 비밀번호를 안전하게 암호화하기 위한 PasswordEncoder를 Bean으로 등록합니다.
     * BCrypt 알고리즘을 사용합니다.
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}