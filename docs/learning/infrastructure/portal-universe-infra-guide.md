# Portal Universe Infrastructure Guide

Portal Universe 프로젝트의 전체 인프라 구성 및 운영 가이드입니다.

---

## 1. 인프라 개요

### 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Portal Universe Infrastructure                      │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                              Frontend Layer                              ││
│  │  ┌───────────────┐  ┌───────────────┐  ┌───────────────────────────┐   ││
│  │  │ portal-shell  │  │ blog-frontend │  │    shopping-frontend      │   ││
│  │  │   (Vue 3)     │  │   (Vue 3)     │  │       (React 18)          │   ││
│  │  │   :30000      │  │   :30001      │  │        :30002             │   ││
│  │  └───────┬───────┘  └───────┬───────┘  └────────────┬──────────────┘   ││
│  └──────────┼──────────────────┼───────────────────────┼────────────────────┘│
│             │                  │                       │                     │
│             └──────────────────┼───────────────────────┘                     │
│                                ▼                                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                           API Gateway :8080                              ││
│  │                        (Spring Cloud Gateway)                            ││
│  └────────────────────────────────┬────────────────────────────────────────┘│
│                                   │                                          │
│  ┌────────────────────────────────┼────────────────────────────────────────┐│
│  │                         Backend Services                                 ││
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  ││
│  │  │ auth-service │  │ blog-service │  │   shopping   │  │notification│  ││
│  │  │    :8081     │  │    :8082     │  │    :8083     │  │   :8084    │  ││
│  │  │  (OAuth2)    │  │  (MongoDB)   │  │   (MySQL)    │  │  (Kafka)   │  ││
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └────────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                         Data & Messaging Layer                           ││
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ ││
│  │  │  MySQL   │  │ MongoDB  │  │  Redis   │  │  Kafka   │  │   ES     │ ││
│  │  │  :3307   │  │  :27017  │  │  :6379   │  │  :9092   │  │  :9200   │ ││
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘ ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                         Monitoring Stack                                 ││
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ ││
│  │  │Prometheus│  │ Grafana  │  │  Loki    │  │ Promtail │  │  Zipkin  │ ││
│  │  │  :9090   │  │  :3000   │  │  :3100   │  │  Agent   │  │  :9411   │ ││
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘ ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                         Development Tools                                ││
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐                              ││
│  │  │LocalStack│  │  Dozzle  │  │  Kibana  │                              ││
│  │  │  :4566   │  │  :9999   │  │  :5601   │                              ││
│  │  └──────────┘  └──────────┘  └──────────┘                              ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 환경별 구성

### 환경 비교

| 구성 요소 | Local | Docker Compose | Kubernetes |
|----------|-------|----------------|------------|
| Frontend | npm run dev | Nginx Container | K8s Deployment |
| Backend | ./gradlew bootRun | Docker Container | K8s Deployment |
| Database | Docker | Docker | K8s StatefulSet |
| Ingress | - | Nginx (portal-shell) | NGINX Ingress |

### 실행 방법

#### Local 개발

```bash
# 인프라만 Docker로 실행
docker-compose up -d mysql-db mongodb redis kafka elasticsearch

# Backend 서비스 실행
cd services/auth-service && ./gradlew bootRun

# Frontend 실행
cd frontend/portal-shell && npm run dev
```

#### Docker Compose

```bash
# 전체 서비스 실행
docker-compose up -d

# 특정 서비스만 실행
docker-compose up -d mysql-db redis kafka auth-service

# 로그 확인
docker-compose logs -f auth-service
```

#### Kubernetes (Kind)

```bash
# 클러스터 생성
kind create cluster --config k8s/base/kind-config.yaml

# Namespace 생성
kubectl apply -f k8s/base/namespace.yaml

# Infrastructure 배포
kubectl apply -f k8s/infrastructure/

# Services 배포
kubectl apply -f k8s/services/
```

---

## 3. 포트 매핑

### Docker Compose 포트

