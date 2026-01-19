# TimeDeal API Reference

## 목차
1. [개요](#개요)
2. [에러 코드](#에러-코드)
3. [사용자 API](#사용자-api)
4. [관리자 API](#관리자-api)

---

## 개요

**Base URL**: `http://localhost:8080/api/shopping`

한정 시간 특가 판매 시스템으로, Redis Lua Script를 활용한 원자적 재고 관리와 1인당 구매 제한을 지원합니다.

### 핵심 기술
- **Redis Lua Script**: 재고 차감 + 1인당 구매 제한을 원자적으로 처리
- **분산 락 스케줄러**: 1분 주기로 타임딜 상태 자동 업데이트 (SCHEDULED → ACTIVE → ENDED)
- **@DistributedLock**: Redisson 기반 분산 락으로 중복 실행 방지

### 타임딜 상태
| 상태 | 설명 |
|------|------|
| SCHEDULED | 예정됨 (시작 전) |
| ACTIVE | 진행중 |
| ENDED | 종료됨 |
| CANCELLED | 취소됨 |

### 상태 전이
```
SCHEDULED ──(startsAt 도래)──> ACTIVE ──(endsAt 도래)──> ENDED
     │                           │
     └───(관리자 취소)───> CANCELLED <───(관리자 취소)───┘
```

---

## 에러 코드

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| S701 | 404 | Time deal not found | 타임딜을 찾을 수 없음 |
| S702 | 400 | Time deal is not active | 진행중이 아닌 타임딜 |
| S703 | 400 | Time deal has expired | 타임딜 종료됨 |
| S704 | 409 | Time deal product is sold out | 품절 |
| S705 | 400 | Purchase limit exceeded | 1인당 구매 제한 초과 |
| S706 | 404 | Time deal product not found | 타임딜 상품 없음 |
| S707 | 409 | Time deal already exists | 중복된 타임딜 |
| S708 | 400 | Invalid time deal period | 잘못된 기간 설정 |

---

## 사용자 API

### 1. 진행중인 타임딜 목록 조회

현재 진행중인 모든 타임딜을 조회합니다.

**Endpoint**: `GET /api/shopping/time-deals`

**권한**: 없음 (Public)

**Response (200 OK)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "신년 특가 타임딜",
      "description": "2026년 새해 맞이 특별 할인",
      "status": "ACTIVE",
      "startsAt": "2026-01-18T10:00:00",
      "endsAt": "2026-01-18T12:00:00",
      "products": [
        {
          "id": 1,
          "productId": 100,
          "productName": "MacBook Pro 16",
          "originalPrice": 3490000.00,
          "dealPrice": 2990000.00,
          "discountRate": 14.33,
          "dealQuantity": 50,
          "soldQuantity": 23,
          "remainingQuantity": 27,
          "maxPerUser": 1,
          "available": true
        }
      ],
      "createdAt": "2026-01-17T15:00:00"
    }
  ],
  "error": null
}
```

---

### 2. 타임딜 상세 조회

**Endpoint**: `GET /api/shopping/time-deals/{timeDealId}`

**권한**: 없음 (Public)

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| timeDealId | Long | O | 타임딜 ID |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "신년 특가 타임딜",
    "description": "2026년 새해 맞이 특별 할인",
    "status": "ACTIVE",
    "startsAt": "2026-01-18T10:00:00",
    "endsAt": "2026-01-18T12:00:00",
    "products": [
      {
        "id": 1,
        "productId": 100,
        "productName": "MacBook Pro 16",
        "originalPrice": 3490000.00,
        "dealPrice": 2990000.00,
        "discountRate": 14.33,
        "dealQuantity": 50,
        "soldQuantity": 23,
        "remainingQuantity": 27,
        "maxPerUser": 1,
        "available": true
      },
      {
        "id": 2,
        "productId": 101,
        "productName": "iPhone 15 Pro",
        "originalPrice": 1550000.00,
        "dealPrice": 1290000.00,
        "discountRate": 16.77,
        "dealQuantity": 100,
        "soldQuantity": 100,
        "remainingQuantity": 0,
        "maxPerUser": 2,
        "available": false
      }
    ],
    "createdAt": "2026-01-17T15:00:00"
  },
  "error": null
}
```

---

### 3. 타임딜 상품 구매

타임딜 상품을 구매합니다. Redis Lua Script로 원자적 처리됩니다.

**Endpoint**: `POST /api/shopping/time-deals/purchase`

**권한**: USER (JWT 필수)

**Request Headers**:
```http
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "timeDealProductId": 1,
  "quantity": 1
}
```

**Request Schema**:

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| timeDealProductId | Long | O | - | 타임딜 상품 ID |
| quantity | Integer | O | >= 1 | 구매 수량 |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "timeDealProductId": 1,
    "productName": "MacBook Pro 16",
    "quantity": 1,
    "purchasePrice": 2990000.00,
    "totalPrice": 2990000.00,
    "purchasedAt": "2026-01-18T10:35:00"
  },
  "error": null
}
```

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 400 | S702 | 진행중이 아닌 타임딜 |
| 400 | S703 | 타임딜 종료됨 |
| 400 | S705 | 1인당 구매 제한 초과 |
| 404 | S706 | 타임딜 상품 없음 |
| 409 | S704 | 품절 |

**Example - cURL**:
```bash
curl -X POST http://localhost:8080/api/shopping/time-deals/purchase \
  -H "Authorization: Bearer eyJhbGc..." \
  -H "Content-Type: application/json" \
  -d '{
    "timeDealProductId": 1,
    "quantity": 1
  }'
