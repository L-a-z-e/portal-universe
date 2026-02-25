# Event-Driven Architecture: 멀티 메시징 시스템

## 개요
Portal Universe의 이벤트 기반 아키텍처는 **3개의 메시징 시스템**을 목적에 따라 사용합니다. Apache Kafka가 서비스 간 도메인 이벤트의 핵심 채널이며, AWS SQS가 이메일 등 비동기 작업 큐, Amazon EventBridge가 조건부 이벤트 라우팅을 담당합니다.

| 항목 | 내용 |
|------|------|
| **범위** | System |
| **주요 기술** | Apache Kafka (KRaft Mode), AWS SQS/SNS, Amazon EventBridge, CloudWatch |
| **배포 환경** | Docker Compose (LocalStack), Kubernetes |
| **관련 서비스** | auth-service, shopping-service, shopping-seller-service, shopping-settlement-service, blog-service, prism-service, drive-service, notification-service |

### 메시징 시스템 역할 분담

| 시스템 | 역할 | 특성 | 사용 서비스 |
|--------|------|------|-------------|
| **Kafka** | 도메인 이벤트 스트림 | 고처리량, 순서 보장, 이벤트 리플레이 | 전체 서비스 |
| **SQS** | 비동기 작업 큐 | 신뢰성 높은 Point-to-Point, DLQ 내장 | notification-service |
| **EventBridge** | 조건부 이벤트 라우팅 | 규칙 기반 팬아웃, 콘텐츠 필터링 | shopping-service |
| **CloudWatch** | 메트릭 + 알람 | 커스텀 메트릭, 임계값 알람 → SNS → SQS | shopping-service |

---

## 아키텍처 다이어그램

```mermaid
graph LR
    A[auth-service<br/>Java/Spring] -->|auth.*| K[Kafka<br/>KRaft Mode]
    S[shopping-service<br/>Java/Spring] -->|shopping.*| K
    S -->|@Async| EB[EventBridge<br/>portal-universe]
    S -->|PutMetricData| CW[CloudWatch<br/>Custom Metrics]
    SS[shopping-seller-service<br/>Java/Spring] -->|shopping.*| K
    B[blog-service<br/>Java/Spring] -->|blog.*| K
    D[drive-service<br/>Java/Spring] -->|drive.*| K
    P[prism-service<br/>NestJS] -->|prism.*| K
    K -->|notification-group| N[notification-service<br/>Java/Spring]
    K -->|seller-group| SS
    K -->|settlement-group| ST[shopping-settlement-service<br/>Java/Spring]
    K --- SR[Schema Registry<br/>Avro 스키마 관리]
    N -->|SendMessage| SQS[SQS<br/>email-queue]
    SQS -->|ReceiveMessage| N
    EB -->|Rule Matching| SQS2[SQS / Lambda / etc]
    CW -->|Alarm| SNS[SNS<br/>saga-alerts]
    SNS --> SQS3[SQS<br/>alert-queue]

    style K fill:#ff9900,stroke:#333,stroke-width:2px
    style N fill:#4CAF50,stroke:#333,stroke-width:2px
    style SR fill:#9C27B0,stroke:#333,stroke-width:2px
    style EB fill:#E91E63,stroke:#333,stroke-width:2px
    style SQS fill:#2196F3,stroke:#333,stroke-width:2px
    style CW fill:#FF5722,stroke:#333,stroke-width:2px
```

---

## Kafka 인프라 개요

### KRaft Mode
- ZooKeeper를 사용하지 않는 최신 Kafka 운영 모드
- Controller 역할을 Kafka 브로커가 직접 수행

### Consumer Configuration
- **Consumer Group**: `notification-group`
- **auto-offset-reset**: `earliest` (처음부터 소비)
- **enable-auto-commit**: `false` (수동 커밋으로 at-least-once 보장)

