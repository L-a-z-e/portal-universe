---
id: api-config
title: Config Service API
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [api, config, spring-cloud-config]
related:
  - architecture-config
---

# Config Service API

> Spring Cloud Config Server API 명세서

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `http://localhost:8888` |
| **인증** | 없음 (내부 서비스 전용) |
| **버전** | Spring Cloud 2025.0.0 |
| **Config Repo** | https://github.com/L-a-z-e/portal-universe-config-repo.git |

Config Service는 Spring Cloud Config Server를 기반으로 모든 마이크로서비스의 설정을 중앙에서 관리합니다. Git 저장소를 백엔드로 사용하여 버전 관리와 감사 추적이 가능합니다.

---

## 📑 API 목록

### 설정 조회

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/{application}/{profile}` | 애플리케이션/프로파일별 설정 조회 |
| GET | `/{application}/{profile}/{label}` | 특정 브랜치의 설정 조회 |
| GET | `/{application}-{profile}.yml` | YAML 형식으로 설정 다운로드 |
| GET | `/{application}-{profile}.properties` | Properties 형식으로 설정 다운로드 |

### 암호화/복호화

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/encrypt` | 평문을 암호화 |
| POST | `/decrypt` | 암호문을 복호화 |

### Actuator 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/actuator/health` | 서버 상태 확인 |
| GET | `/actuator/info` | 서버 정보 조회 |
| POST | `/actuator/bus-refresh` | 모든 클라이언트 설정 갱신 (Spring Cloud Bus 사용 시) |

---

## 🔹 설정 조회 (JSON)

### Request

```http
GET /auth-service/local
Accept: application/json
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|----------|------|------|------|------|
| `application` | string | ✅ | 애플리케이션 이름 | `auth-service`, `blog-service` |
| `profile` | string | ✅ | 프로파일 (환경) | `local`, `docker`, `k8s` |

### Response (200 OK)

```json
{
  "name": "auth-service",
  "profiles": ["local"],
  "label": null,
  "version": "a1b2c3d4e5f6",
  "state": null,
  "propertySources": [
    {
      "name": "https://github.com/L-a-z-e/portal-universe-config-repo.git/auth-service-local.yml",
      "source": {
        "spring.datasource.url": "jdbc:mysql://localhost:3306/auth_db",
        "spring.datasource.username": "root",
        "spring.datasource.driver-class-name": "com.mysql.cj.jdbc.Driver",
        "spring.jpa.hibernate.ddl-auto": "update",
        "spring.jpa.show-sql": "true",
        "server.port": "8081"
      }
    },
    {
      "name": "https://github.com/L-a-z-e/portal-universe-config-repo.git/auth-service.yml",
      "source": {
        "spring.application.name": "auth-service",
        "management.endpoints.web.exposure.include": "health,info"
      }
    }
  ]
}
```

### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `name` | string | 애플리케이션 이름 |
| `profiles` | string[] | 적용된 프로파일 목록 |
| `label` | string | Git 브랜치/태그 (null이면 기본 브랜치) |
| `version` | string | Git 커밋 해시 |
| `propertySources` | array | 설정 소스 목록 (우선순위 순) |
| `propertySources[].name` | string | 설정 파일 경로 |
| `propertySources[].source` | object | 실제 설정 key-value 쌍 |

### cURL 예시

```bash
curl http://localhost:8888/auth-service/local
```

---

## 🔹 설정 조회 (특정 브랜치)

### Request

```http
GET /blog-service/docker/develop
Accept: application/json
```

### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|----------|------|------|------|------|
| `application` | string | ✅ | 애플리케이션 이름 | `blog-service` |
| `profile` | string | ✅ | 프로파일 | `docker` |
| `label` | string | ✅ | Git 브랜치/태그/커밋 | `develop`, `v1.0.0`, `abc123` |

### Response (200 OK)

```json
{
  "name": "blog-service",
  "profiles": ["docker"],
  "label": "develop",
  "version": "f6e5d4c3b2a1",
  "state": null,
  "propertySources": [
    {
      "name": "https://github.com/L-a-z-e/portal-universe-config-repo.git/blog-service-docker.yml (document #0)",
      "source": {
        "spring.data.mongodb.uri": "mongodb://mongodb:27017/blog_db"
      }
    }
  ]
}
```

### cURL 예시

```bash
curl http://localhost:8888/blog-service/docker/develop
```

---

## 🔹 YAML 형식 다운로드

### Request

```http
GET /auth-service-local.yml
Accept: text/plain
```

### Response (200 OK)

```yaml
server:
  port: 8081

