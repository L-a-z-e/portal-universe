---
id: runbook-config-refresh
title: Config Service 설정 갱신 절차
type: runbook
status: current
created: 2026-01-18
updated: 2026-01-18
author: Documenter Agent
tags: [config-refresh, config-service, runbook, spring-cloud-bus]
---

# Config Service 설정 갱신 Runbook

> Git 저장소의 설정 변경사항을 재배포 없이 실시간으로 반영하는 절차

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **대상 서비스** | Config Service + 모든 클라이언트 서비스 |
| **예상 소요 시간** | 1-2분 |
| **필요 권한** | Config Server Actuator 접근 권한 |

---

## ✅ 사전 조건

### 필수 권한
- [ ] Config Server `/actuator/refresh` 엔드포인트 접근 권한
- [ ] Git 저장소 push 권한

### 필수 도구
- [ ] curl 또는 httpie
- [ ] Git CLI

### 사전 확인
- [ ] 변경할 설정 파일 위치 확인
- [ ] 영향 받는 서비스 목록 파악
- [ ] 설정 변경이 서비스 재시작 없이 반영 가능한지 확인

---

## 🔍 설정 갱신 방법 비교

| 방법 | 범위 | 소요 시간 | 다운타임 | 사용 시나리오 |
|------|------|-----------|----------|---------------|
| **단일 서비스 Refresh** | 1개 서비스 | 즉시 | 없음 | 테스트, 특정 서비스만 |
| **Spring Cloud Bus** | 모든 서비스 | 즉시 | 없음 | 프로덕션, 전체 갱신 |
| **서비스 재시작** | 1개 서비스 | 30초-1분 | 짧음 | @Value 변경, Bean 재생성 필요 |

---

## 🔄 절차

### 방법 1: 단일 서비스 수동 갱신 (테스트 환경 권장)

#### Step 1: Git 저장소에 설정 변경

**설명**: Config 저장소에서 설정 파일을 수정하고 커밋합니다.

```bash
# Config 저장소 클론
git clone https://github.com/L-a-z-e/portal-universe-config-repo.git
cd portal-universe-config-repo

# 설정 파일 수정 (예: auth-service)
vim auth-service/application-docker.yml

# 예시: 로그 레벨 변경
# logging:
#   level:
#     com.example.auth: DEBUG

# 커밋 및 푸시
git add .
git commit -m "chore(config): change auth-service log level to DEBUG"
git push origin main
```

**확인 방법**:
```bash
# GitHub에서 커밋 확인
curl https://api.github.com/repos/L-a-z-e/portal-universe-config-repo/commits?per_page=1
```

---

#### Step 2: Config Server 캐시 갱신

**설명**: Config Server가 Git 저장소의 최신 변경사항을 가져오도록 합니다.

```bash
# Config Server Refresh
curl -X POST http://localhost:8888/actuator/refresh
```

**예상 결과**:
```json
[
  "config.client.version",
  "logging.level.com.example.auth"
]
```

> 변경된 속성 키 목록이 반환됩니다.

---

#### Step 3: 클라이언트 서비스 Refresh

**설명**: 설정을 다시 로드할 클라이언트 서비스를 갱신합니다.

```bash
# Auth Service Refresh
curl -X POST http://localhost:8081/actuator/refresh

# Blog Service Refresh
curl -X POST http://localhost:8082/actuator/refresh
```

**예상 결과**:
```json
[
  "logging.level.com.example.auth"
]
```

**확인 방법**:
```bash
# 로그 레벨 변경 확인 (예시)
curl http://localhost:8081/actuator/loggers/com.example.auth
```

---

### 방법 2: Spring Cloud Bus로 전체 서비스 갱신 (프로덕션 권장)

#### Step 1: Git 저장소에 설정 변경

**설명**: 방법 1의 Step 1과 동일

```bash
cd portal-universe-config-repo
vim auth-service/application-k8s.yml

# 설정 변경
git add .
git commit -m "chore(config): update auth-service config"
git push origin main
```

