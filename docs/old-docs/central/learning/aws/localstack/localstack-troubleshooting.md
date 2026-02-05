# LocalStack Troubleshooting

## 학습 목표
- LocalStack 사용 중 발생하는 일반적인 문제를 파악한다
- 문제 발생 시 체계적인 디버깅 방법을 학습한다
- 로그 분석 및 해결 방법을 익힌다
- Portal Universe 환경에서의 트러블슈팅 노하우를 습득한다

---

## 개념 설명

### 트러블슈팅 프로세스

```
문제 발생
    │
    ▼
┌─────────────────┐
│ 1. 증상 파악    │  무엇이 작동하지 않는가?
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ 2. 로그 확인    │  docker logs localstack
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ 3. 헬스 체크    │  curl localhost:4566/_localstack/health
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ 4. 설정 검증    │  docker-compose.yml, application.yml
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ 5. 해결 시도    │  재시작, 설정 수정, 권한 확인
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ 6. 검증        │   테스트로 확인
└─────────────────┘
```

---

## 일반적인 문제와 해결책

### 문제 1: Connection Refused

#### 증상
```bash
$ awslocal s3 ls
Could not connect to the endpoint URL: "http://localhost:4566/"

# 또는
org.springframework.web.client.ResourceAccessException:
  I/O error on POST request for "http://localhost:4566":
  Connection refused
```

#### 원인
- LocalStack 컨테이너가 실행 중이지 않음
- 포트 4566이 다른 프로세스에 의해 사용 중
- Docker Desktop이 실행 중이지 않음

#### 해결책

**Step 1: LocalStack 실행 상태 확인**
```bash
docker ps | grep localstack

# 출력이 없으면 실행 중이 아님
```

**Step 2: LocalStack 시작**
```bash
cd /Users/laze/Laze/Project/portal-universe
docker-compose up -d localstack

# 로그 확인 (30초 정도 대기)
docker logs -f localstack
```

**Step 3: 포트 충돌 확인**
```bash
# macOS/Linux
lsof -i :4566

# 다른 프로세스가 4566 포트 사용 중이면 종료
kill -9 <PID>
```

**Step 4: Docker Desktop 확인**
```bash
# Docker가 실행 중인지 확인
docker info

# Docker Desktop 재시작 (필요 시)
# macOS: Cmd+Q로 종료 후 재실행
```

---

### 문제 2: Bucket Not Found (NoSuchBucket)

#### 증상
```bash
$ awslocal s3 ls s3://portal-blog-uploads/
An error occurred (NoSuchBucket) when calling the ListObjectsV2 operation:
  The specified bucket does not exist

# 또는 Spring Boot 애플리케이션 로그
S3Exception: The specified bucket does not exist (Service: S3, Status Code: 404)
```

#### 원인
- 버킷이 생성되지 않음
- LocalStack 재시작 후 버킷 메타데이터 초기화
- 버킷 이름 오타

#### 해결책

**Step 1: 버킷 목록 확인**
```bash
awslocal s3 ls

# 출력이 비어있으면 버킷이 없음
```

**Step 2: 버킷 수동 생성**
```bash
awslocal s3 mb s3://portal-blog-uploads

# 버킷 생성 확인
awslocal s3 ls
# 2024-01-22 14:30:00 portal-blog-uploads
```

**Step 3: 자동 생성 확인 (권장)**
```java
// FileService.java의 @PostConstruct 확인
@PostConstruct
public void ensureBucketExists() {
    try {
        s3Client.headBucket(HeadBucketRequest.builder()
            .bucket(bucketName)
            .build());
        log.info("✅ S3 bucket '{}' exists", bucketName);
    } catch (NoSuchBucketException e) {
        s3Client.createBucket(CreateBucketRequest.builder()
            .bucket(bucketName)
            .build());
        log.info("🚀 Created S3 bucket '{}'", bucketName);
    }
}
```

**Step 4: 애플리케이션 재시작 (자동 생성 트리거)**
```bash
docker-compose restart blog-service

# 로그 확인
docker logs blog-service | grep bucket
```

---

### 문제 3: SignatureDoesNotMatch

#### 증상
```bash
An error occurred (SignatureDoesNotMatch) when calling the PutObject operation:
  The request signature we calculated does not match the signature you provided.

# 또는
S3Exception: The request signature we calculated does not match the signature you provided
```

#### 원인
- AWS Credentials가 잘못 설정됨
- LocalStack 환경변수와 애플리케이션 설정 불일치
- AWS Profile이 실제 AWS credentials를 사용

#### 해결책

**Step 1: LocalStack 환경변수 확인**
```yaml
# docker-compose.yml
localstack:
  environment:
    - AWS_ACCESS_KEY_ID=test     # 반드시 'test'
    - AWS_SECRET_ACCESS_KEY=test # 반드시 'test'
```

