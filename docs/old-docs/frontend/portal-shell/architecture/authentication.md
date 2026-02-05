---
id: arch-portal-shell-authentication
title: Authentication Architecture
type: architecture
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [architecture, authentication, oauth2, pkce, oidc, jwt]
related:
  - arch-portal-shell-system-overview
---

# Authentication Architecture

## 📋 개요

Portal Shell은 OAuth2 Authorization Code + PKCE Flow를 사용하여 사용자 인증을 처리합니다. Spring Authorization Server와 oidc-client-ts 라이브러리를 통해 표준 OIDC 프로토콜을 구현하며, Silent Renewal을 통해 사용자 경험을 최적화합니다.

---

## 🔐 인증 방식

### OAuth2 Authorization Code + PKCE

| 항목 | 내용 |
|------|------|
| **프로토콜** | OAuth 2.0 + OpenID Connect (OIDC) |
| **Flow** | Authorization Code + PKCE |
| **토큰 타입** | JWT (Access Token, Refresh Token) |
| **라이브러리** | oidc-client-ts |
| **Authorization Server** | Spring Authorization Server (Auth Service) |

---

## 🏗️ 인증 아키텍처

```mermaid
graph TB
    subgraph "Portal Shell"
        UI[LoginModal]
        AS[AuthService]
        UM[UserManager<br/>oidc-client-ts]
        STORE[Auth Store<br/>Pinia]

        UI --> AS
        AS --> UM
        UM --> STORE
    end

    subgraph "Browser Storage"
        LS[localStorage<br/>WebStorageStateStore]

        UM -.->|Save Token| LS
        UM -.->|Load Token| LS
    end

    subgraph "Backend"
        AUTH[Auth Service<br/>:8081]
        GW[API Gateway<br/>:8080]

        UM -->|OAuth2 PKCE| AUTH
        UI -->|API Calls| GW
        GW -->|JWT Verify| AUTH
    end

    subgraph "Silent Renewal"
        IFRAME[silent-renew.html<br/>iframe]
        UM -.->|Auto Renew| IFRAME
        IFRAME -.->|New Token| UM
    end

    classDef client fill:#e1f5ff,stroke:#0288d1
    classDef backend fill:#ffebee,stroke:#c62828
    classDef storage fill:#f3e5f5,stroke:#7b1fa2

    class UI,AS,UM,STORE client
    class AUTH,GW backend
    class LS,IFRAME storage
```

---

## 🔄 인증 흐름

### 1. 로그인 (Authorization Code + PKCE)

```mermaid
sequenceDiagram
    participant User
    participant PS as Portal Shell
    participant UM as UserManager
    participant AS as Auth Service
    participant Store as Auth Store

    User->>PS: "로그인" 버튼 클릭
    PS->>UM: signinRedirect()

    Note over UM: PKCE Code Verifier 생성<br/>Code Challenge 계산 (SHA256)

    UM->>AS: GET /oauth2/authorize<br/>?response_type=code<br/>&client_id=portal-client<br/>&redirect_uri=http://localhost:30000/callback<br/>&scope=openid profile<br/>&code_challenge=xxx<br/>&code_challenge_method=S256

    AS-->>User: 로그인 페이지 리다이렉트
    User->>AS: 아이디/비밀번호 입력
    AS->>AS: 인증 검증

    AS-->>UM: 302 Redirect<br/>?code=xxx

    Note over UM: Callback 페이지로 이동

    UM->>AS: POST /oauth2/token<br/>grant_type=authorization_code<br/>code=xxx<br/>code_verifier=xxx<br/>redirect_uri=http://localhost:30000/callback

    AS->>AS: Code Verifier 검증<br/>SHA256(verifier) == challenge

    AS-->>UM: {<br/>  access_token: "eyJhbGc...",<br/>  refresh_token: "xxx",<br/>  expires_in: 3600<br/>}

    UM->>UM: JWT 파싱 (parseJwtPayload)
    UM->>Store: setUser(user)
    Store-->>PS: 인증 완료
    PS-->>User: 홈페이지로 이동
```

---

### 2. Silent Renewal (자동 토큰 갱신)

```mermaid
sequenceDiagram
    participant UM as UserManager
    participant Timer as Access Token<br/>Expiring Timer
    participant Iframe as silent-renew.html
    participant AS as Auth Service
    participant Store as Auth Store

    Note over Timer: 만료 60초 전
    Timer->>UM: addAccessTokenExpiring()

    UM->>Iframe: iframe 생성<br/>src=/silent-renew.html?...

    Iframe->>AS: GET /oauth2/authorize<br/>?prompt=none<br/>&code_challenge=xxx

    Note over AS: 기존 세션 확인

    AS-->>Iframe: 302 Redirect<br/>?code=xxx

    Iframe->>Iframe: oidc-client-ts 로드<br/>signinSilentCallback()

    Iframe->>AS: POST /oauth2/token<br/>grant_type=authorization_code<br/>code=xxx

    AS-->>Iframe: { access_token, refresh_token }

    Iframe->>UM: CustomEvent 발송<br/>'oidc-silent-renew-message'

    UM->>UM: signinSilentCallback()
    UM->>Store: setUser(newUser)

    Note over UM: ✅ 토큰 갱신 완료
```

