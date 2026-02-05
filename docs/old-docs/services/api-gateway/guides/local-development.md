---
id: api-gateway-guide-local-development
title: API Gateway 로컬 개발 가이드
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: documenter-agent
tags: [api-gateway, local-development, setup, configuration]
related: []
---

# API Gateway 로컬 개발 가이드

이 가이드는 API Gateway를 로컬 환경에서 개발하고 테스트하는 방법을 설명합니다.

## 1. 사전 요구사항

### 필수 소프트웨어
- **Java 17**: OpenJDK 17 이상
- **Gradle 8.x**: 빌드 도구 (프로젝트에 Gradle Wrapper 포함)
- **Config Server**: API Gateway는 Config Server로부터 설정을 가져오므로 반드시 먼저 실행되어야 합니다

### 선택적 요구사항
- **Docker**: 의존 서비스 실행 시 필요
- **IntelliJ IDEA / VS Code**: 개발 IDE

## 2. 프로젝트 구조

```
services/api-gateway/
├── src/main/java/com/portal/universe/apigateway/
│   ├── config/           # 설정 클래스 (Security, CORS, Circuit Breaker 등)
│   ├── controller/       # Fallback 컨트롤러
│   └── ApiGatewayApplication.java  # 메인 애플리케이션
├── src/main/resources/
│   └── application.yml   # 로컬 설정 (Config Server URL, Profile 등)
├── src/test/java/        # 테스트 코드
└── build.gradle          # 빌드 설정
```

### 주요 디렉토리 설명

