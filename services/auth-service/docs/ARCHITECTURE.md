# Auth Service 아키텍처

## 시스템 개요

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Frontend       │───▶│   API Gateway    │───▶│  Auth Service   │
│  (Portal Shell) │    │   (JWT 검증)     │    │  (OAuth2 Server)│
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                       │
                              ┌────────────────────────┼────────────────┐
                              ▼                        ▼                ▼
                       ┌──────────┐            ┌──────────┐      ┌───────────┐
                       │  MySQL   │            │  Kafka   │      │  Google   │
                       │ (사용자) │            │ (이벤트) │      │  OAuth2   │
                       └──────────┘            └──────────┘      └───────────┘
```

## OAuth2 Authorization Code Flow with PKCE

```
1. Frontend → /oauth2/authorize (+ code_challenge)
2. Auth Service → 로그인 페이지 리다이렉트
3. User → 로그인 정보 입력
4. Auth Service → redirect_uri + authorization_code
5. Frontend → /oauth2/token (+ code_verifier)
6. Auth Service → JWT (access_token, refresh_token)
```

### PKCE 보안
- **목적**: Authorization Code 탈취 방지
- **code_challenge**: code_verifier의 SHA-256 해시
- **검증**: 토큰 요청 시 code_verifier 확인

## JWT 구조

### Access Token (2분)
```json
{
  "sub": "user-uuid",
  "aud": "portal-client",
  "scope": "openid profile read write",
  "iss": "http://localhost:8081",
  "exp": 1737000120,
  "roles": ["ROLE_USER"]
}
```

### Refresh Token (7일)
- Access Token 갱신용
- Stateless 구현 (DB 저장 없음)

## 데이터 모델

### User
```java
@Entity
public class User {
    private Long id;
    private String uuid;       // 외부 공개 ID
    private String email;      // 로그인 ID
    private String password;   // BCrypt
    private Role role;         // ROLE_USER, ROLE_ADMIN
    private UserStatus status; // ACTIVE, DORMANT, DELETED
}
```

### UserProfile (1:1)
```java
@Entity
public class UserProfile {
    private String nickname;
    private String realName;
    private String profileImage;
    private Boolean marketingAgree;
}
```

### SocialAccount (1:N)
```java
@Entity
public class SocialAccount {
    private SocialProvider provider; // GOOGLE, NAVER, KAKAO
    private String socialId;
    private String email;
}
```

## 보안 설정

### Filter Chain 우선순위

1. **Authorization Server** (@Order(1))
   - `/oauth2/**`, `/.well-known/**`, `/oauth2/jwks`

2. **Default Security** (@Order(2))
   - `POST /api/users/signup` → permitAll
   - `/login`, `/logout` → permitAll
   - `/api/admin/**` → hasRole("ADMIN")
   - `/**` → authenticated

### 암호화
- 비밀번호: BCrypt (strength=10)
- JWT 서명: RS256 (RSA 2048-bit)

## Kafka 이벤트

### user-signup 토픽
```json
{
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "nickname": "john_doe"
}
```

**구독자**: notification-service (환영 이메일 발송)

## 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| A001 | 409 | 이메일 중복 |

## 설정 클래스

| 클래스 | 역할 |
|--------|------|
| `SecurityConfig` | 보안 필터 체인 |
| `AuthorizationServerConfig` | OAuth2 서버 설정 |
| `OAuth2ClientProperties` | 외부 설정 매핑 |
| `DataInitializer` | 테스트 데이터 초기화 |

## 확장 고려사항

- **분산 환경**: RegisteredClient를 DB 저장
- **토큰 블랙리스트**: Redis 활용
- **Rate Limiting**: 로그인 시도 제한
