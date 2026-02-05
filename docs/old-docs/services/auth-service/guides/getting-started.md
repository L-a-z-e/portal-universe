---
id: guide-getting-started
title: Auth Service Getting Started
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [setup, environment, auth-service]
related:
  - arch-system-overview
---

# Auth Service Getting Started

## 📋 개요

이 가이드는 Auth Service를 로컬 개발 환경에서 실행하는 방법을 안내합니다.

**예상 소요 시간**: 15-20분
**대상 독자**: 백엔드 개발자, DevOps 엔지니어

## ✅ 사전 요구사항

Auth Service를 실행하기 전에 다음 도구들이 설치되어 있어야 합니다:

### 필수 도구
- **Java 17**: OpenJDK 17 이상
  ```bash
  java -version  # 17.x.x 확인
  ```

- **Docker Desktop**: MySQL, Kafka 등 의존 서비스 실행용
  ```bash
  docker --version  # 20.x 이상 권장
  docker-compose --version  # 2.x 이상 권장
  ```

### 권장 도구
- **IDE**: IntelliJ IDEA (Ultimate 권장, Community도 가능)
- **Postman** 또는 **curl**: API 테스트용
- **Git**: 소스 코드 클론용

## 🔧 환경 설정

### Step 1: 저장소 클론

```bash
git clone https://github.com/L-a-z-e/portal-universe.git
cd portal-universe
```

### Step 2: 의존성 서비스 실행

Auth Service는 다음 외부 서비스에 의존합니다:
- **MySQL**: 사용자 데이터 저장
- **Kafka**: 이벤트 발행 (회원가입, 로그인 등)

Docker Compose로 의존성 서비스를 실행합니다:

```bash
# 프로젝트 루트에서 실행
docker-compose up -d mysql kafka
```

서비스 상태 확인:
```bash
docker-compose ps

# 다음과 같이 표시되어야 합니다:
# NAME                COMMAND                  SERVICE    STATUS
# mysql               "docker-entrypoint.s…"   mysql      Up
# kafka               "/etc/confluent/dock…"   kafka      Up
```

MySQL 연결 테스트:
```bash
docker exec -it mysql mysql -uroot -ppassword -e "SHOW DATABASES;"
# authdb가 목록에 있어야 합니다
```

Kafka 연결 테스트:
```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Step 3: 환경 변수 설정

Auth Service는 다음 환경 변수를 사용합니다:

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `SPRING_DATASOURCE_URL` | MySQL 연결 URL | `jdbc:mysql://localhost:3306/authdb` |
| `SPRING_DATASOURCE_USERNAME` | MySQL 사용자 | `root` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL 비밀번호 | `password` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka 브로커 주소 | `localhost:9092` |
| `SPRING_PROFILES_ACTIVE` | Spring Profile | `local` |

**로컬 개발 환경**에서는 기본값을 사용하므로 별도 설정이 필요 없습니다.

환경 변수를 오버라이드하려면:
```bash
# macOS/Linux
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/authdb
export SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Windows (PowerShell)
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/authdb"
$env:SPRING_KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
```

### Step 4: 애플리케이션 실행

#### 방법 1: Gradle 명령어 (권장)

```bash
# 프로젝트 루트에서 실행
./gradlew :services:auth-service:bootRun
```

#### 방법 2: IntelliJ IDEA

1. IntelliJ에서 프로젝트 열기
2. `services/auth-service/src/main/java/.../AuthServiceApplication.java` 찾기
3. `main` 메서드 옆 녹색 실행 버튼 클릭
4. Run Configuration에서 Active profiles: `local` 설정

#### 방법 3: JAR 빌드 후 실행

```bash
# 빌드
./gradlew :services:auth-service:build

# 실행
java -jar services/auth-service/build/libs/auth-service-0.0.1-SNAPSHOT.jar
```

#### 애플리케이션 시작 로그 확인

정상 시작 시 다음과 같은 로그가 출력됩니다:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.5.5)

