---
id: guide-shopping-frontend-gap-implementation
title: Shopping Frontend-Backend Gap 구현 완료 보고서
type: guide
status: current
created: 2026-01-28
updated: 2026-01-28
author: Laze
tags: [shopping-frontend, implementation, gap-analysis, react]
related:
  - guide-admin-product-guide
---

# Shopping Frontend-Backend Gap 구현 완료 보고서

## 📋 개요

shopping-service 백엔드 API는 존재하지만 shopping-frontend에 미구현된 10개 Gap을 모두 구현 완료했습니다.

**프로젝트**: Portal Universe
**서비스**: Shopping Service + Shopping Frontend
**기술 스택**: Spring Boot 3.5.5, React 18, TypeScript
**구현 기간**: 2026-01-28

---

## 🎯 구현 완료 Gap 목록

### 1. 검색 자동완성 (Gap 1)
**Backend API**: `GET /api/v1/search/suggest?q={query}`

**구현 내용**:
- `SearchAutocomplete.tsx` 컴포넌트 - 자동완성 드롭다운 UI
- `useSearchSuggest` hook - 디바운스 기반 검색어 제안 API 호출
- ProductListPage에 통합

**주요 기능**:
```typescript
// 300ms 디바운스 처리
const { suggestions, loading } = useSearchSuggest(searchQuery, 300);
```

---

### 2. 인기 검색어 (Gap 2)
**Backend API**: `GET /api/v1/search/popular?limit={limit}`

**구현 내용**:
- `PopularKeywords.tsx` 컴포넌트 - 인기 검색어 버블 UI
- `usePopularKeywords` hook - 인기 검색어 조회
- ProductListPage에 통합

**주요 기능**:
- Top 10 검색어 표시
- 클릭 시 검색 실행

---

### 3. 최근 검색어 (Gap 3)
**Backend API**:
- `GET /api/v1/search/recent` - 조회
- `DELETE /api/v1/search/recent/{keyword}` - 삭제

**구현 내용**:
- `RecentKeywords.tsx` 컴포넌트 - 최근 검색어 리스트 + 삭제 버튼
- `useRecentKeywords` hook - 조회 및 삭제 기능
- ProductListPage에 통합

**주요 기능**:
- 검색어별 삭제 가능
- 클릭 시 재검색

---

### 4. 재고 실시간 SSE (Gap 4)
**Backend API**: `GET /api/v1/inventory/stream/{productId}` (SSE)

**구현 내용**:
- `useInventoryStream` hook - EventSource 기반 실시간 재고 업데이트
- ProductDetailPage에 통합

**주요 기능**:
```typescript
// Server-Sent Events로 재고 변화 실시간 수신
const { currentStock } = useInventoryStream(productId);
```

**장점**:
- 폴링 대비 네트워크 효율 향상
- 실시간 재고 표시

---

### 5. 상품 리뷰 Blog 연동 (Gap 5)
**Backend API**: `GET /api/v1/products/{productId}/reviews`

**구현 내용**:
- `ProductReviews.tsx` 컴포넌트 - 리뷰 리스트 UI (Blog 데이터)
- `useProductReviews` hook - 블로그 리뷰 조회
- ProductDetailPage에 새 섹션 추가

**주요 기능**:
- Blog-service와 연동하여 상품별 리뷰 조회
- 작성자, 날짜, 내용, 평점 표시

---

### 6. 결제 환불 Admin (Gap 6)
**Backend API**: `POST /api/v1/admin/payments/{paymentId}/refund`

**구현 내용**:
- `useAdminPayments` hook - 관리자 결제 관리 (환불 포함)
- AdminOrderDetailPage에 "환불 처리" 버튼 추가

**주요 기능**:
- 주문 상세 페이지에서 환불 버튼 클릭
- 환불 사유 입력 후 처리

---

### 7. Admin 대기열 관리 (Gap 7)
**Backend API**:
- `GET /api/v1/admin/queue/waiting` - 대기열 조회
- `POST /api/v1/admin/queue/process` - 대기열 처리

**구현 내용**:
- `AdminQueuePage.tsx` - 대기열 관리 페이지
- `useAdminQueue` hook - 대기열 조회 및 처리

**주요 기능**:
- 대기 중인 사용자 목록 표시
- 일괄 처리 기능

