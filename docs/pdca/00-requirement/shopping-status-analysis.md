# Shopping 서비스 전체 현황 분석

**작성일**: 2026-01-31
**작성자**: Laze
**목적**: Shopping 서비스의 백엔드 API, 프론트엔드 UI, E2E 테스트 현황을 종합 분석

---

## 1. 백엔드 API 현황 (65개 엔드포인트)

### 도메인별 요약

| 도메인 | 엔드포인트 수 | 상태 | 비고 |
|--------|------------|------|------|
| Product | 10 | ✅ 완성 | Admin 4 + User 6 |
| Cart | 6 | ✅ 완성 | |
| Order | 7 | ✅ 완성 | Admin 3 + User 4 |
| Payment | 4 | ⚠️ Mock PG | 실제 PG 연동 미완 |
| Delivery | 3 | ⚠️ 부분 | Saga step 4 skip |
| Inventory | 4 | ✅ 완성 | |
| Coupon | 8 | ✅ 완성 | Admin 3 + User 5 |
| TimeDeal | 8 | ✅ 완성 | Admin 4 + User 4 |
| Queue | 7 | ✅ 완성 | Admin 3 + User 4 |
| Search | 7 | ✅ 완성 | |
| Test | 1 | ✅ 헬스체크 | |

### Saga 현황 (OrderSagaOrchestrator)

실행 흐름:
1. `RESERVE_INVENTORY` - 재고 예약 ✅
2. `PROCESS_PAYMENT` - 결제 처리 ✅
3. `DEDUCT_INVENTORY` - 재고 차감 ✅
4. **`CREATE_DELIVERY`** - ⚠️ **skip** (별도 서비스 처리 주석)
5. `CONFIRM_ORDER` - 주문 확정 ✅

**갭**: CREATE_DELIVERY 단계가 건너뛰어지며 배송 생성이 자동으로 이루어지지 않음.

---

## 2. 프론트엔드 현황

### 라우트별 페이지 (총 23+ 페이지)

#### 사용자 영역

| 도메인 | 경로 | 페이지 | 인증 |
|--------|------|--------|------|
| Product | `/`, `/products` | ProductListPage | No |
| Product | `/products/:productId` | ProductDetailPage | No |
| Cart | `/cart` | CartPage | Yes |
| Checkout | `/checkout` | CheckoutPage | Yes |
| Order | `/orders` | OrderListPage | Yes |
| Order | `/orders/:orderNumber` | OrderDetailPage | Yes |
| Coupon | `/coupons` | CouponListPage | Yes |
| TimeDeal | `/time-deals` | TimeDealListPage | No |
| TimeDeal | `/time-deals/:id` | TimeDealDetailPage | No |
| TimeDeal | `/time-deals/purchases` | TimeDealPurchasesPage | Yes |
| Queue | `/queue/:eventType/:eventId` | QueueWaitingPage | No |
| Error | `/403` | ForbiddenPage | No |

#### 관리자 영역 (ROLE_SHOPPING_ADMIN / ROLE_SUPER_ADMIN)

| 경로 | 페이지 |
|------|--------|
| `/admin/products` | AdminProductListPage |
| `/admin/products/new`, `/admin/products/:id` | AdminProductFormPage |
| `/admin/coupons` | AdminCouponListPage |
| `/admin/coupons/new` | AdminCouponFormPage |
| `/admin/time-deals` | AdminTimeDealListPage |
| `/admin/time-deals/new` | AdminTimeDealFormPage |
| `/admin/orders` | AdminOrderListPage |
| `/admin/orders/:orderNumber` | AdminOrderDetailPage |
| `/admin/deliveries` | AdminDeliveryPage |
| `/admin/stock-movements` | AdminStockMovementPage |
| `/admin/queue` | AdminQueuePage |

### 주요 컴포넌트

