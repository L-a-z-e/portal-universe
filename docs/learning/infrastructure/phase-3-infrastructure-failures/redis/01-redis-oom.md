# Redis OOM (Out of Memory) 시나리오

Redis가 메모리 부족으로 동작하지 않을 때의 영향과 대응 방법을 학습합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 리소스 고갈 |
| **영향 범위** | Rate Limiting, 세션, 캐시 |
| **난이도** | ⭐⭐⭐ |
| **예상 시간** | 30분 |

---

## 1. 현재 설정 분석

**파일**: `k8s/infrastructure/redis.yaml`

```yaml
resources:
  requests:
    memory: "128Mi"
  limits:
    memory: "256Mi"
```

### Portal Universe에서 Redis 용도

| 용도 | 영향받는 서비스 |
|------|----------------|
| Rate Limiting | API Gateway |
| 세션 저장 | Auth Service |
| 캐시 | Shopping Service |

---

## 2. 예상 영향

### Redis OOM 시 API Gateway 동작

Rate Limiting이 Redis에 의존하므로:

**Fail-Open** (현재 설정):
- Redis 연결 실패 시 요청 통과
- DDoS 방어 불가

**Fail-Closed** (대안):
- Redis 연결 실패 시 모든 요청 거부
- 정상 사용자도 차단

---

## 3. 장애 주입 절차

### Step 1: 현재 메모리 사용량 확인

```bash
# Redis 메모리 정보
kubectl exec -it $(kubectl get pod -n portal-universe -l app=redis -o jsonpath='{.items[0].metadata.name}') -n portal-universe -- \
  redis-cli INFO memory | grep -E "used_memory_human|maxmemory"
```

### Step 2: 메모리 제한 낮추기

```bash
# 64Mi로 제한 (OOM 유발)
kubectl patch deployment redis -n portal-universe -p \
  '{"spec":{"template":{"spec":{"containers":[{"name":"redis","resources":{"limits":{"memory":"64Mi"}}}]}}}}'
```

### Step 3: OOM 관찰

```bash
# Pod 상태 모니터링
kubectl get pods -n portal-universe -l app=redis -w

# API Gateway 로그에서 Redis 연결 에러 확인
kubectl logs -f deployment/api-gateway -n portal-universe | grep -i redis
```

### Step 4: 복구

```bash
# 원래 제한으로 복원
kubectl patch deployment redis -n portal-universe -p \
  '{"spec":{"template":{"spec":{"containers":[{"name":"redis","resources":{"limits":{"memory":"256Mi"}}}]}}}}'
```

---

## 4. 개선 방안

### 메모리 정책 설정

```bash
# maxmemory-policy 설정 (LRU)
kubectl exec -it <redis-pod> -n portal-universe -- \
  redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

### 알림 규칙

```yaml
- alert: RedisMemoryHigh
  expr: redis_memory_used_bytes / redis_memory_max_bytes > 0.85
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Redis 메모리 85% 초과"
```

---

## 5. 체크리스트

- [ ] 현재 Redis 메모리 설정 확인
- [ ] OOM 발생 시 API Gateway 동작 확인
- [ ] 메모리 정책 설정
- [ ] 복구 절차 확인

---

## 다음 시나리오

[02-redis-connection-pool.md](./02-redis-connection-pool.md) - 연결 풀 고갈
