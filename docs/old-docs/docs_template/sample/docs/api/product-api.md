---
id: api-product
title: Product API
type: api
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [api, product]
related:
  - PRD-001
---

# 📦 Product API

**Base URL**: `/api/v1/products`

## Endpoints

### 상품 목록 조회
```
GET /api/v1/products
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| page | int | N | 페이지 번호 (기본: 0) |
| size | int | N | 페이지 크기 (기본: 20) |
| category | string | N | 카테고리 필터 |

**Response (200)**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "상품명",
      "price": 10000,
      "stock": 100
    }
  ],
  "meta": { "page": 0, "size": 20, "total": 50 }
}
```

### 상품 상세 조회
```
GET /api/v1/products/{id}
```

### 상품 등록
```
POST /api/v1/products
```

**Request Body**
```json
{
  "name": "새 상품",
  "description": "상품 설명",
  "price": 15000,
  "stock": 50,
  "categoryId": 1
}
```

### 상품 수정
```
PUT /api/v1/products/{id}
```

### 상품 삭제
```
DELETE /api/v1/products/{id}
```
