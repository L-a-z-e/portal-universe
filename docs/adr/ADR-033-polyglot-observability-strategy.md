# ADR-033: Polyglot 서비스 관찰성(Observability) 통일 전략

**Status**: Accepted
**Date**: 2026-02-11
**Author**: Laze
**Supersedes**: -

## Context

Portal Universe는 Java/Spring, NestJS, Python FastAPI로 구성된 polyglot 마이크로서비스 아키텍처이다. 현재 Java 5개 서비스(api-gateway, auth, blog, shopping, notification)는 Logback+LogstashEncoder(JSON 로그), Micrometer(Prometheus 메트릭), Brave(Zipkin 분산 추적)를 통해 완전한 관찰성을 갖추었으나, NestJS(prism-service)와 Python(chatbot-service)는 기본 텍스트 로깅만 존재하여 운영 가시성에 큰 격차가 있다. 이로 인해 서비스 간 요청 추적 불가, 언어별 대시보드 파편화, 장애 시 근본 원인 파악 지연 등의 문제가 발생한다.

## Decision

**각 언어 네이티브 관찰성 도구를 채택하되, 공통 포맷 표준(JSON 로그, Prometheus 메트릭, Zipkin 추적)으로 백엔드를 통일한다.**

- **Java 서비스**: 현행 유지 (Micrometer + Brave + Logback JSON)
- **NestJS(prism-service)**: OTel SDK 통합(메트릭+추적) + `winston`(JSON 로그)
  - `@opentelemetry/sdk-node` + `@opentelemetry/auto-instrumentations-node`
  - `@opentelemetry/exporter-prometheus` (메트릭 → Prometheus, 포트 9464)
  - `@opentelemetry/exporter-zipkin` (추적 → Zipkin)
  - `winston` + `nest-winston` (JSON 로깅)
  - `instrumentation.ts` → `main.ts` import 순서: instrumentation이 반드시 먼저 로드되어야 auto-instrumentation 동작
- **Python(chatbot-service)**: `prometheus_client`(메트릭) + `opentelemetry-python`(추적) + `python-json-logger`(JSON 로그)
- **공통 백엔드**: Prometheus(:9090), Zipkin(:9411), Elasticsearch(:9200)

### NestJS OTel SDK 통합 근거

초기 설계에서 `@willsoto/nestjs-prometheus` + `prom-client` 조합을 검토했으나, OTel SDK 통합으로 변경한 이유:

1. **이중 계측 방지**: `nestjs-prometheus`와 OTel auto-instrumentation이 동시에 HTTP 메트릭을 생성하여 중복 발생
2. **단일 SDK**: 메트릭과 추적을 하나의 SDK(`@opentelemetry/sdk-node`)로 통합 관리
3. **자동 계측**: kafkajs, TypeORM(pg), express가 auto-instrumentation으로 자동 계측
4. **Prometheus exporter**: OTel SDK가 별도 포트(9464)에서 `/metrics`를 직접 노출하므로 별도 라이브러리 불필요

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① 각 언어 네이티브 도구 (채택, Java만) | 에코시스템 검증된 도구, Java 기존 투자 보존 | NestJS/Python에서 이중 계측 위험 |
| ② OTel SDK (NestJS/Python에 채택) | 벤더 독립, W3C 표준, 메트릭+추적 단일 SDK | Java Micrometer→OTel 마이그레이션 비용 (현재 불필요) |
| ③ Sidecar 패턴 (Envoy) | 코드 수정 최소, 언어 무관 | K8s 필수, 애플리케이션 내부 계측 불가, 복잡도 증가 |

**최종 전략**: Java는 ① 유지, NestJS/Python은 ② OTel SDK 채택 (하이브리드)

## Rationale

- Java 5개 서비스의 Micrometer+Brave 투자(logback-spring.xml 200줄, application.yml prometheus/zipkin 설정 등)를 폐기하는 것은 비용 대비 효과가 낮다
- NestJS/Python은 OTel SDK가 메트릭+추적을 단일 SDK로 제공하여 의존성과 설정이 간결하다
- 백엔드(Prometheus, Zipkin, ELK)를 통일하면 Grafana 단일 대시보드에서 언어 무관하게 메트릭 조회 가능하다
- OpenTelemetry Collector는 현 시점에 불필요한 인프라 복잡도를 추가한다 (직접 export로 충분)
- Trace Context 전파는 HTTP 헤더(`traceparent`)를 통해 자동 구현되며, OTel auto-instrumentation이 이를 처리한다

