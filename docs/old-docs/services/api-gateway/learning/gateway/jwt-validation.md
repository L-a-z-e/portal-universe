# JWT 검증 (API Gateway)

## 학습 목표
- Gateway 레벨 JWT 검증 방식 이해
- 경로별 접근 제어 학습
- 하위 서비스로의 사용자 정보 전달 패턴

---

## 1. JWT 검증 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           JWT VALIDATION FLOW                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌──────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│   │  Client  │───►│ API Gateway  │───►│  JWT Filter  │───►│   Service    │ │
│   │          │    │              │    │              │    │              │ │
│   └──────────┘    └──────────────┘    └──────────────┘    └──────────────┘ │
│        │                                     │                    │         │
│        │ Authorization: Bearer <token>       │                    │         │
│        │                                     ▼                    │         │
│        │                          ┌──────────────────┐            │         │
│        │                          │ Token Validation │            │         │
│        │                          │ ─────────────────│            │         │
│        │                          │ 1. 서명 검증     │            │         │
│        │                          │ 2. 만료 확인     │            │         │
│        │                          │ 3. Claims 추출   │            │         │
│        │                          └──────────────────┘            │         │
│        │                                     │                    │         │
│        │                                     ▼                    │         │
│        │                          ┌──────────────────┐            │         │
│        │                          │  Header 추가     │            │         │
│        │                          │ ─────────────────│            │         │
│        │                          │ X-User-Id        │───────────►│         │
│        │                          │ X-User-Roles     │            │         │
│        │                          └──────────────────┘            │         │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. JwtAuthenticationFilter 분석

### 2.1 필터 구조

```java
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtConfig jwtConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1. 공개 경로 확인
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 2. Authorization 헤더 추출
        String authHeader = request.getHeaders()
            .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);  // SecurityConfig에 위임
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // 3. JWT 검증
            Claims claims = validateToken(token);
            String userId = claims.getSubject();
            String roles = claims.get("roles", String.class);

            // 4. Security Context 설정
            List<SimpleGrantedAuthority> authorities = roles != null
                ? List.of(new SimpleGrantedAuthority(roles))
                : Collections.emptyList();

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // 5. 헤더 추가 (하위 서비스용)
            ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Roles", roles != null ? roles : "")
                .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (ExpiredJwtException e) {
            return handleUnauthorized(exchange, "Token expired");
        } catch (JwtException e) {
            return handleUnauthorized(exchange, "Invalid token");
        }
    }
}
```

### 2.2 토큰 검증 로직

```java
private SecretKey getSigningKey() {
    byte[] keyBytes = jwtConfig.getSecretKey()
        .getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);  // HMAC-SHA256
}

private Claims validateToken(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())  // 서명 검증
        .build()
        .parseSignedClaims(token)     // 파싱
        .getPayload();                // Claims 반환
}
```

---

## 3. 공개 경로 설정

### 3.1 JwtAuthenticationFilter의 공개 경로

```java
private boolean isPublicPath(String path) {
    return path.startsWith("/auth-service/") ||
           path.startsWith("/api/auth/") ||
           path.startsWith("/api/users/") ||
           path.startsWith("/actuator/") ||
           // Shopping 공개 경로
           path.equals("/api/shopping/products") ||
           path.startsWith("/api/shopping/products/") ||
           path.equals("/api/shopping/categories") ||
           path.startsWith("/api/shopping/categories/") ||
           path.equals("/api/shopping/coupons") ||
           path.equals("/api/shopping/time-deals") ||
           path.startsWith("/api/shopping/time-deals/");
}
```

### 3.2 SecurityConfig의 접근 제어

```java
.authorizeExchange(authorize -> authorize
    // [공개] 인증 없이 접근 가능
    .pathMatchers("/auth-service/**", "/api/auth/**", "/api/users/**")
        .permitAll()

    // Blog - GET 요청은 공개
    .pathMatchers(HttpMethod.GET, "/api/blog/**")
        .permitAll()

    // Shopping - 상품/카테고리 조회 공개
    .pathMatchers("/api/shopping/products", "/api/shopping/products/**")
        .permitAll()
    .pathMatchers("/api/shopping/categories", "/api/shopping/categories/**")
        .permitAll()

    // Actuator
    .pathMatchers("/actuator/**", "/api/*/actuator/**")
        .permitAll()

    // [관리자] ADMIN 권한 필요
    .pathMatchers("/api/shopping/admin/**")
        .hasRole("ADMIN")

    // [비공개] 인증 필요
    .anyExchange().authenticated()
)
```

---

## 4. 접근 제어 매트릭스

### 4.1 서비스별 접근 권한

| 경로 | GET | POST | PUT | DELETE | 필요 권한 |
|------|-----|------|-----|--------|----------|
| `/api/auth/**` | ✓ | ✓ | ✓ | ✓ | 공개 |
| `/api/users/**` | ✓ | ✓ | ✓ | ✓ | 공개 |
| `/api/blog/**` | ✓ | 인증 | 인증 | 인증 | GET만 공개 |
| `/api/shopping/products/**` | ✓ | 인증 | 인증 | 인증 | GET만 공개 |
| `/api/shopping/cart/**` | 인증 | 인증 | 인증 | 인증 | 인증 필요 |
| `/api/shopping/orders/**` | 인증 | 인증 | 인증 | 인증 | 인증 필요 |
| `/api/shopping/admin/**` | ADMIN | ADMIN | ADMIN | ADMIN | ADMIN |

