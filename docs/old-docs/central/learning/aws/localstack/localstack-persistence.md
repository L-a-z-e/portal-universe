# LocalStack Persistence (데이터 영속성)

## 학습 목표
- LocalStack의 데이터 영속성 메커니즘을 이해한다
- "서버 껐다키면 저장이 안됨" 문제의 원인을 파악한다
- 데이터가 유지되지 않는 일반적인 시나리오를 학습한다
- 올바른 영속성 설정 방법을 습득한다
- 버킷 자동 생성 전략을 구현한다

---

## 개념 설명

### LocalStack의 데이터 영속성

#### 기본 동작 (PERSISTENCE=0 또는 미설정)
```
┌─────────────────────────────────────┐
│  LocalStack Container               │
│                                     │
│  메모리에만 데이터 저장             │
│  컨테이너 재시작 시 모든 데이터 소실│
└─────────────────────────────────────┘
```

**결과**: `docker-compose restart` 시 업로드한 파일, 버킷 설정 등 모두 사라짐 ❌

#### 영속성 활성화 (PERSISTENCE=1)
```
┌─────────────────────────────────────┐
│  LocalStack Container               │
│                                     │
│  /var/lib/localstack/               │  <- 컨테이너 내부 경로
│  ├── state/                         │
│  ├── data/                          │
│  └── tmp/                           │
└─────────────────────────────────────┘
         │
         │ Volume Mount (중요!)
         ▼
   ./localstack_data/                   <- 호스트 경로
   ├── state/
   ├── data/
   └── tmp/
```

**결과**: 컨테이너 재시작 후에도 데이터 유지 ✅

### PERSISTENCE=1의 실제 동작

#### 저장되는 데이터
- ✅ S3 버킷에 업로드한 파일 (실제 바이너리 데이터)
- ✅ DynamoDB 테이블 데이터
- ✅ SQS 큐 메시지
- ✅ Lambda 함수 코드

#### 저장되지 않을 수 있는 데이터
- ⚠️ 버킷 메타데이터 (버킷 생성 정보)
- ⚠️ IAM 정책
- ⚠️ CloudFormation 스택 상태

**중요**: LocalStack Pro가 아닌 Community 버전에서는 일부 메타데이터가 재시작 시 초기화될 수 있습니다.

---

## Portal Universe 적용

### 문제 시나리오: "서버 껐다키면 저장이 안됨"

#### 증상
```bash
# 1. 파일 업로드
curl -X POST http://localhost:8081/api/v1/files/upload \
  -F "file=@image.jpg"
# 응답: { "fileUrl": "http://localhost:4566/portal-blog-uploads/..." }

# 2. 파일 접근 가능
curl http://localhost:4566/portal-blog-uploads/abc-123.jpg
# 응답: (이미지 데이터)

# 3. LocalStack 재시작
docker-compose restart localstack

# 4. 파일 접근 실패
curl http://localhost:4566/portal-blog-uploads/abc-123.jpg
# 응답: NoSuchBucket 또는 NoSuchKey
```

#### 원인 분석

**원인 1: 볼륨 마운트 누락**
```yaml
# ❌ 잘못된 설정 (영속성 없음)
localstack:
  environment:
    - PERSISTENCE=1
    - DATA_DIR=/var/lib/localstack
  # volumes 설정 없음!
```

**원인 2: 볼륨 경로 불일치**
```yaml
# ❌ 잘못된 설정 (경로 불일치)
localstack:
  environment:
    - DATA_DIR=/tmp/localstack  # 잘못된 경로
  volumes:
    - ./localstack_data:/var/lib/localstack  # 다른 경로
```

**원인 3: docker-compose down -v 사용**
```bash
# ❌ 위험한 명령어 (볼륨 삭제!)
docker-compose down -v
# -v 플래그는 Named Volume과 Anonymous Volume을 모두 삭제합니다
```

**원인 4: 버킷 메타데이터 초기화**
```
재시작 시:
1. 파일 데이터는 ./localstack_data/에 유지됨 ✅
2. 버킷 생성 정보는 초기화될 수 있음 ⚠️
3. 버킷이 없으면 파일에 접근할 수 없음 ❌
```

### 올바른 설정

