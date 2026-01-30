---
id: api-auth
title: Auth Service API
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-01-30
author: Claude
tags: [api, auth, oauth2, oidc, rbac, membership, follow, seller]
related:
  - arch-system-overview
  - ADR-006-rbac-authorization
  - ADR-009-membership-system
---

# Auth Service API

> Portal Universe 인증/인가 서비스 종합 API 명세서. OAuth2, RBAC, Membership, Follow, Seller 관리 API를 제공합니다.

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `http://localhost:8081` (로컬) / `http://auth-service:8081` (Docker/K8s) |
| **인증 방식** | OAuth2 Authorization Code with PKCE, JWT Bearer Token |
| **지원 Grant Types** | Authorization Code, Refresh Token |
| **토큰 형식** | JWT (RS256) |
| **Access Token 유효기간** | 15분 (900초) |
| **Refresh Token 유효기간** | 7일 |
| **총 Controllers** | 11개 |
| **총 Endpoints** | ~38개 |

---

## 🎯 Controller Overview

| Controller | Base Path | 주요 기능 | 인증 요구 | 권한 요구 |
|------------|-----------|----------|----------|----------|
| **AuthController** | `/api/auth` | JWT 로그인/로그아웃 | 일부 | ❌ |
| **LoginController** | `/login` | HTML 로그인 페이지 | ❌ | ❌ |
| **UserController** | `/api/users` | 회원가입, 프로필 조회/수정 | 일부 | ❌ |
| **ProfileController** | `/api/profile` | 프로필 관리, 계정 삭제 | ✅ | ❌ |
| **FollowController** | `/api/users` | 팔로우/팔로워 관리 | 일부 | ❌ |
| **RbacAdminController** | `/api/admin/rbac` | 역할/권한 관리 (Admin) | ✅ | SUPER_ADMIN |
| **PermissionController** | `/api/permissions` | 내 권한 조회 | ✅ | ❌ |
| **MembershipController** | `/api/memberships` | 멤버십 조회/변경 | 일부 | ❌ |
| **MembershipAdminController** | `/api/admin/memberships` | 멤버십 관리 (Admin) | ✅ | SUPER_ADMIN |
| **SellerController** | `/api/seller` | 셀러 신청 | ✅ | ❌ |
| **SellerAdminController** | `/api/admin/seller` | 셀러 승인 (Admin) | ✅ | SHOPPING_ADMIN, SUPER_ADMIN |
| **Spring OAuth2** | `/oauth2/*` | OAuth2 표준 엔드포인트 | 일부 | ❌ |

---

## 🔐 1. AuthController (`/api/auth`)

JWT 기반 로그인, 토큰 갱신, 로그아웃 API.

### 1.1. 로그인 (POST `/api/auth/login`)

이메일/비밀번호로 로그인하여 JWT 토큰을 발급받습니다.

**Request**
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | string | ✅ | 이메일 주소 |
| `password` | string | ✅ | 비밀번호 |

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 900
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

**Response Fields**

| 필드 | 타입 | 설명 |
|------|------|------|
| `accessToken` | string | JWT Access Token (15분) |
| `refreshToken` | string | JWT Refresh Token (7일) |
| `expiresIn` | number | Access Token 만료 시간 (초) |

---

### 1.2. 토큰 갱신 (POST `/api/auth/refresh`)

Refresh Token으로 새 Access Token을 발급받습니다.

**Request**
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 900
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 1.3. 로그아웃 (POST `/api/auth/logout`)

**인증 필요**: ✅

Refresh Token을 무효화하여 로그아웃합니다.

