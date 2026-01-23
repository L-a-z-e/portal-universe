# 12-Factor App

## 개요

**12-Factor App**은 2011년 Heroku의 공동 창립자인 Adam Wiggins가 제안한 클라우드 네이티브 애플리케이션 개발을 위한 방법론입니다. 수백만 개의 앱을 배포하고 운영한 경험을 바탕으로, SaaS(Software as a Service) 애플리케이션을 구축하기 위한 12가지 원칙을 정의했습니다.

### 왜 12-Factor App인가?

현대의 소프트웨어는 일반적으로 서비스로 제공되며, 다음과 같은 특성이 요구됩니다:

| 요구사항 | 설명 |
|----------|------|
| **이식성** | 다양한 환경(개발, 스테이징, 프로덕션)에서 일관되게 동작 |
| **확장성** | 수평적 확장(Scale-out)이 용이 |
| **탄력성** | 장애 발생 시 빠른 복구 |
| **연속 배포** | CI/CD 파이프라인과의 원활한 통합 |
| **운영 효율성** | 운영 환경과 개발 환경의 차이 최소화 |

12-Factor 방법론은 이러한 요구사항을 충족하는 애플리케이션을 설계하기 위한 가이드라인을 제공합니다.

---

## 12가지 요소 상세 설명

### I. Codebase (코드베이스)

> **버전 관리되는 하나의 코드베이스, 다수의 배포**

하나의 앱은 하나의 코드 저장소(repository)를 가지며, 이 코드베이스로부터 여러 환경(개발, 스테이징, 프로덕션)에 배포합니다.

```
┌─────────────────────────────────────────────────────────────┐
│                    Git Repository                           │
│                  (Single Codebase)                          │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
   ┌─────────┐          ┌─────────┐          ┌─────────┐
   │   Dev   │          │ Staging │          │  Prod   │
   │  Deploy │          │  Deploy │          │  Deploy │
   └─────────┘          └─────────┘          └─────────┘
```

**Portal Universe 적용:**
```bash
# 단일 monorepo 구조
portal-universe/
├── services/           # 백엔드 마이크로서비스
│   ├── auth-service/
│   ├── blog-service/
│   ├── shopping-service/
│   └── notification-service/
├── frontend/           # 프론트엔드 앱
└── k8s/               # Kubernetes 매니페스트
```

각 서비스는 동일한 코드베이스에서 `local`, `docker`, `kubernetes` 환경으로 배포됩니다.

---

### II. Dependencies (의존성)

> **명시적으로 선언하고 격리하라**

모든 의존성은 명시적으로 선언되어야 하며, 시스템에 미리 설치된 패키지에 의존하면 안 됩니다.

**Portal Universe 적용 (Gradle):**
```groovy
// services/auth-service/build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.kafka:spring-kafka'

    runtimeOnly 'com.mysql:mysql-connector-j'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

**Node.js (Frontend):**
```json
// frontend/portal-shell/package.json
{
  "dependencies": {
    "vue": "^3.4.0",
    "pinia": "^2.1.0",
    "@vueuse/core": "^10.0.0"
  }
}
```

---

### III. Config (설정)

> **설정을 환경에 저장하라**

코드와 설정을 엄격히 분리합니다. 설정은 환경 변수를 통해 주입되어야 하며, 코드에 하드코딩되어서는 안 됩니다.

**Portal Universe 적용:**

**1. Spring Profiles를 통한 환경 분리:**
```yaml
# application.yml (공통 설정)
spring:
  application:
    name: auth-service
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

jwt:
  secret-key: ${JWT_SECRET_KEY:default-dev-secret-key}
```

```yaml
# application-kubernetes.yml (Kubernetes 환경)
spring:
  datasource:
    url: jdbc:mysql://mysql-db:3306/auth_db
    username: ${DB_USER:laze}
    password: ${DB_PASSWORD}
  kafka:
    bootstrap-servers: kafka:29092
```

**2. Kubernetes ConfigMap:**
```yaml
# k8s/infrastructure/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: portal-universe-config
  namespace: portal-universe
data:
  SPRING_PROFILES_ACTIVE: "kubernetes"
  KAFKA_BOOTSTRAP_SERVERS: "kafka:29092"
  REDIS_HOST: "redis"
  REDIS_PORT: "6379"
  MYSQL_HOST: "mysql-db"
```

**3. Kubernetes Secret (민감 정보):**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: portal-universe-secret
type: Opaque
data:
  MYSQL_PASSWORD: <base64-encoded>
  JWT_SECRET_KEY: <base64-encoded>
```

