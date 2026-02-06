# Security & Authentication API Reference

## 목차
1. [개요](#개요)
2. [OAuth2 & OpenID Connect](#oauth2--openid-connect)
3. [공통 응답 형식](#공통-응답-형식)
4. [에러 코드](#에러-코드)
5. [OAuth2 엔드포인트](#oauth2-엔드포인트)
6. [사용자 인증 API](#사용자-인증-api)
7. [인증 흐름 가이드](#인증-흐름-가이드)

---

## 개요

**Base URL**: `http://localhost:8080`

**Scope**: Auth Service의 모든 인증/인가 API 엔드포인트

**Protocol**: OAuth2 / OpenID Connect 1.0

**Token Type**: JWT (JSON Web Token)

### API 구조
```
/oauth2
├── /authorize              - Authorization Endpoint (인가 코드 발급)
├── /token                  - Token Endpoint (액세스 토큰 발급)
├── /jwks                   - JWK Set Endpoint (공개 키 조회)
├── /introspect             - Token Introspection (토큰 검증)
└── /revoke                 - Token Revocation (토큰 폐기)

/.well-known
└── /openid-configuration   - OpenID Discovery (서버 메타데이터)

/api/users
├── POST /signup            - 회원가입
└── GET  /me                - 내 정보 조회 (인증 필요)

/login                      - 로그인 페이지
/logout                     - 로그아웃
```

---

## OAuth2 & OpenID Connect

### 지원하는 Grant Type

| Grant Type | 용도 | Client Type |
|------------|------|-------------|
| Authorization Code | 웹/모바일 앱 로그인 | Public Client (PKCE 필수) |
| Refresh Token | 액세스 토큰 갱신 | 모든 클라이언트 |

### Client 정보

| 항목 | 값 |
|------|------|
| Client ID | `portal-client` |
| Client Secret | 없음 (Public Client) |
| PKCE | 필수 (Proof Key for Code Exchange) |
| Redirect URIs | 설정 파일에서 관리 |

### 지원하는 Scope

| Scope | 설명 |
|-------|------|
| `openid` | OpenID Connect 활성화 (필수) |
| `profile` | 사용자 프로필 정보 접근 |
| `read` | 읽기 권한 |
| `write` | 쓰기 권한 |

### Token 설정

| Token Type | TTL (Time To Live) |
|------------|-------------------|
| Access Token | 2분 |
| Refresh Token | 7일 (재사용 불가) |
| ID Token | Access Token과 동일 |

---

## 공통 응답 형식

### 성공 응답

모든 성공 응답은 다음 구조를 따릅니다:

```json
{
  "success": true,
  "data": {
    // 실제 데이터
  },
  "error": null
}
```

### 에러 응답

모든 실패 응답은 다음 구조를 따릅니다:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A001",
    "message": "Email already exists"
  }
}
```

### HTTP 상태 코드

| 코드 | 의미 | 예시 |
|------|------|------|
| 200 | OK | 로그인 성공, 토큰 발급 성공 |
| 201 | Created | 회원가입 성공 |
| 400 | Bad Request | 유효성 검증 실패, 잘못된 요청 |
| 401 | Unauthorized | 인증 필요, 토큰 만료/유효하지 않음 |
| 403 | Forbidden | 권한 부족 |
| 409 | Conflict | 이메일 중복 |
| 500 | Internal Server Error | 서버 오류 |

---

## 에러 코드

### Auth Service 에러 코드 (A0XX)

| 코드 | HTTP | 메시지 | 설명 | 원인 |
|------|------|--------|------|------|
| A001 | 409 | Email already exists | 이미 존재하는 이메일 | 회원가입 시 중복 이메일 사용 |

### 공통 에러 코드 (C0XX)

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| C001 | 400 | Invalid request parameter | 잘못된 요청 파라미터 |
| C002 | 401 | Unauthorized | 인증되지 않음 |
| C003 | 503 | Service unavailable | 다른 서비스 통신 실패 |

### OAuth2 표준 에러 코드

| 에러 코드 | 설명 | 발생 상황 |
|----------|------|----------|
| `invalid_request` | 요청 파라미터 누락 또는 잘못됨 | 필수 파라미터 누락 |
| `invalid_client` | 클라이언트 인증 실패 | 잘못된 client_id |
| `invalid_grant` | 인가 코드 또는 Refresh Token이 유효하지 않음 | 만료되거나 잘못된 토큰 |
| `unauthorized_client` | 클라이언트가 해당 Grant Type을 사용할 수 없음 | 권한 없는 요청 |
| `unsupported_grant_type` | 지원하지 않는 Grant Type | 잘못된 grant_type |
| `invalid_scope` | 요청한 scope이 유효하지 않음 | 존재하지 않는 scope |

---

## OAuth2 엔드포인트

### 1. OpenID Discovery

OpenID Connect Provider의 메타데이터를 조회합니다.

**Endpoint**: `GET /.well-known/openid-configuration`

**권한**: 없음 (Public)

**Response (200 OK)**:
```json
{
  "issuer": "http://localhost:8080",
  "authorization_endpoint": "http://localhost:8080/oauth2/authorize",
  "token_endpoint": "http://localhost:8080/oauth2/token",
  "jwks_uri": "http://localhost:8080/oauth2/jwks",
  "userinfo_endpoint": "http://localhost:8080/userinfo",
  "introspection_endpoint": "http://localhost:8080/oauth2/introspect",
  "revocation_endpoint": "http://localhost:8080/oauth2/revoke",
  "end_session_endpoint": "http://localhost:8080/connect/logout",
  "response_types_supported": ["code"],
  "grant_types_supported": ["authorization_code", "refresh_token"],
  "subject_types_supported": ["public"],
  "id_token_signing_alg_values_supported": ["RS256"],
  "scopes_supported": ["openid", "profile", "read", "write"],
  "token_endpoint_auth_methods_supported": ["none"],
  "code_challenge_methods_supported": ["S256"]
}
```

**Example - cURL**:
```bash
curl -X GET http://localhost:8080/.well-known/openid-configuration
```

---

### 2. JWK Set Endpoint

JWT 토큰 검증에 사용되는 공개 키 정보를 조회합니다.

**Endpoint**: `GET /oauth2/jwks`

**권한**: 없음 (Public)

**Response (200 OK)**:
```json
{
  "keys": [
    {
      "kty": "RSA",
      "e": "AQAB",
      "kid": "key-id",
      "n": "public-key-modulus...",
      "use": "sig",
      "alg": "RS256"
    }
  ]
}
```

**Example - cURL**:
```bash
curl -X GET http://localhost:8080/oauth2/jwks
```

---

### 3. Authorization Endpoint

OAuth2 인가 코드 발급 요청을 처리합니다. (Authorization Code Flow)

**Endpoint**: `GET /oauth2/authorize`

**권한**: 없음 (사용자가 로그인하지 않은 경우 로그인 페이지로 리다이렉트)

**Query Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| response_type | String | O | `code` (고정값) |
| client_id | String | O | 클라이언트 ID (예: `portal-client`) |
| redirect_uri | String | O | 인가 코드를 받을 Redirect URI |
| scope | String | O | 공백으로 구분된 scope (예: `openid profile read`) |
| state | String | 권장 | CSRF 공격 방지용 랜덤 문자열 |
| code_challenge | String | O | PKCE Code Challenge (SHA256) |
| code_challenge_method | String | O | `S256` (고정값) |

**Response**:

성공 시 `redirect_uri`로 리다이렉트하며, 다음 파라미터를 포함합니다:

```
http://your-app.com/callback?code=AUTHORIZATION_CODE&state=STATE_VALUE
```

**Example - 브라우저 URL**:
```
http://localhost:8080/oauth2/authorize?response_type=code&client_id=portal-client&redirect_uri=http://localhost:3000/callback&scope=openid%20profile%20read&state=random-state&code_challenge=CHALLENGE&code_challenge_method=S256
```

---

### 4. Token Endpoint

인가 코드를 사용하여 액세스 토큰을 발급하거나, Refresh Token으로 새 액세스 토큰을 발급합니다.

**Endpoint**: `POST /oauth2/token`

**권한**: 없음 (Public)

**Request Headers**:
```http
Content-Type: application/x-www-form-urlencoded
```

#### 4-1. Authorization Code Grant

**Request Body (Form Data)**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| grant_type | String | O | `authorization_code` |
| code | String | O | Authorization Endpoint에서 받은 인가 코드 |
| redirect_uri | String | O | Authorization 요청 시 사용한 Redirect URI |
| client_id | String | O | 클라이언트 ID |
| code_verifier | String | O | PKCE Code Verifier |

**Response (200 OK)**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 120,
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "scope": "openid profile read",
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response Schema**:

| 필드 | 타입 | 설명 |
|------|------|------|
| access_token | String | JWT 액세스 토큰 (2분 유효) |
| token_type | String | `Bearer` (고정값) |
| expires_in | Number | 만료 시간 (초 단위, 120초 = 2분) |
| refresh_token | String | Refresh Token (7일 유효) |
| scope | String | 부여된 scope |
| id_token | String | OpenID Connect ID Token |

**Example - cURL**:
```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code=AUTHORIZATION_CODE" \
  -d "redirect_uri=http://localhost:3000/callback" \
  -d "client_id=portal-client" \
  -d "code_verifier=CODE_VERIFIER"
```

#### 4-2. Refresh Token Grant

**Request Body (Form Data)**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| grant_type | String | O | `refresh_token` |
| refresh_token | String | O | 이전에 발급받은 Refresh Token |
| client_id | String | O | 클라이언트 ID |

**Response (200 OK)**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 120,
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "scope": "openid profile read"
}
```

**Example - cURL**:
```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=REFRESH_TOKEN" \
  -d "client_id=portal-client"
```

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 400 | invalid_request | 필수 파라미터 누락 |
| 400 | invalid_grant | 인가 코드 또는 Refresh Token이 유효하지 않음 |
| 400 | invalid_client | 잘못된 client_id |
| 400 | unsupported_grant_type | 지원하지 않는 grant_type |

---

### 5. Token Introspection

액세스 토큰의 유효성을 검증하고 토큰 정보를 조회합니다.

**Endpoint**: `POST /oauth2/introspect`

**권한**: 인증 필요 (리소스 서버가 사용)

**Request Headers**:
```http
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer <RESOURCE_SERVER_TOKEN>
```

**Request Body (Form Data)**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| token | String | O | 검증할 액세스 토큰 |

**Response (200 OK) - 유효한 토큰**:
```json
{
  "active": true,
  "scope": "openid profile read",
  "client_id": "portal-client",
  "username": "user@example.com",
  "token_type": "Bearer",
  "exp": 1737275340,
  "iat": 1737275220,
  "sub": "user@example.com",
  "aud": ["portal-client"],
  "iss": "http://localhost:8080"
}
```

**Response (200 OK) - 유효하지 않은 토큰**:
```json
{
  "active": false
}
```

**Example - cURL**:
```bash
curl -X POST http://localhost:8080/oauth2/introspect \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Bearer <RESOURCE_SERVER_TOKEN>" \
  -d "token=ACCESS_TOKEN"
```

---

### 6. Token Revocation

액세스 토큰 또는 Refresh Token을 폐기합니다.

**Endpoint**: `POST /oauth2/revoke`

**권한**: 인증 필요

**Request Headers**:
```http
Content-Type: application/x-www-form-urlencoded
Authorization: Bearer <ACCESS_TOKEN>
```

**Request Body (Form Data)**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| token | String | O | 폐기할 토큰 |
| token_type_hint | String | X | `access_token` 또는 `refresh_token` (힌트) |

**Response (200 OK)**:
```
(빈 응답)
```

**Example - cURL**:
```bash
curl -X POST http://localhost:8080/oauth2/revoke \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -d "token=REFRESH_TOKEN" \
  -d "token_type_hint=refresh_token"
```

---

## 사용자 인증 API

### 1. 회원가입

새로운 사용자를 등록합니다.

**Endpoint**: `POST /api/v1/users/signup`

**권한**: 없음 (Public)

**Request Headers**:
```http
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "nickname": "my-nickname",
  "realName": "홍길동",
  "marketingAgree": true
}
```

**Request Schema**:

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| email | String | O | 이메일 형식 | 사용자 이메일 (로그인 ID) |
| password | String | O | 8자 이상 | 비밀번호 (BCrypt로 암호화 저장) |
| nickname | String | O | 2-20자 | 닉네임 |
| realName | String | O | 2-50자 | 실명 |
| marketingAgree | Boolean | O | true/false | 마케팅 수신 동의 |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": "User registered successfully",
  "error": null
}
```

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 400 | C001 | 유효성 검증 실패 (필수 필드 누락, 잘못된 형식) |
| 409 | A001 | 이미 존재하는 이메일 |

**Example - cURL**:
```bash
curl -X POST http://localhost:8080/api/v1/users/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePassword123!",
    "nickname": "my-nickname",
    "realName": "홍길동",
    "marketingAgree": true
  }'
```

**Example - JavaScript (Fetch)**:
```javascript
const response = await fetch('http://localhost:8080/api/v1/users/signup', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'SecurePassword123!',
    nickname: 'my-nickname',
    realName: '홍길동',
    marketingAgree: true
  })
});
const data = await response.json();
```

---

### 2. 로그인 (Form Login)

사용자명/비밀번호로 로그인합니다.

**Endpoint**: `POST /login`

**권한**: 없음 (Public)

**Request Headers**:
```http
Content-Type: application/x-www-form-urlencoded
```

**Request Body (Form Data)**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| username | String | O | 사용자 이메일 |
| password | String | O | 비밀번호 |

**Response**:

- **성공 시**: 세션이 생성되고 쿠키(`JSESSIONID`)가 설정됩니다.
- **실패 시**: `/login?error`로 리다이렉트됩니다.

**Example - cURL**:
```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=user@example.com" \
  -d "password=SecurePassword123!" \
  -c cookies.txt
