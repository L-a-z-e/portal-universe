---
id: api-search
title: Search API
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-06
author: System
tags: [api, shopping-service, search, elasticsearch, suggest]
related:
  - api-product
---

# Search API

> 상품 검색, 자동완성, 인기/최근 검색어 관리 API

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/shopping/search` |
| **인증** | 검색/자동완성/인기: PUBLIC / 최근 검색어: Bearer Token (선택) |
| **버전** | v1 |
| **검색 엔진** | Elasticsearch 8.x |
| **캐시** | Redis (인기/최근 검색어) |

---

## 📑 API 목록

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/products` | 상품 검색 | PUBLIC |
| GET | `/suggest` | 자동완성 (검색어 추천) | PUBLIC |
| GET | `/popular` | 인기 검색어 조회 | PUBLIC |
| GET | `/recent` | 내 최근 검색어 조회 | USER (선택) |
| POST | `/recent` | 최근 검색어 추가 | USER (선택) |
| DELETE | `/recent/{keyword}` | 최근 검색어 삭제 | USER (선택) |
| DELETE | `/recent` | 최근 검색어 전체 삭제 | USER (선택) |

---

## 🔹 상품 검색

키워드, 가격 범위, 정렬 조건으로 상품을 검색합니다. Elasticsearch 기반의 전문 검색(Full-text search)을 지원합니다.

### Request

```http
GET /api/shopping/search/products?keyword=Spring Boot&minPrice=10000&maxPrice=50000&sort=relevance&page=0&size=20
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `keyword` | string | ❌ | 검색어 | - |
| `minPrice` | double | ❌ | 최소 가격 | - |
| `maxPrice` | double | ❌ | 최대 가격 | - |
| `sort` | string | ❌ | 정렬 기준 (아래 표 참조) | relevance |
| `page` | integer | ❌ | 페이지 번호 (0부터) | 0 |
| `size` | integer | ❌ | 페이지 크기 | 20 |

### 정렬 옵션

| 값 | 설명 |
|----|------|
| `relevance` | 관련도순 (기본값) |
| `price_asc` | 가격 낮은순 |
| `price_desc` | 가격 높은순 |
| `newest` | 최신순 |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "results": [
      {
        "id": 10,
        "name": "Spring Boot 완벽 가이드",
        "description": "Spring Boot 3.x 기반 웹 애플리케이션 개발 가이드",
        "price": 35000.00,
        "stock": 85,
        "highlightedName": "<em>Spring Boot</em> 완벽 가이드",
        "highlightedDescription": "<em>Spring Boot</em> 3.x 기반 웹 애플리케이션 개발 가이드",
        "score": 8.52
      },
      {
        "id": 25,
        "name": "Spring Boot & Kubernetes 실전",
        "description": "마이크로서비스 배포 자동화",
        "price": 42000.00,
        "stock": 30,
        "highlightedName": "<em>Spring Boot</em> & Kubernetes 실전",
        "highlightedDescription": null,
        "score": 6.31
      }
    ],
    "totalHits": 15,
    "page": 0,
    "size": 20,
    "totalPages": 1
  },
  "timestamp": "2026-02-06T14:00:00Z"
}
```

### Search Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `results` | array | 검색 결과 목록 |
| `totalHits` | long | 전체 매칭 건수 |
| `page` | integer | 현재 페이지 |
| `size` | integer | 페이지 크기 |
| `totalPages` | integer | 전체 페이지 수 |

### Search Result Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | long | 상품 ID |
| `name` | string | 상품명 |
| `description` | string | 상품 설명 |
| `price` | decimal | 가격 |
| `stock` | integer | 재고 수량 |
| `highlightedName` | string | 검색어 강조 상품명 (`<em>` 태그) |
| `highlightedDescription` | string | 검색어 강조 설명 (`<em>` 태그) |
| `score` | double | Elasticsearch 관련도 점수 |

### 검색 동작

- **키워드 검색**: `name` (가중치 3배), `description` 필드에 multi-match 쿼리
- **퍼지 검색**: AUTO fuzziness로 오타 허용
- **가격 필터**: Range 쿼리 (minPrice ~ maxPrice)
- **하이라이팅**: 매칭된 키워드를 `<em>` 태그로 감싸서 반환

