# ADR-012: Shopping Service Frontend-Backend Gap Analysis 및 수정

## 상태
**Accepted**

## 날짜
2026-01-28

---

## 컨텍스트

Shopping Service의 프론트엔드(React 18)와 백엔드(Spring Boot)의 API 연동에서 **24개의 불일치(gap)**가 발견되었습니다.

### 발견된 주요 문제

1. **API 경로 불일치**
   - 프론트엔드 `endpoints.ts`의 경로가 백엔드 Controller와 다름
   - 예: `/products/search` vs `/search/products`
   - 예: `/timedeals/active` vs `/time-deals` (hyphen 표기 불일치)

2. **HTTP 메서드 불일치**
   - 프론트엔드: POST `/api/shopping/inventory/add`
   - 백엔드: PUT `/api/shopping/inventory/add`

3. **요청 파라미터 불일치**
   - 프론트엔드: `timeDealApi.purchaseTimeDeal(id, {quantity})`
   - 백엔드: `@RequestBody {timeDealProductId, quantity}`

4. **백엔드 API 누락**
   - Admin 쿠폰 목록 조회 API 없음 (GET `/admin/coupons`)
   - Admin 타임딜 목록 조회 API 없음 (GET `/admin/time-deals`)

5. **프론트엔드 UI 누락**
   - 타임딜 구매 내역 페이지 없음 (백엔드 API는 존재)
   - 상품-재고 초기화 기능 미연동

### 영향 범위

- 프론트엔드에서 백엔드 API 호출 시 404 또는 400 에러 발생
- Admin 페이지에서 목록 조회 불가
- 사용자가 타임딜 구매 내역을 확인할 수 없음

---

## 결정

**3개 Phase로 나누어 체계적으로 수정합니다.**

### Phase 1: Frontend API 경로/메서드 수정 (10개 Critical Fix)

프론트엔드 `endpoints.ts`를 백엔드 Controller 명세에 맞게 수정합니다.

| # | 항목 | Before | After | 이유 |
|---|------|--------|-------|------|
| 1 | `productApi.searchProducts` | `/products/search` | `/search/products` | SearchController 경로 일치 |
| 2 | `inventoryApi.addStock` | POST | PUT | InventoryController의 `@PutMapping` |
| 3 | `couponApi.getAvailableCoupons` | `/coupons/available` | `/coupons` | CouponController 경로 일치 |
| 4 | `timeDealApi.getActiveTimeDeals` | `/timedeals/active` | `/time-deals` | TimeDealController hyphen 표기 |
| 5 | `timeDealApi.getTimeDeal` | `/timedeals/${id}` | `/time-deals/${id}` | TimeDealController hyphen 표기 |
| 6 | `timeDealApi.purchaseTimeDeal` | `/timedeals/${id}/purchase`<br/>body: `{quantity}` | `/time-deals/purchase`<br/>body: `{timeDealProductId, quantity}` | TimeDealController 실제 구현 일치 |
| 7 | `timeDealApi.getMyPurchases` | (미존재) | `/time-deals/my/purchases` 추가 | 구매 내역 조회 기능 누락 |
| 8 | `adminCouponApi.deactivateCoupon` | POST `/admin/coupons/${id}/deactivate` | DELETE `/admin/coupons/${id}` | AdminCouponController 실제 구현 |
| 9 | `adminTimeDealApi` 전체 | `/admin/timedeals/...` | `/admin/time-deals/...` | AdminTimeDealController hyphen 표기 |
| 10 | `adminQueueApi` 전체 | `/admin/shopping/queue/...` | `/admin/queue/...` | Gateway StripPrefix=2로<br/>`/api/shopping` 이미 제거됨 |

**수정된 파일**: `frontend/shopping-frontend/src/api/endpoints.ts`

---

### Phase 2: Backend 누락 엔드포인트 추가 (2개)

#### 1. AdminCouponController - GET `/admin/coupons`

**목적**: 관리자 쿠폰 전체 목록 페이징 조회

**구현**:
```java
@GetMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<Page<CouponResponse>>> getAllCoupons(
    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
) {
    return ResponseEntity.ok(ApiResponse.success(couponService.getAllCoupons(pageable)));
}
```

**Service 메서드 추가**:
- `CouponService.getAllCoupons(Pageable)`
- `CouponServiceImpl`에서 `couponRepository.findAll(pageable).map(CouponResponse::from)` 구현

#### 2. AdminTimeDealController - GET `/admin/time-deals`

**목적**: 관리자 타임딜 전체 목록 페이징 조회

**구현**:
```java
@GetMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<Page<TimeDealResponse>>> getAllTimeDeals(
    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
) {
    return ResponseEntity.ok(ApiResponse.success(timeDealService.getAllTimeDeals(pageable)));
}
```