---

#### Step 2: Bus-Refresh 트리거

**설명**: 단일 엔드포인트 호출로 모든 클라이언트 서비스를 갱신합니다.

```bash
# Config Server에 Bus-Refresh 요청
curl -X POST http://localhost:8888/actuator/bus-refresh
```

**동작 원리**:
1. Config Server가 Kafka에 RefreshRemoteApplicationEvent 발행
2. 모든 클라이언트 서비스가 Kafka에서 이벤트 수신
3. 각 서비스가 자동으로 설정 다시 로드

**예상 결과**:
```json
{
  "status": "OK"
}
```

---

#### Step 3: 전체 서비스 갱신 확인

**설명**: 모든 클라이언트 서비스가 설정을 갱신했는지 확인합니다.

```bash
# Kubernetes
kubectl logs -n portal-universe -l app=auth-service --tail=20 | grep "Refresh"
kubectl logs -n portal-universe -l app=blog-service --tail=20 | grep "Refresh"

# 예상 로그
# "Received remote refresh request. Keys refreshed: [logging.level.com.example.auth]"
```

**확인 방법**:
```bash
# 각 서비스 actuator 확인
for service in auth-service blog-service shopping-service; do
  echo "=== $service ==="
  kubectl port-forward -n portal-universe svc/$service 8080:8080 &
  sleep 2
  curl http://localhost:8080/actuator/health
  kill %1
done
```

---

### 방법 3: 특정 서비스만 갱신 (Bus-Refresh + Destination)

#### Step 1: 특정 서비스 대상 갱신

**설명**: bus-refresh에 destination 파라미터를 추가하여 특정 서비스만 갱신합니다.

```bash
# Auth Service만 갱신
curl -X POST "http://localhost:8888/actuator/bus-refresh?destination=auth-service:**"

# 특정 인스턴스만 갱신
curl -X POST "http://localhost:8888/actuator/bus-refresh?destination=auth-service:8081"
```

**Destination 패턴**:
- `auth-service:**` - auth-service의 모든 인스턴스
- `auth-service:8081` - 특정 포트의 인스턴스만
- `**:8081` - 모든 서비스의 8081 포트 인스턴스

---

## ⚠️ 주의사항

### 1. @RefreshScope 필수

**설명**: 설정 갱신이 필요한 Bean은 `@RefreshScope`를 추가해야 합니다.

```java
// ❌ 갱신 불가
@Component
public class MyConfig {
    @Value("${my.property}")
    private String property;
}

// ✅ 갱신 가능
@Component
@RefreshScope
public class MyConfig {
    @Value("${my.property}")
    private String property;
}
```

---

### 2. 재시작이 필요한 설정

**다음 설정은 Refresh로 반영 불가, 재배포 필요**:
- 서버 포트 (`server.port`)
- 데이터베이스 URL (`spring.datasource.url`)
- Kafka bootstrap servers
- @ConfigurationProperties의 Bean 구조 변경

---

### 3. 캐시 무효화

**설명**: 캐시를 사용하는 경우 수동으로 캐시를 초기화해야 할 수 있습니다.

```bash
# Redis 캐시 초기화 (예시)
redis-cli FLUSHDB

# 또는 애플리케이션 캐시 초기화 API 호출
curl -X POST http://localhost:8081/api/admin/cache/clear
```

---

## ⚠️ 문제 발생 시

### 문제 1: Bus-Refresh가 동작하지 않음

**증상**:
```
404 Not Found - /actuator/bus-refresh
```

**원인**: Spring Cloud Bus 의존성 누락

**해결 방법**:
```groovy
// build.gradle
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-bus-kafka'
    implementation 'org.springframework.cloud:spring-cloud-starter-config'
}
```

**확인 방법**:
```bash
curl http://localhost:8888/actuator | jq '.["_links"]' | grep bus-refresh
```

---

