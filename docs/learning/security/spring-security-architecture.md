# Spring Security Architecture

Spring Security는 Spring 기반 애플리케이션의 **인증(Authentication)**과 **인가(Authorization)**를 담당하는 강력한 보안 프레임워크입니다.

## 1. 핵심 개념

### 1.1 Authentication vs Authorization

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                  Authentication vs Authorization                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────┐  ┌────────────────────────────────┐     │
│  │        Authentication          │  │        Authorization           │     │
│  │           (인증)               │  │           (인가)               │     │
│  ├────────────────────────────────┤  ├────────────────────────────────┤     │
│  │                                │  │                                │     │
│  │  "Who are you?"                │  │  "What can you do?"            │     │
│  │  "당신은 누구인가?"            │  │  "무엇을 할 수 있는가?"        │     │
│  │                                │  │                                │     │
│  │  - 사용자 신원 확인            │  │  - 권한/역할 확인              │     │
│  │  - 자격 증명 검증              │  │  - 리소스 접근 제어            │     │
│  │    (ID/PW, JWT, OAuth2)        │  │    (URL, Method, Data)         │     │
│  │                                │  │                                │     │
│  │  ┌──────────────────────┐     │  │  ┌──────────────────────┐     │     │
│  │  │  AuthenticationManager│     │  │  │  AccessDecisionManager│     │     │
│  │  │  AuthenticationProvider│    │  │  │  SecurityMetadataSource│    │     │
│  │  │  UserDetailsService  │     │  │  │  Voters               │     │     │
│  │  └──────────────────────┘     │  │  └──────────────────────┘     │     │
│  │                                │  │                                │     │
│  └────────────────────────────────┘  └────────────────────────────────┘     │
│                                                                              │
│                          순서: Authentication → Authorization                │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 주요 컴포넌트

| 컴포넌트 | 역할 | Portal Universe 구현 |
|----------|------|---------------------|
| **SecurityFilterChain** | 필터 체인 구성 | SecurityConfig.java |
| **AuthenticationManager** | 인증 처리 총괄 | Spring 기본 제공 |
| **AuthenticationProvider** | 실제 인증 수행 | DaoAuthenticationProvider |
| **UserDetailsService** | 사용자 정보 로드 | CustomUserDetailsService |
| **PasswordEncoder** | 비밀번호 암호화 | BCryptPasswordEncoder |
| **SecurityContext** | 인증 정보 저장소 | SecurityContextHolder |

---

## 2. Security Filter Chain

### 2.1 필터 체인 구조

Spring Security는 **Servlet Filter 기반**으로 동작합니다.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      Security Filter Chain                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   HTTP Request                                                               │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    DelegatingFilterProxy                             │    │
│  │              (서블릿 컨테이너 ↔ Spring 연결)                          │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    FilterChainProxy                                  │    │
│  │              (Security Filter 체인 관리)                              │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │ 1. SecurityContextPersistenceFilter                           │  │    │
│  │  │    - SecurityContext 로드/저장                                 │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │ 2. HeaderWriterFilter                                         │  │    │
│  │  │    - 보안 헤더 추가 (X-Frame-Options, X-XSS-Protection 등)    │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │ 3. CorsFilter                                                 │  │    │
│  │  │    - CORS 처리                                                 │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │ 4. CsrfFilter                                                 │  │    │
│  │  │    - CSRF 토큰 검증 (JWT 환경에서는 비활성화)                   │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │ 5. LogoutFilter                                               │  │    │
│  │  │    - 로그아웃 처리 (JWT 환경에서는 비활성화)                    │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │ ★ 6. JwtAuthenticationFilter (Custom)                         │  │    │
│  │  │    - JWT 토큰 검증 및 Authentication 설정                      │  │    │
│  │  │    - Portal Universe 커스텀 필터                               │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │ 7. UsernamePasswordAuthenticationFilter                       │  │    │
│  │  │    - 폼 로그인 처리 (JWT 환경에서는 비활성화)                   │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │ 8. OAuth2LoginAuthenticationFilter                            │  │    │
│  │  │    - OAuth2 소셜 로그인 처리                                   │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │ 9. SessionManagementFilter                                    │  │    │
│  │  │    - 세션 관리 (JWT: STATELESS)                                │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │ 10. ExceptionTranslationFilter                                │  │    │
│  │  │    - 보안 예외를 HTTP 응답으로 변환                            │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │ 11. FilterSecurityInterceptor (Authorization)                 │  │    │
│  │  │    - URL 기반 접근 제어                                        │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                         Controller                                   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Portal Universe SecurityConfig

