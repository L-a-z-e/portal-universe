# Avro Schema Guide: 신규 이벤트 추가

새로운 Kafka 이벤트를 추가할 때의 단계별 가이드.

| 항목 | 내용 |
|------|------|
| **범위** | 전체 서비스 |
| **관련 ADR** | [ADR-047](../adr/ADR-047-avro-schema-registry-adoption.md), [ADR-032](../adr/ADR-032-kafka-configuration-standardization.md) |
| **작성자** | Laze |

---

## 1. Avro Schema 작성 (`.avsc`)

`services/event-contracts/schemas/com/portal/universe/event/{domain}/` 에 파일 생성.

```json
{
  "type": "record",
  "name": "NewDomainEvent",
  "namespace": "com.portal.universe.event.{domain}",
  "fields": [
    { "name": "id", "type": "int" },
    { "name": "userId", "type": "string" },
    { "name": "timestamp", "type": { "type": "long", "logicalType": "timestamp-millis" } }
  ]
}
```

### 타입 매핑 참고

| Java 타입 | Avro 타입 | 비고 |
|-----------|-----------|------|
| `int` / `Integer` | `"int"` | |
| `long` / `Long` | `"long"` | |
| `String` | `"string"` | |
| `boolean` / `Boolean` | `"boolean"` | |
| `Instant` | `{"type": "long", "logicalType": "timestamp-millis"}` | epoch millis |
| `@Nullable String` | `["null", "string"]` | default: null |
| `enum` | `{"type": "enum", "name": "...", "symbols": [...]}` | 별도 .avsc 분리 권장 |
| `List<T>` | `{"type": "array", "items": "T"}` | |
| Nested object | `{"type": "record", ...}` | 별도 .avsc 분리 권장 |

### 호환성 규칙 (BACKWARD_TRANSITIVE)

| 변경 유형 | 허용 여부 | 조건 |
|-----------|----------|------|
| 필드 추가 | O | 반드시 `default` 값 포함 |
| 필드 삭제 | O | 해당 필드에 `default`가 이미 있어야 함 |
| 타입 변경 | X | 새 필드 추가 + 기존 필드 deprecated |
| enum 심볼 추가 | O | Consumer 코드에서 unknown 값 처리 필요 |
| 필드명 변경 | X | 새 필드 추가 + 기존 필드 deprecated |

---

## 2. Java 클래스 생성 확인

```bash
./gradlew :event-contracts:generateAvroJava
```

생성 위치: `services/event-contracts/build/generated-main-avro-java/`

생성된 클래스는 `SpecificRecordBase`를 상속하며 Builder 패턴을 제공합니다:

```java
var event = NewDomainEvent.newBuilder()
    .setId(1)
    .setUserId("user-123")
    .setTimestamp(Instant.now())
    .build();
```

---

## 3. Topic 상수 추가

`services/event-contracts/src/main/java/com/portal/universe/event/{domain}/{Domain}Topics.java`:

```java
public final class DomainTopics {
    public static final String NEW_EVENT = "{domain}.{entity}.{past-participle}";
    private DomainTopics() {}
}
```

Topic 명명 규칙: `{domain}.{entity}.{past-participle}` (ADR-032)

---

## 4. Producer 구현

### Java (Spring)

```java
@RequiredArgsConstructor
public class DomainEventPublisher {
    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    public void publish(NewDomainEvent event) {
        avroKafkaTemplate.send(DomainTopics.NEW_EVENT, event.getId().toString(), event);
    }
}
```

`avroKafkaTemplate`은 `common-library`의 `AvroProducerConfig`에서 자동 설정됩니다.

### TypeScript (NestJS)

```typescript
const schemaId = await this.registry.getLatestSchemaId('{topic-name}-value');
const encodedValue = await this.registry.encode(schemaId, event);
await this.producer.send({
  topic: '{topic-name}',
  messages: [{ key, value: encodedValue }]
});
```

---

## 5. Consumer 구현

### Java (Spring)

```java
@KafkaListener(
    topics = DomainTopics.NEW_EVENT,
    groupId = "{service}-group",
    containerFactory = "avroKafkaListenerContainerFactory"
)
public void handleNewEvent(NewDomainEvent event) {
    // event.getId(), event.getUserId() 등 getter 사용
}
```

`avroKafkaListenerContainerFactory`는 `common-library`의 `AvroConsumerConfig`에서 자동 설정됩니다.

---

## 6. 서비스 설정 확인

Producer/Consumer 서비스의 `application-{profile}.yml`에 Schema Registry URL이 설정되어 있는지 확인:

```yaml
spring:
  kafka:
    properties:
      schema.registry.url: http://localhost:18081  # local
      # schema.registry.url: http://schema-registry:8081  # docker/k8s
```

---

## 7. 검증

1. `./gradlew :event-contracts:build` — Avro 스키마 컴파일 성공
2. `./gradlew :{service}:compileJava` — 의존 서비스 컴파일 성공
3. Docker Compose로 전체 스택 실행 후:
   - Producer에서 이벤트 발행
   - AKHQ (`http://localhost:9000`)에서 Avro 메시지 확인
   - Consumer 로그에서 정상 수신 확인

---

## 체크리스트

- [ ] `.avsc` 스키마 파일 작성
- [ ] `generateAvroJava` 빌드 성공
- [ ] `*Topics.java`에 Topic 상수 추가
- [ ] Producer 서비스에서 `avroKafkaTemplate` 사용
- [ ] Consumer 서비스에서 `avroKafkaListenerContainerFactory` 사용
- [ ] 3개 profile (local/docker/k8s) `schema.registry.url` 설정 확인
- [ ] AKHQ에서 Avro 메시지 확인
- [ ] 단위 테스트 작성/수정

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-21 | 초안 작성 | Laze |