### 문제 2: 일부 서비스만 갱신됨

**증상**: Bus-Refresh를 호출했지만 일부 서비스의 설정이 갱신되지 않음

**원인**: Kafka 연결 문제 또는 @RefreshScope 누락

**해결 방법**:
```bash
# 1. Kafka 연결 확인
kubectl logs -n portal-universe -l app=auth-service | grep "Kafka"

# 2. 수동으로 개별 서비스 Refresh
curl -X POST http://localhost:8081/actuator/refresh

# 3. @RefreshScope 추가 확인
```

---

### 문제 3: 설정이 갱신되었지만 동작하지 않음

**증상**: Refresh 성공 응답을 받았지만 설정이 반영되지 않음

**원인**:
- @RefreshScope 누락
- Bean이 싱글톤으로 미리 생성됨
- 캐시된 값 사용 중

**해결 방법**:
```bash
# 서비스 재시작 (K8s)
kubectl rollout restart deployment/auth-service -n portal-universe

# 또는 Pod 재생성
kubectl delete pod -n portal-universe -l app=auth-service
```

---

## ✅ 완료 확인

- [ ] Git 저장소에 설정 변경사항 커밋됨
- [ ] Config Server Refresh 성공 (변경된 키 목록 반환)
- [ ] 클라이언트 서비스 Refresh 성공 (로그 확인)
- [ ] 설정 변경사항이 실제로 동작함 (기능 테스트)
- [ ] 모니터링 대시보드에서 이상 없음

---

## 📊 갱신 로그 확인

### Config Server 로그
```bash
# Docker
docker-compose logs -f config-service | grep "Refresh"

# Kubernetes
kubectl logs -n portal-universe -l app=config-service --tail=50 | grep "Refresh"
```

**예상 로그**:
```
Fetching config from server at : https://github.com/L-a-z-e/portal-universe-config-repo
Located environment: name=auth-service, profiles=[docker], label=main
```

---

### 클라이언트 서비스 로그
```bash
kubectl logs -n portal-universe -l app=auth-service --tail=20 | grep "Refresh"
```

**예상 로그**:
```
Received remote refresh request. Keys refreshed: [logging.level.com.example.auth]
RefreshScope refreshed
```

---

## 🔙 롤백 방법

```bash
# Git 저장소에서 이전 커밋으로 되돌리기
cd portal-universe-config-repo
git revert HEAD
git push origin main

# Config Server 및 클라이언트 갱신
curl -X POST http://localhost:8888/actuator/bus-refresh
```

---

## 📞 에스컬레이션

| 상황 | 담당자 | 연락처 |
|------|--------|--------|
| 설정 갱신 실패 | DevOps 팀 | devops@example.com |
| Kafka 연결 문제 | Platform 팀 | platform@example.com |
| 잘못된 설정으로 인한 장애 | On-Call Engineer | [Slack: #oncall] |

---

## 🔗 관련 문서

- [Deployment Runbook](deployment.md)
- [Incident Response Runbook](incident-response.md)
- [Architecture: Config Management](../architecture/ARCH-002-config-management.md)
- [Spring Cloud Bus 공식 문서](https://spring.io/projects/spring-cloud-bus)

---

## 💡 모범 사례

### 1. 설정 변경 전 검증
```bash
# 로컬에서 설정 테스트
./gradlew :services:auth-service:bootRun --args='--spring.profiles.active=docker'
```

### 2. 단계적 갱신 (Canary)
```bash
# 1개 인스턴스만 먼저 갱신
curl -X POST "http://localhost:8888/actuator/bus-refresh?destination=auth-service:8081"

# 모니터링 후 전체 갱신
curl -X POST http://localhost:8888/actuator/bus-refresh
```

### 3. 변경 이력 관리
```bash
# Git 커밋 메시지에 영향 범위 명시
git commit -m "chore(config): update auth-service timeout (affects: auth-service)"
```

---

**최종 업데이트**: 2026-01-18