**Step 2: Spring Boot 설정 확인**
```yaml
# application-local.yml
spring:
  cloud:
    aws:
      credentials:
        access-key: test    # LocalStack 환경변수와 동일
        secret-key: test    # LocalStack 환경변수와 동일
```

**Step 3: AWS CLI Profile 확인**
```bash
# AWS CLI가 실제 credentials를 사용하지 않도록 확인
unset AWS_PROFILE
unset AWS_ACCESS_KEY_ID
unset AWS_SECRET_ACCESS_KEY

# awslocal 사용 (자동으로 test/test 사용)
awslocal s3 ls
```

**Step 4: Java SDK Configuration**
```java
@Bean
public S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create("http://localhost:4566"))
        .region(Region.AP_NORTHEAST_2)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test", "test")  // 하드코딩 권장
            )
        )
        .serviceConfiguration(
            S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build()
        )
        .build();
}
```

---

### 문제 4: Path-Style Access 오류

#### 증상
```bash
# Virtual-hosted-style URL (작동하지 않음)
http://portal-blog-uploads.localhost:4566/file.jpg

# 예상 에러:
java.net.UnknownHostException: portal-blog-uploads.localhost
```

#### 원인
- LocalStack은 path-style URL만 지원
- S3 Client에서 path-style 설정 누락

#### 해결책

**Path-Style vs Virtual-Hosted-Style**
```
Path-Style (LocalStack 지원):
✅ http://localhost:4566/bucket-name/object-key

Virtual-Hosted-Style (실제 AWS):
❌ http://bucket-name.s3.amazonaws.com/object-key
```

**Spring Boot 설정**
```yaml
# application-local.yml
spring:
  cloud:
    aws:
      s3:
        path-style-access-enabled: true  # 필수!
```

**Java SDK 설정**
```java
S3Configuration.builder()
    .pathStyleAccessEnabled(true)  // 반드시 true
    .build()
```

---

### 문제 5: 데이터 유실 (재시작 후 파일 사라짐)

#### 증상
```bash
# 파일 업로드 성공
$ awslocal s3 cp file.txt s3://portal-blog-uploads/

# LocalStack 재시작
$ docker-compose restart localstack

# 파일 사라짐
$ awslocal s3 ls s3://portal-blog-uploads/
(출력 없음)
```

#### 원인
- PERSISTENCE 설정 누락
- 볼륨 마운트 설정 누락
- `docker-compose down -v` 사용 (볼륨 삭제)

#### 해결책

**Step 1: PERSISTENCE 설정 확인**
```yaml
# docker-compose.yml
localstack:
  environment:
    - PERSISTENCE=1                 # 반드시 활성화
    - DATA_DIR=/var/lib/localstack  # 데이터 디렉토리 지정
  volumes:
    - ./localstack_data:/var/lib/localstack  # 반드시 마운트
```

**Step 2: 볼륨 마운트 확인**
```bash
docker inspect localstack | jq '.[0].Mounts'

# 예상 출력:
# [
#   {
#     "Type": "bind",
#     "Source": "/Users/laze/Laze/Project/portal-universe/localstack_data",
#     "Destination": "/var/lib/localstack",
#     "Mode": "",
#     "RW": true,
#     "Propagation": "rprivate"
#   }
# ]
```

**Step 3: 안전한 재시작**
```bash
# ✅ 안전 (데이터 유지)
docker-compose restart localstack
docker-compose stop localstack && docker-compose start localstack
docker-compose down && docker-compose up -d localstack

# ⚠️ 위험 (데이터 삭제!)
docker-compose down -v  # 절대 사용 금지!
```

**Step 4: 데이터 디렉토리 확인**
```bash
ls -la ./localstack_data/

# 파일이 있어야 함
# drwxr-xr-x  5 laze  staff  160 Jan 22 14:30 .
# drwxr-xr-x 20 laze  staff  640 Jan 22 14:25 ..
# drwxr-xr-x  3 laze  staff   96 Jan 22 14:30 data
# drwxr-xr-x  3 laze  staff   96 Jan 22 14:30 state
```

---

### 문제 6: Permission Denied (권한 오류)

#### 증상
```bash
# LocalStack 로그
docker logs localstack
# ERROR: Permission denied: '/var/lib/localstack'

# 또는
# OSError: [Errno 13] Permission denied: '/var/lib/localstack/state'
```

#### 원인
- 호스트의 localstack_data 디렉토리 권한 문제
- Docker가 쓰기 권한 없음 (macOS/Linux)

#### 해결책

**Step 1: 소유권 변경 (권장)**
```bash
sudo chown -R $(whoami):$(id -gn) ./localstack_data
```

