---
id: api-admin-timedeal
title: Admin TimeDeal API
type: api
status: current
created: 2026-01-19
updated: 2026-01-19
author: System
tags: [admin, timedeal, shopping, api]
related: []
---

# Admin TimeDeal API

## 📋 개요

관리자가 타임딜(Time Deal)을 생성, 조회, 취소하는 API입니다.

**Base URL**: `/api/shopping/admin/time-deals`

**인증 요구사항**: `ADMIN` 권한 필수 (모든 엔드포인트)

---

## 🔐 권한

모든 엔드포인트는 `ADMIN` 역할이 필요합니다.

```http
Authorization: Bearer {admin_access_token}
```

JWT 토큰의 `authorities`에 `ROLE_ADMIN`이 포함되어야 합니다.

---

## 📡 Endpoints

### 1. 타임딜 생성

새로운 타임딜을 생성합니다.

```http
POST /api/shopping/admin/time-deals
```

**Request Headers**

| 헤더 | 값 | 필수 | 설명 |
|------|------|------|------|
| Authorization | Bearer {token} | Y | 관리자 JWT 토큰 |
| Content-Type | application/json | Y | JSON 형식 |

**Request Body**

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| name | string | Y | 최대 100자 | 타임딜 이름 |
| description | string | N | - | 타임딜 설명 |
| startsAt | datetime | Y | ISO 8601 | 시작 시각 |
| endsAt | datetime | Y | 미래 날짜, ISO 8601 | 종료 시각 |
| products | array | Y | 최소 1개 | 타임딜 상품 목록 |
| products[].productId | number | Y | - | 상품 ID |
| products[].dealPrice | number | Y | ≥ 0.01 | 할인가 |
| products[].dealQuantity | number | Y | ≥ 1 | 할인 수량 |
| products[].maxPerUser | number | Y | ≥ 1 | 사용자당 최대 구매 수량 |

**Request Example**

```json
{
  "name": "주말 특가 세일",
  "description": "주말 한정 특별 할인",
  "startsAt": "2026-01-25T00:00:00Z",
  "endsAt": "2026-01-26T23:59:59Z",
  "products": [
    {
      "productId": 101,
      "dealPrice": 29900,
      "dealQuantity": 100,
      "maxPerUser": 2
    },
    {
      "productId": 102,
      "dealPrice": 19900,
      "dealQuantity": 50,
      "maxPerUser": 1
    }
  ]
}
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "주말 특가 세일",
    "description": "주말 한정 특별 할인",
    "status": "SCHEDULED",
    "startsAt": "2026-01-25T00:00:00Z",
    "endsAt": "2026-01-26T23:59:59Z",
    "products": [
      {
        "id": 1,
        "productId": 101,
        "productName": "무선 이어폰",
        "originalPrice": 49900,
        "dealPrice": 29900,
        "discountRate": 40.08,
        "dealQuantity": 100,
        "soldQuantity": 0,
        "remainingQuantity": 100,
        "maxPerUser": 2,
        "available": true
      },
      {
        "id": 2,
        "productId": 102,
        "productName": "스마트 워치",
        "originalPrice": 39900,
        "dealPrice": 19900,
        "discountRate": 50.13,
        "dealQuantity": 50,
        "soldQuantity": 0,
        "remainingQuantity": 50,
        "maxPerUser": 1,
        "available": true
      }
    ],
    "createdAt": "2026-01-19T10:00:00Z"
  },
  "code": null,
  "message": null,
  "timestamp": "2026-01-19T10:00:00Z"
}
```

**Error Responses**

| HTTP Status | Code | 메시지 | 설명 |
|-------------|------|--------|------|
| 400 | S001 | 잘못된 요청입니다 | Validation 실패 (필수 필드 누락, 제약 위반 등) |
| 401 | C002 | 인증이 필요합니다 | 토큰 없음 또는 만료 |
| 403 | C003 | 권한이 없습니다 | ADMIN 권한 없음 |
| 404 | S002 | 상품을 찾을 수 없습니다 | productId가 존재하지 않음 |
| 500 | C001 | 서버 오류가 발생했습니다 | 내부 서버 오류 |

