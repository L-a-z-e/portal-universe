# MTTR, MTBF 지표

가용성을 구성하는 핵심 지표인 MTTR과 MTBF를 학습합니다.

---

## 학습 목표

- [ ] MTTR, MTBF, MTTF의 정의와 차이를 설명할 수 있다
- [ ] 실제 장애 데이터에서 지표를 계산할 수 있다
- [ ] 지표 개선을 위한 전략을 수립할 수 있다

---

## 1. 핵심 지표 정의

### MTBF (Mean Time Between Failures)

**정의**: 장애와 장애 사이의 평균 시간

```
MTBF = 총 가동 시간 / 장애 횟수
```

**예시**:
```
기간: 30일 (720시간)
장애: 2회
총 다운타임: 1시간

MTBF = (720 - 1) / 2 = 359.5시간
```

**의미**: MTBF가 높을수록 시스템이 안정적입니다.

### MTTR (Mean Time To Recovery/Repair)

**정의**: 장애 발생부터 복구까지의 평균 시간

```
MTTR = 총 다운타임 / 장애 횟수
```

**예시**:
```
장애 1: 20분 다운타임
장애 2: 40분 다운타임

MTTR = (20 + 40) / 2 = 30분
```

**의미**: MTTR이 낮을수록 빠르게 복구합니다.

### MTTF (Mean Time To Failure)

**정의**: 복구 불가능한 장애까지의 평균 시간 (교체가 필요한 하드웨어)

```
MTTF = 총 가동 시간 / 장애 횟수 (교체 필요)
```

**MTBF vs MTTF**:
- MTBF: 복구 가능한 시스템 (소프트웨어, 서버)
- MTTF: 복구 불가능한 장치 (하드 드라이브, 전구)

---

## 2. 추가 지표

### MTTA (Mean Time To Acknowledge)

**정의**: 장애 감지부터 담당자가 인지하기까지의 시간

```
MTTA = Σ(인지 시간 - 감지 시간) / 장애 횟수
```

**중요성**: On-call 프로세스 효율성 측정

**목표**: < 5분 (자동 알림, 에스컬레이션)

### MTTD (Mean Time To Detect)

**정의**: 실제 장애 발생부터 시스템이 감지하기까지의 시간

```
MTTD = Σ(감지 시간 - 발생 시간) / 장애 횟수
```

**중요성**: 모니터링 시스템 효율성 측정

**목표**: < 1분 (실시간 모니터링, 적절한 알림 임계값)

---

## 3. 가용성과 지표 관계

### 가용성 공식

```
가용성 = MTBF / (MTBF + MTTR)
```

### 다양한 시나리오

| MTBF | MTTR | 가용성 | 연간 다운타임 |
|------|------|--------|--------------|
| 720시간 (30일) | 43분 | 99.9% | 8.77시간 |
| 720시간 (30일) | 4.3분 | 99.99% | 52분 |
| 168시간 (7일) | 10분 | 99.9% | 8.77시간 |
| 2160시간 (90일) | 130분 | 99.9% | 8.77시간 |

### 개선 전략 비교

**MTBF 2배 vs MTTR 1/2**:

```
현재: MTBF=720h, MTTR=43m → 가용성 99.9%

전략 A (MTBF 2배): MTBF=1440h, MTTR=43m
가용성 = 1440 / (1440 + 0.72) = 99.95%

전략 B (MTTR 절반): MTBF=720h, MTTR=21.5m
가용성 = 720 / (720 + 0.36) = 99.95%
```

**결론**: 같은 효과지만 MTTR 개선이 일반적으로 더 쉽습니다.

---

## 4. MTTR 분해

MTTR은 여러 단계로 분해할 수 있습니다:

```
MTTR = MTTD + MTTA + MTTI + MTTR(fix)

MTTD: 감지 시간 (Detect)
MTTA: 인지 시간 (Acknowledge)
MTTI: 조사 시간 (Investigate)
MTTR(fix): 실제 복구 시간 (Repair)
```

### 예시 타임라인

```
시간 ──────────────────────────────────────────────────────>
     │     │           │                │                  │
     │     │           │                │                  │
   장애   감지       인지             원인파악           복구완료
   발생   (3분)      (5분)            (20분)            (15분)
     │     │           │                │                  │
     └─────┴───────────┴────────────────┴──────────────────┘
       MTTD    MTTA         MTTI           MTTR(fix)
       3분     5분          20분            15분

       총 MTTR = 43분
```

### 단계별 개선 방법

| 단계 | 목표 | 개선 방법 |
|------|------|----------|
| **MTTD** | < 1분 | 실시간 모니터링, 적절한 알림 임계값 |
| **MTTA** | < 5분 | PagerDuty, On-call 로테이션 |
| **MTTI** | < 15분 | 로그 집중화, 분산 추적, Runbook |
| **MTTR(fix)** | < 10분 | 자동 복구, 롤백 자동화, 이중화 |

---

## 5. 실습: 지표 계산

### 샘플 장애 데이터

