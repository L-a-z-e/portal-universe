---
id: local-dev-setup
title: 로컬 개발 환경 구성 가이드
type: guide
status: current
created: 2026-02-05
updated: 2026-02-06
author: Laze
tags: [local-development, setup, docker, guide]
---

# 로컬 개발 환경 구성 가이드

**난이도**: ⭐⭐ | **예상 시간**: 30분 | **카테고리**: Development

## 개요
Portal Universe 프로젝트를 로컬 환경에서 실행하기 위한 전체 구성 가이드입니다.
인프라 컨테이너 실행부터 백엔드/프론트엔드 서비스 기동까지 Zero to Running 과정을 다룹니다.

---

## 사전 요구사항

### 필수 도구
- [ ] Java 17 (JDK)
- [ ] Node.js 20+
- [ ] Python 3.11+ (chatbot-service 실행 시에만 필요)
- [ ] Docker & Docker Compose

### 필수 지식
- 기본적인 터미널 명령어
- Docker 컨테이너 개념
- 마이크로서비스 아키텍처 이해

---

## 단계별 실행

### Step 1: 인프라 실행
로컬 개발에 필요한 데이터베이스, 메시지 큐, 캐시 서버를 Docker로 실행합니다.

```bash
# 프로젝트 루트에서 실행
docker compose -f docker-compose-local.yml up -d
```

**실행되는 인프라 서비스**:
- MySQL: 3307
- PostgreSQL: 5432
- MongoDB: 27017
- Redis: 6379
- Kafka: 9092
- Elasticsearch: 9200

**확인**:
```bash
docker compose -f docker-compose-local.yml ps
```

**예상 출력**:
```
NAME                STATUS              PORTS
mysql               Up                  0.0.0.0:3307->3306/tcp
postgres            Up                  0.0.0.0:5432->5432/tcp
mongodb             Up                  0.0.0.0:27017->27017/tcp
redis               Up                  0.0.0.0:6379->6379/tcp
kafka               Up                  0.0.0.0:9092->9092/tcp
elasticsearch       Up                  0.0.0.0:9200->9200/tcp
```

**주의사항**:
- ⚠️ Kafka는 완전히 시작되는 데 10-20초가 소요됩니다. 백엔드 서비스 실행 전 충분히 대기하세요.

---

### Step 2: 백엔드 서비스 실행
각 서비스 디렉토리에서 개별적으로 실행합니다.

#### 권장 실행 순서
1. api-gateway (8080)
2. auth-service (8081)
3. 나머지 서비스 (순서 무관)

#### Java/Spring Boot 서비스
api-gateway, auth-service, blog-service, shopping-service, notification-service

```bash
# 각 서비스 디렉토리에서 실행 (예: services/api-gateway)
cd services/api-gateway
./gradlew bootRun --args='--spring.profiles.active=local'
```

**필수**: `--spring.profiles.active=local` 플래그를 반드시 포함해야 합니다. 미지정 시 `application-local.yml`이 로드되지 않아 DB 연결이 실패합니다.

**확인**:
```bash
curl http://localhost:8080/actuator/health
```

**예상 출력**:
```json
{"status":"UP"}
```

#### NestJS 서비스 (prism-service)
```bash
cd services/prism-service
npm run start:dev
```

- `.env.local` 파일이 이미 설정되어 있어야 합니다 (DB, Kafka, Redis, Encryption Key 포함).
- Watch 모드로 실행되며 코드 변경 시 자동 재시작됩니다.

**확인**:
```bash
curl http://localhost:8085/health
```

#### Python 서비스 (chatbot-service)
```bash
cd services/chatbot-service
uvicorn app.main:app --reload --port 8086
```

- `.env` 파일 설정 필요 (`.env.example` 참조하여 AI provider 설정).
- `--reload` 옵션으로 코드 변경 시 자동 재시작됩니다.

**확인**:
```bash
curl http://localhost:8086/health
```

---

### Step 3: 프론트엔드 빌드 및 실행
`frontend/` 디렉토리에서 디자인 시스템과 공유 라이브러리를 먼저 빌드한 후 앱을 실행합니다.

```bash
cd frontend

# Step 3-1: 디자인 시스템 빌드 (최초 1회 필수)
npm run build:design

# Step 3-2: React 공유 라이브러리 빌드
npm run build:libs

# Step 3-3: 모든 프론트엔드 앱 동시 실행
npm run dev
```

**개별 실행 (필요 시)**:
```bash
npm run dev:portal    # Portal Shell (:30000)
npm run dev:blog      # Blog (:30001)
npm run dev:shopping  # Shopping (:30002)
npm run dev:prism     # Prism (:30003)
```