---

### 2. 타임딜 조회

특정 타임딜의 상세 정보를 조회합니다.

```http
GET /api/shopping/admin/time-deals/{timeDealId}
```

**Path Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| timeDealId | number | Y | 타임딜 ID |

**Request Headers**

| 헤더 | 값 | 필수 | 설명 |
|------|------|------|------|
| Authorization | Bearer {token} | Y | 관리자 JWT 토큰 |

**Request Example**

```http
GET /api/shopping/admin/time-deals/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "주말 특가 세일",
    "description": "주말 한정 특별 할인",
    "status": "ACTIVE",
    "startsAt": "2026-01-25T00:00:00Z",
    "endsAt": "2026-01-26T23:59:59Z",
    "products": [
      {
        "id": 1,
        "productId": 101,
        "productName": "무선 이어폰",
        "originalPrice": 49900,
        "dealPrice": 29900,
        "discountRate": 40.08,
        "dealQuantity": 100,
        "soldQuantity": 35,
        "remainingQuantity": 65,
        "maxPerUser": 2,
        "available": true
      },
      {
        "id": 2,
        "productId": 102,
        "productName": "스마트 워치",
        "originalPrice": 39900,
        "dealPrice": 19900,
        "discountRate": 50.13,
        "dealQuantity": 50,
        "soldQuantity": 50,
        "remainingQuantity": 0,
        "maxPerUser": 1,
        "available": false
      }
    ],
    "createdAt": "2026-01-19T10:00:00Z"
  },
  "code": null,
  "message": null,
  "timestamp": "2026-01-25T12:30:00Z"
}
```

**Error Responses**

| HTTP Status | Code | 메시지 | 설명 |
|-------------|------|--------|------|
| 401 | C002 | 인증이 필요합니다 | 토큰 없음 또는 만료 |
| 403 | C003 | 권한이 없습니다 | ADMIN 권한 없음 |
| 404 | S003 | 타임딜을 찾을 수 없습니다 | timeDealId가 존재하지 않음 |
| 500 | C001 | 서버 오류가 발생했습니다 | 내부 서버 오류 |

---

### 3. 타임딜 취소

진행 중이거나 예정된 타임딜을 취소합니다.

```http
DELETE /api/shopping/admin/time-deals/{timeDealId}
```

**Path Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| timeDealId | number | Y | 타임딜 ID |

**Request Headers**

| 헤더 | 값 | 필수 | 설명 |
|------|------|------|------|
| Authorization | Bearer {token} | Y | 관리자 JWT 토큰 |

**Request Example**

```http
DELETE /api/shopping/admin/time-deals/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK)**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "주말 특가 세일",
    "description": "주말 한정 특별 할인",
    "status": "CANCELLED",
    "startsAt": "2026-01-25T00:00:00Z",
    "endsAt": "2026-01-26T23:59:59Z",
    "products": [
      {
        "id": 1,
        "productId": 101,
        "productName": "무선 이어폰",
        "originalPrice": 49900,
        "dealPrice": 29900,
        "discountRate": 40.08,
        "dealQuantity": 100,
        "soldQuantity": 35,
        "remainingQuantity": 65,
        "maxPerUser": 2,
        "available": false
      },
      {
        "id": 2,
        "productId": 102,
        "productName": "스마트 워치",
        "originalPrice": 39900,
        "dealPrice": 19900,
        "discountRate": 50.13,
        "dealQuantity": 50,
        "soldQuantity": 50,
        "remainingQuantity": 0,
        "maxPerUser": 1,
        "available": false
      }
    ],
    "createdAt": "2026-01-19T10:00:00Z"
  },
  "code": null,
  "message": null,
  "timestamp": "2026-01-24T15:00:00Z"
}
```

**Error Responses**

