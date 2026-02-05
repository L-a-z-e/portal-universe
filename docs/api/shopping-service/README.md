# Shopping Service API Documentation

> Shopping Service의 모든 API 엔드포인트 명세서입니다.

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/shopping` |
| **인증** | Bearer Token (JWT) |
| **버전** | v1 |
| **응답 형식** | JSON |

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

| 도메인 | 문서 | 설명 |
|--------|------|------|
| **Product** | [product-api.md](./product-api.md) | 상품 CRUD, 리뷰 조회 |
| **Admin Product** | [admin-product-api.md](./admin-product-api.md) | 관리자용 상품 관리 (등록, 수정, 삭제, 재고 관리) |
| **Admin Coupon** | [admin-coupon-api.md](./admin-coupon-api.md) | 관리자용 쿠폰 관리 (생성, 조회, 비활성화) |
| **Admin TimeDeal** | [admin-timedeal-api.md](./admin-timedeal-api.md) | 관리자용 타임딜 관리 (생성, 조회, 취소) |
| **Cart** | [cart-api.md](./cart-api.md) | 장바구니 조회, 아이템 추가/수정/삭제, 체크아웃 |
| **Order** | [order-api.md](./order-api.md) | 주문 생성, 조회, 취소 |
| **Payment** | [payment-api.md](./payment-api.md) | 결제 처리, 조회, 취소, 환불 |
| **Delivery** | [delivery-api.md](./delivery-api.md) | 배송 조회, 상태 변경 |
| **Queue** | [queue-api.md](./queue-api.md) | 대기열 진입, 상태 조회, 실시간 구독(SSE), 이탈 |
| **Admin Queue** | [admin-queue-api.md](./admin-queue-api.md) | 관리자용 대기열 관리 (활성화, 비활성화, 수동 처리) |

---

## 🔗 관련 문서

- [Shopping Service Architecture](../../architecture/shopping-service/system-overview.md)
- [Auth Service API](../auth-service/README.md)

---

**최종 업데이트**: 2026-01-19
