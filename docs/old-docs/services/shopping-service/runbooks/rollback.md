---
id: runbook-shopping-rollback
title: Shopping Service 롤백 절차
type: runbook
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [runbook, shopping-service, rollback, disaster-recovery]
related:
  - runbook-shopping-deployment
---

# Shopping Service 롤백 Runbook

> Shopping Service를 이전 버전으로 복원하는 절차

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **대상 서비스** | shopping-service |
| **서비스 포트** | 8083 |
| **예상 소요 시간** | 5-10분 (환경에 따라 상이) |
| **필요 권한** | Docker 실행 권한, Kubernetes cluster 접근 권한 |

---

## 🚨 롤백 결정 기준

다음과 같은 상황에서 즉시 롤백을 고려해야 합니다:

| 심각도 | 증상 | 조치 |
|--------|------|------|
| 🔴 Critical | 서비스 완전 중단, 5xx 에러율 > 50% | 즉시 롤백 |
| 🟠 High | 주요 API 장애, 응답 시간 > 5초 | 10분 내 롤백 결정 |
| 🟡 Medium | 부분 기능 장애, 에러율 > 10% | 문제 해결 시도 후 30분 내 롤백 결정 |
| 🟢 Low | 경미한 버그, 에러율 < 5% | 핫픽스 또는 다음 배포에서 수정 |

---

## ✅ 사전 조건

### 필수 권한
- [ ] Docker 실행 권한
- [ ] Kubernetes cluster 접근 권한 (K8s 환경)
- [ ] 데이터베이스 접근 권한 (필요시)

### 필수 도구
- [ ] Docker (Docker Compose 환경)
- [ ] kubectl (Kubernetes 환경)
- [ ] curl (헬스체크용)

### 사전 확인
- [ ] 롤백할 이전 버전 확인
- [ ] 롤백 사유 문서화 (Incident Report)
- [ ] 데이터베이스 마이그레이션 확인 (스키마 변경이 있었는지)
- [ ] 관련 팀에 롤백 결정 공유

---

## 🔄 롤백 절차

### 환경별 롤백 선택

