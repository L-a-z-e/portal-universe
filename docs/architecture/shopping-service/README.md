# Shopping Service Architecture

> 10개 도메인, 18개 테이블, 9개 Kafka topic을 가진 전자상거래 마이크로서비스의 아키텍처 문서

---

## 문서 목록

### Core

| 문서 | 설명 | 최종 업데이트 |
|------|------|:---:|
| [System Overview](./system-overview.md) | 10개 도메인, ERD, 기술 스택, Kafka topic, 에러 코드 체계 | 2026-02-06 |
| [Data Flow](./data-flow.md) | 10개 데이터 플로우, 동시성 제어 (Pessimistic Lock, Lua Script, 분산 락) | 2026-02-06 |

### Domain

| 문서 | 설명 | 핵심 기술 | 최종 업데이트 |
|------|------|----------|:---:|
| [Coupon System](./coupon-system.md) | 선착순 쿠폰 발급, Redis-MySQL 이중 관리 | Redis Lua Script | 2026-02-06 |
| [TimeDeal System](./timedeal-system.md) | 시간 한정 할인, Scheduler 라이프사이클 | Redis Lua Script, 분산 락 | 2026-02-06 |
| [Queue System](./queue-system.md) | 이벤트 대기열, 실시간 순번 알림 | Redis Sorted Set, SSE | 2026-02-06 |
| [Search System](./search-system.md) | Full-text 검색, 자동완성, 인기/최근 검색어 | Elasticsearch, Redis | 2026-02-06 |

### Pattern

| 문서 | 설명 | 최종 업데이트 |
|------|------|:---:|
| [Saga Pattern](./saga-pattern.md) | 5단계 Forward/Compensation 분산 트랜잭션 | 2026-02-06 |

---

## 읽는 순서 (추천)

### 신규 팀원 온보딩
```
System Overview → Data Flow → Saga Pattern → 도메인 문서들
```

### 주문/결제 시스템 이해
```
System Overview (Order, Payment) → Data Flow (주문 생성, 결제 처리) → Saga Pattern
```

### 고동시성 시스템 이해
```
Data Flow (동시성 제어) → Coupon System → TimeDeal System → Queue System
```

### 검색 시스템 이해
```
System Overview (Search) → Search System → Data Flow (상품 검색)
```

---

## 관련 문서

### API 명세
- [Product API](../../api/shopping-service/api-product.md)
- [Cart API](../../api/shopping-service/api-cart.md)
- [Order API](../../api/shopping-service/api-order.md)
- [Payment API](../../api/shopping-service/api-payment.md)
- [Delivery API](../../api/shopping-service/api-delivery.md)
- [Inventory API](../../api/shopping-service/api-inventory.md)

### Database
- [Shopping Service ERD](../database/shopping-service-erd.md)

### ADR (Architecture Decision Records)
- Saga Pattern 선택 - [saga-pattern.md](./saga-pattern.md) 기술적 결정 섹션 참조
- Redis Lua Script 채택 - [coupon-system.md](./coupon-system.md) 기술적 결정 섹션 참조

---

## 문서 작성 규칙

새로운 아키텍처 문서를 추가할 때:

1. **파일명**: `[kebab-case].md` (예: `cache-strategy.md`)
2. **메타데이터**: 필수 YAML frontmatter 포함
3. **구조**: `docs/templates/architecture-template.md` 준수
4. **다이어그램**: Mermaid 사용
5. **README 업데이트**: 이 인덱스 파일에 문서 추가
6. **관련 문서 링크**: 양방향 링크 유지

---

**최종 업데이트**: 2026-02-06
