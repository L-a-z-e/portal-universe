---
id: api-auth
title: Auth Service API
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-01-18
author: Claude
tags: [api, auth, oauth2, oidc]
related:
  - arch-system-overview
---

# Auth Service API

> Portal Universe 인증 서비스 API 명세서. OAuth2 Authorization Code with PKCE 및 회원가입 API를 제공합니다.

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `http://localhost:8081` (로컬) / `http://auth-service:8081` (Docker/K8s) |
| **인증 방식** | OAuth2 Authorization Code with PKCE |
| **지원 Grant Types** | Authorization Code, Refresh Token |
| **토큰 형식** | JWT (RS256) |
| **Access Token 유효기간** | 2분 |
| **Refresh Token 유효기간** | 7일 |

---

## 📑 API 목록

### 사용자 API

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|----------|
| POST | `/api/users/signup` | 회원가입 | ❌ |

### OAuth2 엔드포인트 (Spring Authorization Server 표준)

| Method | Endpoint | 설명 | 인증 필요 |
|--------|----------|------|----------|
| GET | `/oauth2/authorize` | 인가 코드 요청 | ❌ |
| POST | `/oauth2/token` | Access Token 발급 | ❌ |
| POST | `/oauth2/token` (refresh) | Access Token 갱신 | ❌ |
| GET | `/oauth2/jwks` | 공개키 조회 (JWT 검증용) | ❌ |
| POST | `/oauth2/revoke` | 토큰 취소 | ❌ |
| POST | `/oauth2/introspect` | 토큰 검증 | ✅ |
| GET | `/.well-known/openid-configuration` | OIDC Discovery 메타데이터 | ❌ |

---

## 🔹 회원가입 (POST /api/users/signup)

이메일 기반 회원가입 API입니다.

### Request

```http
POST /api/users/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "nickname": "johndoe",
  "realName": "John Doe",
  "marketingAgree": true
}
```

### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `email` | string | ✅ | 이메일 주소 | 유효한 이메일 형식, 고유값 |
| `password` | string | ✅ | 비밀번호 | 8자 이상 권장 |
| `nickname` | string | ✅ | 닉네임 | 2~20자 |
| `realName` | string | ✅ | 실명 | 2~50자 |
| `marketingAgree` | boolean | ✅ | 마케팅 수신 동의 | true/false |

### Response (200 OK)

```json
{
  "success": true,
  "data": "User registered successfully",
  "timestamp": "2026-01-18T10:30:00Z"
}
```

### Error Response (409 Conflict)

```json
{
  "success": false,
  "code": "A001",
  "message": "Email already exists",
  "data": null,
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

## 🔹 인가 코드 요청 (GET /oauth2/authorize)

OAuth2 Authorization Code Flow의 첫 단계입니다. 사용자를 로그인 페이지로 리다이렉트하여 인가 코드를 발급받습니다.

### Request

```http
GET /oauth2/authorize?response_type=code&client_id=portal-client&redirect_uri=http://localhost:30000/callback&scope=openid%20profile%20read%20write&code_challenge=CHALLENGE_STRING&code_challenge_method=S256&state=RANDOM_STATE
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `response_type` | string | ✅ | 응답 타입 (항상 `code`) | - |
| `client_id` | string | ✅ | 클라이언트 ID | - |
| `redirect_uri` | string | ✅ | 인가 코드 수신 URI | - |
| `scope` | string | ✅ | 요청 스코프 (공백 구분) | - |
| `code_challenge` | string | ✅ | PKCE Code Challenge | - |
| `code_challenge_method` | string | ✅ | Challenge 방식 (S256) | - |
| `state` | string | ✅ | CSRF 방지용 랜덤 문자열 | - |

### 지원 스코프

| 스코프 | 설명 |
|--------|------|
| `openid` | OIDC 표준 (필수) |
| `profile` | 프로필 정보 접근 |
| `read` | 읽기 권한 |
| `write` | 쓰기 권한 |

### Response

사용자 로그인 후 `redirect_uri`로 리다이렉트되며 인가 코드가 쿼리 파라미터로 전달됩니다.

```http
HTTP/1.1 302 Found
Location: http://localhost:30000/callback?code=AUTHORIZATION_CODE&state=RANDOM_STATE
```

---

## 🔹 Access Token 발급 (POST /oauth2/token)

인가 코드를 사용하여 Access Token과 Refresh Token을 발급받습니다.

