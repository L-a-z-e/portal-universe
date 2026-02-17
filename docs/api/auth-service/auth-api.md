---
id: api-auth
title: Auth Service API
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-02-18
author: Laze
tags: [api, auth, oauth2, jwt, rbac, membership, follow, seller]
related:
  - arch-system-overview
  - ADR-003-authorization-strategy
  - ADR-008-jwt-stateless-redis
  - ADR-021-role-based-membership-restructure
  - ADR-015-role-hierarchy-implementation
  - ADR-044-role-multi-include-dag
---

# Auth Service API

> Portal Universe 인증/인가 서비스 종합 API 명세서. JWT 인증, OAuth2 소셜 로그인, RBAC, Membership, Follow, Seller 관리 API를 제공합니다.

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `http://localhost:8081` (로컬) / `http://auth-service:8081` (Docker/K8s) |
| **API Prefix** | `/api/v1` |
| **인증 방식** | JWT Bearer Token, OAuth2 (소셜 로그인) |
| **소셜 프로바이더** | Google, Naver, Kakao |
| **토큰 형식** | JWT |
| **Access Token 유효기간** | 15분 (900초) |
| **Refresh Token 유효기간** | 7일 (604800초) |
| **Cookie 이름** | `portal_refresh_token` |
| **총 Controllers** | 11개 |
| **총 Endpoints** | 약 54개 |

---

## 🎯 Controller Overview

| Controller | Base Path | 주요 기능 | 인증 요구 | 권한 요구 |
|------------|-----------|----------|----------|----------|
| **AuthController** | `/api/v1/auth` | JWT 로그인/로그아웃/갱신 | ❌ | ❌ |
| **UserController** | `/api/v1/users` | 회원가입, 프로필 조회/수정 | 일부 | ❌ |
| **ProfileController** | `/api/v1/profile` | 프로필 관리, 계정 삭제 | ✅ | ❌ |
| **FollowController** | `/api/v1/users` | 팔로우/팔로워 관리 | ✅ | ❌ |
| **RbacAdminController** | `/api/v1/admin/rbac` | 역할/권한 관리 (Admin) | ✅ | SUPER_ADMIN |
| **PermissionController** | `/api/v1/permissions` | 내 권한 조회 | ✅ | ❌ |
| **MembershipController** | `/api/v1/memberships` | 멤버십 조회/변경 | 일부 | ❌ |
| **MembershipAdminController** | `/api/v1/admin/memberships` | 멤버십 관리 (Admin) | ✅ | SUPER_ADMIN |
| **SellerController** | `/api/v1/seller` | 셀러 신청 | ✅ | ❌ |
| **SellerAdminController** | `/api/v1/admin/seller` | 셀러 승인 (Admin) | ✅ | SHOPPING_ADMIN, SUPER_ADMIN |
| **RoleHierarchyController** | `/api/v1/internal/role-hierarchy` | 역할 계층 해석 (Internal) | ❌ | Gateway 전용 |

---

## 🔐 보안 정책 (SecurityConfig)

### 공개 경로 (permitAll)

```
/api/v1/auth/**                     # 모든 인증 API
POST /api/v1/users/signup            # 회원가입
GET /api/v1/memberships/tiers/**     # 멤버십 티어 목록
/api/v1/internal/**                  # Gateway 전용 내부 API
/oauth2/**                           # OAuth2 소셜 로그인
/login/oauth2/**                     # OAuth2 콜백
/.well-known/**                      # OIDC Discovery
/actuator/health, /actuator/info     # 헬스체크
```

### 권한별 경로

| 경로 | 권한 |
|------|------|
| `/api/v1/admin/rbac/**` | ROLE_SUPER_ADMIN |
| `/api/v1/admin/memberships/**` | ROLE_SUPER_ADMIN |
| `/api/v1/admin/seller/**` | ROLE_SHOPPING_ADMIN or ROLE_SUPER_ADMIN |
| `/api/v1/admin/**` | ROLE_SUPER_ADMIN (catch-all) |

### 인증 필수 경로

```
/api/v1/profile/**
/api/v1/seller/**
/api/v1/memberships/**  (tiers/** 제외)
/api/v1/permissions/**
anyRequest().authenticated()          # 위에 없는 모든 경로
```

**중요**: 다음 경로들은 SecurityConfig에서 명시적 permitAll이 없으므로 인증이 필요합니다:
- `GET /api/v1/users/{username}` - 공개 프로필 조회
- `GET /api/v1/users/check-username/**` - Username 중복 확인
- `GET /api/v1/users/{username}/followers` - 팔로워 목록
- `GET /api/v1/users/{username}/following` - 팔로잉 목록
- `GET /api/v1/users/{username}/follow/status` - 팔로우 상태

---

## 🔐 1. AuthController (`/api/v1/auth`)

JWT 기반 로그인, 토큰 갱신, 로그아웃 API.

### 1.1. 로그인 (POST `/api/v1/auth/login`)

**인증 필요**: ❌

이메일/비밀번호로 로그인하여 JWT 토큰을 발급받습니다.

**Request**
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Request Body** (`LoginRequest`)

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| `email` | string | ✅ | @Email @NotBlank | 이메일 주소 |
| `password` | string | ✅ | @NotBlank | 비밀번호 |

**Response (200 OK)** (`LoginResponse`)
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 900
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Response Fields**

| 필드 | 타입 | 설명 |
|------|------|------|
| `accessToken` | string | JWT Access Token (15분) |
| `refreshToken` | string | JWT Refresh Token (7일) |
| `expiresIn` | long | Access Token 만료 시간 (초 단위) |

**Cookie**: Refresh Token은 `portal_refresh_token` 쿠키에도 저장됩니다.

**Error Response (401 Unauthorized)**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A002",
    "message": "Invalid credentials"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**관련 에러 코드**
- `A002`: INVALID_CREDENTIALS
- `A018`: ACCOUNT_TEMPORARILY_LOCKED
- `A019`: TOO_MANY_LOGIN_ATTEMPTS
- `A024`: PASSWORD_EXPIRED

---

### 1.2. 토큰 갱신 (POST `/api/v1/auth/refresh`)

**인증 필요**: ❌

Refresh Token으로 새 Access Token을 발급받습니다. Cookie 우선, 없으면 Body에서 읽습니다.

**Request**
```http
POST /api/v1/auth/refresh
Content-Type: application/json
Cookie: portal_refresh_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Request Body** (`RefreshRequest`, optional)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `refreshToken` | string | ❌ | Refresh Token (Cookie 우선) |

**Response (200 OK)** (`RefreshResponse`)
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 900
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Response Fields**

| 필드 | 타입 | 설명 |
|------|------|------|
| `accessToken` | string | 새 JWT Access Token |
| `refreshToken` | string | 새 Refresh Token (rotated) |
| `expiresIn` | long | 만료 시간 (초) |

**Error Response (401 Unauthorized)**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A003",
    "message": "Invalid refresh token"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 1.3. 로그아웃 (POST `/api/v1/auth/logout`)

**인증 필요**: ❌ (SecurityConfig는 permitAll, 컨트롤러에서 JWT 파싱)

Refresh Token을 무효화하여 로그아웃합니다. Cookie 우선, 없으면 Body에서 읽습니다.

**Request**
```http
POST /api/v1/auth/logout
Authorization: Bearer {accessToken}
Content-Type: application/json
Cookie: portal_refresh_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Request Body** (`LogoutRequest`, optional)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `refreshToken` | string | ❌ | Refresh Token (Cookie 우선) |

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "message": "로그아웃 성공"
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Cookie**: `portal_refresh_token` 쿠키가 삭제됩니다 (Max-Age=0).

---

### 1.4. 비밀번호 정책 조회 (GET `/api/v1/auth/password-policy`)

**인증 필요**: ❌

비밀번호 정책을 조회합니다.

**Request**
```http
GET /api/v1/auth/password-policy
```

