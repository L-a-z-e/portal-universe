# Loki Logging

Grafana Loki를 사용한 로그 수집 및 분석을 학습합니다.

---

## 1. Loki 개요

### Loki란?

**Grafana Loki**는 로그 수집 및 쿼리를 위한 수평 확장 가능한 시스템입니다. Prometheus와 유사한 아키텍처를 가지며, 로그의 메타데이터(라벨)만 인덱싱하여 비용 효율적입니다.

### ELK Stack vs Loki

| 특성 | ELK Stack | Loki |
|------|-----------|------|
| 인덱싱 | 전체 텍스트 인덱싱 | 라벨만 인덱싱 |
| 스토리지 | 높은 비용 | 낮은 비용 |
| 복잡도 | 높음 | 낮음 |
| 쿼리 언어 | Lucene | LogQL (PromQL 유사) |
| 통합 | 별도 Kibana | Grafana 통합 |

### Loki 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                      Log Collection Flow                         │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ auth-service │  │ blog-service │  │  shopping    │          │
│  │   (logs)     │  │   (logs)     │  │   (logs)     │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                 │                 │                   │
│         └─────────────────┼─────────────────┘                   │
│                           │ Docker logs                         │
│                           ▼                                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                   Promtail (Agent)                         │  │
│  │  - Docker 로그 수집                                         │  │
│  │  - 라벨 추가 (service, environment)                         │  │
│  │  - 로그 파싱 및 변환                                         │  │
│  └─────────────────────────┬────────────────────────────────┘  │
│                            │ HTTP POST /loki/api/v1/push        │
│                            ▼                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                     Loki :3100                             │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────────────┐  │  │
│  │  │ Distributor│→ │  Ingester  │→ │   Querier          │  │  │
│  │  │            │  │  (Storage) │  │   (LogQL)          │  │  │
│  │  └────────────┘  └────────────┘  └────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                            │ Query                              │
│                            ▼                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  Grafana :3000                             │  │
│  │  - Log Exploration                                         │  │
│  │  - Dashboard                                               │  │
│  │  - Alerting                                                │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Portal Universe Loki 설정

### docker-compose.yml

```yaml
# Loki - 로그 저장소
loki:
  image: grafana/loki:2.9.0
  container_name: loki
  volumes:
    - ./monitoring/loki/loki-config.yml:/etc/loki/local-config.yaml
    - loki-data:/loki
  ports:
    - "3100:3100"
  networks:
    - portal-universe-net
  command: -config.file=/etc/loki/local-config.yaml
  restart: unless-stopped

# Promtail - 로그 수집 에이전트
promtail:
  image: grafana/promtail:2.9.0
  container_name: promtail
  volumes:
    - ./monitoring/promtail/promtail-config.yml:/etc/promtail/config.yml
    - /var/lib/docker/containers:/var/lib/docker/containers:ro
    - /var/run/docker.sock:/var/run/docker.sock
  networks:
    - portal-universe-net
  command: -config.file=/etc/promtail/config.yml
  depends_on:
    - loki
  restart: unless-stopped
```

### Loki Configuration

```yaml
# monitoring/loki/loki-config.yml
auth_enabled: false

server:
  http_listen_port: 3100
  grpc_listen_port: 9096

common:
  instance_addr: 127.0.0.1
  path_prefix: /loki
  storage:
    filesystem:
      chunks_directory: /loki/chunks
      rules_directory: /loki/rules
  replication_factor: 1
  ring:
    kvstore:
      store: inmemory

# 쿼리 결과 캐싱
query_range:
  results_cache:
    cache:
      embedded_cache:
        enabled: true
        max_size_mb: 100

# 스키마 설정
schema_config:
  configs:
    - from: 2020-10-24
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: index_
        period: 24h

# Alertmanager 연동
ruler:
  alertmanager_url: http://alertmanager:9093

# 제한 설정
limits_config:
  reject_old_samples: true
  reject_old_samples_max_age: 168h      # 7일 이전 로그 거부
  ingestion_rate_mb: 16                  # 초당 최대 수집량
  ingestion_burst_size_mb: 24
```

---

## 3. Promtail 설정

### Promtail Configuration