```

**동시성 처리 흐름**:
```
1. Redis Lua Script 실행
   ├─ 현재 재고 확인
   ├─ 사용자별 구매 수량 확인
   ├─ 1인당 제한 검증
   ├─ 재고 원자적 차감 (DECRBY)
   └─ 사용자 구매 수량 증가 (INCRBY)

2. 결과에 따른 처리
   ├─ > 0: 구매 성공 → DB 저장
   ├─ = 0: 재고 소진 → S704 에러
   └─ = -1: 구매 제한 초과 → S705 에러
```

---

### 4. 내 타임딜 구매 내역 조회

사용자의 타임딜 구매 내역을 조회합니다.

**Endpoint**: `GET /api/shopping/time-deals/my/purchases`

**권한**: USER (JWT 필수)

**Request Headers**:
```http
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "timeDealProductId": 1,
      "productName": "MacBook Pro 16",
      "quantity": 1,
      "purchasePrice": 2990000.00,
      "totalPrice": 2990000.00,
      "purchasedAt": "2026-01-18T10:35:00"
    }
  ],
  "error": null
}
```

---

## 관리자 API

### 1. 타임딜 생성

새로운 타임딜을 생성합니다.

**Endpoint**: `POST /api/shopping/admin/time-deals`

**권한**: ADMIN

**Request Headers**:
```http
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "name": "주말 특가 타임딜",
  "description": "주말 한정 특별 할인",
  "startsAt": "2026-01-25T10:00:00",
  "endsAt": "2026-01-25T18:00:00",
  "products": [
    {
      "productId": 100,
      "dealPrice": 2990000.00,
      "dealQuantity": 50,
      "maxPerUser": 1
    },
    {
      "productId": 101,
      "dealPrice": 1290000.00,
      "dealQuantity": 100,
      "maxPerUser": 2
    }
  ]
}
```

**Request Schema**:

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| name | String | O | 최대 100자 | 타임딜명 |
| description | String | X | - | 설명 |
| startsAt | LocalDateTime | O | - | 시작 일시 |
| endsAt | LocalDateTime | O | 미래 시점 | 종료 일시 |
| products | Array | O | 1개 이상 | 타임딜 상품 목록 |

**products 배열 스키마**:

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| productId | Long | O | - | 상품 ID |
| dealPrice | BigDecimal | O | > 0 | 할인 가격 |
| dealQuantity | Integer | O | >= 1 | 판매 수량 |
| maxPerUser | Integer | O | >= 1 | 1인당 최대 구매 수량 |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 2,
    "name": "주말 특가 타임딜",
    "description": "주말 한정 특별 할인",
    "status": "SCHEDULED",
    "startsAt": "2026-01-25T10:00:00",
    "endsAt": "2026-01-25T18:00:00",
    "products": [
      {
        "id": 3,
        "productId": 100,
        "productName": "MacBook Pro 16",
        "originalPrice": 3490000.00,
        "dealPrice": 2990000.00,
        "discountRate": 14.33,
        "dealQuantity": 50,
        "soldQuantity": 0,
        "remainingQuantity": 50,
        "maxPerUser": 1,
        "available": true
      }
    ],
    "createdAt": "2026-01-18T14:00:00"
  },
  "error": null
}
```

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 400 | S708 | 시작 시간이 종료 시간보다 늦음 |
| 404 | S001 | 상품을 찾을 수 없음 |

---

### 2. 타임딜 취소

타임딜을 취소하고 Redis 캐시를 삭제합니다.

**Endpoint**: `DELETE /api/shopping/admin/time-deals/{timeDealId}`

**권한**: ADMIN

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| timeDealId | Long | O | 취소할 타임딜 ID |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

## 스케줄러

타임딜 상태는 `TimeDealScheduler`에 의해 1분마다 자동으로 업데이트됩니다.

### 동작 방식
```java
@Scheduled(fixedRate = 60000) // 1분마다
@DistributedLock(key = "'scheduler:timedeal:status'", waitTime = 0, leaseTime = 55)
public void updateTimeDealStatus() {
    // SCHEDULED → ACTIVE: 시작 시간 도래 시
    activateScheduledDeals(now);

    // ACTIVE → ENDED: 종료 시간 도래 시
    endActiveDeals(now);
}
```

### 분산 환경 고려사항
- **분산 락**: `@DistributedLock`으로 여러 인스턴스에서 중복 실행 방지
- **waitTime=0**: 락 획득 실패 시 즉시 스킵 (다음 주기에 재시도)
- **leaseTime=55초**: 작업 완료 전 락 자동 해제 방지

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0.0 | 2026-01-18 | Claude | 초기 명세 작성 |
