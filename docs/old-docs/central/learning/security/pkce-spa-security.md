# PKCE for SPA Security

**PKCE (Proof Key for Code Exchange)**는 Public Client(SPA, 모바일 앱)에서 **Authorization Code Interception Attack**을 방지하기 위한 OAuth2 확장입니다.

## 1. SPA에서의 보안 문제

### 1.1 왜 SPA에서 OAuth2가 위험한가?

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                   SPA Security Challenges                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                    Public Client vs Confidential Client              │   │
│   ├───────────────────────────────┬─────────────────────────────────────┤   │
│   │       Confidential Client      │        Public Client (SPA)          │   │
│   ├───────────────────────────────┼─────────────────────────────────────┤   │
│   │ - 서버에서 실행                │ - 브라우저에서 실행                 │   │
│   │ - Client Secret 안전 보관 가능 │ - Client Secret 보관 불가능 ⚠️      │   │
│   │ - 코드 숨김 가능               │ - 모든 코드가 공개됨                │   │
│   │                               │ - DevTools로 네트워크 확인 가능     │   │
│   └───────────────────────────────┴─────────────────────────────────────┘   │
│                                                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                    Authorization Code Interception Attack            │   │
│   ├─────────────────────────────────────────────────────────────────────┤   │
│   │                                                                     │   │
│   │   1. 사용자가 OAuth2 로그인 시작                                     │   │
│   │   2. Authorization Server가 code를 redirect URI로 전달              │   │
│   │   3. ⚠️ 악성 앱/확장 프로그램이 redirect를 가로채고 code 탈취       │   │
│   │   4. 공격자가 탈취한 code로 token 요청                               │   │
│   │   5. 공격자가 사용자 계정에 접근                                     │   │
│   │                                                                     │   │
│   │   [User] ──▶ [Auth Server] ──▶ [Code] ──✕──▶ [Malicious App]       │   │
│   │                                    │                                │   │
│   │                                    └───▶ [Legitimate SPA] ✕        │   │
│   │                                                                     │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 토큰 저장 문제

| 저장 위치 | 문제점 | 위험도 |
|----------|--------|-------|
| **LocalStorage** | XSS 공격에 취약 | 높음 |
| **SessionStorage** | XSS 공격에 취약 | 높음 |
| **Cookie** | CSRF 공격에 취약 (HttpOnly 아님) | 중간 |
| **HttpOnly Cookie** | CSRF 가능, 서버 설정 필요 | 낮음 |
| **Memory (변수)** | 새로고침 시 손실 | XSS 안전 |

---

## 2. PKCE 동작 원리

### 2.1 PKCE Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              PKCE Flow                                        │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   SPA                                Auth Server                             │
│    │                                     │                                   │
│ 1. │──Generate code_verifier─────────────│                                   │
│    │   (랜덤 문자열, 43-128자)           │                                   │
│    │                                     │                                   │
│ 2. │──code_challenge = SHA256(verifier)──│                                   │
│    │   (Base64URL 인코딩)                │                                   │
│    │                                     │                                   │
│ 3. │───Authorization Request────────────▶│                                   │
│    │   code_challenge=xxx                │                                   │
│    │   code_challenge_method=S256        │                                   │
│    │                                     │                                   │
│    │                                     │──Store code_challenge             │
│    │                                     │  with authorization code          │
│    │                                     │                                   │
│ 4. │◀───────Authorization Code───────────│                                   │
│    │                                     │                                   │
│ 5. │───Token Request─────────────────────▶│                                  │
│    │   code=xxx                          │                                   │
│    │   code_verifier=original_verifier   │                                   │
│    │                                     │                                   │
│    │                                     │──Verify:                          │
│    │                                     │  SHA256(verifier) == challenge?   │
│    │                                     │                                   │
│ 6. │◀───────Access Token─────────────────│   (일치하면 토큰 발급)             │
│    │                                     │                                   │
│                                                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │  공격자가 Authorization Code를 탈취해도:                             │   │
│   │  - code_verifier를 모름 (SPA 메모리에만 존재)                        │   │
│   │  - Token 요청 시 verifier 필요                                       │   │
│   │  - SHA256(fake_verifier) ≠ stored_challenge                         │   │
│   │  → 토큰 발급 실패 ✓                                                  │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 code_verifier & code_challenge 생성