#### Docker Compose 설정 (현재 Portal Universe)

> **주의**: `DATA_DIR` 환경변수는 최신 LocalStack에서 **deprecated**되었습니다.
> 설정해도 무시되며 경고 로그만 출력됩니다. `PERSISTENCE=1`만 사용하세요.

```yaml
services:
  localstack:
    image: localstack/localstack:latest
    container_name: localstack        # 고정된 컨테이너 이름 (필수)
    ports:
      - "4566:4566"
    environment:
      - SERVICES=s3
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - AWS_DEFAULT_REGION=ap-northeast-2
      - PERSISTENCE=1                 # 영속성 활성화
      # DATA_DIR은 deprecated - 사용하지 않음
    volumes:
      - localstack-data:/var/lib/localstack  # Named Volume 권장
    networks:
      - portal-universe-net
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:4566/_localstack/health"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  localstack-data:  # Named Volume 정의
```

#### Bind Mount vs Named Volume

| 방식 | 장점 | 단점 |
|------|------|------|
| **Bind Mount** (`./localstack_data:/path`) | 호스트에서 직접 접근 가능 | 권한 문제 발생 가능 |
| **Named Volume** (`localstack-data:/path`) | Docker가 권한 관리 | 호스트에서 직접 접근 어려움 |

**권장**: Named Volume 사용 (권한 문제 회피)

#### 디렉토리 권한 설정 (macOS/Linux)
```bash
# LocalStack이 데이터를 쓸 수 있도록 권한 부여
mkdir -p ./localstack_data
sudo chown -R $(whoami):$(id -gn) ./localstack_data

# 또는 Docker user 설정
chmod -R 777 ./localstack_data  # 개발 환경에서만 사용
```

---

## 버킷 자동 생성 전략

### 문제
재시작 후 버킷 메타데이터가 초기화되면, 파일은 있지만 버킷이 없어서 접근 불가

### 해결책: 버킷 자동 생성

#### 방법 1: @PostConstruct (권장, Portal Universe 현재 구현)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * 애플리케이션 시작 시 S3 버킷 존재 확인 및 자동 생성
     * LocalStack 재시작으로 인한 버킷 메타데이터 초기화 대응
     */
    @PostConstruct
    public void ensureBucketExists() {
        try {
            // 버킷 존재 확인
            s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(bucketName)
                .build());
            log.info("✅ S3 bucket '{}' exists", bucketName);
        } catch (NoSuchBucketException e) {
            // 버킷이 없으면 자동 생성
            s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(bucketName)
                .build());
            log.info("🚀 Created S3 bucket '{}'", bucketName);
        } catch (Exception e) {
            log.error("❌ Failed to ensure bucket exists: {}", e.getMessage());
            // 애플리케이션 시작은 계속 진행 (선택적 에러 처리)
        }
    }

    public String uploadFile(MultipartFile file) {
        // 파일 업로드 로직
        String key = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build(),
            RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        return String.format("http://localhost:4566/%s/%s", bucketName, key);
    }
}
```

**장점**:
- ✅ Spring Boot 애플리케이션 시작 시 자동 실행
- ✅ 코드만으로 해결 (인프라 스크립트 불필요)
- ✅ 로컬/운영 환경 모두 대응 가능
- ✅ Portal Universe 현재 구현 방식

**단점**:
- 애플리케이션마다 구현 필요

#### 방법 2: Docker Compose 초기화 스크립트

```bash
# init-scripts/01-create-buckets.sh
#!/bin/bash
awslocal s3 mb s3://portal-blog-uploads || true
awslocal s3 mb s3://portal-user-avatars || true
echo "✅ S3 buckets initialized"
```

```yaml
# docker-compose.yml
services:
  localstack:
    # ... 기존 설정
    volumes:
      - ./localstack_data:/var/lib/localstack
      - ./init-scripts:/etc/localstack/init/ready.d  # 초기화 스크립트
```

**실행 시점**: LocalStack이 "Ready" 상태가 되면 자동 실행

**장점**:
- ✅ 중앙 집중식 관리
- ✅ 여러 서비스의 리소스를 한번에 초기화
- ✅ 인프라 코드로 관리 가능

**단점**:
- 스크립트 파일 관리 필요
- LocalStack 컨테이너에만 적용됨

#### 방법 3: Init Container (Kubernetes 환경)

```yaml
# k8s/localstack-init.yaml
apiVersion: v1
kind: Pod
metadata:
  name: localstack-init