**Response (200 OK)** (`PasswordPolicyResponse`)
```json
{
  "success": true,
  "data": {
    "minLength": 8,
    "maxLength": 128,
    "requirements": [
      "최소 1개의 대문자",
      "최소 1개의 소문자",
      "최소 1개의 숫자",
      "최소 1개의 특수문자"
    ]
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**비밀번호 정책 세부사항**

```yaml
min-length: 8
max-length: 128
require-uppercase: true
require-lowercase: true
require-digit: true
require-special-char: true
history-count: 5      # 최근 5개 재사용 금지
max-age: 90           # 90일 만료
prevent-sequential: true
prevent-user-info: true
```

---

## 👤 2. UserController (`/api/v1/users`)

사용자 회원가입, 프로필 조회/수정, username 설정, 비밀번호 변경 API.

### 2.1. 회원가입 (POST `/api/v1/users/signup`)

**인증 필요**: ❌

이메일 기반 회원가입 API.

**Request**
```http
POST /api/v1/users/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "nickname": "johndoe",
  "realName": "John Doe",
  "marketingAgree": true
}
```

**Request Body** (`UserSignupRequest`)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | string | ✅ | 이메일 주소 |
| `password` | string | ✅ | 비밀번호 (8자 이상) |
| `nickname` | string | ✅ | 닉네임 |
| `realName` | string | ✅ | 실명 |
| `marketingAgree` | boolean | ✅ | 마케팅 수신 동의 |

**Response (200 OK)**
```json
{
  "success": true,
  "data": "User registered successfully",
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Error Response (409 Conflict)**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A001",
    "message": "Email already exists"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 2.2. 공개 프로필 조회 (GET `/api/v1/users/{username}`)

**인증 필요**: ✅ (anyRequest().authenticated())

특정 사용자의 공개 프로필을 조회합니다.

**Request**
```http
GET /api/v1/users/johndoe
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`UserProfileResponse`)
```json
{
  "success": true,
  "data": {
    "id": 123,
    "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "user@example.com",
    "nickname": "John Doe",
    "username": "johndoe",
    "bio": "Software Developer",
    "profileImageUrl": "https://example.com/profile.jpg",
    "website": "https://johndoe.dev",
    "followerCount": 120,
    "followingCount": 80,
    "createdAt": "2025-12-01T00:00:00Z"
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Error Response (404 Not Found)**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A004",
    "message": "User not found"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 2.3. 내 프로필 조회 (GET `/api/v1/users/me`)

**인증 필요**: ✅

현재 로그인한 사용자의 프로필을 조회합니다.

**Request**
```http
GET /api/v1/users/me
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`UserProfileResponse`)
```json
{
  "success": true,
  "data": {
    "id": 123,
    "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "user@example.com",
    "nickname": "John Doe",
    "username": "johndoe",
    "bio": "Software Developer",
    "profileImageUrl": "https://example.com/profile.jpg",
    "website": "https://johndoe.dev",
    "followerCount": 120,
    "followingCount": 80,
    "createdAt": "2025-12-01T00:00:00Z"
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 2.4. 프로필 수정 (PUT `/api/v1/users/me/profile`)

**인증 필요**: ✅

프로필 정보(닉네임, 자기소개, 프로필 이미지, 웹사이트)를 수정합니다.

**Request**
```http
PUT /api/v1/users/me/profile
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "nickname": "John Updated",
  "bio": "Senior Software Developer",
  "profileImageUrl": "https://example.com/new-profile.jpg",
  "website": "https://johndoe.dev"
}
```

**Request Body** (`UserProfileUpdateRequest`)

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| `nickname` | string | ❌ | @Size(max=50) | 닉네임 |
| `bio` | string | ❌ | @Size(max=200) | 자기소개 |
| `profileImageUrl` | string | ❌ | - | 프로필 이미지 URL |
| `website` | string | ❌ | @URL | 웹사이트 URL |

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "uuid": "user-uuid-1234",
    "email": "user@example.com",
    "nickname": "UpdatedNick",
    "username": "johndoe",
    "bio": "Updated bio",
    "profileImageUrl": "https://example.com/new.jpg",
    "website": "https://example.com",
    "followerCount": 10,
    "followingCount": 5,
    "createdAt": "2026-01-15T09:00:00Z"
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 2.5. Username 설정 (POST `/api/v1/users/me/username`)

**인증 필요**: ✅

최초 1회 한정으로 username을 설정합니다.

**Request**
```http
POST /api/v1/users/me/username
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "username": "johndoe"
}
```

**Request Body** (`UsernameSetRequest`)

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| `username` | string | ✅ | @Pattern("^[a-z0-9_]{3,20}$") | 사용자명 (3~20자, 영문소문자/숫자/언더스코어) |

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "uuid": "user-uuid-1234",
    "email": "user@example.com",
    "nickname": "TestNick",
    "username": "johndoe",
    "bio": "Hello",
    "profileImageUrl": "https://example.com/pic.jpg",
    "website": "https://example.com",
    "followerCount": 10,
    "followingCount": 5,
    "createdAt": "2026-01-15T09:00:00Z"
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Error Response (409 Conflict) - 이미 설정된 경우**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A012",
    "message": "Username already set"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Error Response (409 Conflict) - 중복**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A011",
    "message": "Username already exists"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 2.6. Username 중복 확인 (GET `/api/v1/users/check-username/{username}`)

**인증 필요**: ✅ (anyRequest().authenticated())

Username 사용 가능 여부를 확인합니다.

**Request**
```http
GET /api/v1/users/check-username/johndoe
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`UsernameCheckResponse`)
```json
{
  "success": true,
  "data": {
    "username": "johndoe",
    "available": false
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 2.7. 비밀번호 변경 (PUT `/api/v1/users/me/password`)

**인증 필요**: ✅

현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.

**Request**
```http
PUT /api/v1/users/me/password
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!",
  "confirmPassword": "NewPassword456!"
}
```

**Request Body** (`PasswordChangeRequest`)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `currentPassword` | string | ✅ | 현재 비밀번호 |
| `newPassword` | string | ✅ | 새 비밀번호 |
| `confirmPassword` | string | ✅ | 새 비밀번호 확인 |

**Response (200 OK)**
```json
{
  "success": true,
  "data": "Password changed successfully",
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Error Response (401 Unauthorized) - 현재 비밀번호 불일치**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A007",
    "message": "Invalid current password"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Error Response (400 Bad Request) - 확인 불일치**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A008",
    "message": "Password mismatch"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**관련 에러 코드**
- `A006`: SOCIAL_USER_CANNOT_CHANGE_PASSWORD (소셜 로그인 사용자)
- `A020`: PASSWORD_TOO_SHORT
- `A021`: PASSWORD_TOO_WEAK
- `A022`: PASSWORD_RECENTLY_USED
- `A023`: PASSWORD_CONTAINS_USER_INFO
- `A025`: PASSWORD_TOO_LONG
- `A026`: PASSWORD_CONTAINS_SEQUENTIAL

---

## 📝 3. ProfileController (`/api/v1/profile`)

프로필 조회/수정, 비밀번호 변경, 계정 삭제 API.

### 3.1. 내 프로필 조회 (GET `/api/v1/profile/me`)

**인증 필요**: ✅

현재 로그인한 사용자의 상세 프로필을 조회합니다.

**Request**
```http
GET /api/v1/profile/me
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`ProfileResponse`)
```json
{
  "success": true,
  "data": {
    "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "user@example.com",
    "nickname": "John Doe",
    "realName": "John Doe",
    "phoneNumber": "010-1234-5678",
    "profileImageUrl": "https://example.com/profile.jpg",
    "marketingAgree": true,
    "hasSocialAccount": true,
    "socialProviders": ["GOOGLE", "KAKAO"],
    "createdAt": "2025-12-01T00:00:00Z"
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 3.2. 프로필 수정 (PATCH `/api/v1/profile`)

**인증 필요**: ✅

프로필 정보를 부분 수정하고 새 Access Token을 반환합니다.

**Request**
```http
PATCH /api/v1/profile
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "nickname": "Updated Nickname",
  "realName": "John Smith",
  "phoneNumber": "010-9876-5432",
  "profileImageUrl": "https://example.com/new.jpg",
  "marketingAgree": false
}
```

**Request Body** (`UpdateProfileRequest`)

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| `nickname` | string | ❌ | @Size(min=2, max=50) | 닉네임 |
| `realName` | string | ❌ | @Size(max=50) | 실명 |
| `phoneNumber` | string | ❌ | @Size(max=20) | 전화번호 |
| `profileImageUrl` | string | ❌ | @Size(max=255) | 프로필 이미지 URL |
| `marketingAgree` | boolean | ❌ | - | 마케팅 수신 동의 |

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "profile": {
      "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "email": "user@example.com",
      "nickname": "Updated Nickname",
      "realName": "John Smith",
      "phoneNumber": "010-9876-5432",
      "profileImageUrl": "https://example.com/new.jpg",
      "marketingAgree": false,
      "hasSocialAccount": true,
      "socialProviders": ["GOOGLE", "KAKAO"],
      "createdAt": "2025-12-01T00:00:00Z"
    },
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**참고**: 프로필 수정 시 새 Access Token이 발급되므로 클라이언트는 토큰을 갱신해야 합니다.

---

### 3.3. 비밀번호 변경 (POST `/api/v1/profile/password`)

**인증 필요**: ✅

현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.

**Request**
```http
POST /api/v1/profile/password
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!",
  "confirmPassword": "NewPassword456!"
}
```

**Request Body** (`ChangePasswordRequest`)

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| `currentPassword` | string | ✅ | @NotBlank | 현재 비밀번호 |
| `newPassword` | string | ✅ | @NotBlank @Size(min=8, max=100) | 새 비밀번호 |
| `confirmPassword` | string | ✅ | @NotBlank | 새 비밀번호 확인 |

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "message": "비밀번호가 변경되었습니다"
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Error Response**: UserController의 비밀번호 변경과 동일

---

### 3.4. 계정 삭제 (DELETE `/api/v1/profile/account`)

**인증 필요**: ✅

사용자 계정을 영구 삭제합니다. 비밀번호 확인 필요.

**Request**
```http
DELETE /api/v1/profile/account
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "password": "MyPassword123!",
  "reason": "No longer needed"
}
```

**Request Body** (`DeleteAccountRequest`)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `password` | string | ✅ | 현재 비밀번호 (확인용) |
| `reason` | string | ❌ | 탈퇴 사유 (선택) |

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "message": "회원 탈퇴가 완료되었습니다"
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Error Response (401 Unauthorized) - 비밀번호 불일치**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A009",
    "message": "Invalid password"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

## 👥 4. FollowController (`/api/v1/users`)

팔로우/팔로워 관리 API.

### 4.1. 팔로우 토글 (POST `/api/v1/users/{username}/follow`)

**인증 필요**: ✅

특정 사용자를 팔로우하거나 언팔로우합니다. (토글 방식)

**Request**
```http
POST /api/v1/users/johndoe/follow
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`FollowResponse`)
```json
{
  "success": true,
  "data": {
    "following": true,
    "followerCount": 121,
    "followingCount": 81
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Response Fields**

| 필드 | 타입 | 설명 |
|------|------|------|
| `following` | boolean | 팔로우 상태 (true: 팔로우, false: 언팔로우) |
| `followerCount` | int | 대상 사용자의 팔로워 수 |
| `followingCount` | int | 대상 사용자의 팔로잉 수 |

**Error Response (400 Bad Request) - 자기 자신 팔로우**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A016",
    "message": "Cannot follow yourself"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**관련 에러 코드**
- `A017`: FOLLOW_USER_NOT_FOUND

---

### 4.2. 팔로워 목록 조회 (GET `/api/v1/users/{username}/followers`)

**인증 필요**: ✅ (anyRequest().authenticated())

특정 사용자의 팔로워 목록을 조회합니다. (페이지네이션 지원)

**Request**
```http
GET /api/v1/users/johndoe/followers?page=1&size=20
Authorization: Bearer {accessToken}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | number | ❌ | 0 | 페이지 번호 (1부터 시작) |
| `size` | number | ❌ | 20 | 페이지당 항목 수 |

**Response (200 OK)** (`FollowListResponse`)
```json
{
  "success": true,
  "data": {
    "users": [
      {
        "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "username": "follower1",
        "nickname": "Follower One",
        "profileImageUrl": "https://example.com/profile1.jpg",
        "bio": "Bio text"
      },
      {
        "uuid": "b2c3d4e5-f6a7-8901-bcde-f1234567890a",
        "username": "follower2",
        "nickname": "Follower Two",
        "profileImageUrl": "https://example.com/profile2.jpg",
        "bio": "Another bio"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 120,
    "totalPages": 6,
    "hasNext": true
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 4.3. 팔로잉 목록 조회 (GET `/api/v1/users/{username}/following`)

**인증 필요**: ✅ (anyRequest().authenticated())

특정 사용자가 팔로우하는 사용자 목록을 조회합니다. (페이지네이션 지원)

**Request**
```http
GET /api/v1/users/johndoe/following?page=1&size=20
Authorization: Bearer {accessToken}
```

**Query Parameters**: 팔로워 목록과 동일

**Response**: 팔로워 목록과 동일한 구조 (`FollowListResponse`)

---

### 4.4. 내 팔로잉 ID 목록 (GET `/api/v1/users/me/following/ids`)

**인증 필요**: ✅

현재 로그인한 사용자가 팔로우하는 모든 사용자의 UUID 목록을 조회합니다. (클라이언트 캐싱용)

**Request**
```http
GET /api/v1/users/me/following/ids
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`FollowingIdsResponse`)
```json
{
  "success": true,
  "data": {
    "followingIds": [
      "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "b2c3d4e5-f6a7-8901-bcde-f1234567890a",
      "c3d4e5f6-a7b8-9012-cdef-1234567890ab"
    ]
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 4.5. 팔로우 상태 확인 (GET `/api/v1/users/{username}/follow/status`)

**인증 필요**: ✅ (anyRequest().authenticated())

현재 로그인한 사용자가 특정 사용자를 팔로우 중인지 확인합니다.

**Request**
```http
GET /api/v1/users/johndoe/follow/status
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`FollowStatusResponse`)
```json
{
  "success": true,
  "data": {
    "isFollowing": true
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

## 🔑 5. RbacAdminController (`/api/v1/admin/rbac`)

**인증 필요**: ✅
**권한 필요**: `ROLE_SUPER_ADMIN`

RBAC (Role-Based Access Control) 관리 API. 역할 CRUD(조회/생성/수정/상태변경), 역할-권한 관리, 사용자 역할/권한 조회, 역할 부여/회수를 담당합니다.

### 5.1. 전체 역할 조회 (GET `/api/v1/admin/rbac/roles`)

시스템의 모든 역할을 조회합니다.

**Request**
```http
GET /api/v1/admin/rbac/roles
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`List<RoleResponse>`)
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "roleKey": "ROLE_SUPER_ADMIN",
      "displayName": "Super Administrator",
      "description": "Full system access",
      "serviceScope": "SYSTEM",
      "membershipGroup": null,
      "includedRoleKeys": [],
      "system": true,
      "active": true
    },
    {
      "id": 2,
      "roleKey": "ROLE_BLOG_ADMIN",
      "displayName": "Blog Administrator",
      "description": "Blog management access",
      "serviceScope": "BLOG",
      "membershipGroup": null,
      "includedRoleKeys": [],
      "system": false,
      "active": true
    },
    {
      "id": 3,
      "roleKey": "ROLE_SHOPPING_ADMIN",
      "displayName": "Shopping Administrator",
      "description": "Shopping service management access",
      "serviceScope": "SHOPPING",
      "membershipGroup": null,
      "includedRoleKeys": [],
      "system": false,
      "active": true
    }
  ],
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 5.2. 사용자 역할 조회 (GET `/api/v1/admin/rbac/users/{userId}/roles`)

