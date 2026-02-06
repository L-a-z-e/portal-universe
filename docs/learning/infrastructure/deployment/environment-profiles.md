# Environment Profiles

Portal Universe의 환경별 설정 프로필과 네트워크 토폴로지를 학습합니다.

---

## 1. 학습 목표

- 환경별 Spring Boot 프로필의 차이점 이해
- 네트워크 토폴로지와 서비스 디스커버리 메커니즘 학습
- 프로필 활성화 방법 습득
- 환경 전환 시 주의사항 파악

---

## 2. 환경 개요

Portal Universe는 3가지 환경을 지원합니다:

| 환경 | 프로필 | 주 용도 | 인프라 |
|------|--------|---------|---------|
| **Local** | `local` | IDE 개발 | localhost |
| **Docker Compose** | `docker` | 통합 테스트 | Docker 네트워크 |
| **Kubernetes** | `kubernetes` | 운영/스테이징 | K8s 클러스터 |

---

## 3. 네트워크 토폴로지 비교

### Local 개발 환경

```
┌─────────────────────────────────────────────────────┐
│                   localhost                          │
│                                                       │
│  IDE (Spring Boot App)                               │
│  ├─ blog-service:8082                                │
│  │  └─ http://localhost:4566  ──────┐                │
│  │                                   │                │
│  ├─ auth-service:8081                │                │
│  └─ shopping-service:8083            │                │
│                                      │                │
│  LocalStack:4566 <───────────────────┘                │
│  ├─ S3                                                │
│  ├─ SQS                                               │
│  └─ DynamoDB                                          │
│                                                       │
│  MongoDB:27017                                        │
│  Kafka:9092                                           │
│  Redis:6379                                           │
└─────────────────────────────────────────────────────┘

특징:
- 모든 서비스가 localhost 주소 사용
- 포트 충돌 주의 필요
- 빠른 개발 사이클
```

### Docker Compose 환경

```
┌─────────────────────────────────────────────────────┐
│           portal-universe-net (bridge)               │
│                                                       │
│  blog-service ───────────────────┐                   │
│  ├─ Container: blog-service      │                   │
│  └─ http://localstack:4566 ──────┼──┐                │
│                                   │  │                │
│  auth-service                     │  │                │
│  ├─ Container: auth-service       │  │                │
│  └─ http://localstack:4566 ──────┼──┤                │
│                                   │  │                │
│  shopping-service                 │  │                │
│  ├─ Container: shopping-service   │  │                │
│  └─ http://localstack:4566 ──────┼──┘                │
│                                   │                   │
│  localstack <─────────────────────┘                   │
│  ├─ Container: localstack                             │
│  ├─ Internal: 4566                                    │
│  └─ External: localhost:4566 (포트 매핑)              │
│                                                       │
│  mongodb (Container: mongodb)                         │
│  kafka (Container: kafka)                             │
│  redis (Container: redis)                             │
└─────────────────────────────────────────────────────┘

특징:
- Docker DNS 기반 서비스 디스커버리
- 컨테이너 이름으로 통신 (localstack, mongodb, kafka)
- 네트워크 격리
- 호스트에서는 localhost:포트로 접근
```

### Kubernetes 환경

```
┌─────────────────────────────────────────────────────┐
│        Kubernetes Cluster (namespace: default)       │
│                                                       │
│  blog-service Pod                                    │
│  ├─ Container: blog-service                          │
│  └─ http://localstack:4566 ──────┐                   │
│                                   │                   │
│  auth-service Pod                 │                   │
│  ├─ Container: auth-service       │                   │
│  └─ http://localstack:4566 ──────┼──┐                │
│                                   │  │                │
│  shopping-service Pod             │  │                │
│  ├─ Container: shopping-service   │  │                │
│  └─ http://localstack:4566 ──────┼──┤                │
│                                   │  │                │
│  localstack Service <─────────────┘  │                │
│  ├─ ClusterIP: 10.96.X.X             │                │
│  ├─ DNS: localstack.default.svc ─────┘                │
│  └─ Pod: localstack                                   │
│                                                       │
│  mongodb Service                                      │
│  ├─ ClusterIP: 10.96.X.X                              │
│  ├─ DNS: mongodb.default.svc                          │
│  └─ Pod: mongodb                                      │
│                                                       │
│  kafka Service                                        │
│  redis Service                                        │
└─────────────────────────────────────────────────────┘

특징:
- Service 리소스를 통한 로드 밸런싱
- DNS 기반 서비스 디스커버리
  - 짧은 이름: localstack (같은 namespace)
  - FQDN: localstack.default.svc.cluster.local
- Pod IP는 동적 변경 가능 (Service가 추상화)
- Liveness/Readiness probe 지원
```

