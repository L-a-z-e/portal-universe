# 사전 요구사항

고가용성 학습 로드맵을 시작하기 전에 준비해야 할 환경과 지식입니다.

---

## 필수 환경

### 1. Kubernetes 클러스터

로컬 개발 환경에서는 Kind(Kubernetes in Docker)를 권장합니다.

```bash
# Kind 설치 확인
kind version
# kind v0.20.0+

# 클러스터 확인
kubectl cluster-info
kubectl get nodes
```

Portal Universe 클러스터 설정:
```bash
# Kind 클러스터 생성 (프로젝트 루트에서)
kind create cluster --config=k8s/kind-config.yaml --name=portal-universe

# 네임스페이스 확인
kubectl get ns portal-universe
```

### 2. Docker

```bash
docker --version
# Docker version 24.0.0+

# Docker Compose (선택)
docker compose version
```

### 3. kubectl

```bash
kubectl version --client
# Client Version: v1.28.0+

# 자동완성 설정 (권장)
source <(kubectl completion bash)  # bash
source <(kubectl completion zsh)   # zsh
```

### 4. 부하 테스트 도구 (k6)

```bash
# macOS
brew install k6

# Linux
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# 버전 확인
k6 version
```

### 5. Helm (선택)

일부 고급 설정에서 사용합니다.

```bash
# macOS
brew install helm

# 버전 확인
helm version
```

---

## Portal Universe 환경 구성

### 1. 인프라 배포

```bash
# 네임스페이스 생성
kubectl apply -f k8s/base/namespace.yaml

# ConfigMap 배포
kubectl apply -f k8s/base/configmap.yaml

# Secret 배포
kubectl apply -f k8s/base/secrets.yaml

# 인프라 컴포넌트 배포
kubectl apply -f k8s/infrastructure/

# 서비스 배포
kubectl apply -f k8s/services/
```

### 2. 상태 확인

```bash
# 모든 Pod 확인
kubectl get pods -n portal-universe

# 모든 서비스 확인
kubectl get svc -n portal-universe

# 이벤트 확인
kubectl get events -n portal-universe --sort-by='.lastTimestamp'
```

### 3. 모니터링 스택 배포

```bash
# Prometheus + Grafana 배포
kubectl apply -f monitoring/

# 포트 포워딩
kubectl port-forward -n portal-universe svc/grafana 3000:3000 &
kubectl port-forward -n portal-universe svc/prometheus 9090:9090 &

# 접속 확인
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
```

---

## 필수 지식

### Kubernetes 기초

다음 개념을 이해하고 있어야 합니다:

| 개념 | 설명 | 학습 자료 |
|------|------|----------|
| Pod | 가장 작은 배포 단위 | [K8s 공식 문서](https://kubernetes.io/docs/concepts/workloads/pods/) |
| Deployment | 선언적 Pod 관리 | [Deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/) |
| Service | Pod 네트워킹 | [Service](https://kubernetes.io/docs/concepts/services-networking/service/) |
| ConfigMap/Secret | 설정 관리 | [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/) |
| PV/PVC | 영구 스토리지 | [Persistent Volumes](https://kubernetes.io/docs/concepts/storage/persistent-volumes/) |

### 필수 kubectl 명령어

```bash
# Pod 관리
kubectl get pods -n <namespace>
kubectl describe pod <pod-name> -n <namespace>
kubectl logs <pod-name> -n <namespace> [-f] [--tail=100]
kubectl exec -it <pod-name> -n <namespace> -- /bin/sh

# Deployment 관리
kubectl get deployments -n <namespace>
kubectl scale deployment <name> --replicas=N -n <namespace>
kubectl rollout status deployment/<name> -n <namespace>
kubectl rollout restart deployment/<name> -n <namespace>

# 디버깅
kubectl get events -n <namespace> --sort-by='.lastTimestamp'
kubectl top pods -n <namespace>  # metrics-server 필요
```

### Linux 기초 명령어

```bash
# 프로세스 확인
ps aux | grep <process>
top -p <pid>
htop  # 설치 필요

# 네트워크 확인
netstat -tlnp
ss -tlnp
curl -v <url>

# 리소스 확인
free -h
df -h
du -sh <directory>
```

### HTTP/REST API

```bash
# cURL 사용법
curl -X GET http://localhost:8080/api/health
curl -X POST http://localhost:8080/api/resource \
  -H "Content-Type: application/json" \
  -d '{"key": "value"}'

# HTTP 상태 코드 이해
# 2xx: 성공
# 4xx: 클라이언트 에러
# 5xx: 서버 에러 ← 고가용성에서 중요
```

---

## 추천 학습 자료

### 책

| 제목 | 저자 | 내용 |
|------|------|------|
| Site Reliability Engineering | Google | SRE 바이블 |
| Release It! | Michael Nygard | 프로덕션 안정성 패턴 |
| Designing Data-Intensive Applications | Martin Kleppmann | 분산 시스템 기초 |

### 온라인 자료

- [Google SRE Book](https://sre.google/sre-book/table-of-contents/) (무료)
- [Netflix Chaos Engineering](https://netflix.github.io/chaosmonkey/)
- [AWS Well-Architected - Reliability Pillar](https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/)

---

## 환경 검증 체크리스트

학습을 시작하기 전에 아래 항목을 확인하세요:

- [ ] Kind/Kubernetes 클러스터 실행 중
- [ ] Portal Universe 네임스페이스 존재
- [ ] 모든 인프라 Pod (Kafka, Redis, MySQL) Running 상태
- [ ] 최소 1개 서비스 (api-gateway) Running 상태
- [ ] Prometheus 접근 가능 (localhost:9090)
- [ ] Grafana 접근 가능 (localhost:3000)
- [ ] k6 설치 완료

### 검증 스크립트

```bash
#!/bin/bash
# prereq-check.sh

echo "=== 환경 검증 ==="

# kubectl
echo -n "kubectl: "
kubectl version --client --short 2>/dev/null && echo "OK" || echo "FAIL"

# Docker
echo -n "docker: "
docker version --format '{{.Server.Version}}' 2>/dev/null && echo "OK" || echo "FAIL"

# k6
echo -n "k6: "
k6 version 2>/dev/null && echo "OK" || echo "FAIL"

# Kubernetes cluster
echo -n "K8s cluster: "
kubectl cluster-info 2>/dev/null | head -1 && echo "OK" || echo "FAIL"

# Portal Universe namespace
echo -n "portal-universe ns: "
kubectl get ns portal-universe 2>/dev/null && echo "OK" || echo "FAIL"

# Pods
echo "=== Pod 상태 ==="
kubectl get pods -n portal-universe 2>/dev/null || echo "FAIL"

echo "=== 검증 완료 ==="
```

---

## 문제 해결

### Kind 클러스터가 시작되지 않는 경우

```bash
# Docker 데몬 확인
docker info

# 기존 클러스터 삭제 후 재생성
kind delete cluster --name=portal-universe
kind create cluster --config=k8s/kind-config.yaml --name=portal-universe
```

### Pod가 Pending 상태인 경우

```bash
# 이벤트 확인
kubectl describe pod <pod-name> -n portal-universe

# 일반적인 원인:
# - 리소스 부족: requests/limits 조정
# - PVC 바인딩 실패: StorageClass 확인
# - 이미지 풀 실패: 이미지 이름/태그 확인
```

### 메트릭이 수집되지 않는 경우

```bash
# Prometheus targets 확인
curl localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job: .job, health: .health}'

# 서비스 어노테이션 확인
kubectl get pods -n portal-universe -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.metadata.annotations.prometheus\.io/scrape}{"\n"}{end}'
```

---

다음 단계: [Phase 1: 고가용성 기초 개념](./phase-1-concepts/01-ha-fundamentals.md)
