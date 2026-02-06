---
id: notification-service-guide-local-development
title: Notification Service 로컬 개발 가이드
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [notification-service, local-development, kafka, setup, email]
related: []
---

# Notification Service 로컬 개발 가이드

이 가이드는 Notification Service를 로컬 환경에서 개발하고 테스트하는 방법을 설명합니다.

## 1. 사전 요구사항

### 필수 소프트웨어
- **Java 17**: OpenJDK 17 이상
- **Gradle 8.x**: 빌드 도구 (프로젝트에 Gradle Wrapper 포함)
- **Kafka 4.1.0**: 메시지 브로커 (KRaft 모드, Zookeeper 불필요)
- **Config Server**: Notification Service는 Config Server로부터 설정을 가져오므로 반드시 먼저 실행되어야 합니다

### 선택적 요구사항
- **Docker**: Kafka 및 의존 서비스 실행 시 필요
- **IntelliJ IDEA / VS Code**: 개발 IDE
- **SMTP 서버**: 이메일 발송 테스트 시 필요 (Gmail, Mailhog 등)

## 2. 프로젝트 구조

```
services/notification-service/
├── src/main/java/com/portal/universe/notificationservice/
│   ├── consumer/          # Kafka 컨슈머 클래스
│   │   └── NotificationConsumer.java
│   └── NotificationServiceApplication.java  # 메인 애플리케이션
├── src/main/resources/
│   └── application.yml    # 로컬 설정 (Config Server URL, Profile 등)
├── src/test/java/         # 테스트 코드
└── build.gradle           # 빌드 설정
```

### 주요 디렉토리 설명

