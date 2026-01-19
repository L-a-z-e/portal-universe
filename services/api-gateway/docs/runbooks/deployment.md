---
id: api-gateway-runbook-deployment
title: API Gateway 배포 운영 절차서
type: runbook
status: current
created: 2026-01-18
updated: 2026-01-18
author: documenter-agent
tags: [api-gateway, deployment, docker, kubernetes, operations]
related: []
---

# API Gateway 배포 운영 절차서

## 📋 개요

| 항목 | 내용 |
|------|------|
| **서비스명** | api-gateway |
| **포트** | 8080 |
| **예상 소요 시간** | 5-10분 (환경별 차이) |
| **필요 권한** | Docker/Kubernetes 실행 권한 |
| **의존성** | config-service (8888), auth-service (8081), blog-service (8082), shopping-service (8083) |

API Gateway는 Portal Universe의 단일 진입점으로 JWT 검증, 라우팅, CORS 처리를 담당합니다. 이 문서는 Docker Compose 및 Kubernetes 환경에서의 배포 절차를 설명합니다.

## 🎯 배포 환경

| 환경 | Spring Profile | 설명 |
|------|---------------|------|
| Local | `local` | 로컬 개발 환경 (기본값) |
| Docker Compose | `docker` | Docker Compose 기반 통합 환경 |
| Kubernetes | `k8s` | Kubernetes 클러스터 환경 |

---

## 1️⃣ Docker Compose 배포

### ✅ 사전 조건
- Docker 및 Docker Compose 설치
- `docker-compose.yml` 파일 존재
- config-service가 먼저 실행 중이어야 함

### 🔄 배포 절차

#### Step 1: 전체 스택 시작
```bash
docker-compose up -d
```

#### Step 2: API Gateway만 재시작
```bash
docker-compose restart api-gateway
```

#### Step 3: 로그 확인
```bash
docker-compose logs -f api-gateway
```

**예상 결과**:
```
api-gateway    | Started ApiGatewayApplication in 15.234 seconds
api-gateway    | Tomcat started on port(s): 8080 (http)
```

#### Step 4: 헬스 체크
```bash
curl http://localhost:8080/actuator/health
```

**예상 응답**:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### ⚠️ 문제 발생 시
- **컨테이너 시작 실패**: `docker-compose logs api-gateway`로 로그 확인
- **포트 충돌**: `docker ps`로 8080 포트 사용 확인
- **네트워크 연결 실패**: `docker network inspect portal-universe_default` 확인

---

## 2️⃣ Docker 이미지 빌드

### Gradle Bootpack 사용

#### Step 1: 이미지 빌드
```bash
cd /path/to/portal-universe
./gradlew :services:api-gateway:bootBuildImage
```

**예상 결과**:
```
Successfully built image 'docker.io/library/api-gateway:0.0.1-SNAPSHOT'
```

#### Step 2: 이미지 확인
```bash
docker images | grep api-gateway
```

#### Step 3: 이미지 태깅 (선택)
```bash
docker tag api-gateway:0.0.1-SNAPSHOT myregistry.io/api-gateway:latest
```

#### Step 4: 레지스트리에 푸시 (선택)
```bash
docker push myregistry.io/api-gateway:latest
```

### Dockerfile 기반 빌드 (대안)
```bash
docker build -t api-gateway:custom -f services/api-gateway/Dockerfile .
```

---

## 3️⃣ Kubernetes 배포

### ✅ 사전 조건
- Kubernetes 클러스터 접근 권한
- `kubectl` CLI 설치
- 매니페스트 위치: `k8s/` 디렉토리

### 🔄 배포 절차

#### Step 1: ConfigMap 및 Secret 생성
```bash
# ConfigMap 생성 (application-k8s.yml)
kubectl create configmap api-gateway-config \
  --from-file=k8s/config/api-gateway/application-k8s.yml \
  -n portal-universe

# Secret 생성 (JWT 키 등)
kubectl create secret generic api-gateway-secret \
  --from-literal=jwt-secret-key='your-secret-key' \
  -n portal-universe
```

#### Step 2: Deployment 배포
```bash
kubectl apply -f k8s/api-gateway-deployment.yaml
```

**매니페스트 예시**:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
  namespace: portal-universe
spec:
  replicas: 2
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
      - name: api-gateway
        image: api-gateway:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: JAVA_OPTS
          value: "-Xms512m -Xmx1024m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

#### Step 3: Service 배포
```bash
kubectl apply -f k8s/api-gateway-service.yaml
```

