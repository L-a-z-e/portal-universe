# ADR-033: Polyglot 서비스 관찰성(Observability) 통일 전략

**Status**: Proposed
**Date**: 2026-02-11
**Author**: Laze
**Supersedes**: -

## Context

Portal Universe는 Java/Spring, NestJS, Python FastAPI로 구성된 polyglot 마이크로서비스 아키텍처이다. 현재 Java 5개 서비스(api-gateway, auth, blog, shopping, notification)는 Logback+LogstashEncoder(JSON 로그), Micrometer(Prometheus 메트릭), Brave(Zipkin 분산 추적)를 통해 완전한 관찰성을 갖추었으나, NestJS(prism-service)와 Python(chatbot-service)는 기본 텍스트 로깅만 존재하여 운영 가시성에 큰 격차가 있다. 이로 인해 서비스 간 요청 추적 불가, 언어별 대시보드 파편화, 장애 시 근본 원인 파악 지연 등의 문제가 발생한다.

## Decision

**각 언어 네이티브 관찰성 도구를 채택하되, 공통 포맷 표준(JSON 로그, Prometheus 메트릭, Zipkin 추적)으로 백엔드를 통일한다.**

- **Java 서비스**: 현행 유지 (Micrometer + Brave + Logback JSON)
- **NestJS(prism-service)**: `nestjs-prom`(메트릭) + `@opentelemetry/sdk-node`(추적) + `winston`(JSON 로그)
- **Python(chatbot-service)**: `prometheus_client`(메트릭) + `opentelemetry-python`(추적) + `python-json-logger`(JSON 로그)
- **공통 백엔드**: Prometheus(:9090), Zipkin(:9411), Elasticsearch(:9200)

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① 각 언어 네이티브 도구 (채택) | 에코시스템 검증된 도구, Java 기존 투자 보존, 학습 비용 낮음 | 설정/대시보드 통일 노력 필요, Trace Context 전파 수동 구현 |
| ② OpenTelemetry SDK 통일 | 벤더 독립, W3C 표준, Collector로 백엔드 교체 자유 | Java Micrometer→OTel 마이그레이션 비용, 학습 비용, Collector 인프라 추가 |
| ③ Sidecar 패턴 (Envoy) | 코드 수정 최소, 언어 무관 | K8s 필수(로컬 개발 불편), 애플리케이션 내부 계측 불가, 복잡도 증가 |

## Rationale

- Java 5개 서비스의 Micrometer+Brave 투자(logback-spring.xml 200줄, application.yml prometheus/zipkin 설정 등)를 폐기하는 것은 비용 대비 효과가 낮다
- NestJS/Python은 각 언어의 사실상 표준 도구(nestjs-prom, prometheus_client, OpenTelemetry)가 이미 검증되어 있다
- 백엔드(Prometheus, Zipkin, ELK)를 통일하면 Grafana 단일 대시보드에서 언어 무관하게 메트릭 조회 가능하다
- OpenTelemetry Collector는 현 시점에 불필요한 인프라 복잡도를 추가한다 (직접 export로 충분)
- Trace Context 전파는 HTTP 헤더(`traceparent`)를 통해 수동 구현 가능하며, 서비스 간 호출이 제한적(주로 Feign Client)이므로 커스터마이징 부담이 크지 않다

## Trade-offs

✅ **장점**:
- Java 서비스의 기존 설정 유지로 마이그레이션 비용 제로
- 각 언어 커뮤니티에서 검증된 도구 사용으로 문제 해결 자료 풍부
- 백엔드 통일로 Grafana 대시보드 단일 진입점 확보
- 로컬 개발 환경(docker-compose-local.yml)에서도 즉시 적용 가능

⚠️ **단점 및 완화**:
- Trace Context 전파를 수동 구현해야 함 → (완화: Feign Client Interceptor(Java), Axios Interceptor(NestJS)에 `traceparent` 헤더 추가 로직 캡슐화. 약 50줄 이내로 구현 가능)
- 언어별 설정 파일 파편화(logback-spring.xml, winston config, logging config) → (완화: 공통 JSON 로그 포맷 명세를 이 ADR에 명시하여 표준 기준선 역할 수행)
- OpenTelemetry로의 향후 마이그레이션 시 NestJS/Python 재작업 필요 → (완화: OpenTelemetry SDK는 Prometheus/Zipkin exporter를 지원하므로 백엔드 변경 없이 SDK만 교체 가능. 점진적 전환 가능)

## Implementation

### 1. NestJS(prism-service)

**패키지 추가**:
```json
{
  "dependencies": {
    "@willsoto/nestjs-prometheus": "^6.0.0",
    "prom-client": "^15.0.0",
    "@opentelemetry/sdk-node": "^0.46.0",
    "@opentelemetry/auto-instrumentations-node": "^0.40.0",
    "@opentelemetry/exporter-zipkin": "^1.19.0",
    "winston": "^3.11.0",
    "nest-winston": "^1.9.4"
  }
}
```

