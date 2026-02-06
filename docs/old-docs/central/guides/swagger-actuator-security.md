---
id: guide-swagger-actuator-security
title: Swagger/Actuator 보안 설정 가이드
type: guide
status: current
created: 2026-01-23
updated: 2026-01-30
author: Laze
tags: [swagger, actuator, security, spring-boot, prometheus]
related:
  - guide-jwt-rbac-setup
  - guide-security-implementation-spec
---

# Swagger/Actuator 보안 설정 가이드

## 개요

Portal Universe 프로젝트의 모든 서비스에 대한 Swagger UI 및 Actuator 엔드포인트 보안 정책과 접근 방법을 설명합니다.

---

## 환경별 보안 정책

| 환경 | Swagger UI | Actuator (health/info) | Actuator (prometheus/metrics) |
|------|------------|------------------------|-------------------------------|
| **local** | 활성화 (공개) | 공개 | 공개 |
| **docker** | 활성화 (공개) | 공개 | 내부망 전용 |
| **kubernetes** | 비활성화 | 공개 | 내부망 전용 |

### 정책 상세

#### Local 환경
- 모든 엔드포인트 공개 (개발 편의성 우선)
- Swagger UI 접근: `http://localhost:{port}/swagger-ui.html`
- Actuator 접근: `http://localhost:{port}/actuator/{endpoint}`

#### Docker 환경
- Swagger UI 활성화 (팀 내 개발/테스트용)
- Health/Info: 공개 (상태 확인용)
- Prometheus/Metrics: 내부망 전용 (Prometheus가 스크래핑)
- Gateway를 통한 Swagger 접근 차단

#### Kubernetes (프로덕션)
- Swagger UI 비활성화 (보안)
- Health/Info: 공개 (Kubernetes Liveness/Readiness Probe 용)
- Prometheus/Metrics: 내부망 전용 (Prometheus가 스크래핑)
- Gateway를 통한 민감 정보 접근 차단

---

## 서비스별 포트 및 접근 경로

### Auth Service

| 엔드포인트 | 포트 | 경로 | 외부 접근 |
|-----------|------|------|-----------|
| API | 8081 | `/api/auth/**` | Gateway 경유 |
| Swagger UI | 8081 | `/swagger-ui.html` | 직접 접근만 (Gateway 차단) |
| Health | 8081 | `/actuator/health` | Gateway 경유 가능 |
| Info | 8081 | `/actuator/info` | Gateway 경유 가능 |
| Prometheus | 8081 | `/actuator/prometheus` | 직접 접근만 (Gateway 차단) |
| Metrics | 8081 | `/actuator/metrics` | 직접 접근만 (Gateway 차단) |

### Blog Service

| 엔드포인트 | 포트 | 경로 | 외부 접근 |
|-----------|------|------|-----------|
| API | 8082 | `/api/blog/**` | Gateway 경유 |
| Swagger UI | 8082 | `/swagger-ui.html` | 직접 접근만 (Gateway 차단) |
| Health | 8082 | `/actuator/health` | Gateway 경유 가능 |
| Info | 8082 | `/actuator/info` | Gateway 경유 가능 |
| Prometheus | 8082 | `/actuator/prometheus` | 직접 접근만 (Gateway 차단) |
| Metrics | 8082 | `/actuator/metrics` | 직접 접근만 (Gateway 차단) |

### Shopping Service

| 엔드포인트 | 포트 | 경로 | 외부 접근 |
|-----------|------|------|-----------|
| API | 8083 | `/api/shopping/**` | Gateway 경유 |
| Swagger UI | 8083 | `/swagger-ui.html` | 직접 접근만 (Gateway 차단) |
| Health | 8083 | `/actuator/health` | Gateway 경유 가능 |
| Info | 8083 | `/actuator/info` | Gateway 경유 가능 |
| Prometheus | 8083 | `/actuator/prometheus` | 직접 접근만 (Gateway 차단) |
| Metrics | 8083 | `/actuator/metrics` | 직접 접근만 (Gateway 차단) |

---

## 접근 방법

### 1. Swagger UI 접근

#### Local 환경
```bash
# Auth Service
http://localhost:8081/swagger-ui.html

# Blog Service
http://localhost:8082/swagger-ui.html

# Shopping Service
http://localhost:8083/swagger-ui.html
```

#### Docker 환경
```bash
# 컨테이너 내부에서
curl http://auth-service:8081/swagger-ui.html

# 호스트에서 포트 포워딩 설정 시
http://localhost:8081/swagger-ui.html
```

#### Kubernetes 환경
```bash
# Swagger UI 비활성화됨 (404 반환)
# 필요시 port-forward로 직접 접근
kubectl port-forward svc/auth-service 8081:8081 -n portal-universe
http://localhost:8081/swagger-ui.html
```

### 2. Actuator 접근

#### Gateway를 통한 접근 (Health/Info만 가능)
```bash
# Auth Service Health
curl http://localhost:8080/api/auth/actuator/health

# Blog Service Info
curl http://localhost:8080/api/blog/actuator/info

# Shopping Service Health
curl http://localhost:8080/api/shopping/actuator/health
```