**Step 2: 권한 부여 (개발 환경)**
```bash
chmod -R 755 ./localstack_data
```

**Step 3: Docker user 설정 (Linux)**
```yaml
# docker-compose.yml
localstack:
  user: "${UID}:${GID}"
  volumes:
    - ./localstack_data:/var/lib/localstack
```

```bash
# .env 파일
UID=1000
GID=1000
```

**Step 4: 디렉토리 재생성**
```bash
# 기존 디렉토리 삭제 (데이터 백업 후)
rm -rf ./localstack_data

# 올바른 권한으로 재생성
mkdir -p ./localstack_data
chown -R $(whoami):$(id -gn) ./localstack_data

# LocalStack 재시작
docker-compose up -d --force-recreate localstack
```

---

### 문제 7: 느린 시작 속도

#### 증상
```bash
# LocalStack 시작에 2-3분 소요
docker-compose up -d localstack
# Creating localstack ... done
# (오랜 시간 대기...)
```

#### 원인
- 모든 서비스 활성화 (`SERVICES=` 빈 값)
- Docker Desktop 리소스 부족
- 불필요한 초기화 스크립트

#### 해결책

**Step 1: 필요한 서비스만 활성화**
```yaml
# ❌ 느림 (모든 서비스)
environment:
  - SERVICES=

# ✅ 빠름 (필요한 것만)
environment:
  - SERVICES=s3
```

**Step 2: Docker Desktop 리소스 증가**
```
Docker Desktop > Preferences > Resources
- CPUs: 4개 이상
- Memory: 4GB 이상
- Swap: 1GB 이상
```

**Step 3: 초기화 스크립트 최소화**
```yaml
# 불필요한 초기화 스크립트 제거
volumes:
  - ./localstack_data:/var/lib/localstack
  # - ./init-scripts:/etc/localstack/init/ready.d  # 필요할 때만
```

---

### 문제 8: Spring Boot 연동 실패

#### 증상
```
org.springframework.beans.factory.BeanCreationException:
  Error creating bean with name 's3Client'

Caused by: SdkClientException: Unable to execute HTTP request:
  Connect to localhost:4566 failed: Connection refused
```

#### 원인
- LocalStack이 Spring Boot보다 늦게 시작
- 잘못된 endpoint URL
- Profile 설정 오류

#### 해결책

**Step 1: 시작 순서 보장**
```yaml
# docker-compose.yml
blog-service:
  depends_on:
    localstack:
      condition: service_healthy  # LocalStack이 준비될 때까지 대기

localstack:
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:4566/_localstack/health"]
    interval: 10s
    timeout: 5s
    retries: 5
```

**Step 2: Endpoint URL 검증**
```yaml
# application-local.yml
spring:
  cloud:
    aws:
      s3:
        endpoint: http://localstack:4566  # 컨테이너 이름 사용
        # 또는
        endpoint: http://localhost:4566   # 호스트에서 접근
```

**Step 3: Profile 활성화 확인**
```yaml
# docker-compose.yml
blog-service:
  environment:
    - SPRING_PROFILES_ACTIVE=local  # local 프로파일 활성화
```

**Step 4: Connection Timeout 증가**
```yaml
# application-local.yml
spring:
  cloud:
    aws:
      s3:
        endpoint: http://localstack:4566
      client:
        connect-timeout: 30000  # 30초
        read-timeout: 30000
```

---

## 디버깅 도구

### 1. LocalStack 로그 분석

```bash
# 실시간 로그 확인
docker logs -f localstack

# 최근 100줄 확인
docker logs --tail 100 localstack

# 에러만 필터링
docker logs localstack 2>&1 | grep -i error

# 특정 서비스 로그 필터링
docker logs localstack 2>&1 | grep -i s3
```

### 2. 디버그 모드 활성화

```yaml
# docker-compose.yml
localstack:
  environment:
    - DEBUG=1              # 디버그 로그 활성화
    - LS_LOG=trace         # 상세 로그 레벨
    - SERVICES=s3
```

```bash
# 재시작 후 로그 확인
docker-compose up -d --force-recreate localstack
docker logs -f localstack
```

### 3. Health Check

```bash
# 전체 상태 확인
curl http://localhost:4566/_localstack/health | jq

# 예상 출력:
# {
#   "services": {
#     "s3": "running"
#   },
#   "version": "3.0.0",
#   "edition": "community"
# }

# 특정 서비스 확인
curl http://localhost:4566/_localstack/health | jq '.services.s3'
# "running"
```

### 4. AWS CLI 디버그 모드

```bash
# AWS CLI 디버그 출력
awslocal s3 ls --debug

# HTTP 요청/응답 확인
awslocal s3 ls --debug 2>&1 | grep -A 5 "HTTP request"
```

