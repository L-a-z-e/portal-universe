---
id: api-shopping-queue
title: Shopping Queue API
type: api
status: current
version: v1
created: 2026-02-06
updated: 2026-02-06
author: Laze
tags: [api, shopping, frontend, queue, sse, admin]
related: [api-shopping-types, api-shopping-timedeal]
---

# Shopping Queue API

> 대기열 관리 API (SSE 기반, 공개 + 관리자)

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/v1/shopping/queue` |
| **인증** | Bearer Token (필수) |
| **SSE 지원** | Server-Sent Events (실시간 대기열 상태) |
| **엔드포인트** | `queueApi`, `adminQueueApi` |

---

## 공개 API (queueApi)

### 대기열 진입

```typescript
enterQueue(eventType: string, eventId: number): Promise<ApiResponse<QueueStatusResponse>>
```

**Endpoint**: `POST /api/v1/shopping/queue/{eventType}/{eventId}/enter`

**Response**

```json
{
  "success": true,
  "data": {
    "entryToken": "QT-20260206-ABC123",
    "status": "WAITING",
    "position": 45,
    "estimatedWaitSeconds": 90,
    "totalWaiting": 120,
    "message": "대기 중입니다. 예상 대기 시간: 1분 30초"
  }
}
```

---

### 대기열 상태 조회

```typescript
getQueueStatus(eventType: string, eventId: number): Promise<ApiResponse<QueueStatusResponse>>
```

**Endpoint**: `GET /api/v1/shopping/queue/{eventType}/{eventId}/status`

---

### 토큰으로 대기열 상태 조회

```typescript
getQueueStatusByToken(entryToken: string): Promise<ApiResponse<QueueStatusResponse>>
```

**Endpoint**: `GET /api/v1/shopping/queue/token/{entryToken}`

---

### 대기열 이탈

```typescript
leaveQueue(eventType: string, eventId: number): Promise<ApiResponse<void>>
```

**Endpoint**: `DELETE /api/v1/shopping/queue/{eventType}/{eventId}/leave`

---

### 토큰으로 대기열 이탈

```typescript
leaveQueueByToken(entryToken: string): Promise<ApiResponse<void>>
```

**Endpoint**: `DELETE /api/v1/shopping/queue/token/{entryToken}`

---

### SSE 구독 URL 생성

```typescript
getSubscribeUrl(eventType: string, eventId: number, entryToken: string): string
```

실시간 대기열 상태 업데이트를 위한 SSE URL을 생성합니다.

**URL**: `/api/v1/shopping/queue/{eventType}/{eventId}/subscribe/{entryToken}`

---

## 관리자 API (adminQueueApi)

### 대기열 활성화

```typescript
activateQueue(eventType: string, eventId: number, request: QueueActivateRequest): Promise<ApiResponse<void>>
```

**Endpoint**: `POST /api/v1/shopping/admin/queue/{eventType}/{eventId}/activate`

**Request Body**

```json
{
  "maxCapacity": 100,
  "entryBatchSize": 10,
  "entryIntervalSeconds": 30
}
```

**Request Parameters**

| 필드 | 타입 | 설명 |
|------|------|------|
| `maxCapacity` | number | 최대 수용 인원 |
| `entryBatchSize` | number | 1회 입장 인원 |
| `entryIntervalSeconds` | number | 입장 간격 (초) |

---

### 대기열 비활성화

```typescript
deactivateQueue(eventType: string, eventId: number): Promise<ApiResponse<void>>
```

**Endpoint**: `POST /api/v1/shopping/admin/queue/{eventType}/{eventId}/deactivate`

---

### 대기열 수동 처리

```typescript
processQueue(eventType: string, eventId: number): Promise<ApiResponse<void>>
```

**Endpoint**: `POST /api/v1/shopping/admin/queue/{eventType}/{eventId}/process`

---

## React Hooks

### useQueue

대기열 상태 관리 및 SSE 연결

```typescript
import { useQueue } from '@/hooks/useQueue'