**Service 메서드 추가**:
- `TimeDealService.getAllTimeDeals(Pageable)`
- `TimeDealServiceImpl`에서 `timeDealRepository.findAll(pageable).map(TimeDealResponse::from)` 구현

---

### Phase 3: Frontend 누락 UI 구현 (3개)

#### 1. TimeDealPurchasesPage - 사용자 타임딜 구매 내역 페이지

**신규 파일**:
- `pages/timedeal/TimeDealPurchasesPage.tsx` 생성
- `hooks/useTimeDeals.ts`에 `useTimeDealPurchases` 훅 추가
- `router/index.tsx`에 `/time-deals/purchases` 경로 추가
- `types/index.ts`에 `TimeDealPurchase` 타입 정의 추가

**기능**:
- 사용자의 타임딜 구매 내역 목록 표시
- 구매 날짜, 상품명, 수량, 가격 정보 표시
- 페이징 처리

#### 2. 네비게이션 링크 추가

**수정 파일**: `App.tsx`

Standalone 모드 헤더에 링크 추가:
- `/coupons` - 쿠폰 페이지
- `/time-deals` - 타임딜 페이지

#### 3. 상품-재고 자동 초기화

**목적**: 상품 생성 시 재고 자동 초기화

**Backend 수정**:
- `ProductServiceImpl.createProductAdmin`에서 상품 생성 후 `InventoryService.initializeInventory` 자동 호출

**Frontend 수정**:
- `endpoints.ts`에 `inventoryApi.initializeInventory` 메서드 추가 (수동 초기화 지원)

---

## 대안 검토

| 대안 | 장점 | 단점 | 결정 |
|------|------|------|------|
| **프론트엔드만 수정** | 백엔드 변경 없음<br/>빠른 수정 가능 | Admin 목록 조회 불가<br/>타임딜 구매 내역 UI 없음 | ❌ |
| **백엔드도 함께 수정** | 완전한 기능 제공<br/>프론트-백 완전 연동 | 양쪽 변경 필요<br/>통합 테스트 필요 | ✅ **선택** |
| **API Versioning (v2)** | 하위호환성 유지<br/>기존 클라이언트 영향 없음 | 과도한 복잡성<br/>버전 관리 부담 | ❌ |

### 선택 근거

1. **완전한 기능 제공**
   - Admin이 쿠폰/타임딜 목록을 조회할 수 있어야 함
   - 사용자가 타임딜 구매 내역을 확인할 수 있어야 함

2. **장기적 유지보수성**
   - API 경로 불일치는 향후 혼란 야기
   - 명확한 API 명세로 통일 필요

3. **개발 효율성**
   - 누락된 Backend API 추가는 간단 (각 1개 엔드포인트)
   - Frontend UI 추가는 기존 패턴 재사용 가능

4. **위험 완화**
   - 기존 API는 변경하지 않고, 경로/메서드만 수정
   - 신규 API만 추가하므로 기존 기능 영향 없음

---

## 결과

### 긍정적 영향

1. **완전한 API 연동**
   - 24개 gap 중 **Critical 10개 + High 5개 해결**
   - 프론트엔드-백엔드 완전 연동

2. **Admin 기능 완성**
   - 쿠폰 목록 조회 가능
   - 타임딜 목록 조회 가능
   - 재고 관리 기능 정상 동작

3. **사용자 경험 개선**
   - 타임딜 구매 내역 페이지 제공
   - 네비게이션 링크 추가로 접근성 향상

4. **일관된 API 명세**
   - Hyphen 표기법 통일 (`time-deals`, `queue`)
   - HTTP 메서드 일치 (PUT, DELETE)

### 부정적 영향

1. **코드 변경량 증가**
   - Frontend: 1개 파일 수정, 1개 신규 페이지, 1개 타입 추가
   - Backend: 6개 파일 수정 (Controller 2개, Service 4개)

2. **통합 테스트 필요**
   - 수정된 API 엔드포인트 전체 테스트
   - Admin 페이지 E2E 테스트

3. **배포 순서 고려**
   - Backend 먼저 배포 (신규 API 추가)
   - Frontend 나중 배포 (새 경로 사용)

### 완화 방안

1. **문서 동시 업데이트**
   - `docs/api/shopping-api-reference.md` 갱신
   - `docs/api/coupon-api.md` 갱신
   - `docs/api/timedeal-api.md` 갱신

2. **통합 테스트 자동화**
   - Postman Collection 업데이트
   - E2E 테스트 시나리오 추가

3. **단계별 배포**
   - Phase 1 → Phase 2 → Phase 3 순차 배포
   - 각 Phase 완료 후 검증

---

## 영향받는 파일

### Frontend

