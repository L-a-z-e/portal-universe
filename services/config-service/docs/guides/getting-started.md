---
id: guide-config-getting-started
title: Config Service Getting Started
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Portal Universe Team
tags: [config-service, setup, environment, spring-cloud-config]
related:
  - guide-config-client-configuration
  - arch-config-service
---

# Config Service Getting Started

> Config Service 개발 환경 설정 및 실행 가이드

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 15분 |
| **대상** | Config Service 개발자, 백엔드 개발자 |
| **서비스 포트** | 8888 |
| **기술 스택** | Spring Boot 3.5.5, Spring Cloud Config 2025.0.0 |

---

## ✅ 사전 요구사항

### 필수 소프트웨어

| 소프트웨어 | 버전 | 확인 명령어 |
|-----------|------|------------|
| Java | 17+ | `java -version` |
| Gradle | 8.x | `gradle --version` |
| Git | 2.x+ | `git --version` |
| Docker (선택) | 20.x+ | `docker --version` |

### 필수 지식
- Spring Boot 기본 개념
- Git 기본 사용법
- YAML 설정 파일 작성

---

## 🔧 환경 설정

### Step 1: 저장소 클론

```bash
cd /path/to/your/workspace
git clone <repository-url>
cd portal-universe
```

### Step 2: Config 저장소 확인

Config Service는 외부 Git 저장소에서 설정을 가져옵니다.

**기본 설정 저장소**:
```
https://github.com/L-a-z-e/portal-universe-config-repo.git
```

**로컬에서 Config 저장소 클론** (선택사항):
```bash
cd /path/to/your/workspace
git clone https://github.com/L-a-z-e/portal-universe-config-repo.git
```

### Step 3: 환경 변수 설정

Config Service는 Git 저장소 접근을 위해 환경 변수가 필요할 수 있습니다.

`.env` 파일 생성 (필요시):
```bash
cd services/config-service
cat > .env << EOF
# Git 저장소 인증 (Private 저장소인 경우)
GIT_USERNAME=your-username
GIT_PASSWORD=your-token

# 또는 SSH 키 사용
GIT_PRIVATE_KEY_PATH=/path/to/private-key
EOF
```

---

## 🚀 실행 방법

### 방법 1: Gradle로 직접 실행 (개발 모드)

```bash
# 프로젝트 루트에서
./gradlew :services:config-service:bootRun
```

**또는 특정 프로필로 실행**:
```bash
./gradlew :services:config-service:bootRun --args='--spring.profiles.active=local'
```

### 방법 2: Docker Compose 사용

```bash
# 프로젝트 루트에서
docker-compose up -d config-service
```

### 방법 3: JAR 파일 빌드 후 실행

```bash
# 빌드
./gradlew :services:config-service:build

# 실행
java -jar services/config-service/build/libs/config-service-*.jar
```

---

## ✅ 실행 확인

### 1. 헬스 체크

```bash
curl http://localhost:8888/actuator/health
```

**예상 결과**:
```json
{
  "status": "UP"
}
```

### 2. 설정 조회 테스트

특정 서비스의 설정을 조회하여 Config Server가 정상 동작하는지 확인합니다.

```bash
# auth-service의 local 프로필 설정 조회
curl http://localhost:8888/auth-service/local
```

**예상 결과**:
```json
{
  "name": "auth-service",
  "profiles": ["local"],
  "label": null,
  "version": "abc123...",
  "state": null,
  "propertySources": [
    {
      "name": "https://github.com/.../auth-service-local.yml",
      "source": {
        "server.port": 8081,
        "spring.datasource.url": "jdbc:mysql://localhost:3306/auth_db",
        ...
      }
    }
  ]
}
```

### 3. 공통 설정 조회

```bash
# 모든 서비스가 공유하는 기본 설정 조회
curl http://localhost:8888/application/default
```

### 4. Actuator 엔드포인트 확인

```bash
curl http://localhost:8888/actuator
```

---

## 📂 Config 저장소 구조

Config Service가 읽어오는 Git 저장소의 구조:

```
config-repo/
├── application.yml              # 모든 서비스 공통 설정
├── application-local.yml        # local 프로필 공통 설정
├── application-docker.yml       # docker 프로필 공통 설정
├── application-k8s.yml          # k8s 프로필 공통 설정
├── auth-service.yml             # auth-service 기본 설정
├── auth-service-local.yml       # auth-service local 프로필
├── auth-service-docker.yml      # auth-service docker 프로필
├── blog-service.yml             # blog-service 기본 설정
├── blog-service-local.yml
├── shopping-service.yml
└── ...
```

