# ADR-032: Kafka Configuration Standardization

**Status**: Accepted
**Date**: 2026-02-10
**Author**: Laze
**Supersedes**: -

## Context

5개 서비스(auth, shopping, blog, prism, notification)에 걸친 Kafka 설정이 일관성 없이 구현되어 있어, 유지보수 비용이 증가하고 설정 drift에 의한 장애 위험이 존재한다. 현재 발견된 문제 5가지:

1. **Topic 이름 중복 정의**: 동일 topic 이름이 Producer Config, Consumer Constants, Publisher 등 최대 3곳에 하드코딩되어 있어 Single Source of Truth 원칙을 위반한다.
   - 예: `shopping.order.created`가 `ShoppingKafkaConfig.java`, `NotificationConstants.java`에 각각 정의

2. **NewTopic Bean 누락**: blog-service는 4개 topic 중 2개(`blog.comment.replied`, `blog.user.followed`)의 NewTopic Bean이 누락되어 auto-create에 의존하고 있다.

3. **Topic 명명 불일치**: auth-service의 `user-signup`이 나머지 15개 topic의 `{domain}.{entity}.{past-participle}` 패턴을 따르지 않는다.

4. **Producer 설정 혼재**: Java Bean 설정과 YAML 설정이 동시에 존재하여 어느 것이 실효 설정인지 불명확하다. auth-service는 KafkaConfig 없이 YAML + Auto-configuration에만 의존한다.

5. **SRP 위반**: shopping-service의 `KafkaConfig.java`가 ProducerFactory(9개 설정) + KafkaTemplate + NewTopic Bean 9개를 단일 클래스에서 관리한다(약 150줄).

## Decision

5개 결정(D1~D5)을 통해 Kafka 설정을 표준화한다. 코드 변경은 별도 작업으로 진행하되, 이 ADR이 표준 기준선 역할을 한다.

### D1: Topic 상수를 events 모듈의 Topics 클래스로 집중

각 도메인의 events 모듈에 `*Topics.java` 상수 클래스를 추가하여 topic 이름의 Single Source of Truth를 확보한다.

```java
// services/shopping-events/.../ShoppingTopics.java
public final class ShoppingTopics {
    public static final String ORDER_CREATED = "shopping.order.created";
    public static final String ORDER_CONFIRMED = "shopping.order.confirmed";
    public static final String ORDER_CANCELLED = "shopping.order.cancelled";
    public static final String PAYMENT_COMPLETED = "shopping.payment.completed";
    public static final String PAYMENT_FAILED = "shopping.payment.failed";
    public static final String INVENTORY_RESERVED = "shopping.inventory.reserved";
    public static final String DELIVERY_SHIPPED = "shopping.delivery.shipped";
    public static final String COUPON_ISSUED = "shopping.coupon.issued";
    public static final String TIMEDEAL_STARTED = "shopping.timedeal.started";

    private ShoppingTopics() {}
}
```

**대상 모듈 및 topic 수**:

| 모듈 | 클래스 | Topic 수 |
|------|--------|----------|
| auth-events | `AuthTopics` | 1 |
| shopping-events | `ShoppingTopics` | 9 |
| blog-events | `BlogTopics` | 4 |
| prism-events | `PrismTopics` | 2 |

**NestJS(prism-service) 제약 해소**: prism-service에 `prism-topics.ts` 상수 파일을 도입하여 하드코딩을 제거했다. `services/event-contracts/schemas/`의 JSON Schema가 SSOT이며, `scripts/validate-event-contracts.js`가 Java/TypeScript 양측의 topic 상수와 이벤트 필드 일치를 CI에서 검증한다. (ADR-038 참조)

### D2: Java Bean 유지 + 표준 설정값 명시

ProducerFactory Java Bean을 제거하지 않고, ADR에서 표준 설정값을 명시하여 서비스 간 drift를 방지한다.

**Producer 표준 설정값**:

| 설정 | 표준값 | 근거 |
|------|--------|------|
| `acks` | `all` | 모든 replica 동기화 후 응답, 메시지 유실 방지 |
| `retries` | `3` | 일시적 장애 대응 |
| `enable.idempotence` | `true` | 중복 발행 방지 |
| `key.serializer` | `StringSerializer` | 키는 aggregate ID (문자열) |
| `value.serializer` | `JsonSerializer` | 이벤트 객체 JSON 직렬화 |

