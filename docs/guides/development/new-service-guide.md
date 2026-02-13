---
id: guide-new-service
title: 신규 마이크로서비스 추가 가이드
type: guide
status: current
created: 2026-02-11
updated: 2026-02-13
author: Laze
tags: [guide, microservice, backend, frontend, setup, opentelemetry]
related:
  - guide-module-federation
  - arch-system-overview
  - adr-033-polyglot-observability-strategy
---

# 신규 마이크로서비스 추가 가이드

## 1. 개요

이 가이드는 Portal Universe 프로젝트에 새로운 마이크로서비스(Backend + Frontend)를 추가하는 전체 과정을 다룹니다.

### 적용 범위

- **Backend**: Spring Boot 3.5.5 기반 Java 서비스
- **Frontend**: Vue 3 또는 React 18 기반 Module Federation Remote
- **Infra**: MySQL/PostgreSQL, Kafka, Redis, Docker, Kubernetes

### 사전 요구사항

- Java 17+, Node.js 18+
- Docker 인프라 실행 중 (`docker compose -f docker-compose-local.yml up -d`)
- 프로젝트 루트에서 작업

---

## 2. Part A: Backend 서비스 생성

### 2.1 Gradle 설정

#### settings.gradle 수정

프로젝트 루트의 `settings.gradle`에 새 서비스 추가:

```gradle
include 'services:drive-service'
include 'services:drive-events'
```

**규칙**:
- 서비스 본체: `services:{service-name}-service`
- Kafka 이벤트 모듈: `services:{service-name}-events`

#### build.gradle 생성

`services/drive-service/build.gradle` 생성:

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.5'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.portal.universe'
version = '0.0.1-SNAPSHOT'
description = 'drive-service'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

ext {
    set('springCloudVersion', "2025.0.0")
}

dependencies {
    // Internal Libraries
    implementation project(':services:common-library')
    implementation project(':services:drive-events')

    // Web & Data
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    // DB 드라이버 (서비스에 맞게 선택)
    runtimeOnly 'org.postgresql:postgresql'          // PostgreSQL
    // runtimeOnly 'com.mysql:mysql-connector-j'     // MySQL

    // Database Migration
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'  // PostgreSQL
    // implementation 'org.flywaydb:flyway-mysql'              // MySQL

    // Messaging (Kafka)
    implementation 'org.springframework.kafka:spring-kafka'

    // Security
    implementation 'org.springframework.boot:spring-boot-starter-security'

    // Observability (Monitoring & Tracing) - OpenTelemetry (ADR-033)
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'
    implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'

    // Development Tools
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Document
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'  // 또는 mysql
    testImplementation 'org.testcontainers:kafka'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}
```

#### 빌드 검증

```bash
./gradlew services:drive-service:build -x test
```

---

### 2.2 Application 클래스

`services/drive-service/src/main/java/com/portal/universe/driveservice/DriveServiceApplication.java` 생성:

```java
package com.portal.universe.driveservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.portal.universe.driveservice",
    "com.portal.universe.commonlibrary"  // 공통 라이브러리 스캔 필수
})
public class DriveServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriveServiceApplication.class, args);
    }
}
```

**중요**: `@ComponentScan`에 반드시 `common-library` 패키지 포함.

---

### 2.3 Application 설정 (3+1 Profiles)

#### application.yml (기본)

`services/drive-service/src/main/resources/application.yml`:

```yaml
# ===================================================================
# Drive Service - Default Configuration
# ===================================================================

server:
  port: 8087

spring:
  application:
    name: drive-service

  jpa:
    open-in-view: false
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true

  data:
    web:
      pageable:
        one-indexed-parameters: true

  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB

# Swagger/OpenAPI
springdoc:
  api-docs:
    path: /api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    tags-sorter: alpha
    operations-sorter: alpha
    display-request-duration: true
    doc-expansion: none
  show-actuator: false
  default-consumes-media-type: application/json
  default-produces-media-type: application/json

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized
      roles: ACTUATOR_ADMIN
    info:
      enabled: true
    prometheus:
      enabled: true
    metrics:
      enabled: true
  server:
    port: 8087
```

#### application-local.yml

```yaml
# ===================================================================
# Drive Service - Local Development Profile
# ===================================================================

server:
  forward-headers-strategy: framework

spring:
  datasource:
    # PostgreSQL 예시 (MySQL은 jdbc:mysql://localhost:3307/drive_db 형태)
    url: jdbc:postgresql://localhost:5432/drive
    username: laze
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver

  flyway:
    baseline-on-migrate: true
    baseline-version: 1

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  kafka:
    bootstrap-servers: localhost:9092

