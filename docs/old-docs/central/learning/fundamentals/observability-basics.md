# Observability 기초

## 개요

**Observability(관찰 가능성)**는 시스템의 외부 출력을 관찰하여 내부 상태를 이해할 수 있는 능력입니다. 마이크로서비스 아키텍처에서는 수십~수백 개의 서비스가 상호작용하므로, 문제 발생 시 원인을 파악하기 위해 강력한 Observability 전략이 필수입니다.

### Monitoring vs Observability

| 구분 | Monitoring | Observability |
|------|-----------|---------------|
| 접근 방식 | 사전 정의된 메트릭 수집 | 임의의 질문에 답할 수 있는 데이터 수집 |
| 질문 유형 | "알려진 문제가 발생했는가?" | "왜 이 문제가 발생했는가?" |
| 초점 | 시스템 상태 확인 | 시스템 동작 이해 |
| 데이터 활용 | 대시보드, 알림 | 탐색적 분석, 디버깅 |

---

## 1. Observability의 세 가지 기둥 (Three Pillars)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Observability                                │
├─────────────────────┬─────────────────────┬─────────────────────────┤
│        Logs         │       Metrics       │        Traces           │
│   (What happened)   │    (How much)       │    (Where/How long)     │
├─────────────────────┼─────────────────────┼─────────────────────────┤
│ • 이벤트 기록        │ • 수치 데이터        │ • 요청 흐름 추적         │
│ • 상세 컨텍스트      │ • 시계열 데이터      │ • 서비스 간 연결         │
│ • 텍스트 기반        │ • 집계 가능          │ • 지연시간 분석          │
└─────────────────────┴─────────────────────┴─────────────────────────┘
```

### 1.1 Logs (로그)

**무엇을**: 시스템에서 발생한 이산적(discrete) 이벤트의 기록

```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "ERROR",
  "service": "shopping-service",
  "traceId": "abc123def456",
  "spanId": "span789",
  "userId": "user-001",
  "message": "Failed to process order",
  "error": {
    "type": "InsufficientInventoryException",
    "message": "Product SKU-123 out of stock"
  },
  "context": {
    "orderId": "order-456",
    "productId": "SKU-123",
    "requestedQuantity": 5,
    "availableQuantity": 2
  }
}
```

**로그 레벨 가이드라인**:

| Level | 용도 | 예시 |
|-------|------|------|
| `TRACE` | 매우 상세한 디버깅 | 메서드 진입/종료 |
| `DEBUG` | 개발/디버깅용 정보 | 변수 값, 조건문 결과 |
| `INFO` | 정상 운영 이벤트 | 요청 처리 완료, 작업 시작 |
| `WARN` | 잠재적 문제 | 재시도 발생, deprecated API 호출 |
| `ERROR` | 오류 발생 | 예외, 실패한 작업 |
| `FATAL` | 치명적 오류 | 시스템 시작 실패 |

### 1.2 Metrics (메트릭)

**무엇을**: 시간에 따른 수치 데이터의 집계

**메트릭 유형**:

```
┌─────────────────────────────────────────────────────────────────┐
│                        Metric Types                              │
├─────────────────┬───────────────────────────────────────────────┤
│    Counter      │ 단조 증가하는 값 (요청 수, 에러 수)             │
│                 │ request_total{service="auth"} 1523             │
├─────────────────┼───────────────────────────────────────────────┤
│    Gauge        │ 증감하는 현재 값 (메모리 사용량, 동시 접속자)    │
│                 │ jvm_memory_used_bytes 524288000                │
├─────────────────┼───────────────────────────────────────────────┤
│   Histogram     │ 값의 분포 (응답 시간, 요청 크기)                │
│                 │ http_request_duration_seconds_bucket{le="0.5"} │
├─────────────────┼───────────────────────────────────────────────┤
│    Summary      │ 분위수(quantile) 계산 (p50, p95, p99)          │
│                 │ http_request_duration_seconds{quantile="0.95"} │
└─────────────────┴───────────────────────────────────────────────┘
```

**RED 메서드 (서비스 관점)**:
- **R**ate: 초당 요청 수
- **E**rrors: 실패한 요청 수
- **D**uration: 요청 처리 시간

**USE 메서드 (리소스 관점)**:
- **U**tilization: 리소스 사용률
- **S**aturation: 포화도 (큐 길이)
- **E**rrors: 에러 수

### 1.3 Traces (분산 추적)

**무엇을**: 분산 시스템에서 요청이 서비스를 거쳐가는 경로 추적

```
Request: GET /api/orders/123
         │
         ▼