### 4.2 공개 vs 비공개 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ACCESS CONTROL ZONES                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌────────────────────────────────────────────────────────────────────┐    │
│   │                       PUBLIC ZONE (인증 불필요)                     │    │
│   ├────────────────────────────────────────────────────────────────────┤    │
│   │  • /api/auth/**          인증/회원가입                              │    │
│   │  • /api/users/**         사용자 정보                                │    │
│   │  • GET /api/blog/**      블로그 조회                                │    │
│   │  • GET /api/shopping/products/**  상품 조회                         │    │
│   │  • GET /api/shopping/categories/**  카테고리 조회                   │    │
│   │  • GET /api/shopping/time-deals/**  타임딜 조회                     │    │
│   │  • /actuator/**          헬스체크                                   │    │
│   └────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│   ┌────────────────────────────────────────────────────────────────────┐    │
│   │                    AUTHENTICATED ZONE (인증 필요)                   │    │
│   ├────────────────────────────────────────────────────────────────────┤    │
│   │  • POST/PUT/DELETE /api/blog/**  블로그 작성/수정/삭제              │    │
│   │  • /api/shopping/cart/**    장바구니                                │    │
│   │  • /api/shopping/orders/**  주문                                    │    │
│   │  • /api/shopping/payments/**  결제                                  │    │
│   │  • /api/shopping/coupons/issue  쿠폰 발급                           │    │
│   └────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│   ┌────────────────────────────────────────────────────────────────────┐    │
│   │                      ADMIN ZONE (관리자 권한)                       │    │
│   ├────────────────────────────────────────────────────────────────────┤    │
│   │  • /api/shopping/admin/**   관리자 기능                             │    │
│   │    - 상품 관리                                                      │    │
│   │    - 주문 관리                                                      │    │
│   │    - 재고 관리                                                      │    │
│   │    - 쿠폰 관리                                                      │    │
│   └────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. 하위 서비스로 사용자 정보 전달

### 5.1 헤더 추가

```java
// Gateway에서 추가
ServerHttpRequest mutatedRequest = request.mutate()
    .header("X-User-Id", userId)
    .header("X-User-Roles", roles != null ? roles : "")
    .build();
```

### 5.2 하위 서비스에서 사용

```java
// Shopping Service Controller
@GetMapping("/cart")
public ResponseEntity<ApiResponse<CartResponse>> getCart(
    @RequestHeader("X-User-Id") String userId) {

    CartResponse cart = cartService.getCart(userId);
    return ResponseEntity.ok(ApiResponse.success(cart));
}
```

### 5.3 전달 흐름

```
Client                 Gateway              Shopping Service
  │                       │                        │
  │ Authorization: Bearer │                        │
  │ eyJhbGciOiJIUzI1...   │                        │
  ├──────────────────────►│                        │
  │                       │ X-User-Id: user123     │
  │                       │ X-User-Roles: USER     │
  │                       ├───────────────────────►│
  │                       │                        │
  │                       │◄───────────────────────┤
  │◄──────────────────────┤                        │
  │                       │                        │
```

---

## 6. CORS 설정

### 6.1 CorsWebFilter

```java
@Bean
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public CorsWebFilter corsWebFilter() {
    CorsConfiguration configuration = new CorsConfiguration();

    // 허용 Origin
    configuration.setAllowedOrigins(List.of(
        "http://localhost:30000",    // Portal Shell
        "http://localhost:8080",     // Gateway (테스트)
        "https://portal-universe:30000"
    ));

    // 허용 메서드
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS"
    ));

    // 허용 헤더
    configuration.setAllowedHeaders(List.of("*"));

    // 쿠키/인증 헤더 허용
    configuration.setAllowCredentials(true);

    // Preflight 캐시 시간
    configuration.setMaxAge(3600L);  // 1시간

    UrlBasedCorsConfigurationSource source =
        new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return new CorsWebFilter(source);
}
```

### 6.2 Preflight 요청 처리

```
Browser                    Gateway                  Service
   │                          │                         │
   │ OPTIONS /api/shopping/cart                        │
   │ Origin: http://localhost:30000                    │
   ├─────────────────────────►│                         │
   │                          │                         │
   │ 200 OK                   │                         │
   │ Access-Control-Allow-Origin: http://localhost:30000
   │ Access-Control-Allow-Methods: GET, POST, ...       │
   │◄─────────────────────────┤                         │
   │                          │                         │
   │ POST /api/shopping/cart  │                         │
   ├─────────────────────────►│                         │
   │                          ├────────────────────────►│
```

---

## 7. 에러 처리

### 7.1 401 Unauthorized

```java
private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().add("X-Auth-Error", message);
    return exchange.getResponse().setComplete();
}
```

### 7.2 에러 시나리오

| 시나리오 | X-Auth-Error | HTTP Status |
|----------|--------------|-------------|
| 토큰 없음 | - | SecurityConfig에 위임 |
| 토큰 만료 | Token expired | 401 |
| 잘못된 토큰 | Invalid token | 401 |
| 서명 불일치 | Invalid token | 401 |

---

## 8. 핵심 정리

| 개념 | 설명 |
|------|------|
| **HMAC-SHA256** | Auth Service와 동일한 secret key로 서명 검증 |
| **isPublicPath()** | JWT 검증 생략 경로 |
| **SecurityConfig** | 경로별 접근 제어 (permitAll, authenticated, hasRole) |
| **X-User-Id** | 검증된 사용자 ID를 하위 서비스에 전달 |
| **X-User-Roles** | 사용자 권한을 하위 서비스에 전달 |
| **CorsWebFilter** | Gateway 레벨 CORS 처리 |

---

## 다음 학습

- [Spring Cloud Gateway](./spring-cloud-gateway.md)
- [Circuit Breaker](./circuit-breaker.md)
- [OAuth2 인증 흐름](../../docs/learning/security/portal-universe-auth-flow.md)