**YAML vs Java Bean 역할 분리**:
- **YAML**: 환경별로 달라지는 값만 담당 (`bootstrap-servers`, `group-id` 등)
- **Java Bean**: 불변 설정값 담당 (serializer, acks, retries, idempotence 등)
- YAML에 Java Bean과 중복되는 producer 설정이 있으면 제거 (dead code 정리)

### D3: KafkaConfig → 2-Class 분리

단일 `KafkaConfig.java`를 변경 이유가 다른 두 클래스로 분리한다.

| 클래스 | 책임 | 변경 빈도 |
|--------|------|----------|
| `KafkaProducerConfig` | ProducerFactory + KafkaTemplate Bean | 드묾 (직렬화/인증 변경 시) |
| `KafkaTopicConfig` | NewTopic Bean 정의 (Topics 상수 참조) | 잦음 (topic 추가/삭제 시) |

**적용 대상**: shopping-service, blog-service, auth-service (KafkaConfig 신규 생성)
**제외**: notification-service (Consumer 전용, `KafkaConsumerConfig` 단독 유지)

### D4: Topic 명명 `{domain}.{entity}.{past-participle}` 통일

모든 topic 이름을 `{domain}.{entity}.{past-participle}` 패턴으로 통일한다.

**변경 대상**:

| 현재 | 변경 후 | 서비스 |
|------|---------|--------|
| `user-signup` | `auth.user.signed-up` | auth-service |

나머지 15개 topic은 이미 패턴을 준수한다.

**마이그레이션 절차**:

현재 로컬 개발 환경이며 기존 `user-signup` topic에 미소비 메시지나 보존해야 할 데이터가 없으므로, Producer와 Consumer를 동시에 `auth.user.signed-up`으로 변경하는 직접 전환으로 충분하다.

```
Producer(auth-service) + Consumer(notification-service) + Topics 상수를 한 커밋에서 일괄 변경
```

> **참고**: 운영 환경에서 미소비 메시지가 존재하는 경우, Consumer 이중 구독 → Producer 전환 → Old topic 제거의 3-phase 무중단 마이그레이션이 필요하다.

### D5: Consumer 최소 기준 명문화

notification-service의 현재 설정을 기준으로 Consumer 표준을 정의한다.

**필수 (MUST)**:

| 항목 | 표준값 | 근거 |
|------|--------|------|
| Key Deserializer | `ErrorHandlingDeserializer` wrapping `StringDeserializer` | 역직렬화 실패 시 전체 Consumer 중단 방지 |
| Value Deserializer | `ErrorHandlingDeserializer` wrapping `JsonDeserializer` | 동일 |
| `TRUSTED_PACKAGES` | `com.portal.universe.*` | 허용된 패키지만 역직렬화 (보안) |
| `enable-auto-commit` | `false` | 수동 커밋으로 at-least-once 보장 |
| AckMode | `RECORD` | 레코드 단위 커밋 |
| DLQ | `{topic}.DLT`에 DeadLetterPublishingRecoverer | 영구 실패 메시지 격리 |
| Retry | 최대 3회, FixedBackOff 1000ms | 일시적 장애 대응 |
| 재시도 제외 예외 | `IllegalArgumentException`, `NullPointerException` | 재시도 무의미한 에러 즉시 DLQ 전송 |

**권장 (SHOULD)**:

| 항목 | 표준값 | 근거 |
|------|--------|------|
| `group-id` | `{service-name}-group` | 서비스 단위 격리 |
| `auto-offset-reset` | `earliest` | 서비스 재시작 시 미처리 메시지 소비 |
| Retry listener 로깅 | 재시도 시작/종료, DLQ 전송 시 로그 | 운영 가시성 확보 |

## Alternatives

### D1: Topic 상수 위치