# Swagger (Local: enabled)
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true

# Local Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,env,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}
      environment: local
  prometheus:
    metrics:
      export:
        enabled: true
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

logging:
  level:
    root: INFO
```

#### application-docker.yml

```yaml
# ===================================================================
# Drive Service - Docker Profile
# ===================================================================

spring:
  datasource:
    url: jdbc:postgresql://postgresql:5432/drive
    username: laze
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver

  kafka:
    bootstrap-servers: kafka:29092

  jpa:
    show-sql: false
    database-platform: org.hibernate.dialect.PostgreSQLDialect

# Swagger (Docker: disabled)
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

management:
  metrics:
    tags:
      application: ${spring.application.name}
      environment: docker
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

#### application-kubernetes.yml

```yaml
# ===================================================================
# Drive Service - Kubernetes Profile
# ===================================================================

spring:
  datasource:
    url: jdbc:postgresql://postgresql:5432/drive
    username: laze
    password: ${DB_PASSWORD}

  kafka:
    bootstrap-servers: kafka-service:9092

  jpa:
    show-sql: false

# Swagger (K8s: disabled)
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

management:
  endpoint:
    health:
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: kubernetes
  tracing:
    sampling:
      probability: 0.1
  zipkin:
    tracing:
      endpoint: http://zipkin-service:9411/api/v2/spans
```

#### 실행 시 주의사항

```bash
# ⚠️ 필수: --spring.profiles.active=local 지정
./gradlew services:drive-service:bootRun --args='--spring.profiles.active=local'
```

**프로파일을 지정하지 않으면 DB 연결 실패**합니다.

---

### 2.4 SecurityConfig (3-Chain Pattern)

`services/drive-service/src/main/java/com/portal/universe/driveservice/common/config/SecurityConfig.java`:

```java
package com.portal.universe.driveservice.common.config;

import com.portal.universe.commonlibrary.security.filter.GatewayAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Chain 0: Actuator 엔드포인트
     * - health, info: 전체 공개 (Gateway가 외부 접근 차단)
     * - prometheus, metrics: Gateway 전용 (외부 접근 차단)
     */
    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/prometheus", "/actuator/metrics/**").permitAll()
                        .anyRequest().denyAll())
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Chain 1: Swagger 문서
     * - Local 환경에서만 활성화
     */
    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/api-docs",
                        "/api-docs/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**"
                )
                .authorizeHttpRequests(requests -> requests
                        .anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Chain 2: 비즈니스 API
     * - GatewayAuthenticationFilter로 JWT 검증
     * - STATELESS 세션
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/hello").permitAll()
                        // 비즈니스 로직 권한 규칙
                        .requestMatchers(HttpMethod.GET, "/files", "/files/**")
                            .hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/files/**")
                            .hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/files/**")
                            .hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/admin/**")
                            .hasAnyAuthority("ROLE_DRIVE_ADMIN", "ROLE_SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new GatewayAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
```

**핵심 패턴**:
- `@Order(0)`: Actuator → health/info 공개, prometheus는 Gateway만
- `@Order(1)`: Swagger → Local에서만 활성화
- `@Order(2)`: API → GatewayAuthenticationFilter + 비즈니스 권한

---

### 2.5 ErrorCode & HelloController

#### DriveErrorCode

`services/drive-service/src/main/java/com/portal/universe/driveservice/common/exception/DriveErrorCode.java`:

```java
package com.portal.universe.driveservice.common.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum DriveErrorCode implements ErrorCode {

    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "File not found"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "D002", "File upload failed"),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "D003", "File size exceeds limit"),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "D004", "File type not allowed"),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "D005", "File delete failed"),

    FOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "D010", "Folder not found"),
    FOLDER_ALREADY_EXISTS(HttpStatus.CONFLICT, "D011", "Folder already exists"),

    ACCESS_DENIED(HttpStatus.FORBIDDEN, "D020", "Access denied to this resource"),
    STORAGE_QUOTA_EXCEEDED(HttpStatus.BAD_REQUEST, "D030", "Storage quota exceeded");

    private final HttpStatus status;
    private final String code;
    private final String message;

    DriveErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
```

**규칙**:
- 서비스별 prefix (D = Drive, S = Shopping, B = Blog 등)
- `common-library`의 `ErrorCode` 인터페이스 구현

#### HelloController

