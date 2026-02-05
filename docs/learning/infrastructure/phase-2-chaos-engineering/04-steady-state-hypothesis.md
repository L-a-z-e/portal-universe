# 정상 상태 가설 (Steady State Hypothesis)

Chaos Engineering의 핵심인 정상 상태 가설을 수립하는 방법을 학습합니다.

---

## 학습 목표

- [ ] 정상 상태를 정량적으로 정의할 수 있다
- [ ] Portal Universe의 정상 상태 가설을 수립할 수 있다
- [ ] 가설 검증 방법을 설계할 수 있다

---

## 1. 정상 상태란?

### 정의

> 시스템이 비즈니스 목표를 달성하고 있음을 나타내는 **측정 가능한 상태**

### 잘못된 예시 (정성적)

```
❌ "시스템이 잘 동작한다"
❌ "사용자가 불만이 없다"
❌ "에러가 거의 없다"
```

### 올바른 예시 (정량적)

```
✅ "요청 성공률이 99.9% 이상이다"
✅ "p99 응답 시간이 500ms 이하이다"
✅ "초당 100개 이상의 요청을 처리한다"
```

---

## 2. 정상 상태 지표 선정

### 비즈니스 관점

| 지표 | 측정 방법 | 의미 |
|------|----------|------|
| **주문 성공률** | 성공 주문 / 전체 주문 시도 | 매출 직결 |
| **로그인 성공률** | 성공 로그인 / 유효 자격증명 시도 | 사용자 경험 |
| **검색 응답 시간** | 검색 요청 p99 | 전환율 영향 |

### 기술 관점

| 지표 | PromQL | 임계값 |
|------|--------|--------|
| **서비스 가용성** | `up{job="api-gateway"}` | = 1 |
| **요청 성공률** | `sum(rate(http_requests_total{status=~"2.."}[5m])) / sum(rate(http_requests_total[5m]))` | >= 0.999 |
| **응답 시간 p99** | `histogram_quantile(0.99, sum(rate(http_request_duration_seconds_bucket[5m])) by (le))` | <= 0.5s |
| **에러율** | `sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m]))` | < 0.01 |

---

## 3. Portal Universe 정상 상태 정의

### API Gateway

```yaml
component: api-gateway
steady_state:
  availability:
    metric: up{job="api-gateway"}
    expected: 1
    description: "서비스가 UP 상태"

  success_rate:
    metric: |
      sum(rate(http_server_requests_seconds_count{status=~"2..",job="api-gateway"}[5m]))
      /
      sum(rate(http_server_requests_seconds_count{job="api-gateway"}[5m]))
    expected: ">= 0.99"
    description: "99% 이상 요청 성공"

  latency_p99:
    metric: |
      histogram_quantile(0.99,
        sum(rate(http_server_requests_seconds_bucket{job="api-gateway"}[5m])) by (le)
      )
    expected: "<= 0.5"
    unit: "seconds"
    description: "p99 응답 시간 500ms 이하"

  active_pods:
    metric: |
      count(kube_pod_status_ready{namespace="portal-universe",pod=~"api-gateway.*"} == 1)
    expected: ">= 1"
    description: "최소 1개 이상 Ready Pod"
```

### Auth Service

```yaml
component: auth-service
steady_state:
  availability:
    metric: up{job="auth-service"}
    expected: 1

  login_success_rate:
    metric: |
      sum(rate(auth_login_total{result="success"}[5m]))
      /
      sum(rate(auth_login_total[5m]))
    expected: ">= 0.999"
    description: "유효 자격증명으로 99.9% 로그인 성공"

  token_generation_latency:
    metric: |
      histogram_quantile(0.99,
        sum(rate(auth_token_generation_seconds_bucket[5m])) by (le)
      )
    expected: "<= 0.2"
    unit: "seconds"
    description: "토큰 발급 200ms 이하"
```

### Shopping Service

