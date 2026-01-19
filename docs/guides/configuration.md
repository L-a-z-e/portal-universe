# 설정 관리 가이드

## 개요

Portal Universe는 Spring Cloud Config Server를 사용하여 중앙화된 설정 관리를 제공합니다.

## 설정 저장소

**외부 Git 저장소**: https://github.com/L-a-z-e/portal-universe-config-repo.git

## 애플리케이션 프로필

| 프로필 | 환경 | 설명 |
|--------|------|------|
| `local` | 로컬 개발 | IDE에서 직접 실행 (기본값) |
| `docker` | Docker Compose | 컨테이너 환경 |
| `k8s` | Kubernetes | 클러스터 환경 |

## 프로필 활성화

### 환경 변수

```bash
SPRING_PROFILES_ACTIVE=docker
```

### JVM 옵션

```bash
java -Dspring.profiles.active=docker -jar app.jar
```

### Gradle 실행

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew :services:auth-service:bootRun
```

## 설정 파일 구조

```
config-repo/
├── application.yml              # 공통 설정
├── application-local.yml        # 로컬 환경
├── application-docker.yml       # Docker 환경
├── application-k8s.yml          # Kubernetes 환경
├── auth-service.yml             # Auth 서비스 전용
├── auth-service-docker.yml
├── blog-service.yml             # Blog 서비스 전용
└── ...
```

## 설정 조회

Config Server에서 직접 설정 조회:

```bash
# 형식: /{application}/{profile}
curl http://localhost:8888/auth-service/docker
curl http://localhost:8888/blog-service/local
```

## 주요 설정 항목

### 데이터베이스

```yaml
# MySQL (auth-service, shopping-service)
spring:
  datasource:
    url: jdbc:mysql://mysql-db:3306/auth_db
    username: laze
    password: password

# MongoDB (blog-service)
spring:
  data:
    mongodb:
      uri: mongodb://laze:password@mongodb:27017/blog_db
```

### Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: kafka:29092
```

### Redis

```yaml
spring:
  redis:
    host: redis
    port: 6379
```

## 환경별 차이

| 설정 | local | docker | k8s |
|------|-------|--------|-----|
| DB Host | localhost | mysql-db | mysql.portal-universe |
| Kafka | localhost:9092 | kafka:29092 | kafka.portal-universe:9092 |
| Config Server | localhost:8888 | config-service:8888 | config-service:8888 |

## 설정 새로고침

실행 중인 서비스의 설정을 동적으로 새로고침:

```bash
# Actuator 엔드포인트 사용
curl -X POST http://localhost:8080/actuator/refresh
```

## 민감한 정보 관리

### 환경 변수 사용

```yaml
spring:
  datasource:
    password: ${MYSQL_PASSWORD}
```

### Kubernetes Secret

```bash
kubectl create secret generic db-credentials \
  --from-literal=password=your-password
```

## 참고

- [Spring Cloud Config 문서](https://spring.io/projects/spring-cloud-config)
- [설정 저장소](https://github.com/L-a-z-e/portal-universe-config-repo)
- [프로젝트 README](../../README.md)
