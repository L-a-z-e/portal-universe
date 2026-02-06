---
id: api-inventory
title: Inventory API
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [api, shopping-service, inventory, stock, sse]
related:
  - api-product
  - api-order
  - api-admin-product
---

# Inventory API

> 재고 조회, 입고, 이동 이력 및 실시간 재고 스트림(SSE) API

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/shopping/inventory` |
| **인증** | 조회: PUBLIC / 입고, 초기화: Bearer Token (관리자) |
| **버전** | v1 |

---

## 📑 API 목록

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/{productId}` | 단일 상품 재고 조회 | PUBLIC |
| POST | `/batch` | 다중 상품 재고 일괄 조회 | PUBLIC |
| POST | `/{productId}` | 재고 초기화 | USER |
| PUT | `/{productId}/add` | 재고 추가 (입고) | USER |
| GET | `/{productId}/movements` | 재고 이동 이력 조회 | PUBLIC |
| GET | `/stream?productIds=` | 실시간 재고 스트림 (SSE) | PUBLIC |

---

## 🔹 단일 상품 재고 조회

특정 상품의 현재 재고 상태를 조회합니다.

### Request

```http
GET /api/shopping/inventory/{productId}
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
    "productId": 10,
    "availableQuantity": 85,
    "reservedQuantity": 15,
    "totalQuantity": 100,
    "createdAt": "2026-01-10T09:00:00Z",
    "updatedAt": "2026-02-06T10:30:00Z"
  },
  "timestamp": "2026-02-06T10:30:00Z"
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | long | 재고 ID |
| `productId` | long | 상품 ID |
| `availableQuantity` | integer | 판매 가능 수량 |
| `reservedQuantity` | integer | 주문 예약 수량 (결제 대기) |
| `totalQuantity` | integer | 전체 수량 (available + reserved) |
| `createdAt` | string | 생성일시 |
| `updatedAt` | string | 최종 수정일시 |

### Error Response

```json
{
  "success": false,
  "code": "S401",
  "message": "재고 정보를 찾을 수 없습니다.",
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

## 🔹 다중 상품 재고 일괄 조회

여러 상품의 재고를 한번에 조회합니다.

### Request

```http
POST /api/shopping/inventory/batch
Content-Type: application/json

{
  "productIds": [10, 20, 30]
}
```

### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `productIds` | array(long) | ✅ | 상품 ID 목록 | 비어있으면 안됨 |

### Response (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "productId": 10,
      "availableQuantity": 85,
      "reservedQuantity": 15,
      "totalQuantity": 100,
      "createdAt": "2026-01-10T09:00:00Z",
      "updatedAt": "2026-02-06T10:30:00Z"
    },
    {
      "id": 2,
      "productId": 20,
      "availableQuantity": 200,
      "reservedQuantity": 0,
      "totalQuantity": 200,
      "createdAt": "2026-01-15T09:00:00Z",
      "updatedAt": "2026-02-05T14:00:00Z"
    }
  ],
  "timestamp": "2026-02-06T10:30:00Z"
}
```

---

## 🔹 재고 초기화

신규 상품의 초기 재고를 설정합니다. 이미 재고가 존재하면 409 에러가 반환됩니다.

### Request

```http
POST /api/shopping/inventory/{productId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "quantity": 100,
  "reason": "신규 상품 입고"
}
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `productId` | long | ✅ | 상품 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `quantity` | integer | ✅ | 초기 재고 수량 | 0 이상 |
| `reason` | string | ❌ | 초기화 사유 | - |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 5,
    "productId": 50,
    "availableQuantity": 100,
    "reservedQuantity": 0,
    "totalQuantity": 100,
    "createdAt": "2026-02-06T11:00:00Z",
    "updatedAt": "2026-02-06T11:00:00Z"
  },
  "timestamp": "2026-02-06T11:00:00Z"
}
```

### Error Response

```json
{
  "success": false,
  "code": "S407",
  "message": "해당 상품의 재고가 이미 존재합니다.",
  "timestamp": "2026-02-06T11:00:00Z"
}
```

---

## 🔹 재고 추가 (입고)

기존 상품의 재고를 추가합니다.

### Request

```http
PUT /api/shopping/inventory/{productId}/add
Content-Type: application/json
Authorization: Bearer {token}