특정 사용자에게 부여된 역할을 조회합니다.

**Request**
```http
GET /api/v1/admin/rbac/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890/roles
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`List<UserRoleResponse>`)
```json
{
  "success": true,
  "data": [
    {
      "id": 101,
      "roleKey": "ROLE_BLOG_ADMIN",
      "displayName": "Blog Administrator",
      "assignedBy": "admin@example.com",
      "assignedAt": "2026-01-15T10:00:00Z",
      "expiresAt": null
    }
  ],
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 5.3. 사용자 권한 조회 (GET `/api/v1/admin/rbac/users/{userId}/permissions`)

특정 사용자가 가진 모든 권한(역할에서 파생된 권한)을 조회합니다.

**Request**
```http
GET /api/v1/admin/rbac/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890/permissions
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`UserPermissionsResponse`)
```json
{
  "success": true,
  "data": {
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "roles": ["ROLE_BLOG_ADMIN"],
    "permissions": [
      "blog:post:create",
      "blog:post:update",
      "blog:post:delete",
      "blog:comment:moderate"
    ],
    "memberships": {
      "user:blog": {"tier": "PRO", "order": 2},
      "user:shopping": {"tier": "FREE", "order": 0}
    }
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 5.4. 역할 부여 (POST `/api/v1/admin/rbac/roles/assign`)

특정 사용자에게 역할을 부여합니다.

**Request**
```http
POST /api/v1/admin/rbac/roles/assign
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "roleKey": "ROLE_BLOG_ADMIN",
  "expiresAt": null
}
```

**Request Body** (`AssignRoleRequest`)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `userId` | string | ✅ | 대상 사용자 UUID |
| `roleKey` | string | ✅ | 부여할 역할 키 (예: ROLE_BLOG_ADMIN) |
| `expiresAt` | string | ❌ | 만료 시각 (ISO 8601, nullable) |

**Response (201 Created)**
```json
{
  "success": true,
  "data": "Role assigned successfully",
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Error Response (409 Conflict) - 이미 역할 보유**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A031",
    "message": "Role already assigned"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**관련 에러 코드**
- `A030`: ROLE_NOT_FOUND

---

### 5.5. 역할 회수 (DELETE `/api/v1/admin/rbac/users/{userId}/roles/{roleKey}`)

특정 사용자로부터 역할을 회수합니다.

**Request**
```http
DELETE /api/v1/admin/rbac/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890/roles/ROLE_BLOG_ADMIN
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": "Role revoked successfully",
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Error Response (404 Not Found)**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A032",
    "message": "Role not assigned"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**관련 에러 코드**
- `A033`: SYSTEM_ROLE_CANNOT_BE_MODIFIED

---

### 5.6. 대시보드 통계 조회 (GET `/api/v1/admin/rbac/dashboard`)

Admin Dashboard에 표시할 통합 통계 데이터를 반환합니다.

**Request**
```http
GET /api/v1/admin/rbac/dashboard
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`DashboardStatsResponse`)
```json
{
  "success": true,
  "data": {
    "users": {
      "total": 2,
      "byStatus": { "ACTIVE": 2, "DORMANT": 0, "BANNED": 0, "WITHDRAWAL_PENDING": 0 }
    },
    "roles": {
      "total": 5,
      "systemCount": 5,
      "assignments": [
        { "roleKey": "ROLE_SUPER_ADMIN", "displayName": "Super Administrator", "userCount": 1 },
        { "roleKey": "ROLE_USER", "displayName": "User", "userCount": 2 }
      ]
    },
    "memberships": {
      "groups": [
        {
          "group": "user:shopping",
          "activeCount": 2,
          "tiers": [
            { "tierKey": "FREE", "displayName": "Free", "count": 2 },
            { "tierKey": "BASIC", "displayName": "Basic", "count": 0 }
          ]
        }
      ]
    },
    "sellers": { "pending": 0, "approved": 0, "rejected": 0 },
    "recentActivity": [
      {
        "eventType": "ROLE_ASSIGNED",
        "targetUserId": "uuid-...",
        "actorUserId": "SYSTEM_INIT",
        "details": "Role assigned: ROLE_SUPER_ADMIN",
        "createdAt": "2026-02-07T10:00:00"
      }
    ]
  }
}
```

---

### 5.7. 전체 감사 로그 조회 (GET `/api/v1/admin/rbac/audit`)

전체 감사 로그를 페이징으로 조회합니다.

**Request**
```http
GET /api/v1/admin/rbac/audit?page=1&size=20
Authorization: Bearer {accessToken}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | number | ❌ | 0 | 페이지 번호 |
| `size` | number | ❌ | 20 | 페이지당 항목 수 |

**Response (200 OK)** (Spring Page of `AuditLogResponse`)
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "eventType": "ROLE_ASSIGNED",
        "targetUserId": "uuid-...",
        "actorUserId": "SYSTEM_INIT",
        "details": "Role assigned: ROLE_SUPER_ADMIN",
        "ipAddress": null,
        "createdAt": "2026-02-07T10:00:00"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 10,
    "totalPages": 1
  }
}
```

---

### 5.8. 사용자별 감사 로그 조회 (GET `/api/v1/admin/rbac/users/{userId}/audit`)

특정 사용자의 감사 로그를 페이징으로 조회합니다.

**Request**
```http
GET /api/v1/admin/rbac/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890/audit?page=1&size=20
Authorization: Bearer {accessToken}
```

**Query Parameters**: 전체 감사 로그 조회와 동일

**Response**: 전체 감사 로그 조회와 동일한 구조 (해당 userId로 필터링)

---

### 5.9. 사용자 검색 (GET `/api/v1/admin/rbac/users`)

사용자를 검색합니다. query가 비어있으면 전체 목록, UUID 패턴이면 exact match, 그 외 email/username/nickname LIKE 검색.

**Request**
```http
GET /api/v1/admin/rbac/users?query=admin&page=1&size=20
Authorization: Bearer {accessToken}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `query` | string | ❌ | "" | 검색어 (email, username, nickname LIKE 또는 UUID exact match) |
| `page` | number | ❌ | 0 | 페이지 번호 |
| `size` | number | ❌ | 20 | 페이지당 항목 수 |

**Response (200 OK)** (Spring Page of `AdminUserResponse`)
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "uuid": "be83af82-b5b6-4384-8c42-45e778159e09",
        "email": "admin@test.com",
        "username": null,
        "nickname": "관리자",
        "profileImageUrl": null,
        "status": "ACTIVE",
        "createdAt": "2026-02-07T15:31:25.582822",
        "lastLoginAt": null
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

**Response Fields** (`AdminUserResponse`)

| 필드 | 타입 | 설명 |
|------|------|------|
| `uuid` | string | 사용자 UUID |
| `email` | string | 이메일 주소 |
| `username` | string? | 사용자명 (미설정 시 null) |
| `nickname` | string? | 닉네임 |
| `profileImageUrl` | string? | 프로필 이미지 URL |
| `status` | string | 상태 (ACTIVE, DORMANT, BANNED, WITHDRAWAL_PENDING) |
| `createdAt` | string | 가입일 (ISO 8601) |
| `lastLoginAt` | string? | 최근 로그인 (ISO 8601, null 가능) |

---

### 5.10. 역할 상세 조회 (GET `/api/v1/admin/rbac/roles/{roleKey}`)

역할 상세 정보를 권한 목록과 함께 조회합니다.

**Request**
```http
GET /api/v1/admin/rbac/roles/ROLE_BLOG_ADMIN
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`RoleDetailResponse`)
```json
{
  "success": true,
  "data": {
    "id": 2,
    "roleKey": "ROLE_BLOG_ADMIN",
    "displayName": "Blog Administrator",
    "description": "Blog management access",
    "serviceScope": "BLOG",
    "membershipGroup": null,
    "includedRoleKeys": [],
    "system": false,
    "active": true,
    "createdAt": "2026-01-01T00:00:00Z",
    "updatedAt": "2026-01-15T00:00:00Z",
    "permissions": [
      {
        "id": 10,
        "permissionKey": "blog:post:create",
        "service": "BLOG",
        "resource": "post",
        "action": "create",
        "description": "Create blog posts",
        "active": true
      },
      {
        "id": 11,
        "permissionKey": "blog:post:update",
        "service": "BLOG",
        "resource": "post",
        "action": "update",
        "description": "Update blog posts",
        "active": true
      }
    ]
  },
  "error": null,
  "timestamp": "2026-02-07T10:30:00Z"
}
```

**Response Fields** (`PermissionResponse`)

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | number | 권한 ID |
| `permissionKey` | string | 권한 키 (예: blog:post:create) |
| `service` | string | 서비스 (BLOG, SHOPPING 등) |
| `resource` | string | 리소스 (post, comment 등) |
| `action` | string | 액션 (create, update, delete 등) |
| `description` | string | 권한 설명 |
| `active` | boolean | 활성 상태 |

**Error Response (404 Not Found)**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A030",
    "message": "Role not found"
  },
  "timestamp": "2026-02-07T10:30:00Z"
}
```

---

### 5.11. 역할 생성 (POST `/api/v1/admin/rbac/roles`)

새 역할을 생성합니다.

**Request**
```http
POST /api/v1/admin/rbac/roles
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "roleKey": "ROLE_BLOG_MODERATOR",
  "displayName": "Blog Moderator",
  "description": "Moderate blog comments and posts",
  "serviceScope": "BLOG",
  "membershipGroup": null,
  "includedRoleKeys": ["ROLE_USER"]
}
```

**Request Body** (`CreateRoleRequest`)

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| `roleKey` | string | ✅ | @NotBlank | 역할 키 (예: ROLE_BLOG_MODERATOR) |
| `displayName` | string | ✅ | @NotBlank | 표시 이름 |
| `description` | string | ❌ | - | 역할 설명 |
| `serviceScope` | string | ❌ | - | 서비스 범위 (SYSTEM, BLOG, SHOPPING 등) |
| `membershipGroup` | string | ❌ | - | 멤버십 그룹 (user:blog 등) |
| `includedRoleKeys` | string[] | ❌ | - | 포함할 역할 키 목록 (DAG 계층 구조) |

**Response (201 Created)** (`RoleResponse`)
```json
{
  "success": true,
  "data": {
    "id": 10,
    "roleKey": "ROLE_BLOG_MODERATOR",
    "displayName": "Blog Moderator",
    "description": "Moderate blog comments and posts",
    "serviceScope": "BLOG",
    "membershipGroup": null,
    "includedRoleKeys": ["ROLE_USER"],
    "system": false,
    "active": true
  },
  "error": null,
  "timestamp": "2026-02-07T10:30:00Z"
}
```

**Error Response (409 Conflict) - 역할 키 중복**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A039",
    "message": "Role key already exists"
  },
  "timestamp": "2026-02-07T10:30:00Z"
}
```

**Error Response (404 Not Found) - 부모 역할 없음**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A030",
    "message": "Parent role not found"
  },
  "timestamp": "2026-02-07T10:30:00Z"
}
```

