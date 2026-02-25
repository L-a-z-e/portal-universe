# ADR-048: Kubernetes 고가용성 및 스케일링 전략

**Status**: Proposed
**Date**: 2026-02-22
**Author**: Laze

## Context

현재 Kubernetes 설정은 Kind 기반 로컬 개발 환경에 최적화되어 있다.
보안/네트워크(NetworkPolicy, ClusterIP, Ingress)는 잘 갖추어져 있으나,
고가용성(HA)과 스케일링 관련 설정이 부재하여 프로덕션 전환 시 대응이 필요하다.

### 현재 상태

| 항목 | 현재 설정 | 문제점 |
|------|----------|--------|
| Replicas | 대부분 1개 (portal-shell만 2개) | 단일 장애점(SPOF) |
| Pod Anti-Affinity | 미설정 | 같은 Node에 모든 replica 배치 가능 |
| HPA | 미설정 | 트래픽 증가 시 수동 대응 필요 |
| PodDisruptionBudget | 미설정 | 노드 유지보수 시 서비스 중단 가능 |
| Cluster Autoscaler | 미설정 | Node 리소스 부족 시 수동 서버 추가 필요 |

### 현재 인프라 구성

- Kind Cluster: 1 Control-Plane + 2 Worker Node
- 17개 Deployment (Backend 10 + Frontend 7)
- NGINX Ingress Controller로 외부 트래픽 진입

## Decision

프로덕션 전환 시 적용할 고가용성/스케일링 전략을 3단계(Phase)로 정의한다.
각 Phase는 독립적으로 적용 가능하며, 환경(dev/staging/prod)에 따라 선택적으로 도입한다.

### Phase 1: Replica 및 Anti-Affinity (기본 HA)

서비스를 Tier별로 분류하고, Tier에 따라 replica 수와 Anti-Affinity를 차등 적용한다.

| Tier | 서비스 | Replicas | Anti-Affinity |
|------|--------|----------|---------------|
| **Critical** | api-gateway, auth-service | 3 | 필수 (required) |
| **High** | shopping-service, shopping-seller-service, portal-shell | 2 | 권장 (preferred) |
| **Standard** | blog, notification, prism, chatbot, drive, settlement | 1~2 | 선택 |
| **Frontend** | blog/shopping/prism/admin/drive/seller-frontend | 2 | 권장 (preferred) |

**Anti-Affinity 설정 예시 (Critical Tier):**

```yaml
spec:
  replicas: 3
  template:
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchLabels:
                  app: api-gateway
              topologyKey: "kubernetes.io/hostname"
```

**Anti-Affinity 설정 예시 (High Tier):**

```yaml
spec:
  replicas: 2
  template:
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchLabels:
                    app: shopping-service
                topologyKey: "kubernetes.io/hostname"
```

> - `required`: 반드시 다른 Node에 배치. Node 부족 시 Pod이 Pending 상태로 대기
> - `preferred`: 가능하면 다른 Node에 배치. Node 부족 시 같은 Node도 허용

### Phase 2: HPA (자동 Pod 스케일링)

트래픽 부하에 따라 Pod 수를 자동 조절한다. Critical/High Tier 서비스에 우선 적용한다.

| 서비스 | minReplicas | maxReplicas | CPU Target | Memory Target |
|--------|-------------|-------------|------------|---------------|
| api-gateway | 3 | 10 | 70% | 80% |
| auth-service | 3 | 8 | 70% | 80% |
| shopping-service | 2 | 6 | 75% | 80% |
| shopping-seller-service | 2 | 6 | 75% | 80% |
| portal-shell | 2 | 5 | 80% | - |

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-hpa
  namespace: portal-universe
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Pods
          value: 2
          periodSeconds: 60
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Pods
          value: 1
          periodSeconds: 120
```

> `behavior`로 급격한 스케일링을 방지한다:
> - Scale-up: 60초 안정화 후, 60초마다 최대 2개 Pod 추가
> - Scale-down: 300초 안정화 후, 120초마다 최대 1개 Pod 제거

### Phase 3: PodDisruptionBudget + Cluster Autoscaler

**PodDisruptionBudget (PDB):**

Node 유지보수(drain) 시 최소 가용 Pod 수를 보장한다.

| Tier | minAvailable |
|------|-------------|
| Critical | 2 |
| High | 1 |
| Standard | 0 (PDB 없음) |

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: api-gateway-pdb
  namespace: portal-universe
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: api-gateway
```

**Cluster Autoscaler (클라우드 환경 전용):**

Pod을 스케줄링할 Node가 부족하면 자동으로 서버를 추가한다.

