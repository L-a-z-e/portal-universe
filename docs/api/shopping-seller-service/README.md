# Shopping Seller Service API Documentation

> Shopping Seller Service의 모든 API 엔드포인트 명세서입니다.

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/v1/seller` |
| **인증** | Bearer Token (JWT) |
| **포트** | 8088 |
| **응답 형식** | JSON |
| **DB** | shopping_seller_db (PostgreSQL) |

---

## 🔐 인증

모든 판매자 API는 JWT Bearer Token 인증이 필요합니다.

```http
Authorization: Bearer {access_token}
```

### 권한

- **ROLE_SELLER**: 판매자 기본 권한
- **ROLE_SHOPPING_ADMIN**: 쇼핑 관리자 권한
- **ROLE_SUPER_ADMIN**: 슈퍼 관리자 권한

### 토큰 획득

Auth Service의 OAuth2 인증을 통해 토큰을 발급받아야 합니다.

---

## 📊 공통 응답 형식

### 성공 응답

```json
{
  "success": true,
  "data": { ... },
  "code": null,
  "message": null,
  "timestamp": "2026-02-14T10:30:00Z"
}
```

### 에러 응답

```json
{
  "success": false,
  "data": null,
  "code": "SL001",
  "message": "에러 메시지",
  "timestamp": "2026-02-14T10:30:00Z"
}
```

---

## 📚 API 목록

### 판매자 API

#### 1. SellerController (`/sellers`)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| POST | `/sellers/apply` | 판매자 신청 | 인증된 사용자 (ROLE 불필요) |
| GET | `/sellers/my-application` | 내 신청 상태 조회 | 인증된 사용자 (ROLE 불필요) |
| POST | `/sellers/register` | 판매자 등록 (레거시) | USER |
| GET | `/sellers/me` | 내 정보 조회 | SELLER |
| PUT | `/sellers/me` | 정보 수정 | SELLER |

**Request DTO**:
- `SellerApplyRequest`: businessName, businessNumber, representativeName, phone, email, bankName, bankAccount, reason
- `SellerRegisterRequest`: businessName, businessNumber, representativeName, phone, email, bankName, bankAccount (레거시)
- `SellerUpdateRequest`: businessName, phone, email, bankName, bankAccount

**Response DTO**:
- `SellerResponse`: id, userId, businessName, businessNumber, representativeName, phone, email, bankName, bankAccount, commissionRate, status, reason, reviewedBy, reviewComment, reviewedAt, createdAt

**판매자 상태**:
- `PENDING`: 승인 대기 (기본값)
- `ACTIVE`: 활성화
- `SUSPENDED`: 정지
- `WITHDRAWN`: 탈퇴
- `REJECTED`: 거절

**신청 플로우** (ADR-057):
```
1. 회원가입 (ROLE_USER)
2. POST /sellers/apply → Seller PENDING 생성, sellerId 발급
3. 관리자 승인 → SellerApprovedEvent 발행 (Kafka)
4. auth-service가 이벤트 수신 → ROLE_SHOPPING_SELLER 부여
5. 판매자 대시보드 접근 가능
```

#### 1-1. SellerAdminController (`/admin/sellers`)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| GET | `/admin/sellers?status={status}` | 판매자 목록 (상태별 필터) | SHOPPING_ADMIN, SUPER_ADMIN |
| GET | `/admin/sellers` | 전체 판매자 목록 | SHOPPING_ADMIN, SUPER_ADMIN |
| POST | `/admin/sellers/{sellerId}/review` | 판매자 승인/거절 | SHOPPING_ADMIN, SUPER_ADMIN |

**Request DTO**:
- `SellerReviewRequest`: approved (Boolean, 필수), reviewComment (String, 선택)

**Response DTO**:
- `SellerResponse` (동일)
- 목록: `PageResponse<SellerResponse>`

**승인 시 동작**: `SellerApprovedEvent` Kafka 발행 → auth-service에서 `ROLE_SHOPPING_SELLER` 자동 부여

#### 2. ProductController (`/products`)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| GET | `/products` | 상품 목록 (본인 상품) | SELLER |
| GET | `/products/{productId}` | 상품 상세 | SELLER |
| POST | `/products` | 상품 등록 | SELLER |
| PUT | `/products/{productId}` | 상품 수정 | SELLER |
| DELETE | `/products/{productId}` | 상품 삭제 | SELLER |

**Pagination**: `Pageable` 지원 (page, size, sort)

**Request DTO**:
- `ProductCreateRequest`: name, description, price, stock, imageUrl, category
- `ProductUpdateRequest`: name, description, price, stock, imageUrl, category

**Response DTO**:
- `ProductResponse`: id, sellerId, name, description, price, stock, imageUrl, category, createdAt, updatedAt

**권한 확인**: Controller는 JWT userId에서 sellerId를 조회하여 본인 상품만 수정/삭제 가능

**CQRS 이벤트 발행**: 상품 생성/수정/삭제 시 Kafka 이벤트 발행 → shopping-service(Query Side)에서 소비하여 구매자용 상품 데이터 + ES 인덱스 동기화
- `ProductCreatedEvent` → `seller.product.created`
- `ProductUpdatedEvent` → `seller.product.updated`
- `ProductDeletedEvent` → `seller.product.deleted`

**이벤트 필드**: productId, sellerId, name, description, price, discountPrice, imageUrl, category, featured, timestamp (stock 제외 — Saga Feign으로 처리)

#### 3. InventoryController (`/inventory`)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| GET | `/inventory/{productId}` | 재고 조회 | SELLER |
| PUT | `/inventory/{productId}/add` | 재고 추가 | SELLER |
| GET | `/inventory/{productId}/movements` | 재고 이동 이력 | SELLER |
| POST | `/inventory/{productId}` | 재고 초기화 | SELLER |

**Request DTO**:
- `StockAddRequest`: quantity, reason
- `initialQuantity`: Query Parameter (기본값: 0)

**Response DTO**:
- `InventoryResponse`: id, productId, availableQuantity, reservedQuantity, totalQuantity, version, createdAt, updatedAt
- `StockMovementResponse`: id, productId, movementType, quantity, previousAvailable, afterAvailable, previousReserved, afterReserved, referenceType, referenceId, reason, performedBy, createdAt

**Pagination**: `Pageable` 지원 (movements 엔드포인트)

**재고 타입**:
- `availableQuantity`: 가용 재고 (판매 가능)
- `reservedQuantity`: 예약 재고 (주문 진행 중)
- `totalQuantity`: 총 재고 (available + reserved)

#### 4. CouponController (`/coupons`)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| GET | `/coupons` | 쿠폰 목록 (본인) | SELLER |
| GET | `/coupons/{couponId}` | 쿠폰 상세 | SELLER |
| POST | `/coupons` | 쿠폰 생성 | SELLER |
| DELETE | `/coupons/{couponId}` | 쿠폰 비활성화 | SELLER |

**Pagination**: `Pageable` 지원

**Request DTO**:
- `CouponCreateRequest`: code, name, description, discountType, discountValue, minimumOrderAmount, maximumDiscountAmount, totalQuantity, startsAt, expiresAt

**Response DTO**:
- `CouponResponse`: id, sellerId, code, name, description, discountType, discountValue, minimumOrderAmount, maximumDiscountAmount, totalQuantity, issuedQuantity, status, startsAt, expiresAt, createdAt, updatedAt

**쿠폰 상태**: ACTIVE, INACTIVE, EXHAUSTED, EXPIRED
**할인 타입**: FIXED (정액), PERCENTAGE (정률)
**비즈니스 규칙**: code 중복 불가, DELETE는 soft delete (status=INACTIVE), 본인 쿠폰만 관리 가능

**CQRS 이벤트 발행**: 쿠폰 생성/비활성화/만료 시 Kafka 이벤트 발행 → shopping-service(Query Side)에서 소비하여 구매자용 데이터 동기화
- `CouponCreatedEvent` → `seller.coupon.created`
- `CouponUpdatedEvent` → `seller.coupon.updated` (스케줄러 만료 전환 시)
- `CouponDeletedEvent` → `seller.coupon.deleted`

**자동 만료 스케줄러**: 1분마다 `expiresAt`이 지난 ACTIVE 쿠폰을 EXPIRED로 전환하고 `CouponUpdatedEvent` 발행

#### 5. TimeDealController (`/time-deals`)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| GET | `/time-deals` | 타임딜 목록 (본인) | SELLER |
| GET | `/time-deals/{timeDealId}` | 타임딜 상세 | SELLER |
| POST | `/time-deals` | 타임딜 생성 | SELLER |
| DELETE | `/time-deals/{timeDealId}` | 타임딜 취소 | SELLER |

**Pagination**: `Pageable` 지원

**Request DTO**:
- `TimeDealCreateRequest`: name, description, startsAt, endsAt, products[{productId, dealPrice, dealQuantity, maxPerUser}]

**Response DTO**:
- `TimeDealResponse`: id, sellerId, name, description, status, startsAt, endsAt, products[{id, productId, dealPrice, dealQuantity, soldQuantity, maxPerUser}], createdAt, updatedAt

**타임딜 상태**: SCHEDULED, ACTIVE, ENDED, CANCELLED
**비즈니스 규칙**: startsAt < endsAt, 상품은 본인 소유여야 함, 취소는 SCHEDULED/ACTIVE 상태에서만 가능

**CQRS 이벤트 발행**: 타임딜 생성/취소/상태전환 시 Kafka 이벤트 발행 → shopping-service(Query Side)에서 소비하여 구매자용 데이터 동기화
- `TimeDealCreatedEvent` → `seller.timedeal.created`
- `TimeDealUpdatedEvent` → `seller.timedeal.updated` (스케줄러 상태 전환 시)
- `TimeDealCancelledEvent` → `seller.timedeal.cancelled`

**자동 상태 전환 스케줄러**: 1분마다 실행
- `SCHEDULED → ACTIVE`: startsAt 도달 시 활성화
- `ACTIVE → ENDED`: endsAt 도달 시 종료
- 전환 시 `TimeDealUpdatedEvent` 발행

#### 6. QueueController (`/queue`)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| POST | `/queue/{eventType}/{eventId}/activate` | 대기열 활성화 | SELLER |
| POST | `/queue/{eventType}/{eventId}/deactivate` | 대기열 비활성화 | SELLER |
| GET | `/queue/{eventType}/{eventId}/status` | 대기열 상태 | SELLER |

**Request DTO**:
- `QueueActivateRequest`: maxCapacity, entryBatchSize, entryIntervalSeconds

**Response DTO**:
- `QueueStatusResponse`: queueId, eventType, eventId, isActive, waitingCount, enteredCount, maxCapacity, entryBatchSize, entryIntervalSeconds

**비즈니스 규칙**: 이미 활성화된 대기열은 재활성화 불가, 비활성 상태에서만 비활성화 가능

#### 7. DashboardController (`/dashboard`)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| GET | `/dashboard/stats` | 대시보드 통계 | SELLER |

**Response DTO**:
- `DashboardStatsResponse`: productCount, couponCount, activeCouponCount, timeDealCount, activeTimeDealCount

---

### 내부 API (Internal)

**인증**: `X-Internal-Token` 헤더 기반 서비스 토큰 인증 (`InternalTokenAuthFilter`). SecurityConfig에서 `/internal/**` 전용 `@Order(2)` 필터 체인으로 분리. (ADR-053)

#### 4. InternalProductController (`/internal/products`)

| 메서드 | 엔드포인트 | 설명 | 인증 |
|--------|-----------|------|------|
| GET | `/internal/products/{productId}` | 상품 조회 | X-Internal-Token |
| GET | `/internal/products` | 상품 목록 | X-Internal-Token |

**용도**: shopping-service, chatbot-service 등 다른 서비스에서 Feign Client로 호출

#### 5. InternalInventoryController (`/internal/inventory`)

| 메서드 | 엔드포인트 | 설명 | 인증 |
|--------|-----------|------|------|
| POST | `/internal/inventory/reserve` | 재고 예약 (Saga Step 1) | X-Internal-Token |
| POST | `/internal/inventory/deduct` | 재고 차감 (Saga Step 3) | X-Internal-Token |
| POST | `/internal/inventory/release` | 재고 해제 (보상: RESERVE 완료 & DEDUCT 미완료) | X-Internal-Token |
| POST | `/internal/inventory/restore` | 재고 복원 (보상: DEDUCT 완료 후 취소) | X-Internal-Token |

**Request DTO**:
- `StockReserveRequest`: orderNumber, quantities (Map\<Long, Integer\> — productId → quantity)

**용도**: shopping-service의 OrderSagaOrchestrator가 분산 트랜잭션 수행 시 호출

**Saga 단계**:
1. `reserve`: 주문 생성 시 재고 예약 (available -= qty, reserved += qty)
2. `deduct`: 결제 완료 시 재고 차감 (reserved -= qty, total -= qty)
3. `release`: 보상 — 예약 해제 (reserved -= qty, available += qty)
4. `restore`: 보상 — 차감 복원 (available += qty, total += qty) **(Phase 2 추가)**

**동시성 제어**: `@Version` 낙관적 락 + Pessimistic Write Lock

**재고 상태 모델**:
```
┌─────────────┐     reserve      ┌─────────────┐     deduct      ┌─────────────┐
│  available   │ ──────────────→ │  reserved    │ ──────────────→ │  (차감됨)    │
│  (판매 가능)  │ ←────────────── │  (예약 중)    │                 │             │
└─────────────┘     release      └─────────────┘                 └──────┬──────┘
       ↑                                                                │
       └──────────────────── restore (Phase 2) ────────────────────────┘
```

---

## 🔗 에러 코드

| 코드 | 설명 | HTTP 상태 |
|------|------|----------|
| **SL0XX** | **Seller** | |
| SL001 | SELLER_NOT_FOUND | 404 |
| SL002 | SELLER_ALREADY_EXISTS | 409 |
| SL003 | SELLER_SUSPENDED | 403 |
| SL004 | SELLER_PENDING | 403 |
| SL005 | SELLER_APPLICATION_NOT_FOUND | 404 |
| SL006 | SELLER_APPLICATION_NOT_PENDING | 400 |
| **SL1XX** | **Product** | |
| SL101 | PRODUCT_NOT_FOUND | 404 |
| SL102 | PRODUCT_NOT_OWNED | 403 |
| SL103 | INVALID_PRODUCT_PRICE | 400 |
| **SL2XX** | **Inventory** | |
| SL201 | INVENTORY_NOT_FOUND | 404 |
| SL202 | INSUFFICIENT_STOCK | 400 |
| SL203 | STOCK_RESERVATION_FAILED | 500 |
| SL204 | STOCK_RELEASE_FAILED | 500 |
| SL205 | STOCK_DEDUCTION_FAILED | 500 |
| SL206 | INVALID_STOCK_QUANTITY | 400 |
| SL207 | INVENTORY_ALREADY_EXISTS | 409 |
| SL208 | CONCURRENT_STOCK_MODIFICATION | 409 |
| **SL3XX** | **Coupon** | |
| SL301 | COUPON_NOT_FOUND | 404 |
| SL302 | COUPON_CODE_ALREADY_EXISTS | 409 |
| SL303 | COUPON_EXHAUSTED | 409 |
| SL304 | COUPON_EXPIRED | 400 |
| SL305 | COUPON_ALREADY_ISSUED | 409 |
| SL306 | COUPON_NOT_STARTED | 400 |
| SL307 | COUPON_INACTIVE | 400 |
| SL308 | COUPON_NOT_OWNED | 403 |
| **SL4XX** | **TimeDeal** | |
| SL401 | TIMEDEAL_NOT_FOUND | 404 |
| SL402 | TIMEDEAL_NOT_ACTIVE | 400 |
| SL403 | TIMEDEAL_INVALID_PERIOD | 400 |
| SL404 | TIMEDEAL_PRODUCT_NOT_FOUND | 404 |
| SL405 | TIMEDEAL_NOT_OWNED | 403 |
| SL406 | TIMEDEAL_CANNOT_CANCEL | 400 |
| **SL5XX** | **Queue** | |
| SL501 | QUEUE_NOT_FOUND | 404 |
| SL502 | QUEUE_ALREADY_ACTIVE | 409 |
| SL503 | QUEUE_NOT_ACTIVE | 400 |

**접두사**: `SL` (Shopping seLler)

---

## 🔗 관련 문서

- [Shopping Seller Service Architecture](../../architecture/shopping-seller-service/system-overview.md)
- [Shopping Service API](../shopping-service/README.md) - Buyer API
- [Shopping Settlement Service API](../shopping-settlement-service/README.md) - Settlement API
- [Auth Service API](../auth-service/README.md)

---

**최종 업데이트**: 2026-03-01
