# 장애 유형 분류

시스템에서 발생할 수 있는 다양한 장애 유형을 분류하고 특성을 이해합니다.

---

## 학습 목표

- [ ] 장애를 여러 기준으로 분류할 수 있다
- [ ] 각 장애 유형의 특성과 대응 전략을 이해한다
- [ ] Portal Universe에서 발생 가능한 장애를 식별할 수 있다

---

## 1. 범위에 따른 분류

### 부분 장애 (Partial Failure)

시스템의 일부만 영향받는 장애입니다.

```
[정상] API Gateway → [장애] Auth Service → [정상] MySQL
                  → [정상] Shopping Service → [정상] Kafka
```

**특징**:
- 일부 기능만 영향받음
- 장애 격리가 중요
- Circuit Breaker로 확산 방지 가능

**Portal Universe 예시**:
- Auth Service 다운: 로그인/회원가입 불가, 상품 조회는 가능
- Kafka 장애: 알림 지연, 주문은 정상 처리

### 전체 장애 (Total Failure)

시스템 전체가 서비스 불가 상태입니다.

```
[장애] API Gateway → 모든 백엔드 접근 불가
```

**특징**:
- 모든 기능 중단
- 최우선 복구 대상
- 예방이 가장 중요

**Portal Universe 예시**:
- API Gateway 다운: 전체 서비스 중단
- Kubernetes Control Plane 장애

---

## 2. 시간에 따른 분류

### 일시적 장애 (Transient Failure)

짧은 시간 동안 발생하고 자동 복구되는 장애입니다.

**예시**:
- 네트워크 타임아웃 (일시적 패킷 손실)
- GC(Garbage Collection) 일시 정지
- Pod 재시작 중 짧은 다운타임

**대응**:
```java
// Retry with exponential backoff
@Retryable(
    value = {TransientException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public Result callService() {
    return externalService.call();
}
```

### 간헐적 장애 (Intermittent Failure)

불규칙하게 반복되는 장애입니다.

**예시**:
- 특정 조건에서만 발생하는 버그
- 리소스 경합 (Race Condition)
- 메모리 누수 (점진적으로 증가)

**대응**:
- 상세한 로깅 필요
- 패턴 분석
- 재현 환경 구축

### 영구 장애 (Permanent Failure)

수동 개입 없이는 복구되지 않는 장애입니다.

**예시**:
- 디스크 고장
- 데이터 손상
- 설정 오류로 인한 부팅 실패

**대응**:
- 자동 알림 필수
- Runbook 기반 복구
- 인프라 이중화

---

## 3. 원인에 따른 분류

### 하드웨어 장애

| 유형 | 빈도 | 영향 | 대응 |
|------|------|------|------|
| 디스크 고장 | 중 | 데이터 손실 | RAID, 복제 |
| 메모리 오류 | 저 | 크래시, 데이터 손상 | ECC 메모리 |
| 네트워크 카드 | 저 | 연결 불가 | 이중화 |
| 전원 장애 | 저 | 전체 다운 | UPS, 이중화 |

### 소프트웨어 장애

| 유형 | 원인 | 증상 | 대응 |
|------|------|------|------|
| 메모리 누수 | 코드 버그 | OOM, 느려짐 | 프로파일링, 패치 |
| 교착 상태 | 동시성 버그 | 응답 없음 | 스레드 덤프 분석 |
| 무한 루프 | 로직 오류 | CPU 100% | 타임아웃, 모니터링 |
| 설정 오류 | 잘못된 설정 | 시작 실패 | 검증, 롤백 |

### 운영 장애

| 유형 | 예시 | 예방 |
|------|------|------|
| 잘못된 배포 | 버그 있는 버전 배포 | 카나리 배포, 자동화 테스트 |
| 설정 변경 실수 | 잘못된 환경변수 | 변경 관리, 코드 리뷰 |
| 용량 부족 | 디스크/메모리 풀 | 모니터링, 알림 |
| 인증서 만료 | HTTPS 인증서 | 만료 알림, 자동 갱신 |

### 외부 의존성 장애

| 유형 | 예시 | 대응 |
|------|------|------|
| 클라우드 서비스 | AWS/GCP 장애 | 멀티 리전, Fallback |
| 외부 API | 결제 게이트웨이 | 타임아웃, 재시도, 대체 경로 |
| DNS | DNS 서버 장애 | 로컬 캐싱, 다중 DNS |

---

## 4. 장애 영향 모델 (Failure Impact Model)

### 장애 도메인 (Failure Domain)

**정의**: 단일 장애로 영향받는 컴포넌트 범위

```
Level 1: 단일 Pod
Level 2: 단일 Node
Level 3: 단일 Availability Zone
Level 4: 단일 Region
Level 5: 전체 클라우드 프로바이더
```

### Portal Universe 장애 도메인

```yaml
# 현재 상태 (높은 위험)
장애_도메인:
  - domain: "Redis Pod"
    영향: "전체 Rate Limiting 실패, 세션 손실"

  - domain: "Kafka Pod"
    영향: "모든 이벤트 전달 실패"

  - domain: "MySQL Pod"
    영향: "전체 데이터 접근 불가"
```

