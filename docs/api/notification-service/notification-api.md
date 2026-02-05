---
id: api-notification
title: Notification REST API
type: api
status: current
version: v1
created: 2026-01-30
updated: 2026-02-06
author: Claude
tags: [api, notification-service, notification, rest]
related:
  - notification-events
  - notification-service-architecture
---

# Notification REST API

> 알림 조회, 읽음 처리, 삭제 REST API 명세서. API Gateway를 통해 전달되는 `X-User-Id` 헤더 기반 사용자 식별.

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `http://localhost:8084` (로컬) / `http://notification-service:8084` (Docker/K8s) |
| **API Prefix** | `/api/v1/notifications` |
| **인증 방식** | API Gateway에서 JWT 검증 후 `X-User-Id` 헤더 전달 |
| **총 Endpoints** | 6개 |
| **페이지네이션** | Spring Data `Pageable` (page, size, sort) |
| **응답 형식** | `ApiResponse<T>` wrapper |

---

## Endpoint Overview

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| `GET` | `/api/v1/notifications` | 알림 목록 조회 (페이징) | ✅ |
| `GET` | `/api/v1/notifications/unread` | 읽지 않은 알림 조회 (페이징) | ✅ |
| `GET` | `/api/v1/notifications/unread/count` | 읽지 않은 알림 수 조회 | ✅ |
| `PUT` | `/api/v1/notifications/{id}/read` | 알림 읽음 처리 | ✅ |
| `PUT` | `/api/v1/notifications/read-all` | 전체 읽음 처리 | ✅ |
| `DELETE` | `/api/v1/notifications/{id}` | 알림 삭제 | ✅ |

---

## 공통 헤더

모든 엔드포인트에 필수로 포함되는 헤더입니다.

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `X-User-Id` | String | ✅ | 사용자 ID (API Gateway에서 JWT로부터 추출하여 전달, 최대 36자) |

---

## 1. 알림 목록 조회

사용자의 모든 알림을 최신순으로 페이징하여 조회합니다.

**`GET /api/v1/notifications`**

### Request

```http
GET /api/v1/notifications?page=0&size=20&sort=createdAt,desc
X-User-Id: user-123
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | - | 0 | 페이지 번호 (0부터 시작) |
| `size` | integer | - | 20 | 페이지당 항목 수 |
| `sort` | string | - | `createdAt,desc` | 정렬 기준 |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "userId": "user-123",
        "type": "ORDER_CREATED",
        "title": "주문이 접수되었습니다",
        "message": "2개 상품, 45,000원 결제 대기중",
        "link": "/shopping/orders/ORD-20260206-001",
        "status": "UNREAD",
        "referenceId": "ORD-20260206-001",
        "referenceType": "order",
        "createdAt": "2026-02-06T10:00:00",
        "readAt": null
      },
      {
        "id": 2,
        "userId": "user-123",
        "type": "BLOG_LIKE",
        "title": "게시글에 좋아요가 달렸습니다",
        "message": "\"Spring Boot 가이드\"에 kim님이 좋아요를 눌렀습니다",
        "link": "/blog/post-456",
        "status": "READ",
        "referenceId": "like-789",
        "referenceType": "like",
        "createdAt": "2026-02-05T15:30:00",
        "readAt": "2026-02-05T16:00:00"
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
  }
}
```

---

## 2. 읽지 않은 알림 조회

읽지 않은 알림(`status: UNREAD`)만 최신순으로 페이징하여 조회합니다.

**`GET /api/v1/notifications/unread`**

### Request