```java
// SecurityConfig.java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. URL 기반 접근 제어 (Authorization)
            .authorizeHttpRequests(authorize -> authorize
                // 공개 API
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users/signup").permitAll()
                .requestMatchers("/actuator/**", "/oauth2/**").permitAll()

                // 관리자 전용
                .requestMatchers("/api/admin").hasRole("ADMIN")

                // 인증 필요
                .requestMatchers("/api/profile/**").authenticated()
                .anyRequest().authenticated()
            )

            // 2. 세션 정책 - STATELESS (JWT 기반)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 3. JWT 인증 필터 추가 (UsernamePasswordAuthenticationFilter 앞)
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class)

            // 4. 불필요한 기능 비활성화
            .formLogin(AbstractHttpConfigurer::disable)    // 폼 로그인 X
            .httpBasic(AbstractHttpConfigurer::disable)    // HTTP Basic X
            .logout(AbstractHttpConfigurer::disable)       // 기본 로그아웃 X
            .csrf(AbstractHttpConfigurer::disable);        // CSRF X (Stateless)

        return http.build();
    }
}
```

---

## 3. Authentication 상세

### 3.1 Authentication 객체

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      Authentication Object                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   public interface Authentication extends Principal {                        │
│                                                                              │
│       Object getPrincipal();      // 사용자 식별 정보 (userId, UserDetails) │
│       Object getCredentials();    // 자격 증명 (비밀번호, JWT 등)           │
│       Collection<? extends GrantedAuthority> getAuthorities();  // 권한     │
│       Object getDetails();        // 추가 정보 (IP, 세션 ID 등)             │
│       boolean isAuthenticated();  // 인증 여부                               │
│                                                                              │
│   }                                                                          │
│                                                                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Portal Universe에서의 사용:                                                │
│                                                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │  UsernamePasswordAuthenticationToken authentication =              │   │
│   │      new UsernamePasswordAuthenticationToken(                       │   │
│   │          userId,                    // Principal: User UUID         │   │
│   │          null,                      // Credentials: JWT에서는 불필요 │   │
│   │          authorities               // Authorities: [ROLE_USER]     │   │
│   │      );                                                             │   │
│   │                                                                     │   │
│   │  SecurityContextHolder.getContext().setAuthentication(authentication); │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 JwtAuthenticationFilter 상세

```java
// JwtAuthenticationFilter.java
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1. Authorization 헤더에서 토큰 추출
        String token = extractToken(request);

        // 2. 토큰이 있고 블랙리스트에 없으면 검증
        if (token != null && !tokenBlacklistService.isBlacklisted(token)) {
            try {
                // 3. JWT 서명 검증 및 Claims 추출
                Claims claims = tokenService.validateAccessToken(token);
                String userId = claims.getSubject();
                String roles = claims.get("roles", String.class);

                // 4. Authentication 객체 생성
                List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority(roles));

                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userId,       // Principal
                        null,         // Credentials
                        authorities   // Authorities
                    );

                // 5. SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                // 검증 실패 시 인증되지 않은 상태로 진행
                log.debug("JWT authentication failed: {}", e.getMessage());
            }
        }

        // 6. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") ||
               path.startsWith("/oauth2/") ||
               path.startsWith("/actuator/");
    }
}
```

### 3.3 UserDetailsService

```java
// CustomUserDetailsService.java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "User not found with email: " + email));

        // 2. 권한 변환
        List<SimpleGrantedAuthority> authorities =
            Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().getKey())
            );

        // 3. Spring Security UserDetails 객체 반환
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),     // username
                user.getPassword(),  // encoded password
                authorities          // authorities
        );
    }
}
```

---

## 4. Authorization 상세

### 4.1 URL 기반 접근 제어

```java
// SecurityConfig.java
.authorizeHttpRequests(authorize -> authorize
    // 정확한 경로 매칭
    .requestMatchers("/api/auth/**").permitAll()

    // HTTP 메서드 + 경로
    .requestMatchers(HttpMethod.POST, "/api/users/signup").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/blog/**").permitAll()

    // 역할 기반
    .requestMatchers("/api/admin/**").hasRole("ADMIN")          // ROLE_ADMIN
    .requestMatchers("/api/moderator/**").hasRole("MODERATOR")  // ROLE_MODERATOR

    // 권한 기반 (더 세밀한 제어)
    .requestMatchers("/api/blog/write").hasAuthority("BLOG_WRITE")

    // 인증만 필요
    .requestMatchers("/api/profile/**").authenticated()

    // 기본값 - 나머지는 모두 인증 필요
    .anyRequest().authenticated()
)
```

### 4.2 메서드 레벨 보안

