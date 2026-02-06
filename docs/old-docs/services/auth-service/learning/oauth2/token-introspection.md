# Token Introspection (토큰 검증)

## 개요

Token Introspection은 발급된 토큰의 유효성을 검증하는 과정입니다. Portal Universe auth-service는 JWT 자체 검증과 Redis 블랙리스트 확인을 조합하여 토큰을 검증합니다.

## 검증 단계

```
┌─────────────────────────────────────────────────────────────┐
│                    Token Validation Flow                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Token 추출 (Authorization Header)                        │
│     ▼                                                        │
│  2. Blacklist 확인 (Redis)                                   │
│     ▼                                                        │
│  3. JWT 서명 검증 (HMAC-SHA256)                              │
│     ▼                                                        │
│  4. 만료 시간 확인 (exp claim)                               │
│     ▼                                                        │
│  5. Claims 추출 및 인증 객체 생성                            │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## TokenService - 토큰 검증 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtConfig jwtConfig;

    /**
     * Access Token을 검증하고 Claims를 반환합니다.
     *
     * @param token Access Token
     * @return Claims
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    public Claims validateAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())  // 서명 검증
                    .build()
                    .parseSignedClaims(token)     // 파싱 + 검증
                    .getPayload();                // Claims 반환
        } catch (ExpiredJwtException e) {
            log.warn("Access token expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 토큰에서 사용자 UUID를 추출합니다.
     */
    public String getUserIdFromToken(String token) {
        Claims claims = validateAccessToken(token);
        return claims.getSubject();
    }

    /**
     * 토큰의 남은 만료 시간(밀리초)을 계산합니다.
     */
    public long getRemainingExpiration(String token) {
        Claims claims = validateAccessToken(token);
        Date expiration = claims.getExpiration();
        Date now = new Date();
        return expiration.getTime() - now.getTime();
    }
}
```

## JwtAuthenticationFilter - 요청별 검증

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
                // 인증 실패 시 Spring Security가 401 응답
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    /**
     * 공개 엔드포인트는 필터 건너뜀
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

## JWT 검증 예외 처리

### 예외 종류

| 예외 | 원인 | HTTP 상태 |
|------|------|----------|
| `ExpiredJwtException` | 토큰 만료 | 401 |
| `UnsupportedJwtException` | 지원하지 않는 형식 | 401 |
| `MalformedJwtException` | 잘못된 토큰 구조 | 401 |
| `SecurityException` | 서명 불일치 | 401 |
| `IllegalArgumentException` | 빈 토큰 | 401 |

### 예외별 처리 예시

```java
public Claims validateToken(String token) {
    try {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();

    } catch (ExpiredJwtException e) {
        // 토큰 만료 - Refresh Token으로 갱신 필요
        log.info("Token expired for user: {}", e.getClaims().getSubject());
        throw new TokenExpiredException("Token has expired");

    } catch (SecurityException e) {
        // 서명 위변조 - 보안 이슈 로깅
        log.error("Invalid signature detected - potential attack");
        throw new InvalidTokenException("Invalid token signature");

    } catch (MalformedJwtException e) {
        // 잘못된 형식 - 클라이언트 오류
        log.warn("Malformed token received");
        throw new InvalidTokenException("Malformed token");
    }
}
```

## Blacklist 검증 (Redis)

### TokenBlacklistService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Access Token이 블랙리스트에 있는지 확인합니다.
     *
     * @param token Access Token
     * @return 블랙리스트에 있으면 true
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Access Token을 블랙리스트에 추가합니다.
     * TTL은 토큰의 남은 만료 시간으로 설정됩니다.
     *
     * @param token Access Token
     * @param remainingExpiration 남은 만료 시간 (밀리초)
     */
    public void addToBlacklist(String token, long remainingExpiration) {
        if (remainingExpiration <= 0) {
            log.warn("Cannot blacklist expired token");
            return;
        }

        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(
                key,
                "blacklisted",
                remainingExpiration,
                TimeUnit.MILLISECONDS
        );
        log.info("Token added to blacklist with TTL: {}ms", remainingExpiration);
    }
}
```

### Redis 저장 구조

```
┌─────────────────────────────────────────────────────────────┐
│                      Redis Blacklist                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Key Pattern: blacklist:{jwt_token}                          │
│  Value: "blacklisted"                                        │
│  TTL: 토큰 남은 만료 시간                                     │
│                                                              │
│  예시:                                                       │
│  blacklist:eyJhbGciOiJIUzI1NiJ9.eyJz... → "blacklisted"    │
│  TTL: 845000ms (약 14분)                                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 다른 서비스에서 토큰 검증

