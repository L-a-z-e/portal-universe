# Docker Compose

멀티 컨테이너 애플리케이션을 정의하고 실행하는 도구를 학습합니다.

---

## 1. Docker Compose 개요

### 왜 Docker Compose인가?

단일 컨테이너 실행:
```bash
# 하나씩 실행해야 함
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=secret mysql:8.0
docker run -d --name redis redis:7-alpine
docker run -d --name app --link mysql --link redis myapp
```

Docker Compose 사용:
```bash
# 한 번에 모든 서비스 실행
docker-compose up -d
```

### Compose 파일 기본 구조

```yaml
services:       # 컨테이너 정의
  app:
    ...
  db:
    ...

volumes:        # 영구 데이터 저장소
  db-data:

networks:       # 컨테이너 간 통신
  app-network:
```

---

## 2. Portal Universe docker-compose.yml 분석

### 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│                    portal-universe-net (Bridge Network)              │
│                                                                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │
│  │   mysql-db  │  │   mongodb   │  │    redis    │  │   kafka    │ │
│  │    :3306    │  │   :27017    │  │    :6379    │  │   :9092    │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └─────┬──────┘ │
│         │                │                │                │        │
│  ┌──────▼──────────────────────────────────────────────────▼──────┐ │
│  │                        api-gateway :8080                        │ │
│  └──────┬──────────────────────────────────────────────────┬──────┘ │
│         │                                                  │        │
│  ┌──────▼──────┐  ┌──────────────┐  ┌────────────┐  ┌─────▼──────┐ │
│  │auth-service │  │ blog-service │  │  shopping  │  │notification│ │
│  │   :8081     │  │    :8082     │  │   :8083    │  │   :8084    │ │
│  └─────────────┘  └──────────────┘  └────────────┘  └────────────┘ │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │               Monitoring Stack                                   ││
│  │  prometheus:9090 │ grafana:3000 │ loki:3100 │ zipkin:9411       ││
│  └─────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────┘
```

### Infrastructure Services

```yaml
services:
  # ========================================
  # Database Services
  # ========================================
  mysql-db:
    image: mysql:8.0
    container_name: mysql-db
    ports:
      - "3307:3306"              # 호스트:컨테이너 포트 매핑
    volumes:
      - mysql-data:/var/lib/mysql                                    # Named Volume
      - ./infrastructure/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql  # 초기화 스크립트
    env_file:
      - .env.docker              # 환경 변수 파일
    environment:
      - MYSQL_ROOT_HOST=%        # 원격 접속 허용
    networks:
      - portal-universe-net

  mongodb:
    image: mongo:latest
    container_name: mongodb
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db
    env_file:
      - .env.docker
    networks:
      - portal-universe-net

  redis:
    image: redis:7-alpine
    container_name: redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes   # AOF 영속성 활성화
    volumes:
      - redis-data:/data
    networks:
      - portal-universe-net
    healthcheck:                              # 헬스 체크 설정
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### Kafka Configuration

```yaml
  kafka:
    image: apache/kafka:4.1.0
    container_name: kafka
    ports:
      - "9092:9092"              # 외부 접근용
      - "29092:29092"            # 내부 컨테이너 간 통신
      - "9093:9093"              # Controller 통신
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller   # KRaft 모드 (Zookeeper 불필요)
      KAFKA_LISTENERS: PLAINTEXT://:29092,PLAINTEXT_HOST://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_CLUSTER_ID: "5L6g3nShT-eMCtK--X86sw"
    networks:
      - portal-universe-net
```

**Kafka Listener 설명:**

| Listener | 용도 | 접근 주소 |
|----------|------|----------|
| `PLAINTEXT://kafka:29092` | 컨테이너 간 통신 | Docker 네트워크 내부 |
| `PLAINTEXT_HOST://localhost:9092` | 로컬 개발 접근 | 호스트 머신 |
| `CONTROLLER://:9093` | KRaft Controller 통신 | Kafka 내부 |

---

## 3. Application Services

### Service Dependencies