**Service 예시**:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: portal-universe
spec:
  type: LoadBalancer
  ports:
  - port: 8080
    targetPort: 8080
    protocol: TCP
  selector:
    app: api-gateway
```

#### Step 4: 배포 상태 확인
```bash
# Pod 상태 확인
kubectl get pods -n portal-universe -l app=api-gateway

# Deployment 상태 확인
kubectl rollout status deployment/api-gateway -n portal-universe

# 로그 확인
kubectl logs -f deployment/api-gateway -n portal-universe
```

**예상 결과**:
```
NAME                           READY   STATUS    RESTARTS   AGE
api-gateway-7d4c8b9f8d-abcde   1/1     Running   0          2m
api-gateway-7d4c8b9f8d-fghij   1/1     Running   0          2m
```

#### Step 5: 서비스 엔드포인트 확인
```bash
kubectl get svc api-gateway -n portal-universe
```

---

## 4️⃣ 헬스체크 및 모니터링

### Actuator 엔드포인트

#### 헬스 체크
```bash
# 전체 헬스 체크
curl http://localhost:8080/actuator/health

# Liveness 체크 (Kubernetes용)
curl http://localhost:8080/actuator/health/liveness

# Readiness 체크 (Kubernetes용)
curl http://localhost:8080/actuator/health/readiness
```

#### 메트릭 확인
```bash
# 모든 메트릭
curl http://localhost:8080/actuator/metrics

# CPU 사용률
curl http://localhost:8080/actuator/metrics/process.cpu.usage

# 메모리 사용량
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP 요청 통계
curl http://localhost:8080/actuator/metrics/http.server.requests
```

#### 애플리케이션 정보
```bash
curl http://localhost:8080/actuator/info
```

### Prometheus 메트릭 수집

#### 메트릭 엔드포인트
```bash
curl http://localhost:8080/actuator/prometheus
```

**Prometheus 설정 (`prometheus.yml`)**:
```yaml
scrape_configs:
  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-gateway:8080']
```

### Grafana 대시보드

**주요 모니터링 지표**:
- Request Rate (초당 요청 수)
- Response Time (P50, P95, P99)
- Error Rate (5xx 응답 비율)
- Circuit Breaker 상태
- JVM 메모리 사용률
- CPU 사용률

**Grafana 접속**:
```
URL: http://localhost:3000
ID: admin
PW: password
```

### Zipkin 분산 추적

#### Zipkin UI 접속
```
URL: http://localhost:9411
```

#### 트레이스 확인
1. Service Name: `api-gateway` 선택
2. 시간 범위 설정
3. Find Traces 클릭
4. 특정 트레이스 선택하여 상세 확인

**트레이스 예시**:
```
api-gateway → auth-service → database (200ms)
api-gateway → blog-service → mongodb (150ms)
```

---

## 5️⃣ 롤백 절차

### Docker Compose 롤백

#### Step 1: 이전 이미지 확인
```bash
docker images api-gateway
```

#### Step 2: docker-compose.yml에서 이미지 버전 변경
```yaml
services:
  api-gateway:
    image: api-gateway:previous-version
```

#### Step 3: 재배포
```bash
docker-compose up -d api-gateway
```

### Kubernetes 롤백

#### 자동 롤백 (이전 버전으로)
```bash
kubectl rollout undo deployment/api-gateway -n portal-universe
```

#### 특정 리비전으로 롤백
```bash
# 리비전 히스토리 확인
kubectl rollout history deployment/api-gateway -n portal-universe

# 특정 리비전으로 롤백
kubectl rollout undo deployment/api-gateway --to-revision=2 -n portal-universe
```

#### 롤백 상태 확인
```bash
kubectl rollout status deployment/api-gateway -n portal-universe
```

---

## 6️⃣ Circuit Breaker 상태 확인

API Gateway는 Resilience4j를 사용하여 다운스트림 서비스 장애를 처리합니다.

### Circuit Breaker 설정 확인

**application.yml**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      blogService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
```

### Circuit Breaker 상태 확인
```bash
# Actuator를 통한 Circuit Breaker 상태
curl http://localhost:8080/actuator/circuitbreakers

# Circuit Breaker 이벤트 확인
curl http://localhost:8080/actuator/circuitbreakerevents
```

**응답 예시**:
```json
{
  "circuitBreakers": {
    "blogService": {
      "state": "CLOSED",
      "failureRate": "0.0%",
      "bufferedCalls": 5,
      "failedCalls": 0
    }
  }
}
```

### Fallback 엔드포인트 테스트

