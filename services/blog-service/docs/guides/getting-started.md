---
id: guide-getting-started
title: Blog Service Getting Started
type: guide
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [setup, environment, blog-service]
related:
  - backup/README.md
  - backup/ARCHITECTURE.md
---

# Getting Started

> Blog Service 개발 환경 설정 가이드

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **예상 소요 시간** | 30분 |
| **대상** | Blog Service 백엔드 개발자 |
| **서비스 포트** | 8082 |

---

## ✅ 사전 요구사항

### 필수 소프트웨어

| 소프트웨어 | 버전 | 확인 명령어 |
|-----------|------|------------|
| Java | 17+ | `java -version` |
| Gradle | 8.x+ | `gradle --version` |
| MongoDB | 4.x+ | `mongosh --version` |
| Docker | 20.x+ | `docker --version` |
| Git | 2.x+ | `git --version` |

### 필수 AWS 계정 정보

Blog Service는 S3를 사용하여 파일 업로드를 처리합니다:

- AWS Access Key ID
- AWS Secret Access Key
- S3 Bucket Name

**보안 주의**: AWS credentials는 절대 코드에 직접 작성하지 말고, 환경 변수로만 관리하세요.

---

## 🔧 환경 설정

### Step 1: 저장소 클론

```bash
git clone https://github.com/L-a-z-e/portal-universe.git
cd portal-universe
```

### Step 2: 브랜치 확인

```bash
git branch -a
git checkout dev
```

### Step 3: MongoDB 실행 (로컬)

**옵션 A: Docker Compose 사용 (권장)**

```bash
docker-compose up -d mongodb
```

**옵션 B: 로컬 MongoDB 직접 실행**

```bash
mongosh
```

MongoDB 연결 확인:
```bash
mongosh mongodb://localhost:27017
```

### Step 4: 환경 변수 설정

프로젝트 루트 또는 서비스 디렉토리에 `.env` 파일 생성:

```bash
# MongoDB
MONGODB_URI=mongodb://localhost:27017/blog_service

# AWS S3
AWS_S3_BUCKET=your-bucket-name
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
AWS_REGION=ap-northeast-2
```

**IntelliJ IDEA 사용자**: Run/Debug Configuration에서 Environment Variables에 추가
**터미널 사용자**: export 명령어 사용

```bash
export MONGODB_URI=mongodb://localhost:27017/blog_service
export AWS_S3_BUCKET=your-bucket-name
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key
```

### Step 5: 의존성 다운로드

```bash
./gradlew :services:blog-service:dependencies
```

### Step 6: 빌드

```bash
./gradlew :services:blog-service:build
```

### Step 7: 서비스 실행

**로컬 프로필로 실행**:

```bash
./gradlew :services:blog-service:bootRun --args='--spring.profiles.active=local'
```

또는 JAR 파일 직접 실행:

```bash
java -jar services/blog-service/build/libs/blog-service-*.jar --spring.profiles.active=local
```

---

## ✅ 실행 확인

### 헬스 체크

```bash
curl http://localhost:8082/actuator/health
```

**예상 결과**:
```json
{
  "status": "UP"
}
```

### Swagger UI 접근

브라우저에서 열기:

```
http://localhost:8082/swagger-ui.html
```

**확인 사항**:
- Post API, Comment API, Series API, Tag API, File API가 모두 표시되는지 확인
- Try it out 버튼으로 GET 요청 테스트

### MongoDB 데이터 확인

```bash
mongosh mongodb://localhost:27017/blog_service

> show collections
> db.posts.find().pretty()
```

---

## 🧪 테스트 실행

### 단위 테스트

```bash
./gradlew :services:blog-service:test
```

### 특정 테스트 클래스 실행

```bash
./gradlew :services:blog-service:test --tests "PostServiceTest"
```

### 통합 테스트 (MongoDB Testcontainers 사용)

```bash
./gradlew :services:blog-service:integrationTest
```

**참고**: Testcontainers는 Docker를 사용하므로 Docker가 실행 중이어야 합니다.

---

## 🐳 Docker 컨테이너 실행

### Docker Compose로 전체 스택 실행

```bash
docker-compose up -d
```

이 명령어는 다음을 함께 실행합니다:
- MongoDB
- API Gateway
- Auth Service
- **Blog Service**
- Shopping Service
- Notification Service
- Kafka, Prometheus, Grafana 등

### Blog Service만 Docker로 실행

