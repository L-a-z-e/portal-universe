# API Gateway Security

API Gateway는 마이크로서비스 아키텍처의 **단일 진입점(Single Entry Point)**으로서, 중앙집중식 보안 처리를 담당합니다.

## 1. API Gateway 보안 역할

### 1.1 Gateway 보안 책임

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                   API Gateway Security Responsibilities                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Client                  API Gateway                    Microservices       │
│     │                         │                              │               │
│     │                         │                              │               │
│     │   ┌─────────────────────┴─────────────────────┐       │               │
│     │   │                                           │       │               │
│     │   │  1. SSL/TLS Termination                   │       │               │
│     │   │     - HTTPS 처리                          │       │               │
│     │   │     - 인증서 관리                         │       │               │
│     │   │                                           │       │               │
│     │   │  2. Authentication (인증)                 │       │               │
│     │   │     - JWT 토큰 검증                       │       │               │
│     │   │     - 토큰 유효성 확인                    │       │               │
│     │   │                                           │       │               │
│     │   │  3. Authorization (인가)                  │       │               │
│     │   │     - 경로별 접근 제어                    │       │               │
│     │   │     - 역할 기반 권한 검사                 │       │               │
│     │   │                                           │       │               │
│     │   │  4. Rate Limiting                         │       │               │
│     │   │     - 요청 횟수 제한                      │       │               │
│     │   │     - DDoS 방어                           │       │               │
│     │   │                                           │       │               │
│     │   │  5. CORS                                  │       │               │
│     │   │     - Cross-Origin 요청 제어              │       │               │
│     │   │                                           │       │               │
│     │   │  6. Request Validation                    │       │               │
│     │   │     - 악성 요청 필터링                    │       │               │
│     │   │     - 헤더/페이로드 검증                  │       │               │
│     │   │                                           │       │               │
│     │   │  7. Logging & Monitoring                  │       │               │
│     │   │     - 접근 로그                           │       │               │
│     │   │     - 보안 이벤트 기록                    │       │               │
│     │   │                                           │       │               │
│     │   └─────────────────────┬─────────────────────┘       │               │
│     │                         │                              │               │
│     │                         │  X-User-Id: {uuid}          │               │
│     │                         │  X-User-Roles: ROLE_USER    │               │
│     │                         ├─────────────────────────────▶│              │
│     │                         │                              │               │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Portal Universe Gateway 아키텍처

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    Portal Universe Gateway Architecture                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                              Internet                                        │
│                                  │                                           │
│                                  ▼                                           │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                    API Gateway (:8080)                               │   │
│   │                 Spring Cloud Gateway (WebFlux)                       │   │
│   │                                                                     │   │
│   │  ┌─────────────────────────────────────────────────────────────┐   │   │
│   │  │                  Security Filter Chain                       │   │   │
│   │  │  ┌─────────────┬────────────────┬───────────────────────┐   │   │   │
│   │  │  │ CorsFilter  │ JwtAuthFilter  │ AuthorizationFilter   │   │   │   │
│   │  │  └─────────────┴────────────────┴───────────────────────┘   │   │   │
│   │  └─────────────────────────────────────────────────────────────┘   │   │
│   │                              │                                      │   │
│   │                        Route Matching                               │   │
│   │                              │                                      │   │
│   └──────────────────────────────┼──────────────────────────────────────┘   │
│                                  │                                           │
│          ┌───────────────────────┼───────────────────────┐                  │
│          │                       │                       │                  │
│          ▼                       ▼                       ▼                  │
│   ┌──────────────┐       ┌──────────────┐       ┌──────────────┐           │
│   │ auth-service │       │ blog-service │       │shopping-serv │           │
│   │   (:8081)    │       │   (:8082)    │       │   (:8083)    │           │
│   └──────────────┘       └──────────────┘       └──────────────┘           │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. JWT 검증 (Gateway Level)

### 2.1 WebFlux JwtAuthenticationFilter

