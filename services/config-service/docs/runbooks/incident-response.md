---
id: runbook-config-incident-response
title: Config Service 장애 대응 절차
type: runbook
status: current
created: 2026-01-18
updated: 2026-01-18
author: Documenter Agent
tags: [incident-response, config-service, runbook, troubleshooting]
---

# Config Service 장애 대응 Runbook

> Config Service 장애 발생 시 신속한 진단 및 복구 절차

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **대상 서비스** | Config Service (포트 8888) |
| **예상 소요 시간** | 10-30분 (장애 유형에 따라 다름) |
| **필요 권한** | 서비스 재시작, 로그 확인, 모니터링 접근 권한 |

---

## ✅ 사전 조건

### 필수 권한
- [ ] 서비스 재시작 권한
- [ ] 로그 파일 접근 권한
- [ ] 모니터링 대시보드 접근 권한 (Grafana)

### 필수 도구
- [ ] kubectl (K8s 환경)
- [ ] docker-compose (Docker 환경)
- [ ] curl/httpie (API 테스트)

### 사전 확인
- [ ] 알림이 발생한 시간 확인
- [ ] 영향 범위 파악 (어떤 서비스가 영향 받는지)

---

## 🚨 심각도 판단

### P0 (Critical) - 즉시 대응
- Config Service 완전 다운 (모든 클라이언트 서비스 영향)
- Git 저장소 완전 접근 불가

### P1 (High) - 30분 내 대응
- Config Service 응답 지연 (5초 이상)
- 일부 설정 파일 로드 실패

### P2 (Medium) - 1시간 내 대응
- 간헐적 타임아웃
- 캐시 관련 이슈

---

## 🔄 장애 대응 절차

### Phase 1: 신속 진단 (5분 이내)

#### Step 1: 서비스 상태 확인

**설명**: Config Service가 실행 중인지 확인합니다.

```bash
# 로컬/Docker
curl http://localhost:8888/actuator/health

# Kubernetes
kubectl get pods -n portal-universe -l app=config-service
```

**예상 결과 (정상)**:
```json
{
  "status": "UP"
}
```

**비정상 증상**:
- 응답 없음 (서비스 다운)
- "status": "DOWN"
- HTTP 500 에러

---

#### Step 2: 로그 확인

**설명**: 최근 에러 로그를 확인합니다.

```bash
# 로컬
tail -f services/config-service/logs/application.log

# Docker
docker-compose logs -f --tail=100 config-service

# Kubernetes
kubectl logs -n portal-universe -l app=config-service --tail=100
```

**주요 에러 패턴**:
- `OutOfMemoryError`: 메모리 부족
- `Connection refused`: Git 저장소 연결 실패
- `BindException`: 포트 충돌

---

#### Step 3: 의존성 확인

**설명**: Config Service가 의존하는 외부 시스템 상태를 확인합니다.

```bash
# Git 저장소 접근 확인
git ls-remote https://github.com/L-a-z-e/portal-universe-config-repo.git

# 네트워크 연결 확인
ping github.com
```

---

### Phase 2: 긴급 복구 (10분 이내)

#### Step 4: 서비스 재시작

**설명**: 간단한 재시작으로 해결 시도합니다.

```bash
# 로컬
# Ctrl+C로 종료 후 재시작
SPRING_PROFILES_ACTIVE=local ./gradlew :services:config-service:bootRun

# Docker
docker-compose restart config-service

# Kubernetes
kubectl rollout restart deployment/config-service -n portal-universe
```

**확인 방법**:
```bash
curl http://localhost:8888/actuator/health
```

---

#### Step 5: 캐시 초기화

**설명**: Git 캐시 문제가 의심되면 캐시를 초기화합니다.

```bash
# 로컬
rm -rf ~/.config-repo

# Kubernetes - Pod 재생성으로 캐시 초기화
kubectl delete pod -n portal-universe -l app=config-service
```

---

### Phase 3: 근본 원인 분석 (30분 이내)

#### Step 6: 메트릭 확인

**설명**: Grafana에서 이상 징후를 확인합니다.

```bash
# Grafana 접속
# http://localhost:3000 (admin/password)
# Dashboard: Config Service Metrics
```

**확인 항목**:
- CPU/Memory 사용률
- HTTP 요청 응답 시간
- Git fetch 실패 횟수
- JVM Heap 사용량

---

#### Step 7: Git 저장소 동기화 확인

**설명**: Git 저장소와의 동기화 상태를 확인합니다.

```bash
# 수동으로 Git 저장소 클론 테스트
git clone https://github.com/L-a-z-e/portal-universe-config-repo.git /tmp/test-repo

# Config 서버 로그에서 "Located environment" 확인
kubectl logs -n portal-universe -l app=config-service | grep "Located environment"
```

---

#### Step 8: 클라이언트 서비스 영향 확인

**설명**: Config 서버 장애로 영향 받은 클라이언트 서비스를 확인합니다.

```bash
# Kubernetes
kubectl get pods -n portal-universe

# 각 서비스 로그에서 Config 연결 실패 메시지 확인
kubectl logs -n portal-universe -l app=auth-service --tail=50 | grep "Config"
```