```yaml
  auth-service:
    build:
      context: .
      dockerfile: ./services/auth-service/Dockerfile
    container_name: auth-service
    ports:
      - "8081:8081"
    env_file:
      - path: ./.env.docker
        required: true
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - REDIS_HOST=redis
      - REDIS_PORT=6379
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
    depends_on:                              # 의존성 순서 정의
      mysql-db:
        condition: service_started
      kafka:
        condition: service_started
      redis:
        condition: service_healthy           # 헬스체크 통과 후 시작
    networks:
      - portal-universe-net
```

### depends_on 조건

| 조건 | 설명 |
|------|------|
| `service_started` | 컨테이너가 시작되면 바로 진행 |
| `service_healthy` | healthcheck가 성공할 때까지 대기 |
| `service_completed_successfully` | 컨테이너가 성공적으로 종료될 때까지 대기 |

### Build Context

```yaml
  blog-service:
    build:
      context: .                             # 빌드 컨텍스트 (루트 디렉토리)
      dockerfile: ./services/blog-service/Dockerfile
    depends_on:
      api-gateway:
        condition: service_healthy           # API Gateway 준비 후 시작
      mongodb:
        condition: service_started
```

---

## 4. Network Configuration

### Bridge Network

```yaml
networks:
  portal-universe-net:           # 기본 bridge 네트워크
```

### 네트워크 통신 방식

```
┌──────────────────────────────────────────────────────────┐
│                   portal-universe-net                     │
│                                                          │
│   ┌─────────────┐       DNS Resolution       ┌─────────┐│
│   │ auth-service│ ──────────────────────────►│ mysql-db││
│   │             │   mysql-db:3306            │  :3306  ││
│   └─────────────┘                            └─────────┘│
│                                                          │
│   컨테이너 이름 = DNS 호스트명                            │
└──────────────────────────────────────────────────────────┘
```

**서비스 간 통신 예시:**

```yaml
# auth-service의 application-docker.yml
spring:
  datasource:
    url: jdbc:mysql://mysql-db:3306/auth_db   # 컨테이너 이름으로 접근
  data:
    redis:
      host: redis                              # 컨테이너 이름
      port: 6379
```

---

## 5. Volume Management

### Volume 종류

```yaml
volumes:
  # Named Volume (Docker가 관리)
  mysql-data:
  mongo-data:
  redis-data:
  prometheus-data:
  grafana-data:
  loki-data:

services:
  mysql-db:
    volumes:
      # Named Volume 마운트
      - mysql-data:/var/lib/mysql

      # Bind Mount (호스트 경로 직접 지정)
      - ./infrastructure/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql
```

### Volume 유형 비교

| 유형 | 장점 | 단점 | 사용 사례 |
|------|------|------|----------|
| Named Volume | Docker가 관리, 백업 용이 | 호스트에서 직접 수정 어려움 | 데이터베이스 |
| Bind Mount | 호스트에서 직접 수정 가능 | 경로 의존성 | 설정 파일, 개발 코드 |
| tmpfs | 메모리 기반, 빠름 | 컨테이너 종료 시 삭제 | 임시 파일 |

### 볼륨 관리 명령어

```bash
# 볼륨 목록
docker volume ls

# 볼륨 상세 정보
docker volume inspect mysql-data

# 미사용 볼륨 정리
docker volume prune

# 특정 볼륨 삭제
docker volume rm mysql-data
```

---

## 6. Environment Variables

### 환경 변수 설정 방법

```yaml
services:
  auth-service:
    # 방법 1: 직접 정의
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - REDIS_HOST=redis

    # 방법 2: 파일에서 로드
    env_file:
      - .env.docker

    # 방법 3: 호스트 환경 변수 참조
    environment:
      - DATABASE_URL=${DATABASE_URL}
```

### .env.docker 예시

```bash
# .env.docker
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=portal_universe
MYSQL_USER=appuser
MYSQL_PASSWORD=apppassword

MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=rootpassword

GF_SECURITY_ADMIN_USER=admin
GF_SECURITY_ADMIN_PASSWORD=admin123
```

