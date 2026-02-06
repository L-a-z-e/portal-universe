# Kubernetes Kind Cluster

Kind(Kubernetes in Docker)를 사용하여 로컬 Kubernetes 클러스터를 구성합니다.

---

## 1. Kind 개요

### Kind란?

**Kind (Kubernetes in Docker)**는 Docker 컨테이너를 Kubernetes 노드로 사용하여 로컬에서 Kubernetes 클러스터를 실행하는 도구입니다.

### 다른 로컬 K8s 도구와 비교

| 도구 | 특징 | 사용 사례 |
|------|------|----------|
| Kind | Docker 컨테이너 기반, 가볍고 빠름 | CI/CD, 로컬 개발 |
| Minikube | VM 또는 Docker 기반, 다양한 드라이버 | 학습, 단일 노드 테스트 |
| k3s | 경량 K8s, ARM 지원 | Edge, IoT |
| Docker Desktop | 통합 환경, GUI 제공 | macOS/Windows 개발 |

### Kind의 장점

- **빠른 클러스터 생성** (1-2분)
- **멀티 노드 클러스터 지원**
- **리소스 효율적** (VM 없이 Docker만 사용)
- **CI/CD 파이프라인에 적합**

---

## 2. Kind 설치

### macOS

```bash
# Homebrew
brew install kind

# 또는 바이너리 직접 다운로드
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-darwin-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind
```

### Linux

```bash
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind
```

### 설치 확인

```bash
kind version
# kind v0.20.0 go1.20.4 darwin/amd64
```

---

## 3. Portal Universe Kind 클러스터 설정

### kind-config.yaml 분석

```yaml
# k8s/base/kind-config.yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: portal-universe              # 클러스터 이름

nodes:
  # --- Control Plane 노드 (마스터 노드) ---
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            # Ingress Controller 배포용 레이블
            node-labels: "ingress-ready=true"

    # 호스트 ↔ 컨테이너 포트 매핑
    extraPortMappings:
      # HTTP 트래픽
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      # HTTPS 트래픽
      - containerPort: 443
        hostPort: 443
        protocol: TCP

  # --- Worker 노드들 ---
  - role: worker
  - role: worker
```

### 클러스터 구조

```
┌─────────────────────────────────────────────────────────────────┐
│  Host Machine (macOS/Linux)                                      │
│  localhost:80 ─────┐                                             │
│  localhost:443 ────┼────────────────────────────────────────┐   │
│                    │                                         │   │
│  ┌─────────────────▼─────────────────────────────────────────▼──┤
│  │  Docker Container: portal-universe-control-plane              │
│  │  (Control Plane Node)                                         │
│  │  - API Server, etcd, Scheduler, Controller Manager            │
│  │  - Ingress Controller (80, 443)                               │
│  │  Labels: ingress-ready=true                                   │
│  └───────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────────────┤
│  │  Docker Container: portal-universe-worker                     │
│  │  (Worker Node 1)                                              │
│  │  - Application Pods                                           │
│  └───────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────────────┤
│  │  Docker Container: portal-universe-worker2                    │
│  │  (Worker Node 2)                                              │
│  │  - Application Pods                                           │
│  └───────────────────────────────────────────────────────────────┤
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. 클러스터 생성 및 관리

### 클러스터 생성

```bash
# 설정 파일로 클러스터 생성
kind create cluster --config k8s/base/kind-config.yaml

# 생성 확인
kubectl cluster-info --context kind-portal-universe
```

### 클러스터 목록 확인

```bash
kind get clusters
# portal-universe
```

### 노드 확인

```bash
kubectl get nodes
# NAME                            STATUS   ROLES           AGE   VERSION
# portal-universe-control-plane   Ready    control-plane   5m    v1.27.3
# portal-universe-worker          Ready    <none>          4m    v1.27.3
# portal-universe-worker2         Ready    <none>          4m    v1.27.3
```

### 클러스터 삭제

```bash
kind delete cluster --name portal-universe
```

---

## 5. 로컬 이미지 로드

Kind 클러스터는 별도의 Docker 환경을 사용하므로, 로컬에서 빌드한 이미지를 클러스터에 로드해야 합니다.

### 이미지 빌드 및 로드

```bash
# 1. 로컬에서 이미지 빌드
docker build -t portal-universe-auth-service:v1.0.2 \
  -f services/auth-service/Dockerfile .

# 2. Kind 클러스터에 이미지 로드
kind load docker-image portal-universe-auth-service:v1.0.2 \
  --name portal-universe

# 3. 확인
docker exec portal-universe-control-plane \
  crictl images | grep auth-service
```

### 여러 이미지 한 번에 로드

```bash
# 빌드 스크립트 예시
#!/bin/bash
SERVICES="auth-service blog-service shopping-service notification-service api-gateway"
VERSION="v1.0.2"

for SERVICE in $SERVICES; do
  echo "Building $SERVICE..."
  docker build -t portal-universe-$SERVICE:$VERSION \
    -f services/$SERVICE/Dockerfile .

  echo "Loading $SERVICE to Kind..."
  kind load docker-image portal-universe-$SERVICE:$VERSION \
    --name portal-universe
done
```

### Deployment에서 로컬 이미지 사용

```yaml
spec:
  containers:
    - name: auth-service
      image: portal-universe-auth-service:v1.0.2
      imagePullPolicy: Never   # 레지스트리에서 pull하지 않음