---

### 5.12. 역할 수정 (PUT `/api/v1/admin/rbac/roles/{roleKey}`)

역할의 표시 이름과 설명을 수정합니다.

**Request**
```http
PUT /api/v1/admin/rbac/roles/ROLE_BLOG_MODERATOR
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "displayName": "Blog Content Moderator",
  "description": "Moderate all blog content including posts and comments"
}
```

**Request Body** (`UpdateRoleRequest`)

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| `displayName` | string | ✅ | @NotBlank | 표시 이름 |
| `description` | string | ❌ | - | 역할 설명 |

**Response (200 OK)** (`RoleResponse`)
```json
{
  "success": true,
  "data": {
    "id": 10,
    "roleKey": "ROLE_BLOG_MODERATOR",
    "displayName": "Blog Content Moderator",
    "description": "Moderate all blog content including posts and comments",
    "serviceScope": "BLOG",
    "membershipGroup": null,
    "includedRoleKeys": ["ROLE_USER"],
    "system": false,
    "active": true
  },
  "error": null,
  "timestamp": "2026-02-07T10:30:00Z"
}
```

**Error Response (404 Not Found)**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A030",
    "message": "Role not found"
  },
  "timestamp": "2026-02-07T10:30:00Z"
}
```

---

### 5.13. 역할 활성/비활성 (PATCH `/api/v1/admin/rbac/roles/{roleKey}/status`)

역할을 활성화하거나 비활성화합니다.

**Request**
```http
PATCH /api/v1/admin/rbac/roles/ROLE_BLOG_MODERATOR/status?active=false
Authorization: Bearer {accessToken}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `active` | boolean | ✅ | true: 활성화, false: 비활성화 |