spring:
  application:
    name: auth-service
  datasource:
    url: jdbc:mysql://localhost:3306/auth_db
    username: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### cURL 예시

```bash
curl http://localhost:8888/auth-service-local.yml
```

---

## 🔹 Properties 형식 다운로드

### Request

```http
GET /shopping-service-k8s.properties
Accept: text/plain
```

### Response (200 OK)

```properties
server.port=8083
spring.application.name=shopping-service
spring.datasource.url=jdbc:mysql://mysql:3306/shopping_db
spring.datasource.username=root
spring.jpa.hibernate.ddl-auto=update
```

### cURL 예시

```bash
curl http://localhost:8888/shopping-service-k8s.properties
```

---

## 🔹 평문 암호화

### Request

```http
POST /encrypt
Content-Type: text/plain

mysecretpassword
```

### Request Body

평문 텍스트를 body에 직접 전송합니다.

### Response (200 OK)

```text
AQATBxEwvLN...암호화된긴문자열...9fKL3aQ==
```

### 암호화된 값 사용 방법

```yaml
# application.yml에서 사용
spring:
  datasource:
    password: '{cipher}AQATBxEwvLN...9fKL3aQ=='
```

`{cipher}` 접두사를 사용하면 Config Client가 자동으로 복호화합니다.

### cURL 예시

```bash
curl -X POST http://localhost:8888/encrypt \
  -H "Content-Type: text/plain" \
  -d "mysecretpassword"
```

---

## 🔹 암호문 복호화

### Request

```http
POST /decrypt
Content-Type: text/plain

AQATBxEwvLN...암호화된긴문자열...9fKL3aQ==
```

### Request Body

암호화된 텍스트를 body에 직접 전송합니다.

### Response (200 OK)

```text
mysecretpassword
```

### cURL 예시

```bash
curl -X POST http://localhost:8888/decrypt \
  -H "Content-Type: text/plain" \
  -d "AQATBxEwvLN...9fKL3aQ=="
```

---

## 🔹 Health Check

### Request

```http
GET /actuator/health
Accept: application/json
```

### Response (200 OK)

```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### cURL 예시

```bash
curl http://localhost:8888/actuator/health
```

---

## 🔹 서버 정보 조회

### Request

```http
GET /actuator/info
Accept: application/json
```

### Response (200 OK)

```json
{
  "app": {
    "name": "config-service",
    "description": "Spring Cloud Config Server",
    "version": "1.0.0"
  }
}
```

### cURL 예시

```bash
curl http://localhost:8888/actuator/info
```

---

## 🔹 설정 갱신 (Spring Cloud Bus)

> **Note**: Spring Cloud Bus와 RabbitMQ/Kafka가 설정된 경우에만 사용 가능

### Request

```http
POST /actuator/bus-refresh
```

### Response (204 No Content)

응답 본문 없음. 모든 연결된 Config Client에게 refresh 이벤트가 전파됩니다.

### 동작 방식

1. `/actuator/bus-refresh` 호출
2. Config Server가 메시지 브로커(Kafka/RabbitMQ)에 이벤트 발행
3. 모든 Config Client가 이벤트 수신
4. 각 Client가 자동으로 설정 갱신

### cURL 예시

```bash
curl -X POST http://localhost:8888/actuator/bus-refresh
```

---

## ⚠️ 에러 응답

### 404 Not Found - 설정 파일 없음

```json
{
  "timestamp": "2026-01-18T10:30:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "No such label: wrong-branch",
  "path": "/auth-service/local/wrong-branch"
}
```

**원인**:
- 존재하지 않는 Git 브랜치/태그 지정
- Git 저장소에 해당 설정 파일 없음

**해결**:
- Git 저장소 확인
- 브랜치명 확인

---

### 500 Internal Server Error - Git 저장소 접근 실패

```json
{
  "timestamp": "2026-01-18T10:30:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Error occurred cloning to base directory",
  "path": "/auth-service/local"
}
```

**원인**:
- Git 저장소 URL 잘못됨
- 인증 정보 오류 (private 저장소)
- 네트워크 문제

**해결**:
- `application.yml`에서 `spring.cloud.config.server.git.uri` 확인
- Git credentials 확인

---

## 🔒 보안 고려사항

### 1. 암호화 키 관리

Config Server는 암호화/복호화를 위해 대칭키 또는 비대칭키를 사용합니다.

**대칭키 설정 (bootstrap.yml)**
```yaml
encrypt:
  key: mySymmetricKey
