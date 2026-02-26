# ADR-048: Kubernetes 고가용성 및 스케일링 전략

**Status**: Accepted
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

Kustomize base/overlay 구조를 도입하여 Kind(로컬)과 AWS(프로덕션) 환경을 분리하고,
Tier 기반 HA/Scaling 정책을 환경별로 차등 적용한다.

### Overlay 구조

| Overlay | 환경 | 특징 |
|---------|------|------|
| **kind** | Kind + LocalStack | 인프라 Pod 포함, preferred anti-affinity, Metrics Server |
| **aws** | EKS + AWS 관리형 서비스 | 인프라 Pod 없음, required anti-affinity, HPA, PDB |

### Tier별 정책

| Tier | 서비스 | Kind Replicas | AWS Replicas | Anti-Affinity | HPA | PDB |
|------|--------|--------------|-------------|---------------|-----|-----|
| **Critical** | api-gateway, auth-service | 2 | 3 | kind: preferred / aws: required | aws만 (min3/max10) | aws만 (min 2) |
| **High** | shopping-service, shopping-seller-service, portal-shell | 2 | 2 | kind: preferred / aws: preferred | aws만 (min2/max6) | aws만 (min 1) |
| **Standard** | blog, notification, drive, prism, chatbot, settlement | 1 | 1 | 없음 | 없음 | 없음 |
| **Frontend** | 6개 frontend | 2 | 2 | kind: 없음 / aws: preferred | 없음 | aws만 (min 1) |

**Anti-Affinity 설정 예시 (Critical Tier - AWS):**

```yaml
spec:
  replicas: 3
  template:
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchExpressions:
                  - key: app
                    operator: In
                    values:
                      - api-gateway
              topologyKey: kubernetes.io/hostname
```

> - `required`: 반드시 다른 Node에 배치. Node 부족 시 Pod이 Pending 상태로 대기
> - `preferred`: 가능하면 다른 Node에 배치. Node 부족 시 같은 Node도 허용

### HPA (AWS만)

트래픽 부하에 따라 Pod 수를 자동 조절한다. Critical/High Tier 서비스에 적용한다.

| 서비스 | minReplicas | maxReplicas | CPU Target | Memory Target |
|--------|-------------|-------------|------------|---------------|
| api-gateway | 3 | 10 | 70% | 80% |
| auth-service | 3 | 10 | 70% | 80% |
| shopping-service | 2 | 6 | 75% | 85% |
| shopping-seller-service | 2 | 6 | 75% | 85% |
| portal-shell | 2 | 6 | 75% | 85% |

> `behavior`로 급격한 스케일링을 방지한다:
> - Scale-up: 60초 안정화 후, 60초마다 최대 2개 Pod 추가
> - Scale-down: 300초 안정화 후, 120초마다 최대 1개 Pod 제거

### PDB (AWS만)

Node 유지보수(drain) 시 최소 가용 Pod 수를 보장한다.

| Tier | 서비스 | minAvailable |
|------|--------|-------------|
| Critical | api-gateway, auth-service | 2 |
| High | shopping-service, portal-shell | 1 |

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① 현재 유지 (replica 1, 스케일링 없음) | 리소스 절약, 설정 단순 | SPOF, 프로덕션 불가 |
| ② 전 서비스 동일 HA (replica 3 + HPA) | 균일한 가용성 | 과도한 리소스 소비, 비용 비효율 |
| ③ Helm Chart 방식 | 풍부한 에코시스템 | 템플릿 복잡도 높음, 학습곡선 |
| ④ **Kustomize overlay + Tier 기반 차등 적용 (선택)** | 비용 효율적, 순수 YAML, 환경별 분리 | Tier 분류 기준 관리 필요 |

## Rationale

