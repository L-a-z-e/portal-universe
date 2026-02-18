# ADR-047: Avro 및 Schema Registry 도입을 통한 이벤트 계약 관리 고도화

**Status**: Proposed
**Date**: 2026-02-18
**Author**: Laze
**Supersedes**: ADR-038 (부분적으로 보완 및 대체 가능성 검토)

## Context

Portal Universe는 17개 이상의 Kafka 토픽을 사용하는 이벤트 기반 아키텍처(EDA)를 갖추고 있습니다. 현재는 JSON 직렬화와 ADR-038에서 정의한 'JSON Schema + CI 검증' 방식을 통해 이벤트를 관리하고 있습니다. 

현재 방식의 한계점:
1. **런타임 검증 부재**: CI 단계에서는 스키마를 검증하지만, 런타임에 잘못된 포맷의 메시지가 발행되는 것을 원천 차단하지 못합니다 (Poison Pill 위험).
2. **스키마 진화(Schema Evolution)의 어려움**: 필드 추가/삭제 시 하위 호환성(Backward/Forward Compatibility)을 수동으로 관리해야 하며, 실수로 인한 장애 발생 가능성이 높습니다.
3. **페이로드 크기**: 텍스트 기반 JSON은 바이너리 포맷에 비해 네트워크 오버헤드와 저장 공간 소모가 큽니다.
4. **Polyglot 코드 생성**: Java, TypeScript, Python 간의 타입 동기화가 여전히 수동 또는 스크립트에 의존적입니다.

## Decision

**Avro 직렬화 포맷과 Schema Registry를 도입하여 이벤트 계약을 시스템적으로 강제합니다.**

### 핵심 결정 사항
1. **Avro 포맷 채택**: 모든 Kafka 이벤트의 표준 직렬화 포맷으로 Apache Avro를 사용합니다.
2. **Schema Registry 도입**: 스키마의 중앙 저장소 및 버전 관리를 위해 Schema Registry(Confluent 또는 Apicurio)를 인프라에 추가합니다.
3. **호환성 정책 강제**: 스키마 업데이트 시 `BACKWARD_TRANSITIVE` 또는 `FULL_TRANSITIVE` 정책을 기본으로 적용하여 컨슈머의 안정성을 보장합니다.
4. **코드 생성 자동화**: Avro IDL(.avdl) 또는 Schema(.avsc)로부터 각 언어별(Java, TS, Python) DTO/Interface를 자동 생성하는 파이프라인을 구축합니다.

## Rationale

- **느슨한 결합(Decoupling)**: 발행자와 구독자는 구체적인 클래스가 아닌 '중앙 스키마'에만 의존합니다. 스키마 레지스트리가 중간에서 버전 호환성을 보장하므로, 서비스 배포 순서에 구애받지 않고 유연하게 확장할 수 있습니다.
- **데이터 무결성**: 잘못된 스키마를 가진 메시지는 발행 시점에 레지스트리에 의해 차단되므로, 전체 시스템에 Poison Pill이 퍼지는 것을 방지합니다.
- **성능 최적화**: Avro는 스키마를 메시지마다 포함하지 않고 ID만 포함하므로 페이로드 크기가 획기적으로 줄어듭니다(약 30~50% 감소).
- **명확한 스키마 진화**: JSON Schema는 필드 삭제나 변경 시 호환성 규칙이 복잡하지만, Avro는 필드 기본값(default) 설정을 통해 명확한 호환성 규칙을 제공합니다.

## Trade-offs

✅ **장점**:
- 런타임 수준의 강력한 데이터 정합성 보장
- 스키마 진화 자동화를 통한 배포 리스크 감소
- 네트워크 전송 효율성 및 저장 비용 절감
- 문서화된 계약(Contract)으로서의 역할 강화

⚠️ **단점 및 완화**:
- **인프라 복잡도 증가**: Schema Registry 서버를 별도로 운영해야 합니다. (완화: Docker Compose 및 K8s StatefulSet으로 관리 자동화)
- **학습 곡선**: Avro IDL 작성법과 직렬화 방식에 대한 팀원들의 학습이 필요합니다. (완화: 가이드 문서 제공 및 점진적 도입)
- **개발 오버헤드**: 초기 설정 및 코드 생성 도구 통합 비용이 발생합니다. (완화: Gradle/NPM 플러그인으로 빌드 단계에 자동화)

## Implementation

### 1. 인프라 설정 (`docker-compose.yml`)
- Confluent Schema Registry 컨테이너 추가
- Kafka 브로커와 연결 설정

### 2. 공통 라이브러리(`common-library`) 업데이트
- `AvroSerializer`, `AvroDeserializer` 추가
- `SpecificRecord` 기반의 범용 KafkaTemplate 설정 제공

### 3. 스키마 관리 (`event-contracts`)
- 기존 `.json` 스키마를 `.avsc` 또는 `.avdl`로 변환
- `gradle-avro-plugin`을 통한 Java 클래스 생성
- `avro-typescript` 등을 통한 TypeScript 타입 생성

### 4. 마이그레이션 전략
- Phase 1: 신규 토픽에 우선 적용
- Phase 2: 기존 중요 토픽(주문, 결제)부터 점진적 전환
- Phase 3: 전사 표준화

## References

- [ADR-032: Kafka Configuration Standardization](./ADR-032-kafka-configuration-standardization.md)
- [ADR-038: Polyglot Event Contract Management](./ADR-038-polyglot-event-contract-management.md)
- Apache Avro Specification: https://avro.apache.org/docs/current/spec.html
- Confluent Schema Registry Overview: https://docs.confluent.io/platform/current/schema-registry/index.html

---

### 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-18 | 초안 작성 및 Avro/Schema Registry 도입 제안 | Laze |