┌────────────────────────────────────────────────────────────────────┐
│ Trace ID: abc-123-def                                              │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ Span: api-gateway (120ms)                                    │  │
│  │ ├─────────────────────────────────────────────────────────┐ │  │
│  │ │ Span: auth-service (25ms)                               │ │  │
│  │ │ └─ Verify JWT token                                     │ │  │
│  │ └─────────────────────────────────────────────────────────┘ │  │
│  │ ├─────────────────────────────────────────────────────────┐ │  │
│  │ │ Span: shopping-service (80ms)                           │ │  │
│  │ │ ├─────────────────────────────────────────────────────┐ │ │  │
│  │ │ │ Span: database query (15ms)                         │ │ │  │
│  │ │ └─────────────────────────────────────────────────────┘ │ │  │
│  │ │ ├─────────────────────────────────────────────────────┐ │ │  │
│  │ │ │ Span: redis cache (5ms)                             │ │ │  │
│  │ │ └─────────────────────────────────────────────────────┘ │ │  │
│  │ └─────────────────────────────────────────────────────────┘ │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

**핵심 개념**:
- **Trace**: 하나의 요청에 대한 전체 여정
- **Span**: Trace 내의 개별 작업 단위
- **Trace ID**: 전체 요청을 식별하는 고유 ID
- **Span ID**: 개별 작업을 식별하는 고유 ID
- **Parent Span ID**: 부모 작업의 ID

---

## 2. 중앙화된 로깅 (Centralized Logging)

마이크로서비스 환경에서 각 서비스의 로그를 개별적으로 확인하는 것은 비효율적입니다. 중앙화된 로깅 시스템이 필요합니다.

### 2.1 ELK Stack (Elasticsearch, Logstash, Kibana)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ELK Stack                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────┐     ┌─────────┐     ┌─────────────┐     ┌─────────┐   │
│  │ Service │────▶│ Filebeat│────▶│  Logstash   │────▶│Elastic- │   │
│  │  Logs   │     │         │     │ (Transform) │     │ search  │   │
│  └─────────┘     └─────────┘     └─────────────┘     └────┬────┘   │
│                                                            │        │
│  ┌─────────┐     ┌─────────┐                               │        │
│  │ Service │────▶│ Filebeat│──────────────────────────────▶│        │
│  │  Logs   │     │         │                               │        │
│  └─────────┘     └─────────┘                               ▼        │
│                                                        ┌─────────┐  │
│                                                        │ Kibana  │  │
│                                                        │(Visual) │  │
│                                                        └─────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**구성 요소**:

| 컴포넌트 | 역할 | 특징 |
|----------|------|------|
| **Elasticsearch** | 로그 저장/검색 | 전문 검색, 분석 엔진 |
| **Logstash** | 로그 수집/변환 | 다양한 입력/출력 플러그인 |
| **Kibana** | 시각화/분석 | 대시보드, 검색 UI |
| **Filebeat** | 로그 전송 | 경량 에이전트 |

**Logstash 설정 예시**:

```ruby
# logstash.conf
input {
  beats {
    port => 5044
  }
}

filter {
  # JSON 로그 파싱
  json {
    source => "message"
  }

  # Trace ID 추출
  if [traceId] {
    mutate {
      add_field => { "trace_id" => "%{traceId}" }
    }
  }

  # 타임스탬프 파싱
  date {
    match => [ "timestamp", "ISO8601" ]
    target => "@timestamp"
  }

  # 서비스별 태깅
  if [kubernetes][container][name] {
    mutate {
      add_field => { "service_name" => "%{[kubernetes][container][name]}" }
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "logs-%{service_name}-%{+YYYY.MM.dd}"
  }
}
```

### 2.2 Loki + Grafana (경량 대안)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Loki Stack (PLG)                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐       │
│  │ Service │────▶│ Promtail│────▶│  Loki   │────▶│ Grafana │       │
│  │  Logs   │     │ (Agent) │     │ (Store) │     │ (Query) │       │
│  └─────────┘     └─────────┘     └─────────┘     └─────────┘       │
│                                                                      │
│  특징:                                                               │
│  • 로그 내용 인덱싱 없음 (라벨만 인덱싱)                               │
│  • Prometheus와 유사한 라벨 기반 쿼리                                 │
│  • 저렴한 스토리지 비용                                               │
│  • Grafana와 네이티브 통합                                           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**Promtail 설정 예시**:

```yaml
# promtail-config.yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: kubernetes-pods
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        target_label: app
      - source_labels: [__meta_kubernetes_namespace]
        target_label: namespace
    pipeline_stages:
      - json:
          expressions:
            level: level
            traceId: traceId
            message: message
      - labels:
          level:
          traceId:
```

**LogQL 쿼리 예시**:

```logql
# 특정 서비스의 에러 로그
{app="shopping-service"} |= "ERROR"

# JSON 파싱 후 필터링
{app="auth-service"} | json | level="ERROR" | line_format "{{.message}}"

# 로그 카운트 (5분간)
count_over_time({app="shopping-service"} |= "ERROR" [5m])

# 특정 Trace ID의 모든 로그
{traceId="abc-123-def"}
```