- **서비스 중요도가 다르다**: api-gateway 장애는 전체 시스템 마비, blog-service 장애는 블로그만 영향
- **비용 효율성**: 모든 서비스에 replica 3을 적용하면 리소스가 3배 필요하나, 실제 트래픽은 서비스별 편차가 크다
- **환경별 분리**: Kind에서는 인프라 Pod + 최소 HA, AWS에서는 관리형 서비스 + 풀 HA
- **Kustomize 선택**: Helm 대비 학습곡선이 낮고, 기존 순수 YAML을 그대로 활용 가능
- **K8s 네이티브**: 별도 도구 없이 K8s 기본 리소스(HPA, PDB, Affinity)만으로 구현

## Trade-offs

**장점**:
- Critical 서비스(gateway, auth)의 단일 장애점 제거
- 트래픽 급증 시 자동 대응 (HPA)
- Node 장애/유지보수 시 서비스 연속성 보장 (Anti-Affinity + PDB)
- 환경별 독립적인 설정 관리 (kind/aws overlay)

**단점 및 완화**:
- 리소스 소비 증가 → Tier 기반 차등 적용으로 필요한 곳만 강화
- 설정 복잡도 증가 → Kustomize overlay로 구조화, 가이드 문서 제공
- Kind 환경에서 Node 2대로 required anti-affinity 불가 → Kind는 preferred 사용

## Implementation

### 파일 구조

```
k8s/
├── base/
│   ├── kustomization.yaml          # 앱 서비스 17개 + 공통 리소스
│   ├── namespace.yaml
│   ├── secret.yaml
│   ├── jwt-secrets.yaml
│   └── tls-secret.yaml
├── overlays/
│   ├── kind/
│   │   ├── kustomization.yaml      # base + 인프라 + 모니터링 + patches
│   │   ├── metrics-server.yaml     # Kind용 (--kubelet-insecure-tls)
│   │   └── patches/
│   │       ├── replicas.yaml       # Critical: 2, High: 2, Frontend: 2
│   │       └── affinity.yaml       # Critical/High: preferred
│   └── aws/
│       ├── kustomization.yaml      # base + patches + HPA + PDB
│       ├── ingress-alb.yaml        # AWS ALB Ingress
│       ├── patches/
│       │   ├── replicas.yaml       # Critical: 3, High: 2, Frontend: 2
│       │   ├── affinity.yaml       # Critical: required, High/Frontend: preferred
│       │   ├── resources.yaml      # 프로덕션 리소스 상향
│       │   └── configmap.yaml      # RDS/MSK/ElastiCache 엔드포인트
│       ├── hpa/                    # 5개 HPA (Critical + High)
│       └── pdb/                    # 4개 PDB (Critical + High)
├── infrastructure/                 # Kind overlay에서 참조
├── services/                       # base에서 참조
└── scripts/
    └── deploy-all.sh               # --overlay kind|aws 플래그 지원
```

### 배포 명령

```bash
# Kind 환경 (Kustomize)
./k8s/scripts/deploy-all.sh --overlay kind

# AWS 환경 (Kustomize)
./k8s/scripts/deploy-all.sh --overlay aws

# 레거시 (기존 동작 유지)
./k8s/scripts/deploy-all.sh
```

### 검증

```bash
# Kustomize 빌드 확인
kubectl kustomize k8s/overlays/kind/
kubectl kustomize k8s/overlays/aws/

# Metrics Server 확인 (Kind)
kubectl top pods -n portal-universe

# HPA 상태 확인 (AWS)
kubectl get hpa -n portal-universe
```

## References

- [Kubernetes HPA Documentation](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)
- [Pod Anti-Affinity](https://kubernetes.io/docs/concepts/scheduling-eviction/assign-pod-node/#affinity-and-anti-affinity)
- [PodDisruptionBudget](https://kubernetes.io/docs/tasks/run-application/configure-pdb/)
- [Kustomize](https://kustomize.io/)
- ADR-046: MySQL to PostgreSQL Migration (인프라 보안 강화)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-22 | 초안 작성 | Laze |
| 2026-02-26 | Accepted: Kustomize kind/aws overlay 구현, Tier 기반 HA/Scaling 적용 | Laze |