spec:
  initContainers:
  - name: create-buckets
    image: amazon/aws-cli
    command: ["/bin/sh", "-c"]
    args:
      - |
        aws --endpoint-url=http://localstack:4566 \
            s3 mb s3://portal-blog-uploads || true
  containers:
  - name: app
    image: portal-blog-service:latest
```

**장점**:
- ✅ Kubernetes 네이티브 방식
- ✅ 애플리케이션 시작 전 리소스 보장

**단점**:
- Kubernetes 환경에서만 사용 가능

### 권장 사항

| 환경 | 권장 방법 | 이유 |
|------|----------|------|
| **로컬 개발** | @PostConstruct | 간단하고 코드만으로 해결 |
| **Docker Compose** | 초기화 스크립트 | 중앙 집중식 관리 |
| **Kubernetes** | Init Container | 인프라 레벨 보장 |

**Portal Universe**: 현재 방법 1 (@PostConstruct) 사용 중 ✅

---

## 실습 예제

### 실습 1: 영속성 테스트

#### Step 1: 초기 설정 확인
```bash
# LocalStack 시작
cd /Users/laze/Laze/Project/portal-universe
docker-compose up -d localstack

# 데이터 디렉토리 확인
ls -la ./localstack_data/
```

#### Step 2: 파일 업로드
```bash
# Blog Service 시작
docker-compose up -d blog-service

# 파일 업로드 (실제 API 사용)
curl -X POST http://localhost:8081/api/v1/files/upload \
  -F "file=@test-image.jpg"

# 응답 예시:
# {
#   "success": true,
#   "data": {
#     "fileUrl": "http://localhost:4566/portal-blog-uploads/abc-123.jpg"
#   }
# }
```

#### Step 3: 데이터 확인
```bash
# LocalStack 데이터 디렉토리 확인
ls -la ./localstack_data/

# awslocal로 파일 목록 확인
awslocal s3 ls s3://portal-blog-uploads/

# 파일 다운로드
awslocal s3 cp s3://portal-blog-uploads/abc-123.jpg downloaded.jpg
```

#### Step 4: 재시작 테스트
```bash
# LocalStack 재시작
docker-compose restart localstack

# 10초 대기 (LocalStack 초기화 시간)
sleep 10

# 버킷 목록 확인
awslocal s3 ls
# 예상: portal-blog-uploads 버킷이 보임 (자동 생성됨)

# 파일 목록 확인
awslocal s3 ls s3://portal-blog-uploads/
# 예상: abc-123.jpg 파일이 보임 ✅
```

#### Step 5: 완전 삭제 후 재생성 (데이터 유지 확인)
```bash
# 컨테이너 완전 삭제 (볼륨은 유지)
docker-compose down

# 데이터가 여전히 존재하는지 확인
ls -la ./localstack_data/
# 예상: 디렉토리와 파일들이 그대로 있음

# LocalStack 재시작
docker-compose up -d localstack

# Blog Service 재시작 (@PostConstruct 실행)
docker-compose restart blog-service

# 파일이 여전히 접근 가능한지 확인
curl http://localhost:4566/portal-blog-uploads/abc-123.jpg
# 예상: 이미지 데이터 반환 ✅
```

### 실습 2: 볼륨 삭제 시나리오 (데이터 손실)

```bash
# ⚠️ 위험: 볼륨까지 삭제
docker-compose down -v

# 데이터 디렉토리 확인
ls -la ./localstack_data/
# 예상: 빈 디렉토리 또는 디렉토리 자체가 삭제됨

# LocalStack 재시작
docker-compose up -d localstack blog-service

# 파일 접근 시도
awslocal s3 ls s3://portal-blog-uploads/
# 예상: 버킷은 있지만 파일은 없음 ❌

curl http://localhost:4566/portal-blog-uploads/abc-123.jpg
# 예상: NoSuchKey 에러
```

### 실습 3: 버킷 자동 생성 확인

```bash
# LocalStack만 시작 (버킷 없음)
docker-compose up -d localstack