```

---

### 3. 로그아웃

현재 사용자의 세션을 종료합니다.

**Endpoint**: `POST /logout`

**권한**: 인증 필요

**Request Headers**:
```http
Cookie: JSESSIONID=<SESSION_ID>
```

**Response**:

세션이 무효화되고 쿠키(`JSESSIONID`)가 삭제됩니다.

**Example - cURL**:
```bash
curl -X POST http://localhost:8080/logout \
  -b cookies.txt
```

---

### 4. 내 정보 조회

현재 로그인한 사용자의 정보를 조회합니다.

**Endpoint**: `GET /api/v1/users/me`

**권한**: 인증 필요

**Request Headers**:
```http
Authorization: Bearer <ACCESS_TOKEN>
```

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "nickname": "my-nickname",
    "realName": "홍길동",
    "roles": ["ROLE_USER"],
    "createdAt": "2026-01-19T10:00:00Z"
  },
  "error": null
}
```

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 401 | C002 | 액세스 토큰이 없거나 유효하지 않음 |

**Example - cURL**:
```bash
curl -X GET http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer eyJhbGc..."
```

---

## 인증 흐름 가이드

### 1. Authorization Code Flow with PKCE

Portal Universe에서 사용하는 표준 인증 흐름입니다.

#### Step 1: PKCE Code Verifier 및 Challenge 생성

