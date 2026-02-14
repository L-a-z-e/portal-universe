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
| **DB** | shopping_seller_db (MySQL) |

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
| POST | `/sellers/register` | 판매자 등록 | USER |
| GET | `/sellers/me` | 내 정보 조회 | SELLER |
| PUT | `/sellers/me` | 정보 수정 | SELLER |

**Request DTO**:
- `SellerRegisterRequest`: businessName, businessNumber, representativeName, phone, email, bankName, bankAccount
- `SellerUpdateRequest`: businessName, phone, email, bankName, bankAccount

**Response DTO**:
- `SellerResponse`: id, userId, businessName, businessNumber, representativeName, phone, email, bankName, bankAccount, commissionRate, status, createdAt, updatedAt

**판매자 상태**:
- `PENDING`: 승인 대기 (기본값)
- `ACTIVE`: 활성화
- `SUSPENDED`: 정지
- `WITHDRAWN`: 탈퇴

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

#### 3. InventoryController (`/inventory`)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| GET | `/inventory/{productId}` | 재고 조회 | SELLER |
| PUT | `/inventory/{productId}/add` | 재고 추가 | SELLER |
| POST | `/inventory/{productId}` | 재고 초기화 | SELLER |

**Request DTO**:
- `StockAddRequest`: quantity, reason
- `initialQuantity`: Query Parameter (기본값: 0)

**Response DTO**:
- `InventoryResponse`: id, productId, availableQuantity, reservedQuantity, totalQuantity, version, createdAt, updatedAt

**재고 타입**:
- `availableQuantity`: 가용 재고 (판매 가능)
- `reservedQuantity`: 예약 재고 (주문 진행 중)
- `totalQuantity`: 총 재고 (available + reserved)

---

### 내부 API (Internal)

#### 4. InternalProductController (`/internal/products`)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| GET | `/internal/products/{productId}` | 상품 조회 | 내부 호출 |
| GET | `/internal/products` | 상품 목록 | 내부 호출 |

**용도**: shopping-service, chatbot-service 등 다른 서비스에서 Feign Client로 호출

**인증**: Internal API는 Service Mesh 또는 API Gateway에서 인증 처리

#### 5. InternalInventoryController (`/internal/inventory`)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|--------|-----------|------|------|
| POST | `/internal/inventory/reserve` | 재고 예약 (Saga) | 내부 호출 |
| POST | `/internal/inventory/deduct` | 재고 차감 (Saga) | 내부 호출 |
| POST | `/internal/inventory/release` | 재고 해제 (Saga) | 내부 호출 |

**Request DTO**:
- `StockReserveRequest`: productId, quantity, referenceType, referenceId

**용도**: shopping-service의 OrderSagaOrchestrator가 분산 트랜잭션 수행 시 호출

**Saga 단계**:
1. `reserve`: 주문 생성 시 재고 예약 (available → reserved)
2. `deduct`: 결제 완료 시 재고 차감 (reserved → 삭제)
3. `release`: 주문 취소 시 재고 해제 (reserved → available)

**동시성 제어**: `@Version` 낙관적 락 + Pessimistic Write Lock

---

## 🔗 에러 코드

| 코드 | 설명 | HTTP 상태 |
|------|------|----------|
| **SL0XX** | **Seller** | |
| SL001 | SELLER_NOT_FOUND | 404 |
| SL002 | SELLER_ALREADY_EXISTS | 409 |
| SL003 | SELLER_SUSPENDED | 403 |
| SL004 | SELLER_PENDING | 403 |
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
| **SL4XX** | **TimeDeal** | |
| SL401 | TIMEDEAL_NOT_FOUND | 404 |
| SL402 | TIMEDEAL_NOT_ACTIVE | 400 |
| SL403 | TIMEDEAL_INVALID_PERIOD | 400 |
| SL404 | TIMEDEAL_PRODUCT_NOT_FOUND | 404 |
| **SL5XX** | **Queue** | |
| SL501 | QUEUE_NOT_FOUND | 404 |

**접두사**: `SL` (Shopping seLler)

---

## 🔗 관련 문서

- [Shopping Seller Service Architecture](../../architecture/shopping-seller-service/system-overview.md)
- [Shopping Service API](../shopping-service/README.md) - Buyer API
- [Shopping Settlement Service API](../shopping-settlement-service/README.md) - Settlement API
- [Auth Service API](../auth-service/README.md)

---

**최종 업데이트**: 2026-02-14
