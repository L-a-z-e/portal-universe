---
id: api-gateway-health-monitoring
title: API Gateway 헬스체크 및 모니터링
type: api
status: current
created: 2026-02-06
updated: 2026-02-06
author: documenter-agent
tags: [api-gateway, health-check, monitoring, prometheus, zipkin, kubernetes]
related:
  - api-gateway-routing-specification
  - api-gateway-resilience
---

# API Gateway 헬스체크 및 모니터링

## 개요

API Gateway는 자체 상태 확인과 함께 모든 백엔드 서비스의 상태를 통합 조회하는 Health Aggregation API를 제공합니다.

- **통합 Health API**: 7개 서비스 상태를 한 번에 조회
- **Kubernetes 연동**: Pod 정보, Replica 상태 포함
- **Actuator**: 환경별 엔드포인트 노출 설정
- **분산 추적**: Brave/Zipkin 연동 (Docker, K8s)
- **메트릭**: Prometheus 메트릭 수집

## 통합 Health API

> **소스 파일**: `ServiceHealthController.java`, `ServiceHealthAggregator.java`

### GET /api/health/services

모든 마이크로서비스의 상태를 집계하여 반환합니다.

**요청**:
```bash
curl -X GET http://localhost:8080/api/health/services
```

**응답 (200 OK)**:
```json
{
  "overallStatus": "up",
  "timestamp": "2026-02-06T14:23:45.123Z",
  "services": [
    {
      "name": "api-gateway",
      "displayName": "API Gateway",
      "status": "up",
      "responseTime": 5,
      "replicas": null,
      "readyReplicas": null,
      "pods": null
    },
    {
      "name": "auth-service",
      "displayName": "Auth Service",
      "status": "up",
      "responseTime": 42,
      "replicas": null,
      "readyReplicas": null,
      "pods": null
    },
    {
      "name": "prism-service",
      "displayName": "Prism Service",
      "status": "down",
      "responseTime": 3000,
      "replicas": null,
      "readyReplicas": null,
      "pods": null
    }
  ]
}
```

### Overall Status 결정 로직

| 조건 | overallStatus |
|------|---------------|
| 모든 서비스 `up` | `up` |
| 모든 서비스 `down` | `down` |
| 혼합 상태 | `degraded` |
| 서비스 없음 | `unknown` |

### Kubernetes 환경 응답 (Pod 정보 포함)

```json
{
  "overallStatus": "degraded",
  "timestamp": "2026-02-06T14:23:45.123Z",
  "services": [
    {
      "name": "auth-service",
      "displayName": "Auth Service",
      "status": "up",
      "responseTime": 35,
      "replicas": 2,
      "readyReplicas": 2,
      "pods": [
        {
          "name": "auth-service-7d4f5b8c9-x2k4n",
          "phase": "Running",
          "ready": true,
          "restarts": 0
        },
        {
          "name": "auth-service-7d4f5b8c9-m8p2q",
          "phase": "Running",
          "ready": true,
          "restarts": 1
        }
      ]
    }
  ]
}
```

## 모니터링 대상 서비스

> **소스 파일**: `application.yml` - `health-check.services`

| 서비스 | Display Name | Health Path | K8s Deployment |
|--------|-------------|-------------|----------------|
| `api-gateway` | API Gateway | `/actuator/health` | `api-gateway` |
| `auth-service` | Auth Service | `/actuator/health` | `auth-service` |
| `blog-service` | Blog Service | `/actuator/health` | `blog-service` |
| `shopping-service` | Shopping Service | `/actuator/health` | `shopping-service` |
| `notification-service` | Notification Service | `/actuator/health` | `notification-service` |
| `prism-service` | Prism Service | `/api/v1/health` | `prism-service` |
| `chatbot-service` | Chatbot Service | `/api/v1/chat/health` | `chatbot-service` |

### Health Status 해석

| 응답 형식 | 조건 | 상태 |
|-----------|------|------|
| Spring Boot Actuator `{"status": "UP"}` | status == "UP" | `up` |
| Spring Boot Actuator `{"status": "DOWN"}` | status == "DOWN" | `down` |
| Spring Boot Actuator 기타 | - | `degraded` |
| Custom `{"success": true, "data": {"status": "ok"}}` | success && data.status == "ok" | `up` |
| Custom `{"success": true}` | success만 true | `up` |
| 응답 없음 / 타임아웃 | 오류 발생 | `down` |

### API Gateway 자체 Health

