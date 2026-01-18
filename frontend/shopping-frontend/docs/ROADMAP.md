# Shopping Frontend 구현 로드맵

Shopping Frontend의 단계별 구현 계획입니다.

## Phase 1: Bootstrap (현재 상태) ✅

**기간**: Week 1-2

### 완료된 작업
- [x] 프로젝트 구조 설정
- [x] Vite + React 18 기본 설정
- [x] Module Federation 설정
- [x] Dual Mode 라우팅
- [x] Portal Shell 통신 파이프라인
- [x] 인증 상태 관리 (Zustand)
- [x] 테마 동기화 (light/dark mode)
- [x] TypeScript 설정
- [x] Tailwind CSS 설정

## Phase 2: Core Features (다음 2주)

**기간**: Week 3-4

### 상품 관리 기능
- [ ] ProductListPage (검색, 필터, 정렬, 페이지네이션)
- [ ] ProductDetailPage (이미지 갤러리, 상품 정보, 리뷰)
- [ ] productStore (Zustand)
- [ ] useProduct() hook

### 쇼핑 카트 기능
- [ ] cartStore (Zustand)
  - addItem, removeItem, updateQuantity, clear
- [ ] CartPage UI
- [ ] useCart() hook
- [ ] localStorage 동기화

## Phase 3: Checkout & Orders (2주)

**기간**: Week 5-6

### 결제 플로우
- [ ] CheckoutPage (배송 정보, 결제 수단)
- [ ] 주문 생성 API 연동
- [ ] 주문 완료 페이지

### 주문 관리
- [ ] OrderListPage (주문 목록 조회)
- [ ] OrderDetailPage (배송 추적)
- [ ] orderStore (Zustand)

## Phase 4: Admin Panel (2주)

**기간**: Week 7-8

### 관리자 상품 관리
- [ ] AdminProductListPage (데이터 테이블)
- [ ] AdminProductFormPage (등록/수정)
- [ ] 상품 삭제 (소프트 삭제)
- [ ] 대량 작업 기능

### 재고 관리
- [ ] 재고 업데이트 API
- [ ] 재고 부족 알림
- [ ] 재고 조정 기능

## Phase 5: Advanced Features (2주)

**기간**: Week 9-10

### 사용자 기능
- [ ] 위시리스트 (찜하기)
- [ ] 상품 리뷰 및 평점
- [ ] 배송 알림
- [ ] 주문 추적 상세

### 성능 최적화
- [ ] React Query 재통합 (캐싱)
- [ ] 이미지 최적화 (WebP)
- [ ] 번들 최적화
- [ ] 무한 스크롤

### 테스트
- [ ] Unit tests (Vitest)
- [ ] Component tests (React Testing Library)
- [ ] Integration tests
- [ ] E2E tests (Playwright)

## Phase 6: Polish & Deployment (2주)

**기간**: Week 11-12

### UX/UI 개선
- [ ] 사용성 테스트
- [ ] 접근성 개선 (WCAG)
- [ ] 반응형 디자인 최적화
- [ ] 로딩/에러 상태 UI

### 배포
- [ ] 프로덕션 빌드 최적화
- [ ] Docker 이미지
- [ ] Kubernetes 매니페스트
- [ ] CI/CD 파이프라인
- [ ] 모니터링 및 로깅

## 마일스톤

| Milestone | 목표 | 예상 완료 |
|-----------|------|---------|
| M1 | Bootstrap 완료 | Week 2 |
| M2 | 상품/장바구니 | Week 5 |
| M3 | 주문 관리 | Week 7 |
| M4 | 관리자 패널 | Week 9 |
| M5 | 테스트 및 배포 | Week 11 |
| M6 | 프로덕션 배포 | Week 12 |

## 의존성

### 백엔드 API

```
# Products
GET    /api/v1/shopping/products
GET    /api/v1/shopping/products/{id}
POST   /api/v1/shopping/products (admin)
PUT    /api/v1/shopping/products/{id} (admin)
DELETE /api/v1/shopping/products/{id} (admin)

# Cart
POST   /api/v1/shopping/cart/items
DELETE /api/v1/shopping/cart/items/{id}
PUT    /api/v1/shopping/cart/items/{id}

# Orders
GET    /api/v1/shopping/orders
GET    /api/v1/shopping/orders/{number}
POST   /api/v1/shopping/orders

# Admin
GET    /api/v1/shopping/admin/products
POST   /api/v1/shopping/admin/products
PUT    /api/v1/shopping/admin/products/{id}
DELETE /api/v1/shopping/admin/products/{id}
PUT    /api/v1/shopping/admin/products/{id}/stock
```

## 위험 요소

| 위험 | 영향도 | 대응책 |
|------|-------|--------|
| React Query MF 호환성 | 중 | Phase 2에서 재통합 테스트 |
| PG 연동 복잡도 | 높음 | 3주차 초반 PG 선정 및 PoC |
| 대용량 상품 성능 | 중 | Code splitting + 가상 스크롤 |
| 관리자 패널 복잡도 | 낮음 | 재사용 컴포넌트 개발 |
