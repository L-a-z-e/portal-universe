# Kubernetes HA/Scaling 가이드

**Author**: Laze
**Date**: 2026-02-26
**ADR**: [ADR-048](../adr/ADR-048-k8s-ha-scaling-strategy.md)

## 개요

Kustomize base/overlay 구조를 사용하여 Kind(로컬)과 AWS(프로덕션) 환경에 맞는
HA/Scaling 정책을 적용합니다.

## 디렉토리 구조

```
k8s/
├── base/                           # 앱 서비스 + 공통 리소스 (환경 무관)
│   └── kustomization.yaml
├── overlays/
│   ├── kind/                       # Kind + LocalStack 로컬 환경
│   │   ├── kustomization.yaml
│   │   ├── metrics-server.yaml
│   │   └── patches/
│   │       ├── replicas.yaml
│   │       └── affinity.yaml
│   └── aws/                        # EKS + AWS 관리형 서비스
│       ├── kustomization.yaml
│       ├── ingress-alb.yaml
│       ├── patches/
│       │   ├── replicas.yaml
│       │   ├── affinity.yaml
│       │   ├── resources.yaml
│       │   └── configmap.yaml
│       ├── hpa/
│       └── pdb/
├── infrastructure/                 # DB, Kafka, 모니터링 등 (Kind에서만 사용)
├── services/                       # 앱 Deployment + Service
└── scripts/
    └── deploy-all.sh
```

## Tier 분류

| Tier | 서비스 | 기준 |
|------|--------|------|
| **Critical** | api-gateway, auth-service | 장애 시 전체 시스템 불가 |
| **High** | shopping-service, shopping-seller-service, portal-shell | 핵심 비즈니스 서비스 |
| **Standard** | blog, notification, drive, prism, chatbot, settlement | 개별 기능 서비스 |
| **Frontend** | 6개 frontend | 정적 리소스 서빙 |

## 환경별 배포

### Kind 환경 (로컬 개발)

```bash
# Kind 클러스터 생성 (이미 있으면 스킵)
kind create cluster --config k8s/base/kind-config.yaml --name portal-universe

# 이미지 빌드 및 로드
./k8s/scripts/build-and-load.sh

# Kustomize로 배포
./k8s/scripts/deploy-all.sh --overlay kind
```

Kind overlay에 포함되는 리소스:
- base (앱 서비스 17개 + 공통 리소스)
- infrastructure (PostgreSQL, MySQL, MongoDB, Kafka, Redis, Elasticsearch, LocalStack)
- monitoring (Zipkin, Prometheus, Grafana)
- network-policy, ingress (NGINX)
- metrics-server (`--kubelet-insecure-tls` 플래그)

| 설정 | 값 |
|------|-----|
| Critical replicas | 2 |
| High replicas | 2 |
| Frontend replicas | 2 |
| Standard replicas | 1 (base 기본값) |
| Anti-affinity | Critical/High: preferred |
| HPA | 없음 |
| PDB | 없음 |

### AWS 환경 (프로덕션)

```bash
# EKS 클러스터에 kubeconfig 설정
aws eks update-kubeconfig --name portal-universe-prod --region ap-northeast-2

# Kustomize로 배포
./k8s/scripts/deploy-all.sh --overlay aws
```

AWS overlay에 포함되는 리소스:
- base (앱 서비스 17개 + 공통 리소스)
- HPA (Critical + High Tier)
- PDB (Critical + High Tier)
- ALB Ingress (NGINX 대체)
- patches (replicas, affinity, resources, configmap)

| 설정 | 값 |
|------|-----|
| Critical replicas | 3 |
| High replicas | 2 |
| Frontend replicas | 2 |
| Standard replicas | 1 (base 기본값) |
| Anti-affinity | Critical: required, High/Frontend: preferred |
| HPA | Critical: min3/max10, High: min2/max6 |
| PDB | Critical: minAvailable 2, High: minAvailable 1 |

**인프라 없음**: RDS, MSK, ElastiCache 등 AWS 관리형 서비스를 사용합니다.
엔드포인트는 `patches/configmap.yaml`에서 오버라이드합니다.

### 레거시 모드

```bash
# 기존 kubectl apply -f 방식 (하위 호환)
./k8s/scripts/deploy-all.sh
```

## Kustomize 직접 사용

```bash
# 빌드된 YAML 미리보기
kubectl kustomize k8s/overlays/kind/
kubectl kustomize k8s/overlays/aws/

# 직접 적용
kubectl apply -k k8s/overlays/kind/

# diff 확인
kubectl diff -k k8s/overlays/kind/
```

## Metrics Server (Kind)

Kind 환경에서 HPA 테스트나 `kubectl top` 사용을 위해 Metrics Server가 포함됩니다.

```bash
# Metrics Server 확인
kubectl get deployment metrics-server -n kube-system

# Pod 리소스 사용량 확인
kubectl top pods -n portal-universe

# Node 리소스 사용량 확인
kubectl top nodes
```

> Kind에서는 `--kubelet-insecure-tls` 플래그가 필요합니다 (자체 서명 인증서).

## HPA 확인 (AWS)

```bash
# HPA 상태 확인
kubectl get hpa -n portal-universe

# 상세 조회
kubectl describe hpa api-gateway-hpa -n portal-universe

# 실시간 모니터링
kubectl get hpa -n portal-universe -w
```

## PDB 확인 (AWS)

```bash
# PDB 상태 확인
kubectl get pdb -n portal-universe

# 상세 조회
kubectl describe pdb api-gateway-pdb -n portal-universe
```

## 트러블슈팅

### Metrics Server가 동작하지 않을 때

```bash
# Pod 로그 확인
kubectl logs -n kube-system deployment/metrics-server

# API 서비스 확인
kubectl get apiservice v1beta1.metrics.k8s.io
```

### HPA가 `<unknown>` 메트릭을 표시할 때

- Metrics Server가 배포되어 있는지 확인
- Pod에 `resources.requests`가 설정되어 있는지 확인 (HPA는 requests 대비 사용률을 계산)

### Anti-Affinity로 Pod이 Pending일 때

```bash
# Pending 원인 확인
kubectl describe pod <pod-name> -n portal-universe

# Node 수 확인
kubectl get nodes
```

- `required` anti-affinity: replicas 수 <= Node 수여야 함
- Node 부족 시 `preferred`로 변경하거나 Node 추가

## AWS overlay 커스터마이징

### ConfigMap 엔드포인트 변경

`k8s/overlays/aws/patches/configmap.yaml`의 플레이스홀더를 실제 값으로 교체:

```yaml
data:
  POSTGRES_HOST: "your-rds-endpoint.ap-northeast-2.rds.amazonaws.com"
  KAFKA_BOOTSTRAP_SERVERS: "your-msk-endpoint:9092"
  REDIS_HOST: "your-elasticache-endpoint.cache.amazonaws.com"
```

### ALB 인증서 ARN 변경

`k8s/overlays/aws/ingress-alb.yaml`의 인증서 ARN을 실제 값으로 교체:

```yaml
annotations:
  alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:ap-northeast-2:ACCOUNT_ID:certificate/YOUR_CERT_ID
```

## 관련 문서

- [ADR-048: K8s HA/Scaling 전략](../adr/ADR-048-k8s-ha-scaling-strategy.md)
- [ADR-049: AWS Secrets Manager + SSM](../adr/ADR-049-secrets-manager-ssm-integration.md)
