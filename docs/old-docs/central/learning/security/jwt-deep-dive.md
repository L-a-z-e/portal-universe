# JWT Deep Dive

JSON Web Token (JWT)은 당사자 간에 정보를 안전하게 전송하기 위한 **자체 포함(self-contained)** 토큰 형식입니다.

## 1. JWT 구조

JWT는 점(`.`)으로 구분된 세 부분으로 구성됩니다.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           JWT Structure                                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.                                      │
│   eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.│
│   SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c                                │
│                                                                              │
│   └────────┬────────┘ └───────────────────┬────────────────────┘ └────┬────┘│
│          Header                         Payload                   Signature  │
│                                                                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                          Header                                     │   │
│   │   {                                                                 │   │
│   │     "alg": "HS256",   // 서명 알고리즘                               │   │
│   │     "typ": "JWT"      // 토큰 타입                                   │   │
│   │   }                                                                 │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                          Payload (Claims)                           │   │
│   │   {                                                                 │   │
│   │     "sub": "user-uuid-123",     // Subject (사용자 식별자)          │   │
│   │     "email": "user@example.com", // Custom claim                    │   │
│   │     "roles": "ROLE_USER",        // Custom claim                    │   │
│   │     "iat": 1516239022,           // Issued At (발급 시간)           │   │
│   │     "exp": 1516239922            // Expiration (만료 시간)          │   │
│   │   }                                                                 │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                          Signature                                  │   │
│   │                                                                     │   │
│   │   HMACSHA256(                                                       │   │
│   │     base64UrlEncode(header) + "." + base64UrlEncode(payload),       │   │
│   │     secret                                                          │   │
│   │   )                                                                 │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 Registered Claims (표준 클레임)

| Claim | 이름 | 설명 | Portal Universe 사용 |
|-------|------|------|---------------------|
| `iss` | Issuer | 토큰 발급자 | - |
| `sub` | Subject | 토큰 주체 (사용자 ID) | User UUID |
| `aud` | Audience | 토큰 수신자 | - |
| `exp` | Expiration | 만료 시간 (Unix timestamp) | 사용 |
| `nbf` | Not Before | 토큰 활성화 시간 | - |
| `iat` | Issued At | 발급 시간 (Unix timestamp) | 사용 |
| `jti` | JWT ID | 토큰 고유 식별자 | - |

### 1.2 Portal Universe Custom Claims

```java
// TokenService.java
public String generateAccessToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("roles", user.getRole().getKey());      // ROLE_USER, ROLE_ADMIN
    claims.put("email", user.getEmail());

    // Profile 정보 추가
    if (user.getProfile() != null) {
        claims.put("nickname", user.getProfile().getNickname());
        if (user.getProfile().getUsername() != null) {
            claims.put("username", user.getProfile().getUsername());
        }
    }

    return Jwts.builder()
            .claims(claims)
            .subject(user.getUuid())        // sub claim
            .issuedAt(new Date())           // iat claim
            .expiration(new Date(...))      // exp claim
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
}
```

---

## 2. 서명 알고리즘

### 2.1 대칭키 vs 비대칭키

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    Symmetric vs Asymmetric Algorithms                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌───────────────────────────────────┐  ┌───────────────────────────────────┐│
│  │      대칭키 (HMAC)                 │  │      비대칭키 (RSA/EC)            ││
│  ├───────────────────────────────────┤  ├───────────────────────────────────┤│
│  │                                   │  │                                   ││
│  │  ┌─────────┐    ┌─────────┐      │  │  ┌─────────┐    ┌─────────┐      ││
│  │  │  서명   │    │  검증   │      │  │  │  서명   │    │  검증   │      ││
│  │  └────┬────┘    └────┬────┘      │  │  └────┬────┘    └────┬────┘      ││
│  │       │              │           │  │       │              │           ││
│  │       ▼              ▼           │  │       ▼              ▼           ││
│  │  ┌─────────────────────────┐     │  │  ┌──────────┐  ┌──────────┐     ││
│  │  │     동일한 Secret Key   │     │  │  │Private Key│  │Public Key│     ││
│  │  │        (공유)           │     │  │  │  (비밀)   │  │  (공개)  │     ││
│  │  └─────────────────────────┘     │  │  └──────────┘  └──────────┘     ││
│  │                                   │  │                                   ││
│  │  장점:                            │  │  장점:                            ││
│  │  - 빠른 성능                      │  │  - 키 노출 위험 감소               ││
│  │  - 간단한 구현                    │  │  - 서명자 확인 가능                ││
│  │                                   │  │  - JWKS로 키 배포 용이            ││
│  │  단점:                            │  │                                   ││
│  │  - 키 공유 필요                   │  │  단점:                            ││
│  │  - 서비스 증가 시 키 관리 복잡    │  │  - 상대적으로 느림                 ││
│  │                                   │  │  - 구현 복잡                      ││
│  └───────────────────────────────────┘  └───────────────────────────────────┘│
│                                                                              │
│  Portal Universe 선택: HMAC-SHA256 (HS256)                                   │
│  이유: 단일 인증 서비스, 내부 통신, 간단한 키 관리                            │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 알고리즘별 비교

