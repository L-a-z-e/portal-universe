---
id: api-admin-product
title: Admin Product API
type: api
status: current
version: v1
created: 2026-01-19
updated: 2026-01-19
author: Claude
tags: [api, shopping-service, admin, product]
related:
  - PRD-001
---

# Admin Product API

> 관리자용 상품 관리 API (등록, 수정, 삭제, 재고 관리)

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/shopping/admin/products` |
| **인증** | Bearer Token 필요 |
| **권한** | ADMIN 역할 필수 |
| **버전** | v1 |

> ⚠️ **중요**: 이 API의 모든 엔드포인트는 `ADMIN` 권한이 필요합니다.

---

## 📑 API 목록

| Method | Endpoint | 설명 | HTTP Status |
|--------|----------|------|-------------|
| POST | `/` | 새로운 상품 등록 | 201 Created |
| PUT | `/{productId}` | 상품 정보 수정 | 200 OK |
| DELETE | `/{productId}` | 상품 삭제 | 200 OK |
| PATCH | `/{productId}/stock` | 상품 재고 수정 | 200 OK |

---

## 🔹 상품 등록

새로운 상품을 시스템에 등록합니다. (관리자 전용)

### Request

```http
POST /api/shopping/admin/products
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Kotlin 인 액션",
  "description": "코틀린으로 더 나은 자바 애플리케이션 개발하기",
  "price": 42000,
  "stock": 150
}
```

### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `name` | string | ✅ | 상품명 | 1~200자 |
| `description` | string | ❌ | 상품 설명 | 최대 2000자 |
| `price` | number | ✅ | 가격 | 0보다 큰 양수 |
| `stock` | number | ✅ | 재고 수량 | 0 이상 |

### Response (201 Created)

```json
{
  "success": true,
  "data": {
    "id": 15,
    "name": "Kotlin 인 액션",
    "description": "코틀린으로 더 나은 자바 애플리케이션 개발하기",
    "price": 42000,
    "stock": 150
  },
  "code": null,
  "message": null,
  "timestamp": "2026-01-19T14:30:00Z"
}
```

### Error Responses

| HTTP Status | Code | 설명 |
|-------------|------|------|
| 400 | `S002` | 유효성 검증 실패 (가격이 0 이하, 이름 누락 등) |
| 401 | `C001` | 인증 토큰 없음 또는 만료 |
| 403 | `C002` | ADMIN 권한 없음 |

---

## 🔹 상품 정보 수정

등록된 상품의 정보를 수정합니다. (관리자 전용)

### Request

```http
PUT /api/shopping/admin/products/{productId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Kotlin 인 액션 [2판]",
  "description": "Kotlin 2.0 기반 최신 업데이트",
  "price": 45000,
  "stock": 200
}
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `productId` | long | ✅ | 수정할 상품 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `name` | string | ✅ | 상품명 | 1~200자 |
| `description` | string | ❌ | 상품 설명 | 최대 2000자 |
| `price` | number | ✅ | 가격 | 0보다 큰 양수 |
| `stock` | number | ✅ | 재고 수량 | 0 이상 |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 15,
    "name": "Kotlin 인 액션 [2판]",
    "description": "Kotlin 2.0 기반 최신 업데이트",
    "price": 45000,
    "stock": 200
  },
  "code": null,
  "message": null,
  "timestamp": "2026-01-19T15:00:00Z"
}
```

### Error Responses

| HTTP Status | Code | 설명 |
|-------------|------|------|
| 400 | `S002` | 유효성 검증 실패 |
| 401 | `C001` | 인증 토큰 없음 또는 만료 |
| 403 | `C002` | ADMIN 권한 없음 |
| 404 | `S001` | 상품을 찾을 수 없음 |

---

## 🔹 상품 삭제

시스템에서 상품을 삭제합니다. (관리자 전용)

### Request

```http
DELETE /api/shopping/admin/products/{productId}
Authorization: Bearer {token}
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `productId` | long | ✅ | 삭제할 상품 ID |

### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "code": null,
  "message": null,
  "timestamp": "2026-01-19T15:30:00Z"
}
```

### Error Responses

| HTTP Status | Code | 설명 |
|-------------|------|------|
| 401 | `C001` | 인증 토큰 없음 또는 만료 |
| 403 | `C002` | ADMIN 권한 없음 |
| 404 | `S001` | 상품을 찾을 수 없음 |

---

## 🔹 상품 재고 수정

특정 상품의 재고 수량만 업데이트합니다. (관리자 전용)

> 💡 **Use Case**: 전체 정보를 수정하지 않고 재고만 빠르게 조정할 때 사용합니다.

### Request

```http
PATCH /api/shopping/admin/products/{productId}/stock
Content-Type: application/json
Authorization: Bearer {token}

{
  "stock": 300
}
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `productId` | long | ✅ | 재고를 수정할 상품 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `stock` | number | ✅ | 새로운 재고 수량 | 0 이상 |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 15,
    "name": "Kotlin 인 액션 [2판]",
    "description": "Kotlin 2.0 기반 최신 업데이트",
    "price": 45000,
    "stock": 300
  },
  "code": null,
  "message": null,
  "timestamp": "2026-01-19T16:00:00Z"
}
```

### Error Responses

| HTTP Status | Code | 설명 |
|-------------|------|------|
| 400 | `S002` | 유효성 검증 실패 (음수 재고 등) |
| 401 | `C001` | 인증 토큰 없음 또는 만료 |
| 403 | `C002` | ADMIN 권한 없음 |
| 404 | `S001` | 상품을 찾을 수 없음 |

---

## ⚠️ 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `S001` | 404 | 상품을 찾을 수 없습니다 |
| `S002` | 400 | 유효성 검증 실패 (입력값 오류) |
| `C001` | 401 | 인증 필요 (토큰 없음 또는 만료) |
| `C002` | 403 | 권한 없음 (ADMIN 역할 필요) |

---

## 🔐 권한 요구사항

이 API의 **모든 엔드포인트**는 다음 조건을 만족해야 합니다:

1. **인증**: 유효한 JWT Bearer Token 필요
2. **권한**: `ADMIN` 역할(Role) 보유
3. **토큰 검증**: Auth Service에서 발급한 토큰

### 권한 확인 방법

```java
@PreAuthorize("hasRole('ADMIN')")
```

Spring Security의 `@PreAuthorize` 어노테이션으로 ADMIN 권한을 검증합니다.

---

## 🔗 관련 문서

- [Product API](./product-api.md) - 일반 사용자용 상품 조회 API
- [Cart API](./cart-api.md) - 장바구니 API
- [Order API](./order-api.md) - 주문 API

---

## 📝 참고사항

### DTO 정보

**AdminProductRequest**
```java
{
  name: String (1-200자, 필수)
  description: String (최대 2000자, 선택)
  price: BigDecimal (0보다 큰 양수, 필수)
  stock: Integer (0 이상, 필수)
}
```

**StockUpdateRequest**
```java
{
  stock: Integer (0 이상, 필수)
}
```

**ProductResponse**
```java
{
  id: Long
  name: String
  description: String
  price: BigDecimal
  stock: Integer
}
```

### 응답 래퍼

모든 응답은 `ApiResponse<T>` 래퍼로 감싸져 반환됩니다:
```java
{
  success: Boolean
  data: T | null
  code: String | null
  message: String | null
  timestamp: String (ISO 8601)
}
```

---

**최종 업데이트**: 2026-01-19
