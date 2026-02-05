# AlertManager 알림 설정

효과적인 알림 시스템을 구축합니다.

---

## 학습 목표

- [ ] AlertManager의 동작을 이해한다
- [ ] 알림 규칙을 설정할 수 있다
- [ ] 알림 라우팅을 구성할 수 있다

---

## 1. AlertManager란?

### 역할

```
Prometheus → Alert Rules → AlertManager → Notifications
                              │
                    ┌─────────┼─────────┐
                    ↓         ↓         ↓
                  Slack    Email    PagerDuty
```

### 핵심 기능

- **그룹화**: 관련 알림 묶음
- **억제**: 중복 알림 방지
- **침묵**: 유지보수 중 알림 중지
- **라우팅**: 심각도별 다른 채널

---

## 2. 알림 규칙 (Prometheus)

### 파일 구조

```yaml
# monitoring/prometheus/rules/alerts.yml
groups:
  - name: service-alerts
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "{{ $labels.job }} 다운"
          description: "{{ $labels.job }}가 1분 이상 다운 상태입니다."
          runbook_url: "https://wiki.example.com/runbooks/service-down"
```

### 심각도 레벨

| 레벨 | 설명 | 대응 시간 | 예시 |
|------|------|----------|------|
| **critical** | 즉시 대응 | 5분 내 | 서비스 다운 |
| **warning** | 주의 필요 | 30분 내 | 높은 에러율 |
| **info** | 정보성 | 업무 시간 | 디스크 70% |

---

## 3. AlertManager 설정

### alertmanager.yml

```yaml
global:
  resolve_timeout: 5m

route:
  receiver: 'default-receiver'
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

  routes:
    - match:
        severity: critical
      receiver: 'critical-receiver'
      repeat_interval: 1h

    - match:
        severity: warning
      receiver: 'warning-receiver'
      repeat_interval: 4h

receivers:
  - name: 'default-receiver'
    webhook_configs:
      - url: 'http://alertmanager-webhook:5001/'

  - name: 'critical-receiver'
    slack_configs:
      - api_url: '${SLACK_WEBHOOK_URL}'
        channel: '#alerts-critical'
        send_resolved: true
        title: '🚨 {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'

  - name: 'warning-receiver'
    slack_configs:
      - api_url: '${SLACK_WEBHOOK_URL}'
        channel: '#alerts-warning'
        send_resolved: true

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname']
```

---

## 4. Portal Universe 알림 규칙

### 핵심 알림

```yaml
groups:
  - name: portal-universe-critical
    rules:
      # 서비스 완전 다운
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical

      # 에러율 10% 초과
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)
          /
          sum(rate(http_server_requests_seconds_count[5m])) by (job)
          > 0.10
        for: 2m
        labels:
          severity: critical

      # Circuit Breaker 열림
      - alert: CircuitBreakerOpen
        expr: resilience4j_circuitbreaker_state{state="open"} == 1
        for: 1m
        labels:
          severity: critical

      # Kafka 브로커 없음
      - alert: KafkaBrokersDown
        expr: kafka_brokers < 1
        for: 1m
        labels:
          severity: critical
```

---

## 5. 알림 피로 방지

### 좋은 알림의 특징

- ✅ **행동 가능**: 알림 수신 후 할 일이 명확
- ✅ **중요함**: 무시하면 문제가 심각해짐
- ✅ **희소**: 자주 발생하면 무시됨
- ✅ **명확**: 무엇이 문제인지 바로 알 수 있음

### 피해야 할 패턴

- ❌ 너무 민감한 임계값
- ❌ 너무 많은 알림
- ❌ 행동 불가능한 알림
- ❌ 해결 알림 없음

---

## 다음 단계

[03-incident-response.md](./03-incident-response.md) - 인시던트 대응 프로세스