- **로컬 개발 환경**: [Section A](#section-a-로컬-개발-환경-롤백) 참조
- **Docker Compose 환경**: [Section B](#section-b-docker-compose-롤백) 참조
- **Kubernetes 환경**: [Section C](#section-c-kubernetes-롤백) 참조

---

## Section A: 로컬 개발 환경 롤백

### Step 1: 이전 버전 체크아웃

**설명**: Git에서 이전 버전으로 체크아웃합니다.

```bash
cd /path/to/portal-universe

# 이전 커밋 해시 확인
git log --oneline -n 10

# 이전 버전으로 체크아웃
git checkout <previous-commit-hash>
```

**예상 결과**:
```
HEAD is now at abc1234 Previous working version
```

---

### Step 2: 서비스 중지

**설명**: 현재 실행 중인 서비스를 중지합니다.

```bash
# bootRun으로 실행 중인 경우 Ctrl+C로 종료

# 또는 프로세스 찾아서 종료
lsof -i :8083
kill -9 <PID>
```

---

### Step 3: 이전 버전 빌드 및 실행

**설명**: 이전 버전을 빌드하고 실행합니다.

```bash
# 빌드
./gradlew :services:shopping-service:build

# 실행
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

## Section B: Docker Compose 롤백

### Step 1: 현재 서비스 중지

**설명**: 문제가 있는 shopping-service 컨테이너를 중지합니다.

```bash
docker-compose stop shopping-service
```

**예상 결과**:
```
Stopping shopping-service ... done
```

---

### Step 2: 이전 이미지 확인

**설명**: 로컬에 있는 이전 버전 이미지를 확인합니다.

```bash
docker images | grep shopping-service
```

**예상 결과**:
```
shopping-service   0.0.1-SNAPSHOT   abc123   2 days ago   350MB
shopping-service   previous         def456   1 week ago   348MB
```

---

### Step 3: docker-compose.yml 수정 (옵션 1)

**설명**: docker-compose.yml에서 이미지 버전을 명시적으로 지정합니다.

```bash
vi docker-compose.yml
```

변경:
```yaml
shopping-service:
  image: shopping-service:previous  # 이전 버전 태그 지정
  # build 섹션을 주석 처리하거나 제거
  ports:
    - "8083:8083"
  # ... 나머지 설정
```

---

### Step 4: Git 체크아웃 (옵션 2)

**설명**: Git에서 이전 버전으로 체크아웃하고 다시 빌드합니다.

```bash
# 이전 커밋 확인
git log --oneline -n 10

# 이전 버전으로 체크아웃
git checkout <previous-commit-hash>

# 이전 버전 빌드
./gradlew :services:shopping-service:bootBuildImage
```

---

### Step 5: 컨테이너 재시작

**설명**: 이전 버전으로 shopping-service를 시작합니다.

```bash
# 기존 컨테이너 제거
docker-compose rm -f shopping-service

# 이전 버전으로 시작
docker-compose up -d shopping-service
```

**예상 결과**:
```
Creating shopping-service ... done
```

---

### Step 6: 로그 확인

**설명**: 서비스 시작 로그를 확인합니다.

```bash
docker-compose logs -f shopping-service
```

**예상 결과**:
```
shopping-service    | Started ShoppingServiceApplication in 20.123 seconds
```

**종료**: `Ctrl+C`

---

### Step 7: 헬스체크

**설명**: 서비스가 정상 동작하는지 확인합니다.

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

### Step 8: API 기능 확인

**설명**: 주요 API 엔드포인트가 정상 동작하는지 확인합니다.

```bash
# API Gateway를 통한 확인
curl http://localhost:8080/api/v1/shopping/products

# 직접 확인
curl http://localhost:8083/api/v1/products
```

---

## Section C: Kubernetes 롤백

### Step 1: Rollout History 확인

**설명**: Deployment의 배포 이력을 확인합니다.

```bash
kubectl rollout history deployment/shopping-service -n shopping
```

**예상 결과**:
```
REVISION  CHANGE-CAUSE
1         Initial deployment
2         Update to v1.2.3
3         Update to v1.2.4 (current - problematic)
```

---

### Step 2: 자동 롤백 (권장)

**설명**: Kubernetes의 rollout undo 명령으로 이전 버전으로 롤백합니다.

```bash
# 바로 이전 버전으로 롤백
kubectl rollout undo deployment/shopping-service -n shopping

# 특정 revision으로 롤백 (옵션)
kubectl rollout undo deployment/shopping-service -n shopping --to-revision=2
```

**예상 결과**:
```
deployment.apps/shopping-service rolled back
```

---

### Step 3: 롤아웃 상태 확인

**설명**: 롤백이 정상적으로 진행되는지 확인합니다.

```bash
kubectl rollout status deployment/shopping-service -n shopping
```

**예상 결과**:
```
deployment "shopping-service" successfully rolled out
```

---

### Step 4: Pod 상태 확인

**설명**: 새로운 Pod가 정상적으로 실행되는지 확인합니다.

```bash
kubectl get pods -n shopping -l app=shopping-service
```

**예상 결과**:
```
NAME                               READY   STATUS    RESTARTS   AGE
shopping-service-7d8f9c5b6-old01   1/1     Running   0          2m
shopping-service-7d8f9c5b6-old02   1/1     Running   0          2m
```

모든 Pod가 `Running` 상태이고 `READY`가 `1/1`이어야 합니다.

---

### Step 5: Pod 로그 확인

**설명**: Pod 로그를 확인하여 정상 시작 여부를 검증합니다.

```bash
kubectl logs -n shopping -l app=shopping-service --tail=100
```

**예상 결과**:
```
Started ShoppingServiceApplication in 25.456 seconds
```

에러 로그가 없어야 합니다.

---

### Step 6: Service Endpoints 확인

**설명**: Service가 올바른 Pod에 연결되었는지 확인합니다.

```bash
kubectl get endpoints -n shopping shopping-service
```

**예상 결과**:
```
NAME               ENDPOINTS                         AGE
shopping-service   10.244.1.15:8083,10.244.2.20:8083 5m
```

---

### Step 7: 헬스체크

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

### Step 8: Ingress/Gateway 테스트

**설명**: 실제 사용자 경로로 접근이 가능한지 확인합니다.

```bash
# Ingress를 통한 확인
curl https://your-domain.com/api/v1/shopping/actuator/health

# 또는 API Gateway를 통한 확인
curl http://api-gateway-url/api/v1/shopping/actuator/health
```

---

## 💾 데이터베이스 롤백 (필요시)

### ⚠️ 주의사항

데이터베이스 롤백은 신중하게 결정해야 합니다:
- **데이터 손실 위험**: 롤백 후 최신 데이터 손실 가능
- **스키마 변경**: 마이그레이션이 있었다면 복잡도 증가
- **다운타임**: 일시적인 서비스 중단 필요

---

### Step 1: 데이터베이스 백업 확인

**설명**: 현재 데이터베이스 상태를 백업합니다.

```bash
# Docker Compose 환경
docker-compose exec mysql-db mysqldump -u root -p shopping_db > backup_before_rollback.sql

# Kubernetes 환경
kubectl exec -n shopping mysql-pod-xxx -- mysqldump -u root -p shopping_db > backup_before_rollback.sql
```

---

### Step 2: 이전 스키마로 복원 (스키마 변경이 있었던 경우만)

**설명**: Flyway/Liquibase 롤백 스크립트를 실행하거나 백업에서 복원합니다.

```bash
# 백업에서 복원
mysql -h localhost -P 3307 -u root -p shopping_db < backup_before_deployment.sql
```

---

### Step 3: 데이터베이스 연결 확인

**설명**: 애플리케이션이 데이터베이스에 정상 연결되는지 확인합니다.

```bash
# 로그에서 데이터베이스 연결 확인
docker-compose logs shopping-service | grep -i "database"

# Kubernetes
kubectl logs -n shopping -l app=shopping-service | grep -i "database"
```

---

## ✅ 완료 확인

### 필수 확인 항목
- [ ] 서비스 헬스체크 성공 (UP 상태)
- [ ] 모든 Pod/컨테이너가 Running 상태
- [ ] 로그에 에러 없음
- [ ] 주요 API 엔드포인트 응답 정상
- [ ] 응답 시간 정상 범위 내
- [ ] 에러율 정상 수준 (< 1%)

### 모니터링 확인
- [ ] Prometheus 메트릭 정상
- [ ] Grafana 대시보드 정상
- [ ] 로그 집계 시스템 정상
- [ ] APM 트레이스 정상

### 사용자 영향 확인
- [ ] 실제 사용자 요청 성공률 확인
- [ ] 클라이언트 에러 로그 확인
- [ ] 고객 지원팀에 피드백 요청

---

## 📝 롤백 후 조치

### 1. Incident Report 작성

**필수 포함 항목**:
- 롤백 사유
- 발생한 문제
- 영향 범위
- 롤백 시간
- 담당자

**문서 위치**: `docs/troubleshooting/YYYY/MM/TS-YYYYMMDD-XXX-[title].md`

---

### 2. 원인 분석 (Root Cause Analysis)

다음 질문에 답변:
- 왜 문제가 발생했는가?
- 배포 전에 발견할 수 있었는가?
- 어떻게 재발을 방지할 것인가?

---

### 3. 재배포 계획 수립

- 문제 수정 방안
- 테스트 계획
- 재배포 일정

---

### 4. 관련 팀 공유

다음 팀에 롤백 완료 공지:
- [ ] 개발팀
- [ ] QA팀
- [ ] 운영팀
- [ ] 고객 지원팀
- [ ] 관리자

---

## ⚠️ 문제 발생 시

### 문제 1: 롤백 후에도 서비스 불안정

**증상**: 이전 버전으로 롤백했지만 여전히 에러 발생

**가능한 원인**:
- 데이터베이스 상태가 호환되지 않음
- 의존 서비스의 문제
- 네트워크/인프라 문제

**해결 방법**:

1. 의존 서비스 확인:
```bash
# Config Service
curl http://config-service:8888/actuator/health

# MySQL
docker-compose ps mysql-db

# API Gateway
curl http://api-gateway:8080/actuator/health
```

2. 데이터베이스 상태 확인:
```bash
# 데이터베이스 연결 테스트
docker-compose exec shopping-service nc -zv mysql-db 3306
```

3. 전체 스택 재시작 고려:
```bash
docker-compose restart
```

---

### 문제 2: Kubernetes 롤백이 진행되지 않음

**증상**:
```
error: timed out waiting for the condition
```

**해결 방법**:

1. Pod 상태 상세 확인:
```bash
kubectl describe pods -n shopping -l app=shopping-service
```

2. 이미지 풀 문제 확인:
```bash
kubectl get events -n shopping --sort-by='.lastTimestamp'
```

3. 수동 스케일 조정:
```bash
# 스케일을 0으로
kubectl scale deployment/shopping-service -n shopping --replicas=0

# 다시 스케일 업
kubectl scale deployment/shopping-service -n shopping --replicas=2
```

---

### 문제 3: 데이터베이스 롤백 후 데이터 불일치

**증상**: 애플리케이션과 데이터베이스 스키마 불일치

**해결 방법**:

1. 스키마 버전 확인:
```sql
-- Flyway
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

-- Liquibase
SELECT * FROM DATABASECHANGELOG ORDER BY DATEEXECUTED DESC LIMIT 5;
```

2. 스키마 마이그레이션 재실행:
```bash
./gradlew :services:shopping-service:flywayMigrate
```

3. 필요시 전문가(DBA) 에스컬레이션

---

## 🔙 롤백도 실패한 경우

### 긴급 조치

1. **서비스 중지** (추가 피해 방지):
```bash
# Docker Compose
docker-compose stop shopping-service

# Kubernetes
kubectl scale deployment/shopping-service -n shopping --replicas=0
```

2. **장애 공지**:
   - 상태 페이지 업데이트
   - 사용자 공지
   - 관련 팀 긴급 소집

3. **에스컬레이션**:
   - On-Call Engineer에게 즉시 연락
   - 시니어 엔지니어 지원 요청
   - 필요시 벤더 지원 요청

---

## 📞 에스컬레이션

| 상황 | 담당자 | 연락처 | 대응 시간 |
|------|--------|--------|-----------|
| 롤백 실패 | On-Call Engineer | oncall@example.com | 즉시 |
| 데이터베이스 문제 | DBA | dba@example.com | 5분 이내 |
| 인프라 문제 | Infrastructure Team | infra@example.com | 10분 이내 |
| 전사 장애 | CTO / VP Engineering | emergency@example.com | 즉시 |

---

## 🔗 관련 문서

- [Deployment Runbook](./deployment.md)
- [Troubleshooting Guide](../../../docs/troubleshooting/README.md)
- [Shopping Service Architecture](../architecture/system-overview.md)
- [Database Migration Guide](../guides/database-migration.md)

---

## 💡 교훈 및 개선 사항

### 롤백을 줄이기 위한 방법

1. **철저한 테스트**:
   - 단위 테스트, 통합 테스트, E2E 테스트
   - Staging 환경에서 충분한 검증

2. **점진적 배포**:
   - Blue-Green 배포
   - Canary 배포
   - Feature Flag 활용

3. **자동화된 롤백**:
   - 헬스체크 실패 시 자동 롤백
   - Circuit Breaker 패턴 적용

4. **모니터링 강화**:
   - 실시간 메트릭 모니터링
   - 알림 설정
   - 이상 탐지 자동화

---

**최종 업데이트**: 2026-01-18