#### 직접 접근 (모든 엔드포인트)
```bash
# Local 환경
curl http://localhost:8081/actuator/prometheus
curl http://localhost:8081/actuator/metrics

# Docker 환경 (컨테이너 내부)
curl http://auth-service:8081/actuator/prometheus

# Kubernetes 환경 (port-forward)
kubectl port-forward svc/auth-service 8081:8081 -n portal-universe
curl http://localhost:8081/actuator/prometheus
```

---

## Spring Security 설정 구조

### Actuator 보안 필터 체인 (Order=0)
- `/actuator/health`, `/actuator/info`: **permitAll()** - 공개
- `/actuator/prometheus`, `/actuator/metrics/**`: **permitAll()** - 서비스 레벨에서는 허용하되, Gateway에서 차단
- `/actuator/**`: **denyAll()** - 나머지 모두 차단

### Swagger 보안 필터 체인 (Order=1)
- `/swagger-ui.html`, `/swagger-ui/**`, `/api-docs/**`: **permitAll()** - 서비스 레벨에서는 허용하되, Gateway에서 차단

### API 보안 필터 체인 (Order=2)
- 각 서비스별 비즈니스 로직 인가 정책 적용

---

## API Gateway 라우팅 정책

### Actuator 라우팅
- **허용**: `/api/{service}/actuator/health`, `/api/{service}/actuator/info`
- **차단**: `/api/{service}/actuator/prometheus`, `/api/{service}/actuator/metrics`, 기타 모든 Actuator 엔드포인트

### Swagger 라우팅
- **차단**: 모든 Swagger 관련 경로 (`/swagger-ui/**`, `/api-docs/**`)
- 이유: 프로덕션에서 API 스펙 노출 방지

### 비즈니스 API 라우팅
- **허용**: `/api/auth/**`, `/api/blog/**`, `/api/shopping/**` 등
- Rate Limiting, Circuit Breaker 적용

---

## Prometheus 메트릭 수집

### Docker 환경
Prometheus는 Docker 내부 네트워크를 통해 직접 서비스에 접근:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'auth-service'
    static_configs:
      - targets: ['auth-service:8081']
    metrics_path: '/actuator/prometheus'
```

### Kubernetes 환경
Prometheus는 ServiceMonitor를 통해 메트릭 수집:

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: auth-service
spec:
  selector:
    matchLabels:
      app: auth-service
  endpoints:
  - port: http
    path: /actuator/prometheus
```

---

## 설정 파일 위치

### Actuator 설정
```
services/*/src/main/resources/
├── application.yml              # 기본 설정
├── application-local.yml        # Local 프로필
├── application-docker.yml       # Docker 프로필
└── application-kubernetes.yml   # Kubernetes 프로필
```

### Security 설정
```
services/*/src/main/java/**/config/
└── SecurityConfig.java          # Spring Security 설정
```

### Gateway 라우팅 설정
```
services/api-gateway/src/main/resources/
└── application.yml              # Gateway 라우팅 규칙
```

---

## 프로필 활성화 방법

### Local 개발
```bash
# application-local.yml 자동 활성화 (기본값)
./gradlew :services:auth-service:bootRun
```

### Docker Compose
```bash
# docker-compose.yml에서 SPRING_PROFILES_ACTIVE=docker 설정
docker-compose up
```

### Kubernetes
```yaml
# deployment.yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "kubernetes"
```

---

## 트러블슈팅

### 1. Swagger UI 404 오류

**증상**: `/swagger-ui.html` 접근 시 404 반환

**원인 및 해결**:
- Kubernetes 프로필: Swagger 비활성화됨 → 정상 동작
- Local/Docker 프로필: `springdoc.swagger-ui.enabled=true` 확인

### 2. Actuator 403 Forbidden

**증상**: `/actuator/prometheus` 접근 시 403 반환

**원인 및 해결**:
- Gateway를 통한 접근: 차단됨 (정상) → 직접 서비스 포트로 접근
- SecurityConfig에서 `denyAll()` 설정 확인

### 3. Gateway를 통한 Swagger 접근 불가

**증상**: `http://localhost:8080/api/auth/swagger-ui.html` 404

**원인 및 해결**:
- Gateway 라우팅에 Swagger 경로 없음 (정상 설계)
- 직접 서비스 포트로 접근: `http://localhost:8081/swagger-ui.html`

### 4. Prometheus 메트릭 수집 실패

**증상**: Prometheus 대시보드에서 서비스 메트릭 없음

**원인 및 해결**:
- Prometheus 설정 확인: `scrape_configs`의 `targets` 확인
- 네트워크 접근 확인: `curl http://service:port/actuator/prometheus`
- Actuator 활성화 확인: `management.endpoints.web.exposure.include`

---

## 참고 자료

- [Spring Boot Actuator 공식 문서](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)
- [Springdoc OpenAPI 공식 문서](https://springdoc.org/)
- [Spring Cloud Gateway 공식 문서](https://docs.spring.io/spring-cloud-gateway/reference/)
- [Prometheus Spring Boot Exporter](https://github.com/prometheus/client_java)

---

## 변경 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-01-23 | 초기 문서 작성 - Swagger/Actuator 보안 정책 수립 |
