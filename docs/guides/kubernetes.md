# Kubernetes 배포 가이드

## 사전 요구사항

- kubectl 1.25+
- Kind 또는 Minikube (로컬 개발)
- Docker 20.10+
- 16GB+ RAM 권장

## Kind 클러스터 생성

```bash
# 클러스터 생성
kind create cluster --name portal-universe

# 클러스터 확인
kubectl cluster-info
kubectl get nodes
```

## 배포 순서

### 1. 네임스페이스 생성

```bash
kubectl create namespace portal-universe
kubectl config set-context --current --namespace=portal-universe
```

### 2. 인프라 배포

```bash
# 데이터베이스, 메시지 큐, 모니터링
kubectl apply -k k8s/infrastructure/
```

### 3. 서비스 배포

```bash
# 모든 마이크로서비스
kubectl apply -k k8s/services/

# 배포 상태 확인
kubectl get deployments
kubectl get pods
kubectl get svc
```

## 서비스 접근

### 포트 포워딩

```bash
# API Gateway
kubectl port-forward svc/api-gateway 8080:8080

# Grafana
kubectl port-forward svc/grafana 3000:3000

# Prometheus
kubectl port-forward svc/prometheus 9090:9090
```

### Ingress 설정 (선택)

```bash
kubectl apply -f k8s/base/ingress.yaml
```

## Kubernetes 매니페스트 구조

```
k8s/
├── base/                    # 공통 기본 설정
│   ├── namespace.yaml
│   └── ingress.yaml
├── infrastructure/          # 인프라 서비스
│   ├── mysql/
│   ├── mongodb/
│   ├── kafka/
│   ├── redis/
│   └── monitoring/
├── services/                # 마이크로서비스
│   ├── api-gateway/
│   ├── auth-service/
│   ├── blog-service/
│   ├── shopping-service/
│   └── notification-service/
└── scripts/                 # 자동화 스크립트
    ├── deploy-all.sh
    └── delete-all.sh
```

## 주요 리소스

| 리소스 | 용도 |
|--------|------|
| Deployment | 스테이트리스 서비스 |
| StatefulSet | 데이터베이스 (MySQL, MongoDB) |
| ConfigMap | 설정 파일 |
| Secret | 자격증명, 키 |
| Service | 서비스 노출 |
| PersistentVolumeClaim | 데이터 영속성 |

## 스케일링

```bash
# 수동 스케일링
kubectl scale deployment auth-service --replicas=3

# HPA 설정 (선택)
kubectl apply -f k8s/services/auth-service/hpa.yaml
```

## 로그 및 모니터링

```bash
# Pod 로그 확인
kubectl logs -f deployment/auth-service

# 리소스 사용량
kubectl top pods
kubectl top nodes
```

## 트러블슈팅

### Pod 상태 확인

```bash
kubectl describe pod <pod-name>
kubectl logs <pod-name> --previous
```

### 이벤트 확인

```bash
kubectl get events --sort-by='.lastTimestamp'
```

### 재시작

```bash
kubectl rollout restart deployment/auth-service
```

## 클러스터 삭제

```bash
# Kind 클러스터 삭제
kind delete cluster --name portal-universe
```

## 참고

- [Kubernetes 공식 문서](https://kubernetes.io/docs/)
- [Kind 문서](https://kind.sigs.k8s.io/)
- [프로젝트 README](../../README.md)
