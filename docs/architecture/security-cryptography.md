# Portal Universe - 암호화 및 보안 전략 아키텍처

## 1. 개요

본 문서는 Portal Universe의 **보안 및 암호화 전략**을 다룹니다.
OAuth2 인증 서버를 기반으로 JWT 토큰 보안(RS256), 비밀번호 보안(BCrypt), PKCE를 통한 Public Client 보호, RBAC 기반 권한 제어를 구현합니다.

---

## 2. 암호화 전략 개요

Portal Universe는 다층 보안 전략을 채택하여 인증(Authentication)과 인가(Authorization)를 안전하게 처리합니다.

| 보안 영역 | 기술 스택 | 적용 범위 |
|:---:|:---:|:---|
| **JWT 토큰 보안** | RS256 (비대칭 암호화) | Access Token, ID Token 서명 |
| **비밀번호 보안** | BCrypt (해싱 알고리즘) | 사용자 비밀번호 저장 |
| **Public Client 보호** | PKCE (RFC 7636) | SPA, 모바일 앱 인증 흐름 |
| **권한 제어** | RBAC (Role-Based Access Control) | API 엔드포인트 접근 제어 |

---

## 3. JWT 토큰 보안 (RS256)

### 3.1 RS256 비대칭 암호화

Portal Universe는 **RS256 (RSA Signature with SHA-256)** 알고리즘을 사용하여 JWT 토큰을 서명합니다.

#### 특징
- **비대칭 키 쌍**: Private Key로 서명, Public Key로 검증
- **토큰 위변조 방지**: Private Key 없이는 유효한 토큰 생성 불가
- **분산 검증**: Resource Server들이 Public Key만으로 독립적으로 토큰 검증 가능

#### 설정 위치
```java
// AuthorizationServerConfig.java
.tokenSettings(TokenSettings.builder()
    .idTokenSignatureAlgorithm(SignatureAlgorithm.RS256)
    .accessTokenTimeToLive(Duration.ofMinutes(2))
    .refreshTokenTimeToLive(Duration.ofDays(7))
    .reuseRefreshTokens(false)
    .build()
)
```

### 3.2 토큰 구조

#### JWT 구성 요소
```
[Header].[Payload].[Signature]
```

| 구성 요소 | 설명 | 예시 |
|:---:|:---|:---|
| **Header** | 알고리즘 및 토큰 타입 | `{"alg":"RS256","typ":"JWT"}` |
| **Payload** | 클레임(사용자 정보, 권한 등) | `{"sub":"user-uuid","roles":["ROLE_USER"],"exp":1234567890}` |
| **Signature** | Private Key로 서명된 값 | `RSA_SHA256(base64(header).base64(payload), privateKey)` |

#### 토큰 종류 및 수명

| 토큰 종류 | 용도 | 유효 기간 | 재사용 여부 |
|:---:|:---|:---:|:---:|
| **Access Token** | API 요청 인증 | 2분 | - |
| **Refresh Token** | Access Token 갱신 | 7일 | ❌ (Rotation) |
| **ID Token** | 사용자 식별 정보 | Access Token과 동일 | - |

### 3.3 JWT 클레임 커스터마이징

Access Token에 사용자 권한(roles)을 포함시켜 Resource Server에서 즉시 권한 검증이 가능합니다.

```java
// AuthorizationServerConfig.java
@Bean
public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
    return context -> {
        if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {
            Authentication principal = context.getPrincipal();
            Set<String> authorities = principal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            // 토큰에 'roles' 클레임 추가
            context.getClaims().claim("roles", authorities);
        }
    };
}
```

