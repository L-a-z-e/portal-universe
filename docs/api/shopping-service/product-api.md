---
id: api-product
title: Product API
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-02-17
author: Laze
tags: [api, shopping-service, product]
related:
  - PRD-001
---

# Product API

> 상품 관리 API (생성, 조회, 수정, 삭제, 리뷰 조회)

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/shopping/products` |
| **인증** | Bearer Token 필요 |
| **버전** | v1 |

---

## 📑 API 목록

| Method | Endpoint | 설명 | 인증 | 권한 |
|--------|----------|------|------|------|
| GET | `/` | 상품 목록 조회 | ❌ | - |
| POST | `/` | 상품 생성 (deprecated) | ✅ | SHOPPING_SELLER, SHOPPING_ADMIN, SUPER_ADMIN |
| GET | `/{productId}` | 상품 조회 | ❌ | - |
| PUT | `/{productId}` | 상품 수정 (deprecated) | ✅ | SHOPPING_SELLER, SHOPPING_ADMIN, SUPER_ADMIN |
| DELETE | `/{productId}` | 상품 삭제 (deprecated) | ✅ | SHOPPING_SELLER, SHOPPING_ADMIN, SUPER_ADMIN |
| GET | `/{productId}/with-reviews` | 상품 + 리뷰 조회 | ❌ | - |

> **Note**: POST, PUT, DELETE 엔드포인트는 deprecated 상태입니다. Admin 전용 API는 `AdminProductController`를 사용하세요.

---

## 🔹 상품 목록 조회

페이징된 상품 목록을 조회합니다. (공개 API - 인증 불필요)

### Request

```http
GET /api/shopping/products?page=1&size=12
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `page` | integer | ❌ | 페이지 번호 (1부터 시작) | 1 |
| `size` | integer | ❌ | 페이지 크기 | 12 |
| `category` | string | ❌ | 카테고리 필터 | - |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "name": "MacBook Pro 14\"",
        "description": "Apple M3 Pro 칩, 18GB RAM, 512GB SSD",
        "price": 2390000.0,
        "discountPrice": 2190000.0,
        "stock": 50,
        "imageUrl": "https://picsum.photos/seed/macbook/600/400",
        "category": "전자제품",
        "featured": true,
        "images": [
          "https://picsum.photos/seed/macbook1/600/400",
          "https://picsum.photos/seed/macbook2/600/400"
        ],
        "averageRating": null,
        "reviewCount": null,
        "createdAt": "2026-02-17T10:00:00",
        "updatedAt": "2026-02-17T10:00:00"
      }
    ],
    "page": 1,
    "size": 12,
    "totalElements": 16,
    "totalPages": 2
  },
  "timestamp": "2026-02-17T10:30:00Z"
}
```

---

## 🔹 상품 생성

> **Deprecated**: Admin 전용 API는 `AdminProductController`를 사용하세요.

새로운 상품을 등록합니다. (SHOPPING_SELLER, SHOPPING_ADMIN, SUPER_ADMIN 권한 필요)

### Request

```http
POST /api/shopping/products
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Spring Boot 완벽 가이드",
  "description": "Spring Boot 3.0 기반 마이크로서비스 구축",
  "price": 35000,
  "stockQuantity": 100,
  "category": "BOOK"
}
```

### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `name` | string | ✅ | 상품명 | 1~200자 |
| `description` | string | ❌ | 상품 설명 | 최대 2000자 |
| `price` | integer | ✅ | 가격 | 0 이상 |
| `stockQuantity` | integer | ✅ | 재고 수량 | 0 이상 |
| `category` | string | ❌ | 카테고리 | - |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Spring Boot 완벽 가이드",
    "description": "Spring Boot 3.0 기반 마이크로서비스 구축",
    "price": 35000.0,
    "stock": 100
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

## 🔹 상품 조회

특정 ID를 가진 상품을 조회합니다. (공개 API - 인증 불필요)

### Request

```http
GET /api/shopping/products/{productId}
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `productId` | long | ✅ | 상품 ID |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "MacBook Pro 14\"",
    "description": "Apple M3 Pro 칩, 18GB RAM, 512GB SSD",
    "price": 2390000.0,
    "discountPrice": 2190000.0,
    "stock": 50,
    "imageUrl": "https://picsum.photos/seed/macbook/600/400",
    "category": "전자제품",
    "featured": true,
    "images": [
      "https://picsum.photos/seed/macbook1/600/400",
      "https://picsum.photos/seed/macbook2/600/400"
    ],
    "averageRating": 4.5,
    "reviewCount": 12,
    "createdAt": "2026-02-17T10:00:00",
    "updatedAt": "2026-02-17T10:00:00"
  },
  "timestamp": "2026-02-17T10:30:00Z"
}
```

> **Note**: `averageRating`과 `reviewCount`는 상세 조회 시에만 Blog Service Feign 호출을 통해 계산됩니다. 목록 조회에서는 `null`로 반환됩니다.

### Error Response

```json
{
  "success": false,
  "code": "S001",
  "message": "상품을 찾을 수 없습니다.",
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

## 🔹 상품 수정

> **Deprecated**: Admin 전용 API는 `AdminProductController`를 사용하세요.

특정 상품 정보를 수정합니다. (SHOPPING_SELLER, SHOPPING_ADMIN, SUPER_ADMIN 권한 필요)

### Request

```http
PUT /api/shopping/products/{productId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Spring Boot 완벽 가이드 [개정판]",
  "description": "Spring Boot 3.5 기반 마이크로서비스 구축",
  "price": 38000,
  "stockQuantity": 150
}
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `productId` | long | ✅ | 상품 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | string | ❌ | 상품명 |
| `description` | string | ❌ | 상품 설명 |
| `price` | integer | ❌ | 가격 |
| `stockQuantity` | integer | ❌ | 재고 수량 |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Spring Boot 완벽 가이드 [개정판]",
    "description": "Spring Boot 3.5 기반 마이크로서비스 구축",
    "price": 38000.0,
    "stock": 150
  },
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

## 🔹 상품 삭제

> **Deprecated**: Admin 전용 API는 `AdminProductController`를 사용하세요.

특정 상품을 삭제합니다. (SHOPPING_SELLER, SHOPPING_ADMIN, SUPER_ADMIN 권한 필요)

### Request

```http
DELETE /api/shopping/products/{productId}
Authorization: Bearer {token}
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `productId` | long | ✅ | 상품 ID |

### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

## 🔹 상품 + 리뷰 조회

상품 정보와 해당 상품에 대한 리뷰(블로그 게시물) 목록을 함께 조회합니다.
Blog Service와의 Feign 통신을 통해 데이터를 조합합니다. (공개 API - 인증 불필요)

### Request

```http
GET /api/shopping/products/{productId}/with-reviews
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `productId` | long | ✅ | 상품 ID |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "product": {
      "id": 1,
      "name": "Spring Boot 완벽 가이드",
      "description": "Spring Boot 3.0 기반 마이크로서비스 구축",
      "price": 35000,
      "stockQuantity": 100,
      "category": "BOOK"
    },
    "reviews": [
      {
        "id": "post-123",
        "title": "Spring Boot 책 리뷰",
        "excerpt": "정말 좋은 책입니다...",
        "author": "user1",
        "createdAt": "2026-01-17T10:00:00Z"
      }
    ]
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

## ⚠️ 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `S001` | 404 | 상품을 찾을 수 없습니다 |
| `S002` | 400 | 유효성 검증 실패 |
| `C001` | 401 | 인증 필요 |
| `C002` | 403 | 권한 없음 (SHOPPING_SELLER, SHOPPING_ADMIN, SUPER_ADMIN 전용) |

---

## 🔗 관련 문서

- [Cart API](./cart-api.md)
- [Blog Service API](../blog-service/README.md)

---

---

## 변경 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-02-17 | Product 확장: discountPrice, featured, images, averageRating, reviewCount 필드 추가. 상품 목록 조회에 category 필터 파라미터 추가 |
| 2026-02-08 | 페이지네이션 기본값 수정: page 0 → 1 (ADR-031 정합) |
| 2026-02-07 | 최초 작성 |

---

**최종 업데이트**: 2026-02-17
