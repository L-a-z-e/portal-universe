---
id: runbook-config-deployment
title: Config Service 배포 절차
type: runbook
status: current
created: 2026-01-18
updated: 2026-01-18
author: Documenter Agent
tags: [deployment, config-service, runbook]
---

# Config Service 배포 Runbook

> Config Service를 로컬, Docker, Kubernetes 환경에 배포하는 표준 절차

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **대상 서비스** | Config Service (포트 8888) |
| **예상 소요 시간** | 5-10분 |
| **필요 권한** | Git clone, Docker 실행, Gradle 실행 권한 |

---

## ✅ 사전 조건

### 필수 권한
- [ ] Git 저장소 접근 권한
- [ ] Docker 실행 권한 (Docker 배포 시)
- [ ] Kubernetes 클러스터 접근 권한 (K8s 배포 시)

### 필수 도구
- [ ] Java 17 이상
- [ ] Gradle 8.x 이상 (또는 ./gradlew 사용)
- [ ] Docker (Docker 배포 시)
- [ ] kubectl (K8s 배포 시)

### 사전 확인
- [ ] Config 저장소 접근 가능 여부 확인 (https://github.com/L-a-z-e/portal-universe-config-repo.git)
- [ ] 포트 8888 사용 가능 여부 확인
- [ ] 의존 서비스 상태 확인 (없음 - Config Service는 독립 실행)

---

## 🔄 절차

### 환경별 배포 절차

#### A. 로컬 환경 배포

#### Step 1: 저장소 클론 (최초 1회)

**설명**: 프로젝트 저장소를 로컬에 클론합니다.

```bash
git clone https://github.com/L-a-z-e/portal-universe-docs.git
cd portal-universe-docs
```

---

#### Step 2: Gradle 빌드

**설명**: Config Service를 빌드합니다.

```bash
./gradlew :services:config-service:build
```

**예상 결과**:
```
BUILD SUCCESSFUL in 15s
10 actionable tasks: 10 executed
```

**확인 방법**:
```bash
ls -la services/config-service/build/libs/
```

---

#### Step 3: 서비스 실행

**설명**: local 프로필로 Config Service를 실행합니다.

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew :services:config-service:bootRun
```

**예상 결과**:
```
Started ConfigServiceApplication in 8.5 seconds
Tomcat started on port(s): 8888 (http)
```

**확인 방법**:
```bash
curl http://localhost:8888/actuator/health
```

**예상 응답**:
```json
{
  "status": "UP"
}
```

---

#### B. Docker Compose 배포

#### Step 1: Docker 이미지 빌드

**설명**: Spring Boot Buildpacks를 사용하여 Docker 이미지를 생성합니다.

```bash
./gradlew :services:config-service:bootBuildImage
```

**예상 결과**:
```
Successfully built image 'docker.io/library/config-service:0.0.1-SNAPSHOT'
```

**확인 방법**:
```bash
docker images | grep config-service
```

---

#### Step 2: Docker Compose로 실행

**설명**: docker 프로필로 Config Service를 실행합니다.

```bash
docker-compose up -d config-service
```

**예상 결과**:
```
Creating config-service ... done
```

**확인 방법**:
```bash
docker-compose ps config-service
docker-compose logs -f config-service
```

---

#### Step 3: 헬스체크

**설명**: 서비스가 정상적으로 시작되었는지 확인합니다.

```bash
curl http://localhost:8888/actuator/health
```

**예상 결과**:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

---

#### C. Kubernetes 배포

#### Step 1: 네임스페이스 확인

**설명**: config-service가 배포될 네임스페이스를 확인합니다.

```bash
kubectl get namespace portal-universe
```

**없으면 생성**:
```bash
kubectl create namespace portal-universe
```

---

#### Step 2: ConfigMap/Secret 적용

**설명**: Config Service에 필요한 환경 변수를 설정합니다.

```bash
kubectl apply -f k8s/config-service/configmap.yaml -n portal-universe
kubectl apply -f k8s/config-service/secret.yaml -n portal-universe
```

**확인 방법**:
```bash
kubectl get configmap -n portal-universe | grep config-service
kubectl get secret -n portal-universe | grep config-service
```

---

#### Step 3: Deployment 적용

**설명**: Config Service를 Kubernetes에 배포합니다.

```bash
kubectl apply -f k8s/config-service/deployment.yaml -n portal-universe
kubectl apply -f k8s/config-service/service.yaml -n portal-universe
```

**예상 결과**:
```
deployment.apps/config-service created
service/config-service created
```

**확인 방법**:
```bash
kubectl get pods -n portal-universe -l app=config-service
kubectl get svc -n portal-universe config-service
```

---

#### Step 4: Pod 상태 확인

**설명**: Pod가 Running 상태인지 확인합니다.

```bash
kubectl get pods -n portal-universe -l app=config-service -w
```

**예상 결과**:
```
NAME                              READY   STATUS    RESTARTS   AGE
config-service-7d9c8f6b5d-abcde   1/1     Running   0          2m
```

---

#### Step 5: 헬스체크

**설명**: Port-forward를 통해 헬스체크를 수행합니다.

```bash
kubectl port-forward -n portal-universe svc/config-service 8888:8888
```

**다른 터미널에서 확인**:
```bash
curl http://localhost:8888/actuator/health
```

---

## ✅ 완료 확인

- [ ] Config Service가 포트 8888에서 실행 중
- [ ] `/actuator/health` 엔드포인트 응답 확인
- [ ] Git 저장소 연결 확인 (로그에서 "Located environment" 메시지 확인)
- [ ] 클라이언트 서비스가 Config 서버에 연결 가능 (예: auth-service 시작 테스트)

---

## ⚠️ 문제 발생 시

### 문제 1: 포트 8888이 이미 사용 중

**증상**:
```
Port 8888 was already in use
```

**해결 방법**:
```bash
# 사용 중인 프로세스 확인
lsof -i :8888

# 프로세스 종료
kill -9 [PID]

# 또는 다른 포트로 실행
SERVER_PORT=8889 ./gradlew :services:config-service:bootRun
```

---

### 문제 2: Git 저장소 접근 실패

**증상**:
```
Could not clone or checkout repository
```

**해결 방법**:
```bash
# Git 저장소 접근 권한 확인
git clone https://github.com/L-a-z-e/portal-universe-config-repo.git

# SSH 키 문제라면 application.yml에서 HTTPS URL로 변경
# 또는 Personal Access Token 사용
```

👉 상세 절차: [Troubleshooting: Git Repository Connection Issues](../troubleshooting/2026/01/TS-20260118-001-git-connection.md)

---

### 문제 3: Docker 이미지 빌드 실패

**증상**:
```
Docker image build failed
```

**해결 방법**:
```bash
# Docker 데몬 실행 확인
docker ps

# Gradle 캐시 정리 후 재시도
./gradlew clean
./gradlew :services:config-service:bootBuildImage
```

---

### 문제 4: Kubernetes Pod CrashLoopBackOff

**증상**:
```bash
kubectl get pods -n portal-universe
# config-service-xxx   0/1   CrashLoopBackOff
```

**해결 방법**:
```bash
# 로그 확인
kubectl logs -n portal-universe -l app=config-service --tail=100

# 일반적 원인:
# 1. ConfigMap/Secret 누락 → Step 2 재실행
# 2. 리소스 부족 → kubectl describe pod로 확인
# 3. Git 저장소 접근 실패 → Secret에 Git credentials 확인
```

---

## 🔙 롤백 방법

### 로컬/Docker
```bash
# 실행 중인 서비스 종료
# 로컬
Ctrl+C

# Docker
docker-compose stop config-service
docker-compose down config-service
```

### Kubernetes
```bash
# 이전 버전으로 롤백
kubectl rollout undo deployment/config-service -n portal-universe

# 특정 revision으로 롤백
kubectl rollout history deployment/config-service -n portal-universe
kubectl rollout undo deployment/config-service -n portal-universe --to-revision=2
```

---

## 📞 에스컬레이션

| 상황 | 담당자 | 연락처 |
|------|--------|--------|
| 배포 실패 | DevOps 팀 | devops@example.com |
| Git 저장소 접근 문제 | Infrastructure 팀 | infra@example.com |
| Kubernetes 클러스터 문제 | Platform 팀 | platform@example.com |

---

## 🔗 관련 문서

- [Config Service Architecture](../architecture/ARCH-001-overview.md)
- [Config Refresh Runbook](config-refresh.md)
- [Incident Response Runbook](incident-response.md)

---

**최종 업데이트**: 2026-01-18