API Gateway 자체 상태는 HTTP 호출이 아닌 Spring Boot `HealthEndpoint`를 직접 호출하여 self-call 타임아웃을 방지합니다.

### Health Check 타임아웃

| 항목 | 값 |
|------|-----|
| 연결 타임아웃 | 2초 (`ChannelOption.CONNECT_TIMEOUT_MILLIS`) |
| 응답 타임아웃 | 3초 (`HEALTH_CHECK_TIMEOUT`) |
| K8s 정보 조회 | 2초 (비동기) |

## Kubernetes 연동

> **소스 파일**: `ServiceHealthAggregator.java`, `KubernetesClientConfig.java`

K8s 환경에서는 `fabric8 KubernetesClient`를 통해 추가 정보를 수집합니다.

### 수집 정보

| 정보 | 소스 | 설명 |
|------|------|------|
| `replicas` | Deployment.status.replicas | 전체 Replica 수 |
| `readyReplicas` | Deployment.status.readyReplicas | Ready 상태 Replica 수 |
| `pods[].name` | Pod.metadata.name | Pod 이름 |
| `pods[].phase` | Pod.status.phase | Pod 상태 (Running, Pending 등) |
| `pods[].ready` | ContainerStatus.ready | 모든 컨테이너 Ready 여부 |
| `pods[].restarts` | ContainerStatus.restartCount | 재시작 횟수 합계 |

### Namespace

```yaml
# 환경 변수로 설정
KUBERNETES_NAMESPACE: portal-universe  # 기본값
```

### K8s Client 부재 시

`KubernetesClient` Bean이 없는 환경(Local, Docker)에서는 Pod 정보 없이 Health 상태만 반환합니다 (`@Autowired(required = false)`).

## Actuator 설정

### 환경별 노출 엔드포인트

| 엔드포인트 | Local | Docker | Kubernetes |
|-----------|-------|--------|------------|
| `health` | O | O | O |
| `info` | O | O | O |
| `prometheus` | O | O | O |
| `metrics` | O | O | O |
| `env` | O | O | - |
| Health Probes | - | O | O |
| Liveness | - | - | O |
| Readiness | - | - | O |

### Health Details

모든 환경에서 `show-details: always` 설정됨.

### Kubernetes Probes

```yaml
# application-kubernetes.yml
management:
  endpoint:
    health:
      probes:
        enabled: true
      liveness:
        enabled: true
      readiness:
        enabled: true
```

Kubernetes Liveness/Readiness Probe가 Actuator 엔드포인트를 사용합니다:
- Liveness: `GET /actuator/health/liveness`
- Readiness: `GET /actuator/health/readiness`

## Prometheus 메트릭

모든 환경에서 Prometheus 메트릭 수집이 활성화됩니다.

```yaml
management:
  prometheus:
    metrics:
      export:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: docker  # 또는 kubernetes
```

**메트릭 접근**: `GET /actuator/prometheus`

## 분산 추적 (Zipkin)

Docker 및 Kubernetes 환경에서 Brave/Zipkin 기반 분산 추적이 활성화됩니다.

```yaml
# application-docker.yml, application-kubernetes.yml
management:
  tracing:
    sampling:
      probability: 1.0    # 100% 샘플링
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

| 설정 | 값 | 설명 |
|------|-----|------|
| 샘플링 확률 | `1.0` (100%) | 모든 요청 추적 |
| Zipkin 엔드포인트 | `http://zipkin:9411/api/v2/spans` | Zipkin 서버 |

> Local 환경에서는 Zipkin 설정이 없어 분산 추적이 비활성화됩니다.

## 소스 파일 참조

| 파일 | 역할 |
|------|------|
| `health/ServiceHealthController.java` | `GET /api/health/services` 엔드포인트 |
| `health/ServiceHealthAggregator.java` | 7서비스 Health 집계, K8s 정보 보강 |
| `health/config/HealthCheckProperties.java` | Health Check 대상 서비스 설정 |
| `health/config/KubernetesClientConfig.java` | K8s Client 설정 |
| `health/dto/ServiceHealthResponse.java` | 응답 DTO (overallStatus, services) |
| `health/dto/ServiceHealthInfo.java` | 서비스별 상태 DTO |
| `health/dto/PodInfo.java` | Pod 정보 DTO |

## 관련 문서

- [장애 복원력](./resilience.md) - Circuit Breaker 상태 모니터링
- [라우팅 명세](./routing-specification.md) - Actuator 프록시 라우트
