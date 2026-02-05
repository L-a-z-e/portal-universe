# 이중화 패턴

시스템 가용성을 높이기 위한 이중화(Redundancy) 패턴을 학습합니다.

---

## 학습 목표

- [ ] Active-Active와 Active-Passive 패턴의 차이를 설명할 수 있다
- [ ] 각 패턴의 장단점과 적용 시나리오를 이해한다
- [ ] Portal Universe에 적합한 이중화 전략을 설계할 수 있다

---

## 1. 이중화의 기본 개념

### 왜 이중화가 필요한가?

**단일 장애점 (SPOF: Single Point of Failure)**

```
[현재 Portal Universe 구조]

Client → API Gateway → Auth Service → MySQL
              │              ↓
              │           Redis
              │
              └── Shopping → Kafka → Notification

모든 컴포넌트가 replicas: 1
→ 어떤 컴포넌트든 다운되면 해당 기능 전체 중단
```

### 이중화의 효과

```
단일 인스턴스: 99.9% 가용성
장애 확률: 0.1%

이중화 (독립 인스턴스 2개):
동시 장애 확률: 0.1% × 0.1% = 0.0001%
가용성: 99.9999% (6 nines)

※ 실제로는 공통 원인 장애(Common Cause Failure)로 인해 이상적인 값보다 낮음
```

---

## 2. Active-Active 패턴

### 개념

모든 인스턴스가 동시에 트래픽을 처리합니다.

```
           ┌─────────────┐
           │ Load        │
           │ Balancer    │
           └──────┬──────┘
                  │
        ┌─────────┼─────────┐
        │         │         │
   ┌────▼────┐ ┌──▼───┐ ┌───▼────┐
   │ Server  │ │Server│ │ Server │
   │   A     │ │  B   │ │   C    │
   └─────────┘ └──────┘ └────────┘
       (Active) (Active) (Active)
```

### 장점

| 장점 | 설명 |
|------|------|
| **높은 처리량** | 모든 인스턴스가 트래픽 처리 |
| **빠른 Failover** | 장애 인스턴스만 제외 |
| **비용 효율** | 모든 리소스 활용 |
| **수평 확장 용이** | 인스턴스 추가로 확장 |

### 단점

| 단점 | 설명 |
|------|------|
| **복잡한 상태 관리** | 세션/캐시 동기화 필요 |
| **데이터 일관성** | 분산 트랜잭션 고려 필요 |
| **비용** | 로드 밸런서 필요 |

### Portal Universe 적용 예시

```yaml
# Stateless 서비스: API Gateway, Auth Service
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
spec:
  replicas: 3  # Active-Active
  template:
    spec:
      containers:
        - name: api-gateway
          # 세션은 Redis에 저장 (외부화)
          # 모든 인스턴스가 동일한 역할
```

```yaml
# Kubernetes Service (Load Balancing)
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
spec:
  type: ClusterIP
  selector:
    app: api-gateway
  # 자동으로 모든 Pod에 트래픽 분산
```

---

## 3. Active-Passive 패턴

### 개념

Primary가 트래픽을 처리하고, Secondary는 대기합니다.

```
           ┌─────────────┐
           │ Load        │
           │ Balancer    │
           └──────┬──────┘
                  │
                  │ (트래픽)
                  │
           ┌──────▼──────┐
           │   Primary   │◄──── 활성
           │   (Master)  │
           └──────┬──────┘
                  │
            (복제/동기화)
                  │
           ┌──────▼──────┐
           │  Secondary  │◄──── 대기
           │  (Standby)  │
           └─────────────┘
```

### 장점

| 장점 | 설명 |
|------|------|
| **단순한 구조** | 상태 동기화가 단방향 |
| **데이터 일관성** | Primary만 쓰기 |
| **적은 충돌** | 동시 쓰기 없음 |

### 단점

| 단점 | 설명 |
|------|------|
| **리소스 낭비** | Standby는 유휴 상태 |
| **Failover 시간** | 승격(Promotion) 시간 필요 |
| **복잡한 Failover** | 자동화 구성 필요 |

### Portal Universe 적용 예시

```yaml
# MySQL Primary-Replica (향후 구현)
# Primary: 읽기/쓰기
# Replica: 읽기 전용, Primary 장애 시 승격

# Redis Sentinel (향후 구현)
# Master: 읽기/쓰기
# Slave: 복제, Master 장애 시 Sentinel이 승격 관리
```

---

## 4. 패턴 비교

