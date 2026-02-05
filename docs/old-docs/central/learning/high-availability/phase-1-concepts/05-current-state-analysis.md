# Portal Universe 현재 상태 분석

Portal Universe의 현재 고가용성 상태를 분석하고 개선 포인트를 식별합니다.

---

## 학습 목표

- [ ] 현재 인프라의 강점과 약점을 파악할 수 있다
- [ ] 단일 장애점(SPOF)을 식별할 수 있다
- [ ] 개선 우선순위를 결정할 수 있다

---

## 1. 현재 아키텍처 개요

### 서비스 구성

```
┌─────────────────────────────────────────────────────────────────┐
│                        Portal Universe                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Frontend]                                                     │
│  ├── portal-shell (Vue 3, :30000)                               │
│  ├── blog-frontend (Vue 3, :30001)                              │
│  ├── shopping-frontend (React 18, :30002)                       │
│  └── prism-frontend (React 18, :30003)                          │
│                                                                 │
│  [API Gateway]                                                  │
│  └── api-gateway (Spring Boot, :8080) ─────────────────┐        │
│                                                        │        │
│  [Backend Services]                                    │        │
│  ├── auth-service (Spring Boot, :8081) ◄───────────────┤        │
│  ├── blog-service (Spring Boot, :8082) ◄───────────────┤        │
│  ├── shopping-service (Spring Boot, :8083) ◄───────────┤        │
│  ├── notification-service (Spring Boot, :8084) ◄───────┤        │
│  ├── prism-service (NestJS, :8085) ◄───────────────────┤        │
│  └── chatbot-service (Python, :8086) ◄─────────────────┘        │
│                                                                 │
│  [Infrastructure]                                               │
│  ├── MySQL (:3306)                                              │
│  ├── Redis (:6379)                                              │
│  ├── Kafka (:9092)                                              │
│  ├── Elasticsearch (:9200)                                      │
│  └── MongoDB (:27017)                                           │
│                                                                 │
│  [Observability]                                                │
│  ├── Prometheus (:9090)                                         │
│  ├── Grafana (:3000)                                            │
│  └── Zipkin (:9411)                                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 이미 구현된 것들 (강점)

### ✅ Circuit Breaker (Resilience4j)

**위치**: `services/api-gateway/src/main/resources/application.yml`

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-type: count_based
        sliding-window-size: 20
        failure-rate-threshold: 50           # 50% 실패 시 열림
        wait-duration-in-open-state: 10s     # 10초 후 Half-Open
        permitted-number-of-calls-in-half-open-state: 5
        automatic-transition-from-open-to-half-open-enabled: true
```

**효과**:
- 연쇄 장애(Cascading Failure) 방지
- 장애 서비스 격리
- 빠른 실패(Fail Fast)로 리소스 보호

### ✅ Rate Limiting (Redis 기반)

**위치**: `services/api-gateway/.../RateLimiterConfig.java`

```java
// 5가지 Rate Limiter 전략
@Bean
public RedisRateLimiter strictRedisRateLimiter() {
    // 로그인: 1 req/sec, burst 5 (Brute Force 방어)
    return new RedisRateLimiter(1, 5, 1);
}

@Bean
public RedisRateLimiter authenticatedRedisRateLimiter() {
    // 인증 사용자: 2 req/sec, burst 100
    return new RedisRateLimiter(2, 100, 1);
}
```

**효과**:
- DDoS 방어
- 리소스 공정 분배
- API 남용 방지

### ✅ Health Check (3-tier Probes)

**위치**: `k8s/services/api-gateway.yaml`

```yaml
# Startup Probe: 애플리케이션 시작 대기
startupProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 18  # 최대 3분

# Liveness Probe: 응답 여부 확인
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  periodSeconds: 10
  failureThreshold: 3

# Readiness Probe: 트래픽 수신 가능 여부
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  periodSeconds: 5
  failureThreshold: 3
```

**효과**:
- 느린 시작 서비스 보호
- 비정상 Pod 자동 재시작
- 트래픽 라우팅 제어

### ✅ Monitoring Stack

