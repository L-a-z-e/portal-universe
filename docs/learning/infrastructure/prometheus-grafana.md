# Prometheus & Grafana

메트릭 수집과 시각화를 위한 Prometheus와 Grafana를 학습합니다.

---

## 1. 개요

### Prometheus란?

**Prometheus**는 시계열(time-series) 데이터베이스 기반의 모니터링 시스템입니다.

**특징:**
- Pull 기반 메트릭 수집
- PromQL 쿼리 언어
- 서비스 디스커버리
- Alert 규칙 정의

### Grafana란?

**Grafana**는 메트릭 데이터를 시각화하는 대시보드 플랫폼입니다.

**특징:**
- 다양한 데이터소스 지원
- 풍부한 시각화 옵션
- 알림 설정
- 대시보드 공유/템플릿

### 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                    Monitoring Architecture                       │
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │ auth-service │    │ blog-service │    │  shopping    │      │
│  │   :8081      │    │    :8082     │    │   :8083      │      │
│  │ /actuator/   │    │ /actuator/   │    │ /actuator/   │      │
│  │  prometheus  │    │  prometheus  │    │  prometheus  │      │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘      │
│         │                   │                   │               │
│         └───────────────────┼───────────────────┘               │
│                             │ Pull (15s interval)               │
│                             ▼                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Prometheus :9090                              │  │
│  │  - Time Series Database                                    │  │
│  │  - PromQL Query Engine                                     │  │
│  │  - Alert Rules                                             │  │
│  └─────────────────────────┬────────────────────────────────┘  │
│                            │ Query                              │
│                            ▼                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Grafana :3000                                 │  │
│  │  - Dashboards                                              │  │
│  │  - Visualization                                           │  │
│  │  - Alerts                                                  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Alertmanager :9093                            │  │
│  │  - Alert Routing                                           │  │
│  │  - Notification (Slack, Email, etc.)                       │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Portal Universe Prometheus 설정

### docker-compose.yml

```yaml
prometheus:
  image: prom/prometheus:latest
  container_name: prometheus
  volumes:
    - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    - ./monitoring/prometheus/rules:/etc/prometheus/rules
    - prometheus-data:/prometheus
  ports:
    - "9090:9090"
  networks:
    - portal-universe-net
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.path=/prometheus'
    - '--storage.tsdb.retention.time=30d'   # 30일 보관
    - '--web.enable-lifecycle'               # 설정 리로드 API 활성화
    - '--web.enable-admin-api'               # 관리 API 활성화
```

### prometheus.yml 설정

```yaml
# monitoring/prometheus/prometheus.yml
global:
  scrape_interval: 15s          # 메트릭 수집 주기
  evaluation_interval: 15s      # 알림 규칙 평가 주기

# Alertmanager 연동
alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093

# 알림 규칙 파일
rule_files:
  - /etc/prometheus/rules/*.yml

# 스크레이프 설정
scrape_configs:
  # Prometheus 자체 메트릭
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # API Gateway 메트릭
  - job_name: 'api-gateway'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['api-gateway:8080']

  # Auth Service 메트릭
  - job_name: 'auth-service'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['auth-service:8081']

  # Blog Service 메트릭
  - job_name: 'blog-service'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['blog-service:8082']

  # Shopping Service 메트릭
  - job_name: 'shopping-service'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['shopping-service:8083']

  # Notification Service 메트릭
  - job_name: 'notification-service'
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['notification-service:8084']
```

### Kubernetes Service Discovery

```yaml
# k8s/infrastructure/prometheus.yaml (ConfigMap 부분)
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: portal-universe
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s

    scrape_configs:
      # Kubernetes Pod 자동 발견
      - job_name: 'kubernetes-pods'
        kubernetes_sd_configs:
          - role: pod
            namespaces:
              names:
                - portal-universe

        relabel_configs:
          # prometheus.io/scrape: "true" 어노테이션이 있는 Pod만 수집
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
            action: keep
            regex: true

          # 메트릭 경로 설정
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
            action: replace
            target_label: __metrics_path__
            regex: (.+)

          # 포트 설정
          - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
            action: replace
            regex: ([^:]+)(?::\d+)?;(\d+)
            replacement: $1:$2
            target_label: __address__
```

---

## 3. Spring Boot Actuator 설정

### 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
}
```

### application.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
```

### Kubernetes Deployment 어노테이션

```yaml
spec:
  template:
    metadata:
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8081"
```

---

## 4. Grafana 설정

### docker-compose.yml

```yaml
grafana:
  image: grafana/grafana-oss:latest
  container_name: grafana
  env_file:
    - .env.docker
  environment:
    - GF_USERS_ALLOW_SIGN_UP=false
    - GF_SERVER_ROOT_URL=http://localhost:3000
    - GF_INSTALL_PLUGINS=grafana-piechart-panel
  volumes:
    - grafana-data:/var/lib/grafana
    - ./monitoring/grafana/provisioning:/etc/grafana/provisioning
  ports:
    - "3000:3000"
  networks:
    - portal-universe-net
  depends_on:
    - prometheus
    - loki
```

### Datasource Provisioning

```yaml
# monitoring/grafana/provisioning/datasources/prometheus.yml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
    jsonData:
      timeInterval: "15s"
      httpMethod: POST
```

### 기본 접속 정보

| 항목 | 값 |
|------|-----|
| URL | http://localhost:3000 |
| Username | admin |
| Password | admin (또는 .env.docker에서 설정) |