```

**비대칭키 설정 (Keystore)**
```yaml
encrypt:
  key-store:
    location: classpath:/config-server.jks
    password: keystorePassword
    alias: configServerKey
    secret: keyPassword
```

### 2. 접근 제어

Config Server는 기본적으로 인증이 없습니다. 프로덕션 환경에서는 반드시 보안 설정이 필요합니다.

**Spring Security 추가 예시**
```yaml
spring:
  security:
    user:
      name: configUser
      password: '{cipher}AQA...'
```

### 3. Git 저장소 인증

**SSH 키 사용**
```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: git@github.com:user/config-repo.git
          ignore-local-ssh-settings: false
```

**HTTP 인증**
```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/user/config-repo.git
          username: gituser
          password: '{cipher}AQA...'
```

---

## 📌 설정 파일 우선순위

Config Server는 다음 순서로 설정 파일을 탐색하고 병합합니다 (아래쪽이 우선):

1. `application.yml` (모든 애플리케이션 공통)
2. `application-{profile}.yml` (프로파일별 공통)
3. `{application}.yml` (특정 애플리케이션)
4. `{application}-{profile}.yml` (특정 애플리케이션 + 프로파일)

### 예시: auth-service/local 조회 시

```
1. application.yml              (우선순위 낮음)
2. application-local.yml
3. auth-service.yml
4. auth-service-local.yml       (우선순위 높음)
```

동일한 키가 여러 파일에 있으면 **우선순위가 높은 파일의 값이 적용**됩니다.

---

## 🔄 Config Client 통합

### 1. 의존성 추가 (build.gradle)

```gradle
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-config'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

### 2. bootstrap.yml 설정

```yaml
spring:
  application:
    name: auth-service
  cloud:
    config:
      uri: http://localhost:8888
      fail-fast: true
      retry:
        max-attempts: 6
        initial-interval: 1000
        max-interval: 2000
  profiles:
    active: local
```

### 3. 런타임 설정 갱신

**@RefreshScope 사용**
```java
@RestController
@RefreshScope
public class ConfigController {

    @Value("${custom.message}")
    private String message;

    @GetMapping("/message")
    public String getMessage() {
        return message;
    }
}
```

**갱신 트리거**
```bash
curl -X POST http://localhost:8081/actuator/refresh
```

---

## 📊 모니터링

### JVM 메트릭 (Actuator)

```http
GET /actuator/metrics
GET /actuator/metrics/jvm.memory.used
GET /actuator/metrics/http.server.requests
```

### Git 저장소 상태 확인

Config Server 로그에서 Git 클론/풀 작업을 확인할 수 있습니다:

```
INFO  Adding property source: file:/tmp/config-repo-xxx/auth-service.yml
INFO  Adding property source: file:/tmp/config-repo-xxx/auth-service-local.yml
```

---

## 🔗 관련 문서

- [Config Service Architecture](../architecture/config-architecture.md)
- [Spring Cloud Config 공식 문서](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/)
- [Config Repository](https://github.com/L-a-z-e/portal-universe-config-repo)

---

## 📝 Changelog

### v1.0.0 (2026-01-18)
- 최초 API 문서 작성
- Spring Cloud Config Server 기본 API 명세
- 암호화/복호화 API 문서화
- Actuator 엔드포인트 추가

---

**최종 업데이트**: 2026-01-18
