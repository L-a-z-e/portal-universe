# Access/Refresh Token 관리

## 개요

Portal Universe auth-service는 JWT 기반의 Access Token과 Refresh Token 이중 토큰 시스템을 사용합니다. Access Token은 짧은 수명으로 API 인증에 사용되고, Refresh Token은 긴 수명으로 새로운 Access Token 발급에 사용됩니다.

## 토큰 구조

### Access Token

| 항목 | 값 | 설명 |
|------|-----|------|
| 알고리즘 | HS256 | HMAC-SHA256 대칭키 |
| 만료 시간 | 15분 | 900,000ms |
| 저장 위치 | Client | localStorage/memory |
| 용도 | API 인증 | Authorization 헤더 |

### Refresh Token

| 항목 | 값 | 설명 |
|------|-----|------|
| 알고리즘 | HS256 | HMAC-SHA256 대칭키 |
| 만료 시간 | 7일 | 604,800,000ms |
| 저장 위치 | Redis + Client | 서버 검증 가능 |
| 용도 | 토큰 갱신 | Access Token 재발급 |

## TokenService 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtConfig jwtConfig;

    /**
     * Secret Key를 기반으로 서명용 키를 생성합니다.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Access Token 생성
     * Claims: sub(UUID), roles, email, nickname, username
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRole().getKey());
        claims.put("email", user.getEmail());

        // Profile 정보 추가
        if (user.getProfile() != null) {
            claims.put("nickname", user.getProfile().getNickname());
            if (user.getProfile().getUsername() != null) {
                claims.put("username", user.getProfile().getUsername());
            }
        }

        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUuid())       // sub claim
                .issuedAt(now)                 // iat claim
                .expiration(expiration)        // exp claim
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Refresh Token 생성
     * Claims: sub(UUID) - 최소한의 정보만 포함
     */
    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(user.getUuid())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }
}
```

## RefreshTokenService - Redis 관리

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtConfig jwtConfig;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * Refresh Token을 Redis에 저장
     * Key: refresh_token:{userId}
     * TTL: refreshTokenExpiration
     */
    public void saveRefreshToken(String userId, String token) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(
                key,
                token,
                jwtConfig.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Redis에서 Refresh Token 조회
     */
    public String getRefreshToken(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        Object token = redisTemplate.opsForValue().get(key);
        return token != null ? token.toString() : null;
    }

    /**
     * Refresh Token 삭제 (로그아웃 시)
     */
    public void deleteRefreshToken(String userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
    }

    /**
     * Refresh Token 검증
     * - Redis에 저장된 토큰과 요청 토큰 비교
     */
    public boolean validateRefreshToken(String userId, String token) {
        String storedToken = getRefreshToken(userId);
        if (storedToken == null) {
            log.warn("Refresh token not found for user: {}", userId);
            return false;
        }
        return storedToken.equals(token);
    }
}
```

## 토큰 흐름

### 1. 로그인 시

```
┌──────────┐    POST /api/auth/login     ┌────────────────┐
│  Client  │ ─────────────────────────▶  │  AuthController │
└──────────┘   {email, password}         └───────┬────────┘
                                                 │
                                    ┌────────────┼────────────┐
                                    ▼            ▼            ▼
                            ┌───────────┐  ┌───────────┐  ┌───────┐
                            │TokenService│  │RefreshToken│  │ Redis │
                            │generateXXX │  │Service     │  │       │
                            └───────────┘  └───────────┘  └───────┘
                                    │            │            │
                                    │   saveRefreshToken()    │
                                    │            ────────────▶│
                                    ▼                         │
                            ┌───────────────────────────────────┐
                            │ Response:                          │
                            │ {                                  │
                            │   accessToken: "eyJ...",          │
                            │   refreshToken: "eyJ...",         │
                            │   expiresIn: 900                  │
                            │ }                                  │
                            └───────────────────────────────────┘
```

### 2. API 요청 시

```
┌──────────┐   GET /api/users/me          ┌──────────────────────┐
│  Client  │ ─────────────────────────▶   │ JwtAuthenticationFilter │
└──────────┘   Authorization: Bearer xxx   └───────────┬──────────┘
                                                       │
                                      ┌────────────────┼────────────────┐
                                      ▼                ▼                ▼
                              ┌────────────┐   ┌──────────────┐   ┌─────────┐
                              │TokenService │   │BlacklistService│   │Controller│
                              │validateXXX  │   │isBlacklisted  │   │         │
                              └────────────┘   └──────────────┘   └─────────┘
```

### 3. 토큰 갱신 시

