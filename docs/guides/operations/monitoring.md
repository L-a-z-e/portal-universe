---
id: monitoring
title: Monitoring Stack Documentation
type: guide
status: current
created: 2026-01-19
updated: 2026-02-25
author: Laze
tags: [monitoring, prometheus, grafana, zipkin, guide, opentelemetry, polyglot]
related:
  - adr-033-polyglot-observability-strategy
  - learning-zipkin-tracing
  - learning-prometheus-grafana
---

# Monitoring Stack Documentation

**난이도**: ⭐⭐⭐ | **예상 시간**: 30분 | **카테고리**: Operations

## 모니터링 스택 개요

Portal Universe 프로젝트는 다음 4가지 핵심 모니터링 도구를 사용합니다:

- **Prometheus**: 메트릭 수집 및 저장 (애플리케이션 메트릭)
- **Grafana**: 메트릭 시각화 대시보드
- **Zipkin**: 분산 추적(Distributed Tracing)
- **CloudWatch**: AWS 관리 서비스 메트릭 및 커스텀 비즈니스 메트릭

---

## 1. Prometheus 설정

### 배포 구성

```yaml
image: prom/prometheus:v2.53.5
port: 9090
```

**접속 URL**: http://portal-universe:8080/prometheus

### 메트릭 수집 설정

Prometheus는 다음 간격으로 메트릭을 수집합니다:

- **scrape_interval**: 15초 (메트릭 수집 주기)
- **evaluation_interval**: 15초 (Rule 평가 주기)
- **kubernetes_sd_configs**를 통한 Pod 자동 발견

### 어노테이션 기반 수집

Pod에 아래 어노테이션을 추가하면 Prometheus가 자동으로 메트릭을 수집합니다:

```yaml
annotations:
  prometheus.io/scrape: "true"
  prometheus.io/path: "/actuator/prometheus"
  prometheus.io/port: "8081"
```

**예시**: Spring Boot Actuator의 Prometheus 엔드포인트를 노출하는 경우

---

## 2. Grafana 설정

### 배포 구성

```yaml
image: grafana/grafana-oss:11.4.0
port: 3000
```

**접속 URL**: http://portal-universe:8080/grafana

### 기본 계정

- **Username**: admin
- **Password**: admin (첫 로그인 후 변경 권장)

### 기본 플러그인

- `grafana-piechart-panel`: 파이 차트 시각화

### 데이터소스 연결

Prometheus를 데이터소스로 추가:

1. Grafana 로그인
2. Configuration > Data Sources
3. Add data source > Prometheus 선택
4. **URL**: `http://prometheus:9090`
5. Save & Test

---

## 3. Zipkin 설정

### 배포 구성

```yaml
image: openzipkin/zipkin:3.4.2
port: 9411
```

**접속 URL**: http://portal-universe:8080/zipkin

### 저장소

- **개발 환경**: In-memory (기본값)
- **운영 환경**: Elasticsearch 권장 (장기 데이터 보관)

### Health Check

```yaml
livenessProbe:
  httpGet:
    path: /health
    port: 9411
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /health
    port: 9411
  initialDelaySeconds: 20
  periodSeconds: 5
```

### 리소스 설정

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

---

## 서비스 연동

### Spring Boot 서비스에서 Zipkin 연동

**Spring Boot 3.x**부터는 **Micrometer Tracing** + **OpenTelemetry**를 사용합니다.

#### 의존성 (build.gradle)

```gradle
// Micrometer Tracing with OpenTelemetry
implementation 'io.micrometer:micrometer-tracing-bridge-otel'
implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
```

#### application.yml 설정

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% 샘플링 (개발 환경)
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

**운영 환경**: `probability`를 0.1 ~ 0.2 (10~20%)로 조정하여 성능 영향 최소화

---

## 접속 정보 요약

| 도구 | 포트 | 접속 URL | 용도 |
|------|------|----------|------|
| Prometheus | 9090 | http://portal-universe:8080/prometheus | 메트릭 쿼리 |
| Grafana | 3000 | http://portal-universe:8080/grafana | 대시보드 시각화 |
| Zipkin | 9411 | http://portal-universe:8080/zipkin | 분산 추적 |

---

## 모니터링 대시보드 활용

### Grafana 대시보드 추천

1. **JVM (Micrometer)**: Spring Boot 애플리케이션 메트릭
2. **Kubernetes Cluster**: 클러스터 리소스 사용량
3. **Pod Monitoring**: 개별 Pod CPU/Memory 사용량

### Prometheus 쿼리 예시

```promql
# HTTP 요청 수
sum(rate(http_server_requests_seconds_count[5m])) by (uri)

# JVM 메모리 사용량
jvm_memory_used_bytes{area="heap"}

# Pod CPU 사용률
sum(rate(container_cpu_usage_seconds_total[5m])) by (pod)
```

---

## Polyglot 서비스 지원 (ADR-033)

Portal Universe는 **Java/Spring**, **NestJS(TypeScript)**, **Python** 등 다양한 스택을 통합 모니터링합니다.

### Unified Metrics

Prometheus Recording Rules를 통해 스택별 메트릭을 통합:

