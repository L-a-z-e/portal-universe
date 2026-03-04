---
id: api-shopping-product
title: Shopping Product API
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-03-04
author: Laze
tags: [api, shopping, frontend, product]
related: [api-shopping-types, api-shopping-inventory]
---

# Shopping Product API

> 상품 조회/검색 API (Read Model) — CUD는 seller-frontend에서 수행

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/v1/shopping` |
| **인증** | 공개 API (인증 불필요) |
| **엔드포인트** | `productApi`, `adminProductApi` (조회만), `productReviewApi` |

> **Note**: 상품 CUD는 `shopping-seller-frontend` → `shopping-seller-service`를 통해 수행됩니다.

---

## 공개 API (productApi)

### 상품 목록 조회

```typescript
getProducts(page = 1, size = 12, category?: string): Promise<ApiResponse<PageResponse<Product>>>
```

**Endpoint**

```http
GET /api/v1/shopping/products?page=1&size=12&category=전자제품
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `page` | number | ❌ | 페이지 번호 (1부터 시작) | 1 |
| `size` | number | ❌ | 페이지 크기 | 12 |
| `category` | string | ❌ | 카테고리 필터 | - |

**Response**

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "name": "MacBook Pro 14\"",
        "description": "Apple M3 Pro 칩, 18GB RAM, 512GB SSD",
        "price": 2390000,
        "discountPrice": 2190000,
        "imageUrl": "https://picsum.photos/seed/macbook/600/400",
        "category": "전자제품",
        "featured": true,
        "images": [
          "https://picsum.photos/seed/macbook1/600/400"
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
  }
}
```

---

### 상품 상세 조회

```typescript
getProduct(id: number): Promise<ApiResponse<Product>>
```

**Endpoint**

```http
GET /api/v1/shopping/products/{id}
```

**Response**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "MacBook Pro 14\"",
    "description": "Apple M3 Pro 칩, 18GB RAM, 512GB SSD",
    "price": 2390000,
    "discountPrice": 2190000,
    "imageUrl": "https://picsum.photos/seed/macbook/600/400",
    "category": "전자제품",
    "featured": true,
    "images": ["https://picsum.photos/seed/macbook1/600/400"],
    "averageRating": 4.5,
    "reviewCount": 12,
    "createdAt": "2026-02-17T10:00:00",
    "updatedAt": "2026-02-17T10:00:00"
  }
}
```

> **Note**: `averageRating`과 `reviewCount`는 상세 조회에서만 Blog Service Feign 호출을 통해 계산됩니다.

---

### 상품 검색

```typescript
searchProducts(keyword: string, page = 1, size = 12): Promise<ApiResponse<PageResponse<Product>>>
```

**Endpoint**

```http
GET /api/v1/shopping/search/products?keyword=spring&page=1&size=12
```

---

## 관리자 조회 API (adminProductApi)

> 상품 CUD 메서드는 제거되었습니다. 조회 기능만 유지됩니다.

### 관리자 상품 목록 조회

```typescript
getProducts(params: {
  page?: number; size?: number; keyword?: string;
  category?: string; status?: string;
  sortBy?: string; sortOrder?: 'asc' | 'desc'
}): Promise<ApiResponse<PageResponse<Product>>>
```

### 관리자 상품 상세 조회

```typescript
getProduct(id: number): Promise<ApiResponse<Product>>
```

---

## 리뷰 API (productReviewApi)

### 상품 + 리뷰 조회

```typescript
getProductWithReviews(productId: number): Promise<ApiResponse<ProductWithReviews>>
```

**Endpoint**

```http
GET /api/v1/shopping/products/{productId}/with-reviews
```

---

## 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `PRODUCT_NOT_FOUND` | 404 | 상품을 찾을 수 없음 |

---

## 관련 문서

- [Client API](./client-api.md)
- [Inventory API](./inventory-api.md)
- [공통 타입 정의](./types.md)

---

## 변경 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-03-04 | CQRS Read Model 전환: productApi/adminProductApi CUD 제거, stock/stockQuantity 필드 제거, admin hooks 제거 |
| 2026-02-17 | Product 확장: discountPrice, featured, images, averageRating, reviewCount 필드 추가 |
| 2026-02-08 | 페이지네이션 기본값 수정: page 0 → 1 (ADR-031 정합) |
| 2026-02-06 | 최초 작성 |

---

**최종 업데이트**: 2026-03-04
