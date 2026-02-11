# TS-20260211-002: Grafana 대시보드 "No data" 패널 다수 발생

**심각도**: 🟠 High
**상태**: Resolved
**영향 서비스**: api-gateway, auth-service, blog-service, shopping-service, notification-service

| 항목 | 내용 |
|------|------|
| **발생일시** | 2026-02-11 |
| **해결일시** | 2026-02-11 |
| **담당자** | Laze |

## 증상 (Symptoms)

Grafana 대시보드 8개 중 4개에서 다수의 패널이 "No data"를 표시했다.

| 대시보드 | No data 패널 수 | 전체 패널 수 |
|---------|----------------|-------------|
| SLO/SLI | 3 | 18 |
| Bottleneck Detection | 9 | 10 |
| Load Test Overview | 10 | 12 |
| Service Overview | 1 | 21 |
| API Performance | 2 | 13 |

- 에러 메시지: 패널에 "No data" 표시 (Grafana 쿼리 에러 아닌 빈 응답)
- 영향 범위: SLO 가용성 게이지, P95/P99 latency, USE 메트릭, HikariCP 커넥션 풀 등 핵심 모니터링 패널

## 원인 (Root Cause)

6가지 독립된 원인이 복합적으로 작용했다.

### 원인 1: Spring Boot histogram bucket 미활성화
`histogram_quantile()` PromQL을 사용하는 패널이 `http_server_requests_seconds_bucket` 메트릭을 참조하지만, Spring Boot의 `percentiles-histogram` 설정이 누락되어 bucket 메트릭이 생성되지 않았다.

- 영향: P95/P99 latency 패널 전체 (SLO/SLI, API Performance, Bottleneck Detection)
- 확인 쿼리: `count(http_server_requests_seconds_bucket)` → 0 series

### 원인 2: 5xx 에러 series 부재 + `or vector(0)` label mismatch
5xx 에러가 발생하지 않은 정상 상태에서 `{status=~"5.."}` 필터가 빈 시리즈를 반환하면, `rate()` 결과가 empty가 되어 전체 수식이 "No data"로 표시되었다.

초기 수정으로 `or vector(0)` 패턴을 적용했으나, `by (application)` 또는 `by (job)` grouping이 있는 수식에서는 `vector(0)`가 label을 갖지 않아 나눗셈 결과가 여전히 empty였다:

```promql
# vector(0)는 label이 없음 → by (application) 나눗셈 시 label mismatch → 빈 결과
(numerator or vector(0)) / sum(rate(...)) by (application) * 100
```

- 영향: SLO 가용성 게이지, Error Rate 패널 (SLO/SLI, Service Overview, Bottleneck Detection)
- 원인: `vector(0)`는 instant vector with no labels. `by (label)` grouping과 함께 사용 시 label이 일치하지 않아 binary operation이 실패한다

### 원인 7: Local profile에 `management.metrics.tags` 미설정
Docker profile (`application-docker.yml`)에는 `management.metrics.tags.application` 설정이 있었으나, Local profile (`application-local.yml`)에는 누락되어 있었다. 이로 인해 Prometheus 쿼리 `{application=~"api-gateway"}`가 빈 결과를 반환했다.

- 영향: Local 환경에서 모든 application 기반 PromQL 쿼리
- 확인: `http_server_requests_seconds_count` 에 `application` label 없음

### 원인 3: cAdvisor container 메트릭 `name` 라벨 미제공 (macOS)
Bottleneck Detection과 Load Test Overview의 USE(Utilization, Saturation, Errors) 패널이 `container_cpu_usage_seconds_total{name=~"api-gateway|..."}` 형태로 쿼리하지만, macOS Docker Desktop에서 cAdvisor는 `name` 라벨을 제공하지 않는다.

- 영향: CPU Utilization, Memory Utilization, Network I/O 패널
- 확인: `container_cpu_usage_seconds_total` 시리즈에 `name` 라벨 없음, `id` 라벨만 존재

### 원인 4: MySQL Exporter 인증 실패
`mysql_up` 메트릭이 0으로, MySQL exporter가 DB에 연결하지 못했다. `exporter` 사용자가 MySQL에 생성되지 않았다.

- 영향: Bottleneck Detection의 DB Connection 패널
- 확인: `mysql_up` → 0

### 원인 5: HikariCP 패널 template variable 매칭 실패
HikariCP 커넥션 풀 패널에서 `application="$service"` (정확 매칭)를 사용했는데, Grafana 변수 `$service`가 "All" 선택 시 regex 패턴(`api-gateway|auth-service|...`)을 생성하여 매칭 실패.

- 영향: HikariCP Active/Idle/Pending 패널
- 확인: 특정 서비스 선택 시 데이터 표시, "All" 선택 시 "No data"