### 5. Docker 네트워크 확인

```bash
# 네트워크 목록
docker network ls

# 특정 네트워크 상세 정보
docker network inspect portal-universe-net

# LocalStack IP 확인
docker inspect localstack | jq '.[0].NetworkSettings.Networks'
```

### 6. 포트 리스닝 확인

```bash
# macOS/Linux
lsof -i :4566

# 또는
netstat -an | grep 4566

# 또는
nc -zv localhost 4566
# Connection to localhost port 4566 [tcp/*] succeeded!
```

---

## 실습 예제

### 실습 1: 체계적 트러블슈팅

```bash
# Step 1: 문제 재현
awslocal s3 ls s3://portal-blog-uploads/
# An error occurred (NoSuchBucket)

# Step 2: LocalStack 상태 확인
docker ps | grep localstack
# CONTAINER ID   IMAGE                          STATUS
# abc123         localstack/localstack:latest   Up 5 minutes

# Step 3: Health Check
curl http://localhost:4566/_localstack/health | jq '.services.s3'
# "running"

# Step 4: 버킷 목록 확인
awslocal s3 ls
# (출력 없음 - 버킷이 없음을 확인)

# Step 5: 버킷 생성
awslocal s3 mb s3://portal-blog-uploads

# Step 6: 검증
awslocal s3 ls
# 2024-01-22 14:30:00 portal-blog-uploads ✅
```

### 실습 2: 로그 기반 디버깅

```bash
# 문제 발생
curl http://localhost:4566/portal-blog-uploads/file.jpg
# 404 Not Found

# 로그 확인
docker logs localstack --tail 50 | grep -A 5 -B 5 "portal-blog-uploads"

# S3 관련 에러 찾기
docker logs localstack 2>&1 | grep -i "s3.*error"

# 디버그 모드로 재시작
docker-compose down
# docker-compose.yml에서 DEBUG=1 추가
docker-compose up -d localstack

# 상세 로그 관찰
docker logs -f localstack
```

---

## 핵심 요약

### 문제 해결 체크리스트

```yaml
☐ LocalStack 컨테이너가 실행 중인가?
  → docker ps | grep localstack

☐ Health Check가 통과하는가?
  → curl http://localhost:4566/_localstack/health

☐ PERSISTENCE=1 및 볼륨 마운트가 설정되었는가?
  → docker inspect localstack | jq '.[0].Mounts'

☐ 버킷이 생성되었는가?
  → awslocal s3 ls

☐ Credentials가 test/test인가?
  → docker-compose.yml 환경변수 확인

☐ Path-style access가 활성화되었는가?
  → Spring Boot 설정에서 pathStyleAccessEnabled: true

☐ 데이터 디렉토리 권한이 올바른가?
  → ls -la ./localstack_data/

☐ 필요한 서비스만 활성화되었는가?
  → SERVICES=s3 (불필요한 서비스 제거)
```

### 주요 명령어 모음

| 작업 | 명령어 |
|------|--------|
| 로그 확인 | `docker logs -f localstack` |
| Health Check | `curl http://localhost:4566/_localstack/health` |
| 디버그 재시작 | `docker-compose up -d -e DEBUG=1 localstack` |
| 볼륨 확인 | `docker inspect localstack \| jq '.[0].Mounts'` |
| 권한 수정 | `sudo chown -R $(whoami) ./localstack_data` |
| 포트 확인 | `lsof -i :4566` |
| 안전한 재시작 | `docker-compose restart localstack` |

### Portal Universe 특화 팁

1. **버킷 자동 생성**: Blog Service의 @PostConstruct가 제대로 실행되는지 확인
   ```bash
   docker logs blog-service | grep bucket
   ```

2. **컨테이너 시작 순서**: LocalStack이 먼저 준비되어야 함
   ```yaml
   depends_on:
     localstack:
       condition: service_healthy
   ```

3. **데이터 영속성**: `docker-compose down -v` 절대 사용 금지

4. **네트워크**: 같은 네트워크에서 컨테이너 이름으로 접근
   ```
   http://localstack:4566 (컨테이너 간)
   http://localhost:4566 (호스트에서)
   ```

---

## 관련 문서
- [LocalStack Setup](./localstack-setup.md) - 설치 및 기본 설정
- [LocalStack Persistence](./localstack-persistence.md) - 데이터 영속성 문제 해결
- [LocalStack Services](./localstack-services.md) - 지원 서비스 가이드
- [Docker Troubleshooting](https://docs.docker.com/config/daemon/logs/) - Docker 로그 분석
- [LocalStack GitHub Issues](https://github.com/localstack/localstack/issues) - 커뮤니티 지원