2026-01-18 ... : Starting AuthServiceApplication using Java 17 ...
2026-01-18 ... : The following profiles are active: local
2026-01-18 ... : Started AuthServiceApplication in 5.234 seconds
2026-01-18 ... : Tomcat started on port(s): 8081 (http)
```

## ✅ 실행 확인

### Health Check 엔드포인트

Auth Service가 정상적으로 실행되었는지 확인합니다:

```bash
curl http://localhost:8081/actuator/health
```

예상 응답:
```json
{
  "status": "UP"
}
```

### OAuth2 Authorization Server 확인

Spring Authorization Server가 정상 동작하는지 확인:

```bash
curl http://localhost:8081/.well-known/oauth-authorization-server
```

OAuth2 메타데이터가 JSON 형식으로 반환되어야 합니다.

### API 테스트

기본 API 동작 확인:

```bash
# 사용자 등록
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Password123!"
  }'
```

예상 응답:
```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Operation completed successfully",
  "data": {
    "userId": "...",
    "username": "testuser",
    "email": "test@example.com"
  }
}
```

### 데이터베이스 확인

MySQL에 사용자가 정상 저장되었는지 확인:

```bash
docker exec -it mysql mysql -uroot -ppassword authdb -e "SELECT username, email FROM users;"
```

## ⚠️ 자주 발생하는 문제

### MySQL 연결 실패

**증상**:
```
Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago
```

**해결 방법**:
1. MySQL 컨테이너가 실행 중인지 확인:
   ```bash
   docker-compose ps mysql
   ```
2. MySQL이 준비될 때까지 대기 (최대 30초 소요):
   ```bash
   docker-compose logs -f mysql  # "ready for connections" 메시지 확인
   ```
3. 포트 3306이 다른 프로세스에 의해 사용 중인지 확인:
   ```bash
   lsof -i :3306  # macOS/Linux
   netstat -ano | findstr :3306  # Windows
   ```

### Kafka 연결 실패

**증상**:
```
Failed to construct kafka producer
Connection to node -1 could not be established
```

**해결 방법**:
1. Kafka 컨테이너가 실행 중인지 확인:
   ```bash
   docker-compose ps kafka
   ```
2. Kafka가 준비될 때까지 대기 (최대 60초 소요):
   ```bash
   docker-compose logs -f kafka  # "started (kafka.server.KafkaServer)" 메시지 확인
   ```
3. Kafka 토픽 자동 생성 확인:
   ```bash
   docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
   ```

### Port 8081 Already in Use

**증상**:
```
Port 8081 is already in use
```

**해결 방법**:
1. 포트를 사용 중인 프로세스 확인 및 종료:
   ```bash
   # macOS/Linux
   lsof -ti:8081 | xargs kill -9

   # Windows
   netstat -ano | findstr :8081
   taskkill /PID <PID> /F
   ```
2. 또는 `application-local.yml`에서 포트 변경:
   ```yaml
   server:
     port: 8082  # 다른 포트로 변경
   ```

### Gradle 빌드 실패

**증상**:
```
Could not resolve dependencies
```

**해결 방법**:
1. Gradle 캐시 정리:
   ```bash
   ./gradlew clean --refresh-dependencies
   ```
2. 네트워크 연결 확인 (Maven Central 접근 가능 여부)
3. `~/.gradle/` 디렉토리 삭제 후 재빌드

## ➡️ 다음 단계

Auth Service가 정상 실행되었다면 다음 단계를 진행하세요:

1. **API 테스트**: Postman 또는 Swagger UI로 전체 API 테스트
   - Swagger UI: `http://localhost:8081/swagger-ui.html` (설정된 경우)

2. **OAuth2 Flow 테스트**: Authorization Code Flow 실습
   - 참조: [OAuth2 Integration Guide](./oauth2-integration.md)

3. **API Gateway 연동**: API Gateway를 통한 라우팅 테스트
   - 참조: [API Gateway Setup](../../api-gateway/docs/guides/getting-started.md)

4. **프론트엔드 연동**: Portal Shell에서 로그인 기능 테스트
   - 참조: [Frontend Integration Guide](../../../frontend/portal-shell/docs/guides/auth-integration.md)

## 📚 관련 문서

- [Auth Service Architecture](../architecture/system-overview.md)
- [API Reference](../api/endpoints.md)
- [Database Schema](../architecture/database-schema.md)
- [Troubleshooting Guide](../troubleshooting/common-issues.md)

## 💬 도움이 필요하신가요?

- **Issue Tracker**: [GitHub Issues](https://github.com/L-a-z-e/portal-universe/issues)
- **Slack Channel**: #auth-service (내부 팀원만)
- **Email**: dev-support@portaluniverse.com