```yaml
# EKS Managed Node Group 예시
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig
metadata:
  name: portal-universe-prod
managedNodeGroups:
  - name: app-nodes
    instanceType: m5.xlarge
    minSize: 3
    maxSize: 15
    desiredCapacity: 5
    labels:
      role: app
  - name: infra-nodes
    instanceType: r5.large
    minSize: 2
    maxSize: 5
    desiredCapacity: 2
    labels:
      role: infrastructure
    taints:
      - key: dedicated
        value: infrastructure
        effect: NoSchedule
```

> - `app-nodes`: 비즈니스 서비스 전용 (3~15대 자동 조절)
> - `infra-nodes`: DB/Kafka/Redis 전용 (Taint로 비즈니스 Pod 진입 차단)

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① 현재 유지 (replica 1, 스케일링 없음) | 리소스 절약, 설정 단순 | SPOF, 프로덕션 불가 |
| ② 전 서비스 동일 HA (replica 3 + HPA) | 균일한 가용성 | 과도한 리소스 소비, 비용 비효율 |
| ③ **Tier 기반 차등 적용 (선택)** | 비용 효율적, 중요 서비스 보호 | Tier 분류 기준 관리 필요 |

## Rationale

- **서비스 중요도가 다르다**: api-gateway 장애는 전체 시스템 마비, blog-service 장애는 블로그만 영향
- **비용 효율성**: 모든 서비스에 replica 3을 적용하면 리소스가 3배 필요하나, 실제 트래픽은 서비스별 편차가 크다
- **점진적 도입**: Phase별 독립 적용으로 dev에서는 Phase 1만, prod에서는 Phase 1~3 전체 적용 가능
- **K8s 네이티브**: 별도 도구 없이 K8s 기본 리소스(HPA, PDB, Affinity)만으로 구현

## Trade-offs

✅ **장점**:
- Critical 서비스(gateway, auth)의 단일 장애점 제거
- 트래픽 급증 시 자동 대응 (HPA)
- Node 장애/유지보수 시 서비스 연속성 보장 (Anti-Affinity + PDB)
- 클라우드 전환 시 비용 최적화 (Cluster Autoscaler)

⚠️ **단점 및 완화**:
- 리소스 소비 증가 → (완화: Tier 기반 차등 적용으로 필요한 곳만 강화)
- 설정 복잡도 증가 → (완화: Phase별 점진 도입, 환경별 Kustomize overlay로 관리)
- Kind 개발 환경에서는 Node 2대로 Anti-Affinity required 적용 불가 → (완화: dev는 preferred 또는 replica 1 유지)

## Implementation

### 파일 구조 (Kustomize overlay 방식)

```
k8s/
├── base/                    # 현재 설정 (개발 기본값)
├── overlays/
│   ├── dev/                 # Phase 1 일부 (replica 1~2, preferred affinity)
│   │   └── kustomization.yaml
│   ├── staging/             # Phase 1~2 (replica 2~3, HPA)
│   │   └── kustomization.yaml
│   └── prod/                # Phase 1~3 (전체 적용)
│       ├── kustomization.yaml
│       ├── hpa/
│       │   ├── api-gateway-hpa.yaml
│       │   └── auth-service-hpa.yaml
│       └── pdb/
│           ├── api-gateway-pdb.yaml
│           └── auth-service-pdb.yaml
└── services/                # 기존 서비스 yaml (base)
```

### 환경별 적용 범위

| Phase | dev (Kind) | staging | prod |
|-------|-----------|---------|------|
| Phase 1: Replica + Affinity | replica 1, affinity 없음 | replica 2, preferred | replica 2~3, required |
| Phase 2: HPA | 없음 | 있음 (보수적) | 있음 (적극적) |
| Phase 3: PDB + Cluster Autoscaler | 없음 | PDB만 | PDB + Cluster Autoscaler |

### 주요 변경 파일

- `k8s/services/api-gateway.yaml` — replicas, affinity 추가
- `k8s/services/auth-service.yaml` — replicas, affinity 추가
- `k8s/overlays/prod/hpa/` — HPA 리소스 (신규)
- `k8s/overlays/prod/pdb/` — PDB 리소스 (신규)
- `k8s/base/kind-config.yaml` — 변경 없음 (dev 전용)

## References

- [Kubernetes HPA Documentation](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)
- [Pod Anti-Affinity](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#affinity-and-anti-affinity)
- [PodDisruptionBudget](https://kubernetes.io/docs/tasks/run-application/configure-pdb/)
- [Cluster Autoscaler](https://github.com/kubernetes/autoscaler/tree/master/cluster-autoscaler)
- ADR-046: MySQL to PostgreSQL Migration (인프라 보안 강화)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-22 | 초안 작성 | Laze |