---

## 4. 프로필별 설정 비교

### Blog Service 예시

#### application-local.yml

```yaml
# Local 개발 환경
services:
  auth:
    url: "http://localhost:8081"
  blog:
    url: "http://localhost:8082"
  shopping:
    url: "http://localhost:8083"

spring:
  data:
    mongodb:
      uri: mongodb://laze:password@localhost:27017/blog_db?authSource=admin
  kafka:
    bootstrap-servers: localhost:9092

aws:
  s3:
    endpoint: http://localhost:4566
    bucket-name: blog-bucket
    region: ap-northeast-2

management:
  endpoints:
    web:
      exposure:
        include: health,env,info,metrics

logging:
  level:
    root: INFO
```

#### application-docker.yml

```yaml
# Docker Compose 환경
services:
  auth:
    url: "http://auth-service:8081"
  blog:
    url: "http://blog-service:8082"
  shopping:
    url: "http://shopping-service:8083"

spring:
  data:
    mongodb:
      uri: mongodb://laze:password@mongodb:27017/blog_db?authSource=admin
  kafka:
    bootstrap-servers: kafka:29092

aws:
  s3:
    endpoint: http://localstack:4566
    bucket-name: blog-bucket
    region: ap-northeast-2

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  prometheus:
    metrics:
      export:
        enabled: true
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans

logging:
  level:
    root: INFO
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

#### application-kubernetes.yml

```yaml
# Kubernetes 환경
services:
  auth:
    url: "http://auth-service"
  blog:
    url: "http://blog-service"
  shopping:
    url: "http://shopping-service"

spring:
  data:
    mongodb:
      uri: mongodb://laze:password@mongodb:27017/blog_db?authSource=admin
  kafka:
    bootstrap-servers: kafka:29092

aws:
  s3:
    endpoint: http://localstack:4566
    bucket-name: blog-bucket
    region: ap-northeast-2

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      probes:
        enabled: true
      liveness:
        enabled: true
      readiness:
        enabled: true
  prometheus:
    metrics:
      export:
        enabled: true
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans

logging:
  level:
    root: INFO
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### 주요 차이점 비교

| 항목 | Local | Docker | Kubernetes |
|------|-------|--------|------------|
| **서비스 URL** | localhost:포트 | 컨테이너이름:포트 | service이름 |
| **LocalStack** | localhost:4566 | localstack:4566 | localstack:4566 |
| **MongoDB** | localhost:27017 | mongodb:27017 | mongodb:27017 |
| **Kafka** | localhost:9092 | kafka:29092 | kafka:29092 |
| **Prometheus** | ❌ | ✅ | ✅ |
| **Zipkin** | ❌ | ✅ | ✅ |
| **Health Probes** | ❌ | ❌ | ✅ |

---

## 5. 프로필 활성화 방법

### IDE (IntelliJ IDEA)

**방법 1: Run Configuration**

```
Run > Edit Configurations
├─ Environment Variables
│  └─ SPRING_PROFILES_ACTIVE=local
└─ VM Options
   └─ -Dspring.profiles.active=local
```

**방법 2: application.yml**

```yaml
spring:
  profiles:
    active: local
```

### Docker Compose

```yaml
# docker-compose.yml
services:
  blog-service:
    image: blog-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    networks:
      - portal-universe-net
```

### Kubernetes

**ConfigMap 사용**

```yaml
# k8s/blog-service/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: blog-service-config
  namespace: default
data:
  SPRING_PROFILES_ACTIVE: "kubernetes"
```

```yaml
# k8s/blog-service/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: blog-service
spec:
  template:
    spec:
      containers:
        - name: blog-service
          image: blog-service:latest
          envFrom:
            - configMapRef:
                name: blog-service-config
```

**직접 환경 변수 지정**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: blog-service
spec:
  template:
    spec:
      containers:
        - name: blog-service
          image: blog-service:latest
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "kubernetes"
```

---

## 6. 환경 전환 시 주의사항

### 1. 네트워크 주소 변경

```java
// ❌ 하드코딩된 주소
String authUrl = "http://localhost:8081";