#### Access Token Payload 예시
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "iss": "http://localhost:8081",
  "roles": ["ROLE_USER"],
  "exp": 1705738800,
  "iat": 1705738680
}
```

---

## 4. 비밀번호 보안 (BCrypt)

### 4.1 BCrypt 해싱 알고리즘

사용자 비밀번호는 **BCrypt** 해싱 알고리즘을 통해 암호화되어 저장됩니다.

#### BCrypt 특징
- **단방향 해시**: 복호화 불가능, 오직 비교만 가능
- **Salt 자동 생성**: Rainbow Table 공격 방어
- **Adaptive Cost Factor**: 연산 비용 조절 가능 (기본값: 10)
- **느린 해싱**: Brute Force 공격 방어

#### 설정 위치
```java
// SecurityConfig.java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### 4.2 비밀번호 처리 흐름

```mermaid
sequenceDiagram
    participant U as User
    participant A as Auth Service
    participant DB as Database

    rect rgb(200, 230, 255)
        Note over U,DB: 회원가입 (비밀번호 저장)
        U->>A: POST /api/users/signup<br/>{ password: "myPassword123" }
        A->>A: BCrypt.hash("myPassword123")<br/>→ "$2a$10$..."
        A->>DB: INSERT users<br/>(password_hash: "$2a$10$...")
        DB-->>A: Success
        A-->>U: 201 Created
    end

    rect rgb(255, 230, 200)
        Note over U,DB: 로그인 (비밀번호 검증)
        U->>A: POST /login<br/>{ email, password }
        A->>DB: SELECT password_hash<br/>FROM users WHERE email = ?
        DB-->>A: "$2a$10$..."
        A->>A: BCrypt.matches(password, hash)<br/>→ true/false
        alt 검증 성공
            A-->>U: 302 Redirect (OAuth2 Authorization Code)
        else 검증 실패
            A-->>U: 401 Unauthorized
        end
    end
```

### 4.3 BCrypt 해시 포맷

```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
│  │  │                                                  │
│  │  └─ Salt (22자)                          └─ Hash (31자)
│  └─ Cost Factor (10 = 2^10 rounds)
└─ Algorithm Version (2a = BCrypt)
```

---

## 5. PKCE (Public Client 보호)

### 5.1 PKCE 개요

**PKCE (Proof Key for Code Exchange, RFC 7636)**는 Authorization Code 탈취 공격을 방지하기 위한 보안 확장입니다.

#### 적용 대상
- SPA (Single Page Application)
- 모바일 앱
- Client Secret을 안전하게 저장할 수 없는 Public Client

#### 설정 위치
```java
// AuthorizationServerConfig.java
.clientSettings(ClientSettings.builder()
    .requireProofKey(true)  // PKCE 강제
    .requireAuthorizationConsent(false)
    .build()
)
```

### 5.2 PKCE 인증 흐름

```mermaid
sequenceDiagram
    participant C as Client (SPA)
    participant A as Auth Server
    participant RS as Resource Server

    rect rgb(230, 255, 230)
        Note over C,A: Step 1: Authorization Request
        C->>C: 1. code_verifier 생성 (Random 43-128자)
        C->>C: 2. code_challenge = SHA256(code_verifier)
        C->>A: GET /oauth2/authorize<br/>?client_id=portal-client<br/>&code_challenge={hash}<br/>&code_challenge_method=S256
        A-->>C: 302 Redirect to Login
    end

    rect rgb(255, 240, 230)
        Note over C,A: Step 2: User Login & Authorization
        C->>A: POST /login (email, password)
        A->>A: BCrypt 검증
        A-->>C: 302 Redirect with Authorization Code
    end

    rect rgb(230, 240, 255)
        Note over C,A: Step 3: Token Exchange
        C->>A: POST /oauth2/token<br/>?code={code}<br/>&code_verifier={verifier}
        A->>A: Verify: SHA256(verifier) == stored challenge
        alt 검증 성공
            A-->>C: Access Token + Refresh Token
        else 검증 실패
            A-->>C: 400 Invalid Grant
        end
    end

    rect rgb(255, 230, 255)
        Note over C,RS: Step 4: API Access
        C->>RS: GET /api/posts<br/>Authorization: Bearer {token}
        RS->>RS: JWT 서명 검증 (RS256)
        RS-->>C: 200 OK + Data
    end
```

