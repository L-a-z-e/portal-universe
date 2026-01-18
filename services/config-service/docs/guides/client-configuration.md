---
id: guide-config-client-configuration
title: Config Client Configuration Guide
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Portal Universe Team
tags: [config-client, spring-cloud-config, configuration, setup]
related:
  - guide-config-getting-started
  - arch-config-service
---

# Config Client Configuration Guide

> 다른 서비스에서 Config Service에 연결하는 방법

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 20분 |
| **대상** | 백엔드 개발자, 마이크로서비스 개발자 |
| **적용 대상 서비스** | auth-service, blog-service, shopping-service 등 |

Spring Cloud Config Client를 사용하여 Config Service에서 설정을 가져오는 방법을 설명합니다.

---

## 🎯 Config Client 동작 원리

```
┌─────────────────┐
│  Client Service │
│  (auth-service) │
└────────┬────────┘
         │ 1. 시작 시 설정 요청
         │ GET /auth-service/local
         ▼
┌─────────────────┐
│ Config Service  │
│   (Port 8888)   │
└────────┬────────┘
         │ 2. Git에서 설정 가져오기
         ▼
┌─────────────────┐
│  Git Repository │
│  (config-repo)  │
└─────────────────┘
```

### 설정 우선순위

높음 ← → 낮음

1. **명령줄 인수**: `--server.port=8081`
2. **환경 변수**: `SERVER_PORT=8081`
3. **Config Service**: `auth-service-local.yml`
4. **로컬 설정 파일**: `application-local.yml`
5. **기본 설정**: `application.yml`

---

## 🔧 클라이언트 설정 방법

### Step 1: Gradle 의존성 추가

`services/[service-name]/build.gradle`:

```gradle
dependencies {
    // Spring Cloud Config Client
    implementation 'org.springframework.cloud:spring-cloud-starter-config'

    // Spring Boot Actuator (선택, Refresh 기능 사용 시)
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Spring Cloud Bus (선택, 자동 갱신 사용 시)
    implementation 'org.springframework.cloud:spring-cloud-starter-bus-kafka'
}
```

### Step 2: application.yml 설정

**기본 설정** (`src/main/resources/application.yml`):

```yaml
spring:
  application:
    name: auth-service  # Config Service에서 찾을 설정 파일 이름

  config:
    import: optional:configserver:http://localhost:8888  # Config Service URL

  cloud:
    config:
      fail-fast: false  # Config Service 연결 실패 시에도 서비스 시작
      retry:
        initial-interval: 1000  # 재시도 초기 대기 시간 (ms)
        max-attempts: 6         # 최대 재시도 횟수
        multiplier: 1.1         # 재시도 대기 시간 증가 배율
        max-interval: 2000      # 최대 대기 시간 (ms)
```

### Step 3: 프로필별 Config Service URL 설정

**로컬 개발 환경** (`application-local.yml`):

```yaml
spring:
  config:
    import: optional:configserver:http://localhost:8888
```

**Docker 환경** (`application-docker.yml`):

```yaml
spring:
  config:
    import: optional:configserver:http://config-service:8888
```

**Kubernetes 환경** (`application-k8s.yml`):

```yaml
spring:
  config:
    import: optional:configserver:http://config-service.default.svc.cluster.local:8888
```

---

## 📂 Config 저장소 파일 작성

### 명명 규칙

Config Service는 다음 순서로 설정 파일을 찾습니다:

```
{application-name}-{profile}.yml  (우선순위 높음)
{application-name}.yml
application-{profile}.yml
application.yml                   (우선순위 낮음)
```

**예시 (auth-service, local 프로필)**:
1. `auth-service-local.yml` ← 가장 먼저 적용
2. `auth-service.yml`
3. `application-local.yml`
4. `application.yml`

### 설정 파일 예시

**공통 설정** (`application.yml`):

```yaml
# 모든 서비스에 적용되는 설정
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus

  metrics:
    export:
      prometheus:
        enabled: true

logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

**서비스별 기본 설정** (`auth-service.yml`):

```yaml
server:
  port: 8081

spring:
  application:
    name: auth-service

# OAuth2 공통 설정
oauth2:
  token:
    validity:
      access-token: 3600
      refresh-token: 86400
```

**프로필별 설정** (`auth-service-local.yml`):

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_db
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

  kafka:
    bootstrap-servers: localhost:9092

logging:
  level:
    com.portaluniverse.auth: DEBUG
```

---

## 🔄 동적 설정 갱신

### @RefreshScope 사용