**Response (200 OK)** (`RoleResponse`)
```json
{
  "success": true,
  "data": {
    "id": 10,
    "roleKey": "ROLE_BLOG_MODERATOR",
    "displayName": "Blog Content Moderator",
    "description": "Moderate all blog content including posts and comments",
    "serviceScope": "BLOG",
    "membershipGroup": null,
    "includedRoleKeys": ["ROLE_USER"],
    "system": false,
    "active": false
  },
  "error": null,
  "timestamp": "2026-02-07T10:30:00Z"
}
```

**Error Response (404 Not Found)**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A030",
    "message": "Role not found"
  },
  "timestamp": "2026-02-07T10:30:00Z"
}
```

**Error Response (400 Bad Request) - 시스템 역할 비활성화**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A033",
    "message": "System role cannot be modified"
  },
  "timestamp": "2026-02-07T10:30:00Z"
}
```

---

### 5.14. 역할 권한 조회 (GET `/api/v1/admin/rbac/roles/{roleKey}/permissions`)

특정 역할에 할당된 권한 목록을 조회합니다.

**Request**
```http
GET /api/v1/admin/rbac/roles/ROLE_BLOG_ADMIN/permissions
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`List<PermissionResponse>`)
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "permissionKey": "blog:post:create",
      "service": "BLOG",
      "resource": "post",
      "action": "create",
      "description": "Create blog posts",
      "active": true
    },
    {
      "id": 11,
      "permissionKey": "blog:post:update",
      "service": "BLOG",
      "resource": "post",
      "action": "update",
      "description": "Update blog posts",
      "active": true
    }
  ],
  "error": null,
  "timestamp": "2026-02-07T10:30:00Z"
}
```

**Error Response (404 Not Found)**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A030",
    "message": "Role not found"
  },
  "timestamp": "2026-02-07T10:30:00Z"
}
```

---

### 5.15. 역할에 권한 할당 (POST `/api/v1/admin/rbac/roles/{roleKey}/permissions`)

특정 역할에 권한을 할당합니다.

**Request**
```http
POST /api/v1/admin/rbac/roles/ROLE_BLOG_ADMIN/permissions?permissionKey=blog:comment:moderate
Authorization: Bearer {accessToken}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `permissionKey` | string | ✅ | 할당할 권한 키 (예: blog:comment:moderate) |

**Response (201 Created)**
```json
{
  "success": true,
  "data": "Permission assigned to role successfully",
  "error": null,
  "timestamp": "2026-02-07T10:30:00Z"
}
```

**Error Response (404 Not Found) - 역할 없음**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A030",
    "message": "Role not found"
  },
  "timestamp": "2026-02-07T10:30:00Z"
}
```

**Error Response (404 Not Found) - 권한 없음**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A034",
    "message": "Permission not found"
  },
  "timestamp": "2026-02-07T10:30:00Z"
}
```

---

### 5.16. 역할에서 권한 해제 (DELETE `/api/v1/admin/rbac/roles/{roleKey}/permissions/{permissionKey}`)

특정 역할에서 권한을 해제합니다.

**Request**
```http
DELETE /api/v1/admin/rbac/roles/ROLE_BLOG_ADMIN/permissions/blog:comment:moderate
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": "Permission removed from role successfully",
  "error": null,
  "timestamp": "2026-02-07T10:30:00Z"
}
```

**Error Response (404 Not Found) - 역할 없음**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A030",
    "message": "Role not found"
  },
  "timestamp": "2026-02-07T10:30:00Z"
}
```

**Error Response (404 Not Found) - 권한 없음**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A034",
    "message": "Permission not found"
  },
  "timestamp": "2026-02-07T10:30:00Z"
}
```

---

### 5.17. 전체 권한 목록 (GET `/api/v1/admin/rbac/permissions`)

시스템의 모든 활성 권한을 조회합니다. (드롭다운용)

**Request**
```http
GET /api/v1/admin/rbac/permissions
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`List<PermissionResponse>`)
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "permissionKey": "blog:post:create",
      "service": "BLOG",
      "resource": "post",
      "action": "create",
      "description": "Create blog posts",
      "active": true
    },
    {
      "id": 11,
      "permissionKey": "blog:post:update",
      "service": "BLOG",
      "resource": "post",
      "action": "update",
      "description": "Update blog posts",
      "active": true
    },
    {
      "id": 20,
      "permissionKey": "shopping:product:create",
      "service": "SHOPPING",
      "resource": "product",
      "action": "create",
      "description": "Create products",
      "active": true
    }
  ],
  "error": null,
  "timestamp": "2026-02-07T10:30:00Z"
}
```

---

## 🔓 6. PermissionController (`/api/v1/permissions`)

### 6.1. 내 권한 조회 (GET `/api/v1/permissions/me`)

**인증 필요**: ✅

현재 로그인한 사용자가 가진 모든 권한을 조회합니다.