```bash
# 이미지 빌드
./gradlew :services:blog-service:bootBuildImage

# 컨테이너 실행
docker run -d \
  -p 8082:8082 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e MONGODB_URI=mongodb://mongodb:27017/blog_service \
  -e AWS_S3_BUCKET=your-bucket \
  -e AWS_ACCESS_KEY_ID=your-key \
  -e AWS_SECRET_ACCESS_KEY=your-secret \
  --name blog-service \
  blog-service:latest
```

### 로그 확인

```bash
docker logs -f blog-service
```

---

## 🔍 디버깅 팁

### 1. IntelliJ IDEA 디버그 모드

1. Run → Edit Configurations
2. Spring Boot → blog-service 선택
3. Environment Variables에 필수 변수 추가
4. Debug 버튼 클릭

### 2. 원격 디버그 (Docker 컨테이너)

Docker 실행 시 JDWP 활성화:

```bash
docker run -d \
  -p 8082:8082 \
  -p 5005:5005 \
  -e JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" \
  blog-service:latest
```

IntelliJ에서 Remote JVM Debug 설정:
- Host: localhost
- Port: 5005

### 3. 로그 레벨 조정

`application-local.yml` 수정:

```yaml
logging:
  level:
    com.portaluniverse.blogservice: DEBUG
    org.springframework.data.mongodb: DEBUG
```

또는 런타임에 변경:

```bash
./gradlew :services:blog-service:bootRun --args='--logging.level.com.portaluniverse.blogservice=DEBUG'
```

### 4. MongoDB 쿼리 디버깅

`application-local.yml`에 추가:

```yaml
logging:
  level:
    org.springframework.data.mongodb.core.MongoTemplate: DEBUG
```

---

## ⚠️ 자주 발생하는 문제

### 문제 1: MongoDB 연결 실패

**증상**:
```
com.mongodb.MongoTimeoutException: Timed out after 30000 ms
```

**해결 방법**:

1. MongoDB가 실행 중인지 확인:
   ```bash
   docker ps | grep mongodb
   ```

2. 연결 문자열 확인:
   ```bash
   echo $MONGODB_URI
   ```

3. 방화벽/포트 확인:
   ```bash
   telnet localhost 27017
   ```

### 문제 2: AWS S3 권한 오류

**증상**:
```
AmazonS3Exception: Access Denied
```

**해결 방법**:

1. IAM 사용자 권한 확인:
   - `s3:PutObject`
   - `s3:GetObject`
   - `s3:DeleteObject`

2. 버킷 정책 확인

3. Credentials 재설정:
   ```bash
   aws configure
   ```

### 문제 3: 포트 충돌 (8082)

**증상**:
```
Port 8082 was already in use
```

**해결 방법**:

1. 사용 중인 프로세스 확인:
   ```bash
   lsof -i :8082
   ```

2. 프로세스 종료:
   ```bash
   kill -9 <PID>
   ```

3. 또는 다른 포트 사용:
   ```bash
   ./gradlew :services:blog-service:bootRun --args='--server.port=8092'
   ```

### 문제 4: Gradle 빌드 실패

**증상**:
```
Could not resolve all dependencies
```

**해결 방법**:

1. Gradle 캐시 삭제:
   ```bash
   rm -rf ~/.gradle/caches
   ```

2. 재빌드:
   ```bash
   ./gradlew clean build --refresh-dependencies
   ```

3. Gradle Wrapper 업데이트:
   ```bash
   ./gradlew wrapper --gradle-version=8.11.1
   ```

---

## 📊 API 테스트 예시

### 게시물 생성 (인증 필요)

```bash
curl -X POST http://localhost:8082/posts \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "My First Post",
    "content": "Hello World!",
    "category": "tech",
    "tags": ["spring", "mongodb"]
  }'
```

### 게시물 목록 조회 (인증 불필요)

```bash
curl http://localhost:8082/posts?page=0&size=10
```

### 게시물 검색

```bash
curl "http://localhost:8082/posts/search?keyword=spring"
```

---

## ➡️ 다음 단계

1. **API 명세 확인**: [API.md](../api/) - 전체 API 엔드포인트 상세
2. **아키텍처 이해**: [ARCHITECTURE.md](../architecture/) - 도메인 모델, 검색, 통계 기능
3. **개발 워크플로우**: [Development Workflow](./development-workflow.md) - Git, PR 프로세스 (작성 예정)
4. **배포 가이드**: [Deployment](../runbooks/) - K8s 배포 방법 (작성 예정)

---

## 🔗 관련 문서

- [Blog Service README](../backup/README.md)
- [Blog Service Architecture](../backup/ARCHITECTURE.md)
- [Portal Universe CLAUDE.md](/Users/laze/Laze/Project/portal-universe-docs/CLAUDE.md)

---

**최종 업데이트**: 2026-01-18
