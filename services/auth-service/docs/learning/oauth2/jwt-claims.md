# JWT Claims 설계

## 개요

JWT(JSON Web Token)의 payload에 담기는 claims는 토큰의 목적과 보안에 직접적인 영향을 미칩니다. Portal Universe auth-service는 최소한의 필수 정보만 claims에 포함하여 보안과 효율성을 균형있게 유지합니다.

## JWT 구조

```
┌─────────────────────────────────────────────────────────────┐
│                        JWT Token                             │
├───────────────┬───────────────────────┬─────────────────────┤
│    Header     │       Payload         │      Signature      │
│  (알고리즘)    │      (Claims)         │      (서명)         │
├───────────────┼───────────────────────┼─────────────────────┤
│ {             │ {                     │ HMACSHA256(         │
│   "alg":"HS256│   "sub": "uuid",      │   base64(header) +  │
│   "typ":"JWT" │   "roles": "ROLE_USER"│   base64(payload),  │
│ }             │   "email": "...",     │   secret            │
│               │   "exp": 1234567890   │ )                   │
│               │ }                     │                     │
└───────────────┴───────────────────────┴─────────────────────┘
```

## Claims 종류

### Registered Claims (표준)

| Claim | 이름 | 설명 | Portal Universe 사용 |
|-------|------|------|---------------------|
| `sub` | Subject | 토큰 주체 식별자 | User UUID |
| `iss` | Issuer | 토큰 발급자 | 미사용 (단일 서비스) |
| `aud` | Audience | 토큰 수신자 | 미사용 |
| `exp` | Expiration | 만료 시간 | 필수 사용 |
| `iat` | Issued At | 발급 시간 | 사용 |
| `nbf` | Not Before | 유효 시작 시간 | 미사용 |
| `jti` | JWT ID | 토큰 고유 ID | 미사용 |

### Custom Claims (Portal Universe)

| Claim | 타입 | 설명 | 예시 |
|-------|------|------|------|
| `roles` | String | 사용자 권한 | "ROLE_USER" |
| `email` | String | 이메일 주소 | "user@example.com" |
| `nickname` | String | 닉네임 | "홍길동" |
| `username` | String | 유저네임 (optional) | "hong_gildong" |

## 구현 코드

### Access Token Claims 생성

```java
public String generateAccessToken(User user) {
    Map<String, Object> claims = new HashMap<>();

    // 권한 정보 (필수)
    claims.put("roles", user.getRole().getKey());  // "ROLE_USER" or "ROLE_ADMIN"

    // 이메일 (사용자 식별 보조)
    claims.put("email", user.getEmail());

    // Profile 정보 (UI 표시용)
    if (user.getProfile() != null) {
        claims.put("nickname", user.getProfile().getNickname());

        // username은 설정된 경우에만 포함
        if (user.getProfile().getUsername() != null) {
            claims.put("username", user.getProfile().getUsername());
        }
    }

    Date now = new Date();
    Date expiration = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());

    return Jwts.builder()
            .claims(claims)                    // Custom claims
            .subject(user.getUuid())           // sub claim (주체)
            .issuedAt(now)                     // iat claim (발급시간)
            .expiration(expiration)            // exp claim (만료시간)
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
}
```

### Refresh Token Claims (최소화)

```java
public String generateRefreshToken(User user) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + jwtConfig.getRefreshTokenExpiration());

    // Refresh Token은 최소한의 정보만 포함
    return Jwts.builder()
            .subject(user.getUuid())  // sub만 포함
            .issuedAt(now)
            .expiration(expiration)
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
}
```

## Token Payload 예시

### Access Token Payload

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "roles": "ROLE_USER",
  "email": "user@example.com",
  "nickname": "홍길동",
  "username": "hong_gildong",
  "iat": 1704067200,
  "exp": 1704068100
}
```

### Refresh Token Payload

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "iat": 1704067200,
  "exp": 1704672000
}
```

## Claims 설계 원칙

### 1. 최소 정보 원칙 (Minimal Claims)

