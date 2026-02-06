# 🔑 JWT Key Rotation 학습

> JWT 서명 키를 주기적으로 교체하여 보안을 강화하는 기법

**난이도**: ⭐⭐⭐⭐ (고급)
**학습 시간**: 60분
**실습 시간**: 45분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] JWT 구조와 서명 검증 원리 이해하기
- [ ] Key Rotation이 필요한 이유 설명하기
- [ ] Kid 기반 멀티 키 관리 구현하기
- [ ] 무중단 키 교체 전략 수립하기

---

## 1️⃣ JWT 기초

### JWT란?

JSON Web Token의 약자로, 인증 정보를 안전하게 전달하는 표준

```
eyJraWQiOiJrZXktMjAyNi0wMSIsImFsZyI6IkhTMjU2In0.
eyJzdWIiOiJ1c2VyLTEyMyIsImV4cCI6MTczNzY0MDgwMH0.
oKKftUlpuB2Sjy9qlNRyVuggeaVdskgHvVdCxiqfK2k
```

### 구조 분해

```json
// 1. Header (Base64 인코딩)
{
  "kid": "key-2026-01",    // Key ID
  "alg": "HS256"            // 알고리즘
}

// 2. Payload (Base64 인코딩)
{
  "sub": "user-123",        // 사용자 ID
  "email": "test@example.com",
  "roles": "ROLE_USER",
  "exp": 1737640800         // 만료 시간
}

// 3. Signature (비밀키로 서명)
HMACSHA256(
  base64(header) + "." + base64(payload),
  secret_key
)
```

### 서명 검증 과정

```
1. JWT 수신
2. Header에서 kid 추출 → "key-2026-01"
3. kid로 secret_key 조회
4. Header + Payload를 secret_key로 서명
5. 계산한 서명 == JWT 서명 → ✓ 검증 성공
```

---

## 2️⃣ 왜 Key Rotation이 필요한가?

### 문제 상황

```
🔴 키 유출 시나리오
- 개발자 실수로 GitHub에 키 커밋
- 해킹으로 서버 접근
- 내부자 공격
- 로그 파일에 키 노출

결과:
→ 공격자가 임의의 JWT 생성 가능
→ 모든 사용자 계정 탈취 가능
→ 시스템 전체 장악
```

### 해결책: 주기적 키 교체

```
✅ Key Rotation 적용
- 3개월마다 키 교체
- 유출된 키의 유효기간 제한
- 피해 범위 최소화
- 침해 사고 대응 시간 확보
```

---

## 3️⃣ 단순 키 교체의 문제점

### ❌ 잘못된 방법

```yaml
# 1단계: 키 변경
jwt:
  secret-key: "old-key"  →  secret-key: "new-key"

# 결과
- 기존 JWT 모두 무효화 ❌
- 모든 사용자 강제 로그아웃
- 서비스 중단
```

### 문제점

1. **사용자 경험 저하**: 갑작스런 로그아웃
2. **운영 리스크**: 피크 시간 키 교체 불가
3. **롤백 어려움**: 문제 발생 시 복구 곤란

---

## 4️⃣ Kid 기반 멀티 키 관리

### 개념

여러 개의 키를 동시에 유지하고, `kid`로 구분

```yaml
jwt:
  current-key-id: key-2026-01      # 새 토큰 생성에 사용
  keys:
    key-2026-01:                    # 현재 키
      secret-key: "new-secret..."
      activated-at: 2026-01-01T00:00:00
    key-2025-12:                    # 이전 키 (검증용)
      secret-key: "old-secret..."
      activated-at: 2025-12-01T00:00:00
      expires-at: 2026-03-01T00:00:00  # 3개월 유예
```

### 동작 원리

```
┌─────────────────────────────────────────────┐
│ JWT 생성 (Auth Service)                     │
├─────────────────────────────────────────────┤
│ 1. current-key-id 조회 → "key-2026-01"     │
│ 2. 해당 키로 서명                           │
│ 3. Header에 kid 포함                        │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│ JWT 검증 (API Gateway)                      │
├─────────────────────────────────────────────┤
│ 1. Header에서 kid 추출                      │
│ 2. kid로 키 조회 (key-2026-01 또는 2025-12)│
│ 3. 해당 키로 서명 검증                      │
│ 4. 키 유효기간 확인                         │
└─────────────────────────────────────────────┘
```

---

## 5️⃣ 프로젝트 구현