| 알고리즘 | 타입 | 키 크기 | 성능 | 사용 사례 |
|---------|------|--------|------|----------|
| **HS256** | HMAC | 256-bit | 매우 빠름 | 단일 서비스, 내부 통신 |
| **HS384** | HMAC | 384-bit | 빠름 | 높은 보안 요구 |
| **HS512** | HMAC | 512-bit | 빠름 | 최고 보안 레벨 |
| **RS256** | RSA | 2048-bit+ | 느림 | 외부 공개 API, OIDC |
| **RS384** | RSA | 3072-bit+ | 느림 | 높은 보안 요구 |
| **RS512** | RSA | 4096-bit+ | 매우 느림 | 최고 보안 레벨 |
| **ES256** | ECDSA | P-256 | 중간 | 모바일, IoT |
| **ES384** | ECDSA | P-384 | 중간 | 높은 보안 요구 |
| **ES512** | ECDSA | P-521 | 중간 | 최고 보안 레벨 |

### 2.3 Portal Universe HMAC 구현

```java
// TokenService.java
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtConfig jwtConfig;

    /**
     * Secret Key를 기반으로 서명용 키를 생성합니다.
     * 최소 256비트(32바이트) 이상이어야 합니다.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 토큰 생성 시
    return Jwts.builder()
            .signWith(getSigningKey(), Jwts.SIG.HS256)  // HMAC-SHA256
            .compact();

    // 토큰 검증 시
    return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
}
```

---

## 3. Access Token vs Refresh Token

### 3.1 토큰 전략

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                   Access Token / Refresh Token Strategy                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Timeline                                                                    │
│  ────────────────────────────────────────────────────────────────────────▶  │
│                                                                              │
│  │◀──────────── Access Token (15분) ────────────▶│                          │
│  │                                               │                          │
│  │                                               │◀── Refresh ──▶│          │
│  │                                               │                │          │
│  │                                               │◀── New Access ─│──────▶│  │
│  │                                               │    Token (15분) │       │  │
│  │                                               │                │       │  │
│  │◀────────────────────────────────────────────────────────────────────────│ │
│  │                      Refresh Token (7일)                                │  │
│  │                                                                         │  │
│                                                                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────┬─────────────────────────────────────────────┐   │
│  │                        │             비교                            │   │
│  │      속성              ├──────────────────┬──────────────────────────┤   │
│  │                        │   Access Token   │     Refresh Token        │   │
│  ├────────────────────────┼──────────────────┼──────────────────────────┤   │
│  │ 목적                   │ API 접근 권한     │ Access Token 갱신        │   │
│  │ 만료 시간              │ 15분 (900초)      │ 7일 (604800초)           │   │
│  │ 저장 위치 (클라이언트)  │ Memory           │ 전송 후 즉시 삭제         │   │
│  │ 저장 위치 (서버)        │ 없음 (Stateless) │ Redis                    │   │
│  │ 포함 정보              │ Claims (풍부)     │ Subject만                │   │
│  │ 사용 빈도              │ 모든 API 요청     │ 만료 시 1회              │   │
│  │ 탈취 시 위험도         │ 높음 (짧은 수명)  │ 매우 높음 (긴 수명)      │   │
│  └────────────────────────┴──────────────────┴──────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 토큰 생성 구현

```java
// TokenService.java

/**
 * Access Token 생성
 * - 짧은 만료 시간 (15분)
 * - 풍부한 Claims 포함
 */
public String generateAccessToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("roles", user.getRole().getKey());
    claims.put("email", user.getEmail());

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
            .subject(user.getUuid())
            .issuedAt(now)
            .expiration(expiration)
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
}

/**
 * Refresh Token 생성
 * - 긴 만료 시간 (7일)
 * - 최소한의 정보만 포함 (Subject만)
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
```

### 3.3 Refresh Token Redis 저장

```java
// RefreshTokenService.java
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtConfig jwtConfig;
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * Refresh Token을 Redis에 저장
     * Key: refresh_token:{userId}
     * Value: {token}
     * TTL: 7일
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
     * 저장된 Refresh Token과 비교하여 검증
     * - Token Replay Attack 방지
     * - 다른 기기에서의 로그인 감지
     */
    public boolean validateRefreshToken(String userId, String token) {
        String storedToken = getRefreshToken(userId);
        if (storedToken == null) {
            return false;
        }
        return storedToken.equals(token);
    }
}
```

---

## 4. Token Blacklist (로그아웃)

### 4.1 왜 Blacklist가 필요한가?

