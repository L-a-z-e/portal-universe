# ADR-053: Shopping Saga Phase 2 — Cross-Service 보상 및 데이터 정합성

**Status**: Accepted
**Date**: 2026-02-27
**Author**: Laze
**Implements**: ADR-041 (Shopping Service 분해), ADR-026 (Saga Compensation Failure Policy)

## Context

ADR-041에서 shopping-service를 buyer/seller/settlement 3개 서비스로 분해했다. 그러나 기존 Saga 보상 로직에 여러 정합성 문제가 있었다:

1. **서비스 간 인증 부재**: Kafka Consumer/Scheduler에서 Feign 호출 시 HTTP 컨텍스트가 없어 403 Forbidden 발생
2. **DEDUCT_INVENTORY 보상 누락**: compensate()에서 log.warn()만 출력, 실제 재고 복원 없음
3. **PAYMENT 보상 분기 누락**: PROCESS_PAYMENT 완료 시 환불 로직 미존재
4. **Dead Saga 방치**: STARTED 상태로 멈춘 Saga에 대한 자동 복구 없음 → 재고 영구 잠금
5. **completedSteps 문자열 오판**: `"RESERVE".contains("RESERVE_INVENTORY")` 부분 일치 버그
6. **sellerId 미전파**: CartItem/OrderItem에 판매자 정보 없음 → 멀티셀러 정산 불가
7. **결제 취소 고립**: cancelPayment()가 결제 상태만 변경, 주문/재고 취소 누락

## Decision

8개 변경을 통해 Saga 보상 체계를 완성한다.

### D1. 서비스 간 내부 API 토큰 인증

`/internal/**` 엔드포인트에 대해 `X-Internal-Token` 헤더 기반 인증을 도입한다.

- `InternalTokenAuthFilter` (common-library): `X-Internal-Token` 검증 → `ROLE_INTERNAL` 부여
- `FeignClientConfig`: 항상 `X-Internal-Token` 추가 (HTTP/Kafka/Scheduler 컨텍스트 무관)
- seller-service SecurityConfig: `@Order(2)` 전용 필터 체인으로 `/internal/**` 분리

### D2. sellerId 전파 (스냅샷 방식)

CartItem/OrderItem에 `sellerId` 컬럼을 추가하여 상품 담기/주문 시점의 판매자 정보를 스냅샷한다.

- Avro: `OrderItemInfo.avsc`에 `sellerId (default: 0)`, `PaymentCompletedEvent.avsc`에 `items` 배열
- Settlement: `SettlementEventConsumer`에서 `groupingBy(getSellerId)` → 판매자별 개별 ledger 생성
- UNIQUE 제약: `(order_number, seller_id, event_type)` 확장

### D3. Saga 보상 완성 (restoreStock + refundPaymentForCompensation)

- **restoreStock**: 차감된 재고 복원 (deduct의 역연산: `available += qty, total += qty`)
- **releaseStock**: 예약된 재고 해제 (reserve의 역연산: `reserved -= qty, available += qty`)
- **refundPaymentForCompensation**: Saga 보상 전용 환불 (silent 실패: 결제 없거나 환불 불가 시 로그만)
- 보상 순서: DELIVERY 취소 → DEDUCT 복원 → PAYMENT 환불 → RESERVE 해제(DEDUCT 미완료 시만)

### D4. Dead Saga 자동 복구

`DeadSagaRecoveryScheduler`가 5분마다 30분 이상 멈춘 Saga를 탐지하여 자동 보상한다.
`PESSIMISTIC_WRITE + SKIP_LOCKED`로 다중 인스턴스 경합을 방지한다.

### D5. completedSteps 타입 안전성

`SagaStepSetConverter`를 도입하여 DB CSV 문자열 ↔ `Set<SagaStep>` 변환을 수행한다. `isStepCompleted()`는 `Set.contains()`로 Enum 정확 매칭한다.

### D6. 결제 취소 → 주문 취소 이벤트 연결

`PaymentCancelledEvent` (Kafka) → `PaymentCancelledEventConsumer` → `cancelOrder()` 연쇄 호출.
Payment 서비스 분리 후에도 코드 변경 없이 동작하는 이벤트 기반 설계.

## Alternatives