```yaml
component: shopping-service
steady_state:
  availability:
    metric: up{job="shopping-service"}
    expected: 1

  order_success_rate:
    metric: |
      sum(rate(order_created_total{status="success"}[5m]))
      /
      sum(rate(order_created_total[5m]))
    expected: ">= 0.999"
    description: "99.9% 주문 성공"

  product_list_latency:
    metric: |
      histogram_quantile(0.99,
        sum(rate(http_server_requests_seconds_bucket{uri="/api/products"}[5m])) by (le)
      )
    expected: "<= 0.3"
    unit: "seconds"
```

### 인프라

```yaml
component: infrastructure
steady_state:
  kafka_available:
    metric: kafka_brokers > 0
    expected: ">= 1"
    description: "최소 1개 Kafka 브로커 가용"

  redis_available:
    metric: redis_up
    expected: 1
    description: "Redis 서버 가용"

  redis_memory:
    metric: redis_memory_used_bytes / redis_memory_max_bytes
    expected: "< 0.85"
    description: "Redis 메모리 85% 미만"

  mysql_connections:
    metric: |
      mysql_global_status_threads_connected
      /
      mysql_global_variables_max_connections
    expected: "< 0.8"
    description: "MySQL 커넥션 80% 미만"
```

---

## 4. 가설 수립 패턴

### 기본 구조

```
GIVEN: [정상 상태가 유지되고 있을 때]
WHEN: [특정 장애가 발생하면]
THEN: [시스템은 다음과 같이 반응해야 한다]
AND: [정상 상태는 X 시간 이내에 복구되어야 한다]
```

### 예시 1: Pod 삭제

```yaml
hypothesis:
  id: HYP-001
  name: "API Gateway Pod 복구"

  given:
    - "API Gateway가 정상 동작 중 (success_rate >= 99%)"
    - "1개의 Pod가 Running 상태"

  when:
    - "API Gateway Pod가 삭제됨"

  then:
    - "Kubernetes가 새 Pod를 생성"
    - "60초 이내에 새 Pod가 Ready 상태"
    - "총 다운타임 60초 이하"

  verification:
    - metric: "kube_pod_status_ready"
      condition: ">= 1 within 60s"
    - metric: "success_rate"
      condition: ">= 0.99 within 120s"
```

### 예시 2: 의존 서비스 장애

```yaml
hypothesis:
  id: HYP-002
  name: "Auth Service 장애 시 Circuit Breaker 동작"

  given:
    - "API Gateway, Auth Service 모두 정상"
    - "Circuit Breaker가 CLOSED 상태"

  when:
    - "Auth Service가 다운됨"
    - "인증이 필요한 요청이 발생"

  then:
    - "Circuit Breaker가 OPEN 상태로 전환"
    - "API Gateway는 fallback 응답 반환"
    - "API Gateway 자체는 다운되지 않음"
    - "비인증 엔드포인트는 정상 동작"

  verification:
    - metric: "resilience4j_circuitbreaker_state{name='authCircuitBreaker',state='open'}"
      condition: "== 1 within 30s"
    - metric: "up{job='api-gateway'}"
      condition: "== 1 throughout"
```

### 예시 3: 리소스 고갈

```yaml
hypothesis:
  id: HYP-003
  name: "Redis OOM 시 Rate Limiting Fallback"

  given:
    - "Redis가 정상 동작"
    - "Rate Limiting이 활성화됨"

  when:
    - "Redis가 OOM으로 다운됨"

  then:
    - "API Gateway는 요청을 거부하지 않음 (fail-open)"
    - "또는: 모든 요청을 거부함 (fail-closed)"
    - "Redis 복구 후 Rate Limiting 정상화"

  verification:
    - metric: "http_server_requests_seconds_count{status='200'}"
      condition: "증가 지속 (fail-open) 또는 0 (fail-closed)"
```

---

## 5. 가설 검증 자동화

### Prometheus 기반 검증 스크립트