```
frontend/shopping-frontend/src/
├── api/
│   └── endpoints.ts                           (수정) Phase 1
├── pages/
│   └── timedeal/
│       └── TimeDealPurchasesPage.tsx          (신규) Phase 3
├── hooks/
│   └── useTimeDeals.ts                        (수정) Phase 3
├── router/
│   └── index.tsx                              (수정) Phase 3
├── types/
│   └── index.ts                               (수정) Phase 3
└── App.tsx                                     (수정) Phase 3
```

### Backend

```
services/shopping-service/src/main/java/com/portal/universe/shoppingservice/
├── coupon/
│   ├── service/
│   │   ├── CouponService.java                 (수정) Phase 2
│   │   └── CouponServiceImpl.java             (수정) Phase 2
│   └── controller/
│       └── AdminCouponController.java         (수정) Phase 2
├── timedeal/
│   ├── service/
│   │   ├── TimeDealService.java               (수정) Phase 2
│   │   └── TimeDealServiceImpl.java           (수정) Phase 2
│   └── controller/
│       └── AdminTimeDealController.java       (수정) Phase 2
└── product/
    └── service/
        └── ProductServiceImpl.java            (수정) Phase 3
```

---

## 구현 계획

### Phase 1: Frontend API 수정 (즉시)
- [x] `endpoints.ts` 경로/메서드 수정 (10개 항목)
- [ ] API 호출 테스트 (Postman 또는 Frontend 직접)

### Phase 2: Backend API 추가 (1일)
- [ ] `CouponService.getAllCoupons(Pageable)` 구현
- [ ] `AdminCouponController.getAllCoupons` 추가
- [ ] `TimeDealService.getAllTimeDeals(Pageable)` 구현
- [ ] `AdminTimeDealController.getAllTimeDeals` 추가
- [ ] Unit Test 작성
- [ ] API 문서 업데이트

### Phase 3: Frontend UI 추가 (2일)
- [ ] `TimeDealPurchasesPage.tsx` 구현
- [ ] `useTimeDealPurchases` 훅 추가
- [ ] Router 경로 추가
- [ ] `TimeDealPurchase` 타입 정의
- [ ] App.tsx 네비게이션 링크 추가
- [ ] `inventoryApi.initializeInventory` 추가
- [ ] E2E 테스트

---

## 참고 자료

### API 명세서
- [Shopping API Reference](../api/shopping-api-reference.md)
- [Coupon API](../api/coupon-api.md)
- [TimeDeal API](../api/timedeal-api.md)

### 아키텍처 문서
- [Admin API 엔드포인트 설계 (ADR-002)](./ADR-002-api-endpoint-design.md)
- [Admin 권한 검증 전략 (ADR-003)](./ADR-003-authorization-strategy.md)

### 프로젝트 가이드
- [CLAUDE.md - 프로젝트 규칙](../../CLAUDE.md)
- [React Patterns](./.claude/rules/react.md)
- [Spring Patterns](./.claude/rules/spring.md)

---

## Gap Analysis 상세 리포트

### Critical (10개) - Phase 1에서 해결
1. ✅ Product Search API 경로
2. ✅ Inventory Add Stock 메서드
3. ✅ Coupon Available API 경로
4. ✅ TimeDeal Active API 경로
5. ✅ TimeDeal Get API 경로
6. ✅ TimeDeal Purchase API 경로/파라미터
7. ✅ TimeDeal My Purchases API 추가
8. ✅ Admin Coupon Deactivate API 메서드
9. ✅ Admin TimeDeal API 전체 경로
10. ✅ Admin Queue API 전체 경로

### High (5개) - Phase 2, 3에서 해결
1. ⏳ Admin Coupon List API (Phase 2)
2. ⏳ Admin TimeDeal List API (Phase 2)
3. ⏳ TimeDeal Purchases Page (Phase 3)
4. ⏳ Navigation Links (Phase 3)
5. ⏳ Product-Inventory Auto Init (Phase 3)

### Medium (9개) - 향후 검토
- 타입 정의 보완
- 에러 핸들링 개선
- 로딩 상태 처리
- 페이징 UI 개선

---

## 다음 단계

1. **Phase 1 완료 확인**
   - Frontend API 호출 테스트
   - 404/400 에러 해결 확인

2. **Phase 2 Backend 구현**
   - Admin 목록 조회 API 추가
   - Unit Test 작성
   - API 문서 업데이트

3. **Phase 3 Frontend UI 구현**
   - 타임딜 구매 내역 페이지
   - 네비게이션 개선
   - 재고 초기화 연동

4. **문서화**
   - API 명세서 갱신
   - Postman Collection 업데이트
   - 사용자 가이드 작성

---

**문서 버전**: 1.0
**작성자**: Documenter Agent
**최종 업데이트**: 2026-01-28