### ELK vs Loki 비교

| 구분 | ELK Stack | Loki |
|------|-----------|------|
| **인덱싱** | 전문 인덱싱 | 라벨만 인덱싱 |
| **검색 속도** | 빠름 | 상대적으로 느림 |
| **스토리지 비용** | 높음 | 낮음 |
| **리소스 사용** | 높음 (특히 ES) | 낮음 |
| **쿼리 언어** | Lucene/KQL | LogQL |
| **적합 케이스** | 대규모, 복잡한 분석 | 중소규모, 비용 민감 |

---

## 3. 메트릭 수집 (Metrics Collection)

### 3.1 Prometheus

Prometheus는 CNCF 프로젝트로, Pull 기반 메트릭 수집 시스템입니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                       Prometheus Architecture                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│              ┌─────────────┐                                         │
│              │ Prometheus  │                                         │
│              │   Server    │                                         │
│              └──────┬──────┘                                         │
│                     │                                                │
│         ┌──────────┼───────────┐                                    │
│         │          │           │                                     │
│         ▼          ▼           ▼                                     │
│    ┌─────────┐ ┌─────────┐ ┌─────────┐                              │
│    │ Service │ │ Service │ │ Service │  Pull /metrics               │
│    │ :8080   │ │ :8081   │ │ :8082   │                              │
│    │/metrics │ │/metrics │ │/metrics │                              │
│    └─────────┘ └─────────┘ └─────────┘                              │
│                                                                      │
│    Service Discovery:                                                │
│    • Kubernetes                                                      │
│    • Consul                                                          │
│    • Static config                                                   │
│                                                                      │
│    ┌─────────────┐     ┌─────────────┐                              │
│    │ Alertmanager│     │   Grafana   │                              │
│    │  (Alerts)   │◀────│  (Visual)   │                              │
│    └─────────────┘     └─────────────┘                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**Prometheus 설정 예시**:

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

rule_files:
  - "alert_rules.yml"

scrape_configs:
  # Prometheus 자체 메트릭
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Spring Boot 서비스들
  - job_name: 'spring-boot-services'
    metrics_path: '/actuator/prometheus'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
```

### 3.2 PromQL 쿼리 예시

```promql
# 서비스별 초당 요청 수
rate(http_server_requests_seconds_count{application="shopping-service"}[5m])

# 응답 시간 95 퍼센타일
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)
)

# 에러율 계산
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m])) * 100

# JVM 힙 메모리 사용률
jvm_memory_used_bytes{area="heap"}
/ jvm_memory_max_bytes{area="heap"} * 100

# CPU 사용률 (컨테이너)
rate(container_cpu_usage_seconds_total{container="shopping-service"}[5m]) * 100
```

### 3.3 Grafana 대시보드

Grafana는 다양한 데이터소스를 지원하는 시각화 도구입니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Grafana Dashboard Example                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    Service Overview                          │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │    │
│  │  │ Requests │ │ Error %  │ │ Latency  │ │ Saturation│       │    │
│  │  │  12.5k   │ │  0.12%   │ │  45ms    │ │   67%    │       │    │
│  │  │  /min    │ │          │ │  p95     │ │          │       │    │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌─────────────────────────────┐ ┌────────────────────────────┐    │
│  │     Request Rate Graph      │ │    Response Time Graph      │    │
│  │       ___________           │ │       ___________           │    │
│  │      /           \          │ │      /           \          │    │
│  │     /             \____     │ │   __/             \____     │    │
│  │    /                        │ │  /                          │    │
│  │   /                         │ │ /                           │    │
│  │  0    6    12   18   24     │ │ 0    6    12   18   24      │    │
│  └─────────────────────────────┘ └────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**Grafana Dashboard JSON 예시**:

```json
{
  "dashboard": {
    "title": "Shopping Service Dashboard",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{application=\"shopping-service\"}[5m]))",
            "legendFormat": "Requests/sec"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{application=\"shopping-service\",status=~\"5..\"}[5m])) / sum(rate(http_server_requests_seconds_count{application=\"shopping-service\"}[5m])) * 100",
            "legendFormat": "Error %"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "thresholds": {
              "steps": [
                { "color": "green", "value": null },
                { "color": "yellow", "value": 1 },
                { "color": "red", "value": 5 }
              ]
            }
          }
        }
      }
    ]
  }
}
```

---

## 4. 분산 추적 (Distributed Tracing)

### 4.1 분산 추적의 필요성

```
단일 요청이 여러 서비스를 거치는 경우:

사용자 ──▶ API Gateway ──▶ Auth Service ──▶ Shopping Service ──▶ DB
                                    │
                                    └──▶ Inventory Service ──▶ Redis
                                    │
                                    └──▶ Notification Service ──▶ Kafka