| 결정 | 채택안 | 대안 | 불채택 이유 |
|------|--------|------|------------|
| 내부 API 인증 | **B. 서비스 토큰** (X-Internal-Token) | A. RecordInterceptor (Kafka userId 추출) | Scheduler 미커버. Kafka 전용이라 확장성 부족 |
| sellerId 전파 | **스냅샷** (CartItem/OrderItem 컬럼) | Order 자체를 sellerId별 분리 | 멀티셀러 장바구니 UX 복잡도 증가 |
| Dead Saga 복구 | **DB 스케줄러** (@Scheduled + SKIP_LOCKED) | Kafka Retry Topic | 인프라 의존성 증가, 설정 복잡 |
| completedSteps | **JPA Converter** (Set\<SagaStep\>) | DB JSONB 배열 | 기존 CSV 구조 유지, 마이그레이션 불필요 |
| 결제→주문 취소 | **Kafka 이벤트** (PaymentCancelledEvent) | 동기 Feign 호출 | 순환 의존성, 서비스 분리 시 변경 필요 |

## Trade-offs

**장점**:
- Saga 보상 체계 완성: 모든 단계에서 실패 시 실제 복원 수행
- Dead Saga 자동 복구로 재고 영구 잠금 방지
- 멀티셀러 정산 기반 마련 (sellerId별 groupBy)
- 이벤트 기반 결제→주문 취소로 서비스 분리 대비

**단점 및 완화**:
- 내부 토큰이 대칭키 고정값 → (완화: 환경변수로 관리, 프로덕션 시 rotation 정책 추가)
- PaymentCompletedEvent에 items 포함은 임시 → (완화: Phase 4.5에서 OrderSettlementCreatedEvent로 이전)
- Dead Saga 스케줄러 최대 5분 지연 → (완화: CRITICAL 알림 + CloudWatch 메트릭)

## Implementation

### 수정 파일

**common-library**:
- `AuthConstants.java`: `INTERNAL_TOKEN = "X-Internal-Token"` 상수
- `InternalTokenAuthFilter.java` (신규): OncePerRequestFilter, ROLE_INTERNAL 부여

**shopping-seller-service**:
- `SecurityConfig.java`: `@Order(2)` /internal/** 전용 필터 체인
- `InternalInventoryController.java`: `POST /internal/inventory/restore` 엔드포인트
- `Inventory.java`: `restore(qty)` 메서드 (available += qty, total += qty)
- `MovementType.java`: `RESTORE` 타입 추가
- `application.yml`: `app.internal.token` 설정

**shopping-service**:
- `FeignClientConfig.java`: X-Internal-Token 항상 추가
- `SellerInventoryClient.java`: `restoreStock()` Feign 메서드
- `OrderSagaOrchestrator.java`: `compensateSagaSteps()` 실제 보상 로직
- `SagaState.java`: `completedSteps` → `Set<SagaStep>` (SagaStepSetConverter)
- `SagaStepSetConverter.java` (신규): DB CSV ↔ EnumSet 변환
- `DeadSagaRecoveryScheduler.java` (신규): 5분마다 Dead Saga 탐지/복구
- `PaymentServiceImpl.java`: `refundPaymentForCompensation()`, `cancelPayment()` Kafka 발행
- `PaymentCancelledEventConsumer.java` (신규): 결제 취소 → 주문 취소 연쇄
- `PaymentCompletedEventConsumer.java` (신규): 결제 완료 → Saga 후속 단계
- `CartItem.java`, `OrderItem.java`: sellerId 컬럼
- `V3__add_seller_id_columns.sql` (신규): DDL

**shopping-settlement-service**:
- `SettlementEventConsumer.java`: sellerId별 groupBy 정산
- `SettlementLedger.java`: UniqueConstraint 확장
- `V3__update_ledger_unique_constraint.sql` (신규): UNIQUE (order_number, seller_id, event_type)

**event-contracts**:
- `OrderItemInfo.avsc`: sellerId 필드
- `PaymentCompletedEvent.avsc`: items 배열
- `PaymentCancelledEvent.avsc` (신규): 결제 취소 이벤트

## References

- [ADR-041: Shopping Service 분해](./ADR-041-shopping-service-decomposition.md)
- [ADR-026: Saga Compensation Failure Policy](./ADR-026-saga-compensation-failure-policy.md)
- [ADR-047: Avro Schema Registry Adoption](./ADR-047-avro-schema-registry-adoption.md)
- [Saga Pattern Architecture](../architecture/shopping-service/saga-pattern.md)

## Change Log

| Date | Change | Author |
|------|--------|--------|
| 2026-02-27 | 초안 작성, Accepted | Laze |