```
┌─────────────────────────────────────────────────────────────┐
│                    BAD: 과도한 정보 포함                      │
├─────────────────────────────────────────────────────────────┤
│ {                                                            │
│   "sub": "uuid",                                            │
│   "email": "user@example.com",                              │
│   "password": "...",          ← 절대 포함 금지               │
│   "phoneNumber": "010-...",   ← 민감 정보                    │
│   "address": "...",           ← 불필요한 정보                │
│   "creditCard": "..."         ← 극히 위험                    │
│ }                                                            │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   GOOD: 필수 정보만 포함                      │
├─────────────────────────────────────────────────────────────┤
│ {                                                            │
│   "sub": "uuid",              ← 식별자                       │
│   "roles": "ROLE_USER",       ← 권한 (필수)                  │
│   "email": "user@example.com",← 표시용 (선택)                │
│   "nickname": "홍길동"         ← UI용 (선택)                  │
│ }                                                            │
└─────────────────────────────────────────────────────────────┘
```

### 2. sub에 UUID 사용

```java
// GOOD: UUID 사용 (외부 노출에 안전)
.subject(user.getUuid())  // "550e8400-e29b-41d4-a716-446655440000"

// BAD: DB ID 사용 (추측 가능)
.subject(user.getId().toString())  // "12345"
```

UUID 사용 이유:
- 순차적이지 않아 추측 불가
- 다른 사용자 정보 접근 시도 방지
- 분산 시스템에서 충돌 없음

### 3. 권한 정보 포함

```java
// Spring Security 권한 체크에 직접 사용
claims.put("roles", user.getRole().getKey());  // "ROLE_USER" or "ROLE_ADMIN"
```

## Claims 검증 및 추출

### JwtAuthenticationFilter에서 Claims 사용

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                               HttpServletResponse response,
                               FilterChain filterChain) throws ServletException, IOException {

    String token = extractToken(request);

    if (token != null && !tokenBlacklistService.isBlacklisted(token)) {
        try {
            // JWT 토큰 검증 및 Claims 추출
            Claims claims = tokenService.validateAccessToken(token);

            // sub claim에서 userId 추출
            String userId = claims.getSubject();

            // roles claim에서 권한 추출
            String roles = claims.get("roles", String.class);

            // Spring Security Authentication 객체 생성
            List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(roles));

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // SecurityContext에 설정
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.debug("JWT authentication failed: {}", e.getMessage());
        }
    }

    filterChain.doFilter(request, response);
}
```

## Frontend에서 Claims 활용

```typescript
// JWT Decode 라이브러리 사용
import jwtDecode from 'jwt-decode';

interface JwtPayload {
  sub: string;
  roles: string;
  email: string;
  nickname: string;
  username?: string;
  exp: number;
  iat: number;
}

function getUserFromToken(token: string): JwtPayload {
  return jwtDecode<JwtPayload>(token);
}

// 사용 예시
const user = getUserFromToken(accessToken);
console.log(user.nickname);  // "홍길동"
console.log(user.roles);     // "ROLE_USER"

// 만료 체크
const isExpired = user.exp * 1000 < Date.now();
```

## Claims 확장 가이드

### 새로운 Claim 추가 시 고려사항

| 항목 | 체크 |
|------|------|
| 민감 정보가 아닌가? | 비밀번호, 카드 정보 등 제외 |
| 자주 변경되지 않는가? | 변경 시 토큰 재발급 필요 |
| 토큰 크기가 적절한가? | 매 요청마다 전송됨 |
| 필수 정보인가? | 불필요한 정보 제외 |

### 예시: 구독 상태 추가

```java
// 구독 정보가 필요한 경우
claims.put("subscription", user.getSubscriptionType());  // "FREE", "PREMIUM"

// 대신 권장: API 호출로 조회
// 자주 변경될 수 있는 정보는 토큰에 포함하지 않음
```

## 보안 고려사항

### 1. Claim 탈취 대비

```java
// Email은 로깅에 노출될 수 있음 - 마스킹 처리
String maskedEmail = email.replaceAll("(^[^@]{2})[^@]*", "$1***");
// user@example.com → us***@example.com
```

### 2. 토큰 크기 제한

```
HTTP Header 크기 제한 (일반적으로 8KB)
- Claims이 너무 많으면 헤더 초과 오류 발생
- 권장: 토큰 크기 1KB 이하 유지
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/TokenService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/security/JwtAuthenticationFilter.java`

## 참고 자료

- [JWT Claims RFC 7519](https://datatracker.ietf.org/doc/html/rfc7519#section-4)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