문제: "주문 처리가 느립니다" - 어디서 지연이 발생하는가?
```

### 4.2 OpenTelemetry

OpenTelemetry는 CNCF의 Observability 표준 프레임워크입니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                      OpenTelemetry Architecture                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Application                                                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐                      │    │
│  │  │ Traces  │  │ Metrics │  │  Logs   │  ← OTel SDK          │    │
│  │  └────┬────┘  └────┬────┘  └────┬────┘                      │    │
│  │       └───────────┬┴───────────┘                            │    │
│  │                   ▼                                          │    │
│  │            ┌─────────────┐                                   │    │
│  │            │ OTel Agent  │  ← Auto-instrumentation           │    │
│  │            └──────┬──────┘                                   │    │
│  └───────────────────┼──────────────────────────────────────────┘    │
│                      ▼                                               │
│            ┌─────────────────┐                                       │
│            │  OTel Collector │  ← Receive, Process, Export          │
│            └────────┬────────┘                                       │
│         ┌───────────┼───────────┐                                   │
│         ▼           ▼           ▼                                    │
│    ┌─────────┐ ┌─────────┐ ┌─────────┐                              │
│    │ Jaeger  │ │Prometheus│ │  Loki   │  ← Backends                 │
│    └─────────┘ └─────────┘ └─────────┘                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**OpenTelemetry Collector 설정**:

```yaml
# otel-collector-config.yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
    timeout: 1s
    send_batch_size: 1024

  memory_limiter:
    check_interval: 1s
    limit_mib: 1000

  resource:
    attributes:
      - key: environment
        value: production
        action: insert

exporters:
  jaeger:
    endpoint: jaeger:14250
    tls:
      insecure: true

  prometheus:
    endpoint: "0.0.0.0:8889"

  loki:
    endpoint: "http://loki:3100/loki/api/v1/push"

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [jaeger]

    metrics:
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [prometheus]

    logs:
      receivers: [otlp]
      processors: [memory_limiter, batch]
      exporters: [loki]
```

### 4.3 Jaeger

Jaeger는 Uber에서 개발한 분산 추적 시스템입니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Jaeger Architecture                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Services                   Jaeger                                   │
│  ┌─────────┐               ┌─────────────┐                          │
│  │ Service │──────────────▶│   Agent     │ (sidecar, UDP)           │
│  └─────────┘               └──────┬──────┘                          │
│  ┌─────────┐                      │                                  │
│  │ Service │──────────────────────┼──────▶ ┌─────────────┐          │
│  └─────────┘               ┌──────┴──────┐ │  Collector  │          │
│  ┌─────────┐               │             │ │   (gRPC)    │          │
│  │ Service │───────────────┼─────────────┼▶└──────┬──────┘          │
│  └─────────┘               │             │        │                  │
│                            └─────────────┘        ▼                  │
│                                            ┌─────────────┐          │
│                                            │   Storage   │          │
│                                            │ (ES/Cassandra)│         │
│                                            └──────┬──────┘          │
│                                                   │                  │
│                                                   ▼                  │
│                                            ┌─────────────┐          │
│                                            │   Query     │          │
│                                            │    UI       │          │
│                                            └─────────────┘          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.4 Zipkin

Zipkin은 Twitter에서 개발한 분산 추적 시스템입니다. Portal Universe에서 사용합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Zipkin Architecture                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Services                                                            │
│  ┌─────────┐                                                         │
│  │ Service │──────────────┐                                          │
│  └─────────┘              │                                          │
│  ┌─────────┐              ▼                                          │
│  │ Service │────────▶ ┌─────────────────────────────────┐           │
│  └─────────┘          │           Zipkin Server          │           │
│  ┌─────────┐          │  ┌─────────┐  ┌─────────┐       │           │
│  │ Service │────────▶ │  │Collector│  │  Query  │       │           │
│  └─────────┘          │  └────┬────┘  └────┬────┘       │           │
│                       │       │            │             │           │
│                       │       ▼            ▼             │           │
│   Transport:          │  ┌─────────────────────┐        │           │
│   • HTTP              │  │      Storage        │        │           │
│   • Kafka             │  │ (Memory/MySQL/ES)   │        │           │
│   • RabbitMQ          │  └─────────────────────┘        │           │
│                       │                                  │           │
│                       │  ┌─────────────────────┐        │           │
│                       │  │        UI           │        │           │
│                       │  │ (Dependency Graph)  │        │           │
│                       │  └─────────────────────┘        │           │
│                       └─────────────────────────────────┘           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Jaeger vs Zipkin 비교

| 구분 | Jaeger | Zipkin |
|------|--------|--------|
| **개발사** | Uber → CNCF | Twitter |
| **언어** | Go | Java |
| **UI** | 풍부함 | 심플함 |
| **스토리지** | Cassandra, ES, Kafka | Memory, MySQL, ES, Cassandra |
| **성능** | 대규모에 최적화 | 중소규모에 적합 |
| **적응적 샘플링** | 지원 | 미지원 |
| **OTel 지원** | Native | Native |

---

## 5. Portal Universe의 Observability 스택

Portal Universe는 다음과 같은 Observability 스택을 사용합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│              Portal Universe Observability Stack                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                      Spring Boot Services                    │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │    │
│  │  │auth-service │  │blog-service │  │shopping-svc │         │    │
│  │  │             │  │             │  │             │         │    │
│  │  │ Micrometer  │  │ Micrometer  │  │ Micrometer  │         │    │
│  │  │ + Zipkin    │  │ + Zipkin    │  │ + Zipkin    │         │    │
│  │  │ Brave       │  │ Brave       │  │ Brave       │         │    │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘         │    │
│  └─────────┼────────────────┼────────────────┼──────────────────┘    │
│            │                │                │                       │
│            └────────────────┼────────────────┘                       │
│                             │                                        │
│            ┌────────────────┼────────────────┐                       │
│            ▼                ▼                ▼                       │
│     ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               │
│     │ Prometheus  │  │   Zipkin    │  │    Loki     │               │
│     │  (Metrics)  │  │  (Traces)   │  │   (Logs)    │               │
│     └──────┬──────┘  └──────┬──────┘  └──────┬──────┘               │
│            │                │                │                       │
│            └────────────────┼────────────────┘                       │
│                             ▼                                        │
│                      ┌─────────────┐                                 │
│                      │   Grafana   │                                 │
│                      │ (Dashboard) │                                 │
│                      └─────────────┘                                 │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.1 Micrometer + Prometheus 설정

**build.gradle (Kotlin DSL)**:

```kotlin
dependencies {
    // Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Micrometer Prometheus
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Micrometer Tracing (Zipkin/Brave)
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
}
```

**application.yml**:

```yaml
# Actuator 설정
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true
    prometheus:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:local}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
      slo:
        http.server.requests: 50ms, 100ms, 200ms, 500ms