### JwtProperties 설정

```java
// services/auth-service/src/main/java/.../config/JwtProperties.java

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String currentKeyId;                // 현재 사용할 키 ID
    private Map<String, KeyInfo> keys;          // 키 맵

    @Getter
    @Setter
    public static class KeyInfo {
        private String secretKey;               // 비밀키
        private LocalDateTime activatedAt;      // 활성화 시간
        private LocalDateTime expiresAt;        // 만료 시간 (선택)
    }

    // 현재 키 조회
    public KeyInfo getCurrentKey() {
        return keys.get(currentKeyId);
    }

    // kid로 키 조회
    public KeyInfo getKey(String kid) {
        KeyInfo key = keys.get(kid);
        if (key == null) {
            throw new IllegalArgumentException("JWT key not found: " + kid);
        }

        // 만료 확인
        if (key.getExpiresAt() != null &&
            LocalDateTime.now().isAfter(key.getExpiresAt())) {
            throw new IllegalArgumentException("JWT key expired: " + kid);
        }

        return key;
    }
}
```

### 토큰 생성 (Auth Service)

```java
// TokenService.java

public String generateAccessToken(User user) {
    JwtProperties.KeyInfo currentKey = jwtProperties.getCurrentKey();

    return Jwts.builder()
        .setHeaderParam("kid", jwtProperties.getCurrentKeyId())  // kid 추가
        .claim("email", user.getEmail())
        .claim("roles", user.getRoles())
        .setSubject(user.getId().toString())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
        .signWith(Keys.hmacShaKeyFor(currentKey.getSecretKey().getBytes()))
        .compact();
}
```

### 토큰 검증 (API Gateway)

```java
// JwtAuthenticationFilter.java

private Claims validateToken(String token) {
    try {
        // 1. Header에서 kid 추출 (서명 검증 전)
        String kid = Jwts.parserBuilder().build()
            .parseClaimsJwt(token.substring(0, token.lastIndexOf('.') + 1))
            .getHeader()
            .get("kid", String.class);

        // 2. kid로 키 조회
        JwtProperties.KeyInfo keyInfo = jwtProperties.getKey(kid);

        // 3. 해당 키로 서명 검증
        return Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(keyInfo.getSecretKey().getBytes()))
            .build()
            .parseClaimsJws(token)
            .getBody();

    } catch (ExpiredJwtException e) {
        throw new CustomBusinessException(CommonErrorCode.EXPIRED_TOKEN);
    } catch (JwtException e) {
        throw new CustomBusinessException(CommonErrorCode.INVALID_TOKEN);
    }
}
```

---

## 6️⃣ 무중단 키 교체 절차

### Phase 1: 새 키 추가 (Day 0)

```yaml
# application.yml 업데이트
jwt:
  current-key-id: key-2026-01  # 새 키로 변경
  keys:
    key-2026-01:                 # 새 키 추가
      secret-key: "${JWT_NEW_KEY}"
      activated-at: 2026-01-01T00:00:00
    key-2025-12:                 # 이전 키 유지
      secret-key: "${JWT_OLD_KEY}"
      activated-at: 2025-12-01T00:00:00
      expires-at: 2026-03-01T00:00:00  # 3개월 후 만료
```

**배포**:
1. Auth Service 배포 → 새 키로 JWT 생성
2. API Gateway 배포 → 두 키 모두 검증 가능

**결과**:
- 새 로그인: key-2026-01로 서명된 JWT
- 기존 사용자: key-2025-12로 서명된 JWT 계속 사용 가능 ✓

### Phase 2: 유예 기간 (Day 1 ~ 90)

```
Day 1:  새 JWT 10%, 구 JWT 90%
Day 30: 새 JWT 50%, 구 JWT 50%
Day 60: 새 JWT 80%, 구 JWT 20%
Day 90: 새 JWT 99%, 구 JWT 1%
```

**모니터링**:
```bash
# Grafana 대시보드
- kid별 JWT 검증 횟수
- key-2025-12 사용 빈도 추적
- 1% 이하 시 제거 가능
```

### Phase 3: 구 키 제거 (Day 90)

```yaml
jwt:
  current-key-id: key-2026-01
  keys:
    key-2026-01:                 # 새 키만 유지
      secret-key: "${JWT_NEW_KEY}"
      activated-at: 2026-01-01T00:00:00
    # key-2025-12 제거
```

---