```java
// 활성화
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig { }

// 사용 예시
@Service
public class PostService {

    @PreAuthorize("hasRole('ADMIN')")
    public void deletePost(Long postId) {
        // 관리자만 삭제 가능
    }

    @PreAuthorize("hasRole('USER') and #userId == authentication.principal")
    public Post updatePost(String userId, Long postId, PostRequest request) {
        // 본인 게시글만 수정 가능
    }

    @PostAuthorize("returnObject.authorId == authentication.principal")
    public Post getPost(Long postId) {
        // 본인 게시글만 조회 가능 (반환 후 검증)
    }
}
```

---

## 5. SecurityContext

### 5.1 SecurityContextHolder

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      SecurityContextHolder                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                    SecurityContextHolder                            │   │
│   │                    (ThreadLocal 기반)                               │   │
│   │                                                                     │   │
│   │   Thread 1                    Thread 2                              │   │
│   │   ┌──────────────────┐       ┌──────────────────┐                  │   │
│   │   │ SecurityContext  │       │ SecurityContext  │                  │   │
│   │   │ ┌──────────────┐ │       │ ┌──────────────┐ │                  │   │
│   │   │ │Authentication│ │       │ │Authentication│ │                  │   │
│   │   │ │  - User A    │ │       │ │  - User B    │ │                  │   │
│   │   │ │  - ROLE_USER │ │       │ │  - ROLE_ADMIN│ │                  │   │
│   │   │ └──────────────┘ │       │ └──────────────┘ │                  │   │
│   │   └──────────────────┘       └──────────────────┘                  │   │
│   │                                                                     │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│   전략 (Strategy):                                                           │
│   - MODE_THREADLOCAL (기본): 스레드별 독립                                   │
│   - MODE_INHERITABLETHREADLOCAL: 자식 스레드에 상속                          │
│   - MODE_GLOBAL: 전체 공유 (일반적으로 사용 안 함)                            │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 현재 사용자 정보 조회

```java
// 1. 직접 조회
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String userId = (String) auth.getPrincipal();

// 2. 컨트롤러에서 주입
@GetMapping("/me")
public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal String userId) {
    // userId는 JWT의 subject (UUID)
    return ResponseEntity.ok(userService.findByUuid(userId));
}

// 3. 커스텀 어노테이션 (권장)
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal
public @interface CurrentUser {
}

@GetMapping("/profile")
public ResponseEntity<?> getProfile(@CurrentUser String userId) {
    return ResponseEntity.ok(profileService.getProfile(userId));
}
```

---

## 6. OAuth2 통합

### 6.1 OAuth2 소셜 로그인 설정

```java
// SecurityConfig.java
// OAuth2 소셜 로그인: ClientRegistrationRepository가 있을 때만 활성화
if (isOAuth2Enabled()) {
    http.oauth2Login(oauth2 -> oauth2
        // 사용자 정보 로드 서비스
        .userInfoEndpoint(userInfo -> userInfo
            .userService(customOAuth2UserService))

        // 성공 핸들러 - JWT 발급 후 프론트엔드로 리다이렉트
        .successHandler(oAuth2AuthenticationSuccessHandler)

        // 실패 핸들러
        .failureHandler(oAuth2AuthenticationFailureHandler)
    );
}
```

### 6.2 OAuth2 성공 핸들러

```java
// OAuth2AuthenticationSuccessHandler.java
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        // 1. JWT 토큰 발급
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        // 2. Refresh Token Redis 저장
        refreshTokenService.saveRefreshToken(user.getUuid(), refreshToken);

        // 3. 프론트엔드로 리다이렉트 (Fragment로 토큰 전달)
        String targetUrl = UriComponentsBuilder
            .fromUriString(frontendBaseUrl + "/oauth2/callback")
            .fragment("access_token=" + accessToken +
                     "&refresh_token=" + refreshToken +
                     "&expires_in=900")
            .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
```

---

## 7. WebFlux Security (API Gateway)

API Gateway는 **WebFlux** 기반이므로 다른 설정을 사용합니다.

```java
// API Gateway SecurityConfig.java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(authorize -> authorize
                // 공개 경로
                .pathMatchers("/auth-service/**", "/api/auth/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/blog/**").permitAll()

                // 관리자 전용
                .pathMatchers("/api/shopping/admin/**").hasRole("ADMIN")

                // 나머지는 인증 필요
                .anyExchange().authenticated()
            )
            // JWT 필터 추가
            .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
    }
}
```

---

## 8. 참고 자료

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Spring Security Architecture](https://spring.io/guides/topicals/spring-security-architecture/)

## 9. 다음 단계

1. [RBAC Implementation](./rbac-implementation.md)
2. [API Gateway Security](./api-gateway-security.md)
3. [Portal Universe Auth Flow](./portal-universe-auth-flow.md)
