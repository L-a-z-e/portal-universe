# Logout & Token Revocation (Redis 블랙리스트)

## 개요

JWT는 Stateless 특성상 서버에서 직접 무효화할 수 없습니다. Portal Universe auth-service는 Redis 블랙리스트를 활용하여 로그아웃된 Access Token을 거부하고, Refresh Token은 Redis에서 삭제하여 토큰을 효과적으로 무효화합니다.

## 로그아웃 전략

### JWT의 Stateless 한계

```
┌─────────────────────────────────────────────────────────────┐
│                   JWT Without Revocation                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  사용자: 로그아웃 요청                                        │
│  서버: "알겠습니다" (하지만 할 수 있는 게 없음)               │
│                                                              │
│  공격자: 탈취한 JWT로 API 요청                               │
│  서버: JWT 서명 유효 → 요청 승인! (문제!)                    │
│                                                              │
│  → 만료 시간까지 토큰이 계속 유효                             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Portal Universe 해결 방법

```
┌─────────────────────────────────────────────────────────────┐
│                 JWT With Redis Blacklist                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. 로그아웃 시:                                             │
│     - Access Token → Redis Blacklist에 추가                  │
│     - Refresh Token → Redis에서 삭제                         │
│                                                              │
│  2. API 요청 시:                                             │
│     - Blacklist 확인 → 있으면 거부 (401)                     │
│     - JWT 검증 → 유효하면 승인                               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## TokenBlacklistService 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

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
            return;  // 이미 만료된 토큰은 블랙리스트 불필요
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

    /**
     * Access Token이 블랙리스트에 있는지 확인합니다.
     *
     * @param token Access Token
     * @return 블랙리스트에 있으면 true, 그렇지 않으면 false
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}
```

## AuthController - 로그아웃 구현

```java
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 로그아웃 API
     * Access Token을 블랙리스트에 추가하고, Refresh Token을 Redis에서 삭제합니다.
     *
     * @param authorization Authorization 헤더 (Bearer {accessToken})
     * @param request 로그아웃 요청 (refreshToken)
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody LogoutRequest request) {

        log.info("Logout attempt");

        // 1. Authorization 헤더에서 Access Token 추출
        String accessToken = TokenUtils.extractBearerToken(authorization);

        try {
            // 2. Access Token 검증 및 userId 추출
            Claims claims = tokenService.validateAccessToken(accessToken);
            String userId = claims.getSubject();

            // 3. Access Token 블랙리스트 추가
            long remainingExpiration = tokenService.getRemainingExpiration(accessToken);
            tokenBlacklistService.addToBlacklist(accessToken, remainingExpiration);

            // 4. Refresh Token Redis에서 삭제
            refreshTokenService.deleteRefreshToken(userId);

            log.info("Logout successful for user: {}", userId);

            return ResponseEntity.ok(
                    ApiResponse.success(Map.of("message", "로그아웃 성공")));
        } catch (Exception e) {
            log.warn("Logout failed: {}", e.getMessage());
            throw new CustomBusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
```

## Redis 데이터 구조

### Blacklist

```
┌─────────────────────────────────────────────────────────────┐
│                    Redis Blacklist                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Key: blacklist:{jwt_token_string}                           │
│  Value: "blacklisted"                                        │
│  TTL: 토큰 남은 만료 시간                                     │
│                                                              │
│  예시:                                                       │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Key: blacklist:eyJhbGciOiJIUzI1NiJ9.eyJzdWIi...    │    │
│  │ Value: "blacklisted"                                │    │
│  │ TTL: 845000ms (약 14분)                             │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  * 토큰 만료 시 자동 삭제됨 (메모리 효율)                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Refresh Token

```
┌─────────────────────────────────────────────────────────────┐
│                  Redis Refresh Token                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Key: refresh_token:{user_uuid}                              │
│  Value: {jwt_refresh_token_string}                           │
│  TTL: 604800000ms (7일)                                      │
│                                                              │
│  예시:                                                       │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ Key: refresh_token:550e8400-e29b-41d4-a716-4466... │    │
│  │ Value: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBl...   │    │
│  │ TTL: 604800000ms (7일)                              │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  * 로그아웃 시 삭제됨                                         │
│  * 사용자당 하나만 존재 (최신 토큰만 유효)                    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 로그아웃 Flow

```
┌──────────┐    POST /api/auth/logout     ┌────────────────┐
│  Client  │ ─────────────────────────▶   │  AuthController │
└──────────┘  Authorization: Bearer xxx    └───────┬────────┘
              Body: { refreshToken }               │
                                                   │
                                     ┌─────────────┼─────────────┐
                                     ▼             ▼             ▼
                            ┌─────────────┐  ┌───────────────┐  ┌───────┐
                            │TokenService │  │ Blacklist Svc │  │ Redis │
                            │validateToken│  │ addToBlacklist│  │       │
                            └─────────────┘  └───────────────┘  └───────┘
                                     │             │             │
                                     │ getRemainingExpiration()  │
                                     │─────────────▶             │
                                     │             │             │
                                     │             │ SET blacklist:xxx │
                                     │             │────────────▶│
                                     │             │             │
                            ┌─────────────┐        │             │
                            │RefreshToken │        │             │
                            │Service      │        │             │
                            │deleteToken  │        │             │
                            └─────────────┘        │             │
                                     │             │             │
                                     │ DEL refresh_token:uuid    │
                                     │─────────────────────────▶ │
                                     │             │             │
                                     ▼             │             │
                            ┌───────────────────────────────────┐
                            │ Response:                          │
                            │ { message: "로그아웃 성공" }       │
                            └───────────────────────────────────┘
```

## 검증 시 Blacklist 확인

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                               HttpServletResponse response,
                               FilterChain filterChain) throws ServletException, IOException {

    String token = extractToken(request);

    // 1. 토큰이 있고 2. 블랙리스트에 없는 경우에만 검증 진행
    if (token != null && !tokenBlacklistService.isBlacklisted(token)) {
        try {
            Claims claims = tokenService.validateAccessToken(token);
            // ... 인증 처리
        } catch (Exception e) {
            // 검증 실패
        }
    }
    // 블랙리스트에 있으면 인증 없이 다음 필터로 (401 응답)

    filterChain.doFilter(request, response);
}
```

## 모든 세션 로그아웃 (선택적 구현)

```java
/**
 * 사용자의 모든 세션 종료 (비밀번호 변경, 보안 이슈 시)
 */