| 대안 | 장점 | 단점 |
|------|------|------|
| **① events 모듈에 Topics 클래스 (채택)** | Producer/Consumer 모두 같은 상수 참조, 이벤트와 같은 모듈에 위치 | events 모듈에 비즈니스 로직 외 코드 추가 |
| ② 공통 kafka-common 모듈 | 전체 topic을 한 곳에서 관리 | 도메인 간 결합 유발, 어떤 서비스든 전체 topic 목록에 의존 |
| ③ YAML 외부화 (환경변수) | 코드 변경 없이 topic 이름 변경 가능 | topic 이름은 계약이므로 런타임 변경 가능하면 오히려 위험 |
| ④ 현행 유지 (각 서비스 하드코딩) | 변경 비용 없음 | 중복 정의에 의한 불일치 위험 지속 |

### D2: Producer 설정 방식

| 대안 | 장점 | 단점 |
|------|------|------|
| **① Java Bean 유지 + ADR 기준선 (채택)** | 커스터마이징 자유도 유지, 설정 변경 시 컴파일 타임 검증 | 서비스 간 drift 가능 (ADR로 완화) |
| ② Spring Boot Auto-configuration만 사용 (YAML) | 코드 최소화, 환경별 오버라이드 용이 | ProducerFactory 커스터마이징 불가 (interceptor, partitioner 등) |
| ③ Shared Config 라이브러리 | 완전한 설정 통일 보장 | 라이브러리 버전 관리 부담, 서비스별 예외 처리 복잡 |

### D3: Config 클래스 분리 단위

| 대안 | 장점 | 단점 |
|------|------|------|
| **① 2-class (ProducerConfig + TopicConfig) (채택)** | SRP 준수, topic 추가 시 TopicConfig만 변경 | 파일 수 증가 (1 → 2) |
| ② 3-class (Producer + Topic + Template) | 더 세밀한 분리 | 과도한 분리, KafkaTemplate은 ProducerFactory에 종속 |
| ③ 단일 클래스 유지 | 파일 수 적음, 간단 | SRP 위반, 변경 영향 범위 큼 |

### D4: Topic 명명 규칙

| 대안 | 장점 | 단점 |
|------|------|------|
| **① `{domain}.{entity}.{past-participle}` (채택)** | 기존 15/16 topic이 이미 준수, 이벤트의 도메인 소속과 과거형 행위가 명확 | `user-signup` 1건 마이그레이션 필요 |
| ② `{domain}-{entity}-{action}` (kebab-case) | 시각적 일관성 | 기존 15개 topic 전체 마이그레이션 필요 |
| ③ 현행 유지 (혼합) | 변경 비용 없음 | 명명 불일치로 인한 혼동 지속 |

### D5: Consumer 설정 적용 방식

| 대안 | 장점 | 단점 |
|------|------|------|
| **① ADR 명문화 + 서비스별 구현 (채택)** | 유연성 유지, 서비스별 특수 요구 반영 가능 | 이행 여부를 수동 검증해야 함 |
| ② Shared Consumer Config 라이브러리 | 코드 레벨 강제 | 라이브러리 의존성 관리, 예외 처리 복잡 |

## Rationale

- **D1**: events 모듈은 Publisher와 Subscriber 양쪽이 이미 의존하는 공유 모듈이므로, topic 상수를 추가하면 별도 의존성 없이 SSOT를 달성할 수 있다
- **D2**: 현재 shopping-service의 idempotent producer, blog-service의 async callback 등 서비스별 커스터마이징이 존재하므로, Auto-configuration으로의 전환은 기능 손실을 초래한다
- **D3**: shopping-service KafkaConfig는 9개 NewTopic + ProducerFactory를 하나에 담고 있어, topic 추가 시 producer 설정까지 변경 이력에 포함되는 문제가 있다
- **D4**: `user-signup` 1건만 변경하면 전체 통일이 가능하므로, 마이그레이션 비용 대비 일관성 획득 효과가 크다
- **D5**: notification-service의 현재 설정이 이미 at-least-once, DLQ, ErrorHandlingDeserializer를 포함하고 있어 별도 설계 없이 기준선으로 채택 가능하다

## Trade-offs

**장점**:
- Topic 이름 변경 시 컴파일 에러로 불일치 사전 감지 (D1)
- 새 서비스 추가 시 표준 설정을 즉시 참조할 수 있는 기준선 확보 (D2, D5)
- Config 변경의 blast radius 축소 (D3)
- Topic 이름만으로 도메인/엔티티/행위를 즉시 파악 가능 (D4)