| HTTP Status | Code | 메시지 | 설명 |
|-------------|------|--------|------|
| 400 | S004 | 이미 종료된 타임딜입니다 | 종료된 타임딜은 취소 불가 |
| 401 | C002 | 인증이 필요합니다 | 토큰 없음 또는 만료 |
| 403 | C003 | 권한이 없습니다 | ADMIN 권한 없음 |
| 404 | S003 | 타임딜을 찾을 수 없습니다 | timeDealId가 존재하지 않음 |
| 500 | C001 | 서버 오류가 발생했습니다 | 내부 서버 오류 |

---

## 📊 Response 필드 설명

### TimeDealResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| id | number | 타임딜 ID |
| name | string | 타임딜 이름 |
| description | string | 타임딜 설명 |
| status | string | 타임딜 상태 (`SCHEDULED`, `ACTIVE`, `ENDED`, `CANCELLED`) |
| startsAt | datetime | 시작 시각 (ISO 8601) |
| endsAt | datetime | 종료 시각 (ISO 8601) |
| products | array | 타임딜 상품 목록 |
| createdAt | datetime | 생성 시각 (ISO 8601) |

### TimeDealProductResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| id | number | 타임딜 상품 ID |
| productId | number | 상품 ID |
| productName | string | 상품명 |
| originalPrice | number | 정가 |
| dealPrice | number | 할인가 |
| discountRate | number | 할인율 (%) |
| dealQuantity | number | 전체 할인 수량 |
| soldQuantity | number | 판매된 수량 |
| remainingQuantity | number | 남은 수량 |
| maxPerUser | number | 사용자당 최대 구매 수량 |
| available | boolean | 구매 가능 여부 |

---

## 🚨 에러 코드

### Common Error Codes

| 코드 | HTTP Status | 메시지 | 설명 |
|------|-------------|--------|------|
| C001 | 500 | 서버 오류가 발생했습니다 | 내부 서버 오류 |
| C002 | 401 | 인증이 필요합니다 | 인증 토큰 없음/만료 |
| C003 | 403 | 권한이 없습니다 | 권한 부족 |

### Shopping Error Codes

| 코드 | HTTP Status | 메시지 | 설명 |
|------|-------------|--------|------|
| S001 | 400 | 잘못된 요청입니다 | Validation 실패 |
| S002 | 404 | 상품을 찾을 수 없습니다 | 존재하지 않는 상품 |
| S003 | 404 | 타임딜을 찾을 수 없습니다 | 존재하지 않는 타임딜 |
| S004 | 400 | 이미 종료된 타임딜입니다 | 종료된 타임딜 취소 시도 |

---

## 📝 비즈니스 규칙

### 타임딜 상태 전환

```mermaid
stateDiagram-v2
    [*] --> SCHEDULED: 생성
    SCHEDULED --> ACTIVE: 시작 시각 도달
    SCHEDULED --> CANCELLED: 관리자 취소
    ACTIVE --> ENDED: 종료 시각 도달
    ACTIVE --> CANCELLED: 관리자 취소
    ENDED --> [*]
    CANCELLED --> [*]
```

### Validation 규칙

1. **시간 규칙**
   - `endsAt`는 반드시 미래 시간이어야 함
   - `endsAt`는 `startsAt`보다 이후여야 함

2. **상품 규칙**
   - 최소 1개 이상의 상품이 포함되어야 함
   - `dealPrice`는 0.01 이상이어야 함
   - `dealQuantity`는 1 이상이어야 함
   - `maxPerUser`는 1 이상이어야 함

3. **취소 규칙**
   - `ENDED` 상태의 타임딜은 취소 불가
   - `CANCELLED` 상태의 타임딜은 중복 취소 불가

---

## 🔗 관련 문서

- [Shopping Service Architecture](../architecture/system-architecture.md)
- [TimeDeal Domain Model](../architecture/domain-model.md)
- [Common Error Handling](../../../../docs/guides/error-handling.md)

---

**최종 업데이트**: 2026-01-19
