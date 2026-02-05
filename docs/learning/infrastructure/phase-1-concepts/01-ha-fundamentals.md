# 고가용성 기초 개념

고가용성(High Availability)의 핵심 개념과 지표를 학습합니다.

---

## 학습 목표

- [ ] 가용성(Availability)의 정의를 설명할 수 있다
- [ ] SLA, SLO, SLI의 차이를 이해한다
- [ ] "9s"로 표현되는 가용성 수준을 계산할 수 있다
- [ ] Portal Universe에 적합한 가용성 목표를 설정할 수 있다

---

## 1. 가용성(Availability)이란?

### 정의

가용성은 시스템이 **정상적으로 서비스를 제공하는 시간의 비율**입니다.

```
가용성(%) = (전체 시간 - 다운타임) / 전체 시간 × 100
```

### 예시

| 가용성 | 연간 다운타임 | 월간 다운타임 | 주간 다운타임 |
|--------|--------------|--------------|--------------|
| 99% (Two 9s) | 3.65일 | 7.31시간 | 1.68시간 |
| 99.9% (Three 9s) | 8.77시간 | 43.83분 | 10.08분 |
| 99.99% (Four 9s) | 52.60분 | 4.38분 | 1.01분 |
| 99.999% (Five 9s) | 5.26분 | 26.30초 | 6.05초 |

### 현실적인 목표

| 시스템 유형 | 일반적인 목표 | 비고 |
|------------|--------------|------|
| 내부 도구 | 99% | 업무 시간 내 가용성 |
| 일반 웹 서비스 | 99.5% ~ 99.9% | 대부분의 SaaS |
| 결제/금융 시스템 | 99.99% | 규정 준수 필요 |
| 응급 서비스 | 99.999% | 생명 관련 |

> **Portal Universe 목표**: 일반 웹 서비스로서 **99.9% (Three 9s)** 목표

---

## 2. SLA, SLO, SLI

### SLI (Service Level Indicator)

**정의**: 서비스 수준을 측정하는 **지표**

```yaml
# Portal Universe SLI 예시
지표:
  - name: 요청 성공률
    formula: "성공한 요청 수 / 전체 요청 수"

  - name: 응답 시간 (p99)
    formula: "99번째 백분위수 응답 시간"

  - name: 가용성
    formula: "정상 응답 시간 / 전체 시간"
```

### SLO (Service Level Objective)

**정의**: SLI에 대한 **목표값**

```yaml
# Portal Universe SLO 예시
목표:
  - sli: 요청 성공률
    target: ">= 99.9%"
    window: "30일 롤링"

  - sli: 응답 시간 (p99)
    target: "<= 500ms"
    window: "30일 롤링"

  - sli: 가용성
    target: ">= 99.9%"
    window: "월간"
```

### SLA (Service Level Agreement)

**정의**: 고객과의 **계약**으로 SLO + 보상 조건 포함

```yaml
# 상용 SLA 예시
계약:
  - slo: 가용성 >= 99.9%
    위반_보상: "월 요금의 10% 크레딧"

  - slo: 가용성 >= 99.5%
    위반_보상: "월 요금의 25% 크레딧"

  - slo: 가용성 < 99.5%
    위반_보상: "월 요금의 50% 크레딧"
```

### 관계도

```
SLI (지표) → SLO (목표) → SLA (계약)
  ↑              ↑            ↑
측정 가능      달성 가능     비즈니스 결정
```

---

## 3. 가용성 계산

### 단일 컴포넌트

```
컴포넌트 A 가용성 = 99.9%
연간 다운타임 = 365 × 24 × (1 - 0.999) = 8.76시간
```

### 직렬 연결 (AND)

모든 컴포넌트가 동작해야 서비스 가능

```
A → B → C

전체 가용성 = A × B × C
            = 0.999 × 0.999 × 0.999
            = 0.997 (99.7%)
```

### 병렬 연결 (OR, 이중화)

하나만 동작해도 서비스 가능

```
A ─┬─ B
   └─ B' (복제본)

B 영역 가용성 = 1 - (1-B) × (1-B')
             = 1 - (1-0.999) × (1-0.999)
             = 1 - 0.000001
             = 0.999999 (99.9999%)
```

### Portal Universe 아키텍처 분석

**현재 상태 (단일 인스턴스)**:

```
Client → API Gateway → Auth Service → MySQL
            ↓
        Shopping Service → Kafka → Notification Service
            ↓
          Redis

각 컴포넌트 99.9% 가정:
전체 = 0.999^6 = 0.994 (99.4%)
연간 다운타임 = 52.6시간
```

**이중화 후 예상**:

```
Client → API Gateway (2 replicas)
              ↓
         Auth (2 replicas) → MySQL (Primary-Replica)
              ↓
         Shopping (2 replicas) → Kafka (3 brokers)
              ↓
         Redis (Sentinel)

예상 가용성: 99.95% ~ 99.99%
```