**단점 및 완화**:
- NestJS(prism-service)는 Java Topics 상수를 참조할 수 없어 하드코딩 유지 → (완화: `PrismTopics.java`를 문서적 SSOT로 삼고, prism-service 코드에 참조 주석 명시. CI에서 문자열 비교 lint 추가 가능)
- `user-signup` → `auth.user.signed-up` topic 이름 변경 필요 → (현재 로컬 환경에 미소비 메시지 없으므로 직접 전환으로 충분. 운영 환경 적용 시 3-phase 무중단 마이그레이션 고려)
- ADR 기준선은 강제력이 없어 drift 재발 가능 → (완화: PR 리뷰 시 체크리스트에 Kafka 설정 표준 항목 추가)

## Implementation

구현은 이 ADR 승인 후 별도 작업으로 진행한다.

### 영향받는 파일

**D1 - Topics 상수 클래스 추가**:
- `services/auth-events/.../AuthTopics.java` (신규)
- `services/shopping-events/.../ShoppingTopics.java` (신규)
- `services/blog-events/.../BlogTopics.java` (신규)
- `services/prism-events/.../PrismTopics.java` (신규)
- `services/notification-service/.../NotificationConstants.java` (topic 상수 제거, *Topics 참조로 변경)
- `services/auth-service/.../UserSignupEventHandler.java` (하드코딩 → `AuthTopics.TOPIC_USER_SIGNED_UP` 참조)
- `services/shopping-service/.../KafkaConfig.java` (하드코딩 → `ShoppingTopics.*` 참조)
- `services/blog-service/.../BlogEventPublisher.java` (하드코딩 → `BlogTopics.*` 참조)

**D2 - YAML dead code 정리**:
- `services/shopping-service/src/main/resources/application-local.yml` (중복 producer 설정 제거)
- `services/blog-service/src/main/resources/application-local.yml` (중복 producer 설정 제거)
- `services/auth-service/src/main/resources/application-local.yml` (YAML producer 설정 유지 → Java Bean 생성 후 YAML 정리)

**D3 - Config 클래스 분리**:
- `services/shopping-service/.../config/KafkaConfig.java` → `KafkaProducerConfig.java` + `KafkaTopicConfig.java`
- `services/blog-service/.../config/KafkaConfig.java` → `KafkaProducerConfig.java` + `KafkaTopicConfig.java`
- `services/auth-service/.../config/KafkaProducerConfig.java` (신규)
- `services/auth-service/.../config/KafkaTopicConfig.java` (신규)

**D4 - Topic 이름 마이그레이션**:
- `services/auth-events/.../AuthTopics.java` (topic명 `auth.user.signed-up` 정의)
- `services/auth-service/.../UserSignupEventHandler.java` (발행 topic 변경)
- `services/notification-service/.../consumer/*Consumer.java` (이중 구독 → 단일 구독)

**D5 - Consumer 표준 적용**:
- 현재 notification-service가 이미 표준 준수 → 변경 불필요
- 향후 새 Consumer 서비스 추가 시 이 ADR의 D5 섹션을 기준으로 구현

### 구현 순서 (권장)

```
1. D1: Topics 상수 클래스 추가 (events 모듈)
2. D3: Config 클래스 분리 (shopping, blog, auth)
3. D1 적용: 하드코딩 → Topics 상수 참조로 변경
4. D2: YAML dead code 정리
5. D4: user-signup → auth.user.signed-up 마이그레이션
```

## References

- [Event-Driven Architecture](../architecture/system/event-driven-architecture.md) - 현재 Kafka 아키텍처 및 17개 topic 매핑
- [Kafka Events 구현 가이드](../../.claude/skills/kafka-events.md) - Producer/Consumer 구현 패턴

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-10 | 초안 작성 | Laze |
| 2026-02-10 | Status → Accepted, D1~D4 구현 완료 | Laze |
| 2026-02-13 | NestJS topic 상수화 완료 (prism-topics.ts), ADR-038 연동 반영 | Laze |