**주의사항**:
- ⚠️ `build:design`과 `build:libs`를 건너뛰고 `dev`를 실행하면 Module Federation 에러(remoteEntry.js 404)가 발생합니다.
- ⚠️ Vite dev 서버는 코드 변경 시 자동 리로드되므로, 설정/의존성 변경이 아닌 한 재시작이 불필요합니다.

**확인**:
브라우저에서 `http://localhost:30000` 접속하여 Portal Shell이 정상적으로 로드되는지 확인합니다.

---

### Step 4: 환경변수 확인
주요 서비스의 환경변수 파일이 이미 설정되어 있는지 확인합니다.

#### 이미 설정된 파일
- `services/auth-service/.env.local` - OAuth2 client ID/Secret
- `services/prism-service/.env.local` - DB, Kafka, Redis, Encryption Key

#### 추가 설정 필요 (chatbot-service 사용 시)
```bash
cd services/chatbot-service
# .env.example을 참조하여 .env 파일 생성 및 AI provider 설정
```

**주의사항**:
- ⚠️ 환경변수 파일은 `.gitignore`에 포함되어 있어 Git에 커밋되지 않습니다.
- ⚠️ `.env.example`을 그대로 복사하지 말고, 실제 값으로 대체하세요.

---

## 검증

### 최종 확인 체크리스트
- [ ] 인프라 컨테이너 6개가 모두 Up 상태
- [ ] API Gateway (8080) health check 응답
- [ ] Auth Service (8081) health check 응답
- [ ] 기타 백엔드 서비스 health check 응답
- [ ] Portal Shell (30000) 브라우저 접속 성공
- [ ] 브라우저 콘솔에 remoteEntry.js 404 에러 없음

### 전체 서비스 포트 확인
```bash
# 실행 중인 포트 목록 확인
lsof -i :8080,8081,8082,8083,8084,8085,8086,30000,30001,30002,30003
```

**예상 결과**:
모든 포트에서 Java, Node, Python, Vite 프로세스가 LISTEN 상태로 표시되어야 합니다.

---

## 문제 해결

### 자주 발생하는 문제

#### 문제 1: 포트 충돌 에러
**증상**: `Address already in use` 에러 발생

**원인**: 이전 실행이 종료되지 않았거나 다른 프로세스가 포트를 사용 중입니다.

**해결**:
```bash
# 해당 포트를 사용 중인 프로세스 확인
lsof -i :8080

# 프로세스 종료 (PID 확인 후)
kill -9 [PID]
```

#### 문제 2: DB 연결 실패 (Connection refused)
**증상**: 백엔드 서비스 실행 시 `Connection refused` 또는 `Cannot connect to database` 에러

**원인**:
1. Docker 인프라가 실행되지 않았거나 아직 준비되지 않음
2. `--spring.profiles.active=local` 플래그 누락

**해결**:
```bash
# 인프라 상태 확인
docker compose -f docker-compose-local.yml ps

# 인프라가 Down 상태라면 재시작
docker compose -f docker-compose-local.yml up -d

# Spring Boot 실행 시 반드시 profile 지정
./gradlew bootRun --args='--spring.profiles.active=local'
```

#### 문제 3: remoteEntry.js 404 에러 (Module Federation)
**증상**: Portal Shell 브라우저 콘솔에 `http://localhost:30002/remoteEntry.js 404` 에러

**원인**: 프론트엔드 빌드 순서를 지키지 않았거나 Remote 앱이 실행되지 않음

**해결**:
```bash
cd frontend

# 디자인 시스템과 라이브러리 재빌드
npm run build:design
npm run build:libs

# 앱 재실행
npm run dev
```

#### 문제 4: Kafka 연결 실패
**증상**: 백엔드 서비스 로그에 `TimeoutException: Topic creation timed out`

**원인**: Kafka가 완전히 시작되지 않은 상태에서 서비스 실행

**해결**:
1. Kafka 컨테이너 로그 확인:
```bash
docker logs kafka
```

2. `started (kafka.server.KafkaServer)` 메시지를 확인한 후 백엔드 서비스 재시작

#### 문제 5: 빌드 후 서비스가 이전 코드로 실행됨
**증상**: 코드 수정 후 빌드했으나 변경사항이 반영되지 않음

**원인**: 기존 프로세스가 종료되지 않고 계속 실행 중

**해결**:
1. 기존 프로세스를 Ctrl+C 또는 `kill` 명령으로 완전히 종료
2. 이 가이드에 정의된 실행 명령어로 재시작 (자체 명령어 사용 금지)

---

## 다음 단계

이 가이드를 완료했다면 아래 참고 자료를 확인하세요.

---

## 참고 자료
- [실행 가이드 상세 규칙](../../../.claude/rules/execution.md)
- [서비스 진단 스킬](../../../.claude/skills/service-diagnostics.md)
- [Module Federation 트러블슈팅](../../../.claude/skills/module-federation.md)

---

작성자: Laze
