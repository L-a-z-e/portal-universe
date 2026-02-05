---
id: api-notification
title: Notification API
type: api
status: current
version: v1
created: 2026-01-30
updated: 2026-01-30
author: Claude
tags: [api, notification-service, notification, rest]
related:
  - notification-service-architecture
---

# Notification API

> 알림 조회, 읽음 처리, 삭제 REST API 명세서. API Gateway를 통해 전달되는 `X-User-Id` 헤더 기반 사용자 식별.

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `http://localhost:8084` (로컬) / `http://notification-service:8084` (Docker/K8s) |
| **API Prefix** | `/api/notifications` |
| **인증 방식** | API Gateway에서 JWT 검증 후 `X-User-Id` 헤더 전달 |
| **총 Endpoints** | 6개 |
| **페이지네이션** | Spring Data `Pageable` (page, size, sort) |

---

## Endpoint Overview

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| `GET` | `/api/notifications` | 알림 목록 조회 (페이징) | ✅ |
| `GET` | `/api/notifications/unread` | 읽지 않은 알림 조회 (페이징) | ✅ |
| `GET` | `/api/notifications/unread/count` | 읽지 않은 알림 수 조회 | ✅ |
| `PUT` | `/api/notifications/{id}/read` | 알림 읽음 처리 | ✅ |
| `PUT` | `/api/notifications/read-all` | 전체 읽음 처리 | ✅ |
| `DELETE` | `/api/notifications/{id}` | 알림 삭제 | ✅ |

---

## 공통 헤더

모든 엔드포인트에 필수로 포함되는 헤더입니다.

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `X-User-Id` | Long | ✅ | 사용자 ID (API Gateway에서 JWT로부터 추출하여 전달) |

---

## 1. 알림 목록 조회

사용자의 모든 알림을 페이징하여 조회합니다.

**`GET /api/notifications`**

### Request

```http
GET /api/notifications?page=0&size=20&sort=createdAt,desc
X-User-Id: 123
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | ❌ | 0 | 페이지 번호 (0부터 시작) |
| `size` | integer | ❌ | 20 | 페이지당 항목 수 |
| `sort` | string | ❌ | - | 정렬 기준 (예: `createdAt,desc`) |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "userId": 123,
        "type": "ORDER_CREATED",
        "title": "주문 접수 알림",
        "message": "주문이 접수되었습니다.",
        "link": "/orders/ORD-20260130-001",
        "status": "UNREAD",
        "referenceId": "ORD-20260130-001",
        "referenceType": "ORDER",
        "createdAt": "2026-01-30T10:00:00",
        "readAt": null
      },
      {
        "id": 2,
        "userId": 123,
        "type": "DELIVERY_COMPLETED",
        "title": "배송 완료 알림",
        "message": "상품이 배송 완료되었습니다.",
        "link": "/orders/ORD-20260125-005",
        "status": "READ",
        "referenceId": "ORD-20260125-005",
        "referenceType": "ORDER",
        "createdAt": "2026-01-29T15:30:00",
        "readAt": "2026-01-29T16:00:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
      },
      "offset": 0,
      "paged": true,
      "unpaged": false
    },
    "totalElements": 45,
    "totalPages": 3,
    "last": false,
    "first": true,
    "size": 20,
    "number": 0,
    "numberOfElements": 20,
    "empty": false
  },
  "error": null,
  "timestamp": "2026-01-30T10:00:00Z"
}
```

---

## 2. 읽지 않은 알림 조회

읽지 않은 알림(`status: UNREAD`)만 페이징하여 조회합니다.

**`GET /api/notifications/unread`**

### Request