### Schema Registry
- **역할**: Avro 스키마 중앙 저장소 + 호환성 검증기
- **스토리지**: Kafka `_schemas` 토픽 (별도 DB 불필요)
- **호환성**: `BACKWARD_TRANSITIVE` (새 스키마로 기존 모든 버전 메시지 읽기 가능)
- **포트**: 8081 (Docker 내부), 18081 (호스트 매핑)
- **디버깅**: AKHQ (`:9000`) — Avro 메시지 자동 디시리얼라이제이션

### Topic 기본 설정
- **파티션 수**: 3
- **Replication Factor**: 1 (dev 환경 기준)
- **Serialization**: StringSerializer (key), KafkaAvroSerializer (value) — Avro Wire Format
- **acks**: `all` (모든 replica 동기화 후 응답)
- **retries**: 3회

---

## 전체 토픽 매핑 (16개)

### Topic 명명 규칙

모든 topic은 `{domain}.{entity}.{past-participle}` 패턴을 따른다. Topic 이름의 Single Source of Truth는 각 도메인의 events 모듈에 있는 `*Topics.java` 상수 클래스이다 (ADR-032).

| 토픽 | Publisher | Subscriber | Event Class | 용도 | 상태 |
|------|-----------|------------|-------------|------|------|
| `auth.user.signed-up` | auth-service | notification-service | UserSignedUpEvent | 회원가입 환영 알림 | ✅ Active |
| `shopping.order.created` | shopping-service | notification-service | OrderCreatedEvent | 주문 생성 알림 | ✅ Active |
| `shopping.order.confirmed` | shopping-service | - | OrderConfirmedEvent | (미사용) | ⚠️ Unused |
| `shopping.order.cancelled` | shopping-service | notification-service | OrderCancelledEvent | 주문 취소 알림 | ✅ Active |
| `shopping.payment.completed` | shopping-service | notification-service | PaymentCompletedEvent | 결제 완료 알림 | ✅ Active |
| `shopping.payment.failed` | shopping-service | notification-service | PaymentFailedEvent | 결제 실패 알림 | ✅ Active |
| `shopping.inventory.reserved` | shopping-service | - | InventoryReservedEvent | (미사용) | ⚠️ Unused |
| `shopping.delivery.shipped` | shopping-service | notification-service | DeliveryShippedEvent | 배송 시작 알림 | ✅ Active |
| `shopping.coupon.issued` | shopping-service | notification-service | CouponIssuedEvent | 쿠폰 발급 알림 | ✅ Active |
| `shopping.timedeal.started` | shopping-service | - | TimeDealStartedEvent | 브로드캐스트 (미구현) | ⚠️ Unused |
| `blog.post.liked` | blog-service | notification-service | PostLikedEvent | 좋아요 알림 | ✅ Active |
| `blog.post.commented` | blog-service | notification-service | CommentCreatedEvent | 댓글 알림 | ✅ Active |
| `blog.comment.replied` | blog-service | notification-service | CommentRepliedEvent | 답글 알림 | ✅ Active |
| `blog.user.followed` | blog-service | notification-service | UserFollowedEvent | 팔로우 알림 | ✅ Active |
| `prism.task.completed` | prism-service | notification-service | PrismTaskCompletedEvent | AI 작업 완료 알림 | ✅ Active |
| `prism.task.failed` | prism-service | notification-service | PrismTaskFailedEvent | AI 작업 실패 알림 | ✅ Active |

**주요 특징**:
- 현재 notification-service가 **유일한 Consumer**로 설계됨
- 미사용 토픽 3개는 향후 확장 계획(Saga 패턴, 브로드캐스트 알림 등)을 위해 예약됨
- 토픽 명명 규칙: `{domain}.{entity}.{past-participle}` (예: `shopping.order.created`) - ADR-032

---

## 핵심 컴포넌트

### 1. Event Publishers (4개 서비스)

#### auth-service (Java/Spring)
**역할**: 사용자 인증 도메인 이벤트 발행

