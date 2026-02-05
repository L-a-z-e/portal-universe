# Architecture Documentation

> Shopping Service의 아키텍처 문서 목록

---

## 📋 문서 목록

| ID | 제목 | 상태 | 최종 업데이트 |
|----|------|------|--------------|
| [arch-system-overview](./system-overview.md) | System Overview | ✅ Current | 2026-01-18 |
| [arch-data-flow](./data-flow.md) | Data Flow | ✅ Current | 2026-01-18 |

---

## 📚 문서 설명

### [System Overview](./system-overview.md)
Shopping Service의 전체 시스템 구조를 설명합니다.

**포함 내용**:
- High-Level Architecture
- 도메인 구조 (Product, Cart, Order, Payment, Delivery, Inventory)
- 데이터베이스 스키마 및 ERD
- 기술 스택
- 성능 목표
- 확장 계획

---

### [Data Flow](./data-flow.md)
Shopping Service의 주요 데이터 흐름과 이벤트 처리를 설명합니다.

**포함 내용**:
- 주문 생성 흐름 (Saga Pattern)
- 결제 처리 흐름
- 재고 관리 흐름
- 배송 추적 흐름
- Kafka 이벤트 발행
- 동시성 제어 (Pessimistic Lock)
- Saga 보상(Compensation) 전략

---

## 🎯 읽는 순서 (추천)

1. **신규 팀원 온보딩**:
   ```
   System Overview → Data Flow → API 문서들
   ```

2. **주문 시스템 이해**:
   ```
   Data Flow (주문 생성) → Saga Pattern → API: Order/Payment
   ```

3. **재고 관리 이해**:
   ```
   System Overview (Inventory) → Data Flow (동시성 제어) → API: Inventory
   ```

---

## 🔗 관련 문서

### API 문서
- [Product API](../api/api-product.md)
- [Cart API](../api/api-cart.md)
- [Order API](../api/api-order.md)
- [Payment API](../api/api-payment.md)
- [Delivery API](../api/api-delivery.md)
- [Inventory API](../api/api-inventory.md)

### ADR (Architecture Decision Records)
- [ADR-001: Saga Pattern 선택](../adr/ADR-001-saga-pattern.md) (예정)
- [ADR-002: Pessimistic Lock 채택](../adr/ADR-002-pessimistic-lock.md) (예정)

### Guides
- [Shopping Service 개발 가이드](../guides/development-guide.md) (예정)
- [Saga 트러블슈팅 가이드](../guides/saga-troubleshooting.md) (예정)

---

## 📝 문서 작성 규칙

새로운 아키텍처 문서를 추가할 때:

1. **파일명**: `[kebab-case].md` (예: `cache-strategy.md`)
2. **메타데이터**: 필수 YAML frontmatter 포함
3. **다이어그램**: Mermaid 사용 권장
4. **README 업데이트**: 이 인덱스 파일에 문서 추가
5. **관련 문서 링크**: 양방향 링크 유지

---

**최종 업데이트**: 2026-01-18