---

## 🔍 주요 설정 확인

### Config Service 자체 설정

`services/config-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8888

spring:
  application:
    name: config-service
  cloud:
    config:
      server:
        git:
          uri: https://github.com/L-a-z-e/portal-universe-config-repo.git
          default-label: main
          clone-on-start: true
          force-pull: true
```

### 주요 속성 설명

| 속성 | 설명 |
|------|------|
| `spring.cloud.config.server.git.uri` | Config 저장소 Git URL |
| `spring.cloud.config.server.git.default-label` | 기본 브랜치 (main/master) |
| `spring.cloud.config.server.git.clone-on-start` | 시작 시 저장소 클론 |
| `spring.cloud.config.server.git.force-pull` | 강제 pull 여부 |

---

## ⚠️ 자주 발생하는 문제

### 문제 1: Git 저장소 접근 실패

**증상**:
```
Error: Cannot clone or checkout repository
```

**원인**:
- Private 저장소인 경우 인증 정보 누락
- SSH 키 설정 오류
- 네트워크 연결 문제

**해결 방법**:

**A. HTTPS + Personal Access Token 사용**:
```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/username/config-repo.git
          username: ${GIT_USERNAME}
          password: ${GIT_TOKEN}
```

**B. SSH 키 사용**:
```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: git@github.com:username/config-repo.git
          ignore-local-ssh-settings: false
```

**C. Public 저장소로 테스트**:
```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/spring-cloud-samples/config-repo
```

### 문제 2: 설정 파일을 찾을 수 없음

**증상**:
```bash
curl http://localhost:8888/auth-service/local
# 빈 propertySources 배열 반환
```

**원인**:
- Config 저장소에 `auth-service-local.yml` 파일이 없음
- 브랜치가 잘못 지정됨

**해결 방법**:

```bash
# Config 저장소에 파일이 있는지 확인
cd /path/to/config-repo
ls -l auth-service*

# 없다면 생성
cat > auth-service-local.yml << EOF
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_db
    username: root
    password: password
EOF

# Commit & Push
git add auth-service-local.yml
git commit -m "Add auth-service local config"
git push
```

### 문제 3: 설정 변경이 반영되지 않음

**증상**:
- Config 저장소에서 설정을 변경했지만 서비스에서 이전 값을 계속 사용

**원인**:
- Config Server가 캐시된 설정을 사용
- 클라이언트 서비스가 재시작되지 않음

**해결 방법**:

**A. Config Service 재시작**:
```bash
# Docker Compose 사용 시
docker-compose restart config-service

# Gradle 사용 시 (Ctrl+C로 중지 후)
./gradlew :services:config-service:bootRun
```

**B. Git 강제 pull**:
```bash
curl -X POST http://localhost:8888/actuator/refresh
```

**C. 클라이언트 서비스 재시작**:
```bash
# 예: auth-service 재시작
./gradlew :services:auth-service:bootRun
```

### 문제 4: 포트 8888이 이미 사용 중

**증상**:
```
Port 8888 is already in use
```

**해결 방법**:

```bash
# 포트 사용 중인 프로세스 확인 (macOS/Linux)
lsof -i :8888

# 프로세스 종료
kill -9 <PID>

# 또는 다른 포트로 실행
./gradlew :services:config-service:bootRun --args='--server.port=8889'
```

---

## 🧪 테스트

### 단위 테스트 실행

```bash
./gradlew :services:config-service:test
```

### 통합 테스트 실행

```bash
./gradlew :services:config-service:integrationTest
```

---

## 📝 로그 확인

### 개발 모드 로그 레벨 조정

`application-local.yml`:
```yaml
logging:
  level:
    org.springframework.cloud.config: DEBUG
    org.springframework.web: DEBUG
```

### Docker 로그 확인

```bash
docker-compose logs -f config-service
```

---

## ➡️ 다음 단계

1. [Client Configuration Guide](./client-configuration.md) - 다른 서비스에서 Config Service 연결 방법
2. [Architecture Document](../architecture/config-service-architecture.md) - Config Service 아키텍처 이해
3. [Runbook](../runbooks/config-service-operations.md) - 운영 가이드

---

## 🔗 참고 자료

- [Spring Cloud Config 공식 문서](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)
- [Config 저장소 (GitHub)](https://github.com/L-a-z-e/portal-universe-config-repo.git)
- [Portal Universe 전체 아키텍처](../../../docs/architecture/)

---

**최종 업데이트**: 2026-01-18