| 서비스 | 내부 포트 | 외부 포트 | 설명 |
|--------|----------|----------|------|
| portal-shell | 443 | 30000 | HTTPS Frontend |
| blog-frontend | 80 | 30001 | Blog Frontend |
| shopping-frontend | 80 | 30002 | Shopping Frontend |
| api-gateway | 8080 | 8080 | API Gateway |
| auth-service | 8081 | 8081 | Auth Service |
| blog-service | 8082 | 8082 | Blog Service |
| shopping-service | 8083 | 8083 | Shopping Service |
| notification-service | 8084 | 8084 | Notification Service |
| mysql-db | 3306 | 3307 | MySQL (호스트 충돌 방지) |
| mongodb | 27017 | 27017 | MongoDB |
| redis | 6379 | 6379 | Redis |
| kafka | 9092 | 9092 | Kafka (외부) |
| elasticsearch | 9200 | 9200 | Elasticsearch |
| prometheus | 9090 | 9090 | Prometheus |
| grafana | 3000 | 3000 | Grafana |
| loki | 3100 | 3100 | Loki |
| zipkin | 9411 | 9411 | Zipkin |
| localstack | 4566 | 4566 | LocalStack |

### Kubernetes 접근

```bash
# /etc/hosts 설정
127.0.0.1 portal-universe

# 접근 URL
http://portal-universe/              # Frontend
http://portal-universe/api/          # API Gateway
http://portal-universe/grafana/      # Grafana
http://portal-universe/prometheus/   # Prometheus
http://portal-universe/zipkin/       # Zipkin
```

---

## 4. 서비스별 데이터 저장소

### 데이터베이스 매핑

| 서비스 | Primary DB | Cache | Search | Message Queue |
|--------|-----------|-------|--------|---------------|
| auth-service | MySQL | Redis | - | Kafka (Producer) |
| blog-service | MongoDB | - | - | - |
| shopping-service | MySQL | Redis | Elasticsearch | Kafka (Producer/Consumer) |
| notification-service | MySQL | Redis | - | Kafka (Consumer) |

### 데이터베이스 스키마

```
MySQL (auth_db)
├── users
├── roles
├── user_roles
└── oauth2_authorization

MySQL (shopping_db)
├── products
├── categories
├── orders
├── order_items
└── inventory

MySQL (notification_db)
├── notifications
├── notification_templates
└── notification_logs

MongoDB (blog_db)
├── posts
├── comments
└── categories
```

---

## 5. 환경 변수 관리

### .env.docker 예시

```bash
# Database
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=portal_universe
MYSQL_USER=appuser
MYSQL_PASSWORD=apppassword

# MongoDB
MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=rootpassword

# Grafana
GF_SECURITY_ADMIN_USER=admin
GF_SECURITY_ADMIN_PASSWORD=admin123

# JWT
JWT_SECRET=your-secret-key-here
```

### Kubernetes Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: portal-universe-secret
  namespace: portal-universe
type: Opaque
data:
  MYSQL_PASSWORD: base64_encoded_password
  JWT_SECRET: base64_encoded_secret
```

---

## 6. 네트워크 설정

### Docker Network

```yaml
networks:
  portal-universe-net:
    driver: bridge
```

모든 서비스는 `portal-universe-net` 네트워크에서 컨테이너 이름으로 통신합니다.

```
auth-service → mysql-db:3306
blog-service → mongodb:27017
shopping-service → redis:6379
notification-service → kafka:29092
```

### Kubernetes Network

```yaml
# ClusterIP Service로 내부 통신
auth-service.portal-universe.svc.cluster.local:8081

# Ingress로 외부 트래픽 라우팅
portal-universe/ → portal-shell
portal-universe/api/ → api-gateway
```

---

## 7. 모니터링 스택

### Observability 3가지 축

| 축 | 도구 | 용도 |
|---|------|------|
| Metrics | Prometheus + Grafana | 성능 지표, 알림 |
| Logs | Loki + Promtail | 로그 수집, 분석 |
| Traces | Zipkin | 분산 추적 |

### 데이터 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│  Application                                                     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Spring Boot Actuator                                      │  │
│  │  - /actuator/prometheus → Prometheus                      │  │
│  │  - /actuator/health → Health Check                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Micrometer Tracing                                        │  │
│  │  - TraceId/SpanId → Zipkin                                │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Logback                                                   │  │
│  │  - JSON Logs → Docker → Promtail → Loki                   │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Grafana 대시보드 접근

| 대시보드 | URL | 용도 |
|---------|-----|------|
| JVM Metrics | /grafana/d/jvm | JVM 메모리, GC |
| Spring Boot | /grafana/d/spring | HTTP 요청, 에러율 |
| Loki Logs | /grafana/explore | 로그 검색 |

---

## 8. CI/CD 배포 흐름

### 이미지 빌드 및 배포

```bash
# 1. 이미지 빌드
docker build -t portal-universe-auth-service:v1.0.2 \
  -f services/auth-service/Dockerfile .