**Request**
```http
POST /api/auth/logout
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": "Logged out successfully",
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

## 🖥️ 2. LoginController (`/login`)

Spring MVC 로그인 페이지 제공.

### 2.1. 로그인 페이지 (GET `/login`)

**인증 필요**: ❌

**Response**: HTML 로그인 페이지

```http
GET /login
```

브라우저에서 직접 접근 가능한 HTML 페이지를 반환합니다. OAuth2 Authorization Code Flow의 사용자 인증 단계에서 사용됩니다.

---

## 👤 3. UserController (`/api/users`)

사용자 회원가입, 프로필 조회/수정, username 설정, 비밀번호 변경 API.

### 3.1. 회원가입 (POST `/api/users/signup`)

**인증 필요**: ❌

이메일 기반 회원가입 API.

**Request**
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

**Request Body**

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `email` | string | ✅ | 이메일 주소 | 유효한 이메일 형식, 고유값 |
| `password` | string | ✅ | 비밀번호 | 8자 이상 권장 |
| `nickname` | string | ✅ | 닉네임 | 2~20자 |
| `realName` | string | ✅ | 실명 | 2~50자 |
| `marketingAgree` | boolean | ✅ | 마케팅 수신 동의 | true/false |

**Response (200 OK)**
```json
{
  "success": true,
  "data": "User registered successfully",
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
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
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 3.2. 공개 프로필 조회 (GET `/api/users/{username}`)

**인증 필요**: ❌

특정 사용자의 공개 프로필을 조회합니다.

**Request**
```http
GET /api/users/johndoe
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "username": "johndoe",
    "nickname": "John Doe",
    "bio": "Software Developer",
    "profileImageUrl": "https://example.com/profile.jpg",
    "website": "https://johndoe.dev",
    "followersCount": 120,
    "followingCount": 80,
    "createdAt": "2025-12-01T00:00:00Z"
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 3.3. 내 프로필 조회 (GET `/api/users/me`)

**인증 필요**: ✅

현재 로그인한 사용자의 프로필을 조회합니다.

**Request**
```http
GET /api/users/me
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "id": 123,
    "email": "user@example.com",
    "username": "johndoe",
    "nickname": "John Doe",
    "bio": "Software Developer",
    "profileImageUrl": "https://example.com/profile.jpg",
    "website": "https://johndoe.dev",
    "realName": "John Doe",
    "marketingAgree": true,
    "followersCount": 120,
    "followingCount": 80,
    "createdAt": "2025-12-01T00:00:00Z"
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 3.4. 프로필 수정 (PUT `/api/users/me/profile`)

**인증 필요**: ✅

프로필 정보(닉네임, 자기소개, 프로필 이미지, 웹사이트)를 수정합니다.

**Request**
```http
PUT /api/users/me/profile
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "nickname": "John Updated",
  "bio": "Senior Software Developer",
  "profileImageUrl": "https://example.com/new-profile.jpg",
  "website": "https://johndoe.dev"
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `nickname` | string | ❌ | 닉네임 |
| `bio` | string | ❌ | 자기소개 |
| `profileImageUrl` | string | ❌ | 프로필 이미지 URL |
| `website` | string | ❌ | 웹사이트 URL |

**Response (200 OK)**
```json
{
  "success": true,
  "data": "Profile updated successfully",
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 3.5. Username 설정 (POST `/api/users/me/username`)

**인증 필요**: ✅

최초 1회 한정으로 username을 설정합니다.

**Request**
```http
POST /api/users/me/username
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "username": "johndoe"
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `username` | string | ✅ | 사용자명 | 3~20자, 영문소문자/숫자/하이픈/언더스코어, 고유값 |

**Response (200 OK)**
```json
{
  "success": true,
  "data": "Username set successfully",
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

**Error Response (409 Conflict) - 이미 설정된 경우**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A002",
    "message": "Username already set"
  },
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 3.6. Username 중복 확인 (GET `/api/users/check-username/{username}`)

**인증 필요**: ❌

Username 사용 가능 여부를 확인합니다.

**Request**
```http
GET /api/users/check-username/johndoe
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "username": "johndoe",
    "available": false
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 3.7. 비밀번호 변경 (PUT `/api/users/me/password`)

**인증 필요**: ✅

현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.

**Request**
```http
PUT /api/users/me/password
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!"
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `currentPassword` | string | ✅ | 현재 비밀번호 |
| `newPassword` | string | ✅ | 새 비밀번호 (8자 이상) |

**Response (200 OK)**
```json
{
  "success": true,
  "data": "Password changed successfully",
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

**Error Response (400 Bad Request) - 현재 비밀번호 불일치**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A003",
    "message": "Current password is incorrect"
  },
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

## 📝 4. ProfileController (`/api/profile`)

프로필 조회/수정, 비밀번호 변경, 계정 삭제 API.

### 4.1. 내 프로필 조회 (GET `/api/profile/me`)

**인증 필요**: ✅

현재 로그인한 사용자의 상세 프로필을 조회합니다. (UserController의 `/api/users/me`와 유사)

**Request**
```http
GET /api/profile/me
Authorization: Bearer {accessToken}
```

**Response**: UserController의 `/api/users/me`와 동일

---

### 4.2. 프로필 수정 (PATCH `/api/profile`)

**인증 필요**: ✅

프로필 정보를 부분 수정합니다.

**Request**
```http
PATCH /api/profile
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "nickname": "Updated Nickname",
  "bio": "Updated bio"
}
```

**Request Body**: 수정할 필드만 포함 (optional)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `nickname` | string | ❌ | 닉네임 |
| `bio` | string | ❌ | 자기소개 |
| `profileImageUrl` | string | ❌ | 프로필 이미지 URL |
| `website` | string | ❌ | 웹사이트 URL |

**Response (200 OK)**
```json
{
  "success": true,
  "data": "Profile updated successfully",
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 4.3. 비밀번호 변경 (POST `/api/profile/password`)

**인증 필요**: ✅

현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다. (UserController의 PUT `/api/users/me/password`와 유사)

**Request**
```http
POST /api/profile/password
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!"
}
```

**Response**: UserController의 비밀번호 변경과 동일

---

### 4.4. 계정 삭제 (DELETE `/api/profile/account`)

**인증 필요**: ✅

사용자 계정을 영구 삭제합니다. 비밀번호 확인 필요.

**Request**
```http
DELETE /api/profile/account
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "password": "MyPassword123!"
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `password` | string | ✅ | 현재 비밀번호 (확인용) |

**Response (200 OK)**
```json
{
  "success": true,
  "data": "Account deleted successfully",
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

**Error Response (400 Bad Request) - 비밀번호 불일치**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A004",
    "message": "Password is incorrect"
  },
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

## 👥 5. FollowController (`/api/users`)

팔로우/팔로워 관리 API.

### 5.1. 팔로우 토글 (POST `/api/users/{username}/follow`)

**인증 필요**: ✅

특정 사용자를 팔로우하거나 언팔로우합니다. (토글 방식)

**Request**
```http
POST /api/users/johndoe/follow
Authorization: Bearer {accessToken}
```

**Response (200 OK) - 팔로우 성공**
```json
{
  "success": true,
  "data": {
    "following": true,
    "message": "Followed successfully"
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

**Response (200 OK) - 언팔로우 성공**
```json
{
  "success": true,
  "data": {
    "following": false,
    "message": "Unfollowed successfully"
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 5.2. 팔로워 목록 조회 (GET `/api/users/{username}/followers`)

**인증 필요**: ❌

특정 사용자의 팔로워 목록을 조회합니다. (페이지네이션 지원)

**Request**
```http
GET /api/users/johndoe/followers?page=0&size=20
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | number | ❌ | 0 | 페이지 번호 (0부터 시작) |
| `size` | number | ❌ | 20 | 페이지당 항목 수 |

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "username": "follower1",
        "nickname": "Follower One",
        "profileImageUrl": "https://example.com/profile1.jpg",
        "bio": "Bio text",
        "followedAt": "2026-01-15T10:00:00Z"
      },
      {
        "username": "follower2",
        "nickname": "Follower Two",
        "profileImageUrl": "https://example.com/profile2.jpg",
        "bio": "Another bio",
        "followedAt": "2026-01-20T14:30:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
        "sorted": false,
        "empty": true,
        "unsorted": true
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 120,
    "totalPages": 6,
    "last": false,
    "size": 20,
    "number": 0,
    "first": true,
    "numberOfElements": 20,
    "empty": false
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 5.3. 팔로잉 목록 조회 (GET `/api/users/{username}/following`)

**인증 필요**: ❌

특정 사용자가 팔로우하는 사용자 목록을 조회합니다. (페이지네이션 지원)

**Request**
```http
GET /api/users/johndoe/following?page=0&size=20
```

**Query Parameters**: 팔로워 목록과 동일

**Response**: 팔로워 목록과 동일한 구조

---

### 5.4. 내 팔로잉 ID 목록 (GET `/api/users/me/following/ids`)

**인증 필요**: ✅

현재 로그인한 사용자가 팔로우하는 모든 사용자의 ID 목록을 조회합니다. (클라이언트 캐싱용)

**Request**
```http
GET /api/users/me/following/ids
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "followingIds": [12, 45, 78, 123, 456]
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 5.5. 팔로우 상태 확인 (GET `/api/users/{username}/follow/status`)

**인증 필요**: ✅

현재 로그인한 사용자가 특정 사용자를 팔로우 중인지 확인합니다.

**Request**
```http
GET /api/users/johndoe/follow/status
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "username": "johndoe",
    "following": true
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

## 🔑 6. RbacAdminController (`/api/admin/rbac`)

**인증 필요**: ✅
**권한 필요**: `SUPER_ADMIN`

RBAC (Role-Based Access Control) 관리 API. 역할 조회, 사용자 역할/권한 조회, 역할 부여/회수를 담당합니다.

### 6.1. 전체 역할 조회 (GET `/api/admin/rbac/roles`)

시스템의 모든 활성 역할을 조회합니다.

**Request**
```http
GET /api/admin/rbac/roles
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "roleKey": "SUPER_ADMIN",
      "roleName": "Super Administrator",
      "description": "Full system access",
      "isActive": true
    },
    {
      "id": 2,
      "roleKey": "BLOG_ADMIN",
      "roleName": "Blog Administrator",
      "description": "Blog management access",
      "isActive": true
    },
    {
      "id": 3,
      "roleKey": "SHOPPING_ADMIN",
      "roleName": "Shopping Administrator",
      "description": "Shopping service management access",
      "isActive": true
    }
  ],
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 6.2. 사용자 역할 조회 (GET `/api/admin/rbac/users/{userId}/roles`)

특정 사용자에게 부여된 역할을 조회합니다.

**Request**
```http
GET /api/admin/rbac/users/123/roles
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": [
    {
      "roleKey": "BLOG_ADMIN",
      "roleName": "Blog Administrator",
      "assignedAt": "2026-01-15T10:00:00Z"
    }
  ],
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 6.3. 사용자 권한 조회 (GET `/api/admin/rbac/users/{userId}/permissions`)

특정 사용자가 가진 모든 권한(역할에서 파생된 권한)을 조회합니다.

**Request**
```http
GET /api/admin/rbac/users/123/permissions
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "userId": 123,
    "permissions": [
      {
        "permissionKey": "BLOG_POST_CREATE",
        "permissionName": "Create Blog Post",
        "serviceName": "BLOG"
      },
      {
        "permissionKey": "BLOG_POST_UPDATE",
        "permissionName": "Update Blog Post",
        "serviceName": "BLOG"
      },
      {
        "permissionKey": "BLOG_POST_DELETE",
        "permissionName": "Delete Blog Post",
        "serviceName": "BLOG"
      }
    ]
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 6.4. 역할 부여 (POST `/api/admin/rbac/roles/assign`)

특정 사용자에게 역할을 부여합니다.

**Request**
```http
POST /api/admin/rbac/roles/assign
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "userId": 123,
  "roleKey": "BLOG_ADMIN"
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `userId` | number | ✅ | 대상 사용자 ID |
| `roleKey` | string | ✅ | 부여할 역할 키 (예: BLOG_ADMIN) |

**Response (201 Created)**
```json
{
  "success": true,
  "data": "Role assigned successfully",
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

**Error Response (409 Conflict) - 이미 역할 보유**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "A005",
    "message": "User already has this role"
  },
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 6.5. 역할 회수 (DELETE `/api/admin/rbac/users/{userId}/roles/{roleKey}`)

특정 사용자로부터 역할을 회수합니다.

**Request**
```http
DELETE /api/admin/rbac/users/123/roles/BLOG_ADMIN
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": "Role revoked successfully",
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

## 🔓 7. PermissionController (`/api/permissions`)

### 7.1. 내 권한 조회 (GET `/api/permissions/me`)

**인증 필요**: ✅

현재 로그인한 사용자가 가진 모든 권한을 조회합니다.

**Request**
```http
GET /api/permissions/me
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "permissions": [
      {
        "permissionKey": "BLOG_POST_CREATE",
        "permissionName": "Create Blog Post",
        "serviceName": "BLOG"
      },
      {
        "permissionKey": "SHOPPING_PRODUCT_VIEW",
        "permissionName": "View Products",
        "serviceName": "SHOPPING"
      }
    ]
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

## 💎 8. MembershipController (`/api/memberships`)

멤버십 조회, 변경, 취소 API.

### 8.1. 내 멤버십 전체 조회 (GET `/api/memberships/me`)

**인증 필요**: ✅

현재 로그인한 사용자의 모든 서비스별 멤버십을 조회합니다.

**Request**
```http
GET /api/memberships/me
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": [
    {
      "serviceName": "BLOG",
      "tier": "PREMIUM",
      "startDate": "2026-01-01T00:00:00Z",
      "endDate": "2026-02-01T00:00:00Z",
      "status": "ACTIVE"
    },
    {
      "serviceName": "SHOPPING",
      "tier": "FREE",
      "startDate": "2025-12-15T00:00:00Z",
      "endDate": null,
      "status": "ACTIVE"
    }
  ],
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 8.2. 특정 서비스 멤버십 조회 (GET `/api/memberships/me/{serviceName}`)

**인증 필요**: ✅

특정 서비스의 멤버십 정보를 조회합니다.

**Request**
```http
GET /api/memberships/me/BLOG
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "serviceName": "BLOG",
    "tier": "PREMIUM",
    "startDate": "2026-01-01T00:00:00Z",
    "endDate": "2026-02-01T00:00:00Z",
    "status": "ACTIVE",
    "features": [
      "Ad-free reading",
      "Premium content access",
      "Early access to new posts"
    ]
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 8.3. 서비스 멤버십 티어 조회 (GET `/api/memberships/tiers/{serviceName}`)

**인증 필요**: ❌

특정 서비스의 이용 가능한 멤버십 티어 목록을 조회합니다. (공개 API)

**Request**
```http
GET /api/memberships/tiers/BLOG
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": [
    {
      "serviceName": "BLOG",
      "tier": "FREE",
      "name": "Free Plan",
      "description": "Basic access to blog",
      "price": 0,
      "features": [
        "Read public posts",
        "Comment on posts"
      ]
    },
    {
      "serviceName": "BLOG",
      "tier": "PREMIUM",
      "name": "Premium Plan",
      "description": "Full access with premium features",
      "price": 9900,
      "features": [
        "Ad-free reading",
        "Premium content access",
        "Early access to new posts",
        "Priority support"
      ]
    },
    {
      "serviceName": "BLOG",
      "tier": "PRO",
      "name": "Pro Plan",
      "description": "For professional bloggers",
      "price": 19900,
      "features": [
        "All Premium features",
        "Analytics dashboard",
        "Custom domain support",
        "API access"
      ]
    }
  ],
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 8.4. 멤버십 변경 (PUT `/api/memberships/me`)

**인증 필요**: ✅

특정 서비스의 멤버십 티어를 변경합니다.

**Request**
```http
PUT /api/memberships/me
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "serviceName": "BLOG",
  "tier": "PRO"
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `serviceName` | string | ✅ | 서비스명 (BLOG, SHOPPING, etc.) |
| `tier` | string | ✅ | 변경할 티어 (FREE, PREMIUM, PRO) |

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "serviceName": "BLOG",
    "tier": "PRO",
    "startDate": "2026-01-30T10:30:00Z",
    "endDate": "2026-02-30T10:30:00Z",
    "status": "ACTIVE"
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 8.5. 멤버십 취소 (DELETE `/api/memberships/me/{serviceName}`)

**인증 필요**: ✅

특정 서비스의 멤버십을 취소합니다. (FREE 티어로 전환)

**Request**
```http
DELETE /api/memberships/me/BLOG
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": "Membership cancelled successfully. You will be downgraded to FREE tier.",
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

## 💎 9. MembershipAdminController (`/api/admin/memberships`)

**인증 필요**: ✅
**권한 필요**: `SUPER_ADMIN`

관리자용 멤버십 관리 API.

### 9.1. 사용자 멤버십 조회 (GET `/api/admin/memberships/users/{userId}`)

특정 사용자의 모든 멤버십을 조회합니다.

**Request**
```http
GET /api/admin/memberships/users/123
Authorization: Bearer {accessToken}
```

**Response**: MembershipController의 `/api/memberships/me`와 동일한 구조

---

### 9.2. 사용자 멤버십 변경 (PUT `/api/admin/memberships/users/{userId}`)

관리자가 특정 사용자의 멤버십을 강제로 변경합니다.

**Request**
```http
PUT /api/admin/memberships/users/123
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "serviceName": "BLOG",
  "tier": "PRO",
  "reason": "Customer support request"
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `serviceName` | string | ✅ | 서비스명 |
| `tier` | string | ✅ | 변경할 티어 |
| `reason` | string | ❌ | 변경 사유 (로그용) |

**Response (200 OK)**
```json
{
  "success": true,
  "data": "User membership updated successfully",
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

## 🛒 10. SellerController (`/api/seller`)

셀러(판매자) 신청 API.

### 10.1. 셀러 신청 (POST `/api/seller/apply`)

**인증 필요**: ✅

판매자 자격을 신청합니다.

**Request**
```http
POST /api/seller/apply
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "businessName": "My Shop",
  "businessNumber": "123-45-67890",
  "businessAddress": "123 Main St, Seoul",
  "phoneNumber": "010-1234-5678",
  "bankAccount": "110-123-456789",
  "description": "I want to sell handmade goods"
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `businessName` | string | ✅ | 사업자명 (또는 상호) |
| `businessNumber` | string | ✅ | 사업자 등록번호 |
| `businessAddress` | string | ✅ | 사업장 주소 |
| `phoneNumber` | string | ✅ | 연락처 |
| `bankAccount` | string | ✅ | 정산 계좌 |
| `description` | string | ❌ | 신청 사유 |

**Response (201 Created)**
```json
{
  "success": true,
  "data": {
    "applicationId": 456,
    "status": "PENDING",
    "submittedAt": "2026-01-30T10:30:00Z"
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

**Error Response (409 Conflict) - 이미 신청 존재**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "S001",
    "message": "Seller application already exists"
  },
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 10.2. 내 신청 상태 조회 (GET `/api/seller/application`)

**인증 필요**: ✅

현재 로그인한 사용자의 셀러 신청 상태를 조회합니다.

**Request**
```http
GET /api/seller/application
Authorization: Bearer {accessToken}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "applicationId": 456,
    "status": "PENDING",
    "businessName": "My Shop",
    "submittedAt": "2026-01-30T10:30:00Z",
    "reviewedAt": null,
    "reviewedBy": null,
    "rejectReason": null
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

**Status Values**: `PENDING`, `APPROVED`, `REJECTED`

---

## 🛒 11. SellerAdminController (`/api/admin/seller`)

**인증 필요**: ✅
**권한 필요**: `SHOPPING_ADMIN` 또는 `SUPER_ADMIN`

셀러 신청 승인/거부 관리 API.

### 11.1. 대기 중인 신청 조회 (GET `/api/admin/seller/applications/pending`)

승인 대기 중인 셀러 신청 목록을 조회합니다. (페이지네이션)

**Request**
```http
GET /api/admin/seller/applications/pending?page=0&size=20
Authorization: Bearer {accessToken}
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | number | ❌ | 0 | 페이지 번호 |
| `size` | number | ❌ | 20 | 페이지당 항목 수 |

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "applicationId": 456,
        "userId": 123,
        "userEmail": "user@example.com",
        "businessName": "My Shop",
        "businessNumber": "123-45-67890",
        "businessAddress": "123 Main St, Seoul",
        "phoneNumber": "010-1234-5678",
        "bankAccount": "110-123-456789",
        "description": "I want to sell handmade goods",
        "status": "PENDING",
        "submittedAt": "2026-01-30T10:30:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 45,
    "totalPages": 3,
    "last": false,
    "first": true
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

### 11.2. 전체 신청 조회 (GET `/api/admin/seller/applications`)

모든 셀러 신청 목록을 조회합니다. (페이지네이션)

**Request**
```http
GET /api/admin/seller/applications?page=0&size=20
Authorization: Bearer {accessToken}
```

**Query Parameters**: 대기 중인 신청 조회와 동일

**Response**: 대기 중인 신청 조회와 동일한 구조 (단, status가 PENDING/APPROVED/REJECTED 모두 포함)

---

### 11.3. 신청 심사 (POST `/api/admin/seller/applications/{applicationId}/review`)

셀러 신청을 승인하거나 거부합니다.

**Request**
```http
POST /api/admin/seller/applications/456/review
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "action": "APPROVE",
  "rejectReason": null
}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `action` | string | ✅ | APPROVE 또는 REJECT |
| `rejectReason` | string | ❌ | 거부 시 사유 (REJECT일 때 권장) |

**Response (200 OK) - 승인**
```json
{
  "success": true,
  "data": {
    "applicationId": 456,
    "status": "APPROVED",
    "reviewedAt": "2026-01-30T11:00:00Z",
    "reviewedBy": "admin@example.com"
  },
  "error": null,
  "timestamp": "2026-01-30T11:00:00Z"
}
```

**Response (200 OK) - 거부**
```json
{
  "success": true,
  "data": {
    "applicationId": 456,
    "status": "REJECTED",
    "rejectReason": "Invalid business number",
    "reviewedAt": "2026-01-30T11:00:00Z",
    "reviewedBy": "admin@example.com"
  },
  "error": null,
  "timestamp": "2026-01-30T11:00:00Z"
}
```

---

## 🔐 12. OAuth2 Endpoints (Spring Authorization Server)

OAuth2 Authorization Code Flow with PKCE 표준 엔드포인트.

### 12.1. 인가 코드 요청 (GET `/oauth2/authorize`)

**인증 필요**: ❌ (로그인 페이지로 리다이렉트)

OAuth2 Authorization Code Flow의 첫 단계. 사용자를 로그인 페이지로 리다이렉트하여 인가 코드를 발급받습니다.

**Request**
```http
GET /oauth2/authorize?response_type=code&client_id=portal-client&redirect_uri=http://localhost:30000/callback&scope=openid%20profile%20read%20write&code_challenge=CHALLENGE_STRING&code_challenge_method=S256&state=RANDOM_STATE
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `response_type` | string | ✅ | 항상 `code` |
| `client_id` | string | ✅ | 클라이언트 ID |
| `redirect_uri` | string | ✅ | 인가 코드 수신 URI |
| `scope` | string | ✅ | 요청 스코프 (공백 구분) |
| `code_challenge` | string | ✅ | PKCE Code Challenge |
| `code_challenge_method` | string | ✅ | Challenge 방식 (S256) |
| `state` | string | ✅ | CSRF 방지용 랜덤 문자열 |

**지원 스코프**

| 스코프 | 설명 |
|--------|------|
| `openid` | OIDC 표준 (필수) |
| `profile` | 프로필 정보 접근 |
| `read` | 읽기 권한 |
| `write` | 쓰기 권한 |

**Response**: 사용자 로그인 후 `redirect_uri`로 리다이렉트

```http
HTTP/1.1 302 Found
Location: http://localhost:30000/callback?code=AUTHORIZATION_CODE&state=RANDOM_STATE
```

---

### 12.2. Access Token 발급 (POST `/oauth2/token`)

**인증 필요**: ❌

인가 코드를 사용하여 Access Token과 Refresh Token을 발급받습니다.

**Request**
```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code=AUTHORIZATION_CODE
&redirect_uri=http://localhost:30000/callback
&client_id=portal-client
&code_verifier=CODE_VERIFIER_STRING
```

**Request Body (application/x-www-form-urlencoded)**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `grant_type` | string | ✅ | 항상 `authorization_code` |
| `code` | string | ✅ | 인가 코드 |
| `redirect_uri` | string | ✅ | 인가 시 사용한 URI (동일해야 함) |
| `client_id` | string | ✅ | 클라이언트 ID |
| `code_verifier` | string | ✅ | PKCE Code Verifier |

**Response (200 OK)**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "scope": "openid profile read write",
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 900
}
```

**JWT Access Token Payload 예시**
```json
{
  "sub": "user@example.com",
  "aud": ["portal-client"],
  "nbf": 1737184200,
  "scope": ["openid", "profile", "read", "write"],
  "roles": ["ROLE_USER"],
  "iss": "http://localhost:8081",
  "exp": 1737185100,
  "iat": 1737184200,
  "jti": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

### 12.3. Token 갱신 (POST `/oauth2/token` with refresh_token)

**인증 필요**: ❌

Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.

**Request**
```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token
&refresh_token=REFRESH_TOKEN
&client_id=portal-client
```

**Response**: Access Token 발급과 동일한 구조

> **참고**: Refresh Token은 재사용되지 않으며, 갱신 시마다 새로운 Refresh Token이 발급됩니다.

---

### 12.4. JWK Set 조회 (GET `/oauth2/jwks`)

**인증 필요**: ❌

JWT 토큰 검증에 사용되는 공개키 정보를 조회합니다.

**Request**
```http
GET /oauth2/jwks
```

**Response (200 OK)**
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

### 12.5. Token 취소 (POST `/oauth2/revoke`)

**인증 필요**: ❌

발급된 토큰을 취소합니다.

**Request**
```http
POST /oauth2/revoke
Content-Type: application/x-www-form-urlencoded

token=REFRESH_TOKEN
&client_id=portal-client
```

**Response (200 OK)**: 빈 응답

---

### 12.6. Token 검증 (POST `/oauth2/introspect`)

**인증 필요**: ✅ (Client Credentials)

토큰의 유효성과 메타데이터를 검증합니다.

**Request**
```http
POST /oauth2/introspect
Content-Type: application/x-www-form-urlencoded
Authorization: Basic {base64(client_id:client_secret)}

token=ACCESS_TOKEN
```

**Response (200 OK)**
```json
{
  "active": true,
  "sub": "user@example.com",
  "scope": "openid profile read write",
  "exp": 1737185100,
  "iat": 1737184200
}
```

---

### 12.7. OIDC Discovery (GET `/.well-known/openid-configuration`)

**인증 필요**: ❌

OpenID Connect Discovery 메타데이터를 조회합니다.

**Request**
```http
GET /.well-known/openid-configuration
```

**Response (200 OK)**
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

## 📦 API Response Format

모든 RESTful API는 통일된 `ApiResponse` wrapper를 사용합니다. (OAuth2 표준 엔드포인트 제외)

### Success Response

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
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
  "timestamp": "2026-01-30T10:30:00Z"
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
| `A001` | 409 Conflict | 이메일 중복 (회원가입 시) |
| `A002` | 409 Conflict | Username 이미 설정됨 |
| `A003` | 400 Bad Request | 현재 비밀번호 불일치 |
| `A004` | 400 Bad Request | 비밀번호 확인 실패 (계정 삭제 시) |
| `A005` | 409 Conflict | 사용자가 이미 해당 역할 보유 |

### Shopping Service Errors (Seller)

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `S001` | 409 Conflict | 셀러 신청이 이미 존재함 |

### Common Errors

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `C001` | 400 Bad Request | 잘못된 요청 (유효성 검증 실패) |
| `C002` | 401 Unauthorized | 인증 실패 (로그인 필요) |
| `C003` | 403 Forbidden | 권한 없음 |
| `C004` | 404 Not Found | 리소스를 찾을 수 없음 |
| `C005` | 500 Internal Server Error | 서버 내부 오류 |

### OAuth2 Standard Errors

| Error | HTTP Status | 설명 |
|-------|-------------|------|
| `invalid_request` | 400 | 필수 파라미터 누락 |
| `invalid_grant` | 400 | 인가 코드 또는 Refresh Token 무효 |
| `invalid_client` | 401 | 클라이언트 인증 실패 |
| `unauthorized_client` | 400 | 클라이언트가 해당 Grant Type 사용 불가 |
| `unsupported_grant_type` | 400 | 지원하지 않는 Grant Type |
| `invalid_scope` | 400 | 잘못된 스코프 요청 |

---

## 🔒 Security Summary

### 1. Authentication Methods

| Method | Endpoints | Description |
|--------|-----------|-------------|
| **OAuth2 PKCE** | `/oauth2/*` | Authorization Code Flow with PKCE |
| **JWT Bearer Token** | `/api/**` | Access Token in Authorization header |
| **None** | 회원가입, 공개 프로필 등 | 인증 불필요 |

### 2. Authorization Levels

| Level | Roles | Access |
|-------|-------|--------|
| **Public** | - | 회원가입, 공개 프로필, 멤버십 티어 조회 등 |
| **Authenticated** | `ROLE_USER` | 프로필 수정, 팔로우, 멤버십 관리 등 |
| **Admin** | `SUPER_ADMIN` | RBAC 관리, 멤버십 관리 |
| **Service Admin** | `BLOG_ADMIN`, `SHOPPING_ADMIN` | 서비스별 관리 기능 |

### 3. PKCE (Proof Key for Code Exchange)

Public Client를 위해 PKCE를 **필수**로 요구합니다.

**PKCE Flow**:
1. Code Verifier 생성 (43~128자 랜덤 문자열)
2. Code Challenge 생성 (`BASE64URL(SHA256(code_verifier))`)
3. 인가 요청 시 `code_challenge`, `code_challenge_method=S256` 포함
4. 토큰 요청 시 `code_verifier` 포함

### 4. State Parameter

CSRF 공격 방지를 위해 `state` 파라미터를 반드시 사용해야 합니다.

```javascript
const state = crypto.randomUUID();
sessionStorage.setItem('oauth_state', state);
// 콜백 수신 시 검증
if (callbackState !== sessionStorage.getItem('oauth_state')) {
  throw new Error('State mismatch');
}
```

### 5. Token Security

- **Access Token**: 15분 유효기간 (짧은 수명)
- **Refresh Token**: 7일 유효기간, 재사용 불가 (`reuseRefreshTokens: false`)
- **JWT Signature**: RS256 (RSA-SHA256) 공개키 암호화
- **Token Storage**: HttpOnly Cookie 권장 (XSS 방지)

---

## 📌 사용 예시

### 1. 회원가입 및 로그인 (JWT)

```typescript
// 1. 회원가입
const signupResponse = await fetch('http://localhost:8081/api/users/signup', {
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
const loginResponse = await fetch('http://localhost:8081/api/auth/login', {
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
const profileResponse = await fetch('http://localhost:8081/api/users/me', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
```

---

### 2. OAuth2 Flow (oidc-client-ts)

```typescript
import { UserManager } from 'oidc-client-ts';

const userManager = new UserManager({
  authority: 'http://localhost:8081',
  client_id: 'portal-client',
  redirect_uri: 'http://localhost:30000/callback',
  response_type: 'code',
  scope: 'openid profile read write',
  post_logout_redirect_uri: 'http://localhost:30000',
  automaticSilentRenew: true,
});

// 로그인
await userManager.signinRedirect();

// 콜백 처리
const user = await userManager.signinRedirectCallback();
console.log('Access Token:', user.access_token);

// API 호출
const response = await fetch('http://localhost:8080/api/v1/blog/posts', {
  headers: { 'Authorization': `Bearer ${user.access_token}` }
});
```

---

### 3. 팔로우 관리

```typescript
// 팔로우 토글
await fetch('http://localhost:8081/api/users/johndoe/follow', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${accessToken}` }
});

// 팔로워 목록 조회
const followersResponse = await fetch(
  'http://localhost:8081/api/users/johndoe/followers?page=0&size=20'
);
const { data } = await followersResponse.json();
console.log('Followers:', data.content);
```

---

### 4. 멤버십 관리

```typescript
// 내 멤버십 조회
const membershipResponse = await fetch('http://localhost:8081/api/memberships/me', {
  headers: { 'Authorization': `Bearer ${accessToken}` }
});

// 멤버십 업그레이드
await fetch('http://localhost:8081/api/memberships/me', {
  method: 'PUT',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    serviceName: 'BLOG',
    tier: 'PREMIUM'
  })
});
```

---

### 5. 관리자: 역할 부여

```typescript
// BLOG_ADMIN 역할 부여
await fetch('http://localhost:8081/api/admin/rbac/roles/assign', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${adminAccessToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    userId: 123,
    roleKey: 'BLOG_ADMIN'
  })
});
```

---

## 🔗 관련 문서

- [ADR-006: RBAC Authorization Strategy](../../docs/adr/ADR-006-rbac-authorization.md)
- [ADR-009: Membership System Design](../../docs/adr/ADR-009-membership-system.md)
- [Architecture Overview](../../docs/architecture/system-overview.md)
- [API Gateway 설정](../../../api-gateway/docs/api/gateway-api.md)

---

## 📝 변경 이력

### v1.1.0 (2026-01-30)
- **대규모 확장**: 11개 Controllers, ~38개 Endpoints 추가
- UserController 7개 엔드포인트 문서화
- ProfileController 4개 엔드포인트 문서화
- FollowController 5개 엔드포인트 문서화
- RbacAdminController 5개 엔드포인트 문서화 (SUPER_ADMIN 전용)
- PermissionController 1개 엔드포인트 문서화
- MembershipController 5개 엔드포인트 문서화
- MembershipAdminController 2개 엔드포인트 문서화 (SUPER_ADMIN 전용)
- SellerController 2개 엔드포인트 문서화
- SellerAdminController 3개 엔드포인트 문서화 (SHOPPING_ADMIN 전용)
- AuthController JWT 로그인/로그아웃 추가
- API Response Format 섹션 추가
- Security Summary 섹션 추가
- Error Codes 확장

### v1.0.0 (2026-01-18)
- 최초 작성
- OAuth2 Authorization Code with PKCE 지원
- 회원가입 API 추가
- Spring Authorization Server 표준 엔드포인트 문서화

---

**최종 업데이트**: 2026-01-30