```yaml
incidents:
  - id: INC-001
    start: "2026-01-15T14:23:00Z"
    detected: "2026-01-15T14:24:30Z"
    acknowledged: "2026-01-15T14:27:00Z"
    resolved: "2026-01-15T14:58:00Z"
    impact: "Auth Service 응답 지연"

  - id: INC-002
    start: "2026-01-22T09:15:00Z"
    detected: "2026-01-22T09:16:00Z"
    acknowledged: "2026-01-22T09:18:00Z"
    resolved: "2026-01-22T09:35:00Z"
    impact: "Redis OOM"

  - id: INC-003
    start: "2026-01-28T22:45:00Z"
    detected: "2026-01-28T22:47:00Z"
    acknowledged: "2026-01-28T22:55:00Z"
    resolved: "2026-01-29T00:10:00Z"
    impact: "Kafka 브로커 다운"
```

### 지표 계산

```python
# 계산 스크립트 (참고용)
incidents = [
    {"duration": 35, "mttd": 1.5, "mtta": 2.5},  # INC-001
    {"duration": 20, "mttd": 1.0, "mtta": 2.0},  # INC-002
    {"duration": 85, "mttd": 2.0, "mtta": 8.0},  # INC-003
]

# MTTR
mttr = sum(i["duration"] for i in incidents) / len(incidents)
print(f"MTTR: {mttr:.1f}분")  # 46.7분

# MTTD
mttd = sum(i["mttd"] for i in incidents) / len(incidents)
print(f"MTTD: {mttd:.1f}분")  # 1.5분

# MTTA
mtta = sum(i["mtta"] for i in incidents) / len(incidents)
print(f"MTTA: {mtta:.1f}분")  # 4.2분

# MTBF (30일 기간, 3회 장애)
total_hours = 30 * 24  # 720시간
total_downtime = 140 / 60  # 2.33시간
mtbf = (total_hours - total_downtime) / 3
print(f"MTBF: {mtbf:.1f}시간")  # 239.2시간

# 가용성
availability = mtbf / (mtbf + mttr/60)
print(f"가용성: {availability*100:.3f}%")  # 99.67%
```

### 결과 분석

| 지표 | 현재 값 | 목표 | 개선 필요 |
|------|--------|------|----------|
| MTTR | 46.7분 | < 30분 | ⚠️ 예 |
| MTTD | 1.5분 | < 1분 | ⚠️ 예 |
| MTTA | 4.2분 | < 5분 | ✅ 양호 |
| MTBF | 239.2시간 | > 360시간 | ⚠️ 예 |
| 가용성 | 99.67% | > 99.9% | ⚠️ 예 |

---

## 6. Prometheus를 통한 지표 수집

### Recording Rules

```yaml
# monitoring/prometheus/rules/recording.yml 추가
groups:
  - name: availability-metrics
    rules:
      # 서비스별 업타임 (1일 단위)
      - record: service:uptime:1d
        expr: avg_over_time(up[1d])

      # 요청 성공률 (5분 단위)
      - record: service:success_rate:5m
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"2.."}[5m])) by (job)
          /
          sum(rate(http_server_requests_seconds_count[5m])) by (job)
```

### 알림 규칙

```yaml
# 장애 감지를 위한 알림
groups:
  - name: mttr-alerts
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "{{ $labels.job }} is down"
          # MTTD 기록 시작 시점

      - alert: HighLatency
        expr: |
          histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job))
          > 1.0
        for: 2m
        labels:
          severity: warning
```

---

## 7. 지표 개선 우선순위

### 빠른 효과 (Quick Wins)

1. **MTTD 개선**: 알림 임계값 조정
   - 너무 민감: 알림 피로 → 무시
   - 너무 둔감: 늦은 감지

2. **MTTA 개선**: On-call 프로세스
   - PagerDuty 연동
   - 에스컬레이션 정책

### 중기 투자

3. **MTTI 개선**: 관찰성 강화
   - 로그 집중화 (ELK, Loki)
   - 분산 추적 (Zipkin, Jaeger)
   - Runbook 작성

4. **MTTR(fix) 개선**: 자동화
   - Auto-healing (Kubernetes)
   - 롤백 자동화
   - 스크립트화된 복구

### 장기 투자

5. **MTBF 개선**: 아키텍처 변경
   - 이중화 (replicas > 1)
   - 부하 분산
   - Chaos Engineering

---

## 핵심 정리

1. **가용성 = MTBF / (MTBF + MTTR)** 공식을 기억하세요
2. **MTTR 개선**이 MTBF 개선보다 보통 더 현실적입니다
3. **MTTR을 분해**하면 어디를 개선해야 할지 알 수 있습니다
4. **자동화**는 MTTD, MTTA, MTTR 모두를 개선합니다
5. **지표를 측정**해야 개선 여부를 알 수 있습니다

---

## 다음 단계

[04-redundancy-patterns.md](./04-redundancy-patterns.md) - 이중화 패턴을 학습합니다.

---

## 참고 자료

- [Google SRE Book - Chapter 3: Embracing Risk](https://sre.google/sre-book/embracing-risk/)
- [Atlassian - Incident Management Metrics](https://www.atlassian.com/incident-management/kpis)