### 원인 6: k6 Load Test 미실행
Load Test Overview 대시보드는 k6 부하 테스트 실행 시 생성되는 `k6_*` 메트릭을 참조한다. 부하 테스트를 실행하지 않았으므로 해당 메트릭이 존재하지 않는다.

- 영향: Load Test Overview의 k6 관련 패널 (Virtual Users, Request Rate, Response Time 등)
- 상태: 정상 동작 (테스트 미실행 시 데이터 없는 것이 기대 동작)

**분석 과정**:
1. Playwright MCP로 Grafana 8개 대시보드를 순회하며 스크린샷 및 "No data" 패널 수 집계
2. Prometheus UI에서 각 패널의 PromQL 직접 실행하여 빈 결과 확인
3. `count()` 쿼리로 메트릭 존재 여부 확인: `http_server_requests_seconds_bucket` = 0, `http_server_duration_bucket` = 32
4. cAdvisor 메트릭의 라벨 구조 조사: `name` 라벨 부재 확인
5. MySQL exporter 상태 확인: `mysql_up` = 0

## 해결 방법 (Solution)

### Fix 1: Spring Boot histogram bucket 활성화

5개 Spring Boot 서비스의 `application-docker.yml`에 histogram 설정 추가:

```yaml
management:
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
      slo:
        http.server.requests: 50ms,100ms,200ms,500ms,1s,5s
    tags:
      application: ${spring.application.name}
      environment: docker
```

**수정 파일**:
- `services/api-gateway/src/main/resources/application-docker.yml`
- `services/auth-service/src/main/resources/application-docker.yml`
- `services/blog-service/src/main/resources/application-docker.yml`
- `services/shopping-service/src/main/resources/application-docker.yml`
- `services/notification-service/src/main/resources/application-docker.yml`

**결과**: `count(http_server_requests_seconds_bucket)` 0 → 675 series

### Fix 2: PromQL `or denominator * 0` 패턴 적용

**Phase A (초기 수정)**: 5xx 에러가 없을 때 `or vector(0)` 패턴을 적용했으나 불완전했다.

**Phase B (최종 수정)**: `by (application)` grouping이 있는 수식에서 `vector(0)`는 label이 없어 binary operation이 실패하는 것을 발견. 분모 수식에 `* 0`을 곱하여 label을 보존하는 패턴으로 교체:

```promql
# Phase A (불완전) - vector(0)는 label이 없어 by 절과 함께 쓸 때 실패
(sum(rate(...{status=~"5.."}[...])) by (application) or vector(0))
/ sum(rate(...[...])) by (application) * 100

# Phase B (최종) - denominator * 0는 분모의 label을 보존
(sum(rate(...{status=~"5.."}[...])) by (application)
  or sum(rate(...[...])) by (application) * 0)
/ sum(rate(...[...])) by (application) * 100
```

**수정 파일** (총 16개+ PromQL expression):
- `monitoring/grafana/provisioning/dashboards/json/slo-sli.json` (12 expr → Phase A, 5 expr Phase B)
- `monitoring/grafana/provisioning/dashboards/json/service-overview.json` (1 expr → Phase B)
- `monitoring/grafana/provisioning/dashboards/json/api-performance.json` (2 expr)
- `monitoring/grafana/provisioning/dashboards/json/bottleneck-detection.json` (1 expr → Phase B)

### Fix 7: Local profile에 metrics tags 추가

5개 Spring Boot 서비스의 `application-local.yml`에 metrics tags 설정 추가:

```yaml
management:
  metrics:
    tags:
      application: ${spring.application.name}
      environment: local
```

**수정 파일**:
- `services/api-gateway/src/main/resources/application-local.yml`
- `services/auth-service/src/main/resources/application-local.yml`
- `services/blog-service/src/main/resources/application-local.yml`
- `services/shopping-service/src/main/resources/application-local.yml`
- `services/notification-service/src/main/resources/application-local.yml`

### Fix 3: USE 메트릭을 process-level로 대체

cAdvisor의 `name` 라벨 부재를 우회하여 Spring Boot가 직접 노출하는 JVM/process 메트릭으로 대체:

| 원래 쿼리 | 대체 쿼리 |
|----------|----------|
| `container_cpu_usage_seconds_total{name=~"..."}` | `process_cpu_usage{application=~"$service"}` |
| `container_memory_usage_bytes{name=~"..."}/container_spec_memory_limit_bytes` | `jvm_memory_used_bytes{area="heap"}/jvm_memory_max_bytes{area="heap"}` |
| `container_network_*{name=~"..."}` | `container_network_*{id=~"/docker/.*"}` |

