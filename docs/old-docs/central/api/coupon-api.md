# Coupon API Reference

## 목차
1. [개요](#개요)
2. [에러 코드](#에러-코드)
3. [사용자 API](#사용자-api)
4. [관리자 API](#관리자-api)

---

## 개요

**Base URL**: `http://localhost:8080/api/shopping`

선착순 쿠폰 발급 시스템으로, Redis Lua Script를 활용한 원자적 발급을 지원합니다.

### 핵심 기술
- **Redis Lua Script**: 중복 체크 + 재고 차감 + 발급 기록을 원자적으로 처리
- **분산 환경 지원**: 여러 서버 인스턴스에서 동시 요청 시에도 정확한 발급 보장

### 쿠폰 상태
| 상태 | 설명 |
|------|------|
| ACTIVE | 발급 가능 |
| EXHAUSTED | 재고 소진 |
| EXPIRED | 만료됨 |
| INACTIVE | 비활성화 |

### 사용자 쿠폰 상태
| 상태 | 설명 |
|------|------|
| AVAILABLE | 사용 가능 |
| USED | 사용됨 |
| EXPIRED | 만료됨 |

---

## 에러 코드

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| S601 | 404 | Coupon not found | 쿠폰을 찾을 수 없음 |
| S602 | 409 | Coupon is exhausted | 쿠폰 재고 소진 |
| S603 | 400 | Coupon has expired | 쿠폰 만료됨 |
| S604 | 409 | Coupon already issued to this user | 이미 발급받은 쿠폰 |
| S605 | 400 | Coupon issuance has not started yet | 발급 시작 전 |
| S606 | 400 | Coupon is not active | 비활성화된 쿠폰 |
| S607 | 409 | Coupon code already exists | 중복된 쿠폰 코드 |
| S608 | 404 | User coupon not found | 사용자 쿠폰 없음 |
| S609 | 400 | User coupon has already been used | 이미 사용된 쿠폰 |
| S610 | 400 | User coupon has expired | 사용자 쿠폰 만료 |

---

## 사용자 API

### 1. 발급 가능한 쿠폰 목록 조회

현재 발급 가능한 모든 쿠폰을 조회합니다.

**Endpoint**: `GET /api/v1/shopping/coupons`

**권한**: 없음 (Public)

**Response (200 OK)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "WELCOME2026",
      "name": "신규 가입 환영 쿠폰",
      "description": "첫 구매 시 사용 가능",
      "discountType": "FIXED",
      "discountValue": 5000.00,
      "minimumOrderAmount": 30000.00,
      "maximumDiscountAmount": null,
      "totalQuantity": 1000,
      "issuedQuantity": 523,
      "remainingQuantity": 477,
      "status": "ACTIVE",
      "startsAt": "2026-01-01T00:00:00",
      "expiresAt": "2026-12-31T23:59:59",
      "createdAt": "2026-01-01T00:00:00"
    }
  ],
  "error": null
}
```

---

### 2. 쿠폰 상세 조회

**Endpoint**: `GET /api/v1/shopping/coupons/{couponId}`

**권한**: 없음 (Public)

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| couponId | Long | O | 쿠폰 ID |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "WELCOME2026",
    "name": "신규 가입 환영 쿠폰",
    "discountType": "FIXED",
    "discountValue": 5000.00,
    "totalQuantity": 1000,
    "issuedQuantity": 523,
    "remainingQuantity": 477,
    "status": "ACTIVE",
    "startsAt": "2026-01-01T00:00:00",
    "expiresAt": "2026-12-31T23:59:59"
  },
  "error": null
}
```

---

### 3. 선착순 쿠폰 발급

쿠폰을 선착순으로 발급받습니다. Redis Lua Script로 원자적 처리됩니다.

**Endpoint**: `POST /api/v1/shopping/coupons/{couponId}/issue`

**권한**: USER (JWT 필수)

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| couponId | Long | O | 발급받을 쿠폰 ID |

**Request Headers**:
```http
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "couponId": 1,
    "couponCode": "WELCOME2026",
    "couponName": "신규 가입 환영 쿠폰",
    "discountType": "FIXED",
    "discountValue": 5000.00,
    "minimumOrderAmount": 30000.00,
    "maximumDiscountAmount": null,
    "status": "AVAILABLE",
    "issuedAt": "2026-01-18T10:30:00",
    "expiresAt": "2026-12-31T23:59:59",
    "usedAt": null,
    "usedOrderId": null
  },
  "error": null
}
```

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 400 | S603 | 쿠폰이 만료됨 |
| 400 | S605 | 발급 시작 전 |
| 400 | S606 | 비활성화된 쿠폰 |
| 404 | S601 | 쿠폰을 찾을 수 없음 |
| 409 | S602 | 재고 소진 |
| 409 | S604 | 이미 발급받음 |

**Example - cURL**:
```bash
curl -X POST http://localhost:8080/api/v1/shopping/coupons/1/issue \
  -H "Authorization: Bearer eyJhbGc..."
```

---

### 4. 내 쿠폰 목록 조회

사용자가 보유한 모든 쿠폰을 조회합니다.

**Endpoint**: `GET /api/v1/shopping/coupons/my`

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
      "couponId": 1,
      "couponCode": "WELCOME2026",
      "couponName": "신규 가입 환영 쿠폰",
      "discountType": "FIXED",
      "discountValue": 5000.00,
      "status": "AVAILABLE",
      "issuedAt": "2026-01-18T10:30:00",
      "expiresAt": "2026-12-31T23:59:59",
      "usedAt": null,
      "usedOrderId": null
    }
  ],
  "error": null
}
```

---

### 5. 내 사용 가능한 쿠폰 목록 조회

현재 사용 가능한 쿠폰만 조회합니다.

**Endpoint**: `GET /api/v1/shopping/coupons/my/available`

**권한**: USER (JWT 필수)

**Request Headers**:
```http
Authorization: Bearer <JWT_TOKEN>
```

---

## 관리자 API

### 1. 쿠폰 전체 목록 조회

모든 쿠폰을 페이징으로 조회합니다.

**Endpoint**: `GET /api/v1/shopping/admin/coupons`

**권한**: ADMIN (ROLE_SHOPPING_ADMIN 또는 ROLE_SUPER_ADMIN)

**Query Parameters**:

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| page | Integer | 0 | 페이지 번호 |
| size | Integer | 10 | 페이지 크기 |
| sort | String | createdAt | 정렬 기준 |

**Response (200 OK)**:

Page<CouponResponse> 형태의 Spring Data 페이지 응답

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "code": "WELCOME2026",
        "name": "신규 가입 환영 쿠폰",
        "status": "ACTIVE",
        "totalQuantity": 1000,
        "issuedQuantity": 523,
        "remainingQuantity": 477,
        "createdAt": "2026-01-01T00:00:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 1,
    "totalPages": 1
  },
  "error": null
}
```

