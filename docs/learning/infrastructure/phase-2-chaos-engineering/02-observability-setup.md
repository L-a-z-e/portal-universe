# 관찰성(Observability) 설정

Chaos Engineering 실험을 위한 모니터링 대시보드를 설정합니다.

---

## 학습 목표

- [ ] Prometheus + Grafana 대시보드를 설정할 수 있다
- [ ] 핵심 메트릭을 실시간으로 관찰할 수 있다
- [ ] 장애 감지를 위한 알림을 구성할 수 있다

---

## 1. 관찰성의 세 기둥

### Metrics (메트릭)

**수치로 표현되는 시계열 데이터**

```
http_requests_total{method="GET", status="200"} 12345
http_request_duration_seconds_bucket{le="0.5"} 9800
```

**도구**: Prometheus, Grafana

### Logs (로그)

**이벤트 기록**

```
2026-01-15T14:23:45.123Z INFO  [api-gateway] Request completed: method=GET, uri=/api/health, status=200, duration=15ms
2026-01-15T14:23:46.456Z ERROR [auth-service] Authentication failed: user=john, reason=invalid_password
```

**도구**: ELK Stack, Loki

### Traces (추적)

**요청의 전체 흐름**

```
Trace ID: abc123
├── api-gateway (15ms)
│   └── auth-service (8ms)
│       └── mysql-query (3ms)
└── shopping-service (12ms)
    └── redis-cache (1ms)
```

**도구**: Zipkin, Jaeger

---

## 2. 모니터링 스택 확인

### 서비스 접속

```bash
# Prometheus 포트 포워딩
kubectl port-forward -n portal-universe svc/prometheus 9090:9090 &

# Grafana 포트 포워딩
kubectl port-forward -n portal-universe svc/grafana 3000:3000 &

# Zipkin 포트 포워딩 (있는 경우)
kubectl port-forward -n portal-universe svc/zipkin 9411:9411 &
```

### 접속 정보

| 서비스 | URL | 인증 |
|--------|-----|------|
| Prometheus | http://localhost:9090 | 없음 |
| Grafana | http://localhost:3000 | admin/admin |
| Zipkin | http://localhost:9411 | 없음 |

---

## 3. Chaos Engineering 대시보드 생성

### Grafana 대시보드 JSON

아래 JSON을 Grafana에서 Import합니다.

```json
{
  "dashboard": {
    "title": "Chaos Engineering Dashboard",
    "uid": "chaos-engineering",
    "panels": [
      {
        "title": "서비스 상태",
        "type": "stat",
        "gridPos": {"h": 4, "w": 6, "x": 0, "y": 0},
        "targets": [
          {
            "expr": "sum(up{job=~\"api-gateway|auth-service|shopping-service\"})",
            "legendFormat": "Active Services"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "yellow", "value": 2},
                {"color": "green", "value": 3}
              ]
            }
          }
        }
      },
      {
        "title": "요청 성공률 (5분)",
        "type": "gauge",
        "gridPos": {"h": 4, "w": 6, "x": 6, "y": 0},
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{status=~\"2..\"}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) * 100",
            "legendFormat": "Success Rate"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "min": 0,
            "max": 100,
            "unit": "percent",
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "yellow", "value": 95},
                {"color": "green", "value": 99}
              ]
            }
          }
        }
      },
      {
        "title": "응답 시간 p99",
        "type": "timeseries",
        "gridPos": {"h": 6, "w": 12, "x": 0, "y": 4},
        "targets": [
          {
            "expr": "histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job))",
            "legendFormat": "{{job}}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "s",
            "custom": {
              "drawStyle": "line"
            }
          }
        }
      },
      {
        "title": "에러율 (5분)",
        "type": "timeseries",
        "gridPos": {"h": 6, "w": 12, "x": 12, "y": 4},
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])) by (job)",
            "legendFormat": "{{job}}"
          }
        ]
      },
      {
        "title": "Circuit Breaker 상태",
        "type": "stat",
        "gridPos": {"h": 4, "w": 12, "x": 0, "y": 10},
        "targets": [
          {
            "expr": "resilience4j_circuitbreaker_state",
            "legendFormat": "{{name}}: {{state}}"
          }
        ]
      },
      {
        "title": "JVM 메모리 사용량",
        "type": "timeseries",
        "gridPos": {"h": 6, "w": 12, "x": 12, "y": 10},
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"} / jvm_memory_max_bytes{area=\"heap\"} * 100",
            "legendFormat": "{{job}}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "percent"
          }
        }
      }
    ]
  }
}
```

### 대시보드 Import 방법