**수정 파일**:
- `monitoring/grafana/provisioning/dashboards/json/bottleneck-detection.json`
- `monitoring/grafana/provisioning/dashboards/json/load-test-overview.json`

### Fix 4: cAdvisor 컨테이너 라벨 설정

```yaml
# docker-compose.yml
cadvisor:
  command:
    - '--store_container_labels=true'
    - '--whitelisted_container_labels=com.docker.compose.service'
```

**수정 파일**: `docker-compose.yml`

### Fix 5: MySQL Exporter 사용자 생성

MySQL 컨테이너에 exporter 사용자를 수동 생성:

```sql
CREATE USER IF NOT EXISTS 'exporter'@'%' IDENTIFIED BY 'exporter_password';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
FLUSH PRIVILEGES;
```

> `infrastructure/mysql/init.sql`에 이미 정의되어 있으나, 기존 볼륨에서는 init script가 재실행되지 않아 수동 생성이 필요했다.

### Fix 6: HikariCP template variable regex 매칭

```promql
# Before (exact match - "All" 선택 시 실패)
hikaricp_connections_active{application="$service"}

# After (regex match - "All" 선택 시 정상)
hikaricp_connections_active{application=~"$service"}
```

**수정 파일**: `monitoring/grafana/provisioning/dashboards/json/bottleneck-detection.json`

## 수정 후 결과

| 대시보드 | Before | After | 비고 |
|---------|--------|-------|------|
| Service Monitoring | 0 | 0 | - |
| Service Overview | 2 | 0 | 5xx error rate fix |
| API Performance | 2 | 0 | histogram + 5xx fix |
| JVM Deep Dive | 0 | 0 | - |
| Logs & Traces | 0 | 0 | - |
| SLO/SLI | 3 | 0 | histogram + 5xx fix |
| Bottleneck Detection | 9 | 3 | USE + HikariCP + 5xx fix, 잔여 3개는 k6 메트릭 |
| Load Test Overview | 10 | ~6 | USE fix, k6 메트릭은 테스트 실행 필요 |

## 재발 방지 (Prevention)

- [ ] 새 Grafana 대시보드 추가 시 "No data" 상태 테스트 체크리스트에 포함
- [ ] Spring Boot 서비스 생성 가이드에 histogram 설정 필수 항목으로 추가
- [ ] 5xx 에러율 PromQL에는 반드시 `or denominator * 0` 패턴 적용 (`vector(0)` 사용 금지)
- [ ] Grafana template variable 사용 시 `=~` (regex) 매칭 권장
- [ ] 새 Spring Boot profile 추가 시 `management.metrics.tags` 설정 동반 필수

## 학습 포인트

1. **Spring Boot histogram은 명시적으로 활성화해야 한다**
   - `management.metrics.distribution.percentiles-histogram.http.server.requests: true` 없이는 `_bucket` 메트릭이 생성되지 않아 `histogram_quantile()` PromQL이 동작하지 않는다.

2. **`or vector(0)`는 `by` 절과 함께 쓸 수 없다**
   - `vector(0)`는 label이 없는 instant vector를 생성한다. `by (application)` 같은 grouping이 있는 binary operation에서 label mismatch로 빈 결과가 된다.
   - 올바른 패턴: `or DENOMINATOR * 0` — 분모 수식에 0을 곱하면 label 구조가 보존되어 안전하게 fallback 0을 제공한다.

3. **모든 profile에 동일한 metrics tags를 설정해야 한다**
   - Docker profile에만 `management.metrics.tags.application`을 설정하고 Local profile에는 누락하면, Local 환경에서 Grafana 쿼리가 실패한다. 새 설정 키를 추가할 때 `local`, `docker`, `k8s` 3개 profile 모두에 반영해야 한다.

4. **cAdvisor는 플랫폼별 동작이 다르다**
   - Linux에서는 `name` 라벨이 자동 제공되지만, macOS Docker Desktop에서는 제공되지 않는다. 플랫폼 독립적인 메트릭(JVM process-level)을 사용하는 것이 안전하다.

5. **Grafana template variable "All"은 regex 패턴을 생성한다**
   - `$service`가 "All" 선택 시 `api-gateway|auth-service|...` 패턴이 되므로, exact match(`=`)가 아닌 regex match(`=~`)를 사용해야 한다.

---

## 관련 문서
- [ADR-033: Polyglot 통합 Observability 아키텍처](../adr/ADR-033-polyglot-observability.md)
- [OTel Tracing 초기화 타이밍 이슈](./TS-20260211-001-otel-tracing-init-timing.md)
- [Observability 운영 가이드](../guides/observability-guide.md)

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-11 | 초안 작성 | Laze |
| 2026-02-11 | Phase B 검증 결과 반영: vector(0) label mismatch 원인/수정, local metrics tags 추가 | Laze |