```
┌──────────┐   POST /api/auth/refresh    ┌────────────────┐
│  Client  │ ─────────────────────────▶  │  AuthController │
└──────────┘   {refreshToken}            └───────┬────────┘
                                                 │
                              ┌──────────────────┼──────────────────┐
                              ▼                  ▼                  ▼
                      ┌────────────┐     ┌───────────────┐   ┌───────┐
                      │TokenService │     │RefreshTokenSvc │   │ Redis │
                      │validate     │     │validate        │   │       │
                      └────────────┘     └───────────────┘   └───────┘
                              │                  │                  │
                              │     validateRefreshToken()         │
                              │                  ────────────────▶ │
                              │                                    │
                              │     Compare tokens                 │
                              │                  ◀──────────────── │
                              ▼
                      ┌───────────────────────────────────┐
                      │ Response:                          │
                      │ {                                  │
                      │   accessToken: "eyJ...(NEW)",     │
                      │   expiresIn: 900                  │
                      │ }                                  │
                      └───────────────────────────────────┘
```

## AuthController 구현

### 로그인

```java
@PostMapping("/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request) {

    // 1. 사용자 조회 (프로필 포함)
    User user = userRepository.findByEmailWithProfile(request.email())
            .orElseThrow(() -> new CustomBusinessException(
                AuthErrorCode.INVALID_CREDENTIALS));

    // 2. 비밀번호 검증
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
        throw new CustomBusinessException(AuthErrorCode.INVALID_CREDENTIALS);
    }

    // 3. Access Token 발급
    String accessToken = tokenService.generateAccessToken(user);

    // 4. Refresh Token 발급 및 Redis 저장
    String refreshToken = tokenService.generateRefreshToken(user);
    refreshTokenService.saveRefreshToken(user.getUuid(), refreshToken);

    LoginResponse response = new LoginResponse(accessToken, refreshToken, 900);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

### 토큰 갱신

```java
@PostMapping("/refresh")
public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
        @Valid @RequestBody RefreshRequest request) {

    try {
        // 1. Refresh Token JWT 검증
        Claims claims = tokenService.validateAccessToken(request.refreshToken());
        String userId = claims.getSubject();

        // 2. Redis 저장 토큰과 비교
        if (!refreshTokenService.validateRefreshToken(userId, request.refreshToken())) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. 사용자 정보 조회
        User user = userRepository.findByUuidWithProfile(userId)
                .orElseThrow(() -> new CustomBusinessException(
                    AuthErrorCode.USER_NOT_FOUND));

        // 4. 새 Access Token 발급
        String accessToken = tokenService.generateAccessToken(user);

        RefreshResponse response = new RefreshResponse(accessToken, 900);
        return ResponseEntity.ok(ApiResponse.success(response));
    } catch (Exception e) {
        throw new CustomBusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }
}
```

## Frontend 토큰 관리

### Axios Interceptor 예시

```typescript
// API 요청 시 Access Token 자동 첨부
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 401 응답 시 자동 갱신
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refresh_token');

      if (refreshToken) {
        try {
          const { data } = await api.post('/api/auth/refresh', {
            refreshToken
          });

          localStorage.setItem('access_token', data.data.accessToken);

          // 실패한 요청 재시도
          error.config.headers.Authorization =
            `Bearer ${data.data.accessToken}`;
          return api(error.config);
        } catch (refreshError) {
          // Refresh Token도 만료됨 - 로그아웃
          localStorage.removeItem('access_token');
          localStorage.removeItem('refresh_token');
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);
```

## Best Practices

### 1. Access Token은 메모리 저장 권장

```typescript
// 권장: 메모리에 저장 (XSS 공격 방지)
let accessToken: string | null = null;

export function setAccessToken(token: string) {
  accessToken = token;
}

export function getAccessToken() {
  return accessToken;
}
```

### 2. Refresh Token은 HttpOnly Cookie 고려

```java
// HttpOnly Cookie로 설정 (JavaScript 접근 불가)
ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
    .httpOnly(true)
    .secure(true)  // HTTPS only
    .sameSite("Strict")
    .path("/api/auth/refresh")
    .maxAge(Duration.ofDays(7))
    .build();

response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
```

### 3. 토큰 만료 시간 설정 가이드

| 환경 | Access Token | Refresh Token |
|------|-------------|---------------|
| 일반 웹앱 | 15분 | 7일 |
| 금융/보안 | 5분 | 1일 |
| 모바일 앱 | 30분 | 30일 |

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/TokenService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/RefreshTokenService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/controller/AuthController.java`

## 참고 자료

- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [OAuth 2.0 Token Storage](https://oauth.net/articles/authentication/)