| 도구 | 용도 | 위치 |
|------|------|------|
| Prometheus | 메트릭 수집 | `monitoring/prometheus/` |
| Grafana | 시각화 | `monitoring/grafana/` |
| Zipkin | 분산 추적 | 서비스 설정 |

### ✅ 부하 테스트 (k6)

**위치**: `services/load-tests/k6/scenarios/`

| 시나리오 | 파일 | 목적 |
|---------|------|------|
| 쇼핑 플로우 | `a-shopping-flow.js` | E2E 사용자 흐름 |
| 블로그 읽기 | `b-blog-read.js` | 읽기 부하 |
| 쿠폰 스파이크 | `c-coupon-spike.js` | 급격한 부하 |
| 검색 부하 | `d-search-load.js` | 검색 성능 |
| 캐시 Thundering | `e-cache-thundering.js` | 캐시 실패 시나리오 |

---

## 3. 개선이 필요한 부분 (약점)

### ⚠️ 단일 장애점 (SPOF) 분석

| 컴포넌트 | 현재 | 위험도 | 영향 |
|----------|------|--------|------|
| **Kafka** | replicas: 1 | 🔴 높음 | 모든 이벤트 전달 실패 |
| **Redis** | replicas: 1 | 🔴 높음 | Rate Limiting 실패, 세션 손실 |
| **MySQL** | replicas: 1 | 🔴 높음 | 전체 데이터 접근 불가 |
| **API Gateway** | replicas: 1 | 🔴 높음 | 전체 서비스 접근 불가 |
| **각 서비스** | replicas: 1 | 🟡 중간 | 해당 기능 중단 |
| **Elasticsearch** | replicas: 1 | 🟡 중간 | 검색 기능 중단 |

### 현재 설정 확인

**Kafka (k8s/infrastructure/kafka.yaml)**:
```yaml
spec:
  replicas: 1  # ⚠️ SPOF
  # ...
  env:
    - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
      value: "1"  # ⚠️ 복제 없음
```

**Redis (k8s/infrastructure/redis.yaml)**:
```yaml
spec:
  replicas: 1  # ⚠️ SPOF
  # Sentinel 미설정
```

**서비스들 (k8s/services/api-gateway.yaml)**:
```yaml
spec:
  replicas: 1  # ⚠️ SPOF
  # HPA 미설정
  # PDB 미설정
```

### ⚠️ 자동 스케일링 미설정

```yaml
# HPA가 없음
# 부하 증가 시 수동 스케일링 필요
# kubectl scale deployment api-gateway --replicas=3
```

### ⚠️ Pod Disruption Budget 미설정

```yaml
# PDB가 없음
# 노드 유지보수 시 모든 Pod가 동시에 중단될 수 있음
```

### ⚠️ Graceful Shutdown 부분 구현

```yaml
# terminationGracePeriodSeconds 설정 없음
# 기본값 30초 사용
# 진행 중인 요청 처리 보장 불확실
```

---

## 4. 위험 시나리오

### 시나리오 1: Kafka 다운

```
[현재 상태]
1. Kafka Pod 크래시
2. 모든 Producer 실패 (TimeoutException)
3. Shopping Service 주문 이벤트 발행 불가
4. Notification Service 알림 수신 불가
5. 이벤트 유실 위험

[영향]
- 주문 후 알림 미발송
- 이벤트 기반 기능 전체 중단
- 복구 후 이벤트 유실
```

### 시나리오 2: Redis 다운

```
[현재 상태]
1. Redis Pod 크래시
2. Rate Limiting 실패 (모든 요청 통과 또는 차단)
3. 세션 데이터 손실
4. 캐시 데이터 손실

[영향]
- DDoS 방어 불가 또는 정상 요청 차단
- 사용자 로그아웃 (세션 손실)
- DB 부하 급증 (캐시 미스)
```

### 시나리오 3: API Gateway 다운

```
[현재 상태]
1. API Gateway Pod 크래시
2. 모든 외부 요청 실패
3. Frontend → Backend 통신 불가

[영향]
- 전체 서비스 중단
- 모든 사용자 영향
```

---