```typescript
// pkce.ts

/**
 * 랜덤 code_verifier 생성
 * - 43-128자의 unreserved characters
 * - [A-Z] / [a-z] / [0-9] / - / . / _ / ~
 */
function generateCodeVerifier(): string {
    const array = new Uint8Array(32);  // 256 bits
    crypto.getRandomValues(array);
    return base64UrlEncode(array);
}

/**
 * code_challenge 생성 (SHA256)
 */
async function generateCodeChallenge(verifier: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(verifier);
    const digest = await crypto.subtle.digest('SHA-256', data);
    return base64UrlEncode(new Uint8Array(digest));
}

/**
 * Base64 URL-safe 인코딩
 */
function base64UrlEncode(buffer: Uint8Array): string {
    const base64 = btoa(String.fromCharCode(...buffer));
    return base64
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');
}

// 사용 예시
const codeVerifier = generateCodeVerifier();
const codeChallenge = await generateCodeChallenge(codeVerifier);

// codeVerifier는 메모리에 보관 (sessionStorage 가능)
sessionStorage.setItem('pkce_verifier', codeVerifier);
```

---

## 3. SPA에서의 PKCE 구현

### 3.1 Authorization Request

```typescript
// auth.ts

interface PKCEParams {
    codeVerifier: string;
    codeChallenge: string;
    state: string;
}

async function startPKCEAuth(provider: string): Promise<void> {
    // 1. PKCE 파라미터 생성
    const codeVerifier = generateCodeVerifier();
    const codeChallenge = await generateCodeChallenge(codeVerifier);
    const state = generateRandomString(32);

    // 2. 임시 저장 (토큰 교환 시 필요)
    sessionStorage.setItem('pkce_verifier', codeVerifier);
    sessionStorage.setItem('oauth_state', state);

    // 3. Authorization URL 생성
    const authUrl = new URL(`${AUTH_SERVER}/oauth2/authorize`);
    authUrl.searchParams.set('response_type', 'code');
    authUrl.searchParams.set('client_id', CLIENT_ID);
    authUrl.searchParams.set('redirect_uri', REDIRECT_URI);
    authUrl.searchParams.set('scope', 'openid profile email');
    authUrl.searchParams.set('state', state);
    authUrl.searchParams.set('code_challenge', codeChallenge);
    authUrl.searchParams.set('code_challenge_method', 'S256');

    // 4. Authorization Server로 리다이렉트
    window.location.href = authUrl.toString();
}
```

### 3.2 Callback 처리 (Token Exchange)

```typescript
// callback.ts

async function handleCallback(): Promise<TokenResponse> {
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');
    const state = params.get('state');
    const error = params.get('error');

    // 1. 에러 확인
    if (error) {
        throw new Error(`OAuth error: ${error}`);
    }

    // 2. State 검증 (CSRF 방지)
    const savedState = sessionStorage.getItem('oauth_state');
    if (state !== savedState) {
        throw new Error('State mismatch - possible CSRF attack');
    }

    // 3. code_verifier 가져오기
    const codeVerifier = sessionStorage.getItem('pkce_verifier');
    if (!codeVerifier) {
        throw new Error('Code verifier not found');
    }

    // 4. 토큰 교환 요청
    const tokenResponse = await fetch(`${AUTH_SERVER}/oauth2/token`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
            grant_type: 'authorization_code',
            code: code!,
            redirect_uri: REDIRECT_URI,
            client_id: CLIENT_ID,
            code_verifier: codeVerifier,  // PKCE 핵심!
        }),
    });

    // 5. 임시 데이터 정리
    sessionStorage.removeItem('pkce_verifier');
    sessionStorage.removeItem('oauth_state');

    if (!tokenResponse.ok) {
        throw new Error('Token exchange failed');
    }

    return tokenResponse.json();
}
```

