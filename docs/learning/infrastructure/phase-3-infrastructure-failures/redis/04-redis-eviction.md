# Redis 키 Eviction 시나리오

메모리 부족으로 Redis가 키를 자동 삭제(Eviction)할 때의 영향을 분석합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 데이터 손실 |
| **영향 범위** | 캐시 미스, 세션 손실 |
| **난이도** | ⭐⭐ |
| **예상 시간** | 20분 |

---

## 1. Eviction Policy 종류

| 정책 | 설명 |
|------|------|
| `noeviction` | 메모리 초과 시 에러 반환 |
| `allkeys-lru` | 모든 키에서 LRU 삭제 |
| `volatile-lru` | TTL 있는 키에서 LRU 삭제 |
| `allkeys-random` | 모든 키에서 랜덤 삭제 |

---

## 2. 현재 설정 확인

```bash
kubectl exec -it <redis-pod> -n portal-universe -- \
  redis-cli CONFIG GET maxmemory-policy
```

---

## 3. Eviction 모니터링

```bash
# Evicted 키 수 확인
kubectl exec -it <redis-pod> -n portal-universe -- \
  redis-cli INFO stats | grep evicted_keys
```

---

## 4. 영향

- **캐시 미스 증가**: DB 부하 증가
- **세션 손실**: 사용자 로그아웃
- **Rate Limit 카운터 초기화**: 보안 위험

---

## 5. 개선 방안

### 용도별 Redis 분리

```yaml
# 캐시용 (eviction 허용)
redis-cache:
  maxmemory-policy: allkeys-lru

# 세션용 (eviction 불가)
redis-session:
  maxmemory-policy: noeviction
```

### 알림 규칙

```yaml
- alert: RedisEviction
  expr: increase(redis_evicted_keys_total[5m]) > 100
  labels:
    severity: warning
  annotations:
    summary: "Redis 키 Eviction 발생"
```

---

## 다음 섹션

[MySQL 장애 시나리오](../mysql/01-mysql-connection-pool.md)