**발행 패턴** (Avro SpecificRecord):
```java
@Component
@RequiredArgsConstructor
public class UserSignupEventHandler {
    private final KafkaTemplate<String, SpecificRecord> avroKafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserSignup(UserSignedUpApplicationEvent event) {
        var avroEvent = UserSignedUpEvent.newBuilder()
            .setUserId(event.userId())
            .setEmail(event.email())
            .setTimestamp(Instant.now())
            .build();
        avroKafkaTemplate.send(AuthTopics.USER_SIGNED_UP, avroEvent);
    }
}
```

**주요 특징**:
- `@TransactionalEventListener(AFTER_COMMIT)` 사용으로 트랜잭션 완료 후 발행
- DB 커밋 실패 시 이벤트 미발행으로 일관성 보장
- Avro SpecificRecord Builder 패턴으로 타입 안전한 이벤트 생성

#### shopping-service (Java/Spring)
**역할**: 쇼핑 도메인 이벤트 발행 (10개 토픽)

**발행 패턴**:
```java
CompletableFuture<SendResult<String, Object>> future =
    kafkaTemplate.send(topic, orderId, event);

future.whenComplete((result, ex) -> {
    if (ex != null) {
        log.error("Failed to publish event", ex);
    }
});
```

**주요 특징**:
- `enable-idempotence: true` (중복 발행 방지)
- CompletableFuture 비동기 처리
- 주문, 결제, 배송, 쿠폰, 타임딜 등 다양한 도메인 이벤트

#### blog-service (Java/Spring)
**역할**: 블로그 도메인 이벤트 발행 (4개 토픽)

**이벤트 종류**:
- 게시글 좋아요 (`blog.post.liked`)
- 댓글 작성 (`blog.post.commented`)
- 댓글 답글 (`blog.comment.replied`)
- 사용자 팔로우 (`blog.user.followed`)

#### prism-service (NestJS)
**역할**: AI 작업 결과 이벤트 발행

**발행 패턴** (KafkaJS + Avro):
```typescript
const encodedValue = await this.registry.encode(schemaId, event);
await this.producer.send({
  topic: PrismTopics.TASK_COMPLETED,
  messages: [{ key: taskId, value: encodedValue }]
});
```

**주요 특징**:
- 유일한 비-Spring 서비스
- `@kafkajs/confluent-schema-registry`로 Avro Wire Format 발행
- Schema Registry에서 스키마 ID 조회 + Avro 바이너리 인코딩

### 2. Event Subscriber (notification-service)

**역할**: 모든 도메인 이벤트를 소비하여 사용자 알림 생성

**구독 패턴** (Avro SpecificRecord):
```java
@KafkaListener(
    topics = "shopping.order.created",
    groupId = "notification-group",
    containerFactory = "avroKafkaListenerContainerFactory"
)
public void handleOrderCreated(OrderCreatedEvent event) {
    notificationService.sendNotification(
        event.getUserId().toString(),
        "주문이 생성되었습니다: " + event.getOrderId(),
        NotificationType.ORDER
    );
}
```

**주요 책임**:
- 14개 @KafkaListener 메서드 운영
- 이벤트 타입별 알림 메시지 생성
- 알림 채널 라우팅 (Push, Email, SMS)
- 실패 시 DLQ 처리

**기술 스택**:
- Spring Kafka
- MongoDB (알림 이력 저장)
- FCM (Push 알림)

---

## 이벤트 스키마 거버넌스

### Avro Schema + Schema Registry (ADR-047)

모든 이벤트는 `event-contracts` 모듈에서 Avro Schema(`.avsc`)로 정의하고, Schema Registry가 런타임 호환성을 검증합니다.

