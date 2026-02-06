---
id: configuration
title: 설정 관리 가이드
type: guide
status: current
created: 2026-01-19
updated: 2026-02-06
author: Laze
tags: [configuration, profiles, spring-boot, guide]
---

# 설정 관리 가이드

**난이도**: ⭐⭐ | **예상 시간**: 15분 | **카테고리**: Operations

## 개요

Portal Universe는 각 서비스에 설정 파일을 직접 포함하는 방식으로 설정을 관리합니다. 프로필 기반 설정을 통해 환경별(로컬, Docker, Kubernetes) 설정을 분리합니다.

## 애플리케이션 프로필

| 프로필 | 환경 | 설명 |
|--------|------|------|
| `local` | 로컬 개발 | IDE에서 직접 실행 (기본값) |
| `docker` | Docker Compose | 컨테이너 환경 |
| `kubernetes` | Kubernetes | 클러스터 환경 |

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

각 서비스는 다음과 같은 설정 파일 구조를 가집니다:

```
services/{service-name}/src/main/resources/
├── application.yml              # 공통 설정 (기본값)
├── application-local.yml        # 로컬 환경 (localhost)
├── application-docker.yml       # Docker Compose 환경
└── application-kubernetes.yml   # Kubernetes 환경
```

## 주요 설정 항목

### 데이터베이스

```yaml
# MySQL (auth-service, shopping-service, notification-service)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auth_db  # local
    url: jdbc:mysql://mysql-db:3306/auth_db   # docker/k8s
    username: laze
    password: password

# MongoDB (blog-service)
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/blog_db  # local
      uri: mongodb://mongodb:27017/blog_db    # docker/k8s
```

### Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092  # local
    bootstrap-servers: kafka:29092     # docker/k8s
```

### Redis

```yaml
spring:
  data:
    redis:
      host: localhost  # local
      host: redis      # docker/k8s
      port: 6379
```

## 환경별 차이

| 설정 | local | docker | kubernetes |
|------|-------|--------|------------|
| DB Host | localhost | mysql-db | mysql-db |
| Kafka | localhost:9092 | kafka:29092 | kafka:29092 |
| MongoDB | localhost:27017 | mongodb:27017 | mongodb:27017 |
| Redis | localhost:6379 | redis:6379 | redis:6379 |

> **참고**: Kubernetes 환경에서는 Kubernetes DNS를 사용하여 서비스 간 통신을 수행합니다.

## 민감한 정보 관리

### 환경 변수 사용

```yaml
spring:
  datasource:
    password: ${MYSQL_PASSWORD:password}
```

### Docker Compose 환경

`.env` 파일 또는 `docker-compose.yml`에서 환경 변수 설정:

```yaml
services:
  auth-service:
    environment:
      - MYSQL_PASSWORD=your-password
```

### Kubernetes Secret

```bash
kubectl create secret generic portal-universe-secret \
  --from-literal=MYSQL_PASSWORD=your-password \
  -n portal-universe
```

Deployment에서 Secret 참조:

```yaml
env:
  - name: MYSQL_PASSWORD
    valueFrom:
      secretKeyRef:
        name: portal-universe-secret
        key: MYSQL_PASSWORD
```

## Kubernetes ConfigMap

Kubernetes 환경에서는 ConfigMap을 통해 공통 환경 변수를 관리합니다.

**파일 위치**: `k8s/infrastructure/configmap.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: portal-universe-config
  namespace: portal-universe
data:
  SPRING_PROFILES_ACTIVE: "kubernetes"
  KAFKA_BOOTSTRAP_SERVERS: "kafka:29092"
  MYSQL_HOST: "mysql-db"
  MONGODB_HOST: "mongodb"
```

## 참고

- [Spring Boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [Kubernetes ConfigMaps](https://kubernetes.io/docs/concepts/configuration/configmap/)
- [프로젝트 README](../../README.md)