# 2. Kind 클러스터에 로드 (로컬 개발)
kind load docker-image portal-universe-auth-service:v1.0.2 \
  --name portal-universe

# 3. Deployment 업데이트
kubectl set image deployment/auth-service \
  auth-service=portal-universe-auth-service:v1.0.2 \
  -n portal-universe

# 4. 롤아웃 상태 확인
kubectl rollout status deployment/auth-service -n portal-universe
```

### 배포 전략

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

---

## 9. 트러블슈팅

### 일반적인 문제 해결

#### 서비스가 시작되지 않음

```bash
# Docker Compose
docker-compose logs auth-service

# Kubernetes
kubectl logs -f deployment/auth-service -n portal-universe
kubectl describe pod -l app=auth-service -n portal-universe
```

#### 데이터베이스 연결 실패

```bash
# MySQL 연결 테스트
docker exec -it mysql-db mysql -u root -p

# Redis 연결 테스트
docker exec -it redis redis-cli ping
```

#### 네트워크 연결 문제

```bash
# Docker 네트워크 확인
docker network inspect portal-universe_portal-universe-net

# Kubernetes 서비스 확인
kubectl get svc -n portal-universe
kubectl get endpoints -n portal-universe
```

### 유용한 디버깅 명령어

```bash
# 전체 상태 확인
docker-compose ps
kubectl get all -n portal-universe

# 리소스 사용량
docker stats
kubectl top pods -n portal-universe

# 이벤트 확인
kubectl get events -n portal-universe --sort-by='.lastTimestamp'
```

---

## 10. 보안 고려사항

### 환경별 보안 설정

| 항목 | 개발 환경 | 프로덕션 권장 |
|------|----------|--------------|
| DB 비밀번호 | .env 파일 | Secret Manager |
| JWT Secret | 환경변수 | Vault |
| TLS | 자체 서명 | 공인 인증서 |
| Network Policy | 없음 | 적용 필수 |

### 접근 제어

```yaml
# Network Policy 예시
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: backend-isolation
spec:
  podSelector:
    matchLabels:
      tier: backend
  ingress:
    - from:
        - podSelector:
            matchLabels:
              tier: gateway
```

---

## 11. 관련 문서 링크

### 기초
- [Docker Fundamentals](./docker-fundamentals.md)
- [Docker Compose](./docker-compose.md)
- [Kubernetes Fundamentals](./kubernetes-fundamentals.md)

### 네트워킹
- [Kubernetes Networking](./kubernetes-networking.md)
- [Kubernetes Kind Cluster](./kubernetes-kind-cluster.md)

### 모니터링
- [Prometheus & Grafana](./prometheus-grafana.md)
- [Zipkin Tracing](./zipkin-tracing.md)
- [Loki Logging](./loki-logging.md)

### 개발 도구
- [LocalStack S3](./localstack-s3.md)

---

## 12. 빠른 시작 가이드

### 처음 시작하는 경우

```bash
# 1. 저장소 클론
git clone https://github.com/your-org/portal-universe.git
cd portal-universe

# 2. 환경 변수 설정
cp .env.docker.example .env.docker

# 3. 전체 서비스 실행
docker-compose up -d

# 4. 상태 확인
docker-compose ps

# 5. 접속
open https://localhost:30000  # Frontend
open http://localhost:3000    # Grafana
open http://localhost:9411    # Zipkin
```

### 개발 환경 구성

```bash
# 인프라만 실행
docker-compose up -d mysql-db mongodb redis kafka elasticsearch

# Backend 개발
cd services/auth-service
./gradlew bootRun --args='--spring.profiles.active=local'

# Frontend 개발
cd frontend/portal-shell
npm install
npm run dev
```