| 컴포넌트 | 위치 | 용도 |
|----------|------|------|
| ProductCard | components/ | 상품 카드 |
| ProductReviews | components/ | 리뷰 섹션 |
| CartItem | components/ | 장바구니 항목 |
| CouponCard | components/ | 쿠폰 카드 |
| CouponSelector | components/ | 체크아웃 쿠폰 선택 |
| TimeDealCard | components/ | 타임딜 카드 |
| CountdownTimer | components/ | 타임딜 카운트다운 |
| QueueStatus | components/ | 대기열 상태 |
| SearchAutocomplete | components/ | 검색 자동완성 |
| PopularKeywords | components/ | 인기 검색어 |
| RecentKeywords | components/ | 최근 검색어 |
| AdminLayout | components/layout/ | 관리자 레이아웃 |
| ErrorBoundary | components/ | 에러 경계 |
| Pagination | components/ | 페이지네이션 |
| ConfirmModal | components/ | 확인 다이얼로그 |

### 기술 스택

- **Module Federation**: portal-shell(Vue3 Host) ↔ shopping-frontend(React18 Remote)
- **실시간**: SSE (재고), WebSocket (대기열)
- **상태 관리**: Zustand
- **API**: Axios 기반
- **코드 분할**: React.lazy + Suspense
- **RBAC**: RequireAuth, RequireRole 가드

---

## 3. E2E 테스트 현황 (총 64개)

### 기존 테스트

| 파일 | 테스트 수 | 대상 |
|------|----------|------|
| `shopping/smoke.noauth.spec.ts` | 3 | 기본 렌더링 |
| `shopping/product.spec.ts` | 9 | 상품 목록/상세 |
| `shopping/cart.spec.ts` | 9 | 장바구니 CRUD |
| `shopping/checkout.spec.ts` | 11 | 체크아웃 플로우 |
| `shopping/order.spec.ts` | 11 | 주문 목록/상세/취소 |
| `admin/product.spec.ts` | 19 | Admin 상품 관리 |
| **합계** | **64** | |

### 테스트 미커버리지 영역

| 영역 | 현재 테스트 | 갭 |
|------|-----------|-----|
| Coupon | ❌ 없음 | 쿠폰 목록/발급/내 쿠폰/체크아웃 적용 |
| TimeDeal | ❌ 없음 | 타임딜 목록/상세/카운트다운/구매/내역 |
| Queue | ❌ 없음 | 대기열 진입/대기/이탈 |
| Search | ❌ 없음 | 검색/자동완성/인기·최근 검색어 |
| Delivery | ❌ 없음 | 주문 내 배송 정보/추적 UI |
| Admin Coupon | ❌ 없음 | 쿠폰 생성/목록/비활성화 |
| Admin TimeDeal | ❌ 없음 | 타임딜 생성/수정/삭제 |
| Admin Order | ❌ 없음 | 주문 목록/상태 변경 |
| Admin Delivery | ❌ 없음 | 배송 상태 관리 |
| Admin Inventory | ❌ 없음 | 재고 조회/이동 이력 |

---

## 4. 기존 시나리오 문서 현황

| ID | 제목 | Shopping 관련 |
|----|------|-------------|
| SCENARIO-001 | 선착순 쿠폰 발급 | ✅ 백엔드 중심 |
| SCENARIO-002 | 주문/결제 Saga 패턴 | ✅ 백엔드 중심 |
| SCENARIO-004 | 타임딜 (Planned) | ⏳ 미작성 |
| SCENARIO-005 | 대기열 (Planned) | ⏳ 미작성 |

**갭**: E2E 사용자 시나리오 관점의 문서 부재 (SCENARIO-016~021 필요)

---

## 5. 예상 갭 요약

### Critical (P0)
- **Saga CREATE_DELIVERY skip**: 주문 완료 후 배송이 자동 생성되지 않음

### High (P1)
- **E2E 테스트 부재**: Coupon, Search, TimeDeal 등 주요 사용자 플로우 미검증
- **배송 추적 UI**: OrderDetailPage 내 배송 정보 표시 실제 동작 검증 필요

### Medium (P2)
- **네비게이션**: 쿠폰/타임딜/대기열 진입 경로 명확성 확인 필요
- **Admin 테스트**: 19개 상품 관리만 존재, 나머지 Admin 영역 미검증

---

## 6. 다음 단계

1. 시나리오 문서 작성 (SCENARIO-016~021)
2. E2E 테스트 코드 작성 (shopping 5개 + admin 5개)
3. Local 환경에서 Playwright 워크스루 → 실제 갭 식별
4. 갭 구현
5. Docker/K8s 환경 검증
