# Shopping Service API Documentation

> Shopping Service의 모든 API 엔드포인트 명세서입니다.

---

## 📋 개요

**Shopping Service는 2026-02-14 서비스 분해를 거쳐 Buyer 전용 서비스로 재구조화되었습니다.**

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/shopping` |
| **역할** | Buyer 쇼핑 경험 (장바구니, 주문, 결제, 배송 조회, 쿠폰/타임딜 사용, 대기열, 검색) |
| **인증** | Bearer Token (JWT) |
| **버전** | v1 |
| **응답 형식** | JSON |

**분해된 관리자/판매자 기능**:
- Product/Inventory CRUD: [Shopping Seller Service API](../shopping-seller-service/README.md) (:8088)
- Coupon/TimeDeal/Queue Admin: [Shopping Seller Service API](../shopping-seller-service/README.md) (:8088)
- 정산 배치: [Shopping Settlement Service](../shopping-settlement-service/README.md) (:8089)

---

## 🔐 인증

모든 API는 JWT Bearer Token 인증이 필요합니다 (일부 공개 API 제외).

```http
Authorization: Bearer {access_token}
```

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
  "timestamp": "2026-01-18T10:30:00Z"
}
```

### 에러 응답

```json
{
  "success": false,
  "data": null,
  "code": "S001",
  "message": "에러 메시지",
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

## 📚 API 목록

### Buyer API (현재 서비스)

| 도메인 | 문서 | 설명 |
|--------|------|------|
| **Product** | [product-api.md](./product-api.md) | 상품 목록/상세 조회, 리뷰 포함 조회 (읽기 전용) |
| **Cart** | [cart-api.md](./cart-api.md) | 장바구니 조회, 아이템 추가/수정/삭제, 체크아웃 |
| **Order** | [order-api.md](./order-api.md) | 주문 생성, 조회, 취소 |
| **Payment** | [payment-api.md](./payment-api.md) | **[Deprecated]** 결제 API — payment-service로 이전, [payment-service API](../payment-service/payment-api.md) 참조 |
| **Delivery** | [delivery-api.md](./delivery-api.md) | 배송 조회 (읽기 전용) |
| **Coupon** | [coupon-api.md](./coupon-api.md) | 쿠폰 조회, 선착순 발급, 내 쿠폰 관리 (사용자 기능만) |
| **TimeDeal** | [timedeal-api.md](./timedeal-api.md) | 타임딜 조회, 구매, 내 구매 내역 (사용자 기능만) |
| **Queue** | [queue-api.md](./queue-api.md) | 대기열 진입, 상태 조회, 실시간 구독(SSE), 이탈 (사용자 기능만) |
| **Inventory** | [inventory-api.md](./inventory-api.md) | 재고 배치 조회, 단일 조회 (읽기 전용) |
| **Search** | [search-api.md](./search-api.md) | 상품 검색, 자동완성, 인기/최근 검색어 |

### Seller/Admin API (shopping-seller-service로 이전)

아래 관리자 기능은 `shopping-seller-service` (:8088)로 이전되었습니다:
- Product CRUD (상품 등록/수정/삭제)
- Inventory 관리 (입고/조정/이동)
- Coupon 관리 (생성/수정/비활성화)
- TimeDeal 관리 (생성/취소/조회)
- Queue 관리 (활성화/비활성화/수동 처리)
- Order Admin (주문 상태 변경)

자세한 내용: [Shopping Seller Service API](../shopping-seller-service/README.md)

### CQRS 동기화 (Kafka Consumer)

쿠폰/타임딜 데이터는 seller-service(Command Side)에서 생성되고, Kafka 이벤트를 통해 shopping-service(Query Side)로 단방향 동기화됩니다.

| 이벤트 | 토픽 | 동작 |
|--------|------|------|
| `CouponCreatedEvent` | `seller.coupon.created` | Coupon 생성 + Redis 재고 초기화 |
| `CouponUpdatedEvent` | `seller.coupon.updated` | Coupon 갱신 (upsert) |
| `CouponDeletedEvent` | `seller.coupon.deleted` | Coupon 비활성화 + Redis 캐시 삭제 |
| `TimeDealCreatedEvent` | `seller.timedeal.created` | TimeDeal + TimeDealProduct 생성 + Redis 재고 초기화 |
| `TimeDealUpdatedEvent` | `seller.timedeal.updated` | TimeDeal 갱신 (upsert) |
| `TimeDealCancelledEvent` | `seller.timedeal.cancelled` | TimeDeal 취소 + Redis 캐시 삭제 |

**멱등성**: `source_coupon_id`, `source_time_deal_id` UNIQUE constraint로 중복 생성 방지

---

## 🔌 Feign Client

Shopping Service는 Seller Service에 의존하여 상품/재고 정보를 조회합니다:

| Client | 대상 | 메서드 | Endpoint |
|--------|------|--------|----------|
| **SellerProductClient** | shopping-seller-service | 상품 조회 | `GET /api/v1/seller/internal/products/{id}` |
| **SellerInventoryClient** | shopping-seller-service | 재고 예약 | `POST /api/v1/seller/internal/inventory/reserve` |
| **SellerInventoryClient** | shopping-seller-service | 재고 차감 | `POST /api/v1/seller/internal/inventory/deduct` |
| **SellerInventoryClient** | shopping-seller-service | 재고 해제 | `POST /api/v1/seller/internal/inventory/release` |

| **PaymentIntentFeignClient** | payment-service | Intent 생성 | `POST /internal/intents` |
| **PaymentIntentFeignClient** | payment-service | Saga 보상 환불 | `POST /internal/refund/{orderNumber}` |

**Circuit Breaker**: Resilience4j 적용 (fallback: 에러 응답)

---

## 🔗 관련 문서

- [Shopping Service Architecture](../../architecture/shopping-service/system-overview.md)
- [Shopping Seller Service API](../shopping-seller-service/README.md)
- [Shopping Settlement Service](../shopping-settlement-service/README.md)
- [ADR-041: Shopping Service Decomposition](../../adr/ADR-041-shopping-service-decomposition.md)
- [Auth Service API](../auth-service/README.md)

---

## 📝 변경 이력

| Date | Change | Author |
|------|--------|--------|
| 2026-02-28 | Payment API Deprecated, payment-service Feign Client 추가 | Laze |
| 2026-02-14 | 서비스 분해: Buyer 전용 서비스로 전환, Admin API 제거, Feign Client 추가 | Laze |
| 2026-02-06 | 초기 문서 작성 | Laze |

---

**최종 업데이트**: 2026-02-28
