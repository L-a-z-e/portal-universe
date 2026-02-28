# ADR-055: Payment 서비스 독립 추출

**Status**: Accepted
**Date**: 2026-02-28
**Author**: Laze

## Context

Shopping Service에 내장된 Payment 모듈은 쇼핑 주문 결제에만 특화되어 있었다. 그러나 멤버십 결제, 유료 콘텐츠 구독 등 다른 도메인에서도 결제 기능이 필요해지면서 결제는 쇼핑에 종속된 기능이 아닌 플랫폼 공통 관심사임이 명확해졌다. 또한 Payment Intent 패턴(Stripe/Toss 방식)을 도입해 프론트엔드가 서버가 확정한 금액으로만 결제를 진행하도록 하여 금액 변조를 원천 차단해야 했다.

## Decision

Payment 모듈을 shopping-service에서 추출하여 독립 서비스(payment-service :8090)로 분리하고, Payment Intent 패턴을 도입한다. Shopping-service는 Feign Client를 통해 payment-service의 내부 API를 호출하며, 결제 완료 후 payment-service가 Kafka 이벤트를 발행한다.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① shopping-service 내 Payment 유지 | 구현 변경 없음, 단순 | 타 도메인 결제 불가, 책임 혼재 |
| ② 공통 라이브러리로 추출 | 배포 독립성 없이 재사용 | 스키마 공유 불가, 라이브러리 한계 |
| ③ 독립 payment-service + Intent 패턴 | 플랫폼 공통화, 금액 변조 방지, 이벤트 소유권 명확 | 서비스 간 통신 추가, 복잡도 증가 |

## Rationale

- **공통 관심사 분리**: 결제는 쇼핑 외 멤버십·콘텐츠 등 다수 도메인에서 필요한 플랫폼 기능이다
- **Payment Intent 패턴**: 서버가 확정한 금액(amount)과 메타데이터를 intent로 저장하여 프론트엔드 금액 변조를 원천 차단한다
- **이벤트 소유권 명확화**: PaymentCompletedEvent는 payment-service에서 발행하고, shopping-service는 소비자로만 동작한다
- **Transactional Outbox 적용**: 결제 완료와 이벤트 발행의 원자성을 outbox 테이블로 보장한다
- **DB 분리**: payment_db (PostgreSQL)를 독립하여 결제 데이터 보안 경계를 강화한다

## Trade-offs

**장점**:
- 결제 기능을 타 도메인(멤버십, 콘텐츠)에서 재사용 가능
- Payment Intent로 금액 변조 방지 (보안 강화)
- 서비스 독립 배포, 확장, 장애 격리 가능
- 이벤트 소유권이 명확해져 이벤트 체이닝 추적이 용이

**단점 및 완화**:
- [복잡도 증가: Feign 통신 추가] → (완화: internal API + 토큰 검증으로 내부 통신 보안 확보)
- [분산 트랜잭션 관리] → (완화: Outbox 패턴으로 이벤트 발행 원자성 보장, Saga 보상 API `/internal/refund/{orderNumber}` 제공)
- [네트워크 레이턴시] → (완화: payment-service는 동기 Feign으로 Intent 생성, 이후 비동기 이벤트로 처리)

## Implementation

- `services/payment-service/` — 독립 서비스 (포트 :8090, payment_db)
- `services/payment-service/src/main/java/.../intent/` — PaymentIntent 도메인 (CreateIntentRequest, IntentResponse)
- `services/payment-service/src/main/java/.../payment/controller/InternalPaymentController.java` — 서비스 간 내부 API
- `services/payment-service/src/main/java/.../event/outbox/` — Transactional Outbox (OutboxEvent, OutboxPollingScheduler)
- `services/shopping-service/src/main/java/.../feign/PaymentIntentFeignClient.java` — shopping → payment Feign 클라이언트
- `services/shopping-service/src/main/resources/db/migration/V7__drop_payments_table.sql` — shopping_db에서 payments 테이블 제거
- `services/event-contracts/schemas/.../OrderSettlementCreatedEvent.avsc` — 이벤트 체이닝 확장

## References

- [ADR-041: Shopping Service Decomposition](./ADR-041-shopping-service-decomposition.md)
- [ADR-053: Saga Cross-Service Compensation](./ADR-053-saga-cross-service-compensation.md)
- [ADR-054: Event Stability Patterns](./ADR-054-event-stability-patterns.md)
- [Payment Service Architecture](../architecture/payment-service/system-overview.md)
- [Payment Service API](../api/payment-service/payment-api.md)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-28 | 초안 작성 | Laze |