---

### 8. Admin 재고 이동 이력 (Gap 8)
**Backend API**: `GET /api/v1/admin/inventory/stock-movements?productId={id}&startDate={date}&endDate={date}`

**구현 내용**:
- `AdminStockMovementPage.tsx` - 재고 이동 이력 페이지
- `useAdminStockMovements` hook - 이동 이력 조회

**주요 기능**:
- 날짜 범위 필터
- 상품별 필터
- 이동 유형 (입고/출고/조정) 표시

---

### 9. Admin 배송 관리 (Gap 9)
**Backend API**:
- `GET /api/v1/admin/deliveries` - 배송 목록
- `PUT /api/v1/admin/deliveries/{id}/status` - 배송 상태 변경

**구현 내용**:
- `AdminDeliveryPage.tsx` - 배송 관리 페이지
- `useAdminDelivery` hook - 배송 조회 및 상태 변경

**주요 기능**:
- 배송 상태별 필터
- 배송 상태 업데이트 (준비중 → 배송중 → 배송완료)

---

### 10. Admin 주문 관리 (Gap 10)
**Backend API**: ⚠️ **신규 생성 (Backend 누락)**
- `GET /api/v1/admin/orders` - 주문 목록
- `GET /api/v1/admin/orders/{orderNumber}` - 주문 상세

**구현 내용**:
- **Backend**:
  - `AdminOrderController.java` - REST API 엔드포인트
  - `AdminOrderService.java` - 서비스 인터페이스
  - `AdminOrderServiceImpl.java` - 서비스 구현체
  - `OrderRepository.java`에 검색 메서드 추가
- **Frontend**:
  - `AdminOrderListPage.tsx` - 주문 목록 페이지
  - `AdminOrderDetailPage.tsx` - 주문 상세 페이지
  - `useAdminOrders` hook - 주문 조회

**주요 기능**:
- 주문 번호/사용자 ID 검색
- 주문 상태별 필터
- 주문 상세 조회
- 결제 정보 표시

---

## 📂 수정 파일 (6개)

### Frontend

| 파일 | 변경 내용 |
|------|----------|
| `frontend/shopping-frontend/src/types/index.ts` | SearchSuggestion, InventoryUpdate, BlogReview, ProductWithReviews 타입 추가 |
| `frontend/shopping-frontend/src/api/endpoints.ts` | searchApi, inventoryStreamApi, productReviewApi, adminPaymentApi, adminOrderApi 추가 |
| `frontend/shopping-frontend/src/pages/ProductListPage.tsx` | SearchAutocomplete, PopularKeywords, RecentKeywords 컴포넌트 통합 |
| `frontend/shopping-frontend/src/pages/ProductDetailPage.tsx` | SSE 재고 업데이트 + ProductReviews 섹션 추가 |
| `frontend/shopping-frontend/src/components/layout/AdminLayout.tsx` | Orders, Deliveries, Stock Movements, Queue 네비게이션 추가 |
| `frontend/shopping-frontend/src/router/index.tsx` | 5개 Admin 라우트 추가 (/admin/orders, /admin/orders/:orderNumber, /admin/deliveries, /admin/stock-movements, /admin/queue) |

---

## 🆕 생성 파일

### Frontend (17개)

**Hooks (8개)**:
- `hooks/useSearch.ts` - 검색 자동완성, 인기/최근 검색어 통합
- `hooks/useInventoryStream.ts` - SSE 재고 스트림
- `hooks/useProductReviews.ts` - Blog 리뷰 조회
- `hooks/useAdminPayments.ts` - 관리자 결제 관리
- `hooks/useAdminOrders.ts` - 관리자 주문 관리
- `hooks/useAdminDelivery.ts` - 관리자 배송 관리
- `hooks/useAdminStockMovements.ts` - 재고 이동 이력
- `hooks/useAdminQueue.ts` - 대기열 관리

**Components (4개)**:
- `components/search/SearchAutocomplete.tsx` - 검색 자동완성 UI
- `components/search/PopularKeywords.tsx` - 인기 검색어 버블
- `components/search/RecentKeywords.tsx` - 최근 검색어 리스트
- `components/product/ProductReviews.tsx` - 상품 리뷰 섹션