## 7️⃣ 환경별 설정

### Local 환경

```yaml
# application-local.yml
jwt:
  current-key-id: key-default
  keys:
    key-default:
      secret-key: "your-local-secret-key-for-development"
      activated-at: 2026-01-01T00:00:00
```

### Kubernetes 환경

```yaml
# k8s/base/jwt-secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: jwt-secrets
type: Opaque
stringData:
  JWT_CURRENT_KEY_ID: "key-2026-01"
  JWT_KEY_2026_01_SECRET: "base64-encoded-secret"
  JWT_KEY_2026_01_ACTIVATED_AT: "2026-01-01T00:00:00"
```

---

## ✍️ 실습 과제

### 과제 1: JWT 디코딩 (기초)

브라우저 콘솔이나 [jwt.io](https://jwt.io)에서 JWT를 디코딩하세요.

```javascript
// 브라우저 개발자 도구 Console
const token = "eyJraWQiOiJrZXktMjAyNi0wMSIsImFsZyI6IkhTMjU2In0...";
const [header, payload, signature] = token.split('.');

console.log(JSON.parse(atob(header)));    // kid 확인
console.log(JSON.parse(atob(payload)));   // 사용자 정보 확인
```

**확인사항**:
- [ ] kid가 "key-2026-01"인가?
- [ ] exp(만료 시간)가 15분 후인가?
- [ ] 사용자 정보가 포함되어 있는가?

### 과제 2: 키 교체 시뮬레이션 (중급)

로컬 환경에서 키 교체를 시뮬레이션하세요.

```bash
# 1. 현재 키로 로그인
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Token 저장: OLD_TOKEN=eyJ...

# 2. application.yml에 새 키 추가
jwt:
  current-key-id: key-new
  keys:
    key-new:
      secret-key: "new-secret-key"
      activated-at: 2026-01-23T00:00:00
    key-default:
      secret-key: "your-local-secret-key"
      activated-at: 2026-01-01T00:00:00

# 3. 서비스 재시작

# 4. 구 토큰으로 요청 (성공해야 함)
curl http://localhost:8080/api/profile \
  -H "Authorization: Bearer $OLD_TOKEN"

# 5. 새로 로그인하여 새 토큰 획득
# Token 저장: NEW_TOKEN=eyJ...

# 6. 두 토큰 모두 작동 확인
```

### 과제 3: 키 만료 처리 (고급)

expires-at을 과거로 설정하고 에러 처리를 확인하세요.

```yaml
jwt:
  keys:
    key-expired:
      secret-key: "expired-key"
      activated-at: 2025-01-01T00:00:00
      expires-at: 2025-12-31T23:59:59  # 이미 만료됨
```

**예상 결과**:
```json
{
  "success": false,
  "error": {
    "code": "INVALID_TOKEN",
    "message": "JWT key expired: key-expired"
  }
}
```

---

## 🔍 더 알아보기

### RSA vs HMAC

| 항목 | HMAC (HS256) | RSA (RS256) |
|------|--------------|-------------|
| 키 타입 | 대칭키 | 비대칭키 (공개키/개인키) |
| 성능 | 빠름 | 느림 |
| 키 배포 | 어려움 | 쉬움 (공개키만 배포) |
| 사용 사례 | 단일 시스템 | 마이크로서비스 |

**우리 프로젝트**: Auth와 Gateway만 검증 → HMAC 선택

### JWE (JWT Encryption)

Payload 자체를 암호화하여 민감 정보 보호

```
JWT (JWS):  Header.Payload.Signature  (서명만)
JWE:        암호화된 전체 토큰         (암호화 + 서명)
```

### Refresh Token 전략

Access Token (15분) + Refresh Token (7일)

```
1. Access Token 만료
2. Refresh Token으로 재발급 요청
3. 새 Access Token + Refresh Token 발급
4. Refresh Token도 교체 (Refresh Token Rotation)
```

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] JWT 구조(Header, Payload, Signature)를 설명할 수 있다
- [ ] kid의 역할과 필요성을 이해한다
- [ ] 무중단 키 교체 절차를 수행할 수 있다
- [ ] 실제 프로젝트에서 키 교체를 시뮬레이션했다
- [ ] 키 만료 시 에러 처리를 확인했다

---

**이전**: [Rate Limiting](./01-rate-limiting.md)
**다음**: [Login Security 학습하기](./03-login-security.md) →