{
  "quantity": 50,
  "reason": "2월 정기 입고"
}
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `productId` | long | ✅ | 상품 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `quantity` | integer | ✅ | 추가할 수량 | 0 이상 |
| `reason` | string | ❌ | 입고 사유 | - |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "productId": 10,
    "availableQuantity": 135,
    "reservedQuantity": 15,
    "totalQuantity": 150,
    "createdAt": "2026-01-10T09:00:00Z",
    "updatedAt": "2026-02-06T11:30:00Z"
  },
  "timestamp": "2026-02-06T11:30:00Z"
}
```

---

## 🔹 재고 이동 이력 조회

특정 상품의 재고 변동 내역을 페이징으로 조회합니다.

### Request

```http
GET /api/shopping/inventory/{productId}/movements?page=0&size=20&sort=createdAt,desc
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `productId` | long | ✅ | 상품 ID |

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `page` | integer | ❌ | 페이지 번호 (0부터) | 0 |
| `size` | integer | ❌ | 페이지 크기 | 20 |
| `sort` | string | ❌ | 정렬 | createdAt,desc |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 150,
        "productId": 10,
        "movementType": "INBOUND",
        "movementTypeDescription": "입고",
        "quantity": 50,
        "previousAvailable": 85,
        "afterAvailable": 135,
        "previousReserved": 15,
        "afterReserved": 15,
        "referenceType": "ADMIN",
        "referenceId": null,
        "reason": "2월 정기 입고",
        "performedBy": "admin001",
        "createdAt": "2026-02-06T11:30:00Z"
      },
      {
        "id": 149,
        "productId": 10,
        "movementType": "RESERVE",
        "movementTypeDescription": "예약",
        "quantity": 3,
        "previousAvailable": 88,
        "afterAvailable": 85,
        "previousReserved": 12,
        "afterReserved": 15,
        "referenceType": "ORDER",
        "referenceId": "ORD-20260206-0015",
        "reason": null,
        "performedBy": "user456",
        "createdAt": "2026-02-06T10:15:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8
    }
  },
  "timestamp": "2026-02-06T11:30:00Z"
}
```

### Movement Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | long | 이동 이력 ID |
| `productId` | long | 상품 ID |
| `movementType` | string | 이동 유형 (아래 표 참조) |
| `movementTypeDescription` | string | 이동 유형 한글 설명 |
| `quantity` | integer | 이동 수량 |
| `previousAvailable` | integer | 변경 전 가용 재고 |
| `afterAvailable` | integer | 변경 후 가용 재고 |
| `previousReserved` | integer | 변경 전 예약 재고 |
| `afterReserved` | integer | 변경 후 예약 재고 |
| `referenceType` | string | 참조 유형 (ORDER, PAYMENT, RETURN, ADMIN) |
| `referenceId` | string | 참조 ID (주문번호, 결제번호 등) |
| `reason` | string | 이동 사유 |
| `performedBy` | string | 수행자 (사용자 ID 또는 SYSTEM) |
| `createdAt` | string | 발생일시 |

### MovementType 목록

| 값 | 설명 |
|----|------|
| `INITIAL` | 최초 재고 설정 |
| `INBOUND` | 입고 (재고 추가) |
| `RESERVE` | 주문에 의한 재고 예약 |
| `RELEASE` | 예약 해제 (주문 취소 등) |
| `DEDUCT` | 결제 완료 후 실제 차감 |
| `RETURN` | 반품에 의한 재고 복원 |
| `ADJUSTMENT` | 관리자 수동 조정 |

---

## 🔹 실시간 재고 스트림 (SSE)

Server-Sent Events를 통해 특정 상품들의 재고 변동을 실시간으로 수신합니다.

### Request

```http
GET /api/shopping/inventory/stream?productIds=10,20,30
Accept: text/event-stream
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `productIds` | array(long) | ✅ | 구독할 상품 ID 목록 (쉼표 구분) |

### SSE Event Format

```
event: inventory-update
data: {"productId":10,"available":132,"reserved":18,"timestamp":"2026-02-06T11:35:00Z"}

event: inventory-update
data: {"productId":20,"available":198,"reserved":2,"timestamp":"2026-02-06T11:35:05Z"}

: heartbeat
```

### SSE Event Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `productId` | long | 상품 ID |
| `available` | integer | 현재 가용 재고 |
| `reserved` | integer | 현재 예약 재고 |
| `timestamp` | string | 변동 발생 시각 |

### JavaScript 구현 예시

```javascript
const productIds = [10, 20, 30];
const url = `/api/shopping/inventory/stream?productIds=${productIds.join(',')}`;
const eventSource = new EventSource(url);

eventSource.addEventListener('inventory-update', (event) => {
  const data = JSON.parse(event.data);
  console.log(`상품 ${data.productId}: 가용 ${data.available}, 예약 ${data.reserved}`);
});

eventSource.onerror = (error) => {
  console.error('SSE 연결 오류:', error);
  eventSource.close();
};
```

### 기술 상세

- **Heartbeat**: 30초 간격으로 heartbeat 전송 (연결 유지)
- **백엔드**: Redis Pub/Sub + WebFlux Sinks.Many 기반
- **채널**: `inventory:{productId}` Redis 채널로 발행/구독
- **구독자 관리**: 마지막 구독자 해제 시 자동 리스너 정리

---

## 🔄 재고 상태 모델

```
┌─────────────────────────────────────────┐
│              Total Quantity              │
│  ┌──────────────────┐ ┌───────────────┐ │
│  │    Available     │ │   Reserved    │ │
│  │   (판매 가능)    │ │  (결제 대기)  │ │
│  └──────────────────┘ └───────────────┘ │
└─────────────────────────────────────────┘

주문 생성 → Available에서 Reserved로 이동 (RESERVE)
결제 완료 → Reserved에서 차감 (DEDUCT)
주문 취소 → Reserved에서 Available로 복원 (RELEASE)
입고      → Available 증가 (INBOUND)
반품      → Available 증가 (RETURN)
```

---

## 🔒 동시성 제어

- **비관적 락 (Pessimistic Lock)**: `SELECT ... FOR UPDATE` 사용
- **락 타임아웃**: 3초
- **데드락 방지**: 배치 처리 시 상품 ID를 정렬(TreeMap)하여 일정한 순서로 락 획득
- **낙관적 락**: `@Version` 필드로 추가 안전장치

---

## ⚠️ 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `S401` | 404 | 재고 정보를 찾을 수 없습니다 |
| `S402` | 400 | 재고가 부족합니다 |
| `S403` | 500 | 재고 예약에 실패했습니다 |
| `S404` | 500 | 재고 해제에 실패했습니다 |
| `S405` | 500 | 재고 차감에 실패했습니다 |
| `S406` | 400 | 비정상적인 재고 수량입니다 |
| `S407` | 409 | 해당 상품의 재고가 이미 존재합니다 |
| `S408` | 409 | 동시 수정 충돌이 발생했습니다 |
| `C001` | 401 | 인증이 필요합니다 |

---

## 🔗 관련 문서

- [Product API](./product-api.md)
- [Admin Product API](./admin-product-api.md)
- [Order API](./order-api.md)

---

**최종 업데이트**: 2026-02-06