**4. 환경 변수 주입:**
```yaml
# k8s/services/auth-service.yaml
spec:
  containers:
    - name: auth-service
      envFrom:
        - configMapRef:
            name: portal-universe-config
      env:
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: portal-universe-secret
              key: MYSQL_PASSWORD
```

---

### IV. Backing Services (지원 서비스)

> **지원 서비스를 연결된 리소스로 취급하라**

데이터베이스, 메시지 큐, 캐시 등의 지원 서비스는 교체 가능한 리소스로 취급합니다. 로컬 서비스와 서드파티 서비스를 구분하지 않습니다.

```
┌─────────────────────────────────────────────────────────────┐
│                    Application                              │
└──────┬──────────┬──────────┬──────────┬──────────┬─────────┘
       │          │          │          │          │
       ▼          ▼          ▼          ▼          ▼
   ┌───────┐  ┌───────┐  ┌───────┐  ┌───────┐  ┌───────┐
   │ MySQL │  │MongoDB│  │ Redis │  │ Kafka │  │  S3   │
   └───────┘  └───────┘  └───────┘  └───────┘  └───────┘
     Auth      Blog       Cache     Events     Storage
```

**Portal Universe의 Backing Services:**

| 서비스 | 용도 | 사용 서비스 |
|--------|------|-------------|
| **MySQL** | 관계형 데이터 | auth-service, shopping-service, notification-service |
| **MongoDB** | 문서형 데이터 | blog-service |
| **Redis** | 캐시, 세션 | auth-service, shopping-service |
| **Kafka** | 이벤트 스트리밍 | 전체 서비스 간 비동기 통신 |
| **Elasticsearch** | 검색 엔진 | shopping-service (상품 검색) |
| **LocalStack (S3)** | 파일 스토리지 | blog-service (이미지) |

**연결 URL 관리 (환경별 분리):**
```yaml
# Docker 환경
spring:
  datasource:
    url: jdbc:mysql://mysql-db:3306/auth_db

# Kubernetes 환경
spring:
  datasource:
    url: jdbc:mysql://mysql-db:3306/auth_db
```

---

### V. Build, Release, Run (빌드, 릴리스, 실행)

> **빌드와 실행 단계를 엄격히 분리하라**

```
┌─────────┐      ┌─────────┐      ┌─────────┐
│  Build  │ ──▶  │ Release │ ──▶  │   Run   │
└─────────┘      └─────────┘      └─────────┘
   Code +          Build +          Execute
   Dependencies    Config           Release
```

**Portal Universe 적용 (Multi-stage Dockerfile):**
```dockerfile
# services/auth-service/Dockerfile

# =================================================================
# Stage 1: Build Stage
# =================================================================
FROM gradle:8.9-jdk17 AS builder

WORKDIR /app

# 의존성 캐싱을 위한 레이어 분리
COPY build.gradle settings.gradle ./
COPY gradlew ./gradlew
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드
COPY . .
RUN ./gradlew :services:auth-service:build --no-daemon -x test

# =================================================================
# Stage 2: Runtime Stage
# =================================================================
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app
COPY --from=builder /app/services/auth-service/build/libs/auth-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

**CI/CD 파이프라인에서의 분리:**
```yaml
# GitHub Actions 예시
jobs:
  build:   # Build Stage
    steps:
      - run: ./gradlew build
      - run: docker build -t app:${{ github.sha }} .

  release: # Release Stage
    steps:
      - run: docker tag app:${{ github.sha }} registry/app:v1.0.0
      - run: docker push registry/app:v1.0.0

  deploy:  # Run Stage
    steps:
      - run: kubectl apply -f k8s/
```

---

### VI. Processes (프로세스)

> **애플리케이션을 하나 이상의 무상태(Stateless) 프로세스로 실행하라**

프로세스는 무상태(Stateless)이며 아무것도 공유하지 않습니다(Share-nothing). 영속적인 데이터는 Backing Service에 저장합니다.

**Stateless 설계:**
```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        // 세션 상태를 Redis에 저장 (프로세스 내부 X)
        // 모든 요청은 독립적으로 처리
        return ResponseEntity.ok(
            ApiResponse.success(userService.findById(id))
        );
    }
}
```

**Session 외부화 (Redis):**
```yaml
spring:
  session:
    store-type: redis
  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: ${REDIS_PORT:6379}
```

---

### VII. Port Binding (포트 바인딩)

> **포트 바인딩을 통해 서비스를 공개하라**

앱은 자체적으로 HTTP 서버를 내장하여, 특정 포트에 바인딩함으로써 서비스를 공개합니다.

**Portal Universe 서비스 포트:**
```yaml
# 각 서비스별 포트 할당
services:
  api-gateway:      8080
  auth-service:     8081
  blog-service:     8082
  shopping-service: 8083
  notification:     8084