```javascript
// 1. Code Verifier 생성 (43-128자의 랜덤 문자열)
function generateCodeVerifier() {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return base64URLEncode(array);
}

// 2. Code Challenge 생성 (SHA256 해시)
async function generateCodeChallenge(verifier) {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const hash = await crypto.subtle.digest('SHA-256', data);
  return base64URLEncode(new Uint8Array(hash));
}

// Base64 URL Encoding
function base64URLEncode(buffer) {
  return btoa(String.fromCharCode(...buffer))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
}

const codeVerifier = generateCodeVerifier();
const codeChallenge = await generateCodeChallenge(codeVerifier);

// 🔒 codeVerifier는 안전하게 저장 (sessionStorage 또는 메모리)
sessionStorage.setItem('pkce_code_verifier', codeVerifier);
```

#### Step 2: Authorization 요청

```javascript
const authUrl = new URL('http://localhost:8080/oauth2/authorize');
authUrl.searchParams.append('response_type', 'code');
authUrl.searchParams.append('client_id', 'portal-client');
authUrl.searchParams.append('redirect_uri', 'http://localhost:3000/callback');
authUrl.searchParams.append('scope', 'openid profile read write');
authUrl.searchParams.append('state', generateRandomState()); // CSRF 방지
authUrl.searchParams.append('code_challenge', codeChallenge);
authUrl.searchParams.append('code_challenge_method', 'S256');

// 브라우저를 Authorization Endpoint로 리다이렉트
window.location.href = authUrl.toString();
```

