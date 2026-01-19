# Docker Compose 배포 가이드

## 사전 요구사항

- Docker 20.10+
- Docker Compose 2.0+
- 16GB+ RAM 권장
- Git

## 빠른 시작

```bash
# 저장소 클론
git clone https://github.com/L-a-z-e/portal-universe.git
cd portal-universe

# 모든 서비스 시작
docker compose up -d

# 서비스 상태 확인
docker compose ps
```

## 서비스별 시작

### 인프라만 시작

```bash
docker compose up -d mysql-db mongodb redis kafka elasticsearch
```

### 모니터링 스택만 시작

```bash
docker compose up -d prometheus grafana zipkin loki promtail alertmanager
```

### 백엔드 서비스만 시작

```bash
docker compose up -d config-service api-gateway auth-service blog-service shopping-service notification-service
```

## 서비스 포트 매핑

### 백엔드 서비스

| 서비스 | 포트 | URL |
|--------|------|-----|
| API Gateway | 8080 | http://localhost:8080 |
| Config Service | 8888 | http://localhost:8888 |
| Auth Service | 8081 | http://localhost:8081 |
| Blog Service | 8082 | http://localhost:8082 |
| Shopping Service | 8083 | http://localhost:8083 |
| Notification Service | 8084 | http://localhost:8084 |

### 프론트엔드

| 서비스 | 포트 | URL |
|--------|------|-----|
| Portal Shell | 30000 | https://localhost:30000 |
| Blog Frontend | 30001 | http://localhost:30001 |

### 데이터 저장소

| 서비스 | 포트 | 설명 |
|--------|------|------|
| MySQL | 3307 | auth_db, shopping_db |
| MongoDB | 27017 | blog_db |
| Redis | 6379 | 캐싱, 세션 |
| Kafka | 9092 | 메시지 브로커 |
| Elasticsearch | 9200 | 검색 엔진 |

### 모니터링

| 서비스 | 포트 | 인증 정보 |
|--------|------|-----------|
| Prometheus | 9090 | - |
| Grafana | 3000 | admin/password |
| Zipkin | 9411 | - |
| Kibana | 5601 | - |
| Alertmanager | 9093 | - |
| Loki | 3100 | - |
| Dozzle | 9999 | - |

## 로그 확인

```bash
# 특정 서비스 로그
docker compose logs -f api-gateway

# 모든 서비스 로그 (최근 100줄)
docker compose logs --tail=100

# Dozzle UI로 로그 확인
open http://localhost:9999
```

## 빌드 및 재시작

```bash
# 모든 서비스 재빌드
docker compose build

# 특정 서비스 재빌드 및 재시작
docker compose up -d --build auth-service

# 전체 재시작
docker compose down && docker compose up -d
```

## 데이터 초기화

```bash
# 볼륨 포함 전체 삭제
docker compose down -v

# 특정 볼륨만 삭제
docker volume rm portal-universe_mysql-data
```

## 환경 변수

`.env` 파일을 생성하여 환경 변수를 설정합니다:

```bash
# OAuth2 (선택)
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
```

## 트러블슈팅

### 서비스가 시작되지 않는 경우

```bash
# 의존성 서비스 상태 확인
docker compose ps

# 헬스체크 상태 확인
docker inspect --format='{{json .State.Health}}' config-service

# config-service가 먼저 healthy 상태가 되어야 함
```

### 메모리 부족

```bash
# Docker 리소스 확인
docker stats

# 불필요한 서비스 중지
docker compose stop kibana alertmanager dozzle
```

### 포트 충돌

기존에 사용 중인 포트가 있으면 `docker compose.yml`에서 포트 매핑을 수정하세요.

## 참고

- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [프로젝트 README](../../README.md)