### 5.3 PKCE vs Client Secret

| 항목 | Client Secret | PKCE |
|:---:|:---|:---|
| **저장 위치** | 서버 사이드 (안전) | 클라이언트 사이드 (노출 가능) |
| **보안 방식** | 고정된 Secret | 요청마다 다른 Code Verifier |
| **적용 대상** | Confidential Client | Public Client (SPA, Mobile) |
| **탈취 시 위험** | 모든 클라이언트 위험 | 해당 요청만 위험 |

---

## 6. RBAC (역할 기반 접근 제어)

### 6.1 역할 정의

Portal Universe는 **RBAC (Role-Based Access Control)** 모델을 사용하여 사용자 권한을 관리합니다.

```java
// Role.java
public enum Role {
    USER("ROLE_USER"),    // 일반 사용자
    ADMIN("ROLE_ADMIN");  // 관리자

    private final String key;
}
```

### 6.2 권한 검증 방법

#### 방법 1: SecurityFilterChain에서 URL 패턴 기반 검증
```java
// SecurityConfig.java
.authorizeHttpRequests(authorize -> authorize
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")
    .anyRequest().authenticated()
)
```

#### 방법 2: @PreAuthorize 어노테이션 사용 (메서드 레벨)
```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable String id) {
    postService.deletePost(id);
    return ResponseEntity.ok(ApiResponse.success());
}
```

### 6.3 JWT 기반 권한 검증 흐름

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant RS as Resource Server
    participant AS as Auth Server

    rect rgb(230, 255, 230)
        Note over C,AS: Step 1: 로그인 및 토큰 발급
        C->>AS: OAuth2 Login
        AS->>AS: User 조회 → roles: ["ROLE_USER"]
        AS-->>C: Access Token (roles 클레임 포함)
    end

    rect rgb(255, 240, 230)
        Note over C,RS: Step 2: API 요청
        C->>G: DELETE /api/posts/123<br/>Authorization: Bearer {token}
        G->>RS: Forward Request
    end

    rect rgb(230, 240, 255)
        Note over RS: Step 3: JWT 검증 및 권한 확인
        RS->>RS: 1. JWT 서명 검증 (RS256 Public Key)
        RS->>RS: 2. 토큰 만료 확인 (exp 클레임)
        RS->>RS: 3. roles 클레임 추출: ["ROLE_USER"]
        RS->>RS: 4. @PreAuthorize("hasRole('ADMIN')") 검증
        alt ADMIN 권한 없음
            RS-->>C: 403 Forbidden
        else ADMIN 권한 있음
            RS->>RS: 비즈니스 로직 실행
            RS-->>C: 200 OK
        end
    end
```

### 6.4 역할별 접근 권한 예시

| 엔드포인트 | USER | ADMIN | 비고 |
|:---|:---:|:---:|:---|
| `GET /api/posts` | ✅ | ✅ | 게시글 목록 조회 |
| `POST /api/posts` | ✅ | ✅ | 게시글 작성 |
| `PUT /api/posts/{id}` | ✅ (본인만) | ✅ | 게시글 수정 |
| `DELETE /api/posts/{id}` | ❌ | ✅ | 게시글 삭제 (관리자만) |
| `GET /api/admin/users` | ❌ | ✅ | 사용자 관리 |

---

## 7. 보안 흐름 종합 다이어그램

### 7.1 전체 인증/인가 흐름

```mermaid
graph TB
    subgraph "1. 인증 (Authentication)"
        A[User Login] --> B{로그인 방식}
        B -->|Form Login| C[BCrypt 비밀번호 검증]
        B -->|OAuth2 Social| D[Google/Naver 인증]
        C --> E[Session 생성]
        D --> E
    end

    subgraph "2. OAuth2 Authorization Code 흐름 (PKCE)"
        E --> F[GET /oauth2/authorize<br/>+ code_challenge]
        F --> G[User Consent Screen<br/>생략 설정됨]
        G --> H[Redirect with Authorization Code]
        H --> I[POST /oauth2/token<br/>+ code_verifier]
        I --> J{PKCE 검증}
        J -->|실패| K[400 Bad Request]
        J -->|성공| L[Access Token 발급<br/>RS256 서명]
    end

    subgraph "3. 인가 (Authorization)"
        L --> M[Client: API 요청<br/>+ Bearer Token]
        M --> N[Resource Server:<br/>JWT 서명 검증]
        N --> O{토큰 유효성}
        O -->|만료/위조| P[401 Unauthorized]
        O -->|유효| Q[roles 클레임 추출]
        Q --> R{권한 확인<br/>RBAC}
        R -->|권한 없음| S[403 Forbidden]
        R -->|권한 있음| T[비즈니스 로직 실행]
        T --> U[200 OK + Response]
    end

    style C fill:#ffe6e6
    style L fill:#e6f7ff
    style R fill:#f0ffe6