```

---

## 6. Portal Universe 배포 순서

### 1단계: Namespace 생성

```bash
kubectl apply -f k8s/base/namespace.yaml
```

### 2단계: Infrastructure 배포

```bash
# ConfigMap & Secret
kubectl apply -f k8s/infrastructure/configmap.yaml
kubectl apply -f k8s/infrastructure/secret.yaml

# Database
kubectl apply -f k8s/infrastructure/mysql-db.yaml
kubectl apply -f k8s/infrastructure/mongodb.yaml
kubectl apply -f k8s/infrastructure/redis.yaml

# Message Queue
kubectl apply -f k8s/infrastructure/kafka.yaml

# Elasticsearch
kubectl apply -f k8s/infrastructure/elasticsearch.yaml
```

### 3단계: Ingress Controller 배포

```bash
kubectl apply -f k8s/infrastructure/ingress-controller.yaml

# 대기 (Controller가 Ready 될 때까지)
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s
```

### 4단계: Application Services 배포

```bash
kubectl apply -f k8s/services/
```

### 5단계: Monitoring 배포

```bash
kubectl apply -f k8s/infrastructure/prometheus.yaml
kubectl apply -f k8s/infrastructure/grafana.yaml
kubectl apply -f k8s/infrastructure/zipkin.yaml
```

### 6단계: Ingress 설정

```bash
kubectl apply -f k8s/infrastructure/ingress.yaml
```

---

## 7. /etc/hosts 설정

로컬에서 `portal-universe` 호스트명으로 접근하려면 hosts 파일을 수정합니다.

```bash
# macOS/Linux
sudo vi /etc/hosts

# 추가:
127.0.0.1 portal-universe
```

### 접근 테스트

```bash
# Frontend
curl http://portal-universe/

# API Gateway
curl http://portal-universe/api/v1/health

# Grafana
open http://portal-universe/grafana

# Prometheus
open http://portal-universe/prometheus

# Zipkin
open http://portal-universe/zipkin
```

---

## 8. 디버깅 및 트러블슈팅

### Pod 상태 확인

```bash
# 모든 Pod 상태
kubectl get pods -n portal-universe -o wide

# Pod 이벤트 및 상세 정보
kubectl describe pod <pod-name> -n portal-universe

# Pod 로그
kubectl logs -f <pod-name> -n portal-universe
```

### Node 리소스 확인

```bash
# 노드 상태
kubectl describe node portal-universe-control-plane

# 리소스 사용량
kubectl top nodes
kubectl top pods -n portal-universe
```

### 네트워크 테스트

```bash
# 임시 Pod로 네트워크 테스트
kubectl run -it --rm debug --image=busybox -n portal-universe -- sh

# 내부에서 테스트
wget -qO- http://auth-service:8081/actuator/health
nslookup mysql-db
```

### Docker 컨테이너 직접 접속

```bash
# Kind 노드(Docker 컨테이너)에 접속
docker exec -it portal-universe-control-plane bash

# 내부에서 crictl로 컨테이너 확인
crictl ps
crictl logs <container-id>
```

### 일반적인 문제 해결

| 문제 | 해결 방법 |
|------|----------|
| ImagePullBackOff | `kind load docker-image` 확인, `imagePullPolicy: Never` 설정 |
| CrashLoopBackOff | `kubectl logs --previous`로 이전 로그 확인 |
| Pending | `kubectl describe pod`로 리소스 부족 확인 |
| Port 충돌 | `lsof -i :80` 등으로 충돌하는 프로세스 확인 |

---

## 9. 클러스터 재시작

### Docker 재시작 후 복구

```bash
# Kind 클러스터는 Docker 컨테이너이므로,
# Docker 재시작 후 자동으로 복구됩니다.

# 상태 확인
docker ps | grep kind

# 클러스터가 실행 중이지 않으면:
docker start portal-universe-control-plane
docker start portal-universe-worker
docker start portal-universe-worker2

# kubectl 컨텍스트 확인
kubectl config get-contexts
kubectl config use-context kind-portal-universe
```

### 전체 재배포

```bash
# 클러스터 삭제 후 재생성
kind delete cluster --name portal-universe
kind create cluster --config k8s/base/kind-config.yaml

# 이미지 다시 로드
# ... (이미지 빌드 및 로드)

# 리소스 재배포
kubectl apply -f k8s/base/
kubectl apply -f k8s/infrastructure/
kubectl apply -f k8s/services/
```

---

## 10. 유용한 Kind 옵션

### 여러 Control Plane (HA 구성)

```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
  - role: control-plane
  - role: control-plane
  - role: worker
  - role: worker
```

### 특정 Kubernetes 버전 사용

```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    image: kindest/node:v1.28.0@sha256:...
```

### 추가 포트 매핑

```yaml
nodes:
  - role: control-plane
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
      - containerPort: 443
        hostPort: 443
      - containerPort: 30080    # NodePort 범위
        hostPort: 30080
      - containerPort: 30443
        hostPort: 30443
```

---

## 11. 관련 문서

- [Kubernetes Fundamentals](./kubernetes-fundamentals.md) - K8s 기초
- [Kubernetes Networking](./kubernetes-networking.md) - 네트워킹 상세
- [Portal Universe Infra Guide](./portal-universe-infra-guide.md) - 전체 인프라 가이드