### 방법 1: 동일 Secret Key 공유

```java
// blog-service, shopping-service 등에서
@Service
public class TokenValidator {

    @Value("${jwt.secret-key}")
    private String secretKey;

    public Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
            secretKey.getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
```

### 방법 2: Auth Service API 호출 (권장)

```java
// Feign Client를 통한 토큰 검증
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @PostMapping("/api/auth/introspect")
    ApiResponse<TokenInfo> introspectToken(
        @RequestHeader("Authorization") String token
    );
}

// 사용
@Service
public class TokenValidator {

    private final AuthServiceClient authClient;

    public boolean isValidToken(String token) {
        try {
            var response = authClient.introspectToken("Bearer " + token);
            return response.getData().isActive();
        } catch (Exception e) {
            return false;
        }
    }
}
```

## Frontend 토큰 검증

```typescript
import jwtDecode from 'jwt-decode';

interface JwtPayload {
  sub: string;
  roles: string;
  exp: number;
}

/**
 * 토큰 만료 여부 확인 (로컬 검증)
 */
function isTokenExpired(token: string): boolean {
  try {
    const decoded = jwtDecode<JwtPayload>(token);
    // exp는 초 단위, Date.now()는 밀리초 단위
    return decoded.exp * 1000 < Date.now();
  } catch {
    return true; // 디코딩 실패 = 유효하지 않음
  }
}

/**
 * 토큰 만료 전 자동 갱신
 */
function shouldRefreshToken(token: string): boolean {
  try {
    const decoded = jwtDecode<JwtPayload>(token);
    const expiresIn = decoded.exp * 1000 - Date.now();
    // 5분 전에 갱신 시작
    return expiresIn < 5 * 60 * 1000;
  } catch {
    return true;
  }
}
```

## 검증 Flow 다이어그램

```
┌────────────────┐
│   API Request  │
│ Authorization: │
│ Bearer eyJ...  │
└───────┬────────┘
        │
        ▼
┌───────────────────┐
│ JwtAuthentication │
│     Filter        │
└───────┬───────────┘
        │
        ▼
┌───────────────────┐      Yes    ┌─────────────┐
│ Is Blacklisted?   │ ─────────▶  │ 401 Error   │
│ (Redis Check)     │             └─────────────┘
└───────┬───────────┘
        │ No
        ▼
┌───────────────────┐      Invalid  ┌─────────────┐
│  Verify JWT       │ ─────────────▶│ 401 Error   │
│ - Signature       │               └─────────────┘
│ - Expiration      │
└───────┬───────────┘
        │ Valid
        ▼
┌───────────────────┐
│ Extract Claims    │
│ - userId (sub)    │
│ - roles           │
└───────┬───────────┘
        │
        ▼
┌───────────────────┐
│ Set Authentication│
│ to SecurityContext│
└───────┬───────────┘
        │
        ▼
┌───────────────────┐
│   Continue to     │
│   Controller      │
└───────────────────┘
```

## Best Practices

### 1. 검증 순서 최적화

```java
// 1. 블랙리스트 먼저 확인 (Redis 조회가 JWT 파싱보다 빠름)
if (tokenBlacklistService.isBlacklisted(token)) {
    return; // 빠른 거부
}

// 2. JWT 검증 (CPU 연산 필요)
Claims claims = tokenService.validateAccessToken(token);
```

### 2. 캐싱 고려

```java
// 동일 토큰 반복 검증 시 캐싱 적용
@Cacheable(value = "tokenValidation", key = "#token")
public boolean isTokenValid(String token) {
    // ...검증 로직
}
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/TokenService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/security/JwtAuthenticationFilter.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/TokenBlacklistService.java`

## 참고 자료

- [OAuth 2.0 Token Introspection (RFC 7662)](https://datatracker.ietf.org/doc/html/rfc7662)
- [JJWT Documentation](https://github.com/jwtk/jjwt)