---

## 4. 가용성의 구성 요소

### 복구 시간 지표

| 지표 | 정의 | 목표 (Portal Universe) |
|------|------|----------------------|
| **MTBF** (Mean Time Between Failures) | 장애 사이 평균 시간 | 최대화 |
| **MTTR** (Mean Time To Recovery) | 복구 평균 시간 | 최소화 |
| **MTTA** (Mean Time To Acknowledge) | 인지 평균 시간 | < 5분 |
| **MTTD** (Mean Time To Detect) | 감지 평균 시간 | < 1분 |

### 가용성 공식

```
가용성 = MTBF / (MTBF + MTTR)
```

예시:
- MTBF = 720시간 (30일)
- MTTR = 0.72시간 (43분)
- 가용성 = 720 / (720 + 0.72) = 99.9%

### 가용성 향상 전략

| 전략 | 목표 지표 | 방법 |
|------|----------|------|
| **예방** | MTBF 증가 | 이중화, 부하 분산, 용량 계획 |
| **감지** | MTTD 감소 | 모니터링, 알림, Health Check |
| **대응** | MTTA 감소 | On-call, Runbook, 자동화 |
| **복구** | MTTR 감소 | Auto-healing, Rollback, Failover |

---

## 5. Portal Universe SLO 정의

### API Gateway

```yaml
slo:
  - name: 가용성
    target: ">= 99.9%"
    measurement: "HTTP 2xx + 3xx / 전체 요청"
    window: "30일 롤링"

  - name: 응답 시간
    target: "p99 <= 500ms"
    exclude: "/api/v1/chat/stream"  # SSE는 제외
    window: "30일 롤링"
```

### 인증 서비스 (Auth)

```yaml
slo:
  - name: 로그인 성공률
    target: ">= 99.95%"
    measurement: "성공 로그인 / 유효한 자격증명 로그인 시도"
    window: "7일 롤링"

  - name: 토큰 발급 시간
    target: "p99 <= 200ms"
    window: "30일 롤링"
```

### 쇼핑 서비스 (Shopping)

```yaml
slo:
  - name: 주문 성공률
    target: ">= 99.9%"
    measurement: "성공 주문 / 전체 주문 시도"
    window: "30일 롤링"

  - name: 결제 처리 시간
    target: "p99 <= 3s"
    window: "30일 롤링"
```

---

## 실습: SLI 측정

### 1. Prometheus에서 SLI 쿼리

```promql
# 요청 성공률 (5분 단위)
sum(rate(http_server_requests_seconds_count{status=~"2.."}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m]))

# p99 응답 시간
histogram_quantile(0.99,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le)
)

# 가용성 (서비스 업타임)
avg_over_time(up{job="api-gateway"}[24h])
```

### 2. Grafana 대시보드 생성

SLO 추적을 위한 대시보드를 구성합니다:

1. Grafana 접속 (http://localhost:3000)
2. Create Dashboard
3. 아래 패널 추가:
   - **Success Rate**: 요청 성공률 (Gauge)
   - **Latency p99**: 응답 시간 (Time Series)
   - **Error Budget**: 남은 에러 예산 (Stat)

---

## 에러 예산 (Error Budget)

### 개념

에러 예산 = 100% - SLO 목표

```
SLO: 99.9%
에러 예산: 0.1% = 월 43.2분

사용 가능한 다운타임:
- 계획된 유지보수
- 장애 복구 시간
- 배포로 인한 일시적 에러
```

### 에러 예산 정책

| 에러 예산 소진율 | 액션 |
|-----------------|------|
| 0-50% | 정상 개발 진행 |
| 50-80% | 신규 기능 배포 신중히 |
| 80-100% | 안정성 작업 우선 |
| 100%+ | 기능 배포 중단, 안정성 집중 |

---

## 핵심 정리

1. **가용성**은 서비스가 정상 동작하는 시간의 비율입니다
2. **SLI**는 측정 가능한 지표, **SLO**는 목표값, **SLA**는 계약입니다
3. **직렬 구조**는 가용성을 곱하고, **병렬(이중화)**는 장애 확률을 곱합니다
4. **MTTR 감소**가 MTBF 증가보다 현실적인 개선 방법입니다
5. **에러 예산**으로 안정성과 개발 속도의 균형을 맞춥니다

---

## 다음 단계

[02-failure-modes.md](./02-failure-modes.md) - 장애 유형 분류를 학습합니다.

---

## 참고 자료

- [Google SRE Book - Chapter 4: Service Level Objectives](https://sre.google/sre-book/service-level-objectives/)
- [The Site Reliability Workbook - Implementing SLOs](https://sre.google/workbook/implementing-slos/)