## 5. 가용성 예측

### 현재 상태 (모두 replicas: 1)

```
컴포넌트별 가용성: 99.9% 가정

핵심 경로:
Client → API Gateway → Auth → MySQL

가용성 = 0.999 × 0.999 × 0.999 × 0.999
       = 0.996 (99.6%)

연간 다운타임 = 365 × 24 × 0.004 = 35시간
```

### 개선 후 예측 (이중화 적용)

```
컴포넌트별 가용성: 99.99% (이중화)

핵심 경로:
Client → API Gateway(2) → Auth(2) → MySQL(Primary-Replica)

가용성 = 0.9999 × 0.9999 × 0.9999 × 0.9999
       = 0.9996 (99.96%)

연간 다운타임 = 365 × 24 × 0.0004 = 3.5시간
```

---

## 6. 개선 우선순위

### 🔴 P0 (즉시)

| 항목 | 이유 | Phase 4 문서 |
|------|------|--------------|
| 서비스 replicas ≥ 2 | 전체 서비스 가용성 | [01-replicas-scaling.md](../phase-4-ha-architecture/01-replicas-scaling.md) |
| PDB 설정 | 유지보수 안전성 | [03-pdb-setup.md](../phase-4-ha-architecture/03-pdb-setup.md) |

### 🟠 P1 (단기)

| 항목 | 이유 | Phase 4 문서 |
|------|------|--------------|
| HPA 설정 | 자동 스케일링 | [02-hpa-setup.md](../phase-4-ha-architecture/02-hpa-setup.md) |
| Kafka 3-노드 | 이벤트 안정성 | [04-kafka-replication.md](../phase-4-ha-architecture/04-kafka-replication.md) |
| Redis Sentinel | Rate Limiting 안정성 | [05-redis-sentinel.md](../phase-4-ha-architecture/05-redis-sentinel.md) |

### 🟡 P2 (중기)

| 항목 | 이유 | Phase 4 문서 |
|------|------|--------------|
| MySQL Replication | 데이터 보호 | [06-mysql-replication.md](../phase-4-ha-architecture/06-mysql-replication.md) |
| Graceful Shutdown | 무중단 배포 | [07-graceful-shutdown.md](../phase-4-ha-architecture/07-graceful-shutdown.md) |

### 🟢 P3 (장기)

| 항목 | 이유 | Phase 4 문서 |
|------|------|--------------|
| Multi-AZ 배포 | 재해 복구 | [08-multi-zone-deployment.md](../phase-4-ha-architecture/08-multi-zone-deployment.md) |

---

## 7. 빠른 개선 (Quick Wins)

### 지금 바로 적용 가능

```bash
# 서비스 replicas 증가 (1 → 2)
kubectl scale deployment api-gateway --replicas=2 -n portal-universe
kubectl scale deployment auth-service --replicas=2 -n portal-universe
kubectl scale deployment shopping-service --replicas=2 -n portal-universe
```

### YAML 변경

```yaml
# k8s/services/api-gateway.yaml
spec:
  replicas: 2  # 1 → 2로 변경
```

### 리소스 요구사항 확인

```bash
# 노드 리소스 확인
kubectl top nodes

# 현재 사용량 확인
kubectl top pods -n portal-universe
```

---

## 핵심 정리

1. **강점**: Circuit Breaker, Rate Limiting, Health Check, Monitoring 이미 구현됨
2. **약점**: 모든 컴포넌트가 단일 인스턴스 (SPOF)
3. **즉시 개선**: replicas ≥ 2, PDB 설정
4. **단기 개선**: HPA, Kafka/Redis 이중화
5. **현재 가용성**: 약 99.6% → 목표: 99.9%

---

## 다음 단계

Phase 1 완료! Phase 2로 진행합니다.

[Phase 2: Chaos Engineering 기초](../phase-2-chaos-engineering/01-chaos-engineering-intro.md)

---

## 참고

- 현재 K8s 매니페스트: `k8s/` 디렉토리
- 모니터링 설정: `monitoring/` 디렉토리
- 서비스 설정: `services/*/src/main/resources/application*.yml`