# Prometheus 메트릭 엔드포인트
server:
  port: 8080
```

**Custom Metrics 추가**:

```java
@Component
@RequiredArgsConstructor
public class OrderMetrics {

    private final MeterRegistry meterRegistry;

    private Counter orderCreatedCounter;
    private Counter orderFailedCounter;
    private Timer orderProcessingTimer;
    private AtomicInteger activeOrdersGauge;

    @PostConstruct
    public void init() {
        // Counter: 주문 생성 수
        orderCreatedCounter = Counter.builder("orders.created")
            .description("Number of orders created")
            .tag("service", "shopping-service")
            .register(meterRegistry);

        // Counter: 주문 실패 수
        orderFailedCounter = Counter.builder("orders.failed")
            .description("Number of failed orders")
            .tag("service", "shopping-service")
            .register(meterRegistry);

        // Timer: 주문 처리 시간
        orderProcessingTimer = Timer.builder("orders.processing.time")
            .description("Order processing duration")
            .tag("service", "shopping-service")
            .publishPercentileHistogram()
            .register(meterRegistry);

        // Gauge: 진행 중인 주문 수
        activeOrdersGauge = new AtomicInteger(0);
        Gauge.builder("orders.active", activeOrdersGauge, AtomicInteger::get)
            .description("Number of orders being processed")
            .tag("service", "shopping-service")
            .register(meterRegistry);
    }

    public void recordOrderCreated() {
        orderCreatedCounter.increment();
    }

    public void recordOrderFailed(String reason) {
        orderFailedCounter.increment();
        meterRegistry.counter("orders.failed", "reason", reason).increment();
    }

    public Timer.Sample startOrderProcessing() {
        activeOrdersGauge.incrementAndGet();
        return Timer.start(meterRegistry);
    }

    public void endOrderProcessing(Timer.Sample sample) {
        sample.stop(orderProcessingTimer);
        activeOrdersGauge.decrementAndGet();
    }
}
```

### 5.2 Zipkin for Tracing

**application.yml**:

```yaml
# Zipkin/Tracing 설정
management:
  tracing:
    sampling:
      probability: 1.0  # 개발환경: 100% 샘플링
    propagation:
      type: b3  # B3 propagation (Zipkin 표준)

spring:
  application:
    name: shopping-service

# Zipkin 설정
zipkin:
  tracing:
    endpoint: http://zipkin:9411/api/v2/spans

# 로깅에 Trace ID 포함
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

**Custom Span 생성**:

```java
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final Tracer tracer;
    private final InventoryRepository repository;

    public InventoryStatus checkInventory(String productId, int quantity) {
        // 새로운 Span 생성
        Span span = tracer.nextSpan().name("check-inventory").start();

        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // Span에 태그 추가
            span.tag("product.id", productId);
            span.tag("quantity.requested", String.valueOf(quantity));

            // 비즈니스 로직
            Inventory inventory = repository.findByProductId(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

            boolean available = inventory.getQuantity() >= quantity;
            span.tag("inventory.available", String.valueOf(available));

            // 이벤트 기록
            if (!available) {
                span.event("inventory-insufficient");
            }

            return new InventoryStatus(productId, available, inventory.getQuantity());

        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

**RestTemplate/WebClient에 Tracing 전파**:

```java
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        // Spring Boot 3.x에서는 자동으로 tracing 전파됨
        return builder
            .baseUrl("http://auth-service:8080")
            .build();
    }
}

// Feign Client도 자동 지원
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/v1/users/{userId}")
    UserResponse getUser(@PathVariable String userId);
}
```

### 5.3 구조화된 로깅 패턴

**Logback 설정 (logback-spring.xml)**:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- JSON 로그 포맷 (Production) -->
    <springProfile name="docker,k8s">
        <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>traceId</includeMdcKeyName>
                <includeMdcKeyName>spanId</includeMdcKeyName>
                <customFields>{"service":"${spring.application.name}"}</customFields>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="JSON"/>
        </root>
    </springProfile>

    <!-- 콘솔 로그 (Development) -->
    <springProfile name="local">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr([%X{traceId:-},%X{spanId:-}]){yellow} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n%wEx</pattern>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
</configuration>
```

**로깅 유틸리티**:

```java
@Slf4j
@Component
public class OrderProcessor {

    public void processOrder(Order order) {
        // MDC에 컨텍스트 정보 추가
        MDC.put("orderId", order.getId());
        MDC.put("userId", order.getUserId());

        try {
            log.info("Starting order processing");

            // 비즈니스 로직
            validateOrder(order);
            log.debug("Order validation passed");

            reserveInventory(order);
            log.info("Inventory reserved for {} items", order.getItems().size());

            processPayment(order);
            log.info("Payment processed successfully");

            log.info("Order processing completed",
                kv("totalAmount", order.getTotalAmount()),
                kv("itemCount", order.getItems().size()));

        } catch (InsufficientInventoryException e) {
            log.warn("Insufficient inventory for order",
                kv("productId", e.getProductId()),
                kv("requested", e.getRequestedQuantity()),
                kv("available", e.getAvailableQuantity()));
            throw e;

        } catch (PaymentException e) {
            log.error("Payment failed for order",
                kv("reason", e.getReason()),
                kv("paymentMethod", order.getPaymentMethod()), e);
            throw e;

        } finally {
            MDC.remove("orderId");
            MDC.remove("userId");
        }
    }

    // Key-Value 로깅 헬퍼
    private static StructuredArgument kv(String key, Object value) {
        return StructuredArguments.keyValue(key, value);
    }
}
```

**로그 출력 예시 (JSON)**:

```json
{
  "@timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "service": "shopping-service",
  "traceId": "abc123def456",
  "spanId": "span789",
  "orderId": "order-456",
  "userId": "user-001",
  "message": "Order processing completed",
  "totalAmount": 150000,
  "itemCount": 3,
  "logger_name": "com.portal.shopping.OrderProcessor",
  "thread_name": "http-nio-8080-exec-1"
}
```

---

## 6. 알림 및 대시보드 설정

### 6.1 Alertmanager 설정

**alertmanager.yml**:

```yaml
global:
  resolve_timeout: 5m
  slack_api_url: 'https://hooks.slack.com/services/xxx'

route:
  receiver: 'default-receiver'
  group_by: ['alertname', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

  routes:
    # Critical 알림 → PagerDuty + Slack
    - match:
        severity: critical
      receiver: 'critical-receiver'
      continue: true

    # Warning 알림 → Slack only
    - match:
        severity: warning
      receiver: 'warning-receiver'

receivers:
  - name: 'default-receiver'
    slack_configs:
      - channel: '#alerts-default'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

  - name: 'critical-receiver'
    slack_configs:
      - channel: '#alerts-critical'
        title: ':fire: CRITICAL: {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
    pagerduty_configs:
      - service_key: 'xxx'
        severity: critical

  - name: 'warning-receiver'
    slack_configs:
      - channel: '#alerts-warning'
        title: ':warning: WARNING: {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

inhibit_rules:
  # Critical이 발생하면 동일 서비스의 Warning 억제
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'service']
```

### 6.2 Alert Rules

**alert_rules.yml**:

```yaml
groups:
  - name: service-alerts
    rules:
      # 서비스 다운
      - alert: ServiceDown
        expr: up{job=~".*-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.instance }} is down"
          description: "{{ $labels.job }} has been down for more than 1 minute."

      # 높은 에러율
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application)
          /
          sum(rate(http_server_requests_seconds_count[5m])) by (application)
          > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate in {{ $labels.application }}"
          description: "Error rate is {{ $value | humanizePercentage }} in the last 5 minutes."

      # 느린 응답 시간
      - alert: HighLatency
        expr: |
          histogram_quantile(0.95,
            sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application)
          ) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High latency in {{ $labels.application }}"
          description: "95th percentile latency is {{ $value }}s."

      # 메모리 사용량
      - alert: HighMemoryUsage
        expr: |
          jvm_memory_used_bytes{area="heap"}
          / jvm_memory_max_bytes{area="heap"}
          > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage in {{ $labels.application }}"
          description: "Heap memory usage is {{ $value | humanizePercentage }}."

  - name: business-alerts
    rules:
      # 주문 실패율
      - alert: HighOrderFailureRate
        expr: |
          sum(rate(orders_failed_total[5m]))
          / sum(rate(orders_created_total[5m]))
          > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High order failure rate"
          description: "Order failure rate is {{ $value | humanizePercentage }}."

      # 결제 지연
      - alert: PaymentProcessingDelay
        expr: |
          histogram_quantile(0.95,
            sum(rate(payment_processing_seconds_bucket[5m])) by (le)
          ) > 5
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "Payment processing is slow"
          description: "95th percentile payment processing time is {{ $value }}s."
```

### 6.3 Grafana Dashboard

**Service Overview Dashboard**:

```json
{
  "dashboard": {
    "title": "Portal Universe - Service Overview",
    "tags": ["portal-universe", "overview"],
    "templating": {
      "list": [
        {
          "name": "service",
          "type": "query",
          "query": "label_values(up, application)",
          "multi": true,
          "includeAll": true
        }
      ]
    },
    "panels": [
      {
        "title": "Service Health",
        "type": "stat",
        "gridPos": { "x": 0, "y": 0, "w": 6, "h": 4 },
        "targets": [
          {
            "expr": "sum(up{application=~\"$service\"})",
            "legendFormat": "Healthy Services"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "mappings": [],
            "thresholds": {
              "steps": [
                { "color": "red", "value": 0 },
                { "color": "green", "value": 1 }
              ]
            }
          }
        }
      },
      {
        "title": "Request Rate",
        "type": "timeseries",
        "gridPos": { "x": 0, "y": 4, "w": 12, "h": 8 },
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{application=~\"$service\"}[5m])) by (application)",
            "legendFormat": "{{ application }}"
          }
        ]
      },
      {
        "title": "Response Time (P95)",
        "type": "timeseries",
        "gridPos": { "x": 12, "y": 4, "w": 12, "h": 8 },
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application=~\"$service\"}[5m])) by (le, application))",
            "legendFormat": "{{ application }}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "timeseries",
        "gridPos": { "x": 0, "y": 12, "w": 12, "h": 8 },
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{application=~\"$service\",status=~\"5..\"}[5m])) by (application) / sum(rate(http_server_requests_seconds_count{application=~\"$service\"}[5m])) by (application) * 100",
            "legendFormat": "{{ application }}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "percent",
            "thresholds": {
              "steps": [
                { "color": "green", "value": 0 },
                { "color": "yellow", "value": 1 },
                { "color": "red", "value": 5 }
              ]
            }
          }
        }
      }
    ]
  }
}
```

---

## 7. 실습 예제

### 7.1 Docker Compose로 Observability 스택 구성

```yaml
# docker-compose.observability.yml
version: '3.8'

services:
  # ==================== Metrics ====================
  prometheus:
    image: prom/prometheus:v2.48.0
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./observability/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./observability/prometheus/alert_rules.yml:/etc/prometheus/alert_rules.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.enable-lifecycle'
    networks:
      - observability

  alertmanager:
    image: prom/alertmanager:v0.26.0
    container_name: alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./observability/alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    networks:
      - observability

  # ==================== Tracing ====================
  zipkin:
    image: openzipkin/zipkin:2.26
    container_name: zipkin
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem
    networks:
      - observability

  # ==================== Logging ====================
  loki:
    image: grafana/loki:2.9.0
    container_name: loki
    ports:
      - "3100:3100"
    volumes:
      - ./observability/loki/loki-config.yml:/etc/loki/local-config.yaml
      - loki_data:/loki
    command: -config.file=/etc/loki/local-config.yaml
    networks:
      - observability

  promtail:
    image: grafana/promtail:2.9.0
    container_name: promtail
    volumes:
      - ./observability/promtail/promtail-config.yml:/etc/promtail/config.yml
      - /var/log:/var/log:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
    command: -config.file=/etc/promtail/config.yml
    networks:
      - observability

  # ==================== Visualization ====================
  grafana:
    image: grafana/grafana:10.2.0
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - ./observability/grafana/provisioning:/etc/grafana/provisioning
      - grafana_data:/var/lib/grafana
    depends_on:
      - prometheus
      - loki
      - zipkin
    networks:
      - observability

volumes:
  prometheus_data:
  loki_data:
  grafana_data:

networks:
  observability:
    driver: bridge
```