---

## 7. Health Checks

### Spring Boot Actuator 헬스 체크

```yaml
  api-gateway:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 10s        # 체크 간격
      timeout: 5s          # 타임아웃
      retries: 5           # 재시도 횟수
      start_period: 30s    # 시작 대기 시간 (선택)
```

### 다양한 헬스 체크 방식

```yaml
# Redis
healthcheck:
  test: ["CMD", "redis-cli", "ping"]

# Elasticsearch
healthcheck:
  test: ["CMD-SHELL", "curl -s http://localhost:9200/_cluster/health | grep -q 'green\\|yellow'"]

# PostgreSQL
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U postgres"]

# MySQL
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
```

---

## 8. Monitoring Stack

```yaml
  # ========================================
  # Monitoring Stack
  # ========================================
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    volumes:
      - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./monitoring/prometheus/rules:/etc/prometheus/rules
      - prometheus-data:/prometheus
    ports:
      - "9090:9090"
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=30d'
      - '--web.enable-lifecycle'

  grafana:
    image: grafana/grafana-oss:latest
    environment:
      - GF_USERS_ALLOW_SIGN_UP=false
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/grafana/provisioning:/etc/grafana/provisioning
    depends_on:
      - prometheus
      - loki

  loki:
    image: grafana/loki:2.9.0
    volumes:
      - ./monitoring/loki/loki-config.yml:/etc/loki/local-config.yaml
      - loki-data:/loki
    command: -config.file=/etc/loki/local-config.yaml

  promtail:
    image: grafana/promtail:2.9.0
    volumes:
      - ./monitoring/promtail/promtail-config.yml:/etc/promtail/config.yml
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock
    depends_on:
      - loki

  zipkin:
    image: openzipkin/zipkin:latest
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem
```

---

## 9. Docker Compose 명령어

### 기본 명령어

```bash
# 모든 서비스 시작 (백그라운드)
docker-compose up -d

# 특정 서비스만 시작
docker-compose up -d auth-service blog-service

# 서비스 중지
docker-compose stop

# 서비스 중지 및 컨테이너 삭제
docker-compose down

# 볼륨까지 삭제
docker-compose down -v

# 이미지 재빌드 후 시작
docker-compose up -d --build

# 특정 서비스 재시작
docker-compose restart auth-service
```

### 로그 및 모니터링

```bash
# 모든 서비스 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f auth-service

# 마지막 100줄
docker-compose logs --tail=100 auth-service

# 서비스 상태 확인
docker-compose ps

# 리소스 사용량
docker-compose top
```

### 스케일링

```bash
# 서비스 인스턴스 수 조정
docker-compose up -d --scale auth-service=3

# 주의: 포트 충돌 방지를 위해 포트 범위 설정 필요
# ports:
#   - "8081-8083:8081"
```

---

## 10. 실습 예제

### 개발 환경 구성

```yaml
# docker-compose.dev.yml
services:
  app:
    build:
      context: .
      target: development          # Multi-stage build의 특정 stage
    volumes:
      - .:/app                     # 소스 코드 마운트 (핫 리로드)
      - /app/node_modules          # node_modules 제외
    environment:
      - NODE_ENV=development
    command: npm run dev
```

```bash
# 개발 환경 실행
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

### Profile 활용 (Compose v2)

```yaml
# docker-compose.yml
services:
  app:
    # 항상 실행

  debug-tools:
    profiles: ["debug"]            # debug 프로필에서만 실행
    image: debug-tools:latest
```

```bash
# 기본 실행
docker-compose up -d

# debug 프로필 포함 실행
docker-compose --profile debug up -d
```

---

## 11. 관련 문서

- [Docker Fundamentals](./docker-fundamentals.md) - Docker 기초
- [Prometheus & Grafana](./prometheus-grafana.md) - 메트릭 수집 및 시각화
- [Loki Logging](./loki-logging.md) - 로그 수집
- [Portal Universe Infra Guide](./portal-universe-infra-guide.md) - 전체 인프라 가이드
