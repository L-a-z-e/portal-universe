package com.portal.universe.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * 애플리케이션의 전반적인 웹 보안 설정을 담당하는 클래스입니다.
 * Spring Security의 필터 체인을 구성하여 요청에 대한 인증 및 인가를 처리합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * OAuth2 인증 서버 관련 엔드포인트(/oauth2/..., /connect/..., /.well-known/...)에 대한 보안을 설정합니다.
     * 이 필터 체인은 가장 높은 우선순위(@Order(1))를 가지므로, 다른 필터 체인보다 먼저 실행됩니다.
     *
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain 인증 서버용 보안 필터 체인
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                // 인증 서버 엔드포인트에만 이 필터 체인을 적용합니다.
                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .authorizeHttpRequests(authorize -> {
                    // OIDC Provider Configuration, JWK Set 등 공개 엔드포인트는 모두 허용합니다.
                    authorize.requestMatchers(
                            "/.well-known/**",
                            "/connect/logout"
                    ).permitAll();
                    // 그 외 모든 인증 서버 엔드포인트는 인증이 필요합니다.
                    authorize.anyRequest().authenticated();
                })
                // OIDC(OpenID Connect) 1.0 기능을 활성화합니다.
                .with(authorizationServerConfigurer, configurer -> {
                    configurer.oidc(Customizer.withDefaults());
                })
                // 인증 서버 엔드포인트에 대해서는 CSRF 보호를 비활성화합니다.
                .csrf(csrf -> csrf.ignoringRequestMatchers(authorizationServerConfigurer.getEndpointsMatcher()))
                .exceptionHandling(exceptions -> exceptions
                        // 브라우저(HTML) 요청 시에는 로그인 페이지로 리다이렉트합니다.
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                        // API(JSON) 요청 시에는 401 Unauthorized 응답을 반환합니다.
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON)
                        )
                )
                // 리소스 서버 설정을 추가하여 API 요청 시 JWT 토큰을 검증할 수 있도록 합니다.
                .oauth2ResourceServer(server -> server.jwt(Customizer.withDefaults()))
                .cors(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * 애플리케이션의 일반적인 엔드포인트(API, 웹 페이지 등)에 대한 보안을 설정합니다.
     * 인증 서버 필터 체인 다음 순서(@Order(2))로 실행됩니다.
     *
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain 일반용 보안 필터 체인
     * @throws Exception 설정 과정에서 발생할 수 있는 예외
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // 회원가입 API는 누구나 접근 가능해야 합니다.
                        .requestMatchers(HttpMethod.POST, "/api/users/signup").permitAll()
                        // OIDC Discovery, 로그인/로그아웃, Actuator 등 인증이 필요 없는 경로들을 지정합니다.
                        .requestMatchers(
                                "/.well-known/**",
                                "/login",
                                "/logout",
                                "/favicon.ico",
                                "/actuator/**",
                                "/ping",
                                "/force-error"
                        ).permitAll()
                        // /api/admin 경로는 ADMIN 역할을 가진 사용자만 접근 가능합니다.
                        .requestMatchers("/api/admin").hasRole("ADMIN")
                        // 위에서 지정한 경로 외의 모든 요청은 인증이 필요합니다.
                        .anyRequest().authenticated()
                )
                // 기본 폼 로그인 페이지를 사용합니다.
                .formLogin(Customizer.withDefaults())
                // 로그아웃 설정을 정의합니다.
                .logout(logout -> logout
                        .logoutSuccessHandler(new SimpleUrlLogoutSuccessHandler()) // 로그아웃 성공 시 기본 핸들러 사용
                        .invalidateHttpSession(true) // 세션 무효화
                        .clearAuthentication(true) // 인증 정보 삭제
                        .deleteCookies("JSESSIONID") // JSESSIONID 쿠키 삭제
                )
                // 회원가입 API에 대해서는 CSRF 보호를 비활성화합니다.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/users/signup"))
                .oauth2ResourceServer(server -> server.jwt(Customizer.withDefaults()))
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