```java
// JwtAuthenticationFilter.java (API Gateway)
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtConfig jwtConfig;

    /**
     * HMAC Secret Key 생성
     * Auth Service와 동일한 키 사용
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1. 공개 경로는 JWT 검증 생략
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 2. Authorization 헤더에서 토큰 추출
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // 토큰 없음 - SecurityConfig의 접근 제어에 위임
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // 3. JWT 검증
            Claims claims = validateToken(token);
            String userId = claims.getSubject();
            String roles = claims.get("roles", String.class);

            log.debug("JWT validated for user: {}, roles: {}", userId, roles);

            // 4. SecurityContext 설정
            List<SimpleGrantedAuthority> authorities = roles != null
                    ? List.of(new SimpleGrantedAuthority(roles))
                    : Collections.emptyList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // 5. 하위 서비스로 전달할 헤더 추가
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Roles", roles != null ? roles : "")
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();

            // 6. SecurityContext와 함께 다음 필터로
            return chain.filter(mutatedExchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return handleUnauthorized(exchange, "Token expired");
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return handleUnauthorized(exchange, "Invalid token");
        }
    }

    /**
     * JWT 토큰 검증
     */
    private Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 공개 경로 확인
     */
    private boolean isPublicPath(String path) {
        return path.startsWith("/auth-service/") ||
               path.startsWith("/api/auth/") ||
               path.startsWith("/api/users/") ||
               path.startsWith("/actuator/") ||
               path.equals("/api/shopping/products") ||
               path.startsWith("/api/shopping/products/") ||
               path.equals("/api/shopping/categories") ||
               path.startsWith("/api/shopping/categories/");
    }

    /**
     * 401 Unauthorized 응답
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }
}
```

### 2.2 SecurityConfig (WebFlux)

```java
// SecurityConfig.java (API Gateway)
@Configuration
@EnableWebFluxSecurity
@Profile("!test")
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtConfig jwtConfig;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtConfig);
    }

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(authorize -> authorize
                // ========================================
                // [공개] 인증 없이 접근 가능
                // ========================================
                .pathMatchers("/auth-service/**", "/api/auth/**", "/api/users/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/blog/**").permitAll()
                .pathMatchers("/api/shopping/products", "/api/shopping/products/**").permitAll()
                .pathMatchers("/api/shopping/categories", "/api/shopping/categories/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()

                // ========================================
                // [관리자] ADMIN 권한 필요
                // ========================================
                .pathMatchers("/api/shopping/admin/**").hasRole("ADMIN")

                // ========================================
                // [인증 필요] 나머지 모든 요청
                // ========================================
                .anyExchange().authenticated()
            )
            // JWT 필터 추가
            .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            // 기본 인증 비활성화
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
    }
}
```

---

## 3. CORS 설정

### 3.1 Gateway Level CORS

```java
// SecurityConfig.java
@Bean
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public CorsWebFilter corsWebFilter() {
    CorsConfiguration configuration = new CorsConfiguration();

    // 허용할 Origin
    configuration.setAllowedOrigins(List.of(
            "http://localhost:30000",     // portal-shell
            "http://localhost:8080",      // 개발 서버
            "https://portal-universe:30000"
    ));

    // 허용할 HTTP 메서드
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS"
    ));

    // 허용할 헤더
    configuration.setAllowedHeaders(List.of("*"));

    // 자격 증명 허용 (Cookie, Authorization 헤더)
    configuration.setAllowCredentials(true);

    // Preflight 요청 캐시 시간
    configuration.setMaxAge(3600L);  // 1시간

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return new CorsWebFilter(source);
}
```

### 3.2 CORS Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          CORS Preflight Flow                                  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Browser                          API Gateway                Backend        │
│     │                                   │                        │           │
│     │                                   │                        │           │
│  1. │──OPTIONS /api/blog/posts─────────▶│                        │           │
│     │  Origin: http://localhost:30000   │                        │           │
│     │  Access-Control-Request-Method: POST                       │           │
│     │  Access-Control-Request-Headers: Authorization             │           │
│     │                                   │                        │           │
│  2. │                                   │──Check CORS Config─────│          │
│     │                                   │                        │           │
│  3. │◀──200 OK─────────────────────────│                        │           │
│     │  Access-Control-Allow-Origin:     │                        │           │
│     │    http://localhost:30000         │                        │           │
│     │  Access-Control-Allow-Methods:    │                        │           │
│     │    GET, POST, PUT, DELETE         │                        │           │
│     │  Access-Control-Allow-Headers:    │                        │           │
│     │    Authorization, Content-Type    │                        │           │
│     │  Access-Control-Max-Age: 3600     │                        │           │
│     │                                   │                        │           │
│  4. │──POST /api/blog/posts────────────▶│                        │           │
│     │  Origin: http://localhost:30000   │                        │           │
│     │  Authorization: Bearer {token}    │                        │           │
│     │                                   │                        │           │
│  5. │                                   │──JWT Validate──────────│          │
│     │                                   │──Forward Request──────▶│           │
│     │                                   │                        │           │
│  6. │◀──201 Created────────────────────│◀───Response────────────│           │
│     │  Access-Control-Allow-Origin:     │                        │           │
│     │    http://localhost:30000         │                        │           │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Rate Limiting