```
services/event-contracts/
├── schemas/com/portal/universe/event/
│   ├── auth/       UserSignedUpEvent.avsc, RoleAssignedEvent.avsc
│   ├── blog/       PostLikedEvent.avsc, CommentCreatedEvent.avsc, ...
│   ├── drive/      FileUploadedEvent.avsc, FileDeletedEvent.avsc, ...
│   ├── prism/      PrismTaskCompletedEvent.avsc, PrismTaskFailedEvent.avsc, TaskStatus.avsc
│   └── shopping/   OrderCreatedEvent.avsc, PaymentCompletedEvent.avsc, ...
├── src/main/java/com/portal/universe/event/
│   ├── auth/AuthTopics.java
│   ├── blog/BlogTopics.java
│   ├── drive/DriveTopics.java
│   ├── prism/PrismTopics.java
│   └── shopping/ShoppingTopics.java
└── build.gradle    (gradle-avro-plugin → Java SpecificRecord 자동 생성)
```

**설계 원칙**:
- `.avsc`가 이벤트 구조의 Single Source of Truth
- Java: `gradle-avro-plugin`이 SpecificRecord 자동 생성 (수동 동기화 불필요)
- TypeScript: `@kafkajs/confluent-schema-registry`로 런타임 encode/decode
- Schema Registry가 `BACKWARD_TRANSITIVE` 호환성을 런타임에 자동 적용
- Topics 상수는 `*Topics.java`에서 관리 (ADR-032)

**의존성**:
```groovy
// notification-service/build.gradle (Consumer 예시)
dependencies {
    implementation project(':event-contracts')
}
```

---

## 데이터 플로우

### 주문 생성 플로우 (Order Created)

```
1. Client → shopping-service (POST /api/v1/orders)
2. shopping-service → MySQL (주문 저장, @Transactional)
3. shopping-service → Kafka ("shopping.order.created", OrderCreatedEvent)
4. Kafka → notification-service (@KafkaListener)
5. notification-service → MongoDB (알림 저장)
6. notification-service → FCM (Push 발송)
```

### 결제 완료 플로우 (Payment Completed)

```
1. shopping-service → 결제 게이트웨이 (외부 API 호출)
2. 결제 게이트웨이 → shopping-service (Webhook)
3. shopping-service → MySQL (결제 상태 업데이트)
4. shopping-service → Kafka ("shopping.payment.completed", PaymentCompletedEvent)
5. Kafka → notification-service
6. notification-service → 사용자 ("결제가 완료되었습니다" 알림)
```

### AI 작업 완료 플로우 (Prism Task Completed)

```
1. Client → prism-service (POST /api/prism/tasks)
2. prism-service → Redis Queue (작업 큐잉)
3. Worker → AI Model (작업 처리)
4. prism-service → MongoDB (작업 결과 저장)
5. prism-service → Kafka ("prism.task.completed", PrismTaskCompletedEvent)
6. Kafka → notification-service
7. notification-service → 사용자 ("AI 작업이 완료되었습니다" 알림)
```

---

## 에러 처리 및 재시도

### 재시도 정책

```yaml
# notification-service application.yml
spring:
  kafka:
    consumer:
      enable-auto-commit: false  # 수동 커밋
    listener:
      ack-mode: manual
```

**재시도 설정**:
- **재시도 간격**: 1초
- **최대 재시도**: 3회
- **백오프 전략**: Fixed backoff (1000ms)

**재시도 제외 예외**:
- `IllegalArgumentException` (잘못된 이벤트 형식)
- `NullPointerException` (필수 필드 누락)
- → 즉시 DLQ 전송

### Dead Letter Queue (DLQ)

**DLQ 토픽 명명**: `{원본토픽}.DLT`

예시:
- `shopping.order.created` → `shopping.order.created.DLT`
- `blog.post.liked` → `blog.post.liked.DLT`

**DLQ 처리 프로세스**:
1. 3회 재시도 실패 시 자동 DLQ 전송
2. 운영팀에게 Slack 알림
3. 수동 검토 후 재처리 또는 폐기

---

## 기술적 결정

### 선택한 패턴
- **Event Sourcing (부분 적용)**: 주문, 결제 등 중요 이벤트는 이벤트 로그 보관
- **At-Least-Once Delivery**: `enable-auto-commit: false`로 최소 1회 전달 보장
- **Idempotent Consumer**: notification-service는 중복 알림 방지 로직 포함