# 버킷 목록 확인
awslocal s3 ls
# 예상: 비어있음

# Blog Service 시작 (@PostConstruct 실행)
docker-compose up -d blog-service

# 로그 확인
docker logs blog-service | grep "bucket"
# 예상: "🚀 Created S3 bucket 'portal-blog-uploads'"

# 버킷 목록 재확인
awslocal s3 ls
# 예상: portal-blog-uploads 버킷이 자동 생성됨 ✅
```

### 실습 4: 권한 문제 해결 (macOS)

```bash
# 권한 에러 시나리오
docker logs localstack | grep -i permission
# 예상: "Permission denied: /var/lib/localstack"

# 해결책 1: 소유권 변경
sudo chown -R $(whoami):$(id -gn) ./localstack_data

# 해결책 2: 권한 부여
chmod -R 755 ./localstack_data

# 재시작
docker-compose restart localstack
```

---

## 핵심 요약

### 데이터가 유지되지 않는 주요 원인

| 원인 | 증상 | 해결책 |
|------|------|--------|
| **볼륨 마운트 누락** | 재시작 시 모든 데이터 소실 | `volumes` 설정 추가 |
| **경로 불일치** | PERSISTENCE=1인데 데이터 소실 | `DATA_DIR`과 볼륨 경로 일치 확인 |
| **docker-compose down -v** | 볼륨까지 삭제 | `-v` 플래그 제거 |
| **버킷 메타데이터 초기화** | 파일은 있지만 접근 불가 | 버킷 자동 생성 구현 |
| **권한 문제** | LocalStack이 쓰기 실패 | `chown` 또는 `chmod` 실행 |

### 영속성 보장을 위한 체크리스트

```yaml
✅ PERSISTENCE=1 설정
❌ DATA_DIR 환경변수 (deprecated - 사용하지 않음)
✅ container_name 고정 설정
✅ volumes 설정 (Named Volume 권장)
✅ docker-compose down 사용 (down -v 사용 금지)
✅ 버킷 자동 생성 로직 구현 (@PostConstruct 권장)
✅ healthcheck 설정 (서비스 의존성 관리)
```

### Portal Universe 현재 구현
- ✅ PERSISTENCE=1 활성화
- ✅ container_name: localstack 설정
- ✅ Named Volume: `localstack-data:/var/lib/localstack`
- ✅ healthcheck 설정
- ✅ @PostConstruct로 버킷 자동 생성 (Blog Service)
- ✅ 재시작 안전성 보장

### 중요: Community vs Pro 버전

| 기능 | Community | Pro |
|------|-----------|-----|
| PERSISTENCE 지원 | 제한적 | 완전 지원 |
| 버킷 메타데이터 영속성 | X | O |
| 파일 데이터 영속성 | 제한적 | O |

> **참고**: 완전한 영속성이 필요한 프로덕션 환경에서는 LocalStack Pro 사용을 권장합니다.

### 안전한 컨테이너 관리 명령어

```bash
# ✅ 안전: 데이터 유지
docker-compose restart localstack
docker-compose stop localstack
docker-compose down

# ⚠️ 위험: 데이터 손실!
docker-compose down -v  # 절대 사용 금지 (개발 데이터 초기화 시에만)
```

### 트러블슈팅 팁

```bash
# 1. 영속성 설정 확인
docker inspect localstack | grep -A 10 "Mounts"

# 2. 데이터 디렉토리 내용 확인
ls -laR ./localstack_data/

# 3. LocalStack 로그에서 persistence 관련 메시지 확인
docker logs localstack | grep -i persist

# 4. 버킷 자동 생성 로그 확인
docker logs blog-service | grep -i bucket
```

---

## 관련 문서
- [LocalStack Setup](./localstack-setup.md) - 설치 및 기본 설정
- [LocalStack Troubleshooting](./localstack-troubleshooting.md) - 트러블슈팅 가이드
- [S3 Best Practices](../s3/s3-best-practices.md) - S3 사용 모범 사례
- [Docker Volumes](https://docs.docker.com/storage/volumes/) - Docker 볼륨 공식 문서
