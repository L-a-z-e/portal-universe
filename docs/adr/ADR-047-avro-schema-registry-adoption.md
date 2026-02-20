# ADR-047: Avro 및 Schema Registry 도입

**Status**: Accepted
**Date**: 2026-02-21
**Author**: Laze
**Supersedes**: ADR-038 (JSON Schema + CI 검증 → Avro + Schema Registry로 전환)

## Context

Portal Universe는 20개 Kafka 토픽, 10개 서비스(Java 7, NestJS 1, Python 1), Polyglot 이벤트 기반 아키텍처를 운영한다.

ADR-032에서 Topic 상수 표준화를, ADR-038에서 JSON Schema + CI 검증 기반의 이벤트 계약 관리를 도입했다. 그러나 ADR-038은 다음 한계를 갖는다:

1. **파일럿 미확장**: Prism 이벤트 2개만 JSON Schema 적용. 나머지 18개 토픽은 스키마 미작성
2. **런타임 검증 부재**: CI 시점에만 검증. 런타임에 잘못된 메시지가 발행되면 Poison Pill이 전파됨
3. **스키마 진화 수동 관리**: 필드 추가/삭제 시 호환성을 개발자가 직접 판단. 실수 시 Consumer 장애
4. **페이로드 비효율**: JSON 텍스트 직렬화로 인한 네트워크/저장 오버헤드. 대용량 트래픽에서 비용 문제
5. **코드 동기화 수동**: Java record, TypeScript interface, Python dataclass를 각각 수동 관리

### 왜 지금 전환하는가

ADR-038의 "Phase 2 확장"을 진행하기보다 Avro + Schema Registry로 전환하는 이유:

- JSON Schema CI 검증은 **감지만 할 뿐 차단하지 못한다** — 런타임에 잘못된 메시지가 발행되면 감지 불가
- 18개 토픽에 JSON Schema를 모두 작성하고 CI 검증 스크립트를 확장하는 비용과, Avro + Schema Registry를 도입하는 비용이 유사하다
- Schema Registry는 JSON Schema의 상위 호환이다 — 런타임 검증, 호환성 자동 적용, 버전 관리를 모두 포함
- 서비스가 늘어날수록 수동 스키마 동기화의 실수 확률이 기하급수적으로 증가한다

## Decision

**Apache Avro를 Kafka 이벤트 표준 직렬화 포맷으로, Confluent Schema Registry를 스키마 중앙 관리 시스템으로 도입한다.**

### 핵심 결정

| # | 결정 | 근거 |
|---|------|------|
| D1 | Avro 직렬화 채택 | Kafka 생태계 네이티브 통합, 동적 스키마 해석, 바이너리 효율성 |
| D2 | Confluent Schema Registry 도입 | 업계 표준, Kafka 자체를 백엔드 스토리지로 사용, HA 구성 용이 |
| D3 | `BACKWARD_TRANSITIVE` 호환성 정책 | Consumer 안정성 최우선. Producer가 새 스키마로 발행해도 기존 Consumer가 깨지지 않음 |
| D4 | `.avsc`(Avro Schema) 기반 코드 생성 | Java: gradle-avro-plugin, TypeScript: @kafkajs/confluent-schema-registry, Python: fastavro |
| D5 | Subject naming: `{topic-name}-value` | Confluent 표준 TopicNameStrategy. 토픽당 하나의 스키마 |

### 왜 Avro인가 (Protobuf와의 비교)

| 기준 | Avro | Protobuf | 선택 근거 |
|------|------|----------|----------|
| Kafka 생태계 | **네이티브** (Confluent 전체 스택) | 지원하나 2nd-class | Kafka Connect, ksqlDB, Kafka Streams 완벽 통합 |
| 동적 스키마 해석 | **지원** | 불가 (컴파일 필수) | Consumer 재배포 없이 새 필드 읽기 가능 |
| 스키마 진화 | 기본값 기반 (직관적) | 필드 번호 기반 | 기본값만 설정하면 호환성 자동 보장 |
| Polyglot 코드 생성 | Java 우수, TS/Python 보통 | **모든 언어 우수** | 약점이지만 도구/패턴으로 완화 가능 |
| gRPC 겸용 | 불가 | 가능 | 현재 gRPC 미사용. 향후 도입 시 동기=Protobuf, 비동기=Avro 이원화 |