`services/drive-service/src/main/java/com/portal/universe/driveservice/controller/HelloController.java`:

```java
package com.portal.universe.driveservice.controller;

import com.portal.universe.commonlibrary.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public ApiResponse<Map<String, String>> hello() {
        return ApiResponse.success(Map.of(
                "service", "drive-service",
                "status", "running"
        ));
    }
}
```

**검증**:

```bash
# 직접 호출
curl http://localhost:8087/hello

# Gateway 경유
curl http://localhost:8080/api/v1/drive/hello
```

---

### 2.6 Kafka Events 모듈

#### build.gradle

`services/drive-events/build.gradle`:

```gradle
plugins {
    id 'java-library'
}

group = 'com.portal.universe'
version = '0.0.1-SNAPSHOT'
description = 'drive-events'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // 최소 의존성
}
```

#### Events 정의

`services/drive-events/src/main/java/com/portal/universe/event/drive/DriveTopics.java`:

```java
package com.portal.universe.event.drive;

public class DriveTopics {
    public static final String FILE_UPLOADED = "drive.file.uploaded";
    public static final String FILE_DELETED = "drive.file.deleted";
    public static final String FOLDER_CREATED = "drive.folder.created";
}
```

`services/drive-events/src/main/java/com/portal/universe/event/drive/FileUploadedEvent.java`:

```java
package com.portal.universe.event.drive;

public record FileUploadedEvent(
    Long fileId,
    String fileName,
    String userId,
    Long size,
    String mimeType
) {}
```

#### 서비스에서 사용

`services/drive-service/src/main/java/com/portal/universe/driveservice/config/KafkaTopicConfig.java`:

```java
package com.portal.universe.driveservice.config;

import com.portal.universe.event.drive.DriveTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic fileUploadedTopic() {
        return TopicBuilder.name(DriveTopics.FILE_UPLOADED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fileDeletedTopic() {
        return TopicBuilder.name(DriveTopics.FILE_DELETED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
```

---

### 2.7 API Gateway 등록

`services/api-gateway/src/main/resources/application.yml` 수정:

#### Routes 추가

```yaml
spring:
  cloud:
    gateway:
      routes:
        # ========== Drive Service ==========
        # Drive Service Actuator - health, info
        - id: drive-service-actuator-health
          uri: ${services.drive.url:http://localhost:8087}
          predicates:
            - Path=/api/v1/drive/actuator/health,/api/v1/drive/actuator/info
          filters:
            - RewritePath=/api/v1/drive/actuator/(?<segment>.*), /actuator/${segment}
          order: 0

        # Drive Service API
        - id: drive-service-route
          uri: ${services.drive.url:http://localhost:8087}
          predicates:
            - Path=/api/v1/drive/**
          filters:
            - StripPrefix=3  # /api/v1/drive/files → /files
            - name: CircuitBreaker
              args:
                name: driveServiceCircuitBreaker
                fallbackUri: forward:/fallback/drive
          order: 3
```

#### CircuitBreaker 설정

```yaml
resilience4j:
  circuitbreaker:
    instances:
      driveServiceCircuitBreaker:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 5s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
  timelimiter:
    instances:
      driveServiceCircuitBreaker:
        timeoutDuration: 3s
```

#### Health Check 서비스 목록

```yaml
health-check:
  services:
    - name: drive-service
      display-name: Drive Service
      url: ${services.drive.url:http://localhost:8087}
      health-path: /actuator/health
      k8s-deployment-name: drive-service
```

#### 환경별 URL (각 profile에 추가)

**application-local.yml**:
```yaml
services:
  drive:
    url: http://localhost:8087
```

**application-docker.yml**:
```yaml
services:
  drive:
    url: http://drive-service:8087
```

**application-kubernetes.yml**:
```yaml
services:
  drive:
    url: http://drive-service:8087
```

---

### 2.8 Docker & Observability

#### Dockerfile

`services/drive-service/Dockerfile`:

```dockerfile
# ===================================================================
# Stage 1: Gradle Build
# ===================================================================
FROM gradle:8.12-jdk17 AS builder
WORKDIR /app

# Copy Gradle Wrapper & Project Files
COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .

# Copy Common Library & Events
COPY services/common-library services/common-library
COPY services/drive-events services/drive-events

# Copy Service Source
COPY services/drive-service services/drive-service

# Build (Test 제외)
RUN ./gradlew services:drive-service:build -x test --no-daemon

# ===================================================================
# Stage 2: Runtime
# ===================================================================
FROM eclipse-temurin:17-jre
WORKDIR /app

# Non-root User
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# 4-Layer JAR Extraction
COPY --from=builder /app/services/drive-service/build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Copy Layers
COPY --chown=appuser:appgroup --from=builder /app/services/drive-service/build/libs/dependencies/ ./
COPY --chown=appuser:appgroup --from=builder /app/services/drive-service/build/libs/spring-boot-loader/ ./
COPY --chown=appuser:appgroup --from=builder /app/services/drive-service/build/libs/snapshot-dependencies/ ./
COPY --chown=appuser:appgroup --from=builder /app/services/drive-service/build/libs/application/ ./

USER appuser

# Health Check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8087/actuator/health || exit 1

EXPOSE 8087

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

#### Logback 설정

`services/drive-service/src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <springProperty scope="context" name="springAppName" source="spring.application.name"/>

    <!-- Local: CONSOLE_TEXT + FILE_JSON -->
    <springProfile name="local">
        <appender name="CONSOLE_TEXT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>

        <appender name="FILE_JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/${springAppName}.json</file>
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <customFields>{"service":"${springAppName}"}</customFields>
            </encoder>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/${springAppName}-%d{yyyy-MM-dd}.json</fileNamePattern>
                <maxHistory>7</maxHistory>
            </rollingPolicy>
        </appender>

        <root level="INFO">
            <appender-ref ref="CONSOLE_TEXT"/>
            <appender-ref ref="FILE_JSON"/>
        </root>
    </springProfile>

    <!-- Docker/K8s: ASYNC_JSON + FILE_JSON -->
    <springProfile name="docker,kubernetes">
        <appender name="ASYNC_JSON" class="ch.qos.logback.classic.AsyncAppender">
            <queueSize>512</queueSize>
            <appender class="ch.qos.logback.core.ConsoleAppender">
                <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                    <customFields>{"service":"${springAppName}"}</customFields>
                    <includeMdcKeyName>traceId</includeMdcKeyName>
                    <includeMdcKeyName>spanId</includeMdcKeyName>
                </encoder>
            </appender>
        </appender>

        <appender name="FILE_JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/var/log/${springAppName}.json</file>
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <customFields>{"service":"${springAppName}"}</customFields>
            </encoder>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>/var/log/${springAppName}-%d{yyyy-MM-dd}.json</fileNamePattern>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
        </appender>

        <root level="INFO">
            <appender-ref ref="ASYNC_JSON"/>
            <appender-ref ref="FILE_JSON"/>
        </root>
    </springProfile>