| 특성 | Active-Active | Active-Passive |
|------|---------------|----------------|
| **처리량** | 높음 (N배) | 낮음 (1배) |
| **Failover 시간** | 짧음 (밀리초) | 김 (초~분) |
| **리소스 효율** | 높음 | 낮음 |
| **복잡도** | 높음 | 낮음 |
| **데이터 일관성** | 도전적 | 쉬움 |
| **적용 대상** | Stateless 서비스 | Stateful 서비스, DB |

---

## 5. 컴포넌트별 이중화 전략

### API Gateway / Backend Services (Stateless)

**권장**: Active-Active

```yaml
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0  # Zero-downtime 배포
```

**세션 외부화**:
```java
// 세션을 Redis에 저장 (Spring Session)
@EnableRedisHttpSession
public class SessionConfig {
    // 모든 인스턴스가 같은 Redis를 바라봄
}
```

### Kafka

**권장**: Active-Active (Multi-Broker Cluster)

```yaml
# 3-노드 Kafka 클러스터
replicas: 3

# Topic Replication Factor
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: "3"
KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: "3"

# Min ISR (In-Sync Replicas)
KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: "2"
```

**동작 방식**:
- 모든 브로커가 활성 상태
- 각 파티션에 Leader + Followers
- Leader 장애 시 Follower가 자동 승격

### Redis

**옵션 1**: Redis Sentinel (Active-Passive)

```
Master ←─── 복제 ────┬─── Slave 1
                     └─── Slave 2

Sentinel 1, 2, 3 (모니터링 + 자동 Failover)
```

**옵션 2**: Redis Cluster (Active-Active, 데이터 샤딩)

```
Shard 1: Master A + Replica A'
Shard 2: Master B + Replica B'
Shard 3: Master C + Replica C'
```

**Portal Universe 권장**: Sentinel (데이터 규모가 작음)

### MySQL

**권장**: Primary-Replica (Active-Passive for Writes)

```
Primary (Read/Write)
    │
    ├── Replica 1 (Read-Only)
    └── Replica 2 (Read-Only)
```

**읽기 분산**:
```yaml
spring:
  datasource:
    # Primary (쓰기)
    primary:
      url: jdbc:mysql://mysql-primary:3306/portal
    # Replica (읽기)
    replica:
      url: jdbc:mysql://mysql-replica:3306/portal
```

---

## 6. Kubernetes 네이티브 이중화

### ReplicaSet/Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      # Pod Anti-Affinity: 같은 노드에 배치 방지
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchLabels:
                    app: auth-service
                topologyKey: kubernetes.io/hostname
```

### Service (Load Balancing)

```yaml
apiVersion: v1
kind: Service
metadata:
  name: auth-service
spec:
  type: ClusterIP
  selector:
    app: auth-service
  ports:
    - port: 8081
      targetPort: 8081
  # 기본: Round Robin 로드 밸런싱
```

### Pod Disruption Budget (PDB)

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: auth-service-pdb
spec:
  minAvailable: 1  # 최소 1개는 항상 가용
  selector:
    matchLabels:
      app: auth-service
```

---

## 7. 이중화 설계 체크리스트

### 서비스 레벨

- [ ] 모든 서비스 replicas >= 2
- [ ] Pod Anti-Affinity 설정
- [ ] PDB 설정
- [ ] Health Check (Liveness/Readiness) 설정
- [ ] Graceful Shutdown 구현

### 데이터 레벨

- [ ] MySQL: Primary-Replica 구성
- [ ] Redis: Sentinel 또는 Cluster 구성
- [ ] Kafka: 3-broker, replication-factor=3
- [ ] 백업 전략 수립

### 네트워크 레벨

- [ ] 다중 Ingress Controller
- [ ] DNS 이중화
- [ ] 다중 AZ 배포 (가능한 경우)

---

## 핵심 정리

1. **Active-Active**: Stateless 서비스에 적합, 높은 처리량
2. **Active-Passive**: Stateful 서비스/DB에 적합, 데이터 일관성 보장
3. **세션 외부화**: Active-Active를 위한 전제 조건
4. **Kubernetes**가 기본 이중화 메커니즘을 제공합니다
5. **PDB**로 유지보수 중에도 가용성을 보장합니다

---

## 다음 단계

[05-current-state-analysis.md](./05-current-state-analysis.md) - Portal Universe 현재 상태를 분석합니다.

---

## 참고 자료

- [Kubernetes - ReplicaSet](https://kubernetes.io/docs/concepts/workloads/controllers/replicaset/)
- [Redis Sentinel Documentation](https://redis.io/docs/management/sentinel/)
- [Kafka Replication](https://kafka.apache.org/documentation/#replication)