```yaml
# monitoring/promtail/promtail-config.yml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml         # 수집 위치 기록

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  # Docker 컨테이너 로그 수집
  - job_name: containers
    static_configs:
      - targets:
          - localhost
        labels:
          job: containerlogs
          __path__: /var/lib/docker/containers/*/*log

    pipeline_stages:
      # Docker JSON 로그 포맷 파싱
      - json:
          expressions:
            log: log
            stream: stream
            time: time
            attrs: attrs

      # 컨테이너 라벨 추출
      - json:
          expressions:
            container_name: '{{ index .attrs "com.docker.compose.service" }}'
          source: attrs

      # 라벨 추가
      - labels:
          stream:
          container_name:

      # 타임스탬프 파싱
      - timestamp:
          source: time
          format: RFC3339Nano

      # 로그 메시지 출력
      - output:
          source: log

  # Spring Boot 로그 수집
  - job_name: spring-boot
    static_configs:
      - targets:
          - localhost
        labels:
          job: spring-boot
          __path__: /var/log/portal-universe/*.log

    pipeline_stages:
      # Spring Boot 로그 포맷 파싱
      - regex:
          expression: '^(?P<timestamp>\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3})\s+(?P<level>\w+)\s+(?P<pid>\d+)\s+---\s+\[(?P<thread>[^\]]+)\]\s+(?P<logger>[^\s]+)\s+:\s+(?P<message>.*)$'

      - labels:
          level:
          logger:

      - timestamp:
          source: timestamp
          format: '2006-01-02 15:04:05.000'

      - output:
          source: message

  # Docker Service Discovery
  - job_name: portal-services
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
        refresh_interval: 5s
        filters:
          - name: label
            values: ["com.portal-universe.logs=true"]

    relabel_configs:
      - source_labels: ['__meta_docker_container_name']
        regex: '/(.*)'
        target_label: 'container'

      - source_labels: ['__meta_docker_container_label_com_docker_compose_service']
        target_label: 'service'

    pipeline_stages:
      - json:
          expressions:
            log: log
            time: time

      - timestamp:
          source: time
          format: RFC3339Nano

      - output:
          source: log

      # Spring Boot JSON 로그 파싱
      - match:
          selector: '{service=~".+-service"}'
          stages:
            - json:
                expressions:
                  level: level
                  logger: logger_name
                  message: message
                  trace_id: trace_id
                  span_id: span_id

            - labels:
                level:
                logger:
                trace_id:

            - output:
                source: message
```

---

## 4. Spring Boot 로그 설정

### JSON 로그 포맷 (logback-spring.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- JSON 로그 포맷 (Docker/K8s 환경) -->
    <springProfile name="docker,kubernetes">
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

    <!-- 일반 로그 포맷 (로컬 개발) -->
    <springProfile name="local">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%15.15t] [%X{traceId:-},%X{spanId:-}] %-40.40logger{39} : %m%n</pattern>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
</configuration>
```

### 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
}
```

---

## 5. LogQL 쿼리

### 기본 문법

```logql
# 스트림 선택 (라벨 기반)
{container_name="auth-service"}

# 라벨 매처
{container_name="auth-service", level="ERROR"}
{container_name=~".*-service"}              # 정규식 매칭
{container_name!="prometheus"}              # 부정 매칭

# 로그 필터링
{container_name="auth-service"} |= "error"  # 포함
{container_name="auth-service"} != "debug"  # 미포함
{container_name="auth-service"} |~ "(?i)error"  # 정규식
```

### 로그 파싱

```logql
# JSON 파싱
{container_name="auth-service"} | json

# 특정 필드 추출
{container_name="auth-service"}
| json
| level = "ERROR"

# 정규식 파싱
{container_name="auth-service"}
| regexp `(?P<timestamp>\d{4}-\d{2}-\d{2}) (?P<level>\w+) (?P<message>.*)`

# 라인 포맷팅
{container_name="auth-service"}
| json
| line_format "{{.level}} - {{.message}}"
```

### 메트릭 쿼리

```logql
# 초당 로그 수 (Rate)
rate({container_name="auth-service"}[5m])

# 에러 로그 수
count_over_time({container_name="auth-service", level="ERROR"}[1h])

# 서비스별 에러 로그 수
sum by (container_name) (count_over_time({level="ERROR"}[1h]))

# 상위 10개 에러 메시지
topk(10, count_over_time({level="ERROR"} | json | message != "" [1h]) by (message))
```

### 집계 함수

| 함수 | 설명 |
|------|------|
| `count_over_time()` | 시간 범위 내 로그 수 |
| `rate()` | 초당 로그 발생률 |
| `bytes_over_time()` | 시간 범위 내 로그 바이트 |
| `bytes_rate()` | 초당 로그 바이트 |
| `sum()`, `avg()`, `min()`, `max()` | 집계 |
| `topk()`, `bottomk()` | 상위/하위 N개 |