#### Step 3: 사용자 로그인 및 동의

사용자가 로그인하고 권한을 승인하면, Authorization Server가 다음과 같이 리다이렉트합니다:

```
http://localhost:3000/callback?code=AUTHORIZATION_CODE&state=STATE_VALUE
```

#### Step 4: Authorization Code를 액세스 토큰으로 교환

```javascript
// Callback URL에서 code 파라미터 추출
const urlParams = new URLSearchParams(window.location.search);
const code = urlParams.get('code');
const state = urlParams.get('state');

// State 검증 (CSRF 공격 방지)
if (state !== sessionStorage.getItem('oauth_state')) {
  throw new Error('Invalid state parameter');
}

// 저장해둔 Code Verifier 가져오기
const codeVerifier = sessionStorage.getItem('pkce_code_verifier');

// Token Endpoint 호출
const tokenResponse = await fetch('http://localhost:8080/oauth2/token', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded'
  },
  body: new URLSearchParams({
    grant_type: 'authorization_code',
    code: code,
    redirect_uri: 'http://localhost:3000/callback',
    client_id: 'portal-client',
    code_verifier: codeVerifier
  })
});

const tokens = await tokenResponse.json();
// {
//   "access_token": "...",
//   "refresh_token": "...",
//   "id_token": "...",
//   "expires_in": 120
// }

// 🔒 토큰 안전하게 저장
sessionStorage.setItem('access_token', tokens.access_token);
sessionStorage.setItem('refresh_token', tokens.refresh_token);

// Code Verifier는 삭제 (한 번만 사용)
sessionStorage.removeItem('pkce_code_verifier');
```