---

## 🔹 자동완성 (검색어 추천)

입력 중인 키워드에 대한 자동완성 추천을 반환합니다.

### Request

```http
GET /api/shopping/search/suggest?keyword=spr&size=5
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `keyword` | string | ✅ | 입력 키워드 | - |
| `size` | integer | ❌ | 추천 개수 | 5 |

### Response (200 OK)

```json
{
  "success": true,
  "data": [
    "Spring Boot",
    "Spring Security",
    "Spring Cloud",
    "Spring Data JPA",
    "Spring WebFlux"
  ],
  "timestamp": "2026-02-06T14:00:00Z"
}
```

---

## 🔹 인기 검색어 조회

전체 사용자의 검색 빈도 기반 인기 검색어를 조회합니다. Redis Sorted Set에서 상위 N개를 반환합니다.

### Request

```http
GET /api/shopping/search/popular?size=10
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `size` | integer | ❌ | 조회 개수 | 10 |

### Response (200 OK)

```json
{
  "success": true,
  "data": [
    "Spring Boot",
    "Kubernetes",
    "Docker",
    "React",
    "TypeScript",
    "JPA",
    "Redis",
    "Kafka",
    "MSA",
    "DDD"
  ],
  "timestamp": "2026-02-06T14:00:00Z"
}
```

---

## 🔹 내 최근 검색어 조회

현재 사용자의 최근 검색어를 조회합니다. 인증되지 않은 경우 빈 배열을 반환합니다.

### Request

```http
GET /api/shopping/search/recent?size=10
Authorization: Bearer {token}
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `size` | integer | ❌ | 조회 개수 | 10 |

### Response (200 OK)

```json
{
  "success": true,
  "data": [
    "Spring Boot 3",
    "Kubernetes 핸즈온",
    "Docker 입문"
  ],
  "timestamp": "2026-02-06T14:00:00Z"
}
```

---

## 🔹 최근 검색어 추가

현재 사용자의 최근 검색어에 키워드를 추가합니다. 중복 시 기존 항목을 제거하고 최상단에 추가합니다. 최대 20개까지 유지됩니다.

### Request

```http
POST /api/shopping/search/recent?keyword=Spring Boot 3
Authorization: Bearer {token}
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `keyword` | string | ✅ | 추가할 검색어 |

### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-02-06T14:05:00Z"
}
```

---

## 🔹 최근 검색어 삭제

특정 검색어를 최근 검색어에서 삭제합니다.

### Request

```http
DELETE /api/shopping/search/recent/Spring%20Boot
Authorization: Bearer {token}
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `keyword` | string | ✅ | 삭제할 검색어 (URL 인코딩) |

### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-02-06T14:10:00Z"
}
```

---

## 🔹 최근 검색어 전체 삭제

현재 사용자의 모든 최근 검색어를 삭제합니다.

### Request

```http
DELETE /api/shopping/search/recent
Authorization: Bearer {token}
```

### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-02-06T14:15:00Z"
}
```

---

## 💡 기술 상세

### Elasticsearch 인덱스 구조

| 필드 | 타입 | 용도 |
|------|------|------|
| `id` | long | 상품 ID |
| `name` | text (analyzed) | 전문 검색, 가중치 3배 |
| `name.suggest` | completion | 자동완성 |
| `description` | text (analyzed) | 전문 검색 |
| `price` | double | 가격 필터 |
| `stock` | integer | 재고 표시 |

### Redis 저장 구조

| 키 | 타입 | 설명 |
|----|------|------|
| `search:popular` | Sorted Set | 인기 검색어 (keyword → 검색 횟수) |
| `search:recent:{userId}` | List | 사용자별 최근 검색어 (최대 20개) |

---

## ⚠️ 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `S1001` | 500 | 검색 처리에 실패했습니다 |
| `S1002` | 400 | 잘못된 검색 쿼리입니다 |
| `S1003` | 500 | 검색 인덱스를 찾을 수 없습니다 |
| `S1004` | 500 | 자동완성 처리에 실패했습니다 |

---

## 🔗 관련 문서

- [Product API](./product-api.md)

---

**최종 업데이트**: 2026-02-06