**클라이언트 영향**:
- `fail-fast=true`: 서비스 시작 실패 (CrashLoopBackOff)
- `fail-fast=false`: 기본 설정으로 시작 (기능 제한적)

---

## ⚠️ 장애 유형별 대응

### 장애 1: Config Service 완전 다운 (P0)

**증상**:
- `/actuator/health` 응답 없음
- Pod가 CrashLoopBackOff 상태

**긴급 복구**:
```bash
# 1. 이전 버전으로 롤백
kubectl rollout undo deployment/config-service -n portal-universe

# 2. 확인
kubectl get pods -n portal-universe -l app=config-service
curl http://localhost:8888/actuator/health
```

**근본 원인 분석**:
```bash
# 장애 발생 시점 로그 확인
kubectl logs -n portal-universe -l app=config-service --previous

# 일반적 원인:
# - OOM (메모리 부족)
# - Git 저장소 접근 실패
# - 잘못된 설정 변경
```

👉 상세 절차: [Troubleshooting: Service Down](../troubleshooting/2026/01/TS-20260118-002-service-down.md)

---

### 장애 2: Git 저장소 연결 실패 (P0)

**증상**:
```
Cannot clone or checkout repository
```

**긴급 복구**:
```bash
# 1. Git 저장소 접근 확인
git ls-remote https://github.com/L-a-z-e/portal-universe-config-repo.git

# 2. 네트워크 문제라면 임시로 로컬 파일 사용
# application.yml 변경:
# spring.cloud.config.server.git.uri=file:///path/to/local/config

# 3. 서비스 재시작
kubectl rollout restart deployment/config-service -n portal-universe
```

**근본 원인 분석**:
- GitHub 장애 확인: https://www.githubstatus.com/
- 방화벽/Proxy 설정 확인
- Git credentials 만료 확인

---

### 장애 3: 응답 지연 (P1)

**증상**:
- `/actuator/health` 응답 시간 > 5초
- 클라이언트 서비스 시작 지연

**긴급 복구**:
```bash
# 1. JVM Heap 메모리 확인
kubectl top pods -n portal-universe -l app=config-service

# 2. 메모리 부족 시 Pod 재시작
kubectl delete pod -n portal-universe -l app=config-service

# 3. 리소스 제한 증가 (필요 시)
# deployment.yaml 수정:
# resources:
#   limits:
#     memory: "1Gi"
#   requests:
#     memory: "512Mi"
```

---

### 장애 4: 설정 파일 로드 실패 (P1)

**증상**:
- 특정 프로필 설정 로드 실패
- `application-{profile}.yml` not found

**긴급 복구**:
```bash
# 1. Git 저장소에서 파일 존재 확인
git clone https://github.com/L-a-z-e/portal-universe-config-repo.git
ls -la auth-service/application-docker.yml

# 2. 파일 없으면 Git 저장소에 추가
# 3. Config 서버 캐시 갱신
curl -X POST http://localhost:8888/actuator/refresh
```

---

## ✅ 복구 완료 확인

- [ ] Config Service가 정상 응답 (`/actuator/health` 200 OK)
- [ ] Git 저장소 동기화 정상 (로그에서 "Located environment" 확인)
- [ ] 클라이언트 서비스 정상 시작 (auth-service, blog-service 등)
- [ ] 모니터링 메트릭 정상화 (CPU, Memory, 응답 시간)
- [ ] 알림 해소

---

## 📝 장애 보고서 작성

### 필수 항목

```markdown
# Config Service 장애 보고서

## 장애 개요
- 발생 시간: YYYY-MM-DD HH:MM
- 종료 시간: YYYY-MM-DD HH:MM
- 영향 범위: [영향 받은 서비스 목록]
- 심각도: P0 / P1 / P2

## 장애 원인
[근본 원인 설명]

## 대응 조치
[수행한 조치 목록]

## 재발 방지 대책
[향후 예방 조치]

## 교훈
[배운 점]
```

**저장 위치**: `docs/troubleshooting/2026/01/TS-20260118-XXX-[title].md`

---

## 🔙 롤백 방법

```bash
# Kubernetes - 이전 안정 버전으로 롤백
kubectl rollout undo deployment/config-service -n portal-universe

# 특정 버전으로 롤백
kubectl rollout history deployment/config-service -n portal-universe
kubectl rollout undo deployment/config-service -n portal-universe --to-revision=3
```

---

## 📞 에스컬레이션

| 상황 | 담당자 | 연락처 | 대응 시간 |
|------|--------|--------|-----------|
| P0 장애 (서비스 다운) | DevOps On-Call | [Slack: #oncall] | 즉시 |
| Git 저장소 접근 문제 | Infrastructure 팀 | infra@example.com | 30분 |
| 메모리/리소스 이슈 | Platform 팀 | platform@example.com | 1시간 |
| 보안 관련 문제 | Security 팀 | security@example.com | 즉시 |

---

## 🔗 관련 문서

- [Deployment Runbook](deployment.md)
- [Config Refresh Runbook](config-refresh.md)
- [Architecture Overview](../architecture/ARCH-001-overview.md)
- [Troubleshooting Guide](../troubleshooting/README.md)

---

**최종 업데이트**: 2026-01-18