### Request

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code=AUTHORIZATION_CODE
&redirect_uri=http://localhost:30000/callback
&client_id=portal-client
&code_verifier=CODE_VERIFIER_STRING
```

### Request Body (application/x-www-form-urlencoded)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `grant_type` | string | ✅ | 항상 `authorization_code` |
| `code` | string | ✅ | 인가 코드 |
| `redirect_uri` | string | ✅ | 인가 시 사용한 URI (동일해야 함) |
| `client_id` | string | ✅ | 클라이언트 ID |
| `code_verifier` | string | ✅ | PKCE Code Verifier |

### Response (200 OK)

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "scope": "openid profile read write",
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 120
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `access_token` | string | JWT Access Token (유효기간 2분) |
| `refresh_token` | string | JWT Refresh Token (유효기간 7일) |
| `scope` | string | 부여된 스코프 |
| `id_token` | string | OIDC ID Token (사용자 정보 포함) |
| `token_type` | string | 항상 "Bearer" |
| `expires_in` | number | Access Token 만료 시간 (초) |

### JWT Access Token Payload 예시

```json
{
  "sub": "user@example.com",
  "aud": ["portal-client"],
  "nbf": 1737184200,
  "scope": ["openid", "profile", "read", "write"],
  "roles": ["ROLE_USER"],
  "iss": "http://localhost:8081",
  "exp": 1737184320,
  "iat": 1737184200,
  "jti": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

## 🔹 Token 갱신 (POST /oauth2/token)

Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.

### Request

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token
&refresh_token=REFRESH_TOKEN
&client_id=portal-client
```

### Request Body (application/x-www-form-urlencoded)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `grant_type` | string | ✅ | 항상 `refresh_token` |
| `refresh_token` | string | ✅ | 기존 Refresh Token |
| `client_id` | string | ✅ | 클라이언트 ID |

### Response (200 OK)

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "scope": "openid profile read write",
  "token_type": "Bearer",
  "expires_in": 120
}
```

> **참고**: Refresh Token은 재사용되지 않으며 (`reuseRefreshTokens: false`), 갱신 시마다 새로운 Refresh Token이 발급됩니다.

---

## 🔹 JWK Set 조회 (GET /oauth2/jwks)

JWT 토큰 검증에 사용되는 공개키 정보를 조회합니다. API Gateway 및 Resource Server에서 토큰 검증 시 사용됩니다.

### Request

```http
GET /oauth2/jwks
```

### Response (200 OK)

```json
{
  "keys": [
    {
      "kty": "RSA",
      "e": "AQAB",
      "use": "sig",
      "kid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "alg": "RS256",
      "n": "xGOr_hU..."
    }
  ]
}
```

---

## 🔹 Token 취소 (POST /oauth2/revoke)

발급된 토큰을 취소합니다.

### Request

```http
POST /oauth2/revoke
Content-Type: application/x-www-form-urlencoded

token=REFRESH_TOKEN
&client_id=portal-client
```

### Request Body (application/x-www-form-urlencoded)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `token` | string | ✅ | 취소할 토큰 (Access/Refresh Token) |
| `client_id` | string | ✅ | 클라이언트 ID |

### Response (200 OK)

```http
HTTP/1.1 200 OK
```

빈 응답 본문이 반환됩니다.

---

## 🔹 OIDC Discovery (GET /.well-known/openid-configuration)

OpenID Connect Discovery 메타데이터를 조회합니다. 클라이언트가 인증 서버의 엔드포인트와 지원 기능을 자동으로 발견할 수 있습니다.

### Request

```http
GET /.well-known/openid-configuration
```

### Response (200 OK)

```json
{
  "issuer": "http://localhost:8081",
  "authorization_endpoint": "http://localhost:8081/oauth2/authorize",
  "token_endpoint": "http://localhost:8081/oauth2/token",
  "jwks_uri": "http://localhost:8081/oauth2/jwks",
  "revocation_endpoint": "http://localhost:8081/oauth2/revoke",
  "introspection_endpoint": "http://localhost:8081/oauth2/introspect",
  "response_types_supported": ["code"],
  "grant_types_supported": ["authorization_code", "refresh_token"],
  "code_challenge_methods_supported": ["S256"],
  "token_endpoint_auth_methods_supported": ["none"],
  "subject_types_supported": ["public"],
  "id_token_signing_alg_values_supported": ["RS256"],
  "scopes_supported": ["openid", "profile", "read", "write"]
}
```

---

## ⚠️ 에러 코드

### Auth Service 에러

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `A001` | 409 Conflict | 이메일 중복 (회원가입 시) |

### OAuth2 표준 에러

| Error | HTTP Status | 설명 |
|-------|-------------|------|
| `invalid_request` | 400 | 필수 파라미터 누락 |
| `invalid_grant` | 400 | 인가 코드 또는 Refresh Token 무효 |
| `invalid_client` | 401 | 클라이언트 인증 실패 |
| `unauthorized_client` | 400 | 클라이언트가 해당 Grant Type 사용 불가 |
| `unsupported_grant_type` | 400 | 지원하지 않는 Grant Type |
| `invalid_scope` | 400 | 잘못된 스코프 요청 |

### 에러 응답 예시

#### Auth Service 에러

```json
{
  "success": false,
  "code": "A001",
  "message": "Email already exists",
  "data": null,
  "timestamp": "2026-01-18T10:30:00Z"
}
```

#### OAuth2 에러

```json
{
  "error": "invalid_grant",
  "error_description": "The provided authorization grant is invalid, expired, or revoked."
}
```

---

## 🔒 보안 고려사항

### PKCE (Proof Key for Code Exchange)

이 서비스는 Public Client를 위해 PKCE를 **필수**로 요구합니다 (`requireProofKey: true`).

#### PKCE 플로우

1. **Code Verifier 생성**: 43~128자 길이의 랜덤 문자열
2. **Code Challenge 생성**: `BASE64URL(SHA256(code_verifier))`
3. **인가 요청 시**: `code_challenge`, `code_challenge_method=S256` 포함
4. **토큰 요청 시**: `code_verifier` 포함

#### JavaScript 예시

```javascript
// 1. Code Verifier 생성
function generateCodeVerifier() {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return base64URLEncode(array);
}

// 2. Code Challenge 생성
async function generateCodeChallenge(verifier) {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return base64URLEncode(new Uint8Array(digest));
}

function base64URLEncode(buffer) {
  return btoa(String.fromCharCode(...buffer))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}
```

### State 파라미터

CSRF 공격 방지를 위해 `state` 파라미터를 반드시 사용해야 합니다.

```javascript
const state = crypto.randomUUID();
sessionStorage.setItem('oauth_state', state);
// 인가 요청 시 state 포함
// 콜백 수신 시 검증
if (callbackState !== sessionStorage.getItem('oauth_state')) {
  throw new Error('State mismatch');
}
```

---

## 📌 사용 예시

### 전체 OAuth2 플로우 (oidc-client-ts 사용)

```typescript
import { UserManager } from 'oidc-client-ts';

// 1. UserManager 설정
const userManager = new UserManager({
  authority: 'http://localhost:8081',
  client_id: 'portal-client',
  redirect_uri: 'http://localhost:30000/callback',
  response_type: 'code',
  scope: 'openid profile read write',
  post_logout_redirect_uri: 'http://localhost:30000',
  automaticSilentRenew: true, // 자동 토큰 갱신
});

// 2. 로그인
await userManager.signinRedirect();

// 3. 콜백 처리
const user = await userManager.signinRedirectCallback();
console.log('Access Token:', user.access_token);

// 4. API 호출
const response = await fetch('http://localhost:8080/api/v1/blog/posts', {
  headers: {
    'Authorization': `Bearer ${user.access_token}`
  }
});

// 5. 로그아웃
await userManager.signoutRedirect();
```

### 회원가입

```typescript
async function signup(data: SignupRequest) {
  const response = await fetch('http://localhost:8081/api/users/signup', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    if (error.code === 'A001') {
      throw new Error('이미 사용 중인 이메일입니다.');
    }
    throw new Error('회원가입에 실패했습니다.');
  }

  return response.json();
}
```

---

## 🔗 관련 문서

- [Architecture Overview](../architecture/system-overview.md)
- [API Gateway 설정](../../api-gateway/docs/api/gateway-api.md)
- [Frontend 인증 구현](../guides/frontend-auth-integration.md)

---

## 📝 변경 이력

### v1.0.0 (2026-01-18)
- 최초 작성
- OAuth2 Authorization Code with PKCE 지원
- 회원가입 API 추가
- Spring Authorization Server 표준 엔드포인트 문서화

---

**최종 업데이트**: 2026-01-18
