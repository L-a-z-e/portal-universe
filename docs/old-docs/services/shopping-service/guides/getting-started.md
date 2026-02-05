---
id: guide-getting-started
title: Shopping Service Getting Started
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [guide, shopping-service, setup, environment]
---

# Getting Started

> shopping-service 개발 환경 설정 가이드

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 약 30분 |
| **대상** | shopping-service 개발자 |
| **서비스 포트** | 8083 |

---

## ✅ 사전 요구사항

### 필수 소프트웨어

| 소프트웨어 | 버전 | 확인 명령어 | 다운로드 |
|-----------|------|------------|----------|
| Java JDK | 17 | `java -version` | https://adoptium.net |
| Gradle | 8.x+ | `gradle --version` | (Gradle Wrapper 사용 가능) |
| Docker | 최신 | `docker --version` | https://docker.com/get-started |
| Docker Compose | 최신 | `docker-compose --version` | (Docker Desktop 포함) |
| MySQL Client | 8.0+ | `mysql --version` | https://dev.mysql.com/downloads/mysql |
| Git | 2.x+ | `git --version` | https://git-scm.com |

### 선택 사항

| 소프트웨어 | 용도 |
|-----------|------|
| IntelliJ IDEA | Java IDE (권장) |
| Postman | API 테스트 |
| DBeaver | 데이터베이스 GUI |

---

## 🔧 환경 설정

### Step 1: 저장소 클론

```bash
git clone https://github.com/L-a-z-e/portal-universe.git
cd portal-universe
```

### Step 2: 외부 의존성 실행 (Docker Compose)

shopping-service는 다음 외부 의존성이 필요합니다:
- MySQL (데이터베이스)
- Kafka (메시징)
- Zookeeper (Kafka 의존성)
- Config Service (설정 서버)

```bash
# 프로젝트 루트에서 실행
docker-compose up -d mysql kafka zookeeper config-service
```

**예상 결과**:
```
[+] Running 4/4
 ✔ Container portal-universe-zookeeper       Started
 ✔ Container portal-universe-mysql           Started
 ✔ Container portal-universe-kafka           Started
 ✔ Container portal-universe-config-service  Started
```

**서비스 확인**:
```bash
# MySQL 접속 확인
docker exec -it portal-universe-mysql mysql -uroot -proot -e "SHOW DATABASES;"

# Kafka 토픽 확인
docker exec -it portal-universe-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Step 3: 데이터베이스 생성

shopping-service는 `shopping_db` 데이터베이스를 사용합니다.

```bash
# MySQL 컨테이너 접속
docker exec -it portal-universe-mysql mysql -uroot -proot

# 데이터베이스 생성 (MySQL 프롬프트에서)
CREATE DATABASE IF NOT EXISTS shopping_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON shopping_db.* TO 'user'@'%';
FLUSH PRIVILEGES;
EXIT;
```

### Step 4: 환경 변수 설정 (선택)

로컬 개발 시 기본값을 사용하지만, 커스터마이징이 필요한 경우 환경 변수를 설정할 수 있습니다.

```bash
# .env 파일 생성 (프로젝트 루트)
cat <<EOF > .env
SPRING_PROFILES_ACTIVE=local
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=shopping_db
MYSQL_USERNAME=user
MYSQL_PASSWORD=password
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
CONFIG_SERVER_URL=http://localhost:8888
EOF
```

### Step 5: 빌드 및 실행

#### 방법 1: Gradle Wrapper 사용 (권장)

```bash
# 프로젝트 루트에서
./gradlew :services:shopping-service:build
./gradlew :services:shopping-service:bootRun
```

#### 방법 2: IntelliJ IDEA 사용

1. IntelliJ에서 프로젝트 열기
2. `services/shopping-service/src/main/java/com/portal/universe/shopping/ShoppingServiceApplication.java` 파일 열기
3. `main` 메서드 옆 실행 버튼 클릭
4. Run Configuration에서 Active profiles: `local` 설정

---

## ✅ 실행 확인

### 1. 서비스 헬스 체크

```bash
curl http://localhost:8083/actuator/health
```

**예상 결과**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### 2. API 엔드포인트 테스트

```bash
# 상품 목록 조회 (빈 배열 반환)
curl http://localhost:8083/api/v1/shopping/products
```

**예상 결과**:
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": []
}
```

### 3. 데이터베이스 마이그레이션 확인

Flyway가 자동으로 테이블을 생성했는지 확인:

```bash
docker exec -it portal-universe-mysql mysql -uuser -ppassword shopping_db -e "SHOW TABLES;"
```

**예상 결과**:
```
+------------------------+
| Tables_in_shopping_db  |
+------------------------+
| flyway_schema_history  |
| products               |
+------------------------+
```

---

## 🧪 테스트 실행