```

### 7.2 토큰 갱신 흐름 (Refresh Token Rotation)

```mermaid
sequenceDiagram
    participant C as Client
    participant AS as Auth Server

    rect rgb(255, 230, 230)
        Note over C,AS: Access Token 만료
        C->>C: API 요청 시 401 Unauthorized 수신
    end

    rect rgb(230, 255, 230)
        Note over C,AS: Refresh Token으로 재발급
        C->>AS: POST /oauth2/token<br/>grant_type=refresh_token<br/>refresh_token={token}
        AS->>AS: 1. Refresh Token 검증
        AS->>AS: 2. 만료 여부 확인 (7일)
        AS->>AS: 3. 사용 이력 확인 (재사용 방지)
        alt 검증 성공
            AS->>AS: 4. 새 Access Token 발급
            AS->>AS: 5. 새 Refresh Token 발급<br/>(기존 토큰 무효화)
            AS-->>C: 200 OK<br/>{ access_token, refresh_token }
        else 검증 실패
            AS-->>C: 401 Unauthorized<br/>(재로그인 필요)
        end
    end
```

---

## 8. 보안 설정 위치 요약

| 보안 요소 | 설정 파일 | 핵심 설정 |
|:---:|:---|:---|
| **RS256 서명** | `AuthorizationServerConfig.java` | `.idTokenSignatureAlgorithm(SignatureAlgorithm.RS256)` |
| **PKCE 강제** | `AuthorizationServerConfig.java` | `.requireProofKey(true)` |
| **BCrypt** | `SecurityConfig.java` | `new BCryptPasswordEncoder()` |
| **RBAC** | `SecurityConfig.java`, Controller | `.hasRole("ADMIN")`, `@PreAuthorize` |
| **토큰 수명** | `AuthorizationServerConfig.java` | `accessTokenTimeToLive(Duration.ofMinutes(2))` |

---

## 9. 보안 권장 사항

### 9.1 운영 환경 체크리스트

- [ ] HTTPS 강제 적용 (HTTP → HTTPS 자동 리다이렉트)
- [ ] JWT Private Key 안전한 저장 (Vault, KMS 사용)
- [ ] CORS 정책 최소 권한 원칙 적용
- [ ] Rate Limiting 적용 (로그인 시도, API 요청)
- [ ] Access Token 수명 최소화 (현재: 2분)
- [ ] Refresh Token Rotation 활성화 (현재: ✅)

### 9.2 모니터링 항목

- 실패한 로그인 시도 횟수 추적
- 비정상적인 토큰 갱신 패턴 감지
- JWT 검증 실패 로그 수집
- 권한 없는 API 접근 시도 기록

---

## 10. 참고 문서

- [OAuth2 인증 시스템 설계](./auth-system-design.md)
- [RFC 7636 - PKCE](https://datatracker.ietf.org/doc/html/rfc7636)
- [RFC 7519 - JWT](https://datatracker.ietf.org/doc/html/rfc7519)
- [Spring Authorization Server 공식 문서](https://docs.spring.io/spring-authorization-server/reference/index.html)