### 4.1 Rate Limiting 필요성

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        Rate Limiting Benefits                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   1. DDoS 방어                                                               │
│      - 악의적인 대량 요청 차단                                               │
│      - 서비스 가용성 보호                                                    │
│                                                                              │
│   2. 리소스 보호                                                             │
│      - 백엔드 서비스 과부하 방지                                             │
│      - 데이터베이스 부하 제어                                                │
│                                                                              │
│   3. 공정한 사용                                                             │
│      - 사용자/클라이언트별 균등한 리소스 분배                                │
│      - API 남용 방지                                                         │
│                                                                              │
│   4. 비용 관리                                                               │
│      - 클라우드 리소스 비용 제어                                             │
│      - 외부 API 호출 비용 관리                                               │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Spring Cloud Gateway Rate Limiter

```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: blog-service
          uri: lb://blog-service
          predicates:
            - Path=/api/blog/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10  # 초당 10개 요청 허용
                redis-rate-limiter.burstCapacity: 20  # 순간 최대 20개
                redis-rate-limiter.requestedTokens: 1 # 요청당 1개 토큰 소비
                key-resolver: "#{@userKeyResolver}"

        - id: shopping-service
          uri: lb://shopping-service
          predicates:
            - Path=/api/shopping/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 50
                redis-rate-limiter.burstCapacity: 100
                key-resolver: "#{@userKeyResolver}"
```

### 4.3 Key Resolver 구현

```java
// RateLimiterConfig.java
@Configuration
public class RateLimiterConfig {

    /**
     * 사용자 ID 기반 Rate Limit
     * 인증된 사용자는 userId, 비인증은 IP 기반
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders()
                    .getFirst("X-User-Id");

            if (userId != null && !userId.isEmpty()) {
                return Mono.just("user:" + userId);
            }

            // 비인증 사용자는 IP 기반
            String clientIp = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(addr -> addr.getAddress().getHostAddress())
                    .orElse("unknown");

            return Mono.just("ip:" + clientIp);
        };
    }

    /**
     * 경로 기반 Rate Limit
     */
    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getPath().value());
    }

    /**
     * IP 기반 Rate Limit
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String clientIp = Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(addr -> addr.getAddress().getHostAddress())
                    .orElse("unknown");
            return Mono.just(clientIp);
        };
    }
}
```

### 4.4 Rate Limit 응답

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      Rate Limit Response Headers                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   정상 응답 (200):                                                           │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │  X-RateLimit-Remaining: 8                                           │   │
│   │  X-RateLimit-Replenish-Rate: 10                                     │   │
│   │  X-RateLimit-Burst-Capacity: 20                                     │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│   제한 초과 (429 Too Many Requests):                                         │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │  HTTP/1.1 429 Too Many Requests                                     │   │
│   │  X-RateLimit-Remaining: 0                                           │   │
│   │  Retry-After: 1                                                     │   │
│   │                                                                     │   │
│   │  {                                                                  │   │
│   │    "error": "rate_limit_exceeded",                                  │   │
│   │    "message": "요청 한도를 초과했습니다. 잠시 후 다시 시도하세요."   │   │
│   │  }                                                                  │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. 하위 서비스로 사용자 정보 전달

### 5.1 X-User-Id / X-User-Roles 헤더

```java
// JwtAuthenticationFilter.java (Gateway)
// JWT 검증 후 하위 서비스로 헤더 추가
ServerHttpRequest mutatedRequest = request.mutate()
        .header("X-User-Id", userId)
        .header("X-User-Roles", roles != null ? roles : "")
        .build();
```

