# Redis Sentinel 설정

Redis 고가용성을 위한 Sentinel을 구성합니다.

---

## 학습 목표

- [ ] Redis Sentinel의 역할을 이해한다
- [ ] Master-Slave + Sentinel 구조를 구성할 수 있다
- [ ] Failover를 테스트할 수 있다

---

## 1. Redis Sentinel이란?

### 역할

1. **모니터링**: Master/Slave 상태 감시
2. **알림**: 장애 발생 시 알림
3. **자동 Failover**: Master 장애 시 Slave 승격
4. **설정 제공**: 클라이언트에게 현재 Master 정보 제공

### 구조

```
┌─────────────┐
│  Sentinel 1 │──┐
└─────────────┘  │
                 │ 모니터링
┌─────────────┐  │
│  Sentinel 2 │──┼──→ Master ←──→ Slave 1
└─────────────┘  │         ↖
                 │          Slave 2
┌─────────────┐  │
│  Sentinel 3 │──┘
└─────────────┘
```

---

## 2. 간소화된 구성 (Kind 환경)

### Master + Slave StatefulSet

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis
  namespace: portal-universe
spec:
  serviceName: redis
  replicas: 3
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          command:
            - redis-server
          args:
            - --appendonly
            - "yes"
            - --replicaof
            - redis-0.redis.portal-universe.svc.cluster.local
            - "6379"
          ports:
            - containerPort: 6379
```

### Sentinel ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: redis-sentinel-config
  namespace: portal-universe
data:
  sentinel.conf: |
    sentinel monitor mymaster redis-0.redis 6379 2
    sentinel down-after-milliseconds mymaster 5000
    sentinel failover-timeout mymaster 60000
    sentinel parallel-syncs mymaster 1
```

---

## 3. Spring Boot 연결

```yaml
spring:
  redis:
    sentinel:
      master: mymaster
      nodes:
        - sentinel-0.sentinel:26379
        - sentinel-1.sentinel:26379
        - sentinel-2.sentinel:26379
```

---

## 4. Failover 테스트

```bash
# Master 확인
kubectl exec -it redis-0 -n portal-universe -- redis-cli INFO replication

# Master Pod 삭제
kubectl delete pod redis-0 -n portal-universe

# Sentinel 로그에서 Failover 확인
kubectl logs sentinel-0 -n portal-universe | grep failover

# 새 Master 확인
kubectl exec -it redis-1 -n portal-universe -- redis-cli INFO replication
```

---

## 5. 효과

| 항목 | 단일 Redis | Sentinel |
|------|-----------|----------|
| Master 장애 | 수동 복구 | 자동 Failover |
| 데이터 유실 | 가능 | 최소화 |
| 다운타임 | 분 단위 | 초 단위 |

---

## 다음 단계

[06-mysql-replication.md](./06-mysql-replication.md) - MySQL 복제 구성