export function QueuePage() {
  const { status, isLoading, error, isConnected, enterQueue, leaveQueue, entryToken } = useQueue({
    eventType: 'timedeal',
    eventId: 1,
    autoEnter: true  // 자동 진입
  })

  if (isLoading) return <div>로딩 중...</div>
  if (error) return <div>에러: {error.message}</div>

  if (status?.status === 'ENTERED') {
    return <div>입장 완료! 구매 페이지로 이동하세요</div>
  }

  return (
    <div>
      <h2>대기 중</h2>
      <p>대기 순번: {status?.position}</p>
      <p>예상 대기 시간: {formatWaitTime(status?.estimatedWaitSeconds || 0)}</p>
      <p>전체 대기 인원: {status?.totalWaiting}</p>
      {isConnected && <span>🔴 실시간 연결됨</span>}
      <button onClick={leaveQueue}>대기열 나가기</button>
    </div>
  )
}
```

### useQueuePolling

SSE 미지원 환경용 폴링 Hook

```typescript
import { useQueuePolling } from '@/hooks/useQueue'

const { status, isLoading, error } = useQueuePolling(entryToken, 3000)
```

---

## Helper Functions

### 예상 대기 시간 포맷

```typescript
import { formatWaitTime } from '@/hooks/useQueue'

const formatted = formatWaitTime(seconds)
// 30초 → "약 30초"
// 90초 → "약 1분 30초"
// 3660초 → "약 1시간 1분"
```

---

## SSE 이벤트

### queue-status 이벤트

```javascript
eventSource.addEventListener('queue-status', (event) => {
  const data = JSON.parse(event.data)
  // data: QueueStatusResponse
  console.log('대기 순번:', data.position)
  console.log('상태:', data.status) // WAITING | ENTERED | EXPIRED | LEFT
})
```

**이벤트 데이터**

```json
{
  "entryToken": "QT-20260206-ABC123",
  "status": "WAITING",
  "position": 35,
  "estimatedWaitSeconds": 70,
  "totalWaiting": 100,
  "message": "대기 중입니다."
}
```

**상태 변경 시나리오**

1. `WAITING` → 대기 중 (position 감소)
2. `ENTERED` → 입장 완료 (SSE 연결 종료)
3. `EXPIRED` → 만료됨 (시간 초과, SSE 연결 종료)
4. `LEFT` → 사용자가 이탈 (SSE 연결 종료)

---

## 사용 예시

### 타임딜 대기열

```typescript
export function TimeDealQueuePage({ timeDealId }: { timeDealId: number }) {
  const { status, isConnected, enterQueue, leaveQueue } = useQueue({
    eventType: 'timedeal',
    eventId: timeDealId,
    autoEnter: false
  })

  const handleEnter = async () => {
    try {
      await enterQueue()
    } catch (error) {
      alert('대기열 진입 실패')
    }
  }

  if (!status) {
    return <button onClick={handleEnter}>대기열 진입</button>
  }

  if (status.status === 'ENTERED') {
    return <Navigate to={`/time-deals/${timeDealId}/purchase`} />
  }

  return (
    <div>
      <h2>대기 중</h2>
      <p>대기 순번: {status.position}번</p>
      <p>예상 대기 시간: {formatWaitTime(status.estimatedWaitSeconds)}</p>
      {isConnected && <span>🔴 실시간 업데이트 중</span>}
      <button onClick={leaveQueue}>나가기</button>
    </div>
  )
}
```

---

## 타입 정의

```typescript
export type QueueStatus = 'WAITING' | 'ENTERED' | 'EXPIRED' | 'LEFT'

export interface QueueStatusResponse {
  entryToken: string
  status: QueueStatus
  position: number
  estimatedWaitSeconds: number
  totalWaiting: number
  message: string
}

export interface QueueActivateRequest {
  maxCapacity: number
  entryBatchSize: number
  entryIntervalSeconds: number
}
```

---

## 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `QUEUE_NOT_ACTIVE` | 400 | 대기열이 활성화되지 않음 |
| `QUEUE_FULL` | 400 | 대기열 인원 초과 |
| `QUEUE_ENTRY_NOT_FOUND` | 404 | 대기열 진입 기록 없음 |
| `QUEUE_ALREADY_ENTERED` | 400 | 이미 진입한 대기열 |

---

## 관련 문서

- [Client API](./client-api.md)
- [TimeDeal API](./timedeal-api.md)
- [공통 타입 정의](./types.md)

---

**최종 업데이트**: 2026-02-06