@PostMapping("/logout-all")
public ResponseEntity<ApiResponse<Map<String, String>>> logoutAll(
        @RequestHeader("Authorization") String authorization) {

    String accessToken = TokenUtils.extractBearerToken(authorization);
    Claims claims = tokenService.validateAccessToken(accessToken);
    String userId = claims.getSubject();

    // 1. 현재 Access Token 블랙리스트 추가
    long remainingExpiration = tokenService.getRemainingExpiration(accessToken);
    tokenBlacklistService.addToBlacklist(accessToken, remainingExpiration);

    // 2. Refresh Token 삭제
    refreshTokenService.deleteRefreshToken(userId);

    // 3. 토큰 버전 증가 (선택적 - 모든 기존 토큰 무효화)
    // userService.incrementTokenVersion(userId);

    log.info("All sessions logged out for user: {}", userId);

    return ResponseEntity.ok(
        ApiResponse.success(Map.of("message", "모든 세션이 종료되었습니다"))
    );
}
```

## Frontend 로그아웃 처리

```typescript
async function logout(): Promise<void> {
  const accessToken = localStorage.getItem('access_token');
  const refreshToken = localStorage.getItem('refresh_token');

  if (accessToken && refreshToken) {
    try {
      await api.post('/api/auth/logout',
        { refreshToken },
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
    } catch (error) {
      // 서버 오류가 있어도 로컬 토큰은 삭제
      console.warn('Server logout failed:', error);
    }
  }

  // 로컬 토큰 삭제 (항상 실행)
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');

  // 로그인 페이지로 이동
  window.location.href = '/login';
}
```

## 보안 고려사항

### 1. Blacklist 크기 관리

```java
// TTL 설정으로 자동 정리 (메모리 효율)
redisTemplate.opsForValue().set(
    key,
    "blacklisted",
    remainingExpiration,  // 토큰 만료 시 자동 삭제
    TimeUnit.MILLISECONDS
);
```

### 2. Redis 장애 시 대응

```java
public boolean isBlacklisted(String token) {
    try {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    } catch (RedisConnectionFailureException e) {
        // Redis 연결 실패 시 정책 결정:
        // Option 1: 보안 우선 - 모든 토큰 거부
        log.error("Redis connection failed, rejecting all tokens");
        return true;

        // Option 2: 가용성 우선 - JWT 검증만 수행
        // log.warn("Redis connection failed, skipping blacklist check");
        // return false;
    }
}
```

### 3. 로그아웃 감사 로그

```java
@PostMapping("/logout")
public ResponseEntity<?> logout(...) {
    // ...로그아웃 처리

    // 감사 로그 기록
    auditLogService.log(AuditEvent.builder()
        .action("LOGOUT")
        .userId(userId)
        .ipAddress(request.getRemoteAddr())
        .userAgent(request.getHeader("User-Agent"))
        .timestamp(Instant.now())
        .build());

    return ResponseEntity.ok(...);
}
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/TokenBlacklistService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/RefreshTokenService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/controller/AuthController.java`

## 참고 자료

- [OAuth 2.0 Token Revocation (RFC 7009)](https://datatracker.ietf.org/doc/html/rfc7009)
- [JWT Revocation Strategies](https://auth0.com/blog/denylist-json-web-token-api-keys/)