---

### 3. 토큰 검증 및 API 호출

```mermaid
sequenceDiagram
    participant Component
    participant API as apiClient
    participant LS as localStorage
    participant GW as API Gateway
    participant Service as Backend Service

    Component->>API: GET /api/v1/blog/posts

    API->>LS: getAccessToken()
    LS-->>API: "eyJhbGc..."

    API->>API: Axios Interceptor<br/>headers.Authorization = Bearer {token}

    API->>GW: GET /api/v1/blog/posts<br/>Authorization: Bearer eyJhbGc...

    GW->>GW: JWT 검증<br/>1. 서명 확인<br/>2. 만료 확인<br/>3. 권한 확인

    alt 토큰 유효
        GW->>Service: Forward Request
        Service-->>GW: Response
        GW-->>API: 200 OK
        API-->>Component: 데이터 반환
    else 토큰 만료
        GW-->>API: 401 Unauthorized
        API->>API: Axios Interceptor<br/>에러 처리
        API->>Component: 로그인 모달 표시
    end
```

---

## 🛠️ AuthService 구현

### 클래스 구조

Portal Shell의 `authService.ts`는 객체지향 설계 원칙(SOLID)을 따릅니다.

```mermaid
classDiagram
    class AuthenticationService {
        -userManager: UserManager
        -tokenValidator: TokenValidator
        -renewalState: TokenRenewalState
        +login()
        +logout()
        +getUser()
        +isTokenValid()
    }

    class TokenRenewalState {
        -lastRenewalTime: number
        -isRenewingInProgress: boolean
        -isLoggingOut: boolean
        +startRenewal()
        +completeRenewal()
        +isRenewing()
        +startLogout()
    }

    class TokenValidator {
        -userManager: UserManager
        +isValid()
        +logTokenInfo()
    }

    class UserLoadedHandler {
        -lastLoadTime: number
        +handle(user, onTokenRenewed)
    }

    class AccessTokenExpiringHandler {
        +handle(onRenewalStarted)
    }

    class AccessTokenExpiredHandler {
        -lastLogoutAttemptTime: number
        -logoutDebounceMs: number
        +handle(tokenValidator, renewalState, onLogout)
    }

    AuthenticationService --> TokenRenewalState
    AuthenticationService --> TokenValidator
    AuthenticationService --> UserLoadedHandler
    AuthenticationService --> AccessTokenExpiringHandler
    AuthenticationService --> AccessTokenExpiredHandler
```

---

### 핵심 이벤트 핸들러

#### 1. UserLoaded

```typescript
userManager.events.addUserLoaded((user: User) => {
  console.log('✅ User loaded');

  // Pinia Store에 사용자 정보 저장
  const authStore = useAuthStore();
  authStore.setUser(user);

  // 토큰 갱신 상태 완료
  renewalState.completeRenewal();
});
```

#### 2. AccessTokenExpiring

```typescript
userManager.events.addAccessTokenExpiring(() => {
  console.log('⏰ Token expiring soon, auto-renewing...');

  // Silent Renewal 시작
  renewalState.startRenewal();
});
```

#### 3. AccessTokenExpired

```typescript
userManager.events.addAccessTokenExpired(async () => {
  console.log('❌ Access Token Expired');

  // 1. 토큰 유효성 재확인 (Silent Renewal 성공 여부)
  const isValid = await tokenValidator.isValid();
  if (isValid) {
    console.log('✅ Token was renewed, staying logged in');
    return;
  }

  // 2. Debounce: 3초 이내 중복 로그아웃 방지
  if (!renewalState.startLogout()) {
    return;
  }

  // 3. 로그아웃 처리
  await userManager.removeUser();
  authStore.logout();
});
```

#### 4. SilentRenewError

```typescript
userManager.events.addSilentRenewError((error) => {
  console.error('❌ Silent renew failed:', error.message);

  // 에러 분류 및 처리
  if (isNetworkError(error)) {
    console.log('📡 Network error - will retry');
  } else if (isAuthError(error)) {
    console.log('🚨 Authorization error - logging out');
    authStore.logout();
  }
});
```

---

## 🗄️ Auth Store (Pinia)

### State 구조

```typescript
export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<PortalUser | null>(null);

  // Getters
  const isAuthenticated = computed(() => user.value !== null);
  const displayName = computed(() => {
    if (!user.value) return 'Guest';
    const p = user.value.profile;
    return p.nickname || p.username || p.name || p.email;
  });
  const isAdmin = computed(() => hasRole('ROLE_ADMIN'));

  // Actions
  function setUser(oidcUser: User) {
    const payload = parseJwtPayload(oidcUser.access_token);

    user.value = {
      profile: {
        sub: payload.sub,
        email: payload.sub,
        username: payload.preferred_username,
        name: payload.name,
        nickname: payload.nickname,
        // ...
      },
      authority: {
        roles: payload.roles || [],
        scopes: payload.scope?.split(' ') || [],
      },
      preferences: {
        theme: 'light',
        language: 'ko',
        notifications: true,
      },
      _accessToken: oidcUser.access_token,
      _refreshToken: oidcUser.refresh_token,
      _expiresAt: oidcUser.expires_at,
    };
  }

  function logout() {
    user.value = null;
  }

  function hasRole(role: string): boolean {
    return user.value?.authority.roles.includes(role) || false;
  }

  return {
    user,
    isAuthenticated,
    displayName,
    isAdmin,
    hasRole,
    setUser,
    logout,
  };
});
```