### Blast Radius 분석

장애 발생 시 영향 범위를 시각화합니다.

```
[Kafka 장애 발생]
    │
    ├── 직접 영향
    │   ├── Shopping Service: 주문 이벤트 발행 실패
    │   └── Notification Service: 이벤트 수신 실패
    │
    └── 간접 영향
        ├── 사용자: 알림 미수신
        └── 관리자: 실시간 모니터링 불가
```

---

## 5. 장애 패턴 (Failure Patterns)

### Cascading Failure (연쇄 장애)

하나의 장애가 다른 컴포넌트로 전파됩니다.

```
[시나리오]
1. Auth Service 응답 지연 (2초 → 10초)
2. API Gateway 스레드 풀 고갈 (대기 중인 요청 증가)
3. Shopping Service 타임아웃 증가
4. 전체 시스템 응답 불가
```

**방어**:
```yaml
# Circuit Breaker 설정 (application.yml)
resilience4j:
  circuitbreaker:
    instances:
      authCircuitBreaker:
        sliding-window-size: 20
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
```

### Thundering Herd (우레 떼)

동시에 많은 요청이 몰려 시스템을 압도합니다.

```
[시나리오]
1. Redis 재시작 (캐시 비움)
2. 모든 요청이 DB로 직접 접근
3. DB 과부하 → 전체 시스템 다운
```

**방어**:
```java
// Cache-aside with mutex
String value = cache.get(key);
if (value == null) {
    if (lock.tryAcquire(key, 5, TimeUnit.SECONDS)) {
        try {
            value = db.query(key);
            cache.set(key, value);
        } finally {
            lock.release(key);
        }
    } else {
        // 다른 스레드가 캐싱 중 - 대기 또는 stale 데이터 반환
        value = cache.get(key); // retry
    }
}
```

### Gray Failure (회색 장애)

완전히 죽지 않고 느리게 동작하는 상태입니다.

```
[시나리오]
1. 서비스가 200 OK 응답하지만 5초 걸림
2. Health Check는 통과 (UP)
3. 사용자 경험은 심각하게 저하
```

**방어**:
```yaml
# Liveness vs Readiness 분리
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  # 응답 여부만 확인

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  timeoutSeconds: 3  # 3초 이상이면 트래픽 차단
```

---

## 6. Portal Universe 장애 카탈로그

### 인프라 장애

| ID | 컴포넌트 | 장애 유형 | 예상 영향 | Phase 3 문서 |
|----|---------|----------|----------|--------------|
| INF-001 | Kafka | 브로커 다운 | 이벤트 발행 실패 | [kafka/01-kafka-broker-down.md](../phase-3-infrastructure-failures/kafka/01-kafka-broker-down.md) |
| INF-002 | Redis | OOM | Rate Limiting 실패 | [redis/01-redis-oom.md](../phase-3-infrastructure-failures/redis/01-redis-oom.md) |
| INF-003 | MySQL | 커넥션 풀 고갈 | 데이터 접근 불가 | [mysql/01-mysql-connection-pool.md](../phase-3-infrastructure-failures/mysql/01-mysql-connection-pool.md) |
| INF-004 | Elasticsearch | 메모리 부족 | 검색 실패 | [elasticsearch/01-es-memory-oom.md](../phase-3-infrastructure-failures/elasticsearch/01-es-memory-oom.md) |

### 서비스 장애

| ID | 컴포넌트 | 장애 유형 | 예상 영향 | Phase 3 문서 |
|----|---------|----------|----------|--------------|
| SVC-001 | API Gateway | 스레드 고갈 | 전체 서비스 불가 | [services/02-service-thread-exhaustion.md](../phase-3-infrastructure-failures/services/02-service-thread-exhaustion.md) |
| SVC-002 | Auth Service | 메모리 누수 | 인증 실패 | [services/01-service-memory-leak.md](../phase-3-infrastructure-failures/services/01-service-memory-leak.md) |
| SVC-003 | 전체 | 연쇄 장애 | 점진적 시스템 다운 | [services/03-service-cascade-failure.md](../phase-3-infrastructure-failures/services/03-service-cascade-failure.md) |

---

## 핵심 정리

1. **부분 장애**가 더 자주 발생하며, **장애 격리**가 핵심입니다
2. **일시적 장애**는 Retry로, **영구 장애**는 이중화로 대응합니다
3. **Cascading Failure** 방지를 위해 Circuit Breaker가 필수입니다
4. **Gray Failure** 감지를 위해 Latency 기반 Health Check가 필요합니다
5. **장애 카탈로그**를 미리 작성하면 대응 시간이 단축됩니다

---

## 다음 단계

[03-metrics-mttr-mtbf.md](./03-metrics-mttr-mtbf.md) - MTTR, MTBF 지표를 학습합니다.

---

## 참고 자료

- [Google SRE Book - Chapter 10: Postmortems](https://sre.google/sre-book/postmortem-culture/)
- [AWS Architecture Blog - Failure Modes](https://aws.amazon.com/blogs/architecture/)