```promql
# Java: http_server_requests_seconds_count
# NestJS: http_server_duration_milliseconds_count (ms → s 변환)
# Python: http_requests_total
job:http_requests:rate5m
```

### Grafana Polyglot Dashboard

**위치**: `monitoring/grafana/provisioning/dashboards/json/polyglot-overview.json`

**패널**:
- Service Status (UP/DOWN)
- Request Rate (전체 스택)
- Error Rate (5xx)
- P95 Latency (스택별 단위 통일)
- Availability SLI
- Stack Info (언어/프레임워크)

---

## 4. CloudWatch 설정 (AWS Custom Metrics)

### 개요

CloudWatch는 AWS 관리 서비스의 기본 메트릭과 애플리케이션에서 발행하는 커스텀 비즈니스 메트릭을 수집합니다. Prometheus가 애플리케이션 레벨(JVM, HTTP 요청 등) 메트릭을 담당하는 반면, CloudWatch는 AWS 인프라와 도메인 비즈니스 메트릭을 담당합니다.

### 로컬 환경

- **LocalStack** (`localhost:4566`)이 CloudWatch API를 에뮬레이션
- AWS CLI 또는 SDK로 메트릭 조회 가능

### 커스텀 메트릭 (shopping-service)

`OrderSagaOrchestrator`가 Saga 실행 결과를 CloudWatch Custom Metrics로 발행합니다.

| Namespace | Metric Name | 단위 | 설명 |
|-----------|-------------|------|------|
| `PortalUniverse/Shopping` | `SagaCompleted` | Count | Saga 정상 완료 |
| `PortalUniverse/Shopping` | `SagaFailed` | Count | Saga 실패 |
| `PortalUniverse/Shopping` | `SagaCompensationFailed` | Count | 보상 실패 (수동 개입 필요) |
| `PortalUniverse/Shopping` | `SagaDuration` | Milliseconds | Saga 전체 실행 시간 |

### CloudWatch Alarms

임계값 초과 시 SNS → SQS 체인으로 운영 알림을 전달합니다.

| Alarm | 조건 | 기간 | Action |
|-------|------|------|--------|
| `saga-failure-alarm` | SagaFailed ≥ 5 | 5분 | SNS(`saga-alerts-topic`) → SQS(`saga-alert-queue`) |
| `saga-compensation-alarm` | SagaCompensationFailed ≥ 1 | 1분 | SNS(`saga-alerts-topic`) → SQS(`saga-alert-queue`) |

### 메트릭 조회 (로컬 환경)

```bash
# 커스텀 메트릭 네임스페이스 목록
aws --endpoint-url=http://localhost:4566 cloudwatch list-metrics \
  --namespace "PortalUniverse/Shopping"

# 특정 메트릭 통계 조회 (최근 1시간)
aws --endpoint-url=http://localhost:4566 cloudwatch get-metric-statistics \
  --namespace "PortalUniverse/Shopping" \
  --metric-name "SagaFailed" \
  --start-time $(date -u -v-1H +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Sum

# Alarm 상태 확인
aws --endpoint-url=http://localhost:4566 cloudwatch describe-alarms
```

### Prometheus vs CloudWatch 역할 분담

| 영역 | Prometheus | CloudWatch |
|------|-----------|------------|
| JVM 메트릭 (Heap, GC, Thread) | ✅ | - |
| HTTP 요청 메트릭 (Rate, Latency) | ✅ | - |
| Kafka Consumer Lag | ✅ | - |
| SQS 큐 깊이/메시지 수 | - | ✅ |
| EventBridge 이벤트 수 | - | ✅ |
| Lambda 실행 시간/에러 | - | ✅ |
| Saga 비즈니스 메트릭 | - | ✅ (Custom) |
| S3 요청 수/크기 | - | ✅ |

---

## 트러블슈팅

### Prometheus가 메트릭을 수집하지 못할 때

1. Pod 어노테이션 확인
2. Actuator 엔드포인트 활성화 확인: `/actuator/prometheus`
3. NetworkPolicy로 인한 접근 차단 확인
4. **NestJS**: `PrometheusModule` 등록 확인, `/metrics` 경로
5. **Python**: `prometheus_client` 미들웨어 활성화 확인

### Zipkin에 Trace가 표시되지 않을 때

1. **Java**: `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-zipkin` 의존성 확인
2. `management.zipkin.tracing.endpoint` 설정 확인
3. Zipkin 서비스 Health Check 확인
4. **NestJS**: OpenTelemetry SDK 초기화 확인 (`tracing.ts`)
5. **Python**: `opentelemetry-instrumentation` 활성화 확인

---

## 참고 문서

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Zipkin Documentation](https://zipkin.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [ADR-033: Polyglot Observability Strategy](../../adr/ADR-033-polyglot-observability-strategy.md)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-01-19 | 최초 작성 | Laze |
| 2026-02-06 | Zipkin 설정 업데이트 | Laze |
| 2026-02-13 | Brave → OpenTelemetry 마이그레이션, Polyglot 서비스 지원 추가 (ADR-033) | Laze |
| 2026-02-25 | CloudWatch Custom Metrics 섹션 추가, Prometheus vs CloudWatch 역할 분담 | Laze |