**Pages (5개)**:
- `pages/admin/AdminOrderListPage.tsx` - 주문 목록
- `pages/admin/AdminOrderDetailPage.tsx` - 주문 상세
- `pages/admin/AdminDeliveryPage.tsx` - 배송 관리
- `pages/admin/AdminStockMovementPage.tsx` - 재고 이동 이력
- `pages/admin/AdminQueuePage.tsx` - 대기열 관리

---

### Backend (3개 + Repository 메서드)

**Controllers**:
- `services/shopping-service/src/main/java/.../controller/admin/AdminOrderController.java`

**Services**:
- `services/shopping-service/src/main/java/.../service/admin/AdminOrderService.java`
- `services/shopping-service/src/main/java/.../service/admin/impl/AdminOrderServiceImpl.java`

**Repository 확장**:
- `OrderRepository.java`에 추가:
  - `List<Order> findByStatus(OrderStatus status)`
  - `List<Order> findByOrderNumberContainingOrUserIdContaining(String orderNumber, String userId)`

---

## ✅ 검증 결과

### Frontend
```bash
cd frontend/shopping-frontend
npm run build
# ✓ TypeScript 타입 체크 통과
# ✓ 빌드 성공
```

### Backend
```bash
cd services/shopping-service
./gradlew compileJava
# ✓ 컴파일 성공
```

---

## 🔗 접근 경로

| 경로 | 기능 |
|------|------|
| `/` | 검색 자동완성, 인기/최근 검색어 |
| `/products/:id` | 리뷰 섹션, 실시간 재고 (SSE) |
| `/admin/orders` | 주문 목록 |
| `/admin/orders/:orderNumber` | 주문 상세 + 환불 |
| `/admin/deliveries` | 배송 관리 |
| `/admin/stock-movements` | 재고 이동 이력 |
| `/admin/queue` | 대기열 관리 |

---

## 🎨 주요 기술 특징

### 1. EventSource (SSE) 활용
```typescript
// useInventoryStream.ts
const eventSource = new EventSource(
  `${API_BASE_URL}/api/v1/inventory/stream/${productId}`
);

eventSource.onmessage = (event) => {
  const update: InventoryUpdate = JSON.parse(event.data);
  setCurrentStock(update.quantity);
};
```

### 2. Debounce 검색
```typescript
// useSearchSuggest.ts
const debouncedQuery = useDebounce(query, delay);
```

### 3. React 18 Hooks 패턴
- `useState`, `useEffect`, `useCallback` 활용
- Custom Hooks로 로직 분리
- 타입 안정성 확보 (TypeScript strict mode)

### 4. API 응답 래핑
```java
// AdminOrderController.java
return ResponseEntity.ok(ApiResponse.success(orderService.getOrders(...)));
```

---

## 📊 구현 통계

| 항목 | 수량 |
|------|------|
| Gap 해결 | 10개 |
| Frontend 수정 파일 | 6개 |
| Frontend 생성 파일 | 17개 |
| Backend 생성 파일 | 3개 + Repository 메서드 |
| 총 코드 라인 | ~2,500 LOC |

---

## 🔄 다음 단계

### 권장 작업
1. **E2E 테스트 작성** - Playwright로 전체 플로우 검증
2. **SSE 연결 복원 로직** - 네트워크 끊김 시 자동 재연결
3. **환불 워크플로우 확장** - 부분 환불, 환불 승인 프로세스
4. **대기열 Redis 통합** - 현재 In-Memory 구현을 Redis 기반으로 전환

### 모니터링
- SSE 연결 상태 로깅
- 검색 자동완성 응답 시간 측정
- Admin 페이지 성능 프로파일링

---

## 🔗 관련 문서

- [Admin 상품 관리 가이드](./guides/admin-product-guide.md)
- [ADR-002: API 엔드포인트 설계](./adr/ADR-002-api-endpoint-design.md)
- [React Patterns](../.claude/rules/react.md)
- [TypeScript Patterns](../.claude/rules/typescript.md)

---

## 📝 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-01-28 | 초기 작성 - 10개 Gap 구현 완료 보고서 | Claude |

---

**최종 업데이트**: 2026-01-28
**상태**: ✅ 구현 완료
**검증**: ✅ Frontend/Backend 빌드 성공