**Request**
```http
GET /api/v1/permissions/me
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`UserPermissionsResponse`)
```json
{
  "success": true,
  "data": {
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "roles": ["ROLE_USER", "ROLE_BLOG_ADMIN"],
    "permissions": [
      "blog:post:create",
      "blog:post:update",
      "blog:post:delete",
      "blog:comment:moderate"
    ],
    "memberships": {
      "user:blog": {"tier": "PRO", "order": 2},
      "user:shopping": {"tier": "FREE", "order": 0}
    }
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

## 💎 7. MembershipController (`/api/v1/memberships`)

멤버십 조회, 변경, 취소 API.

### 7.1. 내 멤버십 전체 조회 (GET `/api/v1/memberships/me`)

**인증 필요**: ✅

현재 로그인한 사용자의 모든 서비스별 멤버십을 조회합니다.

**Request**
```http
GET /api/v1/memberships/me
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`List<MembershipResponse>`)
```json
{
  "success": true,
  "data": [
    {
      "id": 1001,
      "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "membershipGroup": "user:blog",
      "tierKey": "PREMIUM",
      "tierDisplayName": "Premium",
      "status": "ACTIVE",
      "autoRenew": true,
      "startedAt": "2026-01-01T00:00:00Z",
      "expiresAt": "2026-02-01T00:00:00Z",
      "createdAt": "2026-01-01T00:00:00Z"
    },
    {
      "id": 1002,
      "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "membershipGroup": "user:shopping",
      "tierKey": "FREE",
      "tierDisplayName": "Free",
      "status": "ACTIVE",
      "autoRenew": false,
      "startedAt": "2025-12-15T00:00:00Z",
      "expiresAt": null,
      "createdAt": "2025-12-15T00:00:00Z"
    }
  ],
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**MembershipStatus 값**: `ACTIVE`, `EXPIRED`, `CANCELLED`

---

### 7.2. 특정 그룹 멤버십 조회 (GET `/api/v1/memberships/me/{membershipGroup}`)

**인증 필요**: ✅

특정 멤버십 그룹의 멤버십 정보를 조회합니다.

**Request**
```http
GET /api/v1/memberships/me/user:blog
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`MembershipResponse`)
```json
{
  "success": true,
  "data": {
    "id": 1001,
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "membershipGroup": "user:blog",
    "tierKey": "PREMIUM",
    "tierDisplayName": "Premium",
    "status": "ACTIVE",
    "autoRenew": true,
    "startedAt": "2026-01-01T00:00:00Z",
    "expiresAt": "2026-02-01T00:00:00Z",
    "createdAt": "2026-01-01T00:00:00Z"
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Error Response (404 Not Found)**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A035",
    "message": "Membership not found"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 7.3. 멤버십 그룹별 티어 조회 (GET `/api/v1/memberships/tiers/{membershipGroup}`)

**인증 필요**: ❌

특정 멤버십 그룹의 이용 가능한 티어 목록을 조회합니다. (공개 API)

**Request**
```http
GET /api/v1/memberships/tiers/user:blog
```

**Response (200 OK)** (`List<MembershipTierResponse>`)
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "membershipGroup": "user:blog",
      "tierKey": "FREE",
      "displayName": "Free",
      "priceMonthly": 0,
      "priceYearly": 0,
      "sortOrder": 1
    },
    {
      "id": 2,
      "membershipGroup": "user:blog",
      "tierKey": "PREMIUM",
      "displayName": "Premium",
      "priceMonthly": 9900,
      "priceYearly": 99000,
      "sortOrder": 2
    },
    {
      "id": 3,
      "membershipGroup": "user:blog",
      "tierKey": "PRO",
      "displayName": "Pro",
      "priceMonthly": 19900,
      "priceYearly": 199000,
      "sortOrder": 3
    }
  ],
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 7.4. 멤버십 변경 (PUT `/api/v1/memberships/me`)

**인증 필요**: ✅

특정 서비스의 멤버십 티어를 변경합니다.

**Request**
```http
PUT /api/v1/memberships/me
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "membershipGroup": "user:blog",
  "tierKey": "PRO"
}
```

**Request Body** (`ChangeMembershipRequest`)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `membershipGroup` | string | ✅ | 멤버십 그룹 (user:blog, user:shopping, seller:shopping) |
| `tierKey` | string | ✅ | 변경할 티어 (FREE, PREMIUM, PRO) |

**Response (200 OK)** (`MembershipResponse`)
```json
{
  "success": true,
  "data": {
    "id": 1001,
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "membershipGroup": "user:blog",
    "tierKey": "PRO",
    "tierDisplayName": "Pro",
    "status": "ACTIVE",
    "autoRenew": true,
    "startedAt": "2026-02-06T10:30:00Z",
    "expiresAt": "2026-03-06T10:30:00Z",
    "createdAt": "2026-01-01T00:00:00Z"
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Error Response (404 Not Found)**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A036",
    "message": "Membership tier not found"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**관련 에러 코드**
- `A037`: MEMBERSHIP_ALREADY_EXISTS
- `A038`: MEMBERSHIP_EXPIRED

---

### 7.5. 멤버십 취소 (DELETE `/api/v1/memberships/me/{membershipGroup}`)

**인증 필요**: ✅

특정 멤버십 그룹의 멤버십을 취소합니다. (FREE 티어로 전환)

**Request**
```http
DELETE /api/v1/memberships/me/user:blog
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": "Membership cancelled successfully",
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

## 💎 8. MembershipAdminController (`/api/v1/admin/memberships`)

**인증 필요**: ✅
**권한 필요**: `ROLE_SUPER_ADMIN`

관리자용 멤버십 관리 API.

### 8.1. 멤버십 그룹 목록 조회 (GET `/api/v1/admin/memberships/groups`)

활성 멤버십 그룹 목록을 조회합니다.

**Request**
```http
GET /api/v1/admin/memberships/groups
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": ["seller:shopping", "user:blog", "user:shopping"],
  "error": null,
  "timestamp": "2026-02-07T10:00:00Z"
}
```

---

### 8.2. 사용자 멤버십 조회 (GET `/api/v1/admin/memberships/users/{userId}`)

특정 사용자의 모든 멤버십을 조회합니다. 응답에는 `autoRenew`, `startedAt`, `expiresAt`, `createdAt` 필드가 포함됩니다.

**Request**
```http
GET /api/v1/admin/memberships/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
Authorization: Bearer {accessToken}
```

**Response**: MembershipController의 `/api/v1/memberships/me`와 동일한 구조

---

### 8.3. 사용자 멤버십 변경 (PUT `/api/v1/admin/memberships/users/{userId}`)

관리자가 특정 사용자의 멤버십을 강제로 변경합니다.

**Request**
```http
PUT /api/v1/admin/memberships/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "membershipGroup": "user:blog",
  "tierKey": "PRO"
}
```

**Request Body**: `ChangeMembershipRequest`와 동일

**Response (200 OK)** (`MembershipResponse`)
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "membershipGroup": "user:blog",
    "tierKey": "PRO",
    "tierDisplayName": "프로",
    "status": "ACTIVE",
    "autoRenew": true,
    "startedAt": "2026-02-01T00:00:00",
    "expiresAt": "2027-02-01T00:00:00",
    "createdAt": "2026-02-01T00:00:00"
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

## 🛒 9. SellerController (`/api/v1/seller`)

셀러(판매자) 신청 API.

### 9.1. 셀러 신청 (POST `/api/v1/seller/apply`)

**인증 필요**: ✅

판매자 자격을 신청합니다.

**Request**
```http
POST /api/v1/seller/apply
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "businessName": "My Shop",
  "businessNumber": "123-45-67890",
  "reason": "I want to sell handmade goods"
}
```

**Request Body** (`SellerApplicationRequest`)

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| `businessName` | string | ✅ | @NotBlank @Size(max=200) | 사업자명 (또는 상호) |
| `businessNumber` | string | ❌ | @Size(max=50) | 사업자 등록번호 |
| `reason` | string | ❌ | - | 신청 사유 |

**Response (201 Created)** (`SellerApplicationResponse`)
```json
{
  "success": true,
  "data": {
    "id": 456,
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "businessName": "My Shop",
    "businessNumber": "123-45-67890",
    "reason": "I want to sell handmade goods",
    "status": "PENDING",
    "reviewedBy": null,
    "reviewComment": null,
    "reviewedAt": null,
    "createdAt": "2026-02-06T10:30:00Z"
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**SellerApplicationStatus 값**: `PENDING`, `APPROVED`, `REJECTED`

**Error Response (409 Conflict) - 이미 신청 존재**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A040",
    "message": "Seller application already pending"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 9.2. 내 신청 상태 조회 (GET `/api/v1/seller/application`)

**인증 필요**: ✅

현재 로그인한 사용자의 셀러 신청 상태를 조회합니다.

**Request**
```http
GET /api/v1/seller/application
Authorization: Bearer {accessToken}
```

**Response (200 OK)** (`SellerApplicationResponse`)
```json
{
  "success": true,
  "data": {
    "id": 456,
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "businessName": "My Shop",
    "businessNumber": "123-45-67890",
    "reason": "I want to sell handmade goods",
    "status": "PENDING",
    "reviewedBy": null,
    "reviewComment": null,
    "reviewedAt": null,
    "createdAt": "2026-02-06T10:30:00Z"
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

**Error Response (404 Not Found)**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A041",
    "message": "Seller application not found"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

## 🛒 10. SellerAdminController (`/api/v1/admin/seller`)

**인증 필요**: ✅
**권한 필요**: `ROLE_SHOPPING_ADMIN` 또는 `ROLE_SUPER_ADMIN`

셀러 신청 승인/거부 관리 API.

### 10.1. 대기 중인 신청 조회 (GET `/api/v1/admin/seller/applications/pending`)

승인 대기 중인 셀러 신청 목록을 조회합니다. (페이지네이션)

**Request**
```http
GET /api/v1/admin/seller/applications/pending?page=1&size=20
Authorization: Bearer {accessToken}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | number | ❌ | 0 | 페이지 번호 |
| `size` | number | ❌ | 20 | 페이지당 항목 수 |

**Response (200 OK)** (Spring Page)
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 456,
        "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "businessName": "My Shop",
        "businessNumber": "123-45-67890",
        "reason": "I want to sell handmade goods",
        "status": "PENDING",
        "reviewedBy": null,
        "reviewComment": null,
        "reviewedAt": null,
        "createdAt": "2026-02-06T10:30:00Z"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3
  },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

### 10.2. 전체 신청 조회 (GET `/api/v1/admin/seller/applications`)

모든 셀러 신청 목록을 조회합니다. (페이지네이션)

**Request**
```http
GET /api/v1/admin/seller/applications?page=1&size=20
Authorization: Bearer {accessToken}
```

**Query Parameters**: 대기 중인 신청 조회와 동일

**Response**: 대기 중인 신청 조회와 동일한 구조 (단, status가 PENDING/APPROVED/REJECTED 모두 포함)

---

### 10.3. 신청 심사 (POST `/api/v1/admin/seller/applications/{applicationId}/review`)

셀러 신청을 승인하거나 거부합니다.

**Request**
```http
POST /api/v1/admin/seller/applications/456/review
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "approved": true,
  "reviewComment": "Approved - Valid business"
}
```

**Request Body** (`SellerApplicationReviewRequest`)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `approved` | boolean | ✅ | true: 승인, false: 거부 |
| `reviewComment` | string | ❌ | 심사 코멘트 (거부 시 권장) |

**Response (200 OK) - 승인** (`SellerApplicationResponse`)
```json
{
  "success": true,
  "data": {
    "id": 456,
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "businessName": "My Shop",
    "businessNumber": "123-45-67890",
    "reason": "I want to sell handmade goods",
    "status": "APPROVED",
    "reviewedBy": "admin@example.com",
    "reviewComment": "Approved - Valid business",
    "reviewedAt": "2026-02-06T11:00:00Z",
    "createdAt": "2026-02-06T10:30:00Z"
  },
  "error": null,
  "timestamp": "2026-02-06T11:00:00Z"
}
```

**Response (200 OK) - 거부**
```json
{
  "success": true,
  "data": {
    "id": 456,
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "businessName": "My Shop",
    "businessNumber": "123-45-67890",
    "reason": "I want to sell handmade goods",
    "status": "REJECTED",
    "reviewedBy": "admin@example.com",
    "reviewComment": "Invalid business number",
    "reviewedAt": "2026-02-06T11:00:00Z",
    "createdAt": "2026-02-06T10:30:00Z"
  },
  "error": null,
  "timestamp": "2026-02-06T11:00:00Z"
}
```

**Error Response (400 Bad Request) - 이미 처리됨**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A042",
    "message": "Seller application already processed"
  },
  "timestamp": "2026-02-06T11:00:00Z"
}
```

---

## 🔧 11. RoleHierarchyController (`/api/v1/internal/role-hierarchy`)

**인증 필요**: ❌ (permitAll, Gateway 전용 내부 API)

Gateway가 JWT의 역할 목록을 계층적으로 확장하기 위한 내부 API. Gateway 라우트에 노출되지 않으므로 외부 접근 불가.

### 11.1. 유효 역할 조회 (GET `/api/v1/internal/role-hierarchy/effective-roles`)

역할 키 목록을 받아 계층적으로 확장된 전체 유효 역할을 반환합니다.

**Request**
```http
GET /api/v1/internal/role-hierarchy/effective-roles?roles=ROLE_SHOPPING_SELLER,ROLE_USER
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `roles` | string | ✅ | 쉼표 구분 역할 키 목록 |

**Response (200 OK)** (`List<String>`)
```json
{
  "success": true,
  "data": ["ROLE_SHOPPING_SELLER", "ROLE_USER", "ROLE_SHOPPING_BUYER"],
  "error": null,
  "timestamp": "2026-02-07T10:30:00Z"
}
```

**동작**: `role_includes` DAG를 BFS 탐색하여 역할이 포함하는 모든 하위 역할을 확장합니다. JWT `effectiveRoles` claim에 내장되어 Gateway에서 직접 사용됩니다.

---

## 🔐 12. OAuth2 소셜 로그인

Spring OAuth2 Client를 사용한 소셜 로그인 지원.

### 12.1. 소셜 로그인 시작 (GET `/oauth2/authorization/{provider}`)

**인증 필요**: ❌

소셜 프로바이더의 로그인 페이지로 리다이렉트합니다.

**Request**
```http
GET /oauth2/authorization/google
GET /oauth2/authorization/kakao
GET /oauth2/authorization/naver
```

**지원 프로바이더**: `google`, `kakao`, `naver`

**Response**: 소셜 프로바이더의 로그인 페이지로 리다이렉트

---

### 12.2. OAuth2 콜백 (GET `/login/oauth2/code/{provider}`)

**인증 필요**: ❌

소셜 로그인 후 콜백을 처리하고 JWT 토큰을 발급합니다.

**Request**
```http
GET /login/oauth2/code/google?code=AUTHORIZATION_CODE&state=RANDOM_STATE
```

**Response**: 성공 시 JWT Access Token과 Refresh Token 발급 (AuthController의 로그인과 동일)

**Cookie**: `portal_refresh_token` 쿠키에 Refresh Token 저장

---

## 📦 API Response Format

모든 RESTful API는 통일된 `ApiResponse` wrapper를 사용합니다.

### Success Response

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-02-06T10:30:00Z"
}
```

### Error Response

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A001",
    "message": "Email already exists"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

### Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | boolean | 성공 여부 |
| `data` | object/array/string/null | 응답 데이터 (성공 시) |
| `error` | object/null | 에러 정보 (실패 시) |
| `error.code` | string | 에러 코드 (예: A001) |
| `error.message` | string | 에러 메시지 |
| `timestamp` | string | 응답 타임스탬프 (ISO 8601) |

---

## ⚠️ Error Codes

### Auth Service Errors

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `A001` | 409 Conflict | EMAIL_ALREADY_EXISTS |
| `A002` | 401 Unauthorized | INVALID_CREDENTIALS |
| `A003` | 401 Unauthorized | INVALID_REFRESH_TOKEN |
| `A004` | 404 Not Found | USER_NOT_FOUND |
| `A005` | 401 Unauthorized | INVALID_TOKEN |
| `A006` | 400 Bad Request | SOCIAL_USER_CANNOT_CHANGE_PASSWORD |
| `A007` | 401 Unauthorized | INVALID_CURRENT_PASSWORD |
| `A008` | 400 Bad Request | PASSWORD_MISMATCH (확인 불일치) |
| `A009` | 401 Unauthorized | INVALID_PASSWORD (탈퇴 시) |
| `A011` | 409 Conflict | USERNAME_ALREADY_EXISTS |
| `A012` | 400 Bad Request | USERNAME_ALREADY_SET |
| `A013` | 400 Bad Request | INVALID_USERNAME_FORMAT |
| `A014` | 409 Conflict | ALREADY_FOLLOWING |
| `A015` | 404 Not Found | NOT_FOLLOWING |
| `A016` | 400 Bad Request | CANNOT_FOLLOW_YOURSELF |
| `A017` | 404 Not Found | FOLLOW_USER_NOT_FOUND |
| `A018` | 429 Too Many Requests | ACCOUNT_TEMPORARILY_LOCKED |
| `A019` | 429 Too Many Requests | TOO_MANY_LOGIN_ATTEMPTS |
| `A020` | 400 Bad Request | PASSWORD_TOO_SHORT |
| `A021` | 400 Bad Request | PASSWORD_TOO_WEAK |
| `A022` | 400 Bad Request | PASSWORD_RECENTLY_USED |
| `A023` | 400 Bad Request | PASSWORD_CONTAINS_USER_INFO |
| `A024` | 401 Unauthorized | PASSWORD_EXPIRED |
| `A025` | 400 Bad Request | PASSWORD_TOO_LONG |
| `A026` | 400 Bad Request | PASSWORD_CONTAINS_SEQUENTIAL |
| `A030` | 404 Not Found | ROLE_NOT_FOUND |
| `A031` | 409 Conflict | ROLE_ALREADY_ASSIGNED |
| `A032` | 404 Not Found | ROLE_NOT_ASSIGNED |
| `A033` | 400 Bad Request | SYSTEM_ROLE_CANNOT_BE_MODIFIED |
| `A034` | 404 Not Found | PERMISSION_NOT_FOUND |
| `A035` | 404 Not Found | MEMBERSHIP_NOT_FOUND |
| `A036` | 404 Not Found | MEMBERSHIP_TIER_NOT_FOUND |
| `A037` | 409 Conflict | MEMBERSHIP_ALREADY_EXISTS |
| `A038` | 403 Forbidden | MEMBERSHIP_EXPIRED |
| `A039` | 409 Conflict | ROLE_KEY_ALREADY_EXISTS |
| `A040` | 409 Conflict | SELLER_APPLICATION_ALREADY_PENDING |
| `A041` | 404 Not Found | SELLER_APPLICATION_NOT_FOUND |
| `A042` | 400 Bad Request | SELLER_APPLICATION_ALREADY_PROCESSED |

---

## 🔒 Security Summary

### 1. JWT 설정

```yaml
jwt:
  access-token-expiration: 900000     # 15분 (ms)
  refresh-token-expiration: 604800000 # 7일 (ms)
```

### 2. Refresh Token Cookie

```
이름: portal_refresh_token
HttpOnly: true
Secure: true (local: false)
SameSite: Lax
Path: /
MaxAge: 7일
```

### 3. 비밀번호 정책

```yaml
min-length: 8
max-length: 128
require-uppercase: true
require-lowercase: true
require-digit: true
require-special-char: true
history-count: 5      # 최근 5개 재사용 금지
max-age: 90           # 90일 만료
prevent-sequential: true
prevent-user-info: true
```

### 4. 인증 방식

| Method | Endpoints | Description |
|--------|-----------|-------------|
| **JWT Bearer Token** | `/api/v1/**` | Access Token in Authorization header |
| **OAuth2 Client** | `/oauth2/**`, `/login/oauth2/**` | 소셜 로그인 (Google, Kakao, Naver) |
| **None** | 회원가입, 멤버십 티어 조회 등 | 인증 불필요 |

### 5. 권한 레벨

| Level | Roles | Access |
|-------|-------|--------|
| **Public** | - | 회원가입, 멤버십 티어 조회 등 |
| **Authenticated** | `ROLE_USER` | 프로필 수정, 팔로우, 멤버십 관리 등 |
| **Super Admin** | `ROLE_SUPER_ADMIN` | RBAC 관리, 멤버십 관리 |
| **Service Admin** | `ROLE_BLOG_ADMIN`, `ROLE_SHOPPING_ADMIN` | 서비스별 관리 기능 |

---

## 📌 사용 예시

### 1. 회원가입 및 로그인 (JWT)

```typescript
// 1. 회원가입
const signupResponse = await fetch('http://localhost:8081/api/v1/users/signup', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'SecurePassword123!',
    nickname: 'johndoe',
    realName: 'John Doe',
    marketingAgree: true
  })
});

// 2. 로그인
const loginResponse = await fetch('http://localhost:8081/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'SecurePassword123!'
  })
});

const { data } = await loginResponse.json();
const { accessToken, refreshToken, expiresIn } = data;

// 3. API 호출
const profileResponse = await fetch('http://localhost:8081/api/v1/users/me', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
```

---

### 2. 토큰 갱신

```typescript
const refreshResponse = await fetch('http://localhost:8081/api/v1/auth/refresh', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include', // Cookie 포함
  body: JSON.stringify({
    refreshToken: refreshToken // Cookie 없으면 Body 사용
  })
});

const { data } = await refreshResponse.json();
const { accessToken: newAccessToken, refreshToken: newRefreshToken } = data;
```

---

### 3. 팔로우 관리

```typescript
// 팔로우 토글
await fetch('http://localhost:8081/api/v1/users/johndoe/follow', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${accessToken}` }
});

// 팔로워 목록 조회
const followersResponse = await fetch(
  'http://localhost:8081/api/v1/users/johndoe/followers?page=1&size=20',
  { headers: { 'Authorization': `Bearer ${accessToken}` } }
);
const { data } = await followersResponse.json();
console.log('Followers:', data.users);
```

---

### 4. 멤버십 관리

```typescript
// 내 멤버십 조회
const membershipResponse = await fetch('http://localhost:8081/api/v1/memberships/me', {
  headers: { 'Authorization': `Bearer ${accessToken}` }
});

// 멤버십 업그레이드
await fetch('http://localhost:8081/api/v1/memberships/me', {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    membershipGroup: 'user:blog',
    tierKey: 'PRO'
  })
});
```

---

### 5. 관리자: 역할 부여

```typescript
// BLOG_ADMIN 역할 부여
await fetch('http://localhost:8081/api/v1/admin/rbac/roles/assign', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${adminAccessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    userId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    roleKey: 'ROLE_BLOG_ADMIN'
  })
});
```

---

## 🔗 관련 문서

- [ADR-003: Admin 권한 검증 전략](../../adr/ADR-003-authorization-strategy.md)
- [ADR-008: JWT Stateless + Redis](../../adr/ADR-008-jwt-stateless-redis.md)
- [ADR-015: Role Hierarchy 구현](../../adr/ADR-015-role-hierarchy-implementation.md)
- [ADR-021: 역할 기반 멤버십 재구조화](../../adr/ADR-021-role-based-membership-restructure.md)
- [Architecture Overview](../../architecture/auth-service/system-overview.md)

---

## 📝 변경 이력

### v2.4.2 (2026-02-08)
- 페이지네이션 응답 구조 변경 (ADR-031): content → items, number → page (1-based), Spring 내부 필드 제거

### v2.4.1 (2026-02-07)
- 관련 문서 링크 수정 (존재하지 않는 ADR-006/009 → 실제 ADR-003/008/015/021)

### v3.0.0 (2026-02-18)
- **Role Multi-Include DAG 전환** (ADR-044): `parentRoleKey` 단일 FK → `includedRoleKeys` 다대다 DAG 구조
- `role_includes` 테이블 도입, `parent_role_id` 컬럼 제거 (V4 Flyway)
- `ROLE_GUEST` 역할 추가 (6개 시스템 역할)
- JWT `effectiveRoles` claim 추가 → Gateway에서 auth-service API 호출 제거
- RbacAdminController 5개 엔드포인트 추가:
  - `GET /roles/{roleKey}/includes` - direct includes 조회
  - `POST /roles/{roleKey}/includes` - include 추가 (cycle detection)
  - `DELETE /roles/{roleKey}/includes/{includedRoleKey}` - include 제거
  - `GET /roles/{roleKey}/resolved` - effective roles + permissions
  - `GET /roles/hierarchy` - 전체 DAG 구조
- Error Code A043~A046 추가 (cycle detection, include 관련)
- RoleResponse/RoleDetailResponse: `parentRoleKey` → `includedRoleKeys` + `effectiveRoleKeys`
- RoleHierarchyResolver @Deprecated (구형 JWT fallback용으로만 유지)

### v2.4.0 (2026-02-07)
- RbacAdminController에 역할 CRUD 8개 엔드포인트 추가 (상세, 생성, 수정, 상태변경, 권한 조회/할당/해제, 전체 권한 목록)
- Section 5.10~5.17 추가
- Error Code A039 (ROLE_KEY_ALREADY_EXISTS) 추가

### v2.3.0 (2026-02-07)
- RbacAdminController에 사용자 검색 API 추가 (`GET /api/v1/admin/rbac/users`)
- AdminUserResponse DTO 추가 (email, username, nickname LIKE 검색 + UUID exact match)

### v2.2.0 (2026-02-07)
- RbacAdminController에 Dashboard Stats API 추가 (`GET /api/v1/admin/rbac/dashboard`)
- RbacAdminController에 Audit Log API 추가 (`GET /api/v1/admin/rbac/audit`, `GET /api/v1/admin/rbac/users/{userId}/audit`)

### v2.1.0 (2026-02-07)
- **Membership Group 모델 전환**: `serviceName` → `membershipGroup` (format: `{role_scope}:{service}`)
- Membership 관련 모든 endpoint path variable 및 request body 필드명 업데이트
- RoleResponse에 `membershipGroup` 필드 추가
- 멤버십 enriched format: `{"user:blog": {"tier": "PRO", "order": 2}}`
- RoleHierarchyController 내부 API 섹션 추가 (Section 11)
- SecurityConfig에 `/api/v1/internal/**` permitAll 추가
- Controller 수 10→11, Endpoint 수 ~40→~42

### v2.0.0 (2026-02-06)
- **전면 재작성**: 실제 코드베이스와 100% 일치하도록 수정
- API 경로에 `/api/v1/` prefix 추가
- SecurityConfig 기준으로 인증/권한 요구사항 정확히 기재
- 실제 DTO 필드 기반 예시 JSON 작성
- 에러 코드 전체 목록 업데이트 (42개)
- OAuth2는 Spring Authorization Server가 아닌 OAuth2 Client (소셜 로그인) 명시
- 비밀번호 정책 상세 추가
- Refresh Token Cookie 정보 추가
- 공개 프로필 조회, Username 중복 확인, 팔로우 관련 경로가 인증 필요함을 명시

### v1.1.0 (2026-01-30)
- 11개 Controllers, ~38개 Endpoints 추가

### v1.0.0 (2026-01-18)
- 최초 작성

---

**최종 업데이트**: 2026-02-07