설정이 변경되었을 때 서비스를 재시작하지 않고 갱신할 수 있습니다.

#### Step 1: Actuator Refresh 엔드포인트 활성화

`application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,refresh  # refresh 추가
```

#### Step 2: @RefreshScope 적용

```java
package com.portaluniverse.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope  // 설정 갱신 시 이 빈을 재생성
public class FeatureToggleConfig {

    @Value("${feature.new-checkout:false}")
    private boolean newCheckoutEnabled;

    @Value("${feature.beta-ui:false}")
    private boolean betaUiEnabled;

    public boolean isNewCheckoutEnabled() {
        return newCheckoutEnabled;
    }

    public boolean isBetaUiEnabled() {
        return betaUiEnabled;
    }
}
```

#### Step 3: 설정 갱신 트리거

**A. 단일 서비스 갱신**:

```bash
# Config 저장소에서 설정 변경 후 Commit & Push

# 특정 서비스의 설정 갱신
curl -X POST http://localhost:8081/actuator/refresh
```

**응답 예시**:
```json
[
  "feature.new-checkout",
  "feature.beta-ui"
]
```

**B. Spring Cloud Bus를 통한 전체 서비스 갱신**:

```bash
# Config Service에 갱신 요청 → 모든 클라이언트에 전파
curl -X POST http://localhost:8888/actuator/bus-refresh
```

---

## 🧪 설정 테스트

### 1. 설정이 제대로 로드되는지 확인

**서비스 시작 로그**:
```
INFO --- Fetching config from server at: http://localhost:8888
INFO --- Located environment: name=auth-service, profiles=[local], ...
INFO --- Located property source: CompositePropertySource {name='configService', ...}
```

### 2. Actuator로 현재 설정 확인

```bash
# 환경 변수 및 설정 확인
curl http://localhost:8081/actuator/env

# 특정 프로퍼티 확인
curl http://localhost:8081/actuator/env/spring.datasource.url
```

**응답 예시**:
```json
{
  "property": {
    "source": "configserver:https://github.com/.../auth-service-local.yml",
    "value": "jdbc:mysql://localhost:3306/auth_db"
  }
}
```

### 3. 설정 변경 테스트

**테스트 시나리오**:

```bash
# 1. 현재 설정 확인
curl http://localhost:8081/api/v1/feature-toggles

# 2. Config 저장소에서 설정 변경
cd /path/to/config-repo
vim auth-service-local.yml
# feature.new-checkout: true 로 변경
git commit -am "Enable new checkout"
git push

# 3. 설정 갱신 트리거
curl -X POST http://localhost:8081/actuator/refresh

# 4. 변경된 설정 확인
curl http://localhost:8081/api/v1/feature-toggles
```

---

## ⚙️ 고급 설정

### 1. Config Service 인증

Config Service가 인증을 요구하는 경우:

```yaml
spring:
  cloud:
    config:
      username: config-user
      password: ${CONFIG_PASSWORD}  # 환경 변수로 관리
```

### 2. Config Service 장애 대응

**Fail Fast 모드** (권장하지 않음):

```yaml
spring:
  cloud:
    config:
      fail-fast: true  # Config Service 연결 실패 시 서비스 시작 중단
```

**Resilient 모드** (권장):

```yaml
spring:
  cloud:
    config:
      fail-fast: false  # Config Service 연결 실패해도 로컬 설정으로 시작
```

### 3. 암호화된 설정 사용

**Config 저장소에서 암호화**:

```yaml
# auth-service-local.yml
spring:
  datasource:
    password: '{cipher}AQA12abc...'  # 암호화된 값
```

**Config Service 설정**:

```yaml
encrypt:
  key: my-secret-encryption-key  # 대칭키 사용
```

**암호화/복호화 테스트**:

```bash
# 암호화
curl -X POST http://localhost:8888/encrypt -d "mysecretpassword"

# 복호화
curl -X POST http://localhost:8888/decrypt -d "{cipher}AQA12abc..."
```

### 4. 특정 Label(브랜치) 사용

```yaml
spring:
  cloud:
    config:
      label: feature-branch  # 특정 Git 브랜치의 설정 사용
```

---

## 📊 모니터링

### Config Client 메트릭

**Actuator 엔드포인트**:

```bash
# 설정 소스 확인
curl http://localhost:8081/actuator/env

# Config Service 연결 상태
curl http://localhost:8081/actuator/health
```

### Prometheus 메트릭

Config Client는 다음 메트릭을 제공합니다:

- `config.client.fetch.duration`: 설정 가져오는 시간
- `config.client.fetch.error.count`: 설정 가져오기 실패 횟수