---

## 🔑 JWT 구조

### Access Token Payload

```json
{
  "sub": "user@example.com",
  "iss": "http://localhost:8081",
  "aud": ["portal-client"],
  "exp": 1737273600,
  "iat": 1737270000,
  "scope": "openid profile",
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "preferred_username": "johndoe",
  "name": "John Doe",
  "nickname": "JD",
  "email_verified": true,
  "locale": "ko"
}
```

### JWT 파싱

```typescript
// src/utils/jwt.ts
export function parseJwtPayload(token: string): any | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (err) {
    console.error('Failed to parse JWT:', err);
    return null;
  }
}
```

---

## ⚙️ OIDC 설정

### 환경변수 (.env)

```bash
# Auth Service
VITE_OIDC_AUTHORITY=http://localhost:8081
VITE_OIDC_CLIENT_ID=portal-client
VITE_OIDC_REDIRECT_URI=http://localhost:30000/callback
VITE_OIDC_POST_LOGOUT_REDIRECT_URI=http://localhost:30000
VITE_OIDC_RESPONSE_TYPE=code
VITE_OIDC_SCOPE=openid profile
VITE_OIDC_DISABLE_PKCE=false
```

### UserManager 설정

```typescript
const settings = {
  authority: 'http://localhost:8081',
  client_id: 'portal-client',
  redirect_uri: 'http://localhost:30000/callback',
  post_logout_redirect_uri: 'http://localhost:30000',
  response_type: 'code',
  scope: 'openid profile',

  // Storage
  userStore: new WebStorageStateStore({ store: window.localStorage }),

  // Silent Renewal
  automaticSilentRenew: true,
  silent_redirect_uri: window.location.origin + '/silent-renew.html',
  accessTokenExpiringNotificationTimeInSeconds: 60,

  // PKCE
  disablePKCE: false,
};

const userManager = new UserManager(settings);
```

---

## 🔒 보안 고려사항

### 1. PKCE (Proof Key for Code Exchange)

- Code Verifier: 무작위 문자열 (43-128자)
- Code Challenge: SHA256(Code Verifier)
- Authorization Code 탈취 공격 방지

### 2. Token 저장

- localStorage에 저장 (WebStorageStateStore)
- XSS 공격 주의: CSP(Content Security Policy) 적용 필요
- Refresh Token은 HttpOnly Cookie 권장 (미래 개선)

### 3. Silent Renewal 보안

- iframe의 `prompt=none` 파라미터로 자동 갱신
- 세션 쿠키가 있어야 성공
- 실패 시 자동 로그아웃

### 4. JWT 검증

- API Gateway에서 서명 검증
- 만료 시간 확인
- Audience(aud) 클레임 검증

---

## ⚠️ 주의사항

### 1. Silent Renewal 무한 루프 방지

**문제:** `AccessTokenExpired` 이벤트가 연속 발생하여 무한 로그아웃

**해결:**
- Debounce 메커니즘: 3초 내 중복 로그아웃 방지
- 토큰 갱신 상태 추적 (`TokenRenewalState`)
- 갱신 성공 여부 재확인 (`tokenValidator.isValid()`)

### 2. silent-renew.html 로드 실패

**문제:** iframe에서 oidc-client-ts 로드 실패

**해결:**
1. CDN URL 확인
2. Browser Cache 클리어
3. Network DevTools 확인

### 3. CORS 에러

**문제:** Auth Service와 Portal Shell의 Origin이 다름

**해결:**
- Auth Service에서 CORS 허용
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "http://localhost:30000"
            allowedMethods: "*"
```

---

## 📊 인증 흐름 타이밍

| 단계 | 예상 시간 | 비고 |
|------|-----------|------|
| 로그인 리다이렉트 | < 500ms | Auth Service로 이동 |
| 사용자 입력 | 가변 | 사용자 행동 |
| Token 발급 | < 300ms | Auth Service 처리 |
| Silent Renewal | < 500ms | iframe 로드 포함 |
| API 호출 (인증 포함) | < 200ms | JWT 검증 포함 |

---

## 🔗 관련 문서

- [System Overview](./system-overview.md)
- [Auth Service API 명세](../../../services/auth-service/docs/api/)
- [oidc-client-ts 공식 문서](https://github.com/authts/oidc-client-ts)
- [OAuth 2.0 PKCE RFC](https://datatracker.ietf.org/doc/html/rfc7636)

---

**최종 업데이트**: 2026-01-18
