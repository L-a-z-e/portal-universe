---
id: api-admin-coupon
title: Admin Coupon API
type: api
status: current
version: v1
created: 2026-01-19
updated: 2026-01-19
author: Laze
tags: [api, shopping-service, admin, coupon]
related:
  - PRD-001
---

# Admin Coupon API

> 관리자 전용 쿠폰 관리 API (생성, 조회, 비활성화)

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/shopping/admin/coupons` |
| **인증** | Bearer Token 필요 |
| **권한** | ADMIN 전용 (모든 엔드포인트) |
| **버전** | v1 |

---

## 📑 API 목록

| Method | Endpoint | 설명 | 인증 | 권한 |
|--------|----------|------|------|------|
| POST | `/` | 쿠폰 생성 | ✅ | ADMIN |
| GET | `/{couponId}` | 쿠폰 조회 | ✅ | ADMIN |
| DELETE | `/{couponId}` | 쿠폰 비활성화 | ✅ | ADMIN |

---

## 🔹 쿠폰 생성

새로운 쿠폰을 등록합니다. (관리자 전용)

### Request

```http
POST /api/shopping/admin/coupons
Content-Type: application/json
Authorization: Bearer {token}

{
  "code": "WELCOME2026",
  "name": "신규 가입 환영 쿠폰",
  "description": "첫 구매 시 사용 가능한 10% 할인 쿠폰",
  "discountType": "PERCENTAGE",
  "discountValue": 10.0,
  "minimumOrderAmount": 50000,
  "maximumDiscountAmount": 5000,
  "totalQuantity": 1000,
  "startsAt": "2026-01-20T00:00:00Z",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `code` | string | ✅ | 쿠폰 코드 | 최대 50자, 고유값 |
| `name` | string | ✅ | 쿠폰명 | 최대 100자 |
| `description` | string | ❌ | 쿠폰 설명 | - |
| `discountType` | string | ✅ | 할인 유형 | `PERCENTAGE` 또는 `FIXED` |
| `discountValue` | number | ✅ | 할인 값 | 0.01 이상 |
| `minimumOrderAmount` | number | ❌ | 최소 주문 금액 | 0 이상 |
| `maximumDiscountAmount` | number | ❌ | 최대 할인 금액 | 0 이상 |
| `totalQuantity` | integer | ✅ | 총 발행 수량 | 최소 1 |
| `startsAt` | datetime | ✅ | 유효 시작일 | ISO 8601 형식 |
| `expiresAt` | datetime | ✅ | 유효 종료일 | ISO 8601 형식, 미래 날짜 |

### Discount Type

- **PERCENTAGE**: 퍼센트 할인 (예: 10% 할인)
  - `discountValue`: 10 = 10% 할인
- **FIXED**: 고정 금액 할인 (예: 5,000원 할인)
  - `discountValue`: 5000 = 5,000원 할인

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "WELCOME2026",
    "name": "신규 가입 환영 쿠폰",
    "description": "첫 구매 시 사용 가능한 10% 할인 쿠폰",
    "discountType": "PERCENTAGE",
    "discountValue": 10.0,
    "minimumOrderAmount": 50000,
    "maximumDiscountAmount": 5000,
    "totalQuantity": 1000,
    "issuedQuantity": 0,
    "remainingQuantity": 1000,
    "status": "ACTIVE",
    "startsAt": "2026-01-20T00:00:00Z",
    "expiresAt": "2026-12-31T23:59:59Z",
    "createdAt": "2026-01-19T10:00:00Z"
  },
  "timestamp": "2026-01-19T10:00:00Z"
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | long | 쿠폰 ID |
| `code` | string | 쿠폰 코드 |
| `name` | string | 쿠폰명 |
| `description` | string | 쿠폰 설명 |
| `discountType` | string | 할인 유형 |
| `discountValue` | number | 할인 값 |
| `minimumOrderAmount` | number | 최소 주문 금액 |
| `maximumDiscountAmount` | number | 최대 할인 금액 |
| `totalQuantity` | integer | 총 발행 수량 |
| `issuedQuantity` | integer | 발급된 수량 |
| `remainingQuantity` | integer | 남은 수량 |
| `status` | string | 쿠폰 상태 (`ACTIVE`, `INACTIVE`, `EXPIRED`) |
| `startsAt` | datetime | 유효 시작일 |
| `expiresAt` | datetime | 유효 종료일 |
| `createdAt` | datetime | 생성일시 |

