# Spring Security Config (Filter Chain)

## 개요

Portal Universe auth-service는 Spring Security를 기반으로 인증 및 인가를 처리합니다. JWT 기반 Stateless 인증을 사용하며, OAuth2 소셜 로그인을 선택적으로 지원합니다. 이 문서에서는 SecurityFilterChain의 구성과 각 필터의 역할을 설명합니다.

## Security Filter Chain 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP Request                              │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                 ForwardedHeaderFilter                        │
│         (X-Forwarded-* 헤더 처리 - API Gateway용)            │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│               SecurityFilterChain                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. SecurityContextPersistenceFilter                         │
│     └── SecurityContext 관리                                 │
│                                                              │
│  2. JwtAuthenticationFilter (Custom)                         │
│     └── JWT 토큰 검증 및 인증 정보 설정                       │
│                                                              │
│  3. LogoutFilter (비활성화)                                   │
│     └── JWT 기반이므로 기본 로그아웃 사용 안 함               │
│                                                              │
│  4. OAuth2AuthorizationRequestRedirectFilter                 │
│     └── 소셜 로그인 시작점 (선택적)                           │
│                                                              │
│  5. OAuth2LoginAuthenticationFilter                          │
│     └── 소셜 로그인 콜백 처리 (선택적)                        │
│                                                              │
│  6. UsernamePasswordAuthenticationFilter (비활성화)          │
│     └── Form 로그인 사용 안 함                                │
│                                                              │
│  7. AuthorizationFilter                                      │
│     └── URL 기반 접근 권한 검사                              │
│                                                              │
│  8. ExceptionTranslationFilter                               │
│     └── 인증/인가 예외 처리                                   │
│                                                              │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                      Controller                              │
└─────────────────────────────────────────────────────────────┘
```

## SecurityConfig 전체 코드

```java
package com.portal.universe.authservice.config;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    // OAuth2 관련 빈들은 선택적 주입 (설정이 없으면 null)
    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;
    @Autowired(required = false)
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @Autowired(required = false)
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    // JWT 인증 필터는 필수
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * X-Forwarded-* 헤더를 처리하는 필터를 등록합니다.
     * API Gateway를 통해 들어온 요청의 원본 Host, Proto, Port 정보를 보존합니다.
     */
    @Bean
    public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        FilterRegistrationBean<ForwardedHeaderFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new ForwardedHeaderFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    /**
     * 애플리케이션의 모든 엔드포인트에 대한 보안을 설정합니다.
     * Direct JWT 인증 방식을 사용하며, 세션은 사용하지 않습니다(STATELESS).
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. 요청 권한 설정
            .authorizeHttpRequests(authorize -> authorize
                // 인증 API는 누구나 접근 가능
                .requestMatchers("/api/auth/**").permitAll()
                // 회원가입 API
                .requestMatchers(HttpMethod.POST, "/api/users/signup").permitAll()
                // 정적 리소스
                .requestMatchers("/css/**", "/js/**", "/images/**",
                                "/favicon.ico", "/webjars/**").permitAll()
                // 공개 엔드포인트
                .requestMatchers("/.well-known/**", "/login", "/logout",
                                "/default-ui.css", "/actuator/**", "/ping",
                                "/oauth2/**", "/login/oauth2/**").permitAll()
                // ADMIN 전용
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 인증 필요
                .requestMatchers("/api/profile/**").authenticated()
                // 기타 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )

            // 2. 세션 관리 - Stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 3. JWT 인증 필터 추가
            .addFilterBefore(jwtAuthenticationFilter,
                            UsernamePasswordAuthenticationFilter.class)

            // 4. Form 로그인 비활성화
            .formLogin(AbstractHttpConfigurer::disable)

            // 5. HTTP Basic 비활성화
            .httpBasic(AbstractHttpConfigurer::disable)

            // 6. 기본 로그아웃 비활성화 (JWT 기반 처리)
            .logout(AbstractHttpConfigurer::disable)

            // 7. CSRF 비활성화 (Stateless 환경)
            .csrf(AbstractHttpConfigurer::disable)

            // 8. 헤더 설정
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

            // 9. CORS 설정 (별도 CorsConfig 사용 시)
            .cors(AbstractHttpConfigurer::disable);

        // 10. OAuth2 소셜 로그인 (선택적)
        if (isOAuth2Enabled()) {
            log.info("OAuth2 소셜 로그인 활성화됨");
            http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService))
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            );
        } else {
            log.warn("OAuth2 소셜 로그인 비활성화됨");
        }

        return http.build();
    }

    private boolean isOAuth2Enabled() {
        return clientRegistrationRepository != null
            && customOAuth2UserService != null
            && oAuth2AuthenticationSuccessHandler != null
            && oAuth2AuthenticationFailureHandler != null;
    }

    /**
     * AuthenticationManager Bean 등록
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * BCrypt PasswordEncoder Bean 등록
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

## JwtAuthenticationFilter

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        // 1. Authorization 헤더에서 토큰 추출
        String token = extractToken(request);

        // 2. 토큰이 있고 블랙리스트에 없는 경우에만 검증
        if (token != null && !tokenBlacklistService.isBlacklisted(token)) {
            try {
                // 3. JWT 토큰 검증
                Claims claims = tokenService.validateAccessToken(token);
                String userId = claims.getSubject();
                String roles = claims.get("roles", String.class);

                // 4. Authentication 객체 생성
                List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority(roles));

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

                // 5. SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT authentication successful for user: {}", userId);
            } catch (Exception e) {
                log.debug("JWT authentication failed: {}", e.getMessage());
                // 인증 실패 시 SecurityContext에 설정 안 함
                // Spring Security가 401 응답 처리
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    /**
     * 공개 엔드포인트는 필터 건너뜀 (성능 최적화)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") ||
               path.startsWith("/oauth2/") ||
               path.startsWith("/login/oauth2/") ||
               path.startsWith("/.well-known/") ||
               path.startsWith("/actuator/") ||
               path.equals("/ping") ||
               path.equals("/login") ||
               path.equals("/logout") ||
               path.startsWith("/api/users/signup");
    }
}
```

## 권한 설정 상세

### URL 기반 권한

```java
.authorizeHttpRequests(authorize -> authorize
    // 순서가 중요: 구체적인 것부터 일반적인 것 순서로

    // 1. 완전 공개 (인증 불필요)
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/users/signup").permitAll()

    // 2. 정적 리소스
    .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

    // 3. 특정 역할 필요
    .requestMatchers("/api/admin/**").hasRole("ADMIN")  // ROLE_ADMIN

    // 4. 인증만 필요 (역할 무관)
    .requestMatchers("/api/profile/**").authenticated()

    // 5. 기본: 모든 요청은 인증 필요
    .anyRequest().authenticated()
)
```

### 메서드 보안 (선택적)

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {}

// 사용 예시
@Service
public class AdminService {

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long userId) { ... }

    @PreAuthorize("#userId == authentication.principal or hasRole('ADMIN')")
    public UserDto getUser(Long userId) { ... }
}
```

## Stateless 세션 관리

```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

| 정책 | 설명 | 사용 시나리오 |
|------|------|-------------|
| `STATELESS` | 세션 생성 안 함 | JWT 인증 (현재) |
| `IF_REQUIRED` | 필요 시 생성 | Form 로그인 |
| `NEVER` | 생성 안 하지만 있으면 사용 | 하이브리드 |
| `ALWAYS` | 항상 세션 생성 | 전통적 웹앱 |

## 비활성화된 기능들

```java
// Form 로그인 - JWT 사용으로 불필요
.formLogin(AbstractHttpConfigurer::disable)

// HTTP Basic 인증 - API 서비스에 부적합
.httpBasic(AbstractHttpConfigurer::disable)

// 기본 로그아웃 - JWT 기반 별도 처리
.logout(AbstractHttpConfigurer::disable)

// CSRF - Stateless 환경에서 불필요
// (쿠키 기반 인증 사용 시에는 활성화 필요)
.csrf(AbstractHttpConfigurer::disable)
```

## CORS 설정 (별도 파일)

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:30000",
            "https://portal-universe.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}

// SecurityConfig에서 연결
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

## 예외 처리

### 인증/인가 예외 핸들러

```java
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
            {"success":false,"error":{"code":"UNAUTHORIZED","message":"인증이 필요합니다"}}
        """);
    }
}

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                      HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
            {"success":false,"error":{"code":"FORBIDDEN","message":"접근 권한이 없습니다"}}
        """);
    }
}

// SecurityConfig에 등록
.exceptionHandling(ex -> ex
    .authenticationEntryPoint(customAuthenticationEntryPoint)
    .accessDeniedHandler(customAccessDeniedHandler))
```

## 디버깅 팁

### Security 필터 로그 활성화

```yaml
# application.yml
logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.security.web.FilterChainProxy: DEBUG
```

### 필터 체인 확인

```java
@Autowired
private FilterChainProxy filterChainProxy;

@PostConstruct
public void printFilterChain() {
    filterChainProxy.getFilterChains().forEach(chain -> {
        chain.getFilters().forEach(filter -> {
            log.info("Filter: {}", filter.getClass().getSimpleName());
        });
    });
}
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/config/SecurityConfig.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/security/JwtAuthenticationFilter.java`

## 참고 자료

- [Spring Security Architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html)
- [Spring Security Filter Chain](https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-filterchainproxy)