#### Step 5: 액세스 토큰으로 API 호출

```javascript
const accessToken = sessionStorage.getItem('access_token');

const response = await fetch('http://localhost:8080/api/v1/users/me', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

if (response.status === 401) {
  // 토큰 만료 시 Refresh Token으로 갱신
  await refreshAccessToken();
}
```

#### Step 6: Refresh Token으로 액세스 토큰 갱신

```javascript
async function refreshAccessToken() {
  const refreshToken = sessionStorage.getItem('refresh_token');

  const tokenResponse = await fetch('http://localhost:8080/oauth2/token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: new URLSearchParams({
      grant_type: 'refresh_token',
      refresh_token: refreshToken,
      client_id: 'portal-client'
    })
  });

  if (!tokenResponse.ok) {
    // Refresh Token도 만료됨 → 재로그인 필요
    sessionStorage.clear();
    window.location.href = '/login';
    return;
  }

  const tokens = await tokenResponse.json();

  // 새 토큰 저장
  sessionStorage.setItem('access_token', tokens.access_token);
  sessionStorage.setItem('refresh_token', tokens.refresh_token);
}
```

---

### 2. 회원가입 → 로그인 흐름

#### Step 1: 회원가입

```bash
curl -X POST http://localhost:8080/api/v1/users/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "SecurePassword123!",
    "nickname": "newuser",
    "realName": "김철수",
    "marketingAgree": false
  }'
```

#### Step 2: Authorization Code Flow 시작

회원가입이 완료되면 위의 Authorization Code Flow with PKCE를 따라 로그인합니다.

---

## JWT 토큰 구조

### Access Token Claims

```json
{
  "sub": "user@example.com",
  "aud": ["portal-client"],
  "nbf": 1737275220,
  "scope": ["openid", "profile", "read"],
  "roles": ["ROLE_USER"],
  "iss": "http://localhost:8080",
  "exp": 1737275340,
  "iat": 1737275220,
  "jti": "unique-token-id"
}
```

**Claims 설명**:

| Claim | 설명 |
|-------|------|
| `sub` | Subject (사용자 식별자) |
| `aud` | Audience (토큰 수신자) |
| `nbf` | Not Before (토큰 유효 시작 시간) |
| `scope` | 부여된 권한 범위 |
| `roles` | 사용자 역할 (ROLE_USER, ROLE_ADMIN 등) |
| `iss` | Issuer (토큰 발급자) |
| `exp` | Expiration (토큰 만료 시간) |
| `iat` | Issued At (토큰 발급 시간) |
| `jti` | JWT ID (토큰 고유 ID) |

---

## 보안 가이드

### 1. PKCE (Proof Key for Code Exchange)

Portal Universe는 Public Client를 위해 **PKCE를 필수**로 요구합니다.

- ✅ Authorization Code를 중간에 가로채도 Code Verifier 없이는 사용 불가
- ✅ Client Secret 없이도 안전한 인증 가능

### 2. 토큰 저장 위치

| 저장소 | 보안 수준 | 사용 케ース |
|--------|----------|-------------|
| sessionStorage | 중 | SPA (Single Page Application) |
| localStorage | 낮음 (XSS 취약) | ❌ 권장하지 않음 |
| httpOnly Cookie | 높음 | 서버 사이드 렌더링 |
| 메모리 (변수) | 높음 | 가장 안전하지만 새로고침 시 손실 |

### 3. 토큰 갱신 전략

- Access Token TTL: **2분** (짧게 유지하여 보안 강화)
- Refresh Token TTL: **7일** (재사용 불가)
- **자동 갱신 로직 구현 권장**: API 호출 전 토큰 만료 확인

### 4. HTTPS 사용

프로덕션 환경에서는 반드시 **HTTPS**를 사용해야 합니다.

```
http://localhost:8080  ❌ 개발 환경에서만 사용
https://auth.portal-universe.com  ✅ 프로덕션 환경
```

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0.0 | 2026-01-19 | Laze | 초기 Security API 명세 작성 |