**주요 변경 파일**:
- `services/prism-service/src/main.ts` - OTel SDK 초기화 (bootstrap 전)
- `services/prism-service/src/app.module.ts` - PrometheusModule.register()
- `services/prism-service/src/config/logger.config.ts` (신규) - Winston JSON 포맷 설정
- `services/prism-service/src/common/interceptors/trace-context.interceptor.ts` (신규) - HTTP 요청 시 traceparent 헤더 전파

**표준 메트릭 노출 엔드포인트**: `GET /metrics` (Prometheus scrape 대상)

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
[tool.poetry.dependencies]
prometheus-client = "^0.19.0"
opentelemetry-api = "^1.22.0"
opentelemetry-sdk = "^1.22.0"
opentelemetry-instrumentation-fastapi = "^0.43b0"
opentelemetry-exporter-zipkin-json = "^1.22.0"
python-json-logger = "^2.0.7"
```

**주요 변경 파일**:
- `services/chatbot-service/app/core/telemetry.py` (신규) - OTel SDK 초기화, TracerProvider, ZipkinExporter 설정
- `services/chatbot-service/app/core/logging_config.py` - `logging.basicConfig` → JsonFormatter 교체
- `services/chatbot-service/app/main.py` - FastAPIInstrumentor.instrument_app(), `/metrics` 엔드포인트 추가
- `services/chatbot-service/app/core/metrics.py` (신규) - Prometheus Counter/Histogram 정의

**표준 메트릭 노출 엔드포인트**: `GET /metrics` (Prometheus scrape 대상)

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

### 3. Java 서비스 (변경 없음)

현재 설정 유지:
- `logback-spring.xml` - LogstashEncoder (traceId/spanId MDC 포함)
- `application-*.yml` - `management.prometheus.metrics.export.enabled: true`, `management.zipkin.tracing.endpoint`
- Brave auto-configuration (spring-cloud-starter-sleuth)

### 4. Prometheus 스크랩 설정 (docker-compose.yml)

```yaml
# monitoring/prometheus/prometheus.yml
scrape_configs:
  - job_name: 'spring-services'
    static_configs:
      - targets: ['api-gateway:8080', 'auth-service:8081', 'blog-service:8082', 'shopping-service:8083', 'notification-service:8084']
    metrics_path: '/actuator/prometheus'

  - job_name: 'prism-service'
    static_configs:
      - targets: ['prism-service:8085']
    metrics_path: '/metrics'

  - job_name: 'chatbot-service'
    static_configs:
      - targets: ['chatbot-service:8086']
    metrics_path: '/metrics'
```

### 5. 공통 JSON 로그 포맷 표준

| 필드 | 타입 | 필수 | 설명 | Java | NestJS | Python |
|------|------|------|------|------|--------|--------|
| `timestamp` | ISO8601 | ✅ | UTC 시간 | `yyyy-MM-dd'T'HH:mm:ss.SSSXXX` | `YYYY-MM-DDTHH:mm:ss.SSSZ` | `%Y-%m-%dT%H:%M:%S.%fZ` |
| `level` | string | ✅ | 로그 레벨 | `INFO`, `ERROR` | `info`, `error` | `INFO`, `ERROR` |
| `service_name` | string | ✅ | 서비스 이름 | `spring.application.name` | `PRISM_SERVICE` | `settings.service_name` |
| `traceId` | string | ✅ | 분산 추적 ID | MDC | OTel Context | OTel Context |
| `spanId` | string | ✅ | Span ID | MDC | OTel Context | OTel Context |
| `message` | string | ✅ | 로그 메시지 | logger.info() | logger.log() | logging.info() |
| `logger_name` | string | ❌ | 로거 이름 | `logger` | `context` | `name` |
| `stack_trace` | string | ❌ | 예외 스택 | throwable | error.stack | exc_info |

**Elasticsearch Index 매핑**: 모든 서비스 로그를 `logs-*` 인덱스에 수집, Kibana에서 `service_name` 필터로 조회

## References

- [ADR-032: Kafka Configuration Standardization](./ADR-032-kafka-configuration-standardization.md) - Polyglot 설정 통일 유사 사례
- `services/api-gateway/src/main/resources/logback-spring.xml` - Java JSON 로그 기준
- `services/shopping-service/src/main/resources/application-local.yml` - Prometheus/Zipkin 설정 기준
- [Micrometer Tracing](https://micrometer.io/docs/tracing) - Java 분산 추적 공식 문서
- [nestjs-prometheus](https://github.com/willsoto/nestjs-prometheus) - NestJS Prometheus 통합
- [OpenTelemetry Python](https://opentelemetry.io/docs/instrumentation/python/) - Python OTel 공식 가이드

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-11 | 초안 작성 | Laze |