# Frontend 포트
frontend:
  portal-shell:     30000
  blog-frontend:    30001
  shopping-frontend: 30002
```

**Spring Boot 설정:**
```yaml
server:
  port: 8081
```

**Kubernetes Service:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: auth-service
spec:
  type: ClusterIP
  ports:
    - port: 8081
      targetPort: 8081
      protocol: TCP
  selector:
    app: auth-service
```

---

### VIII. Concurrency (동시성)

> **프로세스 모델을 통한 수평 확장**

앱은 프로세스를 복제하여 수평적으로 확장합니다. 각 프로세스 타입(web, worker 등)은 독립적으로 확장 가능해야 합니다.

```
              Load Balancer
                    │
    ┌───────────────┼───────────────┐
    │               │               │
    ▼               ▼               ▼
┌───────┐      ┌───────┐      ┌───────┐
│ Pod 1 │      │ Pod 2 │      │ Pod 3 │
│ Auth  │      │ Auth  │      │ Auth  │
└───────┘      └───────┘      └───────┘
```

**Kubernetes HPA (Horizontal Pod Autoscaler):**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: auth-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: auth-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

**수동 확장:**
```bash
kubectl scale deployment auth-service --replicas=5
```

---

### IX. Disposability (폐기 가능성)

> **빠른 시작과 Graceful Shutdown으로 견고성 극대화**

프로세스는 빠르게 시작하고 종료되어야 합니다. SIGTERM 신호를 받으면 현재 요청을 완료하고 우아하게(Gracefully) 종료해야 합니다.

**Spring Boot Graceful Shutdown:**
```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

**Kubernetes Probes:**
```yaml
spec:
  containers:
    - name: auth-service
      # Startup Probe: 시작 완료 확인
      startupProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8081
        initialDelaySeconds: 30
        periodSeconds: 10
        failureThreshold: 18

      # Liveness Probe: 생존 확인
      livenessProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8081
        periodSeconds: 10
        failureThreshold: 3

      # Readiness Probe: 트래픽 수신 준비 확인
      readinessProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8081
        periodSeconds: 5
        failureThreshold: 3
```

**PreStop Hook (종료 전 처리):**
```yaml
lifecycle:
  preStop:
    exec:
      command: ["/bin/sh", "-c", "sleep 10"]
```

---

### X. Dev/Prod Parity (개발/프로덕션 환경 일치)

> **개발, 스테이징, 프로덕션 환경을 최대한 비슷하게 유지하라**

| Gap | 전통적 앱 | 12-Factor 앱 |
|-----|-----------|--------------|
| **시간** | 개발~배포까지 수 주 | 수 시간 ~ 수 분 |
| **인력** | 개발자와 운영자 분리 | 개발자가 배포까지 담당 |
| **도구** | 개발은 SQLite, 운영은 PostgreSQL | 동일한 Backing Service |

**Portal Universe의 환경 일치:**

```yaml
# Docker Compose (Local)
services:
  mysql-db:
    image: mysql:8.0
  redis:
    image: redis:7-alpine
  kafka:
    image: apache/kafka:4.1.0

# Kubernetes (Production-like)
# 동일한 이미지 버전 사용
```

**동일한 Spring Profile 구조:**
```
application.yml           # 공통 설정
application-local.yml     # 로컬 개발
application-docker.yml    # Docker Compose
application-kubernetes.yml # Kubernetes
```

---

### XI. Logs (로그)

> **로그를 이벤트 스트림으로 취급하라**

앱은 로그 라우팅에 관여하지 않습니다. 로그는 stdout/stderr로 출력하고, 실행 환경이 로그를 수집하고 집계합니다.

**Spring Boot 로그 설정:**
```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
  level:
    root: INFO
    com.portal.universe: DEBUG
```

**Docker Compose에서 로그 수집 (Promtail + Loki):**
```yaml
# docker-compose.yml
promtail:
  image: grafana/promtail:2.9.0
  volumes:
    - ./monitoring/promtail/promtail-config.yml:/etc/promtail/config.yml
    - /var/lib/docker/containers:/var/lib/docker/containers:ro
    - /var/run/docker.sock:/var/run/docker.sock
  depends_on:
    - loki

loki:
  image: grafana/loki:2.9.0
  ports:
    - "3100:3100"
```

**Kubernetes 환경 (stdout 기반):**
```bash
# Pod 로그 확인
kubectl logs -f auth-service-xxxxx

# 모든 Pod 로그 스트리밍
kubectl logs -f -l app=auth-service --all-containers
```

**로그 집계 아키텍처:**
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   App Pod   │────▶│   Promtail  │────▶│    Loki     │
│  (stdout)   │     │  (Collector)│     │   (Store)   │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │   Grafana   │
                                        │ (Dashboard) │
                                        └─────────────┘
```