---

## 6. Grafana Loki 데이터소스 설정

### Grafana에서 Loki 데이터소스 추가

```yaml
# monitoring/grafana/provisioning/datasources/loki.yml
apiVersion: 1

datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    editable: false
    jsonData:
      maxLines: 1000
```

### Explore에서 로그 조회

```
1. Grafana 접속 → Explore
2. 데이터소스: Loki 선택
3. Label Browser에서 라벨 선택
4. 쿼리 작성 및 실행
```

---

## 7. 로그 대시보드

### 서비스 로그 대시보드 예시

```json
{
  "title": "Portal Universe Logs",
  "panels": [
    {
      "title": "Log Volume",
      "type": "timeseries",
      "targets": [
        {
          "expr": "sum by (container_name) (rate({job=\"containerlogs\"}[5m]))",
          "legendFormat": "{{ container_name }}"
        }
      ]
    },
    {
      "title": "Error Logs",
      "type": "logs",
      "targets": [
        {
          "expr": "{job=\"containerlogs\"} |= \"ERROR\" | json"
        }
      ]
    },
    {
      "title": "Error Count by Service",
      "type": "stat",
      "targets": [
        {
          "expr": "sum by (container_name) (count_over_time({job=\"containerlogs\"} |= \"ERROR\"[1h]))"
        }
      ]
    }
  ]
}
```

---

## 8. Alerting

### Loki Alert Rule

```yaml
# monitoring/loki/rules/alerts.yml
groups:
  - name: portal-universe-logs
    rules:
      # 에러 로그 급증 알림
      - alert: HighErrorLogRate
        expr: |
          sum(rate({job="containerlogs"} |= "ERROR"[5m])) by (container_name) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error log rate in {{ $labels.container_name }}"
          description: "Error log rate is above 1 per second for 5 minutes."

      # OutOfMemory 에러 감지
      - alert: OutOfMemoryError
        expr: |
          count_over_time({job="containerlogs"} |= "OutOfMemoryError"[5m]) > 0
        labels:
          severity: critical
        annotations:
          summary: "OutOfMemoryError detected"
          description: "OutOfMemoryError found in {{ $labels.container_name }}"
```

---

## 9. Trace 연동

### TraceId로 로그 검색

```logql
# 특정 TraceId의 모든 로그
{job="containerlogs"} |= "abc123def456"

# JSON 파싱 후 TraceId 필터
{job="containerlogs"}
| json
| trace_id = "abc123def456"
```

### Grafana에서 Trace-Log 연동

```
1. Zipkin 데이터소스의 Trace 선택
2. "Logs for this trace" 클릭
3. Loki에서 해당 TraceId 로그 표시
```

---

## 10. 트러블슈팅

### 일반적인 문제

| 문제 | 원인 | 해결 방법 |
|------|------|----------|
| 로그가 수집되지 않음 | Promtail 설정 오류 | `docker logs promtail` 확인 |
| 라벨이 없음 | pipeline_stages 오류 | 로그 포맷과 파서 일치 확인 |
| 쿼리 속도 느림 | 라벨 카디널리티 높음 | 라벨 최적화 |
| 디스크 부족 | 보관 기간 과다 | retention 설정 조정 |

### 디버깅

```bash
# Loki 헬스 체크
curl http://localhost:3100/ready

# Promtail 타겟 확인
curl http://localhost:9080/targets

# Promtail 메트릭
curl http://localhost:9080/metrics

# Loki 로그 스트림 확인
curl http://localhost:3100/loki/api/v1/labels
curl http://localhost:3100/loki/api/v1/label/container_name/values
```

---

## 11. 접속 URL

| 환경 | 서비스 | URL |
|------|--------|-----|
| Docker Compose | Loki API | http://localhost:3100 |
| Docker Compose | Grafana | http://localhost:3000 |
| Kubernetes | Grafana | http://portal-universe/grafana |

---

## 12. 관련 문서

- [Prometheus & Grafana](./prometheus-grafana.md) - 메트릭 모니터링
- [Zipkin Tracing](./zipkin-tracing.md) - 분산 추적
- [Docker Compose](./docker-compose.md) - 멀티 컨테이너 구성
- [Portal Universe Infra Guide](./portal-universe-infra-guide.md) - 전체 인프라 가이드
