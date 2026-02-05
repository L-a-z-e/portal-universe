# Redis 연결 풀 고갈 시나리오

Redis 연결 풀이 고갈되었을 때의 영향을 분석합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 연결 리소스 고갈 |
| **영향 범위** | Redis 의존 서비스 |
| **난이도** | ⭐⭐⭐ |
| **예상 시간** | 25분 |

---

## 1. 연결 풀 설정 확인

**API Gateway 설정** (`application.yml`):

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 8      # 최대 연결 수
          max-idle: 8        # 최대 유휴 연결
          min-idle: 0        # 최소 유휴 연결
```

---

## 2. 고갈 원인

1. **트래픽 급증**: 동시 요청 > max-active
2. **연결 누수**: 반환되지 않는 연결
3. **느린 Redis 응답**: 연결 점유 시간 증가

---

## 3. 시뮬레이션

### 부하 테스트로 연결 고갈 유발

```bash
# k6 부하 테스트 (Rate Limiting 엔드포인트 집중 공격)
k6 run services/load-tests/k6/scenarios/c-coupon-spike.js
```

### 연결 수 모니터링

```bash
# Redis 연결 수 확인
kubectl exec -it <redis-pod> -n portal-universe -- \
  redis-cli INFO clients | grep connected_clients
```

---

## 4. 증상

- `Unable to acquire connection from pool`
- 요청 타임아웃 증가
- Rate Limiting 실패

---

## 5. 개선 방안

### 연결 풀 크기 증가

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20  # 증가
          max-wait: 2s    # 대기 시간 설정
```

### 알림 규칙

```yaml
- alert: RedisConnectionsHigh
  expr: redis_connected_clients > 100
  for: 5m
  labels:
    severity: warning
```

---

## 다음 시나리오

[03-redis-latency-spike.md](./03-redis-latency-spike.md) - 지연 급증