</configuration>
```

**핵심**:
- `LogstashEncoder`로 JSON 구조화 로그
- traceId/spanId MDC 자동 포함 (OpenTelemetry → Zipkin 연동, ADR-033)
- Local: TEXT(콘솔) + JSON(파일)
- Docker/K8s: JSON(콘솔) + JSON(파일)

**Observability 통합**: OpenTelemetry SDK가 자동으로 traceId/spanId를 MDC에 주입하므로, 별도 설정 없이 로그-트레이스 연동이 가능합니다.

---

## 3. Part B: Frontend 서비스 생성

### 3.1 Workspace 등록

#### frontend/package.json

`workspaces` 배열에 추가:

```json
{
  "workspaces": [
    "design-tokens",
    "design-system-vue",
    "design-system-react",
    "portal-shell",
    "blog-frontend",
    "shopping-frontend",
    "prism-frontend",
    "drive-frontend",  // ← 추가
    "admin-frontend"
  ],
  "scripts": {
    "dev:drive": "npm run dev -w drive-frontend",
    "build:drive": "npm run build -w drive-frontend",
    "build:drive:dev": "npm run build:dev -w drive-frontend",
    "build:drive:docker": "npm run build:docker -w drive-frontend",
    "build:drive:k8s": "npm run build:k8s -w drive-frontend",
    "build:apps": "npm run build:shell && npm run build:blog && npm run build:shopping && npm run build:prism && npm run build:drive"
  }
}
```

#### drive-frontend/package.json

```json
{
  "name": "drive-frontend",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite --mode dev",
    "build": "vue-tsc -b && vite build --mode dev",
    "build:dev": "vue-tsc -b && vite build --mode dev",
    "build:docker": "vue-tsc -b && vite build --mode docker",
    "build:k8s": "vue-tsc -b && vite build --mode k8s",
    "preview": "vite preview",
    "type-check": "vue-tsc --noEmit -p tsconfig.app.json --composite false"
  },
  "dependencies": {
    "vue": "^3.5.13",
    "vue-router": "^4.5.0",
    "pinia": "^2.3.0",
    "axios": "^1.7.9",
    "@portal/design-system-vue": "*",
    "@portal/design-tokens": "*"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.2.1",
    "@originjs/vite-plugin-federation": "^1.3.6",
    "vite": "^6.0.5",
    "vue-tsc": "^2.1.10",
    "typescript": "~5.6.2",
    "tailwindcss": "^3.4.17",
    "postcss": "^8.4.49",
    "autoprefixer": "^10.4.20"
  }
}
```

---

### 3.2 Vite & Module Federation

#### vite.config.ts

`frontend/drive-frontend/vite.config.ts`:

```typescript
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import federation from '@originjs/vite-plugin-federation';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());

  return {
    plugins: [
      vue(),
      federation({
        name: 'drive',
        filename: 'remoteEntry.js',
        exposes: {
          './bootstrap': './src/bootstrap.ts',
        },
        shared: {
          vue: {
            requiredVersion: '^3.5.13',
            singleton: true,
          },
          pinia: {
            requiredVersion: '^2.3.0',
            singleton: true,
          },
          axios: {
            requiredVersion: '^1.7.9',
            singleton: true,
          },
        },
      }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 30005,
      strictPort: true,
      cors: true,
      headers: {
        'Access-Control-Allow-Origin': '*',
      },
    },
    build: {
      target: 'esnext',
      minify: mode === 'dev' ? false : 'esbuild',
      cssCodeSplit: false,
      rollupOptions: {
        output: {
          format: 'esm',
        },
      },
    },
    define: {
      'process.env': env,
    },
  };
});
```

**핵심**:
- `name: 'drive'`: Module Federation Remote 이름
- `exposes: { './bootstrap': './src/bootstrap.ts' }`: Host가 로드할 진입점
- `shared`: vue, pinia, axios 반드시 포함 (버전 통일)

#### tsconfig 구조

`tsconfig.json` (extends):

```json
{
  "files": [],
  "references": [
    { "path": "./tsconfig.app.json" },
    { "path": "./tsconfig.node.json" }
  ]
}
```

`tsconfig.app.json`:

```json
{
  "extends": "@vue/tsconfig/tsconfig.dom.json",
  "include": ["env.d.ts", "src/**/*", "src/**/*.vue"],
  "exclude": ["src/**/__tests__/*"],
  "compilerOptions": {
    "composite": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

#### 환경변수 파일

`.env.dev`:

```env
VITE_PROFILE=dev
VITE_API_BASE_URL=http://localhost:8080
```

`.env.docker`:

```env
VITE_PROFILE=docker
VITE_API_BASE_URL=https://portal-universe:8080
```

`.env.k8s`:

```env
VITE_PROFILE=k8s
VITE_API_BASE_URL=https://portal-universe/api
```

---

### 3.3 Bootstrap & Dual Mode

#### bootstrap.ts (Embedded/Standalone 지원)

`frontend/drive-frontend/src/bootstrap.ts`:

```typescript
import { createApp, App as VueApp } from 'vue';
import { createPinia } from 'pinia';
import { createMemoryHistory, Router } from 'vue-router';
import App from './App.vue';
import createRouter from './router';

export interface MountOptions {
  /**
   * Module Federation Memory History 기반 라우터 사용 (Embedded)
   * base는 Portal Shell이 제공
   */
  routerBase?: string;

  /**
   * Host로부터 전달받은 초기 경로 (예: /drive/folder/123)
   */
  initialRoute?: string;
}

export interface DriveAppInstance {
  app: VueApp;
  router: Router;
  unmount: () => void;
}

let appInstance: DriveAppInstance | null = null;

export function mountDriveApp(
  container: string | Element,
  options: MountOptions = {}
): DriveAppInstance {
  if (appInstance) {
    console.warn('[Drive] App already mounted. Unmounting previous instance.');
    appInstance.unmount();
  }

  const pinia = createPinia();

  // Memory History (Embedded Mode)
  const router = createRouter({
    history: createMemoryHistory(options.routerBase || '/drive'),
  });

  const app = createApp(App);
  app.use(pinia);
  app.use(router);

  // 초기 라우트 설정
  if (options.initialRoute) {
    router.push(options.initialRoute);
  }

  const el = typeof container === 'string' ? document.querySelector(container) : container;
  if (!el) {
    throw new Error(`[Drive] Mount container not found: ${container}`);
  }

  app.mount(el);

  appInstance = {
    app,
    router,
    unmount: () => {
      app.unmount();
      appInstance = null;
    },
  };

  return appInstance;
}
```

#### main.ts (Standalone Mode)

`frontend/drive-frontend/src/main.ts`:

```typescript
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { createWebHistory } from 'vue-router';
import App from './App.vue';
import createRouter from './router';
import './style.css';

// Portal Shell에 의해 실행 중인지 감지
const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true;

if (!isEmbedded) {
  console.log('[Drive] Running in Standalone Mode');

  const pinia = createPinia();

  // Web History (Standalone)
  const router = createRouter({
    history: createWebHistory('/drive'),
  });

  const app = createApp(App);
  app.use(pinia);
  app.use(router);
  app.mount('#app');
} else {
  console.log('[Drive] Running in Embedded Mode (MF)');
}
```

**Dual Mode 판별**:
- `window.__POWERED_BY_PORTAL_SHELL__`: Portal Shell이 설정
- Embedded → Memory History, Host가 `mountDriveApp()` 호출
- Standalone → Web History, `main.ts`가 직접 마운트

---

### 3.4 Router

#### router/index.ts

`frontend/drive-frontend/src/router/index.ts`:

```typescript
import { createRouter as _createRouter, createMemoryHistory, createWebHistory, RouterHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';

interface CreateRouterOptions {
  history: RouterHistory;
}

export default function createRouter(options?: CreateRouterOptions) {
  const history = options?.history || createWebHistory('/drive');

  const router = _createRouter({
    history,
    routes: [
      {
        path: '/',
        name: 'home',
        component: HomeView,
      },
      {
        path: '/folder/:folderId',
        name: 'folder',
        component: () => import('../views/FolderView.vue'),
      },
      {
        path: '/file/:fileId',
        name: 'file',
        component: () => import('../views/FileView.vue'),
      },
    ],
  });

  // Auth Guard (Embedded)
  const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true;

  if (isEmbedded) {
    router.beforeEach(async (to, from, next) => {
      try {
        const { useAuthStore } = await import('portal/stores');
        const authStore = useAuthStore();

        if (!authStore.isAuthenticated) {
          console.warn('[Drive] Not authenticated, redirecting to login');
          authStore.login();
          return;
        }

        next();
      } catch (error) {
        console.error('[Drive] Failed to load portal stores:', error);
        next();
      }
    });
  } else {
    // Standalone: Fallback auth check
    router.beforeEach((to, from, next) => {
      const token = localStorage.getItem('access_token');
      if (!token && to.path !== '/login') {
        console.warn('[Drive] Standalone: No token, redirect to /login');
        next('/login');
      } else {
        next();
      }
    });
  }

  return router;
}
```

**Router 전략**:
- Embedded: `import('portal/stores')` 동적 로드, Memory History
- Standalone: localStorage fallback, Web History

---

### 3.5 App.vue

#### Embedded + Standalone 지원

`frontend/drive-frontend/src/App.vue`:

```vue
<template>
  <div class="drive-app" :data-service="SERVICE_NAME">
    <!-- Standalone Header -->
    <header v-if="!isEmbedded" class="standalone-header">
      <h1>Drive Service</h1>
      <nav>
        <router-link to="/">Home</router-link>
      </nav>
    </header>

    <!-- Main Content -->
    <main class="drive-main">
      <router-view />
    </main>

    <!-- Standalone Footer -->
    <footer v-if="!isEmbedded" class="standalone-footer">
      <p>&copy; 2026 Drive Service</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onActivated, onDeactivated, ref } from 'vue';

const SERVICE_NAME = 'drive';
const isEmbedded = window.__POWERED_BY_PORTAL_SHELL__ === true;

// Theme Sync (Embedded)
const initThemeSync = () => {
  if (!isEmbedded) return;

  import('portal/stores')
    .then(({ useThemeStore }) => {
      const themeStore = useThemeStore();
      document.documentElement.classList.toggle('dark', themeStore.isDark);

      // Watch theme changes
      themeStore.$subscribe((mutation, state) => {
        document.documentElement.classList.toggle('dark', state.isDark);
      });
    })
    .catch((err) => console.error('[Drive] Failed to sync theme:', err));
};

// Lifecycle: Embedded
if (isEmbedded) {
  onActivated(() => {
    console.log('[Drive] Activated (keep-alive)');
    document.body.setAttribute('data-service', SERVICE_NAME);
  });

  onDeactivated(() => {
    console.log('[Drive] Deactivated (keep-alive)');
  });
}

// Lifecycle: Common
onMounted(() => {
  console.log('[Drive] Mounted');
  initThemeSync();
});
</script>

<style scoped>
.drive-app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.drive-main {
  flex: 1;
}

.standalone-header {
  background: var(--bg-primary);
  padding: 1rem;
  border-bottom: 1px solid var(--border-default);
}

.standalone-footer {
  background: var(--bg-secondary);
  padding: 1rem;
  text-align: center;
}
</style>
```

**핵심**:
- `data-service="drive"`: Tailwind CSS 서비스별 테마 적용
- `onActivated/onDeactivated`: Portal Shell의 `<keep-alive>` 대응
- Theme Sync: `portal/stores` 동적 로드로 Embedded 테마 동기화

---

### 3.6 Design System & Tailwind

#### tailwind.config.js

`frontend/drive-frontend/tailwind.config.js`:

```javascript
import designSystemPreset from '@portal/design-tokens/tailwind';

/** @type {import('tailwindcss').Config} */
export default {
  presets: [designSystemPreset],
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}',
    '../design-system-vue/src/**/*.{vue,js,ts}',
  ],
  theme: {
    extend: {},
  },
  plugins: [],
};
```

#### postcss.config.js

```javascript
export default {
  plugins: {
    'postcss-import': {},
    tailwindcss: {},
    autoprefixer: {},
  },
};
```

#### style.css

`frontend/drive-frontend/src/style.css`:

```css
/* Design System Import */
@import '@portal/design-system-vue/style.css';

/* Tailwind Directives */
@tailwind base;
@tailwind components;
@tailwind utilities;

/* Drive-specific Styles */
body {
  @apply bg-background text-foreground;
}
```

---

### 3.7 Portal Shell 통합

#### remoteRegistry.ts

`frontend/portal-shell/src/config/remoteRegistry.ts`:

```typescript
export type Environment = 'dev' | 'docker' | 'k8s';

export interface RemoteConfig {
  name: string;
  url: string;
  scope: string;
  module: string;
  routeBase: string;
  displayName: string;
}

const remotes: Record<Environment, RemoteConfig[]> = {
  dev: [
    {
      name: 'blog',
      url: 'http://localhost:30001/assets/remoteEntry.js',
      scope: 'blog',
      module: './bootstrap',
      routeBase: '/blog',
      displayName: 'Blog',
    },
    {
      name: 'shopping',
      url: 'http://localhost:30002/assets/remoteEntry.js',
      scope: 'shopping',
      module: './bootstrap',
      routeBase: '/shopping',
      displayName: 'Shopping',
    },
    {
      name: 'prism',
      url: 'http://localhost:30003/assets/remoteEntry.js',
      scope: 'prism',
      module: './bootstrap',
      routeBase: '/prism',
      displayName: 'Prism',
    },
    {
      name: 'drive',  // ← 추가
      url: 'http://localhost:30005/assets/remoteEntry.js',
      scope: 'drive',
      module: './bootstrap',
      routeBase: '/drive',
      displayName: 'Drive',
    },
  ],
  docker: [
    // ... 동일 구조로 docker URL
  ],
  k8s: [
    // ... 동일 구조로 k8s URL
  ],
};

export function getRemotes(env: Environment): RemoteConfig[] {
  return remotes[env] || remotes.dev;
}
```

#### vite.config.ts (portal-shell)

```typescript
export default defineConfig(({ mode }) => {
  // ... 기존 설정

  return {
    plugins: [
      vue(),
      federation({
        name: 'portal',
        remotes: {
          blog: env.VITE_BLOG_REMOTE_URL,
          shopping: env.VITE_SHOPPING_REMOTE_URL,
          prism: env.VITE_PRISM_REMOTE_URL,
          drive: env.VITE_DRIVE_REMOTE_URL,  // ← 추가
        },
        exposes: {
          './api': './src/api/index.ts',
          './stores': './src/stores/index.ts',
        },
        shared: {
          vue: { singleton: true },
          pinia: { singleton: true },
          axios: { singleton: true },
          'vue-router': { singleton: true },
        },
      }),
    ],
  };
});
```

#### .env.dev (portal-shell)

```env
VITE_BLOG_REMOTE_URL=http://localhost:30001/assets/remoteEntry.js
VITE_SHOPPING_REMOTE_URL=http://localhost:30002/assets/remoteEntry.js
VITE_PRISM_REMOTE_URL=http://localhost:30003/assets/remoteEntry.js
VITE_DRIVE_REMOTE_URL=http://localhost:30005/assets/remoteEntry.js  # ← 추가
VITE_ADMIN_REMOTE_URL=http://localhost:30004/assets/remoteEntry.js
```

---

## 4. 검증 체크리스트

### Backend

- [ ] **빌드 성공**: `./gradlew services:drive-service:build -x test`
- [ ] **서비스 실행**: `./gradlew services:drive-service:bootRun --args='--spring.profiles.active=local'`
- [ ] **Health Check**: `curl http://localhost:8087/actuator/health` → `{"status":"UP"}`
- [ ] **Direct API**: `curl http://localhost:8087/hello` → `{"success":true, "data":{"service":"drive-service"}}`
- [ ] **Gateway Proxy**: `curl http://localhost:8080/api/v1/drive/hello` → 동일 응답
- [ ] **Swagger**: http://localhost:8087/swagger-ui.html 접속

### Frontend

- [ ] **빌드 성공**: `npm run build:drive` (frontend/ 디렉토리)
- [ ] **Standalone 실행**: `npm run dev:drive` → http://localhost:30005 접속
- [ ] **Embedded 실행**: Portal Shell 실행 후 http://localhost:30000/drive 접속
- [ ] **Module Federation**: Portal Shell에서 Drive 메뉴 클릭 → 페이지 전환
- [ ] **Theme Sync**: Portal Shell에서 Dark Mode 전환 → Drive도 동기화

### Integration

- [ ] **Auth Flow**: Portal Shell 로그인 → Drive 접속 → 인증 유지
- [ ] **Router**: `/drive/folder/123` → FolderView 렌더링
- [ ] **API Call**: Drive에서 `/api/v1/drive/files` 호출 → Gateway 경유 확인

---

## 5. 설정값 요약 테이블

| 항목 | 값 |
|------|-----|
| **Backend Port** | 8087 |
| **Frontend Port** | 30005 |
| **DB Name** | drive (PostgreSQL) |
| **Package** | com.portal.universe.driveservice |
| **MF Name** | drive |
| **MF Scope** | drive |
| **Gateway Path** | /api/v1/drive/** |
| **Router Base** | /drive |
| **Profile** | local / docker / kubernetes |
| **Kafka Topic Prefix** | drive.* |
| **ErrorCode Prefix** | D (D001, D002, ...) |

---

## 6. 추가 고려사항

### 6.1 Database 마이그레이션

Flyway 스크립트는 `services/drive-service/src/main/resources/db/migration/` 에 위치:

```
V1__create_files_table.sql
V2__create_folders_table.sql
```

### 6.2 Kafka Consumer

다른 서비스에서 Drive 이벤트를 구독하려면:

```java
@KafkaListener(topics = DriveTopics.FILE_UPLOADED, groupId = "notification-service")
public void handleFileUploaded(FileUploadedEvent event) {
    log.info("File uploaded: {}", event.fileName());
}
```

### 6.3 Service-to-Service 호출

Drive → Auth 토큰 검증:

```java
@FeignClient(name = "auth-service", url = "${services.auth.url}")
public interface AuthServiceClient {
    @GetMapping("/validate")
    ValidateResponse validateToken(@RequestHeader("Authorization") String token);
}
```

### 6.4 E2E 테스트

`e2e-tests/tests/drive/basic.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';

test('drive homepage', async ({ page }) => {
  await page.goto('http://localhost:30000/drive');
  await expect(page.locator('h1')).toContainText('Drive');
});
```

---

## 7. 트러블슈팅

| 문제 | 원인 | 해결 |
|------|------|------|
| DB 연결 실패 | Profile 미지정 | `--spring.profiles.active=local` 추가 |
| MF 로드 실패 | shared 누락 | vite.config.ts에서 vue, pinia, axios shared 확인 |
| 401 Unauthorized | GatewayAuthenticationFilter 미적용 | SecurityConfig Chain 2에 filter 추가 확인 |
| Theme 미동기화 | portal/stores import 실패 | Network 탭에서 portal/stores 로드 확인 |
| CircuitBreaker 미동작 | Gateway routes order 문제 | order 값 확인 (0 < 3) |

---

## 8. 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-11 | 최초 작성 (drive-service/drive-frontend 기반) | Laze |
| 2026-02-13 | Observability 의존성 변경 (Brave → OpenTelemetry, ADR-033) | Laze |