Protobuf는 Polyglot 코드 생성에서 우위를 갖지만, **Kafka 중심 EDA**에서는 Avro의 생태계 친화도와 동적 스키마 해석이 더 큰 가치를 제공한다. Protobuf는 향후 gRPC 동기 통신 도입 시 별도 검토한다.

## Trade-offs

### 장점

| 영역 | 효과 |
|------|------|
| **런타임 정합성** | 스키마 불일치 메시지를 발행 시점에 차단. Poison Pill 원천 방지 |
| **스키마 진화 안전성** | Registry가 호환성 규칙을 자동 적용. 비호환 변경 시 등록 거부 |
| **배포 독립성** | 서비스 배포 순서 무관. Producer/Consumer가 다른 스키마 버전을 사용해도 Registry가 중재 |
| **페이로드 효율** | 바이너리 인코딩 + 스키마 ID만 포함. JSON 대비 30-50% 크기 감소 |
| **계약 문서화** | Registry UI에서 모든 스키마의 버전 이력, 호환성 상태를 확인 가능 |
| **코드 생성** | `.avsc`에서 Java 클래스 자동 생성. 수동 동기화 제거 (Java) |

### 단점 및 완화

| 단점 | 영향 | 완화 전략 |
|------|------|----------|
| **디버깅 편의성 저하** | 바이너리 메시지 → `kafka-console-consumer`로 직접 확인 불가 | AKHQ/Redpanda Console 도입 (Avro 자동 디시리얼라이제이션), `kafka-avro-console-consumer` CLI, 개발 환경 JSON 로깅 미들웨어 |
| **인프라 복잡도 증가** | Schema Registry 서버 운영 필요 | Docker Compose에 컨테이너 추가, K8s Deployment 2-3 replicas. 스토리지는 Kafka `_schemas` 토픽 사용 (별도 DB 불필요) |
| **TypeScript DX 마찰** | Avro 코드 생성 도구 미성숙 (`avsc`, `avro-ts` 등) | `@kafkajs/confluent-schema-registry`로 런타임 encode/decode. 타입 정의는 `.avsc`에서 수동 또는 스크립트 생성 후 보정 |
| **빌드 파이프라인 복잡화** | `.avsc` → 코드 생성 → 컴파일 순서 보장 필요 | gradle-avro-plugin (Java), npm 스크립트 (TypeScript) 로 빌드 단계에 통합 |
| **학습 곡선** | Avro 스키마 문법, 호환성 규칙, Registry API | 가이드 문서 제공, Prism 토픽 2개로 파일럿 후 확장 |
| **Schema Registry SPOF** | Registry 다운 시 새 스키마 등록/새 Consumer 시작 실패 | 캐시 메커니즘 (기존 스키마는 로컬 캐시로 계속 동작), K8s 복수 레플리카 |

### 롤백 전략

Avro 전환이 특정 서비스에서 실패할 경우:
- 해당 토픽만 JSON Serializer/Deserializer로 롤백
- Schema Registry는 유지 (다른 토픽에 영향 없음)
- 토픽별 독립 전환이므로 전체 롤백 불필요

## Implementation

### Phase 0: 인프라 설정

**Schema Registry 추가** (`docker-compose.yml`):

```yaml
schema-registry:
  image: confluentinc/cp-schema-registry:8.1.1
  hostname: schema-registry
  ports:
    - "127.0.0.1:8081:8081"   # 외부 접근 차단 (ADR-046 보안 정책)
  environment:
    SCHEMA_REGISTRY_HOST_NAME: schema-registry
    SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: kafka:29092
    SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081
  depends_on:
    kafka:
      condition: service_healthy
```

> 포트 주의: auth-service가 `:8081`을 사용하므로 Schema Registry는 호스트에서 `18081:8081` 등으로 매핑하거나, Docker 네트워크 내부에서만 접근하도록 구성한다.

**디버깅 도구** (`docker-compose.yml`):

```yaml
akhq:
  image: tchiotludo/akhq:latest
  ports:
    - "127.0.0.1:9000:8080"
  environment:
    AKHQ_CONFIGURATION: |
      akhq:
        connections:
          portal:
            properties:
              bootstrap.servers: kafka:29092
            schema-registry:
              url: http://schema-registry:8081
```

### Phase 1: event-contracts 구조 전환

**기존 JSON Schema → Avro Schema 변환**:

```
services/event-contracts/
├── schemas/                         # Avro Schema (.avsc)
│   ├── com/portal/universe/event/
│   │   ├── auth/
│   │   │   └── UserSignedUpEvent.avsc
│   │   ├── shopping/
│   │   │   ├── OrderCreatedEvent.avsc
│   │   │   ├── OrderCancelledEvent.avsc
│   │   │   ├── PaymentCompletedEvent.avsc
│   │   │   ├── PaymentFailedEvent.avsc
│   │   │   ├── DeliveryShippedEvent.avsc
│   │   │   ├── InventoryReservedEvent.avsc
│   │   │   ├── InventoryDeductedEvent.avsc
│   │   │   ├── CouponIssuedEvent.avsc
│   │   │   └── TimeDealStartedEvent.avsc
│   │   ├── blog/
│   │   │   ├── PostLikedEvent.avsc
│   │   │   ├── PostCommentedEvent.avsc
│   │   │   ├── CommentRepliedEvent.avsc
│   │   │   └── UserFollowedEvent.avsc
│   │   ├── prism/
│   │   │   ├── PrismTaskCompletedEvent.avsc
│   │   │   └── PrismTaskFailedEvent.avsc
│   │   └── drive/
│   │       ├── FileUploadedEvent.avsc
│   │       ├── FileDeletedEvent.avsc
│   │       └── FolderCreatedEvent.avsc
├── build.gradle                     # gradle-avro-plugin (Java 클래스 생성)
├── package.json                     # TypeScript 타입 생성 스크립트
└── README.md
```

**Avro Schema 예시** (`PrismTaskCompletedEvent.avsc`):

```json
{
  "type": "record",
  "name": "PrismTaskCompletedEvent",
  "namespace": "com.portal.universe.event.prism",
  "fields": [
    { "name": "taskId", "type": "int" },
    { "name": "boardId", "type": "int" },
    { "name": "userId", "type": "string" },
    { "name": "title", "type": "string" },
    { "name": "status", "type": { "type": "enum", "name": "TaskStatus", "symbols": ["TODO", "IN_PROGRESS", "IN_REVIEW", "DONE", "CANCELLED"] } },
    { "name": "agentName", "type": ["null", "string"], "default": null },
    { "name": "executionId", "type": ["null", "int"], "default": null },
    { "name": "timestamp", "type": { "type": "long", "logicalType": "timestamp-millis" } }
  ]
}
```

### Phase 2: Java 서비스 통합

**common-library 업데이트**:

```java
// KafkaAvroProducerConfig.java
@Bean
public ProducerFactory<String, SpecificRecord> avroProducerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    config.put("schema.registry.url", schemaRegistryUrl);
    config.put(ProducerConfig.ACKS_CONFIG, "all");           // ADR-032 D2 유지
    config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    return new DefaultKafkaProducerFactory<>(config);
}
```

**Consumer 업데이트** (notification-service):

```java
// KafkaAvroConsumerConfig.java
config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
config.put("schema.registry.url", schemaRegistryUrl);
config.put("specific.avro.reader", true);  // SpecificRecord로 역직렬화
// ErrorHandlingDeserializer + DLQ 패턴 유지 (ADR-032 D5)
```

**기존 Java record → SpecificRecord 전환**:
- `gradle-avro-plugin`이 `.avsc`에서 Java 클래스를 자동 생성
- 기존 수동 작성된 Java record 클래스 삭제
- Topics 클래스는 그대로 유지 (ADR-032)

### Phase 3: NestJS(prism-service) 통합

**접근 전략**: `@kafkajs/confluent-schema-registry` 사용

```typescript
// kafka.producer.ts
import { SchemaRegistry, SchemaType } from '@kafkajs/confluent-schema-registry';

const registry = new SchemaRegistry({
  host: process.env.SCHEMA_REGISTRY_URL || 'http://localhost:8081',
});

// encode 시 Registry에서 스키마 ID 조회 + Avro 인코딩
const encodedValue = await registry.encode(schemaId, event);
await producer.send({ topic, messages: [{ key, value: encodedValue }] });
```

**TypeScript 타입 관리**:
- `.avsc`에서 TypeScript interface를 생성하는 npm 스크립트 작성
- 생성된 타입은 `event-contracts/generated/typescript/`에 배치
- 완벽한 자동 생성이 어려운 경우 수동 보정 허용 (타입 파일에 `// @generated` 주석)

### Phase 4: Python(chatbot-service) 대비

현재 chatbot-service는 Kafka를 사용하지 않지만, 향후 도입 시:

```python
# fastavro 기반
from confluent_kafka.avro import AvroProducer
from fastavro.schema import load_schema

schema = load_schema('PrismTaskCompletedEvent.avsc')
producer = AvroProducer({
    'bootstrap.servers': 'localhost:9092',
    'schema.registry.url': 'http://localhost:8081',
}, default_value_schema=schema)
```

### 마이그레이션 순서

| 순서 | 대상 | 토픽 수 | 이유 |
|------|------|--------|------|
| 1 | Prism 이벤트 | 2 | 기존 JSON Schema 파일럿에서 전환. 가장 적은 변경 |
| 2 | Drive 이벤트 | 3 | Consumer가 notification-service뿐. 영향 범위 최소 |
| 3 | Auth 이벤트 | 1 | 단일 토픽, 단순 구조 |
| 4 | Blog 이벤트 | 4 | Consumer가 notification-service뿐 |
| 5 | Shopping 이벤트 | 9 | 가장 복잡. Saga/Feign 연동 고려 필요. 마지막에 전환 |

**각 단계 공통 절차**:
1. `.avsc` 스키마 작성 → Registry에 등록
2. Producer를 Avro Serializer로 전환
3. Consumer를 Avro Deserializer로 전환 (ErrorHandlingDeserializer 유지)
4. 기존 JSON Schema 파일 삭제
5. 검증: 메시지 발행 → AKHQ에서 Avro 디코딩 확인 → Consumer 정상 수신 확인

### Schema Registry HA (K8s)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: schema-registry
spec:
  replicas: 2
  # ...
  env:
    - name: SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS
      value: "kafka:29092"
    # 스토리지: Kafka _schemas 토픽 (별도 DB 불필요)
    # 장애 시: 로컬 캐시로 기존 스키마 계속 동작
```

### 호환성 규칙

| 설정 | 값 | 의미 |
|------|-----|------|
| **Global default** | `BACKWARD_TRANSITIVE` | 새 스키마로 기존 모든 버전의 메시지를 읽을 수 있어야 함 |
| **필드 추가** | 반드시 `default` 값 포함 | `{ "name": "newField", "type": ["null", "string"], "default": null }` |
| **필드 삭제** | 해당 필드에 `default`가 있었어야 함 | 기존 Consumer가 없는 필드를 default로 대체 |
| **필드 타입 변경** | 금지 | 새 필드를 추가하고 기존 필드를 deprecated 처리 |

## ADR-038과의 관계

| ADR-038 산출물 | 전환 후 처리 |
|---------------|-------------|
| `event-contracts/schemas/*.schema.json` | `.avsc`로 변환 후 삭제 |
| `scripts/validate-event-contracts.js` | Registry 호환성 검증으로 대체. 삭제 |
| `prism-topics.ts` | 유지 (Topic 상수는 ADR-032 관할) |
| CI JSON Schema 검증 | CI에서 `mvn schema-registry:test-compatibility`로 대체 |

ADR-038의 핵심 가치(이벤트 계약의 명시적 관리)는 계승하되, 도구를 JSON Schema + CI 스크립트에서 Avro + Schema Registry로 전환한다.

## References

- [ADR-032: Kafka Configuration Standardization](./ADR-032-kafka-configuration-standardization.md) — Topic 상수, Producer/Consumer 표준 설정
- [ADR-038: Polyglot Event Contract Management](./ADR-038-polyglot-event-contract-management.md) — JSON Schema + CI 검증 (본 ADR로 대체)
- [ADR-046: MySQL to PostgreSQL Migration](./ADR-046-mysql-to-postgresql-migration.md) — Docker 포트 `127.0.0.1` 바인딩 정책
- [Event-Driven Architecture](../architecture/system/event-driven-architecture.md) — 전체 Kafka 아키텍처 및 토픽 매핑
- Apache Avro Specification: https://avro.apache.org/docs/current/spec.html
- Confluent Schema Registry: https://docs.confluent.io/platform/current/schema-registry/index.html
- Schema Evolution and Compatibility: https://docs.confluent.io/platform/current/schema-registry/fundamentals/schema-evolution.html

---

### 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-18 | 초안 작성 | Laze |
| 2026-02-20 | 전면 재작성: Protobuf 비교 근거, TypeScript 통합 전략, 디버깅 도구, HA, 롤백 전략, 마이그레이션 순서 추가 | Laze |
| 2026-02-21 | Status: Proposed → Accepted. 전체 마이그레이션 완료 (Phase 0~7) | Laze |