### 단위 테스트 실행

```bash
./gradlew :services:shopping-service:test
```

### 통합 테스트 실행 (Testcontainers)

```bash
./gradlew :services:shopping-service:integrationTest
```

통합 테스트는 자동으로 Docker 컨테이너를 실행하여 MySQL과 Kafka를 테스트합니다.

---

## ⚠️ 자주 발생하는 문제

### 1. Port already in use (8083)

**증상**:
```
Bind for 0.0.0.0:8083 failed: port is already allocated
```

**해결 방법**:
```bash
# 포트 사용 프로세스 확인
lsof -i :8083

# 프로세스 종료
kill -9 [PID]
```

### 2. MySQL Connection refused

**증상**:
```
Communications link failure
```

**해결 방법**:
```bash
# MySQL 컨테이너 상태 확인
docker ps | grep mysql

# MySQL 컨테이너 재시작
docker-compose restart mysql

# 로그 확인
docker logs portal-universe-mysql
```

### 3. Config Server 연결 실패

**증상**:
```
Could not locate PropertySource: I/O error on GET request for "http://localhost:8888"
```

**해결 방법**:
```bash
# Config Service 실행 확인
curl http://localhost:8888/actuator/health

# Config Service 시작
docker-compose up -d config-service

# 또는 로컬 프로필 사용하여 Config Server 우회
./gradlew :services:shopping-service:bootRun --args='--spring.profiles.active=local --spring.cloud.config.enabled=false'
```

### 4. Kafka broker not available

**증상**:
```
org.apache.kafka.common.errors.TimeoutException: Timeout expired while fetching topic metadata
```

**해결 방법**:
```bash
# Kafka 컨테이너 확인
docker-compose ps kafka

# Kafka 재시작
docker-compose restart kafka zookeeper

# Kafka 로그 확인
docker logs portal-universe-kafka
```

### 5. Flyway migration failed

**증상**:
```
FlywayException: Validate failed: Migrations have failed validation
```

**해결 방법**:
```bash
# 데이터베이스 초기화 (개발 환경만!)
docker exec -it portal-universe-mysql mysql -uroot -proot -e "DROP DATABASE shopping_db; CREATE DATABASE shopping_db;"

# 애플리케이션 재시작
./gradlew :services:shopping-service:bootRun
```

### 6. Lombok 관련 컴파일 에러

**증상**:
```
cannot find symbol: method builder()
```

**해결 방법**:
```bash
# IntelliJ IDEA:
# 1. Settings → Plugins → "Lombok" 플러그인 설치
# 2. Settings → Build, Execution, Deployment → Compiler → Annotation Processors → "Enable annotation processing" 체크

# Gradle 빌드 강제 재빌드
./gradlew clean build
```

---

## 🔍 개발 도구

### API Gateway를 통한 접근

shopping-service는 API Gateway (8080)를 통해서도 접근 가능합니다:

```bash
# API Gateway를 통한 요청 (인증 필요)
curl -H "Authorization: Bearer [JWT_TOKEN]" \
  http://localhost:8080/api/v1/shopping/products
```

### Prometheus 메트릭 확인

```bash
# Prometheus 메트릭 엔드포인트
curl http://localhost:8083/actuator/prometheus
```

### Zipkin 분산 추적

Zipkin UI에서 shopping-service 트레이스 확인:
- URL: http://localhost:9411/zipkin
- Service Name: `shopping-service`

---

## 📊 모니터링

### Grafana 대시보드

1. Grafana 접속: http://localhost:3000
2. 로그인: admin / password
3. Dashboard → Spring Boot 2.1 Statistics 선택
4. Service 필터에서 `shopping-service` 선택

### 로그 확인

```bash
# 애플리케이션 로그 (IntelliJ 콘솔 또는)
tail -f logs/shopping-service.log

# Docker 환경에서
docker logs -f portal-universe-shopping-service
```

---

## ➡️ 다음 단계

1. **API 문서 확인**: [API 명세서](../api/)에서 사용 가능한 엔드포인트 확인
2. **아키텍처 이해**: [Architecture 문서](../architecture/)에서 서비스 구조 학습
3. **개발 시작**: 새로운 기능 추가 또는 버그 수정
4. **테스트 작성**: [Testing 가이드](../testing/)를 참고하여 테스트 작성

---

## 🔗 관련 문서

- [API 명세서](../api/)
- [Architecture 문서](../architecture/)
- [Troubleshooting](../troubleshooting/)
- [Runbooks](../runbooks/)

---

## 📞 도움이 필요하면

| 채널 | 용도 |
|------|------|
| GitHub Issues | 버그 리포트, 기능 제안 |
| Slack #shopping-service | 개발 관련 질문 |
| Confluence | 상세 문서 및 위키 |

---

**최종 업데이트**: 2026-01-18