### 3.3 토큰 관리

```typescript
// tokenManager.ts

class TokenManager {
    private accessToken: string | null = null;
    private refreshToken: string | null = null;

    /**
     * 토큰 저장 (메모리에만 - XSS 방지)
     */
    setTokens(access: string, refresh: string): void {
        this.accessToken = access;
        this.refreshToken = refresh;

        // Refresh Token만 안전한 저장소에 (선택적)
        // HttpOnly Cookie를 통해 서버에서 관리하는 것이 더 안전
    }

    /**
     * Access Token 가져오기
     */
    getAccessToken(): string | null {
        return this.accessToken;
    }

    /**
     * Access Token 갱신
     */
    async refreshAccessToken(): Promise<string> {
        if (!this.refreshToken) {
            throw new Error('No refresh token');
        }

        const response = await fetch(`${AUTH_SERVER}/api/auth/refresh`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                refreshToken: this.refreshToken,
            }),
        });

        if (!response.ok) {
            this.clearTokens();
            throw new Error('Token refresh failed');
        }

        const data = await response.json();
        this.accessToken = data.data.accessToken;

        return this.accessToken!;
    }

    /**
     * 로그아웃
     */
    async logout(): Promise<void> {
        if (this.accessToken) {
            try {
                await fetch(`${AUTH_SERVER}/api/auth/logout`, {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${this.accessToken}`,
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        refreshToken: this.refreshToken,
                    }),
                });
            } catch (e) {
                console.warn('Logout request failed:', e);
            }
        }

        this.clearTokens();
    }

    private clearTokens(): void {
        this.accessToken = null;
        this.refreshToken = null;
    }
}

export const tokenManager = new TokenManager();
```

---

## 4. Portal Universe SPA 보안 패턴

### 4.1 Fragment 기반 토큰 전달 (현재 구현)

Portal Universe는 OAuth2 성공 시 **URL Fragment**로 토큰을 전달합니다.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│               Portal Universe OAuth2 Token Delivery                           │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Auth Service                                   Portal Shell (SPA)          │
│       │                                              │                       │
│       │                                              │                       │
│       │──Redirect to:─────────────────────────────▶ │                       │
│       │  http://localhost:30000/oauth2/callback     │                       │
│       │  #access_token=eyJ...                       │                       │
│       │  &refresh_token=eyJ...                      │                       │
│       │  &expires_in=900                            │                       │
│       │                                              │                       │
│       │                                              │                       │
│       │  ┌──────────────────────────────────────────┴──────────────────────┐│
│       │  │                                                                  ││
│       │  │  Fragment (#) 장점:                                              ││
│       │  │  - 서버로 전송되지 않음 (URL은 전송, Fragment는 X)               ││
│       │  │  - 브라우저 히스토리에 안전하게 저장                              ││
│       │  │  - 서버 로그에 노출되지 않음                                      ││
│       │  │                                                                  ││
│       │  │  JavaScript에서 처리:                                            ││
│       │  │  const hash = window.location.hash.substring(1);                 ││
│       │  │  const params = new URLSearchParams(hash);                       ││
│       │  │  const accessToken = params.get('access_token');                 ││
│       │  │                                                                  ││
│       │  └──────────────────────────────────────────────────────────────────┘│
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 OAuth2 Callback 처리 (Vue 3)

```vue
<!-- OAuth2Callback.vue -->
<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';

const router = useRouter();
const authStore = useAuthStore();