---

## 5. PromQL 기본 쿼리

### 메트릭 유형

| 유형 | 설명 | 예시 |
|------|------|------|
| Counter | 누적 증가값 | 요청 수, 에러 수 |
| Gauge | 현재 값 | CPU 사용률, 메모리 |
| Histogram | 분포 측정 | 응답 시간 분포 |
| Summary | 백분위 계산 | 지연 시간 백분위 |

### 기본 쿼리 예시

```promql
# 현재 HTTP 요청 수
http_server_requests_seconds_count

# 특정 서비스의 요청 수
http_server_requests_seconds_count{application="auth-service"}

# 5분간 초당 요청 수 (Rate)
rate(http_server_requests_seconds_count[5m])

# 서비스별 초당 요청 수
sum by (application) (rate(http_server_requests_seconds_count[5m]))

# 95th percentile 응답 시간
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# 에러율 (4xx + 5xx)
sum(rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])) /
sum(rate(http_server_requests_seconds_count[5m])) * 100

# JVM 메모리 사용량
jvm_memory_used_bytes{area="heap"}

# JVM GC 시간
rate(jvm_gc_pause_seconds_sum[5m])
```

### Aggregation 연산자

```promql
# 합계
sum(http_server_requests_seconds_count)

# 평균
avg(http_server_requests_seconds_count)

# 최대/최소
max(jvm_memory_used_bytes)
min(jvm_memory_used_bytes)

# 레이블별 그룹화
sum by (application, method) (http_server_requests_seconds_count)

# 특정 레이블 제외
sum without (instance) (http_server_requests_seconds_count)
```

---

## 6. Alerting 설정

### Alert Rules

```yaml
# monitoring/prometheus/rules/alerts.yml
groups:
  - name: portal-universe-alerts
    rules:
      # 서비스 다운 알림
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"
          description: "{{ $labels.instance }} has been down for more than 1 minute."

      # 높은 에러율 알림
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application)
          /
          sum(rate(http_server_requests_seconds_count[5m])) by (application)
          > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate on {{ $labels.application }}"
          description: "Error rate is above 10% for 5 minutes."

      # 느린 응답 시간
      - alert: SlowResponseTime
        expr: |
          histogram_quantile(0.95,
            sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application)
          ) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow response time on {{ $labels.application }}"
          description: "95th percentile response time is above 2 seconds."

      # JVM 메모리 부족
      - alert: HighMemoryUsage
        expr: |
          jvm_memory_used_bytes{area="heap"}
          /
          jvm_memory_max_bytes{area="heap"}
          > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage on {{ $labels.application }}"
          description: "Heap memory usage is above 90%."
```

### Alertmanager 설정

```yaml
# monitoring/alertmanager/alertmanager.yml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'slack-notifications'

  routes:
    - match:
        severity: critical
      receiver: 'slack-critical'

receivers:
  - name: 'slack-notifications'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/xxx/yyy/zzz'
        channel: '#alerts'
        send_resolved: true

  - name: 'slack-critical'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/xxx/yyy/zzz'
        channel: '#alerts-critical'
        send_resolved: true
```

---

## 7. Grafana 대시보드

### JVM 메트릭 대시보드 JSON

```json
{
  "title": "JVM Metrics",
  "panels": [
    {
      "title": "Heap Memory Usage",
      "type": "timeseries",
      "targets": [
        {
          "expr": "jvm_memory_used_bytes{area=\"heap\"}",
          "legendFormat": "{{ application }} - Used"
        },
        {
          "expr": "jvm_memory_max_bytes{area=\"heap\"}",
          "legendFormat": "{{ application }} - Max"
        }
      ]
    },
    {
      "title": "GC Pause Time",
      "type": "timeseries",
      "targets": [
        {
          "expr": "rate(jvm_gc_pause_seconds_sum[1m])",
          "legendFormat": "{{ application }} - {{ gc }}"
        }
      ]
    },
    {
      "title": "Threads",
      "type": "stat",
      "targets": [
        {
          "expr": "jvm_threads_live_threads",
          "legendFormat": "{{ application }}"
        }
      ]
    }
  ]
}
```

### 공개 대시보드 활용

| 대시보드 ID | 이름 | 용도 |
|------------|------|------|
| 4701 | JVM Micrometer | JVM 메트릭 |
| 11378 | Spring Boot 2.1 | Spring Boot 전반 |
| 10280 | Spring Boot Statistics | 요청 통계 |
| 12708 | Kubernetes Cluster | K8s 클러스터 |

```bash
# Grafana에서 Import
1. 좌측 메뉴 > Dashboards > Import
2. Dashboard ID 입력 (예: 4701)
3. Load > Prometheus 데이터소스 선택 > Import
```

---

## 8. 접속 URL (Portal Universe)

### Docker Compose 환경

| 서비스 | URL |
|--------|-----|
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |
| Alertmanager | http://localhost:9094 |

### Kubernetes 환경

| 서비스 | URL |
|--------|-----|
| Prometheus | http://portal-universe/prometheus |
| Grafana | http://portal-universe/grafana |

---

## 9. 관련 문서

- [Loki Logging](./loki-logging.md) - 로그 수집
- [Zipkin Tracing](./zipkin-tracing.md) - 분산 추적
- [Docker Compose](./docker-compose.md) - 멀티 컨테이너 구성
- [Portal Universe Infra Guide](./portal-universe-infra-guide.md) - 전체 인프라 가이드