- **consumer/**: Kafka 토픽을 구독하고 이벤트를 처리하는 컨슈머 클래스
- **application.yml**: Config Server 연결 정보 및 기본 프로필 설정

## 3. 빌드 및 실행

### 3.1 빌드

프로젝트 루트에서 다음 명령어를 실행합니다:

```bash
# 전체 프로젝트 빌드
./gradlew build

# Notification Service만 빌드
./gradlew :services:notification-service:build

# 테스트 없이 빌드
./gradlew :services:notification-service:build -x test
```

### 3.2 실행

```bash
# Gradle을 통한 실행
./gradlew :services:notification-service:bootRun

# Spring 프로필 지정
./gradlew :services:notification-service:bootRun --args='--spring.profiles.active=local'

# JAR 파일 직접 실행
java -jar services/notification-service/build/libs/notification-service-*.jar
```

### 3.3 테스트

```bash
# 전체 테스트 실행
./gradlew :services:notification-service:test

# 특정 테스트 클래스 실행
./gradlew :services:notification-service:test --tests "NotificationServiceApplicationTests"

# 테스트 리포트 확인
open services/notification-service/build/reports/tests/test/index.html
```

## 4. 환경 설정

### 4.1 Config Server 연결

Notification Service는 Spring Cloud Config를 통해 중앙 집중식 설정을 사용합니다.

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
    name: notification-service
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:http://config-service:8888}
  cloud:
    config:
      fail-fast: false  # Config Server 미연결 시에도 실행 가능
```

### 4.2 포트 설정

- **기본 포트**: 8084
- **Actuator 포트**: 8084 (동일 포트 사용)

포트 변경이 필요한 경우:
```bash
./gradlew :services:notification-service:bootRun --args='--server.port=9084'
```

### 4.3 환경 변수 목록

Notification Service가 사용하는 주요 환경 변수:

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `CONFIG_SERVER_URL` | Config Server URL | http://config-service:8888 |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 브로커 주소 | localhost:9092 |
| `KAFKA_GROUP_ID` | Kafka 컨슈머 그룹 ID | notification-group |
| `MAIL_HOST` | SMTP 서버 호스트 | smtp.gmail.com |
| `MAIL_PORT` | SMTP 서버 포트 | 587 |
| `MAIL_USERNAME` | SMTP 사용자 계정 | - |
| `MAIL_PASSWORD` | SMTP 비밀번호 | - |
| `MAIL_PROPERTIES_MAIL_SMTP_AUTH` | SMTP 인증 사용 | true |
| `MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | TLS 사용 | true |

**환경 변수 설정 예시:**
```bash
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export KAFKA_GROUP_ID=notification-group
export MAIL_HOST=smtp.gmail.com
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
```

## 5. Kafka 설정

### 5.1 로컬 Kafka 실행

**Docker Compose 사용:**
```bash
# 프로젝트 루트에서 실행
docker-compose up -d kafka
```

**Kafka 상태 확인:**
```bash
# Kafka 컨테이너 로그 확인
docker-compose logs -f kafka

# Kafka 토픽 목록 확인
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### 5.2 Kafka 토픽 생성

Notification Service가 구독하는 토픽을 미리 생성합니다:

```bash
# user-signup 토픽 생성
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 \
  --create --topic user-signup \
  --partitions 3 --replication-factor 1

# 토픽 확인
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 \
  --describe --topic user-signup
```

### 5.3 구독 토픽 목록

Notification Service가 구독하는 Kafka 토픽:

| 토픽 | 이벤트 | 처리 내용 |
|------|--------|----------|
| `user-signup` | 회원가입 | 환영 이메일 발송 |
| `order-created` | 주문 생성 | 주문 확인 알림 (예정) |
| `order-cancelled` | 주문 취소 | 취소 알림 (예정) |
| `delivery-status` | 배송 상태 변경 | 배송 알림 (예정) |

**현재 구현 상태:**
- `user-signup`: ✅ 구현됨 (로그 출력)
- 나머지 토픽: ⏳ 예정

## 6. 로컬 테스트 방법

### 6.1 헬스체크

Notification Service가 정상적으로 실행되었는지 확인합니다.

```bash
# 헬스체크
curl http://localhost:8084/actuator/health

# 예상 응답
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "kafka": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 6.2 Kafka 연결 확인

Kafka 컨슈머가 정상적으로 연결되었는지 확인합니다.

```bash
# 컨슈머 그룹 확인
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group notification-group --describe

# 예상 출력: notification-group에 속한 컨슈머와 lag 정보 표시
```

### 6.3 이벤트 발행 테스트

Kafka 토픽에 테스트 이벤트를 발행하여 컨슈머가 정상적으로 처리하는지 확인합니다.

**방법 1: Kafka Console Producer 사용**

```bash
# user-signup 토픽에 JSON 메시지 발행
docker exec -it kafka kafka-console-producer --bootstrap-server localhost:9092 \
  --topic user-signup \
  --property "parse.key=false"

# 아래 JSON을 입력하고 Enter
{"userId":"test-user-123","email":"test@example.com","name":"Test User"}
```

**방법 2: curl로 auth-service를 통해 회원가입**

```bash
# auth-service에 회원가입 요청 (auth-service가 Kafka에 이벤트 발행)
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "password123",
    "name": "New User"
  }'
```

**로그 확인:**

Notification Service 로그에서 다음과 같은 메시지를 확인합니다:

```
INFO  c.p.u.n.c.NotificationConsumer - Received user signup event: UserSignedUpEvent[userId=test-user-123, email=test@example.com, name=Test User]
INFO  c.p.u.n.c.NotificationConsumer - Sending welcome email to: Test User (test@example.com)
```

### 6.4 이메일 발송 테스트 (선택)

실제 이메일 발송을 테스트하려면 SMTP 설정이 필요합니다.

**Gmail SMTP 사용 예시:**

1. **앱 비밀번호 생성:**
   - Gmail 계정 → 보안 → 2단계 인증 활성화
   - 앱 비밀번호 생성 (https://myaccount.google.com/apppasswords)

2. **환경 변수 설정:**
```bash
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password
```

3. **Notification Service 재시작:**
```bash
./gradlew :services:notification-service:bootRun
```

4. **이벤트 발행 후 이메일 수신 확인**

**로컬 테스트용 Mailhog 사용:**

```bash
# Mailhog 실행 (SMTP 서버 + 웹 UI)
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog

# 환경 변수 설정
export MAIL_HOST=localhost
export MAIL_PORT=1025
export MAIL_USERNAME=
export MAIL_PASSWORD=

# 웹 UI에서 이메일 확인
open http://localhost:8025
```

## 7. Spring 프로필

Notification Service는 환경별로 다른 프로필을 지원합니다.

| 프로필 | 설명 | 사용 시점 |
|--------|------|-----------|
| `local` | 로컬 개발 환경 (기본값) | 개발자 PC에서 직접 실행 |
| `docker` | Docker Compose 환경 | docker-compose.yml 사용 |
| `k8s` | Kubernetes 환경 | 클러스터 배포 |

**프로필 활성화:**
```bash
# local 프로필
./gradlew :services:notification-service:bootRun --args='--spring.profiles.active=local'

# docker 프로필
./gradlew :services:notification-service:bootRun --args='--spring.profiles.active=docker'
```

## 8. 트러블슈팅

### 8.1 Config Server 연결 실패

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

### 8.2 Kafka 연결 실패

**증상:**
```
org.apache.kafka.common.errors.TimeoutException: Failed to update metadata after 60000 ms.
```

**해결 방법:**
1. Kafka가 실행 중인지 확인:
   ```bash
   docker-compose ps kafka
   ```

2. Kafka 브로커 주소 확인:
   ```bash
   export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
   ```

3. Kafka 로그 확인:
   ```bash
   docker-compose logs -f kafka
   ```

4. 네트워크 연결 테스트:
   ```bash
   telnet localhost 9092
   ```

### 8.3 토픽이 존재하지 않음

**증상:**
```
org.apache.kafka.common.errors.UnknownTopicOrPartitionException: This server does not host this topic-partition.
```

**해결 방법:**
1. 토픽 목록 확인:
   ```bash
   docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
   ```

2. 토픽이 없으면 생성:
   ```bash
   docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 \
     --create --topic user-signup --partitions 3 --replication-factor 1
   ```

3. Kafka auto-topic-creation 활성화 (선택):
   ```yaml
   # Config Server의 notification-service.yml
   spring:
     kafka:
       consumer:
         properties:
           allow.auto.create.topics: true
   ```

### 8.4 컨슈머 그룹 lag 증가

**증상:**
```
Kafka consumer lag이 계속 증가하여 메시지 처리가 지연됨
```

**해결 방법:**
1. 컨슈머 그룹 lag 확인:
   ```bash
   docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
     --group notification-group --describe
   ```

2. 컨슈머 처리 시간 확인:
   - 로그에서 메시지 처리 시간 측정
   - 병목 지점 식별

3. 컨슈머 스레드 수 증가:
   ```yaml
   spring:
     kafka:
       listener:
         concurrency: 3  # 기본값 1에서 증가
   ```

4. 파티션 수 증가:
   ```bash
   docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 \
     --alter --topic user-signup --partitions 5
   ```

### 8.5 이메일 발송 실패

**증상:**
```
org.springframework.mail.MailAuthenticationException: Authentication failed
```

**해결 방법:**
1. SMTP 자격 증명 확인:
   ```bash
   echo $MAIL_USERNAME
   echo $MAIL_PASSWORD
   ```

2. Gmail 앱 비밀번호 재생성:
   - Gmail 2단계 인증 활성화
   - 앱 비밀번호 생성 (16자리)

3. SMTP 서버 연결 테스트:
   ```bash
   telnet smtp.gmail.com 587
   ```

4. 로컬 테스트용 Mailhog 사용:
   ```bash
   docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
   export MAIL_HOST=localhost
   export MAIL_PORT=1025
   ```

### 8.6 메모리 부족

**증상:**
```
OutOfMemoryError: Java heap space
```

**해결 방법:**
1. JVM 힙 메모리 증가:
   ```bash
   export JAVA_OPTS="-Xmx512m -Xms256m"
   ./gradlew :services:notification-service:bootRun
   ```

2. Gradle 데몬 메모리 설정 (gradle.properties):
   ```properties
   org.gradle.jvmargs=-Xmx2048m
   ```

### 8.7 이벤트 역직렬화 실패

**증상:**
```
org.springframework.kafka.support.serializer.DeserializationException: Failed to deserialize
```

**해결 방법:**
1. 이벤트 스키마 확인:
   - common-library의 `UserSignedUpEvent` 클래스와 Kafka 메시지 형식이 일치하는지 확인

2. JSON 형식 검증:
   ```bash
   # 올바른 형식
   {"userId":"123","email":"user@example.com","name":"User Name"}
   ```

3. 역직렬화 로깅 활성화:
   ```yaml
   logging:
     level:
       org.springframework.kafka: DEBUG
   ```

## 9. 개발 팁

### 9.1 핫 리로드

Spring Boot DevTools를 사용하여 코드 변경 시 자동 재시작:

```gradle
// build.gradle
dependencies {
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
}
```

### 9.2 로깅 설정

디버깅을 위한 로그 레벨 조정:

```yaml
logging:
  level:
    com.portal.universe.notificationservice: DEBUG
    org.springframework.kafka: DEBUG
    org.springframework.mail: DEBUG
```

### 9.3 Actuator 엔드포인트

유용한 모니터링 엔드포인트:

```bash
# 헬스 체크
curl http://localhost:8084/actuator/health

# 메트릭 정보
curl http://localhost:8084/actuator/metrics

# Kafka 컨슈머 정보
curl http://localhost:8084/actuator/metrics/kafka.consumer.fetch.manager.records.consumed.total
```

### 9.4 Kafka 토픽 모니터링

Kafka 토픽의 메시지를 실시간으로 확인:

```bash
# user-signup 토픽 메시지 구독
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic user-signup --from-beginning

# 특정 파티션만 구독
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic user-signup --partition 0 --from-beginning
```

### 9.5 IDE 설정

**IntelliJ IDEA:**
1. `File` → `Project Structure` → `Project SDK`: Java 17 선택
2. `Run` → `Edit Configurations` → `+` → `Spring Boot`
3. Main class: `com.portal.universe.notificationservice.NotificationServiceApplication`
4. Environment variables:
   ```
   CONFIG_SERVER_URL=http://localhost:8888;
   KAFKA_BOOTSTRAP_SERVERS=localhost:9092;
   MAIL_HOST=localhost;
   MAIL_PORT=1025
   ```

## 10. 다음 단계

- [Notification Service 아키텍처](../../architecture/notification-service/README.md)
- [Kafka 설정 가이드](./kafka-configuration.md)
- [이메일 템플릿 가이드](./email-templates.md)
- [알림 발송 전략](./notification-strategy.md)

## 참고 자료

- [Spring for Apache Kafka 공식 문서](https://spring.io/projects/spring-kafka)
- [Spring Boot Mail 문서](https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email)
- [Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Mailhog GitHub](https://github.com/mailhog/MailHog)
