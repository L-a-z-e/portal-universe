# Shopping Frontend API 문서

> shopping-frontend의 API 클라이언트 명세 및 사용법 (React 18 기반)

---

## 개요

shopping-frontend는 React 18 기반의 쇼핑몰 프론트엔드로, API Gateway를 통해 shopping-service와 통신합니다.

| 항목 | 내용 |
|------|------|
| **프레임워크** | React 18 |
| **상태 관리** | Zustand (장바구니), useState + useEffect (기타) |
| **API 클라이언트** | Axios |
| **Module Federation** | Embedded (portal/api 공유) + Standalone 모드 지원 |
| **Base URL** | `/api/v1/shopping` |

---

## 문서 목록

| ID | 제목 | 상태 | 최종 업데이트 |
|----|------|------|---------------|
| `client-api` | [API Client 설정](./client-api.md) | ✅ Current | 2026-02-06 |
| `types` | [공통 타입 정의](./types.md) | ✅ Current | 2026-02-06 |
| `product-api` | [상품 API](./product-api.md) | ✅ Current | 2026-02-06 |
| `inventory-api` | [재고 API (SSE)](./inventory-api.md) | ✅ Current | 2026-02-06 |
| `cart-api` | [장바구니 API (Zustand)](./cart-api.md) | ✅ Current | 2026-02-06 |
| `order-api` | [주문 + 결제 + 배송 API](./order-api.md) | ✅ Current | 2026-02-06 |
| `coupon-api` | [쿠폰 API](./coupon-api.md) | ✅ Current | 2026-02-06 |
| `timedeal-api` | [타임딜 API](./timedeal-api.md) | ✅ Current | 2026-02-06 |
| `queue-api` | [대기열 API (SSE)](./queue-api.md) | ✅ Current | 2026-02-06 |
| `search-api` | [검색 API](./search-api.md) | ✅ Current | 2026-02-06 |

---

## 빠른 시작

### 1. API Client 설정

```typescript
// src/api/client.ts
import { getPortalApiClient } from '@portal/react-bridge'

export const getApiClient = () => {
  // Embedded: portal/api 공유
  // Standalone: local fallback
  return getPortalApiClient() ?? getLocalClient()
}
```

자세한 내용: [API Client 설정](./client-api.md)

### 2. API 호출 예시

```typescript
import { productApi } from '@/api/endpoints'

const response = await productApi.getProducts(0, 20, 'books')
const products = response.data.content
```

### 3. React Hooks 사용

```typescript
import { useAdminProducts } from '@/hooks/useAdminProducts'

const { data, isLoading, error } = useAdminProducts(filters)
```

### 4. Zustand Store (장바구니)

```typescript
import { useCartStore } from '@/stores/cartStore'

const { cart, addItem, removeItem } = useCartStore()
```

---

## API 그룹별 기능

### 상품 관리

| 기능 | 엔드포인트 | 문서 |
|------|-----------|------|
| 상품 목록 조회 | `GET /products` | [Product API](./product-api.md) |
| 상품 검색 | `GET /search/products` | [Search API](./search-api.md) |
| 재고 조회 (SSE) | `GET /inventory/stream` | [Inventory API](./inventory-api.md) |
| 관리자 상품 관리 | `POST /admin/products` | [Product API](./product-api.md) |

### 주문 흐름

| 단계 | 엔드포인트 | 문서 |
|------|-----------|------|
| 1. 장바구니 담기 | `POST /cart/items` | [Cart API](./cart-api.md) |
| 2. 주문 생성 | `POST /orders` | [Order API](./order-api.md) |
| 3. 결제 처리 | `POST /payments` | [Order API](./order-api.md) |
| 4. 배송 추적 | `GET /deliveries/{trackingNumber}` | [Order API](./order-api.md) |

### 프로모션

| 기능 | 엔드포인트 | 문서 |
|------|-----------|------|
| 쿠폰 발급 | `POST /coupons/{id}/issue` | [Coupon API](./coupon-api.md) |
| 타임딜 조회 | `GET /time-deals` | [TimeDeal API](./timedeal-api.md) |
| 대기열 진입 (SSE) | `POST /queue/{type}/{id}/enter` | [Queue API](./queue-api.md) |

---

## 주요 특징

### Module Federation 지원

- **Embedded 모드**: portal-shell에서 실행 시 portal/api 공유
- **Standalone 모드**: 독립 실행 시 local fallback client 사용

### SSE (Server-Sent Events)

- 실시간 재고 업데이트 ([Inventory API](./inventory-api.md))
- 대기열 상태 업데이트 ([Queue API](./queue-api.md))

### Zustand Store

- 장바구니 전역 상태 관리 ([Cart API](./cart-api.md))

### React Hooks 패턴

- useState + useEffect (React Query 미사용)
- Mutation Hook: `mutateAsync`, `isPending`, `error`
- Query Hook: `data`, `isLoading`, `error`, `refetch`

---

## 코드 구조

```
src/
├── api/
│   ├── client.ts          # API 클라이언트 설정
│   └── endpoints.ts       # 엔드포인트 함수 정의
├── hooks/                 # Custom React Hooks
│   ├── useAdminProducts.ts
│   ├── useCoupons.ts
│   ├── useTimeDeals.ts
│   ├── useQueue.ts
│   ├── useSearch.ts
│   └── useInventoryStream.ts
├── stores/
│   └── cartStore.ts       # Zustand Store (장바구니)
└── types/
    ├── index.ts           # 메인 타입 정의
    └── admin.ts           # 관리자 타입
```

---

## 타입 정의

모든 타입은 TypeScript로 정의되어 있습니다.

- 메인 타입: [types.md](./types.md)
- API 응답: `ApiResponse<T>`, `PagedResponse<T>`
- 도메인 타입: `Product`, `Cart`, `Order`, `Coupon`, `TimeDeal` 등

---

## 인증 및 권한

### JWT 토큰

모든 API 요청은 JWT Bearer 토큰을 포함합니다:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 권한 레벨

| 레벨 | 접근 가능 API |
|------|---------------|
| **USER** | 조회, 장바구니, 주문, 쿠폰 발급, 타임딜 구매 |
| **ADMIN** | 상품/재고 관리, 배송 상태 변경, 쿠폰/타임딜 생성, 대기열 관리 |

---

## 에러 처리

공통 에러 응답 형식:

```typescript
interface ApiErrorResponse {
  success: false
  code: string
  message: string
  data: null | { errors?: FieldError[] }
  timestamp: string
}
```

도메인별 에러 코드는 각 API 문서를 참조하세요.

---

## 관련 문서

### Backend
- [Shopping Service API](../../../services/shopping-service/docs/api/) (백엔드 API)

### Architecture
- [Shopping Service Architecture](../../architecture/shopping-service/)

### Guides
- [Shopping Frontend Development Guide](../../guides/)

---

## Changelog

### 2026-02-06
- 전체 API 문서 재작성 (실제 코드 기반)
- React 18 패턴으로 변경 (기존 Vue 3 예시 제거)
- SSE, Zustand Store, Module Federation 문서화
- 누락된 도메인 추가 (Coupon, TimeDeal, Queue, Search, Inventory Stream)

### 2026-01-18
- 최초 API 명세 작성 (Vue 3 기반, 일부 도메인 누락)

---

**최종 업데이트**: 2026-02-06
