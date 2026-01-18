---
id: runbook-shopping-deployment
title: Shopping Service 배포 절차
type: runbook
status: current
created: 2026-01-18
updated: 2026-01-18
author: Claude
tags: [runbook, shopping-service, deployment, docker, kubernetes]
---

# Shopping Service 배포 Runbook

> Shopping Service를 Docker Compose 및 Kubernetes 환경에 배포하는 절차

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **대상 서비스** | shopping-service |
| **서비스 포트** | 8083 |
| **예상 소요 시간** | 10-15분 (환경에 따라 상이) |
| **필요 권한** | Docker 실행 권한, Kubernetes cluster 접근 권한 |

---

## ✅ 사전 조건

### 필수 권한
- [ ] Git 저장소 접근 권한
- [ ] Docker 실행 권한
- [ ] Kubernetes cluster 접근 권한 (K8s 배포 시)
- [ ] Config Server 접근 권한

### 필수 도구
- [ ] Git
- [ ] Docker (20.x 이상)
- [ ] Gradle (8.x)
- [ ] kubectl (K8s 배포 시)
- [ ] curl (헬스체크용)

### 사전 확인
- [ ] 배포할 버전 태그 확인
- [ ] Config Server 정상 동작 확인 (http://localhost:8888/actuator/health)
- [ ] MySQL 데이터베이스 정상 동작 확인
- [ ] API Gateway 정상 동작 확인
- [ ] 데이터베이스 백업 완료 확인

---

## 🔄 배포 절차

### 환경별 배포 선택

- **로컬 개발 환경**: [Section A](#section-a-로컬-개발-환경-배포) 참조
- **Docker Compose 환경**: [Section B](#section-b-docker-compose-배포) 참조
- **Kubernetes 환경**: [Section C](#section-c-kubernetes-배포) 참조

---

## Section A: 로컬 개발 환경 배포

### Step 1: 소스 코드 업데이트

**설명**: 최신 코드를 가져옵니다.

```bash
cd /path/to/portal-universe
git fetch origin
git checkout main
git pull origin main
```

**예상 결과**:
```
Already up to date.
```

---

### Step 2: Gradle 빌드

**설명**: Shopping Service를 빌드합니다.

```bash
./gradlew :services:shopping-service:build
```

**예상 결과**:
```
BUILD SUCCESSFUL in 30s
10 actionable tasks: 10 executed
```

**확인 방법**:
```bash
ls -l services/shopping-service/build/libs/
```

예상 출력:
```
shopping-service-0.0.1-SNAPSHOT.jar
```

---

### Step 3: 서비스 실행

**설명**: Spring Boot 애플리케이션을 실행합니다.

```bash
./gradlew :services:shopping-service:bootRun
```

**예상 결과**:
```
Started ShoppingServiceApplication in 15.234 seconds
```

---

### Step 4: 헬스체크

**설명**: 서비스가 정상적으로 시작되었는지 확인합니다.

```bash
curl http://localhost:8083/actuator/health
```

**예상 결과**:
```json
{
  "status": "UP"
}
```

---

## Section B: Docker Compose 배포

### Step 1: 소스 코드 업데이트

```bash
cd /path/to/portal-universe
git fetch origin
git checkout main
git pull origin main
```

---

### Step 2: Docker 이미지 빌드

**설명**: Gradle bootBuildImage로 Docker 이미지를 생성합니다.

```bash
./gradlew :services:shopping-service:bootBuildImage
```

**예상 결과**:
```
Successfully built image 'docker.io/library/shopping-service:0.0.1-SNAPSHOT'
```

**확인 방법**:
```bash
docker images | grep shopping-service
```

---

### Step 3: 기존 컨테이너 중지 (선택사항)

**설명**: 실행 중인 shopping-service 컨테이너를 중지합니다.

```bash
docker-compose stop shopping-service
docker-compose rm -f shopping-service
```

**예상 결과**:
```
Stopping shopping-service ... done
Going to remove shopping-service
Removing shopping-service ... done
```

---

### Step 4: 서비스 시작

**설명**: Docker Compose로 shopping-service를 시작합니다.

```bash
docker-compose up -d shopping-service
```

**예상 결과**:
```
Creating shopping-service ... done
```

---

### Step 5: 컨테이너 로그 확인

**설명**: 서비스 시작 로그를 확인합니다.

```bash
docker-compose logs -f shopping-service
```

**예상 결과**:
```
shopping-service    | Started ShoppingServiceApplication in 20.123 seconds
```

**종료**: `Ctrl+C`로 로그 확인 종료

---

### Step 6: 헬스체크

**설명**: 서비스가 정상 동작하는지 확인합니다.

```bash
curl http://localhost:8083/actuator/health
```

**예상 결과**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

---

### Step 7: API Gateway를 통한 접근 확인

**설명**: API Gateway를 통해 shopping-service에 접근 가능한지 확인합니다.

```bash
curl http://localhost:8080/api/v1/shopping/actuator/health
```

**예상 결과**:
```json
{
  "status": "UP"
}
```

---

## Section C: Kubernetes 배포

### Step 1: Docker 이미지 빌드 및 푸시

**설명**: Docker 이미지를 빌드하고 레지스트리에 푸시합니다.

```bash
# 이미지 빌드
./gradlew :services:shopping-service:bootBuildImage

# 이미지 태깅
docker tag shopping-service:0.0.1-SNAPSHOT your-registry/shopping-service:v1.2.3

# 레지스트리에 푸시
docker push your-registry/shopping-service:v1.2.3
```

**예상 결과**:
```
v1.2.3: digest: sha256:... size: 2841
```

---

### Step 2: Kubernetes 매니페스트 업데이트

**설명**: Deployment의 이미지 버전을 업데이트합니다.

```bash
# k8s/shopping-service-deployment.yaml 편집
vi k8s/shopping-service-deployment.yaml
```

이미지 태그 변경:
```yaml
spec:
  containers:
  - name: shopping-service
    image: your-registry/shopping-service:v1.2.3  # 버전 업데이트
```

---

### Step 3: ConfigMap/Secret 업데이트 (필요시)

**설명**: 설정 변경이 있는 경우 ConfigMap이나 Secret을 업데이트합니다.

```bash
# ConfigMap 적용
kubectl apply -f k8s/shopping-service-configmap.yaml

# Secret 적용
kubectl apply -f k8s/shopping-service-secret.yaml
```

---

### Step 4: Deployment 적용

**설명**: 업데이트된 매니페스트를 적용합니다.

```bash
kubectl apply -f k8s/shopping-service-deployment.yaml
```

**예상 결과**:
```
deployment.apps/shopping-service configured
```

---

### Step 5: 롤아웃 상태 확인

**설명**: Deployment 롤아웃이 정상적으로 진행되는지 확인합니다.

```bash
kubectl rollout status deployment/shopping-service -n shopping
```

**예상 결과**:
```
deployment "shopping-service" successfully rolled out
```

---

### Step 6: Pod 상태 확인

**설명**: 새로운 Pod가 정상적으로 실행되는지 확인합니다.

```bash
kubectl get pods -n shopping -l app=shopping-service
```

**예상 결과**:
```
NAME                               READY   STATUS    RESTARTS   AGE
shopping-service-7d8f9c5b6-abcd1   1/1     Running   0          2m
shopping-service-7d8f9c5b6-xyz12   1/1     Running   0          2m
```

---

### Step 7: Pod 로그 확인

**설명**: Pod 로그를 확인하여 정상 시작 여부를 검증합니다.

```bash
kubectl logs -n shopping -l app=shopping-service --tail=50
```

**예상 결과**:
```
Started ShoppingServiceApplication in 25.456 seconds
```

---

### Step 8: Service 및 Ingress 확인

**설명**: Service와 Ingress가 정상 동작하는지 확인합니다.

```bash
# Service 확인
kubectl get svc -n shopping

# Ingress 확인 (있는 경우)
kubectl get ingress -n shopping
```

---

### Step 9: 헬스체크 (Kubernetes)

**설명**: 클러스터 내부 또는 외부에서 헬스체크를 수행합니다.

```bash
# Port-forward를 통한 확인
kubectl port-forward -n shopping svc/shopping-service 8083:8083

# 다른 터미널에서
curl http://localhost:8083/actuator/health
```

**예상 결과**:
```json
{
  "status": "UP"
}
```

---

## ✅ 완료 확인

### 공통 확인 항목
- [ ] 서비스 헬스체크 성공 (UP 상태)
- [ ] API Gateway를 통한 접근 가능
- [ ] 데이터베이스 연결 정상
- [ ] 로그에 에러 없음
- [ ] 주요 API 엔드포인트 응답 정상

### Docker Compose 환경
- [ ] `docker-compose ps`에서 shopping-service가 Up 상태
- [ ] 포트 8083 접근 가능

### Kubernetes 환경
- [ ] 모든 Pod가 Running 상태
- [ ] Readiness Probe 통과
- [ ] Liveness Probe 통과
- [ ] Service EndPoints 연결됨

---

## ⚠️ 문제 발생 시

### 문제 1: 서비스가 시작되지 않음

**증상**:
```
Application failed to start
```

**해결 방법**:

1. Config Server 연결 확인:
```bash
curl http://config-service:8888/actuator/health
```

2. 로그 상세 확인:
```bash
# Docker Compose
docker-compose logs --tail=100 shopping-service

# Kubernetes
kubectl logs -n shopping -l app=shopping-service --tail=100
```

3. 환경 변수 확인:
```bash
# Docker Compose
docker-compose config | grep shopping-service -A 20

# Kubernetes
kubectl describe pod -n shopping -l app=shopping-service
```

👉 상세 절차: [TS-20260118-001-service-startup-failure.md](../../../docs/troubleshooting/2026/01/TS-20260118-001-service-startup-failure.md)

---

### 문제 2: 데이터베이스 연결 실패

**증상**:
```
Connection refused: mysql-db:3306
```

**해결 방법**:

1. MySQL 상태 확인:
```bash
# Docker Compose
docker-compose ps mysql-db

# Kubernetes
kubectl get pods -n shopping -l app=mysql
```

2. 네트워크 연결 확인:
```bash
# Docker Compose
docker-compose exec shopping-service ping mysql-db

# Kubernetes
kubectl exec -n shopping -it shopping-service-xxx -- nc -zv mysql 3306
```

👉 상세 절차: [Troubleshooting Guide](../../../docs/troubleshooting/README.md)

---

### 문제 3: API Gateway 라우팅 실패

**증상**:
```bash
curl http://localhost:8080/api/v1/shopping/actuator/health
# 503 Service Unavailable
```

**해결 방법**:

1. API Gateway 로그 확인:
```bash
# Docker Compose
docker-compose logs api-gateway | grep shopping

# Kubernetes
kubectl logs -n gateway -l app=api-gateway | grep shopping
```

2. 서비스 디스커버리 확인:
```bash
# Docker Compose
docker-compose exec api-gateway ping shopping-service

# Kubernetes
kubectl get endpoints -n shopping shopping-service
```

---

## 🔙 롤백 방법

배포 중 문제가 발생하면 즉시 롤백을 수행합니다.

👉 **[rollback.md](./rollback.md) 참조**

**빠른 롤백 명령어**:

```bash
# Docker Compose
docker-compose up -d shopping-service  # 이전 이미지로 자동 복원

# Kubernetes
kubectl rollout undo deployment/shopping-service -n shopping
```

---

## 📞 에스컬레이션

| 상황 | 담당자 | 연락처 | 대응 시간 |
|------|--------|--------|-----------|
| 배포 실패 | DevOps Lead | devops-lead@example.com | 15분 이내 |
| 데이터베이스 문제 | DBA | dba@example.com | 10분 이내 |
| 네트워크 이슈 | Infrastructure Team | infra@example.com | 20분 이내 |
| 긴급 장애 | On-Call Engineer | oncall@example.com | 즉시 |

---

## 🔗 관련 문서

- [Rollback Runbook](./rollback.md)
- [Shopping Service Architecture](../architecture/system-overview.md)
- [API Documentation](../api/README.md)
- [Troubleshooting Guide](../../../docs/troubleshooting/README.md)

---

**최종 업데이트**: 2026-01-18