// ✅ 프로필별 설정 사용
@Value("${services.auth.url}")
private String authUrl;
```

### 2. Kafka 브로커 주소

```yaml
# Local: localhost:9092
# Docker: kafka:29092 (내부 포트)
# K8s: kafka:29092 (Service 이름)

spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

### 3. LocalStack Endpoint

```java
@Configuration
public class AwsConfig {

    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
            .region(Region.AP_NORTHEAST_2);

        // LocalStack 사용 시
        if (!s3Endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(s3Endpoint))
                   .serviceConfiguration(S3Configuration.builder()
                       .pathStyleAccessEnabled(true)  // 필수!
                       .build());
        }

        return builder.build();
    }
}
```

### 4. 데이터베이스 호스트

```yaml
# Local
mongodb://localhost:27017/blog_db

# Docker/K8s
mongodb://mongodb:27017/blog_db
```

### 5. CORS 설정

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.frontend.base-url}")
    private String frontendUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(frontendUrl)  // 프로필별 설정
                .allowedMethods("*")
                .allowCredentials(true);
    }
}
```

```yaml
# Local
app:
  frontend:
    base-url: http://localhost:30000

# Docker
app:
  frontend:
    base-url: https://portal-shell:30000

# K8s
app:
  frontend:
    base-url: https://portal.example.com
```

---

## 7. Portal Universe 적용 예시

### 다중 프로필 구성

```
services/blog-service/src/main/resources/
├── application.yml             # 공통 설정
├── application-local.yml       # Local 전용
├── application-docker.yml      # Docker Compose 전용
└── application-kubernetes.yml  # Kubernetes 전용
```

### application.yml (공통 설정)

```yaml
spring:
  application:
    name: blog-service
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB

feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000

aws:
  s3:
    access-key: ${AWS_ACCESS_KEY_ID:test}
    secret-key: ${AWS_SECRET_ACCESS_KEY:test}
```

### 프로필별 오버라이드

각 프로필 파일은 `application.yml`의 설정을 오버라이드합니다:

```yaml
# application-kubernetes.yml
aws:
  s3:
    # 공통 설정(access-key, secret-key)은 그대로 사용
    # endpoint만 추가
    endpoint: http://localstack:4566
```

---

## 8. 디버깅 팁

### 활성화된 프로필 확인

```bash
# 로그에서 확인
2024-01-22 10:00:00 INFO --- [main] o.s.boot.SpringApplication : The following profiles are active: docker

# Actuator endpoint 활용
curl http://localhost:8082/actuator/env | jq '.activeProfiles'
```

### 환경 변수 확인

```bash
# Docker
docker exec blog-service env | grep SPRING_PROFILES_ACTIVE

# Kubernetes
kubectl exec blog-service-pod -- env | grep SPRING_PROFILES_ACTIVE
```

### 설정값 확인

```bash
# Actuator로 설정 조회
curl http://localhost:8082/actuator/env/services.auth.url
```

---

## 9. 핵심 요약

### 체크리스트

- [ ] 환경별 네트워크 토폴로지 이해
- [ ] localhost vs 컨테이너이름 vs 서비스이름 차이 파악
- [ ] 프로필 활성화 방법 숙지
- [ ] LocalStack endpoint 설정 이해
- [ ] 환경 전환 시 주의사항 인지

### 핵심 포인트

1. **Local**: 모든 서비스가 `localhost` 주소 사용
2. **Docker**: Docker DNS로 `컨테이너이름` 해석
3. **Kubernetes**: Service 리소스로 `서비스이름` 해석
4. **프로필 분리**: 공통 설정 + 환경별 오버라이드 구조
5. **동적 설정**: 환경 변수와 Spring Boot 프로필 조합

---

## 10. 관련 문서

- [Local to Kubernetes 전환](./local-to-kubernetes.md) - 환경 전환 가이드
- [LocalStack S3](../infra/localstack-s3.md) - LocalStack 상세 가이드
- [Docker Compose](../../infra/docker-compose.md) - Docker Compose 설정
- [Kubernetes Fundamentals](../../infra/kubernetes-fundamentals.md) - Kubernetes 기초
- [Kubernetes Networking](../../infra/kubernetes-networking.md) - K8s 네트워킹
