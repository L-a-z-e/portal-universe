# ADR-038: Polyglot 이벤트 계약(Event Contract) 관리 전략

**Status**: Superseded by [ADR-047](./ADR-047-avro-schema-registry-adoption.md)
**Date**: 2026-02-13
**Author**: Laze
**Superseded by**: ADR-047 (Avro + Schema Registry)

## Context

Portal Universe는 Java(Spring Boot), NestJS, Python을 혼용하는 Polyglot 아키텍처를 채택했다. ADR-032에서 Kafka Topic 상수를 Java events 모듈로 집중하여 topic 이름의 중복 정의를 해결했지만, **이벤트 스키마(필드 구조)의 동기화는 여전히 보장되지 않는다**. 특히 prism-service(NestJS)는 Java 모듈을 참조할 수 없어 `PrismTaskCompletedEvent`, `PrismTaskFailedEvent`의 TypeScript 인터페이스를 별도로 정의하고 있으며, Java 이벤트 클래스의 필드 변경 시 TypeScript 인터페이스가 자동으로 업데이트되지 않는다.

## Decision

JSON Schema를 Single Source of Truth로 삼아 CI에서 Java 클래스 및 TypeScript 인터페이스와의 일치성을 검증하는 **대안 3 (JSON Schema + CI 검증)**을 채택한다. Phase 1에서는 Prism 이벤트 2개에만 적용하여 파일럿을 진행하고, 검증 후 다른 이벤트 모듈로 확장한다.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① AsyncAPI spec + 코드 생성 | YAML에서 모든 언어 코드 자동 생성, API 문서 자동화 | 빌드 파이프라인 복잡, 학습 비용, 생성 코드 커스터마이징 어려움 |
| ② Confluent Schema Registry (Avro/Protobuf) | 런타임 스키마 검증, 호환성 체크 자동, 스키마 진화 지원 | 인프라 추가(Schema Registry 서버), Avro/Protobuf로의 직렬화 전환 필요 |
| **③ JSON Schema + CI 검증 (채택)** | 현재 JSON 직렬화 유지, 점진적 도입 가능, 인프라 추가 없음 | 수동 스키마 업데이트 필요 (CI가 불일치 감지만, 자동 수정은 안 함) |
| ④ Shared contract repository (서브모듈) | 독립적 버전 관리 | 서브모듈 관리 복잡, 현재 모노레포 구조와 불일치 |

## Rationale

- 현재 모든 서비스가 JSON 직렬화(`JsonSerializer`, `JSON.stringify`)를 사용 중이므로 Avro/Protobuf 전환은 과도한 변경이다
- AsyncAPI는 16개 이벤트 전체에 대해 YAML 작성 및 생성 코드 통합이 필요하므로 점진적 도입이 어렵다
- JSON Schema는 단 2개 파일로 시작하여 효과를 검증한 후 확장할 수 있다
- TypeScript 생태계에서 JSON Schema 검증 도구(`ajv`, `@hyperjump/json-schema` 등)가 성숙했다
- CI 검증 실패 시 PR을 차단하여 스키마 불일치가 프로덕션에 배포되는 것을 방지한다

## Trade-offs

✅ **장점**:
- 현재 JSON 기반 직렬화를 그대로 유지하면서 계약 관리 도입 가능
- Schema Registry 등의 인프라 추가 없이 CI만으로 검증
- Prism 이벤트 2개로 시작하여 점진적으로 확장
- JSON Schema를 문서로 활용 가능 (이벤트 필드 타입, 필수 여부가 YAML에 명시됨)

⚠️ **단점 및 완화**:
- Java 클래스 변경 시 JSON Schema를 수동으로 업데이트해야 함 → (완화: CI 검증이 실패하면 PR 차단, 개발자가 스키마 누락을 즉시 인지)
- TypeScript 인터페이스도 수동 동기화 필요 → (완화: 동일, CI가 검증 실패 시 변경 강제)
- 런타임 스키마 검증 부재 (Schema Registry 대비) → (현재 단계에서는 컴파일 타임 타입 안전성 + CI 검증으로 충분, 향후 Schema Registry 전환 시 JSON Schema를 Avro로 변환하는 도구 존재)

## Implementation

### Phase 1: Prism 이벤트 파일럿 (구현 완료)

**구현된 파일**:

| 파일 | 역할 |
|------|------|
| `services/event-contracts/schemas/prism.task.completed.schema.json` | TaskCompleted 이벤트 JSON Schema |
| `services/event-contracts/schemas/prism.task.failed.schema.json` | TaskFailed 이벤트 JSON Schema |
| `services/prism-service/src/modules/event/prism-topics.ts` | NestJS Topic 상수 (Java PrismTopics.java 대응) |
| `scripts/validate-event-contracts.js` | CI 검증 스크립트 (Java/TS ↔ Schema 일치성) |

**검증 대상**:
- `services/prism-events/src/main/java/com/portal/universe/event/prism/PrismTaskCompletedEvent.java`
- `services/prism-events/src/main/java/com/portal/universe/event/prism/PrismTaskFailedEvent.java`
- `services/prism-service/src/modules/event/kafka.producer.ts` (`TaskEvent`, `TaskFailedEvent` 인터페이스)

**CI 검증 방식** (외부 의존성 없이 순수 Node.js):
1. Java record 필드명을 정규식으로 파싱
2. TypeScript interface 필드명을 정규식으로 파싱 (상속 포함)
3. JSON Schema의 `properties` 키와 양측 필드 비교
4. Topic 상수 일치성 검증 (PrismTopics.java ↔ prism-topics.ts)
5. 불일치 시 exit code 1 → PR 차단

### Phase 2: 확장 (파일럿 성공 후)

- Shopping 이벤트 9개, Blog 이벤트 4개, Auth 이벤트 1개로 확장
- `validate.ts` 스크립트를 events 모듈별로 실행하도록 확장

## References

- [ADR-032: Kafka Configuration Standardization](./ADR-032-kafka-configuration-standardization.md) - Topic 상수 집중, NestJS 하드코딩 제약 인정
- [Event-Driven Architecture](../architecture/system/event-driven-architecture.md) - 현재 Kafka 아키텍처 및 17개 topic 매핑
- `services/prism-service/src/modules/event/kafka.producer.ts` - NestJS Kafka Producer 현재 구현
- `services/prism-events/` - Prism 이벤트 모듈 (PrismTopics, PrismTaskCompletedEvent, PrismTaskFailedEvent)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-11 | 초안 작성 | Laze |
| 2026-02-13 | Phase 1 구현 완료, Status: Proposed → Accepted | Laze |
| 2026-02-21 | Status: Accepted → Superseded by ADR-047 (Avro + Schema Registry로 전환 완료) | Laze |