```bash
#!/bin/bash
# verify-steady-state.sh

PROMETHEUS_URL="http://localhost:9090"

echo "=== 정상 상태 검증 시작 ==="

# 1. 서비스 가용성
echo -n "API Gateway UP: "
result=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=up{job='api-gateway'}" | jq -r '.data.result[0].value[1]')
if [ "$result" == "1" ]; then
  echo "✅ PASS"
else
  echo "❌ FAIL (value: $result)"
fi

# 2. 요청 성공률
echo -n "Success Rate >= 99%: "
result=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=sum(rate(http_server_requests_seconds_count{status=~\"2..\"}[5m]))/sum(rate(http_server_requests_seconds_count[5m]))" | jq -r '.data.result[0].value[1]')
if (( $(echo "$result >= 0.99" | bc -l) )); then
  echo "✅ PASS ($result)"
else
  echo "❌ FAIL ($result)"
fi

# 3. p99 Latency
echo -n "p99 Latency <= 500ms: "
result=$(curl -s "${PROMETHEUS_URL}/api/v1/query?query=histogram_quantile(0.99,sum(rate(http_server_requests_seconds_bucket[5m]))by(le))" | jq -r '.data.result[0].value[1]')
if (( $(echo "$result <= 0.5" | bc -l) )); then
  echo "✅ PASS (${result}s)"
else
  echo "❌ FAIL (${result}s)"
fi

echo "=== 검증 완료 ==="
```

### 사용 방법

```bash
# 실험 전
./verify-steady-state.sh > before.txt

# 실험 후
./verify-steady-state.sh > after.txt

# 비교
diff before.txt after.txt
```

---

## 6. 정상 상태 대시보드

### Grafana 패널 구성

```json
{
  "title": "Steady State Overview",
  "panels": [
    {
      "title": "서비스 상태",
      "type": "stat",
      "targets": [
        {"expr": "sum(up{job=~'api-gateway|auth-service|shopping-service'})"}
      ],
      "thresholds": [
        {"color": "red", "value": 0},
        {"color": "yellow", "value": 2},
        {"color": "green", "value": 3}
      ]
    },
    {
      "title": "전체 성공률",
      "type": "gauge",
      "targets": [
        {"expr": "sum(rate(http_server_requests_seconds_count{status=~'2..'}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) * 100"}
      ],
      "thresholds": [
        {"color": "red", "value": 0},
        {"color": "yellow", "value": 95},
        {"color": "green", "value": 99}
      ]
    },
    {
      "title": "Circuit Breaker 열림",
      "type": "stat",
      "targets": [
        {"expr": "count(resilience4j_circuitbreaker_state{state='open'} == 1) or vector(0)"}
      ],
      "thresholds": [
        {"color": "green", "value": 0},
        {"color": "red", "value": 1}
      ]
    }
  ]
}
```

---

## 7. 체크리스트

### 정상 상태 정의 체크리스트

- [ ] 모든 핵심 서비스에 대해 가용성 지표 정의됨
- [ ] 비즈니스 KPI와 연결된 지표 포함됨
- [ ] 모든 지표가 Prometheus에서 쿼리 가능
- [ ] 임계값이 SLO와 일치
- [ ] 대시보드에서 실시간 확인 가능

### 가설 수립 체크리스트

- [ ] GIVEN/WHEN/THEN 형식으로 작성됨
- [ ] 검증 조건이 정량적임
- [ ] 타임아웃/시간 제한 명시됨
- [ ] 검증 방법(PromQL, 스크립트)이 준비됨

---

## 핵심 정리

1. **정상 상태**는 반드시 **정량적**으로 정의해야 합니다
2. **비즈니스 지표**와 **기술 지표**를 모두 포함합니다
3. **GIVEN/WHEN/THEN** 패턴으로 가설을 수립합니다
4. **자동화된 검증**으로 일관된 결과를 얻습니다
5. **대시보드**로 실시간 모니터링합니다

---

## 다음 단계

[05-game-day-template.md](./05-game-day-template.md) - Game Day 실행 템플릿을 학습합니다.

---

## 참고 자료

- [Principles of Chaos Engineering - Steady State](https://principlesofchaos.org/)
- [Google SRE - Service Level Objectives](https://sre.google/sre-book/service-level-objectives/)