---

### 2. 쿠폰 생성

새로운 쿠폰을 생성합니다.

**Endpoint**: `POST /api/v1/shopping/admin/coupons`

**권한**: ADMIN

**Request Headers**:
```http
Authorization: Bearer <ADMIN_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "code": "SUMMER2026",
  "name": "여름 특가 쿠폰",
  "description": "여름 시즌 한정 할인",
  "discountType": "PERCENTAGE",
  "discountValue": 15.00,
  "minimumOrderAmount": 50000.00,
  "maximumDiscountAmount": 10000.00,
  "totalQuantity": 500,
  "startsAt": "2026-06-01T00:00:00",
  "expiresAt": "2026-08-31T23:59:59"
}
```

**Request Schema**:

| 필드 | 타입 | 필수 | 제약사항 | 설명 |
|------|------|------|----------|------|
| code | String | O | 최대 50자, 유니크 | 쿠폰 코드 |
| name | String | O | 최대 100자 | 쿠폰명 |
| description | String | X | - | 설명 |
| discountType | Enum | O | FIXED, PERCENTAGE | 할인 유형 |
| discountValue | BigDecimal | O | > 0 | 할인 값 |
| minimumOrderAmount | BigDecimal | X | >= 0 | 최소 주문 금액 |
| maximumDiscountAmount | BigDecimal | X | >= 0 | 최대 할인 금액 |
| totalQuantity | Integer | O | >= 1 | 총 발급 수량 |
| startsAt | LocalDateTime | O | - | 발급 시작 일시 |
| expiresAt | LocalDateTime | O | 미래 시점 | 만료 일시 |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 2,
    "code": "SUMMER2026",
    "name": "여름 특가 쿠폰",
    "discountType": "PERCENTAGE",
    "discountValue": 15.00,
    "totalQuantity": 500,
    "issuedQuantity": 0,
    "remainingQuantity": 500,
    "status": "ACTIVE",
    "startsAt": "2026-06-01T00:00:00",
    "expiresAt": "2026-08-31T23:59:59",
    "createdAt": "2026-01-18T11:00:00"
  },
  "error": null
}
```

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 409 | S607 | 중복된 쿠폰 코드 |

---

### 3. 쿠폰 비활성화

쿠폰을 비활성화하고 Redis 캐시를 삭제합니다.

**Endpoint**: `DELETE /api/v1/shopping/admin/coupons/{couponId}`

**권한**: ADMIN

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| couponId | Long | O | 비활성화할 쿠폰 ID |

**Response (200 OK)**:
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.1.0 | 2026-01-28 | Documenter Agent | 관리자 쿠폰 전체 목록 조회 API 추가 |
| 1.0.0 | 2026-01-18 | Laze | 초기 명세 작성 |