- **config/**: Spring Cloud Gateway 설정, OAuth2 Resource Server, CORS 정책 등
- **controller/**: Circuit Breaker fallback 엔드포인트
- **application.yml**: Config Server 연결 정보 및 기본 프로필 설정

## 3. 빌드 및 실행

### 3.1 빌드

프로젝트 루트에서 다음 명령어를 실행합니다:

```bash
# 전체 프로젝트 빌드
./gradlew build

# API Gateway만 빌드
./gradlew :services:api-gateway:build

# 테스트 없이 빌드
./gradlew :services:api-gateway:build -x test
```

### 3.2 실행

```bash
# Gradle을 통한 실행
./gradlew :services:api-gateway:bootRun

# Spring 프로필 지정
./gradlew :services:api-gateway:bootRun --args='--spring.profiles.active=local'

# JAR 파일 직접 실행
java -jar services/api-gateway/build/libs/api-gateway-*.jar
```

### 3.3 테스트

```bash
# 전체 테스트 실행
./gradlew :services:api-gateway:test

# 특정 테스트 클래스 실행
./gradlew :services:api-gateway:test --tests "ApiGatewayApplicationTests"

# 테스트 리포트 확인
open services/api-gateway/build/reports/tests/test/index.html
```

## 4. 환경 설정

### 4.1 Config Server 연결

API Gateway는 Spring Cloud Config를 통해 중앙 집중식 설정을 사용합니다.

**환경 변수 설정:**
```bash
# Config Server URL (기본값)
export CONFIG_SERVER_URL=http://localhost:8888

# Docker Compose 환경
export CONFIG_SERVER_URL=http://config-service:8888
```

**application.yml 기본 구조:**
```yaml
spring:
  application:
    name: api-gateway
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:http://config-service:8888}
  cloud:
    config:
      fail-fast: false  # Config Server 미연결 시에도 실행 가능
```

### 4.2 포트 설정

- **기본 포트**: 8080
- **Actuator 포트**: 8080 (동일 포트 사용)

포트 변경이 필요한 경우:
```bash
./gradlew :services:api-gateway:bootRun --args='--server.port=9090'
```

### 4.3 JWT 검증 설정

API Gateway는 OAuth2 Resource Server로 동작하며, JWT 토큰을 검증합니다.

**필요한 설정 (Config Server에서 관리):**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081  # auth-service URL
          jwk-set-uri: http://localhost:8081/oauth2/jwks
```

## 5. Spring 프로필

API Gateway는 환경별로 다른 프로필을 지원합니다.

| 프로필 | 설명 | 사용 시점 |
|--------|------|-----------|
| `local` | 로컬 개발 환경 (기본값) | 개발자 PC에서 직접 실행 |
| `docker` | Docker Compose 환경 | docker-compose.yml 사용 |
| `k8s` | Kubernetes 환경 | 클러스터 배포 |

**프로필 활성화:**
```bash
# local 프로필
./gradlew :services:api-gateway:bootRun --args='--spring.profiles.active=local'

# docker 프로필
./gradlew :services:api-gateway:bootRun --args='--spring.profiles.active=docker'
```

## 6. 로컬 테스트 방법

### 6.1 헬스체크

API Gateway가 정상적으로 실행되었는지 확인합니다.

```bash
# 헬스체크
curl http://localhost:8080/actuator/health

# 예상 응답
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 6.2 공개 경로 테스트

인증 없이 접근 가능한 경로를 테스트합니다.

```bash
# Auth Service - 로그인 페이지
curl http://localhost:8080/api/v1/auth/login

# Auth Service - OAuth2 Authorization Endpoint
curl http://localhost:8080/api/v1/auth/oauth2/authorize

# Blog Service - 공개 게시물 목록
curl http://localhost:8080/api/v1/blog/posts/public
```

### 6.3 JWT 토큰 테스트

인증이 필요한 API를 테스트합니다.

**1. 토큰 발급:**
```bash
# auth-service에서 토큰 발급 (예시)
curl -X POST http://localhost:8081/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=user&password=password&client_id=portal-client"
```

**2. 토큰으로 API 호출:**
```bash
# 인증이 필요한 엔드포인트 호출
curl http://localhost:8080/api/v1/blog/posts \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 6.4 라우팅 규칙 확인

API Gateway는 다음과 같은 라우팅 규칙을 사용합니다:

| 경로 패턴 | 대상 서비스 | 포트 |
|-----------|------------|------|
| `/api/v1/auth/**` | auth-service | 8081 |
| `/api/v1/blog/**` | blog-service | 8082 |
| `/api/v1/shopping/**` | shopping-service | 8083 |
| `/api/v1/notifications/**` | notification-service | 8084 |

**라우팅 테스트:**
```bash
# Blog Service로 라우팅
curl http://localhost:8080/api/v1/blog/health

# Shopping Service로 라우팅
curl http://localhost:8080/api/v1/shopping/products
```

## 7. 트러블슈팅

### 7.1 Config Server 연결 실패

**증상:**
```
Could not locate PropertySource: I/O error on GET request for "http://config-service:8888/...
```

**해결 방법:**
1. Config Server가 실행 중인지 확인:
   ```bash
   curl http://localhost:8888/actuator/health
   ```

2. Config Server URL 환경 변수 확인:
   ```bash
   export CONFIG_SERVER_URL=http://localhost:8888
   ```

3. `fail-fast: false` 설정으로 임시 우회:
   ```yaml
   spring:
     cloud:
       config:
         fail-fast: false
   ```

### 7.2 JWT 검증 실패

**증상:**
```
401 Unauthorized
WWW-Authenticate: Bearer error="invalid_token"
```

**해결 방법:**
1. auth-service가 실행 중인지 확인:
   ```bash
   curl http://localhost:8081/actuator/health
   ```

2. JWT 설정 확인:
   ```yaml
   spring:
     security:
       oauth2:
         resourceserver:
           jwt:
             issuer-uri: http://localhost:8081
   ```

3. 토큰 만료 확인:
   - 토큰이 만료되었으면 재발급 필요
   - [jwt.io](https://jwt.io)에서 토큰 디코딩하여 `exp` 클레임 확인

4. 로그 레벨 상향:
   ```yaml
   logging:
     level:
       org.springframework.security: DEBUG
   ```

### 7.3 CORS 오류

**증상:**
```
Access to XMLHttpRequest at 'http://localhost:8080/api/v1/blog/posts'
from origin 'http://localhost:30000' has been blocked by CORS policy
```

**해결 방법:**
1. Config Server의 CORS 설정 확인:
   ```yaml
   spring:
     cloud:
       gateway:
         globalcors:
           corsConfigurations:
             '[/**]':
               allowedOrigins: "http://localhost:30000"
               allowedMethods: "*"
               allowedHeaders: "*"
   ```

2. 프론트엔드 origin이 허용 목록에 포함되어 있는지 확인

3. Preflight 요청 확인:
   ```bash
   curl -X OPTIONS http://localhost:8080/api/v1/blog/posts \
     -H "Origin: http://localhost:30000" \
     -H "Access-Control-Request-Method: GET" \
     -v
   ```

### 7.4 Circuit Breaker 작동

**증상:**
```
503 Service Unavailable
Fallback response
```

**해결 방법:**
1. 대상 서비스 상태 확인:
   ```bash
   curl http://localhost:8082/actuator/health  # blog-service
   ```

2. Circuit Breaker 상태 확인:
   ```bash
   curl http://localhost:8080/actuator/circuitbreakers
   ```

3. Circuit Breaker 설정 조정 (Config Server):
   ```yaml
   resilience4j:
     circuitbreaker:
       instances:
         blogService:
           slidingWindowSize: 10
           failureRateThreshold: 50
           waitDurationInOpenState: 10000
   ```

### 7.5 메모리 부족

**증상:**
```
OutOfMemoryError: Java heap space
```

**해결 방법:**
1. JVM 힙 메모리 증가:
   ```bash
   export JAVA_OPTS="-Xmx512m -Xms256m"
   ./gradlew :services:api-gateway:bootRun
   ```

2. Gradle 데몬 메모리 설정 (gradle.properties):
   ```properties
   org.gradle.jvmargs=-Xmx2048m
   ```

## 8. 개발 팁

### 8.1 핫 리로드

Spring Boot DevTools를 사용하여 코드 변경 시 자동 재시작:

```gradle
// build.gradle
dependencies {
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
}
```

### 8.2 로깅 설정

디버깅을 위한 로그 레벨 조정:

```yaml
logging:
  level:
    com.portal.universe.apigateway: DEBUG
    org.springframework.cloud.gateway: DEBUG
    org.springframework.security: DEBUG
```

### 8.3 Actuator 엔드포인트

유용한 모니터링 엔드포인트:

```bash
# Gateway 라우트 정보
curl http://localhost:8080/actuator/gateway/routes

# 메트릭 정보
curl http://localhost:8080/actuator/metrics

# Circuit Breaker 상태
curl http://localhost:8080/actuator/circuitbreakers
```

### 8.4 IDE 설정

**IntelliJ IDEA:**
1. `File` → `Project Structure` → `Project SDK`: Java 17 선택
2. `Run` → `Edit Configurations` → `+` → `Spring Boot`
3. Main class: `com.portal.universe.apigateway.ApiGatewayApplication`
4. Environment variables: `CONFIG_SERVER_URL=http://localhost:8888`

## 9. 다음 단계

- [API Gateway 아키텍처](../architecture/README.md)
- [라우팅 설정 가이드](./routing-configuration.md)
- [보안 설정 가이드](./security-configuration.md)
- [Circuit Breaker 설정 가이드](./circuit-breaker.md)

## 참고 자료

- [Spring Cloud Gateway 공식 문서](https://spring.io/projects/spring-cloud-gateway)
- [OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [Resilience4j 문서](https://resilience4j.readme.io/)