### 7.2 Grafana Datasource Provisioning

```yaml
# observability/grafana/provisioning/datasources/datasources.yml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false

  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    editable: false

  - name: Zipkin
    type: zipkin
    access: proxy
    url: http://zipkin:9411
    editable: false
```

### 7.3 Spring Boot 서비스 통합

**ObservabilityConfig.java**:

```java
@Configuration
public class ObservabilityConfig {

    @Bean
    public ObservationRegistry observationRegistry(
            MeterRegistry meterRegistry,
            Tracer tracer) {

        ObservationRegistry registry = ObservationRegistry.create();

        // Micrometer 메트릭 핸들러
        registry.observationConfig()
            .observationHandler(new DefaultMeterObservationHandler(meterRegistry));

        // 로깅 핸들러 (요청/응답 로깅)
        registry.observationConfig()
            .observationHandler(new ObservationTextPublisher());

        return registry;
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
```

**@Timed 어노테이션 사용**:

```java
@Service
@Slf4j
public class ProductService {

    @Timed(value = "product.search",
           description = "Product search duration",
           histogram = true)
    public List<Product> searchProducts(String keyword) {
        log.info("Searching products with keyword: {}", keyword);
        // 검색 로직
        return products;
    }

    @Timed(value = "product.get",
           extraTags = {"type", "single"})
    public Product getProduct(String id) {
        // 조회 로직
        return product;
    }
}
```

### 7.4 Kubernetes 배포 설정

**ServiceMonitor (Prometheus Operator)**:

```yaml
# k8s/observability/service-monitor.yml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: spring-boot-services
  namespace: monitoring
  labels:
    release: prometheus
spec:
  selector:
    matchLabels:
      app.kubernetes.io/part-of: portal-universe
  namespaceSelector:
    matchNames:
      - portal-universe
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
      scrapeTimeout: 10s
```

**Pod Annotations**:

```yaml
# k8s/services/shopping-service.yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: shopping-service
spec:
  template:
    metadata:
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
    spec:
      containers:
        - name: shopping-service
          image: portal-universe/shopping-service:latest
          ports:
            - containerPort: 8080
              name: http
          env:
            - name: MANAGEMENT_ZIPKIN_TRACING_ENDPOINT
              value: "http://zipkin.monitoring:9411/api/v2/spans"
            - name: MANAGEMENT_TRACING_SAMPLING_PROBABILITY
              value: "0.1"  # Production: 10% 샘플링
```

### 7.5 Correlation: Logs ↔ Traces ↔ Metrics

**Grafana에서 세 가지 신호 연결**:

```
1. Dashboard에서 에러 스파이크 발견 (Metrics)
   └─▶ "Explore" 클릭

2. 해당 시간대의 에러 로그 검색 (Logs - Loki)
   {app="shopping-service"} |= "ERROR" | json
   └─▶ traceId 확인

3. Trace ID로 요청 흐름 분석 (Traces - Zipkin)
   traceId = "abc123def456"
   └─▶ 어떤 서비스에서 에러 발생했는지 확인
```

**Grafana Exemplars 활용**:

```yaml
# application.yml
management:
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
  tracing:
    sampling:
      probability: 1.0  # Exemplar를 위해 tracing 활성화
```

Grafana에서 Prometheus 메트릭 그래프에서 직접 Trace로 점프할 수 있습니다.

---

## 요약

| 영역 | 도구 | Portal Universe 적용 |
|------|------|---------------------|
| **Metrics** | Prometheus + Grafana | Micrometer + Actuator |
| **Logs** | Loki + Grafana | Logback JSON + MDC |
| **Traces** | Zipkin | Micrometer Tracing + Brave |
| **Alerts** | Alertmanager | Slack/PagerDuty 연동 |

**핵심 원칙**:
1. **Correlation**: traceId로 Logs, Metrics, Traces 연결
2. **Standardization**: 일관된 라벨링 및 메타데이터
3. **Automation**: 자동 계측 (Auto-instrumentation) 활용
4. **Actionable Alerts**: 실행 가능한 알림 설계

---

## 다음 단계

- [Kafka 기초](../kafka/kafka-basics.md) - 이벤트 기반 통신에서의 Observability
- [Kubernetes 배포](../infra/kubernetes-deployment.md) - K8s 환경 Observability 설정
- [Security 기초](../security/security-basics.md) - 보안 이벤트 모니터링
