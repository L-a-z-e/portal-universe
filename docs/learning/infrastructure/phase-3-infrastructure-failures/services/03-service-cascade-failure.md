# 연쇄 장애 (Cascading Failure) 시나리오

하나의 장애가 다른 서비스로 전파되는 연쇄 장애를 분석합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 시스템 전체 영향 |
| **영향 범위** | 다중 서비스 |
| **난이도** | ⭐⭐⭐⭐ |
| **예상 시간** | 45분 |

---

## 1. 연쇄 장애 패턴

```
[시나리오]
1. Auth Service 응답 지연 (2초 → 10초)
2. API Gateway 스레드 대기 증가
3. API Gateway 스레드 풀 고갈
4. 모든 요청 처리 불가
5. 다른 서비스도 타임아웃
```

---

## 2. 방어 메커니즘

### Circuit Breaker

```yaml
resilience4j:
  circuitbreaker:
    instances:
      authCircuitBreaker:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
```

### 타임아웃

```yaml
resilience4j:
  timelimiter:
    instances:
      authCircuitBreaker:
        timeout-duration: 5s
```

---

## 3. 시뮬레이션

### Auth Service 지연 유발

```bash
# Auth Service replicas를 0으로
kubectl scale deployment auth-service -n portal-universe --replicas=0

# 또는 네트워크 지연 주입
```

### 영향 관찰

```bash
# API Gateway 로그
kubectl logs -f deployment/api-gateway -n portal-universe

# Circuit Breaker 상태
curl localhost:8080/actuator/health | jq '.components.circuitBreakers'
```

---

## 4. 복구

```bash
# Circuit Breaker 리셋 (필요시)
# Auth Service 복구
kubectl scale deployment auth-service -n portal-universe --replicas=1
```

---

## 5. 핵심 교훈

1. **Circuit Breaker**가 장애 격리에 필수
2. **타임아웃**이 없으면 스레드 고갈
3. **Fallback** 응답으로 graceful degradation
4. **Bulkhead**로 리소스 격리

---

## 다음 시나리오

[04-network-partition.md](./04-network-partition.md) - 네트워크 파티션
