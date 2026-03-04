---
id: api-product
title: Product API
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-03-04
author: Laze
tags: [api, shopping-service, product]
related:
  - PRD-001
---

# Product API

> 상품 조회 API (Read Model) — CUD는 seller-service에서 수행

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/shopping/products` |
| **인증** | 공개 API (인증 불필요) |
| **버전** | v1 |
| **역할** | CQRS Read Model — seller-service의 이벤트를 Kafka로 수신하여 동기화 |

---

## 📑 API 목록

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/` | 상품 목록 조회 | ❌ |
| GET | `/categories` | 카테고리 목록 조회 | ❌ |
| GET | `/{productId}` | 상품 상세 조회 | ❌ |
| GET | `/{productId}/with-reviews` | 상품 + 리뷰 조회 | ❌ |

> **Note**: 상품 CUD는 `shopping-seller-service`의 Product API를 통해 수행됩니다.

---

## 🔹 상품 목록 조회

페이징된 상품 목록을 조회합니다.

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

## 🔹 상품 상세 조회

특정 ID를 가진 상품을 조회합니다. 상세 조회 시 Blog Service를 통해 리뷰 통계를 함께 반환합니다.

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

## 🔹 상품 + 리뷰 조회

상품 정보와 해당 상품에 대한 리뷰(블로그 게시물) 목록을 함께 조회합니다.
Blog Service와의 Feign 통신을 통해 데이터를 조합합니다.

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
    "id": 1,
    "name": "MacBook Pro 14\"",
    "description": "Apple M3 Pro 칩, 18GB RAM, 512GB SSD",
    "price": 2390000.0,
    "imageUrl": "https://picsum.photos/seed/macbook/600/400",
    "category": "전자제품",
    "reviews": [
      {
        "id": "post-123",
        "title": "MacBook 리뷰",
        "excerpt": "정말 좋은 제품입니다...",
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

---

## 🔗 관련 문서

- [Seller Product API](../shopping-seller-service/product-api.md) — 상품 CUD
- [Cart API](./cart-api.md)
- [Blog Service API](../blog-service/README.md)

---

## 변경 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-03-04 | CQRS Read Model 전환: CUD 엔드포인트 제거, stock 필드 제거 (재고는 Inventory로 관리) |
| 2026-02-17 | Product 확장: discountPrice, featured, images, averageRating, reviewCount 필드 추가. 상품 목록 조회에 category 필터 파라미터 추가 |
| 2026-02-08 | 페이지네이션 기본값 수정: page 0 → 1 (ADR-031 정합) |
| 2026-02-07 | 최초 작성 |

---

**최종 업데이트**: 2026-03-04
