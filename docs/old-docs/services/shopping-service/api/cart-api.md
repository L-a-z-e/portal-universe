---
id: api-cart
title: Cart API
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [api, shopping-service, cart]
related:
  - PRD-001
  - api-product
  - api-order
---

# Cart API

> 장바구니 관리 API (조회, 아이템 추가/수정/삭제, 체크아웃)

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/shopping/cart` |
| **인증** | Bearer Token 필요 (필수) |
| **버전** | v1 |

---

## 📑 API 목록

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/` | 장바구니 조회 |
| POST | `/items` | 상품 추가 |
| PUT | `/items/{itemId}` | 수량 변경 |
| DELETE | `/items/{itemId}` | 상품 삭제 |
| DELETE | `/` | 장바구니 비우기 |
| POST | `/checkout` | 체크아웃 |

---

## 🔹 장바구니 조회

현재 사용자의 장바구니를 조회합니다.

### Request

```http
GET /api/shopping/cart
Authorization: Bearer {token}
```

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": "user123",
    "items": [
      {
        "id": 1,
        "productId": 10,
        "productName": "Spring Boot 완벽 가이드",
        "price": 35000,
        "quantity": 2,
        "subtotal": 70000
      }
    ],
    "totalAmount": 70000,
    "itemCount": 1,
    "status": "ACTIVE",
    "createdAt": "2026-01-18T10:00:00Z",
    "updatedAt": "2026-01-18T10:30:00Z"
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | long | 장바구니 ID |
| `userId` | string | 사용자 ID |
| `items` | array | 장바구니 아이템 목록 |
| `totalAmount` | integer | 총 금액 |
| `itemCount` | integer | 아이템 개수 |
| `status` | string | 상태 (ACTIVE, CHECKED_OUT) |

---

## 🔹 상품 추가

장바구니에 상품을 추가합니다.

### Request

```http
POST /api/shopping/cart/items
Content-Type: application/json
Authorization: Bearer {token}

{
  "productId": 10,
  "quantity": 2
}
```

### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `productId` | long | ✅ | 상품 ID | - |
| `quantity` | integer | ✅ | 수량 | 1 이상 |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": "user123",
    "items": [
      {
        "id": 1,
        "productId": 10,
        "productName": "Spring Boot 완벽 가이드",
        "price": 35000,
        "quantity": 2,
        "subtotal": 70000
      }
    ],
    "totalAmount": 70000,
    "itemCount": 1,
    "status": "ACTIVE",
    "updatedAt": "2026-01-18T10:30:00Z"
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

### Error Response

```json
{
  "success": false,
  "code": "S001",
  "message": "상품을 찾을 수 없습니다.",
  "timestamp": "2026-01-18T10:30:00Z"
}
```

```json
{
  "success": false,
  "code": "S003",
  "message": "재고가 부족합니다.",
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

## 🔹 수량 변경

장바구니 항목의 수량을 변경합니다.

### Request

```http
PUT /api/shopping/cart/items/{itemId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "quantity": 5
}
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `itemId` | long | ✅ | 장바구니 아이템 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `quantity` | integer | ✅ | 변경할 수량 | 1 이상 |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": "user123",
    "items": [
      {
        "id": 1,
        "productId": 10,
        "productName": "Spring Boot 완벽 가이드",
        "price": 35000,
        "quantity": 5,
        "subtotal": 175000
      }
    ],
    "totalAmount": 175000,
    "itemCount": 1,
    "status": "ACTIVE",
    "updatedAt": "2026-01-18T10:35:00Z"
  },
  "timestamp": "2026-01-18T10:35:00Z"
}
```

---

## 🔹 상품 삭제

장바구니에서 특정 항목을 제거합니다.

### Request

```http
DELETE /api/shopping/cart/items/{itemId}
Authorization: Bearer {token}
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `itemId` | long | ✅ | 장바구니 아이템 ID |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": "user123",
    "items": [],
    "totalAmount": 0,
    "itemCount": 0,
    "status": "ACTIVE",
    "updatedAt": "2026-01-18T10:40:00Z"
  },
  "timestamp": "2026-01-18T10:40:00Z"
}
```

---

## 🔹 장바구니 비우기

장바구니의 모든 항목을 제거합니다.

### Request

```http
DELETE /api/shopping/cart
Authorization: Bearer {token}
```

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": "user123",
    "items": [],
    "totalAmount": 0,
    "itemCount": 0,
    "status": "ACTIVE",
    "updatedAt": "2026-01-18T10:45:00Z"
  },
  "timestamp": "2026-01-18T10:45:00Z"
}
```

---

## 🔹 체크아웃

장바구니를 체크아웃합니다. 체크아웃 후에는 주문 생성 API를 호출해야 합니다.

### Request

```http
POST /api/shopping/cart/checkout
Authorization: Bearer {token}
```

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": "user123",
    "items": [
      {
        "id": 1,
        "productId": 10,
        "productName": "Spring Boot 완벽 가이드",
        "price": 35000,
        "quantity": 2,
        "subtotal": 70000
      }
    ],
    "totalAmount": 70000,
    "itemCount": 1,
    "status": "CHECKED_OUT",
    "updatedAt": "2026-01-18T10:50:00Z"
  },
  "timestamp": "2026-01-18T10:50:00Z"
}
```

### Error Response

```json
{
  "success": false,
  "code": "S004",
  "message": "장바구니가 비어있습니다.",
  "timestamp": "2026-01-18T10:50:00Z"
}
```

---

## 🔄 워크플로우

```
1. 상품 추가 (POST /items)
   ↓
2. 수량 조정 (PUT /items/{itemId})
   ↓
3. 체크아웃 (POST /checkout)
   ↓
4. 주문 생성 (Order API)
```

---

## ⚠️ 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `S001` | 404 | 상품을 찾을 수 없습니다 |
| `S003` | 400 | 재고가 부족합니다 |
| `S004` | 400 | 장바구니가 비어있습니다 |
| `S005` | 400 | 이미 체크아웃된 장바구니입니다 |
| `C001` | 401 | 인증 필요 |

---

## 🔗 관련 문서

- [Product API](./product-api.md)
- [Order API](./order-api.md)

---

**최종 업데이트**: 2026-01-18