#### Blog Service Fallback
```bash
# Blog Service가 다운된 경우 fallback 응답 확인
curl http://localhost:8080/api/v1/blog/posts
```

**Fallback 응답**:
```json
{
  "success": false,
  "code": "SERVICE_UNAVAILABLE",
  "message": "Blog service is temporarily unavailable",
  "data": null
}
```

#### Fallback 컨트롤러 경로
```bash
curl http://localhost:8080/fallback/blog
```

### Circuit Breaker 상태별 대응

| 상태 | 설명 | 조치 |
|------|------|------|
| **CLOSED** | 정상 | 모니터링 계속 |
| **OPEN** | 임계값 초과, 요청 차단 중 | 다운스트림 서비스 확인 |
| **HALF_OPEN** | 테스트 요청 허용 중 | 복구 여부 관찰 |

---

## 7️⃣ 장애 대응

### 서비스 불가 시 확인 사항

#### 1. 서비스 실행 상태 확인
```bash
# Docker
docker ps | grep api-gateway

# Kubernetes
kubectl get pods -n portal-universe -l app=api-gateway
```

#### 2. 로그 확인
```bash
# Docker
docker-compose logs --tail=100 api-gateway

# Kubernetes
kubectl logs -f deployment/api-gateway -n portal-universe --tail=100
```

**주요 에러 패턴**:
```
# Config Server 연결 실패
Could not locate PropertySource: I/O error on GET request for "http://config-service:8888"

# 인증 서버 연결 실패
Unable to resolve configuration from issuer http://auth-service:8081/.well-known/oauth-authorization-server

# 메모리 부족
java.lang.OutOfMemoryError: Java heap space
```

#### 3. 의존 서비스 확인
```bash
# Config Service 확인
curl http://config-service:8888/actuator/health

# Auth Service 확인
curl http://auth-service:8081/actuator/health

# Blog Service 확인
curl http://blog-service:8082/actuator/health
```

#### 4. 네트워크 연결 확인
```bash
# Docker
docker exec api-gateway ping config-service

# Kubernetes
kubectl exec -it deployment/api-gateway -n portal-universe -- curl http://config-service:8888/actuator/health
```

### 재시작 절차

#### Docker Compose
```bash
# Graceful restart
docker-compose restart api-gateway

# Force restart
docker-compose stop api-gateway
docker-compose up -d api-gateway
```

#### Kubernetes
```bash
# Pod 재시작
kubectl rollout restart deployment/api-gateway -n portal-universe

# 특정 Pod 강제 삭제 (자동 재생성됨)
kubectl delete pod <pod-name> -n portal-universe
```

### 긴급 조치

#### 1. Circuit Breaker 강제 Open (장애 격리)
```bash
# application.yml에서 강제 설정
resilience4j.circuitbreaker.instances.blogService.forceOpen: true
```

#### 2. 트래픽 제한
```bash
# Rate Limiter 임계값 낮춤
resilience4j.ratelimiter.instances.default.limitForPeriod: 10
```

#### 3. 스케일 다운 (리소스 부족 시)
```bash
# Kubernetes
kubectl scale deployment api-gateway --replicas=1 -n portal-universe
```

### 장애 유형별 대응

| 장애 유형 | 증상 | 조치 |
|----------|------|------|
| **Config 로드 실패** | 시작 불가 | config-service 먼저 재시작 |
| **JWT 검증 실패** | 401 Unauthorized | auth-service 연결 확인, 키 동기화 |
| **메모리 부족** | OOMKilled | JVM 힙 메모리 증가 (-Xmx) |
| **다운스트림 타임아웃** | 504 Gateway Timeout | Circuit Breaker 확인, 타임아웃 조정 |
| **포트 충돌** | Address already in use | 기존 프로세스 종료 |

---

## 📞 에스컬레이션

### 장애 레벨별 대응

| 레벨 | 상황 | 담당자 | 연락처 |
|------|------|--------|--------|
| **L1** | 서비스 재시작으로 해결 | 운영팀 | ops@portal.com |
| **L2** | Circuit Breaker 개입 필요 | 백엔드팀 | backend@portal.com |
| **L3** | 아키텍처 변경 필요 | Tech Lead | tech-lead@portal.com |

### 긴급 연락망
- **Slack**: #portal-universe-ops
- **PagerDuty**: api-gateway-incidents
- **On-call Engineer**: [담당자 로테이션 확인]

---

## 📚 관련 문서
- [API Gateway 아키텍처 설계](../architecture/overview.md)
- [API Gateway 장애 대응 가이드](../troubleshooting/)
- [Spring Cloud Gateway 공식 문서](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)