---

### XII. Admin Processes (관리 프로세스)

> **관리/유지보수 작업을 일회성 프로세스로 실행하라**

데이터베이스 마이그레이션, 일회성 스크립트 등은 별도의 일회성 프로세스로 실행합니다.

**Kubernetes Job (마이그레이션):**
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: db-migration
spec:
  template:
    spec:
      containers:
        - name: migration
          image: portal-universe-auth-service:v1.0.0
          command: ["java", "-jar", "app.jar", "--spring.batch.job.names=migration"]
      restartPolicy: Never
  backoffLimit: 3
```

**일회성 명령 실행:**
```bash
# Pod 내에서 명령 실행
kubectl exec -it auth-service-xxxxx -- /bin/sh

# 일회성 Pod 실행
kubectl run debug --rm -it --image=mysql:8.0 -- mysql -h mysql-db -u laze -p
```

**Flyway/Liquibase 마이그레이션:**
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

---

## 클라우드 네이티브와의 관계

12-Factor App은 클라우드 네이티브 아키텍처의 핵심 원칙과 밀접하게 연관됩니다.

### CNCF Cloud Native 정의와의 매핑

| Cloud Native 특성 | 관련 12-Factor |
|-------------------|----------------|
| **컨테이너화** | V. Build/Release/Run, VI. Processes |
| **마이크로서비스** | I. Codebase, IV. Backing Services |
| **동적 오케스트레이션** | VIII. Concurrency, IX. Disposability |
| **DevOps** | X. Dev/Prod Parity, V. Build/Release/Run |
| **CI/CD** | V. Build/Release/Run |

### 확장된 원칙들 (Beyond 12-Factor)

Kevin Hoffman의 "Beyond the Twelve-Factor App"에서는 추가 원칙을 제안합니다:

| 번호 | 원칙 | 설명 |
|------|------|------|
| XIII | **API First** | API 설계를 우선시 |
| XIV | **Telemetry** | 관찰 가능성 (Metrics, Logs, Traces) |
| XV | **Authentication/Authorization** | 보안을 기본으로 |

**Portal Universe의 Telemetry 구현:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

---

## Portal Universe 12-Factor 체크리스트

| # | Factor | 적용 상태 | 구현 방식 |
|---|--------|:---------:|-----------|
| I | Codebase | ✅ | Git monorepo |
| II | Dependencies | ✅ | Gradle, npm/pnpm |
| III | Config | ✅ | Spring Profiles, ConfigMap, Secret |
| IV | Backing Services | ✅ | MySQL, MongoDB, Redis, Kafka, ES |
| V | Build/Release/Run | ✅ | Multi-stage Dockerfile |
| VI | Processes | ✅ | Stateless, Redis Session |
| VII | Port Binding | ✅ | 각 서비스별 포트 할당 |
| VIII | Concurrency | ✅ | Kubernetes HPA |
| IX | Disposability | ✅ | Graceful Shutdown, Probes |
| X | Dev/Prod Parity | ✅ | Docker Compose / K8s 동일 구성 |
| XI | Logs | ✅ | stdout + Loki/Promtail |
| XII | Admin Processes | ✅ | Kubernetes Job |

---

## 실습: 12-Factor 준수 검증

### 1. Config 분리 확인
```bash
# 환경 변수로 설정 오버라이드 테스트
docker run -e SPRING_PROFILES_ACTIVE=kubernetes \
           -e DB_PASSWORD=secret \
           portal-universe-auth-service:v1.0.0
```

### 2. Disposability 테스트
```bash
# Graceful Shutdown 동작 확인
kubectl delete pod auth-service-xxxxx

# 로그에서 shutdown 메시지 확인
# "Graceful shutdown complete"
```

### 3. Logs 스트리밍 확인
```bash
# 실시간 로그 확인
kubectl logs -f deployment/auth-service

# Grafana Loki에서 로그 쿼리
# {app="auth-service"} |= "error"
```

### 4. Concurrency 테스트
```bash
# Pod 수 증가
kubectl scale deployment auth-service --replicas=3

# 로드 테스트
hey -n 1000 -c 50 http://localhost:8080/api/v1/health
```

---

## 참고 자료

- [12factor.net](https://12factor.net/) - 공식 문서
- [Beyond the Twelve-Factor App](https://www.oreilly.com/library/view/beyond-the-twelve-factor/9781492042631/) - Kevin Hoffman
- [CNCF Cloud Native Definition](https://github.com/cncf/toc/blob/main/DEFINITION.md)
- [Spring Boot Production-Ready Features](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