## Trade-offs

**장점**:
- Java 서비스의 기존 설정 유지로 마이그레이션 비용 제로
- NestJS/Python은 OTel SDK 단일 의존성으로 메트릭+추적 통합
- 백엔드 통일로 Grafana 대시보드 단일 진입점 확보
- 로컬 개발 환경(docker-compose-local.yml)에서도 즉시 적용 가능

**단점 및 완화**:
- 언어별 설정 파일 파편화(logback-spring.xml, winston config, logging config) → (완화: 공통 JSON 로그 포맷 명세를 이 ADR에 명시하여 표준 기준선 역할 수행)
- Spring Micrometer 메트릭명(`http_server_requests_seconds_count`)과 OTel/prometheus_client 메트릭명(`http_request_duration_seconds`)이 다름 → (완화: Alert rule에 `or` 절 추가, Grafana 대시보드에 양쪽 메트릭 쿼리 병합)
- Java와 NestJS/Python의 SDK가 다름 (Micrometer vs OTel) → (완화: 백엔드가 동일하므로 운영자 관점에서는 통일됨. 향후 Java도 OTel 마이그레이션 시 SDK만 교체)

## 환경별 설정 분기

| 환경 | Logging | Metrics | Tracing |
|------|---------|---------|---------|
| local | Console text + File JSON | `/metrics` 활성화 | 비활성화 (`OTEL_TRACES_EXPORTER=none`) |
| docker | Console JSON | `/metrics` 활성화 | Zipkin 활성화 (`http://zipkin:9411/api/v2/spans`) |
| k8s | Console JSON | `/metrics` 활성화 | Zipkin 활성화 |

**Java 서비스 local profile**: `management.tracing.sampling.probability=1.0`, `management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans`로 로컬에서도 Zipkin 추적 가능.

## Grafana/Alert 호환성

### 메트릭명 차이

| 계측 | HTTP 요청 수 | HTTP 지연 시간 |
|------|-------------|---------------|
| Spring Micrometer | `http_server_requests_seconds_count` | `http_server_requests_seconds_bucket` |
| OTel (NestJS) | `http_server_duration_seconds_count` | `http_server_duration_seconds_bucket` |
| prometheus_client (Python) | `http_requests_total` | `http_request_duration_seconds_bucket` |

### Alert Rule 대응

기존 alert rule이 `http_server_requests_seconds_count` (Spring Micrometer)에만 의존하므로, polyglot 메트릭을 포함하는 `or` 절을 추가한다.

```promql
# 예: HighErrorRate
(
  sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)
  / sum(rate(http_server_requests_seconds_count[5m])) by (job)
)
or
(
  sum(rate(http_requests_total{status=~"5.."}[5m])) by (job)
  / sum(rate(http_requests_total[5m])) by (job)
)
> 0.05
```

### Prometheus Scrape Config

NestJS(prism-service)와 Python(chatbot-service)를 scrape 대상에 추가한다.

- prism-service: OTel Prometheus exporter 포트 9464, `/metrics`
- chatbot-service: FastAPI 앱 포트 8086, `/metrics`

## Implementation

### 1. NestJS(prism-service)

**패키지 추가**:
```json
{
  "dependencies": {
    "@opentelemetry/sdk-node": "^0.57.0",
    "@opentelemetry/auto-instrumentations-node": "^0.56.0",
    "@opentelemetry/exporter-prometheus": "^0.57.0",
    "@opentelemetry/exporter-zipkin": "^1.30.0",
    "winston": "^3.17.0",
    "nest-winston": "^1.10.2"
  }
}
```

**주요 변경 파일**:
- `services/prism-service/src/instrumentation.ts` (신규) - OTel SDK 초기화 (메트릭+추적)
- `services/prism-service/src/main.ts` - 최상단 `import './instrumentation'` + Winston logger 설정
- `services/prism-service/src/config/logger.config.ts` (신규) - Winston JSON 포맷 설정

**로드 순서**: `instrumentation.ts` → `main.ts` (auto-instrumentation이 모듈 로드 전에 패치해야 동작)

**표준 메트릭 노출**: OTel Prometheus exporter → 포트 9464, `/metrics`

