# ADR-050: LocalStack AWS 서비스 확장 및 Terraform IaC 도입

**Status**: Accepted
**Date**: 2026-02-25
**Author**: Laze

## Context

LocalStack이 S3만 에뮬레이션하던 초기 구성에서, 서비스가 성장하면서 더 많은 AWS 서비스가 필요해졌다.

### 기존 상태

| 항목 | 상태 |
|------|------|
| LocalStack 서비스 | S3 1개만 |
| 리소스 관리 | Shell script (`localstack-init/`) 수동 관리 |
| notification-service 이메일 | 동기 발송 (Kafka Consumer 블로킹) |
| shopping-service 이벤트 | Kafka 단일 채널 |
| 비즈니스 메트릭 | Prometheus만 (도메인 메트릭 부재) |
| 설정 관리 | .env.local 파일 직접 관리 |

### 요구사항

1. **notification-service**: 이메일 발송을 Kafka Consumer에서 분리, 안정적 재시도 필요 → SQS
2. **shopping-service**: Saga 이벤트의 조건부 라우팅 (고액 주문 필터링 등) → EventBridge
3. **shopping-service**: Saga 비즈니스 메트릭 수집 및 임계값 알람 → CloudWatch
4. **blog-service**: 대용량 파일 서버 경유 없이 직접 업로드 → S3 Presigned URL
5. **전 서비스**: 민감 설정 중앙 관리 → Secrets Manager + SSM (ADR-049)
6. **인프라**: Shell script의 한계 (멱등성, 의존성 관리) → Terraform IaC

### 검토한 대안

| 결정 포인트 | 선택 | 대안 | 선택 이유 |
|-------------|------|------|-----------|
| 이메일 큐 | **SQS** | Kafka 별도 토픽, RabbitMQ | SQS DLQ 내장, 관리 부담 최소, AWS 네이티브 |
| 이벤트 라우팅 | **EventBridge** | Kafka Streams, 별도 Kafka 토픽 | 콘텐츠 기반 필터링, 토픽 추가 없이 구독자 확장 |
| 비즈니스 메트릭 | **CloudWatch** | Prometheus Custom Metrics | AWS 인프라 메트릭과 통합, 알람 체인(SNS→SQS) 네이티브 |
| Kafka↔EventBridge 관계 | **Dual Publish** | EventBridge 완전 대체, Kafka→EventBridge 브릿지 | Kafka Primary 유지, EventBridge 실패 무시 가능 |
| IaC 도구 | **Terraform (tflocal)** | CloudFormation, CDK, Pulumi | LocalStack 공식 지원(tflocal), 선언적, 상태 관리 |

## Decision

### 1. LocalStack을 13개 AWS 서비스로 확장

```
S3, SQS, SNS, IAM, STS, Lambda, EventBridge, CloudWatch,
Secrets Manager, SSM, CloudFormation, Step Functions, Logs
```

### 2. 서비스별 AWS 통합 패턴

| 서비스 | AWS 서비스 | 패턴 |
|--------|-----------|------|
| notification-service | SQS | Kafka → NotificationConsumer → SQS → EmailQueueConsumer |
| shopping-service | EventBridge | Dual Publish: Kafka (Primary) + EventBridge (@Async) |
| shopping-service | CloudWatch | Custom Metrics (PutMetricData) + Alarm → SNS → SQS |
| blog-service | S3 Presigned URL | generatePresignedUrl(PUT, 10분 만료) |
| 전 서비스 | Secrets Manager + SSM | `spring.config.import: optional:aws-secretsmanager:` (ADR-049) |

### 3. EventBridge Dual Publish 설계 원칙

- Kafka가 **항상 Primary** — 핵심 도메인 이벤트 스트림
- EventBridge는 **보조 채널** — 조건부 라우팅, 분석, 알림 확장
- EventBridge 발행은 `@Async` — 실패해도 주문 트랜잭션에 영향 없음
- EventBridge가 Kafka를 대체하지 않음 (보완 관계)

### 4. Terraform IaC

`infra/terraform/localstack/`에서 모든 AWS 리소스를 선언적으로 관리한다.

- **Backend**: local (LocalStack 환경)
- **Provider**: `hashicorp/aws` with `tflocal` wrapper
- **리소스 수**: 42개 (IAM 10 + S3 4 + SQS 6 + Secrets 8 + SSM 6 + EventBridge 4 + CloudWatch 4)
- Init script(`localstack-init/`)는 유지하되, Terraform이 정본

### 5. Init Script 실행 순서

```
01-init-iam.sh → 02-init-s3.sh → 03-init-sqs.sh → 04-init-secrets.sh
→ 05-init-eventbridge.sh → 06-init-lambda.sh → 07-init-cloudwatch.sh
```

## Consequences

### 긍정적

- 로컬에서 프로덕션과 동일한 AWS 패턴 개발 가능
- Kafka Consumer의 이메일 발송 부담 분리 (SQS)
- 도메인 비즈니스 메트릭 수집 체계 확보 (CloudWatch)
- 인프라 리소스의 선언적 관리 (Terraform)
- 조건부 이벤트 라우팅으로 확장성 확보 (EventBridge)

### 부정적

- LocalStack `mem_limit: 1g` 필요 (이전보다 리소스 증가)
- Docker socket mount 필요 (Lambda 실행)
- Lambda 함수 사전 빌드 필요 (`lambda/image-thumbnail/build.sh`)
- 3개 메시징 시스템 학습 부담 (Kafka + SQS + EventBridge)

### 리스크

| 리스크 | 완화 |
|--------|------|
| LocalStack과 실제 AWS 동작 차이 | 통합 테스트에서 주요 시나리오 검증 |
| 메시징 시스템 복잡도 증가 | 명확한 역할 분담 문서화, 선택 기준 가이드 |
| LocalStack 메모리 부족 | `mem_limit: 1g` 설정, 필요 서비스만 활성화 가능 |

## Related

- [ADR-049: Secrets Manager + SSM 통합](./ADR-049-secrets-manager-ssm-integration.md)
- [ADR-047: Avro + Schema Registry 도입](./ADR-047-avro-schema-registry-adoption.md)
- [Event-Driven Architecture](../architecture/system/event-driven-architecture.md)
- [Shopping Service Data Flow](../architecture/shopping-service/data-flow.md)
- [Notification Service Data Flow](../architecture/notification-service/data-flow.md)