---

## ⚠️ 자주 발생하는 문제

### 문제 1: Config Service 연결 실패로 서비스 시작 불가

**증상**:
```
Could not resolve placeholder 'spring.datasource.url'
```

**원인**:
- Config Service가 실행되지 않음
- `fail-fast: true` 설정

**해결 방법**:

```yaml
# fail-fast를 false로 설정하고 로컬 설정 추가
spring:
  cloud:
    config:
      fail-fast: false

  datasource:
    url: jdbc:mysql://localhost:3306/auth_db  # 로컬 fallback 설정
```

### 문제 2: 설정 변경이 반영되지 않음

**증상**:
- Config 저장소에서 변경했지만 서비스에서 이전 값 사용

**원인**:
- `/actuator/refresh` 호출하지 않음
- `@RefreshScope` 누락

**해결 방법**:

```bash
# 1. @RefreshScope가 적용되었는지 확인
# 2. Refresh 엔드포인트 호출
curl -X POST http://localhost:8081/actuator/refresh

# 3. 또는 서비스 재시작
./gradlew :services:auth-service:bootRun
```

### 문제 3: 잘못된 프로필의 설정이 로드됨

**증상**:
- `docker` 프로필 설정이 로드되어야 하는데 `local` 설정이 로드됨

**원인**:
- Active Profile 미지정

**해결 방법**:

```bash
# 프로필 명시적 지정
./gradlew :services:auth-service:bootRun --args='--spring.profiles.active=docker'

# 또는 환경 변수로
export SPRING_PROFILES_ACTIVE=docker
./gradlew :services:auth-service:bootRun
```

### 문제 4: 설정 파일을 찾을 수 없음

**증상**:
```
No such file: auth-service-local.yml
```

**원인**:
- Config 저장소에 파일이 없음
- 파일 이름 오타

**해결 방법**:

```bash
# Config 저장소 확인
cd /path/to/config-repo
ls -l auth-service*

# Config Service에서 확인
curl http://localhost:8888/auth-service/local

# 없다면 생성
cat > auth-service-local.yml << EOF
server:
  port: 8081
EOF

git add auth-service-local.yml
git commit -m "Add auth-service local config"
git push
```

---

## 🔒 보안 고려사항

### 1. 민감한 정보 관리

**절대 Git에 직접 저장하지 마세요**:
- Database 비밀번호
- API Key
- Secret Key

**대신 다음 방법 사용**:

**A. Config Service의 암호화 기능**:
```yaml
spring:
  datasource:
    password: '{cipher}AQA12abc...'
```

**B. 환경 변수로 오버라이드**:
```yaml
spring:
  datasource:
    password: ${DB_PASSWORD}  # 환경 변수에서 주입
```

**C. Vault 통합**:
```yaml
spring:
  cloud:
    config:
      server:
        vault:
          host: localhost
          port: 8200
```

### 2. Config Service 접근 제어

```yaml
# Config Service에 Basic Auth 추가
spring:
  security:
    user:
      name: config-admin
      password: ${CONFIG_ADMIN_PASSWORD}
```

---

## ✅ 체크리스트

새 서비스에 Config Client를 추가할 때:

- [ ] Gradle 의존성 추가 (`spring-cloud-starter-config`)
- [ ] `spring.application.name` 설정
- [ ] `spring.config.import` 설정
- [ ] Config 저장소에 `{service-name}.yml` 파일 생성
- [ ] Config 저장소에 `{service-name}-{profile}.yml` 파일 생성
- [ ] 로컬에서 설정 로드 테스트
- [ ] `@RefreshScope` 적용 (동적 갱신 필요한 경우)
- [ ] Actuator `/refresh` 엔드포인트 활성화
- [ ] 민감한 정보 암호화 또는 환경 변수 사용

---

## ➡️ 다음 단계

1. [Getting Started Guide](./getting-started.md) - Config Service 설정 및 실행
2. [Spring Cloud Bus 통합](./spring-cloud-bus-integration.md) - 자동 설정 갱신 (예정)
3. [Vault 통합 가이드](./vault-integration.md) - 민감 정보 관리 (예정)

---

## 🔗 참고 자료

- [Spring Cloud Config Client 공식 문서](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#_spring_cloud_config_client)
- [Spring Cloud Bus 공식 문서](https://docs.spring.io/spring-cloud-bus/docs/current/reference/html/)
- [Config 저장소 (GitHub)](https://github.com/L-a-z-e/portal-universe-config-repo.git)

---

**최종 업데이트**: 2026-01-18