1. Grafana 접속 (http://localhost:3000)
2. 좌측 메뉴 → Dashboards → Import
3. "Import via panel json" 클릭
4. 위 JSON 붙여넣기
5. "Load" → "Import"

---

## 4. 핵심 메트릭

### 서비스 상태 (Golden Signals)

| Signal | 메트릭 | 의미 |
|--------|--------|------|
| **Latency** | `http_server_requests_seconds_bucket` | 응답 시간 |
| **Traffic** | `http_server_requests_seconds_count` | 요청 수 |
| **Errors** | `http_server_requests_seconds_count{status=~"5.."}` | 에러 수 |
| **Saturation** | `jvm_memory_used_bytes / jvm_memory_max_bytes` | 리소스 사용률 |

### Prometheus 쿼리 예시

```promql
# 서비스별 요청 성공률
sum(rate(http_server_requests_seconds_count{status=~"2.."}[5m])) by (job)
/
sum(rate(http_server_requests_seconds_count[5m])) by (job)

# p99 응답 시간
histogram_quantile(0.99,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job)
)

# 서비스 업타임
up{job=~"api-gateway|auth-service|shopping-service"}

# Circuit Breaker 열림 상태
resilience4j_circuitbreaker_state{state="open"}

# 에러율 (5분 평균)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)
```

---

## 5. 알림 설정

### Prometheus AlertManager 규칙

**파일**: `monitoring/prometheus/rules/chaos-alerts.yml`

```yaml
groups:
  - name: chaos-engineering-alerts
    rules:
      # 서비스 다운
      - alert: ServiceDown
        expr: up == 0
        for: 30s
        labels:
          severity: critical
        annotations:
          summary: "서비스 {{ $labels.job }} 다운"
          description: "{{ $labels.job }}가 30초 이상 다운 상태입니다."

      # 높은 에러율
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)
          /
          sum(rate(http_server_requests_seconds_count[5m])) by (job)
          > 0.05
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.job }} 에러율 {{ $value | humanizePercentage }}"

      # 높은 응답 시간
      - alert: HighLatency
        expr: |
          histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job))
          > 1.0
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "{{ $labels.job }} p99 응답 시간 {{ $value | humanizeDuration }}"

      # Circuit Breaker 열림
      - alert: CircuitBreakerOpen
        expr: resilience4j_circuitbreaker_state{state="open"} == 1
        for: 10s
        labels:
          severity: critical
        annotations:
          summary: "Circuit Breaker {{ $labels.name }} 열림"
          description: "50% 이상 실패로 Circuit Breaker가 열렸습니다."

      # 높은 메모리 사용량
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
          summary: "{{ $labels.job }} 힙 메모리 {{ $value | humanizePercentage }}"
```

### 알림 규칙 적용

```bash
# ConfigMap 업데이트
kubectl create configmap prometheus-rules \
  --from-file=monitoring/prometheus/rules/ \
  -n portal-universe \
  --dry-run=client -o yaml | kubectl apply -f -

# Prometheus 재시작
kubectl rollout restart deployment prometheus -n portal-universe
```

---

## 6. 실시간 관찰 체크리스트

### Chaos 실험 전

- [ ] Grafana 대시보드 열기
- [ ] Prometheus Alerts 페이지 확인
- [ ] 현재 정상 상태 기록 (스크린샷)
- [ ] 터미널에서 `watch` 명령 실행

```bash
# 터미널 1: Pod 상태 모니터링
watch -n 2 'kubectl get pods -n portal-universe'

# 터미널 2: 이벤트 모니터링
kubectl get events -n portal-universe -w

# 터미널 3: 로그 모니터링
kubectl logs -f -l app=api-gateway -n portal-universe
```

### Chaos 실험 중

- [ ] 대시보드 변화 관찰
- [ ] 알림 발생 확인
- [ ] 로그 이상 확인
- [ ] 예상대로 동작하는지 확인

### Chaos 실험 후

- [ ] 정상 상태 복귀 확인
- [ ] 알림 해제 확인
- [ ] 결과 기록

---

## 7. 유용한 kubectl 명령어

### 실시간 모니터링

```bash
# Pod 리소스 사용량
kubectl top pods -n portal-universe

# 노드 리소스 사용량
kubectl top nodes

# Pod 상태 변화 감시
kubectl get pods -n portal-universe -w

# 이벤트 실시간 감시
kubectl get events -n portal-universe -w --sort-by='.lastTimestamp'
```

### 디버깅

```bash
# Pod 상세 정보
kubectl describe pod <pod-name> -n portal-universe

# 컨테이너 로그 (마지막 100줄)
kubectl logs <pod-name> -n portal-universe --tail=100

# 이전 컨테이너 로그 (크래시 원인 확인)
kubectl logs <pod-name> -n portal-universe --previous
```

---

## 8. 대시보드 스크린샷 예시

### 정상 상태

```
┌─────────────────────────────────────────────────────────────┐
│ 서비스 상태: 🟢 3/3    요청 성공률: 🟢 99.9%                │
├─────────────────────────────────────────────────────────────┤
│ 응답 시간 p99                                               │
│   api-gateway ████████░░ 180ms                              │
│   auth-service ██████░░░ 120ms                              │
│   shopping    █████████░ 200ms                              │
├─────────────────────────────────────────────────────────────┤
│ Circuit Breaker: 모두 CLOSED                                │
└─────────────────────────────────────────────────────────────┘
```

### 장애 상태 (Chaos 실험 중)

```
┌─────────────────────────────────────────────────────────────┐
│ 서비스 상태: 🔴 2/3    요청 성공률: 🟡 95.2%                │
├─────────────────────────────────────────────────────────────┤
│ 응답 시간 p99                                               │
│   api-gateway ██████████████████ 850ms ⚠️                   │
│   auth-service ████░░░░░░ (down)                            │
│   shopping    █████████░ 200ms                              │
├─────────────────────────────────────────────────────────────┤
│ Circuit Breaker: authCircuitBreaker OPEN 🔴                 │
│ Alerts: ServiceDown (auth-service)                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 핵심 정리

1. **Golden Signals** (Latency, Traffic, Errors, Saturation)를 모니터링합니다
2. **Grafana 대시보드**로 실시간 시각화합니다
3. **AlertManager**로 자동 알림을 설정합니다
4. **Chaos 실험 전** 정상 상태를 기록합니다
5. **여러 터미널**에서 동시에 관찰합니다

---

## 다음 단계

[03-basic-fault-injection.md](./03-basic-fault-injection.md) - 기본 장애 주입을 실습합니다.

---

## 참고 자료

- [Grafana Documentation](https://grafana.com/docs/)
- [Prometheus Query Examples](https://prometheus.io/docs/prometheus/latest/querying/examples/)
- [Google SRE - Monitoring Distributed Systems](https://sre.google/sre-book/monitoring-distributed-systems/)