onMounted(async () => {
    // 1. URL Fragment에서 토큰 추출
    const hash = window.location.hash.substring(1);
    const params = new URLSearchParams(hash);

    const accessToken = params.get('access_token');
    const refreshToken = params.get('refresh_token');
    const expiresIn = params.get('expires_in');

    // 2. 토큰 검증
    if (!accessToken || !refreshToken) {
        console.error('Missing tokens in callback');
        router.push('/login?error=auth_failed');
        return;
    }

    // 3. 토큰 저장 (Pinia store - 메모리)
    authStore.setTokens(accessToken, refreshToken);

    // 4. URL에서 토큰 정보 제거 (보안)
    window.history.replaceState(null, '', '/oauth2/callback');

    // 5. 사용자 정보 로드 및 리다이렉트
    try {
        await authStore.fetchCurrentUser();
        router.push('/');
    } catch (error) {
        console.error('Failed to fetch user:', error);
        router.push('/login?error=fetch_user_failed');
    }
});
</script>

<template>
    <div class="flex items-center justify-center h-screen">
        <div class="text-center">
            <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-primary mx-auto"></div>
            <p class="mt-4">로그인 처리 중...</p>
        </div>
    </div>
</template>
```

### 4.3 Axios Interceptor

```typescript
// api/interceptors.ts
import axios from 'axios';
import { useAuthStore } from '@/stores/authStore';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
});

// Request Interceptor - 토큰 추가
api.interceptors.request.use(
    (config) => {
        const authStore = useAuthStore();
        const token = authStore.accessToken;

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (error) => Promise.reject(error)
);

// Response Interceptor - 401 처리 및 토큰 갱신
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        const authStore = useAuthStore();

        // 401 에러이고, 재시도하지 않은 경우
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                // Refresh Token으로 새 Access Token 발급
                const newToken = await authStore.refreshToken();

                // 새 토큰으로 원래 요청 재시도
                originalRequest.headers.Authorization = `Bearer ${newToken}`;
                return api(originalRequest);

            } catch (refreshError) {
                // Refresh 실패 - 로그아웃
                authStore.logout();
                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);

export default api;
```

---

## 5. 추가 보안 권장사항

### 5.1 CSP (Content Security Policy)

```html
<!-- index.html -->
<meta http-equiv="Content-Security-Policy" content="
    default-src 'self';
    script-src 'self' 'unsafe-inline';
    style-src 'self' 'unsafe-inline';
    connect-src 'self' https://api.portal-universe.com;
    frame-ancestors 'none';
">
```

### 5.2 XSS 방지

```typescript
// 사용자 입력 Sanitize
import DOMPurify from 'dompurify';

function sanitize(input: string): string {
    return DOMPurify.sanitize(input);
}

// Vue에서 v-html 사용 시
<div v-html="sanitize(userContent)"></div>
```

### 5.3 HTTPS 강제

```typescript
// router/index.ts
router.beforeEach((to, from, next) => {
    // Production에서 HTTPS 강제
    if (import.meta.env.PROD && location.protocol !== 'https:') {
        location.replace(`https:${location.href.substring(location.protocol.length)}`);
        return;
    }
    next();
});
```

---

## 6. PKCE vs 기존 방식 비교

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    PKCE vs Implicit Flow Comparison                           │
├────────────────────────────────┬────────────────────┬────────────────────────┤
│           항목                 │  Implicit Flow     │    PKCE                │
│                                │  (Deprecated)      │   (Recommended)        │
├────────────────────────────────┼────────────────────┼────────────────────────┤
│ Access Token 노출              │ URL Fragment에 노출 │ Backend에서 교환       │
│ Refresh Token 지원             │      X             │        O               │
│ Code Interception 방어         │      X             │        O               │
│ Client Secret 필요             │      X             │        X               │
│ 보안 수준                      │     낮음           │       높음             │
│ OAuth 2.1 권장                 │      X             │        O               │
└────────────────────────────────┴────────────────────┴────────────────────────┘
```

---

## 7. 참고 자료

- [RFC 7636 - PKCE](https://tools.ietf.org/html/rfc7636)
- [OAuth 2.0 for Browser-Based Apps](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-browser-based-apps)
- [OAuth 2.1 Draft](https://oauth.net/2.1/)

## 8. 다음 단계

1. [Portal Universe Auth Flow](./portal-universe-auth-flow.md)