---

## 🔹 쿠폰 조회

특정 ID를 가진 쿠폰 정보를 조회합니다. (관리자 전용)

### Request

```http
GET /api/shopping/admin/coupons/{couponId}
Authorization: Bearer {token}
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `couponId` | long | ✅ | 쿠폰 ID |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "WELCOME2026",
    "name": "신규 가입 환영 쿠폰",
    "description": "첫 구매 시 사용 가능한 10% 할인 쿠폰",
    "discountType": "PERCENTAGE",
    "discountValue": 10.0,
    "minimumOrderAmount": 50000,
    "maximumDiscountAmount": 5000,
    "totalQuantity": 1000,
    "issuedQuantity": 250,
    "remainingQuantity": 750,
    "status": "ACTIVE",
    "startsAt": "2026-01-20T00:00:00Z",
    "expiresAt": "2026-12-31T23:59:59Z",
    "createdAt": "2026-01-19T10:00:00Z"
  },
  "timestamp": "2026-01-19T11:00:00Z"
}
```

### Error Response

```json
{
  "success": false,
  "code": "S003",
  "message": "쿠폰을 찾을 수 없습니다.",
  "timestamp": "2026-01-19T11:00:00Z"
}
```

---

## 🔹 쿠폰 비활성화

특정 쿠폰을 비활성화합니다. (관리자 전용)

쿠폰이 삭제되는 것이 아니라 `status`가 `INACTIVE`로 변경됩니다.

### Request

```http
DELETE /api/shopping/admin/coupons/{couponId}
Authorization: Bearer {token}
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `couponId` | long | ✅ | 쿠폰 ID |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "WELCOME2026",
    "name": "신규 가입 환영 쿠폰",
    "description": "첫 구매 시 사용 가능한 10% 할인 쿠폰",
    "discountType": "PERCENTAGE",
    "discountValue": 10.0,
    "minimumOrderAmount": 50000,
    "maximumDiscountAmount": 5000,
    "totalQuantity": 1000,
    "issuedQuantity": 250,
    "remainingQuantity": 750,
    "status": "INACTIVE",
    "startsAt": "2026-01-20T00:00:00Z",
    "expiresAt": "2026-12-31T23:59:59Z",
    "createdAt": "2026-01-19T10:00:00Z"
  },
  "timestamp": "2026-01-19T12:00:00Z"
}
```

### Error Response

```json
{
  "success": false,
  "code": "S003",
  "message": "쿠폰을 찾을 수 없습니다.",
  "timestamp": "2026-01-19T12:00:00Z"
}
```

---

## ⚠️ 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `S002` | 400 | 유효성 검증 실패 |
| `S003` | 404 | 쿠폰을 찾을 수 없습니다 |
| `S004` | 409 | 중복된 쿠폰 코드입니다 |
| `C001` | 401 | 인증 필요 |
| `C002` | 403 | 권한 없음 (ADMIN 전용) |

### 유효성 검증 실패 예시

```json
{
  "success": false,
  "code": "S002",
  "message": "유효성 검증 실패: code는 필수입니다, expiresAt는 미래 날짜여야 합니다",
  "timestamp": "2026-01-19T10:00:00Z"
}
```

---

## 📌 비즈니스 규칙

### 쿠폰 생성
- `code`는 고유값이어야 하며 중복 시 409 에러 발생
- `expiresAt`는 `startsAt`보다 이후여야 함
- `discountValue`는 `discountType`에 따라 의미가 다름
  - PERCENTAGE: 1~100 사이 값 권장
  - FIXED: 실제 할인 금액

### 쿠폰 비활성화
- 비활성화된 쿠폰은 더 이상 발급되지 않음
- 이미 발급된 쿠폰은 영향 없음 (사용 가능)

### 쿠폰 상태
- **ACTIVE**: 활성화 (발급 가능)
- **INACTIVE**: 비활성화 (발급 불가)
- **EXPIRED**: 만료됨 (자동 처리, 유효기간 종료)

---

## 🔗 관련 문서

- [Coupon API](./coupon-api.md) (사용자용 쿠폰 API)
- [Order API](./order-api.md)
- [Shopping Service Architecture](../architecture/system-architecture.md)

---

**최종 업데이트**: 2026-01-19