### 제약사항
- **현재 구조의 제약**: notification-service가 유일한 Consumer
  - ✅ 장점: 단순한 구조, 운영 부담 최소화
  - ⚠️ 단점: 다른 서비스가 이벤트를 구독할 수 없음 (향후 확장 필요)
- **미사용 토픽**: `order.confirmed`, `inventory.reserved`, `timedeal.started`
  - Saga 패턴, 재고 관리, 브로드캐스트 알림 등 향후 구현 예정
- **성능**: 파티션 3개 기준 최대 3개 Consumer 인스턴스까지 확장 가능
- **보안**: Kafka는 내부 네트워크에서만 접근 가능 (TLS 미적용)

---

## 배포 및 확장

### 배포 구성

**Kafka (KRaft Mode)**:
- **환경**: Dev (Docker Compose), Prod (Kubernetes StatefulSet)
- **리소스**: CPU 1 Core, Memory 2GB
- **복제본**: 1 (dev), 3 (prod)
- **포트**: 9092 (Broker), 9093 (Controller)

**notification-service**:
- **환경**: Kubernetes Deployment
- **리소스**: CPU 500m, Memory 1GB
- **복제본**: 2 (고가용성)
- **Consumer 인스턴스**: 파티션 수만큼 자동 분산 (최대 3개)

### 확장 전략

#### 수평 확장
- **Kafka 확장**: 브로커 추가 + 파티션 리밸런싱
- **Consumer 확장**: notification-service Pod 증설 (최대 파티션 수까지)

#### 병목 지점 및 대응
| 병목 | 증상 | 대응 |
|------|------|------|
| Kafka 디스크 I/O | lag 증가 | SSD 사용, 파티션 증가 |
| notification-service 처리 속도 | Consumer lag 누적 | Pod 증설 (최대 3개) |
| FCM 호출 지연 | 알림 발송 지연 | Async 처리, 배치 발송 |

### 모니터링 지표
- **Producer**: `kafka_producer_record_send_total`, `kafka_producer_record_error_total`
- **Consumer**: `kafka_consumer_records_consumed_total`, `kafka_consumer_records_lag_max`
- **Topic**: 각 토픽별 메시지 수, 처리율

---

## AWS SQS 통합 (notification-service)

Kafka 이벤트를 수신한 notification-service가 이메일 발송과 같은 비동기 작업을 AWS SQS 큐를 통해 처리합니다. Kafka는 도메인 이벤트 전달, SQS는 실제 발송 작업의 안정적 실행을 담당합니다.

### 아키텍처

```
Kafka → NotificationConsumer → SQS(email-notification-queue) → EmailQueueConsumer → 이메일 발송
                                                                     ↓ (실패 시)
                                                           DLQ(email-notification-dlq)
```

### SQS 큐 구성

| 큐 | ARN | 용도 | 설정 |
|-----|-----|------|------|
| `email-notification-queue` | `arn:aws:sqs:ap-northeast-2:000000000000:email-notification-queue` | 이메일 발송 작업 | VisibilityTimeout: 30s, MaxReceiveCount: 3 |
| `email-notification-dlq` | `arn:aws:sqs:ap-northeast-2:000000000000:email-notification-dlq` | 발송 실패 메시지 | 수동 검토 후 재처리 |

### 처리 흐름

1. `NotificationConsumer`가 Kafka 이벤트 수신 → 알림 생성
2. 이메일 발송 필요 시 `SqsMessageSender.sendEmailMessage()` 호출
3. `EmailQueueConsumer`가 `@SqsListener`로 SQS 메시지 수신
4. 이메일 발송 실패 시 SQS 자동 재시도 (최대 3회)
5. 3회 실패 시 RedrivePolicy에 의해 DLQ로 이동

### 핵심 코드 패턴