```http
GET /api/notifications/unread?page=0&size=20
X-User-Id: 123
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | ❌ | 0 | 페이지 번호 (0부터 시작) |
| `size` | integer | ❌ | 20 | 페이지당 항목 수 |
| `sort` | string | ❌ | - | 정렬 기준 |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "userId": 123,
        "type": "ORDER_CREATED",
        "title": "주문 접수 알림",
        "message": "주문이 접수되었습니다.",
        "link": "/orders/ORD-20260130-001",
        "status": "UNREAD",
        "referenceId": "ORD-20260130-001",
        "referenceType": "ORDER",
        "createdAt": "2026-01-30T10:00:00",
        "readAt": null
      }
    ],
    "totalElements": 12,
    "totalPages": 1,
    "number": 0,
    "size": 20,
    "first": true,
    "last": true,
    "empty": false
  },
  "error": null,
  "timestamp": "2026-01-30T10:00:00Z"
}
```

---

## 3. 읽지 않은 알림 수 조회

사용자의 읽지 않은 알림 개수를 조회합니다. 알림 뱃지 표시에 사용합니다.

**`GET /api/notifications/unread/count`**

### Request

```http
GET /api/notifications/unread/count
X-User-Id: 123
```

### Response (200 OK)

```json
{
  "success": true,
  "data": 12,
  "error": null,
  "timestamp": "2026-01-30T10:00:00Z"
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `data` | Long | 읽지 않은 알림 수 |

---

## 4. 알림 읽음 처리

특정 알림을 읽음 상태(`READ`)로 변경합니다.

**`PUT /api/notifications/{id}/read`**

### Request

```http
PUT /api/notifications/1/read
X-User-Id: 123
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `id` | Long | ✅ | 알림 ID |

### Response (200 OK)

읽음 처리된 알림의 상세 정보를 반환합니다.

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 123,
    "type": "ORDER_CREATED",
    "title": "주문 접수 알림",
    "message": "주문이 접수되었습니다.",
    "link": "/orders/ORD-20260130-001",
    "status": "READ",
    "referenceId": "ORD-20260130-001",
    "referenceType": "ORDER",
    "createdAt": "2026-01-30T10:00:00",
    "readAt": "2026-01-30T10:30:00"
  },
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

## 5. 전체 읽음 처리

사용자의 모든 읽지 않은 알림을 일괄 읽음 처리합니다.

**`PUT /api/notifications/read-all`**

### Request

```http
PUT /api/notifications/read-all
X-User-Id: 123
```

### Response (200 OK)

읽음 처리된 알림 수를 반환합니다.

```json
{
  "success": true,
  "data": 12,
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `data` | Integer | 읽음 처리된 알림 수 |

---

## 6. 알림 삭제

특정 알림을 삭제합니다.

**`DELETE /api/notifications/{id}`**

### Request

```http
DELETE /api/notifications/1
X-User-Id: 123
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `id` | Long | ✅ | 알림 ID |

### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-01-30T10:30:00Z"
}
```

---

## NotificationResponse DTO

모든 알림 조회 응답에서 사용되는 DTO 구조입니다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 알림 고유 ID |
| `userId` | Long | 사용자 ID |
| `type` | NotificationType | 알림 유형 (아래 표 참조) |
| `title` | String | 알림 제목 |
| `message` | String | 알림 본문 |
| `link` | String | 관련 페이지 링크 (nullable) |
| `status` | NotificationStatus | 알림 상태 (`UNREAD`, `READ`) |
| `referenceId` | String | 참조 리소스 ID (nullable) |
| `referenceType` | String | 참조 리소스 타입 (nullable) |
| `createdAt` | LocalDateTime | 생성 시각 |
| `readAt` | LocalDateTime | 읽음 시각 (nullable) |

---

## NotificationType (알림 유형)

| Type | 카테고리 | 기본 메시지 |
|------|----------|------------|
| `ORDER_CREATED` | Order | 주문이 접수되었습니다 |
| `ORDER_CONFIRMED` | Order | 주문이 확정되었습니다 |
| `ORDER_CANCELLED` | Order | 주문이 취소되었습니다 |
| `DELIVERY_STARTED` | Delivery | 상품이 발송되었습니다 |
| `DELIVERY_IN_TRANSIT` | Delivery | 상품이 배송 중입니다 |
| `DELIVERY_COMPLETED` | Delivery | 상품이 배송 완료되었습니다 |
| `PAYMENT_COMPLETED` | Payment | 결제가 완료되었습니다 |
| `PAYMENT_FAILED` | Payment | 결제가 실패했습니다 |
| `REFUND_COMPLETED` | Payment | 환불이 완료되었습니다 |
| `COUPON_ISSUED` | Coupon | 쿠폰이 발급되었습니다 |
| `COUPON_EXPIRING` | Coupon | 쿠폰이 곧 만료됩니다 |
| `TIMEDEAL_STARTING` | TimeDeal | 타임딜이 곧 시작됩니다 |
| `TIMEDEAL_STARTED` | TimeDeal | 타임딜이 시작되었습니다 |
| `SYSTEM` | System | 시스템 알림 |

---

## NotificationStatus (알림 상태)

| Status | 설명 |
|--------|------|
| `UNREAD` | 읽지 않은 알림 |
| `READ` | 읽은 알림 |

---

## API Response Format

모든 API는 `ApiResponse` wrapper를 사용합니다.

### Success Response

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-01-30T10:00:00Z"
}
```

