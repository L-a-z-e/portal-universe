# ADR-057: 판매자 등록 플로우 seller-service 통합

**Status**: Accepted
**Date**: 2026-02-28
**Author**: Laze

---

## Context

판매자 등록이 auth-service(신청/승인)와 seller-service(프로필 등록) 두 곳에 분산되어 있었다.
auth-service에서 승인해도 seller-service의 Seller 엔티티는 생성/활성화되지 않고, seller-service의 등록 페이지는 `ROLE_SHOPPING_SELLER` 없이는 접근 불가하여 신청 자체가 불가능한 모순이 존재했다.

## Decision

seller-service로 전체 판매자 라이프사이클(신청 → 심사 → 승인/거절)을 통합하고, auth-service의 `seller_applications` 관련 코드를 제거한다. 승인 시 Kafka 이벤트(`SellerApprovedEvent`)를 발행하여 auth-service가 `ROLE_SHOPPING_SELLER`을 비동기로 부여한다.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① 현행 유지 (auth + seller 분산) | 변경 비용 없음 | 신청 불가 모순, 두 DB에 데이터 분산 |
| ② auth-service에서 seller-service Feign 호출 | auth 중심 관리 유지 | 동기 결합, auth에 비즈니스 로직 과다 |
| ③ seller-service 통합 + Kafka 이벤트 (선택) | 단일 소유권, 비동기 결합 | 이벤트 지연 시 ROLE 부여 지연 |

## Rationale

- **단일 소유권**: Seller 엔티티, 신청, 심사, 승인이 모두 seller-service에 위치하여 데이터 일관성 보장
- **관심사 분리**: auth-service는 인증/권한만 담당, 판매자 비즈니스 로직은 seller-service로 이관
- **비동기 결합**: Kafka 이벤트로 서비스 간 결합도를 최소화하고, 기존 `RoleAssignedEvent` → `MembershipAutoAssignHandler` 체인을 재활용
- **멱등성**: auth-service의 `RbacService.assignRole()`은 중복 할당 시 예외를 던지므로 이벤트 재처리에 안전

## Trade-offs

✅ **장점**:
- 신청-승인-프로필 관리가 한 서비스에서 완결
- auth-service의 seller_applications 테이블 및 관련 코드 제거로 복잡도 감소
- 승인 시 `RoleAssignedEvent` 체인으로 멤버십 자동 생성 (seller:shopping/BRONZE)

⚠️ **단점 및 완화**:
- [이벤트 지연] 승인 후 ROLE 부여에 수초 지연 가능 → (완화: 프론트엔드에서 안내 메시지 표시, 토큰 갱신 시 반영)
- [Kafka 장애] 이벤트 유실 시 ROLE 미부여 → (완화: `ResilientKafkaPublisher`의 Outbox 패턴으로 보장)

## Implementation

### 신규 파일
- `event-contracts`: `SellerApprovedEvent.avsc`, `SellerTopics.java`
- `seller-service`: `SellerApplyRequest`, `SellerReviewRequest`, `SellerAdminController`, `SellerApprovedEventPublisher`
- `auth-service`: `SellerApprovedEventConsumer`
- `frontend`: `SellerApplyPage.tsx`, `SellerPendingPage.tsx`

### 수정 파일
- `Seller.java`: `REJECTED` 상태 추가, 심사 필드(reason, reviewedBy, reviewComment, reviewedAt)
- `SellerController`: `/sellers/apply`, `/sellers/my-application` 엔드포인트
- `SecurityConfig`: 신청 엔드포인트는 `.authenticated()` (ROLE 불필요)
- `API Gateway`: auth-service-admin 라우트에서 `/api/v1/admin/seller/**` 제거
- `admin-frontend`: seller API 경로를 seller-service로 전환

### 삭제 파일 (auth-service)
- `SellerApplication`, `SellerApplicationStatus`, `SellerApplicationRepository`
- `SellerController`, `SellerAdminController`, `SellerApplicationService`
- 관련 DTO 3개, 테스트 3개
- Flyway: `V4__drop_seller_applications.sql`

### Flyway 마이그레이션
- `seller-service V3__seller_application_fields.sql`: 심사 관련 컬럼 추가
- `auth-service V4__drop_seller_applications.sql`: seller_applications 테이블 삭제

## References

- [ADR-041: Shopping Service Decomposition](./ADR-041-shopping-service-decomposition.md)
- [ADR-054: Event Stability Patterns](./ADR-054-event-stability-patterns.md)
- [Seller Service API 문서](../api/shopping-seller-service/README.md)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-28 | 초안 작성 | Laze |