### 5.2 하위 서비스에서 사용

```java
// BlogController.java (blog-service)
@RestController
@RequestMapping("/api/blog/posts")
public class BlogController {

    @PostMapping
    public ResponseEntity<?> createPost(
            @RequestHeader("X-User-Id") String userId,  // Gateway에서 전달
            @RequestBody PostRequest request) {

        Post post = postService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(PostResponse.from(post)));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyPosts(
            @RequestHeader("X-User-Id") String userId) {

        List<Post> posts = postService.findByAuthorId(userId);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }
}
```

### 5.3 보안 고려사항

```java
// 하위 서비스 SecurityConfig
// X-User-Id 헤더를 신뢰하기 위해 Gateway만 접근 가능하도록 설정
// 또는 내부 네트워크에서만 접근 가능하도록 구성

// 추가 검증 옵션
@Component
public class InternalRequestFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Gateway에서 추가한 서명 헤더 검증
        String signature = exchange.getRequest().getHeaders()
                .getFirst("X-Gateway-Signature");

        if (!validateSignature(signature)) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }
}
```

---

## 6. Logging & Monitoring

### 6.1 Request Logging Filter

```java
// GlobalLoggingFilter.java
@Component
@Slf4j
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        long startTime = System.currentTimeMillis();

        log.info("Request: {} {} from {}",
                request.getMethod(),
                request.getURI().getPath(),
                request.getRemoteAddress());

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;

                    log.info("Response: {} {} - {} ({}ms)",
                            request.getMethod(),
                            request.getURI().getPath(),
                            statusCode,
                            duration);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```

### 6.2 Security Event Logging

```java
// SecurityEventLogger.java
@Component
@Slf4j
public class SecurityEventLogger {

    public void logAuthenticationSuccess(String userId, String path) {
        log.info("AUTH_SUCCESS - user: {}, path: {}", userId, path);
    }

    public void logAuthenticationFailure(String reason, String path, String ip) {
        log.warn("AUTH_FAILURE - reason: {}, path: {}, ip: {}",
                reason, path, ip);
    }

    public void logAccessDenied(String userId, String path) {
        log.warn("ACCESS_DENIED - user: {}, path: {}", userId, path);
    }

    public void logRateLimitExceeded(String key, String path) {
        log.warn("RATE_LIMIT_EXCEEDED - key: {}, path: {}", key, path);
    }
}
```

---

## 7. 경로별 보안 정책 요약

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    Portal Universe Gateway Security Matrix                    │
├────────────────────────────────┬─────────┬──────────┬───────────────────────┤
│            Path                │  Auth   │   Role   │      Rate Limit       │
├────────────────────────────────┼─────────┼──────────┼───────────────────────┤
│ /auth-service/**               │    -    │    -     │   20 req/s (IP)       │
│ /api/auth/**                   │    -    │    -     │   20 req/s (IP)       │
│ /api/users/signup              │    -    │    -     │   5 req/min (IP)      │
├────────────────────────────────┼─────────┼──────────┼───────────────────────┤
│ GET /api/blog/**               │    -    │    -     │   50 req/s (IP)       │
│ POST/PUT/DELETE /api/blog/**   │    O    │   USER   │   10 req/s (User)     │
├────────────────────────────────┼─────────┼──────────┼───────────────────────┤
│ /api/shopping/products/**      │    -    │    -     │   100 req/s (IP)      │
│ /api/shopping/cart/**          │    O    │   USER   │   30 req/s (User)     │
│ /api/shopping/orders/**        │    O    │   USER   │   10 req/s (User)     │
│ /api/shopping/admin/**         │    O    │  ADMIN   │   50 req/s (User)     │
├────────────────────────────────┼─────────┼──────────┼───────────────────────┤
│ /api/profile/**                │    O    │   USER   │   20 req/s (User)     │
├────────────────────────────────┼─────────┼──────────┼───────────────────────┤
│ /actuator/**                   │    -    │    -     │   내부 IP만 허용      │
└────────────────────────────────┴─────────┴──────────┴───────────────────────┘
```

---

## 8. 다음 단계

1. [PKCE for SPA](./pkce-spa-security.md)
2. [Portal Universe Auth Flow](./portal-universe-auth-flow.md)
