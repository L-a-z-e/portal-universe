# ADR-012: Shopping Service Frontend-Backend Gap Analysis 및 수정

**Status**: Accepted
**Date**: 2026-01-28

## Context

Shopping Service의 프론트엔드(React 18)와 백엔드(Spring Boot)의 API 연동에서 **24개의 불일치**가 발견되었습니다. API 경로 불일치(예: `/products/search` vs `/search/products`), HTTP 메서드 불일치(POST vs PUT), 요청 파라미터 불일치, 백엔드 API 누락(Admin 쿠폰/타임딜 목록), 프론트엔드 UI 누락(타임딜 구매 내역) 등으로 인해 404/400 에러가 발생하고 Admin 페이지가 정상 동작하지 않습니다.

## Decision

**3개 Phase로 나누어 체계적으로 수정**합니다. Phase 1은 Frontend API 경로/메서드 수정, Phase 2는 Backend 누락 엔드포인트 추가, Phase 3은 Frontend 누락 UI 구현입니다.

## Rationale

- **완전한 기능 제공**: Admin 쿠폰/타임딜 목록 조회, 사용자 타임딜 구매 내역 확인 필수
- **장기적 유지보수성**: API 경로 불일치는 향후 혼란 야기, 명확한 API 명세로 통일 필요
- **개발 효율성**: 누락된 Backend API 추가는 간단, Frontend UI는 기존 패턴 재사용 가능
- **위험 완화**: 기존 API 변경 없이 경로/메서드만 수정, 신규 API만 추가

## Trade-offs

✅ **장점**:
- 24개 gap 중 Critical 10개 + High 5개 해결, 완전한 API 연동
- Admin 기능 완성 (쿠폰/타임딜 목록 조회)
- 사용자 경험 개선 (타임딜 구매 내역 페이지)
- 일관된 API 명세 (hyphen 표기법, HTTP 메서드 통일)

⚠️ **단점 및 완화**:
- 코드 변경량 증가 (Frontend 1파일 수정/1페이지 신규, Backend 6파일 수정) → (완화: Phase별 순차 배포, 검증)
- 통합 테스트 필요 → (완화: Postman Collection 업데이트, E2E 테스트 자동화)
- 배포 순서 고려 (Backend 먼저, Frontend 나중) → (완화: 각 Phase 완료 후 검증)

## Implementation

### Phase 1: Frontend API 수정 (Critical 10개)
**파일**: `frontend/shopping-frontend/src/api/endpoints.ts`

| # | 항목 | Before | After |
|---|------|--------|-------|
| 1 | productApi.searchProducts | `/products/search` | `/search/products` |
| 2 | inventoryApi.addStock | POST | PUT |
| 3 | couponApi.getAvailableCoupons | `/coupons/available` | `/coupons` |
| 4-5 | timeDealApi | `/timedeals/*` | `/time-deals/*` (hyphen) |
| 6 | timeDealApi.purchaseTimeDeal | body: `{quantity}` | body: `{timeDealProductId, quantity}` |
| 7 | timeDealApi.getMyPurchases | (미존재) | `/time-deals/my/purchases` 추가 |
| 8 | adminCouponApi.deactivateCoupon | POST `/admin/coupons/${id}/deactivate` | DELETE `/admin/coupons/${id}` |
| 9 | adminTimeDealApi | `/admin/timedeals/*` | `/admin/time-deals/*` (hyphen) |
| 10 | adminQueueApi | `/admin/shopping/queue/*` | `/admin/queue/*` (StripPrefix=2) |

### Phase 2: Backend API 추가 (2개)
1. **AdminCouponController**: `GET /admin/coupons` - 쿠폰 전체 목록 페이징 조회
2. **AdminTimeDealController**: `GET /admin/time-deals` - 타임딜 전체 목록 페이징 조회

### Phase 3: Frontend UI 구현 (3개)
1. **TimeDealPurchasesPage**: 사용자 타임딜 구매 내역 페이지 (`/time-deals/purchases`)
2. **네비게이션 링크**: App.tsx에 `/coupons`, `/time-deals` 링크 추가
3. **상품-재고 자동 초기화**: ProductService에서 상품 생성 시 InventoryService 자동 호출

## References

- [Shopping API Reference](../api/shopping-api-reference.md)
- [Coupon API](../api/coupon-api.md)
- [TimeDeal API](../api/timedeal-api.md)
- [ADR-002 API 엔드포인트 설계](./ADR-002-api-endpoint-design.md)
- [ADR-003 Admin 권한 검증 전략](./ADR-003-authorization-strategy.md)

---

📂 상세: [old-docs/central/adr/ADR-012-shopping-frontend-backend-gap-analysis.md](../old-docs/central/adr/ADR-012-shopping-frontend-backend-gap-analysis.md)
