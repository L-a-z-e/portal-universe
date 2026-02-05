# Redis 지연 급증 시나리오

Redis 응답 시간이 급증했을 때의 영향을 분석합니다.

---

## 시나리오 개요

| 항목 | 내용 |
|------|------|
| **장애 유형** | 성능 저하 |
| **영향 범위** | 전체 응답 시간 증가 |
| **난이도** | ⭐⭐⭐ |
| **예상 시간** | 25분 |

---

## 1. 지연 원인

1. **메모리 부족**: Swap 사용
2. **큰 키**: 대량 데이터 전송
3. **느린 명령**: KEYS *, SMEMBERS 등
4. **네트워크 문제**: 패킷 손실

---

## 2. 모니터링

### Redis Slow Log 확인

```bash
kubectl exec -it <redis-pod> -n portal-universe -- \
  redis-cli SLOWLOG GET 10
```

### 명령 실행 시간 확인

```bash
kubectl exec -it <redis-pod> -n portal-universe -- \
  redis-cli --latency
```

---

## 3. 시뮬레이션 (주의)

### DEBUG SLEEP (테스트용)

```bash
# Redis 인위적 지연 (2초)
kubectl exec -it <redis-pod> -n portal-universe -- \
  redis-cli DEBUG SLEEP 2
```

---

## 4. 영향 분석

- Rate Limiting 타임아웃
- 세션 조회 지연
- 전체 API 응답 시간 증가

---

## 5. 개선 방안

### 타임아웃 설정

```yaml
spring:
  data:
    redis:
      timeout: 2000ms  # 2초 타임아웃
```

### Slow Log 임계값 설정

```bash
redis-cli CONFIG SET slowlog-log-slower-than 10000  # 10ms 이상
```

---

## 다음 시나리오

[04-redis-eviction.md](./04-redis-eviction.md) - 키 Eviction