```java
// SQS 메시지 발송 (Producer)
@Component
public class SqsMessageSender {
    private final SqsClient sqsClient;

    public void sendEmailMessage(EmailNotificationMessage message) {
        sqsClient.sendMessage(SendMessageRequest.builder()
            .queueUrl(emailQueueUrl)
            .messageBody(objectMapper.writeValueAsString(message))
            .build());
    }
}

// SQS 메시지 소비 (Consumer)
@Component
public class EmailQueueConsumer {
    @Scheduled(fixedDelay = 5000)
    public void pollMessages() {
        var messages = sqsClient.receiveMessage(request).messages();
        for (var message : messages) {
            processAndDelete(message);
        }
    }
}
```

---

## Amazon EventBridge 통합 (shopping-service)

shopping-service는 **Dual Publish 패턴**으로 Kafka와 EventBridge에 동시 발행합니다. Kafka는 핵심 도메인 이벤트 스트림을 담당하고, EventBridge는 조건부 라우팅이 필요한 보조 채널로 활용됩니다.

### Dual Publish 패턴

```
OrderSagaOrchestrator
    ├── Kafka (동기): 핵심 이벤트 스트림 (notification-service 등)
    └── EventBridge (@Async): 조건부 라우팅 (analytics, 알림 확장 등)
```

**설계 원칙**:
- Kafka가 **항상 Primary** — EventBridge 실패가 주문 처리를 중단시키지 않음
- EventBridge 발행은 `@Async`로 비동기 실행 → 주문 응답 지연 없음
- EventBridge 실패 시 로그만 기록 (fire-and-forget)

### EventBridge 구성

| 항목 | 값 |
|------|-----|
| **Event Bus** | `portal-universe` (커스텀 버스) |
| **Source** | `com.portal.universe.shopping` |
| **Detail Type** | `ORDER_SAGA_COMPLETED`, `ORDER_SAGA_FAILED` |

### 이벤트 라우팅 규칙

| Rule | Pattern | Target | 용도 |
|------|---------|--------|------|
| `high-value-order-rule` | `totalAmount > 100000` | SQS | 고액 주문 별도 처리 |
| `saga-failure-rule` | `detailType = ORDER_SAGA_FAILED` | SQS | Saga 실패 알림 |

### 핵심 코드 패턴

```java
@Async
public void publishToEventBridge(String detailType, Object detail) {
    eventBridgeClient.putEvents(PutEventsRequest.builder()
        .entries(PutEventsRequestEntry.builder()
            .eventBusName("portal-universe")
            .source("com.portal.universe.shopping")
            .detailType(detailType)
            .detail(objectMapper.writeValueAsString(detail))
            .build())
        .build());
}
```

---

## CloudWatch Custom Metrics (shopping-service)

`OrderSagaOrchestrator`가 Saga 실행 결과를 CloudWatch Custom Metrics로 발행합니다. 임계값 초과 시 CloudWatch Alarm → SNS → SQS 체인으로 운영 알림을 전달합니다.

### 메트릭 구성

| Namespace | Metric Name | Dimensions | 단위 | 설명 |
|-----------|-------------|------------|------|------|
| `PortalUniverse/Shopping` | `SagaCompleted` | `Service=shopping-service` | Count | Saga 정상 완료 수 |
| `PortalUniverse/Shopping` | `SagaFailed` | `Service=shopping-service` | Count | Saga 실패 수 |
| `PortalUniverse/Shopping` | `SagaCompensationFailed` | `Service=shopping-service` | Count | 보상 실패 (수동 개입 필요) |
| `PortalUniverse/Shopping` | `SagaDuration` | `Service=shopping-service` | Milliseconds | Saga 전체 실행 시간 |

### Alarm → SNS → SQS 파이프라인

```
CloudWatch Alarm (SagaFailed >= 5/5min)
    → SNS Topic (saga-alerts-topic)
        → SQS Queue (saga-alert-queue)
            → 운영팀 폴링/처리
```