**JSON 로그 포맷**:
```json
{
  "timestamp": "2026-02-11T12:34:56.789Z",
  "level": "info",
  "service_name": "prism-service",
  "traceId": "abc123...",
  "spanId": "def456...",
  "message": "Request processed",
  "context": "PrismController"
}
```

### 2. Python(chatbot-service)

**의존성 추가**:
```toml
[project]
dependencies = [
    "python-json-logger>=4.0.0",
    "prometheus-client>=0.21.0",
    "opentelemetry-sdk>=1.30.0",
    "opentelemetry-instrumentation-fastapi>=0.51b0",
    "opentelemetry-exporter-zipkin-json>=1.30.0",
]
```

**주요 변경 파일**:
- `services/chatbot-service/app/core/logging_config.py` - `logging.basicConfig` → JsonFormatter 교체
- `services/chatbot-service/app/core/metrics.py` (신규) - Prometheus Counter/Histogram 정의
- `services/chatbot-service/app/core/telemetry.py` (신규) - OTel TracerProvider, ZipkinExporter 설정
- `services/chatbot-service/app/core/config.py` - tracing 설정 필드 추가
- `services/chatbot-service/app/main.py` - 메트릭/추적 초기화, `/metrics` 엔드포인트

**표준 메트릭 노출 엔드포인트**: `GET /metrics` (포트 8086)

**JSON 로그 포맷**:
```json
{
  "timestamp": "2026-02-11T12:34:56.789Z",
  "level": "INFO",
  "service_name": "chatbot-service",
  "traceId": "abc123...",
  "spanId": "def456...",
  "message": "LLM request completed",
  "name": "app.services.llm_service"
}
```

### 3. Java 서비스 (변경 사항)

현행 Micrometer+Brave 유지. Local profile에 Zipkin tracing 설정 추가:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

### 4. Prometheus 스크랩 설정

```yaml
scrape_configs:
  # 기존 Spring 서비스 유지

  - job_name: 'prism-service'
    metrics_path: /metrics
    static_configs:
      - targets: ['prism-service:9464']  # OTel Prometheus exporter 포트

  - job_name: 'chatbot-service'
    metrics_path: /metrics
    static_configs:
      - targets: ['chatbot-service:8086']
```

### 5. 공통 JSON 로그 포맷 표준

| 필드 | 타입 | 필수 | 설명 | Java | NestJS | Python |
|------|------|------|------|------|--------|--------|
| `timestamp` | ISO8601 | O | UTC 시간 | `yyyy-MM-dd'T'HH:mm:ss.SSSXXX` | `YYYY-MM-DDTHH:mm:ss.SSSZ` | `%Y-%m-%dT%H:%M:%S.%fZ` |
| `level` | string | O | 로그 레벨 | `INFO`, `ERROR` | `info`, `error` | `INFO`, `ERROR` |
| `service_name` | string | O | 서비스 이름 | `spring.application.name` | `prism-service` | `chatbot-service` |
| `traceId` | string | O | 분산 추적 ID | MDC | OTel Context | OTel Context |
| `spanId` | string | O | Span ID | MDC | OTel Context | OTel Context |
| `message` | string | O | 로그 메시지 | logger.info() | logger.log() | logging.info() |
| `logger_name` | string | - | 로거 이름 | `logger` | `context` | `name` |
| `stack_trace` | string | - | 예외 스택 | throwable | error.stack | exc_info |

**Elasticsearch Index 매핑**: 모든 서비스 로그를 `logs-*` 인덱스에 수집, Kibana에서 `service_name` 필터로 조회

## References

- [ADR-032: Kafka Configuration Standardization](./ADR-032-kafka-configuration-standardization.md) - Polyglot 설정 통일 유사 사례
- `services/api-gateway/src/main/resources/logback-spring.xml` - Java JSON 로그 기준
- `services/shopping-service/src/main/resources/application-local.yml` - Prometheus/Zipkin 설정 기준
- [OpenTelemetry JS](https://opentelemetry.io/docs/languages/js/) - NestJS OTel 공식 가이드
- [OpenTelemetry Python](https://opentelemetry.io/docs/languages/python/) - Python OTel 공식 가이드

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-11 | 초안 작성 | Laze |
| 2026-02-11 | Accepted: NestJS → OTel SDK 통합, Python 버전 최신화, 환경별 설정/Alert 호환성 섹션 추가 | Laze |