### Error Response

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "C004",
    "message": "Notification not found"
  },
  "timestamp": "2026-01-30T10:00:00Z"
}
```

---

## Error Codes

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `C001` | 400 Bad Request | 잘못된 요청 (유효성 검증 실패) |
| `C002` | 401 Unauthorized | 인증 실패 (X-User-Id 헤더 누락) |
| `C003` | 403 Forbidden | 권한 없음 (다른 사용자의 알림에 접근) |
| `C004` | 404 Not Found | 알림을 찾을 수 없음 |
| `C005` | 500 Internal Server Error | 서버 내부 오류 |

---

## 사용 예시

### 읽지 않은 알림 뱃지 표시

```typescript
async function fetchUnreadCount(userId: number): Promise<number> {
  const response = await apiClient.get('/api/notifications/unread/count', {
    headers: { 'X-User-Id': userId }
  });
  return response.data.data; // Long
}
```

### 알림 목록 무한 스크롤

```typescript
async function loadNotifications(userId: number, page: number) {
  const response = await apiClient.get('/api/notifications', {
    params: { page, size: 20, sort: 'createdAt,desc' },
    headers: { 'X-User-Id': userId }
  });
  return response.data.data; // Page<NotificationResponse>
}
```

### 알림 클릭 시 읽음 처리 및 이동

```typescript
async function onNotificationClick(userId: number, notification: Notification) {
  // 읽음 처리
  await apiClient.put(`/api/notifications/${notification.id}/read`, null, {
    headers: { 'X-User-Id': userId }
  });

  // 링크가 있으면 해당 페이지로 이동
  if (notification.link) {
    router.push(notification.link);
  }
}
```

### 전체 읽음 처리

```typescript
async function markAllAsRead(userId: number): Promise<number> {
  const response = await apiClient.put('/api/notifications/read-all', null, {
    headers: { 'X-User-Id': userId }
  });
  return response.data.data; // 읽음 처리된 알림 수
}
```

---

## 관련 문서

- [Notification Service Architecture](../architecture/system-overview.md)
- [Notification Service README](../README.md)
- [Auth Service API](../../../auth-service/docs/api/auth-api.md)
- [Shopping Service API](../../../shopping-service/docs/api/product-api.md)

---

## 변경 이력

### v1.0.0 (2026-01-30)
- 실제 컨트롤러 코드 기반 전체 재작성
- NotificationResponse DTO 필드 정확히 반영 (10개 필드)
- NotificationType enum 14개 값 문서화
- NotificationStatus enum 문서화
- `markAsRead` 반환 타입 `NotificationResponse`로 수정
- `markAllAsRead` 반환 타입 `Integer`(처리 건수)로 수정
- `X-User-Id` 헤더 타입 `Long`으로 수정
- 사용 예시 추가

---

**최종 업데이트**: 2026-01-30