```http
GET /api/v1/notifications/unread?page=0&size=20
X-User-Id: user-123
```

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `page` | integer | - | 0 | 페이지 번호 (0부터 시작) |
| `size` | integer | - | 20 | 페이지당 항목 수 |
| `sort` | string | - | `createdAt,desc` | 정렬 기준 |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "userId": "user-123",
        "type": "ORDER_CREATED",
        "title": "주문이 접수되었습니다",
        "message": "2개 상품, 45,000원 결제 대기중",
        "link": "/shopping/orders/ORD-20260206-001",
        "status": "UNREAD",
        "referenceId": "ORD-20260206-001",
        "referenceType": "order",
        "createdAt": "2026-02-06T10:00:00",
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
  }
}
```

---

## 3. 읽지 않은 알림 수 조회

사용자의 읽지 않은 알림 개수를 조회합니다. 알림 뱃지 표시에 사용합니다.

**`GET /api/v1/notifications/unread/count`**

### Request

```http
GET /api/v1/notifications/unread/count
X-User-Id: user-123
```

### Response (200 OK)

```json
{
  "success": true,
  "data": 12
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `data` | long | 읽지 않은 알림 수 |

---

## 4. 알림 읽음 처리

특정 알림을 읽음 상태(`READ`)로 변경합니다. 이미 읽음 상태인 알림에 대해서도 정상 응답합니다 (멱등성).

**`PUT /api/v1/notifications/{id}/read`**

### Request

```http
PUT /api/v1/notifications/1/read
X-User-Id: user-123
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `id` | Long | ✅ | 알림 ID |

### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": "user-123",
    "type": "ORDER_CREATED",
    "title": "주문이 접수되었습니다",
    "message": "2개 상품, 45,000원 결제 대기중",
    "link": "/shopping/orders/ORD-20260206-001",
    "status": "READ",
    "referenceId": "ORD-20260206-001",
    "referenceType": "order",
    "createdAt": "2026-02-06T10:00:00",
    "readAt": "2026-02-06T10:30:00"
  }
}
```

### Error Response (404 Not Found)

해당 ID의 알림이 없거나 다른 사용자의 알림인 경우:

```json
{
  "success": false,
  "error": {
    "code": "N001",
    "message": "Notification not found"
  }
}
```

---

## 5. 전체 읽음 처리

사용자의 모든 읽지 않은 알림을 일괄 읽음 처리합니다.

**`PUT /api/v1/notifications/read-all`**

### Request

```http
PUT /api/v1/notifications/read-all
X-User-Id: user-123
```

### Response (200 OK)

```json
{
  "success": true,
  "data": 12
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `data` | int | 읽음 처리된 알림 수 |

---

## 6. 알림 삭제

특정 알림을 삭제합니다. 해당 사용자의 알림만 삭제 가능합니다.

**`DELETE /api/v1/notifications/{id}`**

### Request

```http
DELETE /api/v1/notifications/1
X-User-Id: user-123
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `id` | Long | ✅ | 알림 ID |

### Response (200 OK)

```json
{
  "success": true
}
```

> `data` 필드는 `null`이므로 `@JsonInclude(NON_NULL)` 설정에 의해 응답에 포함되지 않습니다.

---

## NotificationResponse DTO

모든 알림 조회 응답에서 사용되는 DTO 구조입니다.

| 필드 | 타입 | Nullable | 설명 |
|------|------|----------|------|
| `id` | Long | - | 알림 고유 ID |
| `userId` | String | - | 사용자 ID (최대 36자) |
| `type` | NotificationType | - | 알림 유형 (아래 표 참조) |
| `title` | String | - | 알림 제목 |
| `message` | String | - | 알림 본문 (TEXT) |
| `link` | String | ✅ | 관련 페이지 링크 (최대 500자) |
| `status` | NotificationStatus | - | 알림 상태 (`UNREAD`, `READ`) |
| `referenceId` | String | ✅ | 참조 리소스 ID (최대 100자) |
| `referenceType` | String | ✅ | 참조 리소스 타입 (최대 50자) |
| `createdAt` | LocalDateTime | - | 생성 시각 |
| `readAt` | LocalDateTime | ✅ | 읽음 시각 |

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
| `BLOG_LIKE` | Blog | 회원님의 글에 좋아요가 눌렸습니다 |
| `BLOG_COMMENT` | Blog | 새 댓글이 달렸습니다 |
| `BLOG_REPLY` | Blog | 답글이 달렸습니다 |
| `BLOG_FOLLOW` | Blog | 새 팔로워가 생겼습니다 |
| `BLOG_NEW_POST` | Blog | 새 글이 작성되었습니다 |
| `PRISM_TASK_COMPLETED` | Prism (AI) | AI 작업이 완료되었습니다 |
| `PRISM_TASK_FAILED` | Prism (AI) | AI 작업이 실패했습니다 |
| `SYSTEM` | System | 시스템 알림 |

---

## NotificationStatus (알림 상태)

| Status | 설명 |
|--------|------|
| `UNREAD` | 읽지 않은 알림 |
| `READ` | 읽은 알림 |

---

## API Response Format

모든 API는 `ApiResponse<T>` wrapper를 사용합니다. `@JsonInclude(NON_NULL)` 설정에 의해 `null` 값을 가진 필드는 응답에서 제외됩니다.

### Success Response

```json
{
  "success": true,
  "data": { ... }
}
```

### Error Response

```json
{
  "success": false,
  "error": {
    "code": "N001",
    "message": "Notification not found"
  }
}
```

---

## Error Codes

### Notification Service 전용

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `N001` | 404 Not Found | 알림을 찾을 수 없음 (잘못된 ID 또는 다른 사용자의 알림) |
| `N002` | 500 Internal Server Error | 알림 전송 실패 |
| `N003` | 400 Bad Request | 유효하지 않은 알림 타입 |

### 공통 에러

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `C001` | 401 Unauthorized | 인증 실패 (X-User-Id 헤더 누락) |
| `C002` | 403 Forbidden | 권한 없음 |
| `C003` | 400 Bad Request | 잘못된 요청 (Validation 실패) |

---

## 비즈니스 규칙

### 중복 알림 방지

`referenceId + referenceType + userId` 조합이 동일한 알림이 이미 존재하면 새로 생성하지 않고 기존 알림을 반환합니다. 이는 Kafka 이벤트 재처리(at-least-once) 시 중복 알림이 생성되는 것을 방지합니다.

### 읽음 처리 멱등성

이미 `READ` 상태인 알림에 대해 `markAsRead`를 호출해도 에러가 발생하지 않으며, 현재 상태를 그대로 반환합니다.

### 삭제 권한

알림 삭제는 `userId`로 소유권을 확인합니다. 자신의 알림만 삭제할 수 있습니다.

---

## 사용 예시

### 읽지 않은 알림 뱃지 표시

```typescript
async function fetchUnreadCount(userId: string): Promise<number> {
  const response = await apiClient.get('/api/v1/notifications/unread/count', {
    headers: { 'X-User-Id': userId }
  });
  return response.data.data;
}
```

### 알림 목록 무한 스크롤

```typescript
async function loadNotifications(userId: string, page: number) {
  const response = await apiClient.get('/api/v1/notifications', {
    params: { page, size: 20, sort: 'createdAt,desc' },
    headers: { 'X-User-Id': userId }
  });
  return response.data.data; // Page<NotificationResponse>
}
```

### 알림 클릭 시 읽음 처리 및 이동

```typescript
async function onNotificationClick(userId: string, notification: Notification) {
  await apiClient.put(`/api/v1/notifications/${notification.id}/read`, null, {
    headers: { 'X-User-Id': userId }
  });
  if (notification.link) {
    router.push(notification.link);
  }
}
```

### 전체 읽음 처리

```typescript
async function markAllAsRead(userId: string): Promise<number> {
  const response = await apiClient.put('/api/v1/notifications/read-all', null, {
    headers: { 'X-User-Id': userId }
  });
  return response.data.data; // 읽음 처리된 알림 수
}
```

---

## 관련 문서

- [Notification Events & Real-time Push](./notification-events.md)
- [Notification Service Architecture](../../architecture/notification-service/system-overview.md)
- [Auth Service API](../auth-service/auth-api.md)
- [Shopping Service API](../shopping-service/)
- [Blog Service API](../blog-service/)

---

## 변경 이력

### v1.1.0 (2026-02-06)
- API prefix `/api/notifications` -> `/api/v1/notifications` 수정
- `X-User-Id` 헤더 타입 `Long` -> `String` 수정
- `userId` 필드 타입 `Long` -> `String` 수정
- NotificationType 7개 추가 (Blog 5개, Prism 2개) - 총 21개
- Error Code `C00x` -> `N00x` (서비스 전용 코드) 수정
- ApiResponse 형식에서 `timestamp` 필드 제거 (실제 코드에 없음)
- `@JsonInclude(NON_NULL)` 동작 반영
- 중복 알림 방지 규칙, 멱등성, 삭제 권한 비즈니스 규칙 추가
- 관련 문서 링크 수정
- Kafka Events 문서 분리 (notification-events.md)

### v1.0.0 (2026-01-30)
- 실제 컨트롤러 코드 기반 전체 재작성
- 초기 버전 발행

---

**최종 업데이트**: 2026-02-06