JWT는 **Stateless**하므로 발급된 토큰을 서버에서 무효화할 수 없습니다.
로그아웃 시 토큰을 **Blacklist에 등록**하여 이를 해결합니다.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         Token Blacklist Flow                                  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   로그아웃 요청                                                               │
│       │                                                                      │
│       ▼                                                                      │
│   ┌───────────────────────────────────────────────────────────────┐         │
│   │  1. Access Token에서 남은 만료 시간 계산                       │         │
│   │     long remaining = expiration.getTime() - now.getTime();    │         │
│   └───────────────────────────────────────────────────────────────┘         │
│       │                                                                      │
│       ▼                                                                      │
│   ┌───────────────────────────────────────────────────────────────┐         │
│   │  2. Redis Blacklist에 등록                                     │         │
│   │     Key: blacklist:{token}                                    │         │
│   │     Value: "blacklisted"                                       │         │
│   │     TTL: 남은 만료 시간                                         │         │
│   └───────────────────────────────────────────────────────────────┘         │
│       │                                                                      │
│       ▼                                                                      │
│   ┌───────────────────────────────────────────────────────────────┐         │
│   │  3. Refresh Token 삭제                                         │         │
│   │     DEL refresh_token:{userId}                                │         │
│   └───────────────────────────────────────────────────────────────┘         │
│                                                                              │
│                                                                              │
│   API 요청 시 검증                                                           │
│       │                                                                      │
│       ▼                                                                      │
│   ┌───────────────────────────────────────────────────────────────┐         │
│   │  if (tokenBlacklistService.isBlacklisted(token)) {            │         │
│   │      // 401 Unauthorized                                       │         │
│   │  }                                                             │         │
│   └───────────────────────────────────────────────────────────────┘         │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Blacklist 구현

```java
// TokenBlacklistService.java
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Access Token을 블랙리스트에 추가
     * TTL은 토큰의 남은 만료 시간으로 설정
     * (만료 후에는 자연스럽게 무효화되므로 삭제)
     */
    public void addToBlacklist(String token, long remainingExpiration) {
        if (remainingExpiration <= 0) {
            return;  // 이미 만료된 토큰은 블랙리스트 불필요
        }

        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(
                key,
                "blacklisted",
                remainingExpiration,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
}
```

---

## 5. 토큰 검증 에러 처리

### 5.1 예외 타입별 처리

```java
// TokenService.java
public Claims validateAccessToken(String token) {
    try {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    } catch (ExpiredJwtException e) {
        // 토큰 만료 - 프론트엔드에서 Refresh 필요
        log.warn("Access token expired: {}", e.getMessage());
        throw e;
    } catch (UnsupportedJwtException e) {
        // 지원하지 않는 JWT 형식
        log.warn("Unsupported JWT token: {}", e.getMessage());
        throw e;
    } catch (MalformedJwtException e) {
        // JWT 형식 오류
        log.warn("Malformed JWT token: {}", e.getMessage());
        throw e;
    } catch (SecurityException e) {
        // 서명 검증 실패
        log.warn("Invalid JWT signature: {}", e.getMessage());
        throw e;
    } catch (IllegalArgumentException e) {
        // 빈 토큰
        log.warn("JWT claims string is empty: {}", e.getMessage());
        throw e;
    }
}
```

### 5.2 프론트엔드 에러 처리

```typescript
// api/interceptors.ts
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const errorHeader = error.response.headers['x-auth-error'];

      if (errorHeader === 'Token expired') {
        // Access Token 만료 - Refresh 시도
        try {
          const newToken = await refreshAccessToken();
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return api(originalRequest);
        } catch (refreshError) {
          // Refresh도 실패 - 로그인 페이지로 이동
          logout();
          return Promise.reject(refreshError);
        }
      } else {
        // 다른 인증 오류 - 로그인 페이지로 이동
        logout();
      }
    }

    return Promise.reject(error);
  }
);
```

---

## 6. 보안 Best Practices

### 6.1 Secret Key 관리

```yaml
# application.yml
jwt:
  # 환경 변수로 주입 (최소 32 바이트)
  secret-key: ${JWT_SECRET_KEY:your-256-bit-secret-key-for-jwt-signing-minimum-32-characters-required}

# 프로덕션에서는 Kubernetes Secret 또는 Vault 사용
# kubectl create secret generic jwt-secret --from-literal=JWT_SECRET_KEY=$(openssl rand -base64 32)
```

### 6.2 토큰 전송

```http
# Authorization 헤더 사용 (권장)
GET /api/blog/posts HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...

# Cookie는 CSRF 공격에 취약하므로 비권장
# URL 파라미터는 로그에 노출되므로 비권장
```

### 6.3 Payload 민감정보

```java
// 하지 말아야 할 것
claims.put("password", user.getPassword());  // 절대 금지!
claims.put("creditCard", user.getCreditCard());  // 금지!

// 권장하는 것
claims.put("sub", user.getUuid());       // ID만 포함
claims.put("roles", user.getRoles());    // 필요한 권한만
```

---

## 7. 참고 자료

- [RFC 7519 - JSON Web Token](https://tools.ietf.org/html/rfc7519)
- [RFC 7515 - JSON Web Signature](https://tools.ietf.org/html/rfc7515)
- [jwt.io](https://jwt.io) - JWT 디버거
- [JJWT Library](https://github.com/jwtk/jjwt) - Portal Universe에서 사용

## 8. 다음 단계

1. [Spring Security Architecture](./spring-security-architecture.md)
2. [API Gateway Security](./api-gateway-security.md)
3. [Portal Universe Auth Flow](./portal-universe-auth-flow.md)
