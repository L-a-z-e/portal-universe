# Config Service 아키텍처

## 시스템 구조

```
┌─────────────────────────────────────────────────────┐
│                    Config Server                     │
│                     (Port 8888)                      │
│  ┌─────────────────────────────────────────────┐   │
│  │          @EnableConfigServer                 │   │
│  │  ┌───────────────┐  ┌──────────────────┐   │   │
│  │  │ EnvironmentRepo│  │ HealthIndicator  │   │   │
│  │  │  (Git/Native) │  │ (Actuator)       │   │   │
│  │  └───────────────┘  └──────────────────┘   │   │
│  └─────────────────────────────────────────────┘   │
└───────────────────────┬─────────────────────────────┘
                        │
         ┌──────────────┼──────────────┐
         │              │              │
         ▼              ▼              ▼
   ┌──────────┐  ┌──────────┐  ┌──────────┐
   │auth-svc  │  │blog-svc  │  │shop-svc  │
   └──────────┘  └──────────┘  └──────────┘
```

## 설정 우선순위

```
1. 서비스 로컬 application.yml (낮음)
         ↓
2. Config Server의 application.yml
         ↓
3. Config Server의 {service}.yml
         ↓
4. Config Server의 {service}-{profile}.yml
         ↓
5. 환경 변수 / 시스템 프로퍼티 (높음)
```

## Git 저장소 구조

```yaml
# config-repo/application.yml (공통)
spring:
  kafka:
    bootstrap-servers: localhost:9092
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081

# config-repo/auth-service.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_db

# config-repo/auth-service-docker.yml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/auth_db
```

## 동적 설정 갱신

### Spring Cloud Bus 통합

```
┌──────────────┐     POST /actuator/bus-refresh
│ Config Server│ ─────────────────────────────────┐
└──────┬───────┘                                  │
       │                                          ▼
       │  ┌──────────────────────────────────────────┐
       │  │               Kafka / RabbitMQ           │
       │  └──────────────────────────────────────────┘
       │       │              │              │
       │       ▼              ▼              ▼
       │  ┌──────────┐  ┌──────────┐  ┌──────────┐
       └──│auth-svc  │  │blog-svc  │  │shop-svc  │
          │ @Refresh │  │ @Refresh │  │ @Refresh │
          └──────────┘  └──────────┘  └──────────┘
```

### @RefreshScope 사용

```java
@Configuration
@RefreshScope
public class FeatureToggleConfig {

    @Value("${feature.new-checkout:false}")
    private boolean newCheckoutEnabled;
}
```

## 보안 설정

### 암호화 지원

```yaml
# 암호화된 값
spring:
  datasource:
    password: '{cipher}FKSAJDFGYOS8F7GLHAKERGFHLSAJ'
```

### 암호화/복호화 API

```bash
# 암호화
curl -X POST http://localhost:8888/encrypt -d "my-secret"

# 복호화
curl -X POST http://localhost:8888/decrypt -d "{cipher}..."
```

## 프로파일 전략

| 프로파일 | 용도 | 설정 파일 |
|----------|------|-----------|
| `local` | 로컬 개발 | {service}-local.yml |
| `docker` | Docker Compose | {service}-docker.yml |
| `k8s` | Kubernetes | {service}-k8s.yml |

## Health Check

```bash
# Config Server 상태 확인
curl http://localhost:8888/actuator/health

# 특정 서비스 설정 확인
curl http://localhost:8888/auth-service/local
```

## 장애 대응

### Fail-Fast 비활성화

```yaml
# 클라이언트 설정
spring:
  cloud:
    config:
      fail-fast: false
      retry:
        initial-interval: 1000
        max-attempts: 6
```

Config Server가 다운되어도 클라이언트는 로컬 설정으로 기동 가능합니다.