| Alarm | 조건 | 기간 | 대상 |
|-------|------|------|------|
| `saga-failure-alarm` | SagaFailed ≥ 5 | 5분 | saga-alerts-topic |
| `saga-compensation-alarm` | SagaCompensationFailed ≥ 1 | 1분 | saga-alerts-topic |

### 핵심 코드 패턴

```java
@Component
public class SagaCloudWatchPublisher {
    private final CloudWatchClient cloudWatchClient;

    public void publishSagaMetric(String metricName, double value, StandardUnit unit) {
        cloudWatchClient.putMetricData(PutMetricDataRequest.builder()
            .namespace("PortalUniverse/Shopping")
            .metricData(MetricDatum.builder()
                .metricName(metricName)
                .value(value)
                .unit(unit)
                .dimensions(Dimension.builder()
                    .name("Service").value("shopping-service")
                    .build())
                .timestamp(Instant.now())
                .build())
            .build());
    }
}
```

---

## LocalStack 개발 환경

모든 AWS 서비스(SQS, SNS, EventBridge, CloudWatch 등)는 로컬 개발 환경에서 **LocalStack**으로 에뮬레이션됩니다. `docker-compose-local.yml`의 LocalStack 컨테이너가 13개 AWS 서비스를 제공하며, `localstack-init/` 스크립트가 시작 시 리소스를 자동 생성합니다.

| 서비스 | 로컬 Endpoint | Init Script |
|--------|---------------|-------------|
| SQS | `http://localhost:4566` | `03-init-sqs.sh` |
| SNS | `http://localhost:4566` | `03-init-sqs.sh` (SQS와 함께) |
| EventBridge | `http://localhost:4566` | `05-init-eventbridge.sh` |
| CloudWatch | `http://localhost:4566` | `07-init-cloudwatch.sh` |
| Lambda | `http://localhost:4566` | `06-init-lambda.sh` |
| Secrets Manager | `http://localhost:4566` | `04-init-secrets.sh` |

> 상세 인프라 구성은 [LocalStack Terraform IaC](../../../infra/terraform/localstack/)와 `localstack-init/` 스크립트 참조.

---

## 관련 문서
- [service-communication.md](./service-communication.md) - 서비스 간 통신 패턴 (동기 vs 비동기)
- [notification-service 아키텍처](../notification-service/architecture-overview.md)
- [ADR-032: Kafka Configuration Standardization](../../adr/ADR-032-kafka-configuration-standardization.md)
- [ADR-047: Avro 및 Schema Registry 도입](../../adr/ADR-047-avro-schema-registry-adoption.md)
- [ADR-049: Secrets Manager + SSM 통합](../../adr/ADR-049-secrets-manager-ssm-integration.md)
- [ADR-050: LocalStack AWS 서비스 확장](../../adr/ADR-050-localstack-aws-services-expansion.md)
- [shopping-service Data Flow](../shopping-service/data-flow.md) - EventBridge Dual Publish + CloudWatch 상세
- [notification-service Data Flow](../notification-service/data-flow.md) - SQS 이메일 큐 통합

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|-----------|--------|
| 2026-02-06 | 실제 코드 기반 신규 작성 (17개 토픽 분석 완료) | Laze |
| 2026-02-10 | ADR-032 반영: topic 명명 규칙 통일, user-signup → auth.user.signed-up, Topics SSOT 명시 | Laze |
| 2026-02-21 | ADR-047 반영: JSON → Avro 전환, Schema Registry 추가, event-contracts 통합 모듈, 다이어그램 확장 | Laze |
| 2026-02-25 | AWS 메시징 통합: SQS(notification), EventBridge Dual Publish(shopping), CloudWatch Custom Metrics, LocalStack 섹션 추가 | Laze |

---

📂 Kafka 설정 파일 및 Consumer 구현 상세는 각 서비스 디렉토리의 `application.yml`, `*Consumer.java` 참조
