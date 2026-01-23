# LocalStack Setup

## 학습 목표
- LocalStack이 무엇이고 왜 필요한지 이해한다
- Docker를 이용한 LocalStack 설치 및 실행 방법을 습득한다
- Portal Universe 프로젝트에 적합한 환경 설정을 구성한다
- awslocal CLI를 이용한 관리 방법을 익힌다

---

## 개념 설명

### LocalStack이란?
LocalStack은 AWS 클라우드 서비스를 로컬 환경에서 에뮬레이트하는 오픈소스 프로젝트입니다.

#### 왜 LocalStack을 사용하는가?
1. **비용 절감**: 개발/테스트 중 실제 AWS 리소스 사용으로 인한 비용 발생 방지
2. **개발 속도**: 실제 AWS 계정 설정 없이 즉시 개발 시작 가능
3. **오프라인 개발**: 인터넷 연결 없이도 AWS 서비스 개발/테스트 가능
4. **안전한 실험**: 실제 인프라에 영향 없이 자유롭게 테스트
5. **CI/CD 통합**: 파이프라인에서 AWS 리소스 의존성 테스트 가능

#### 지원 서비스
- **Core Services** (무료): S3, Lambda, DynamoDB, Kinesis, CloudWatch 등
- **Pro Services** (유료): RDS, EKS, ECS, Cognito 등
- [전체 목록 보기](https://docs.localstack.cloud/user-guide/aws/feature-coverage/)

---

## Portal Universe 적용

### 1. Docker Compose 설정

현재 Portal Universe 프로젝트의 LocalStack 설정:

```yaml
# docker-compose.yml
services:
  localstack:
    image: localstack/localstack:latest
    container_name: localstack
    ports:
      - "4566:4566"            # LocalStack Gateway (모든 AWS API 요청)
      - "4510-4559:4510-4559"  # 외부 서비스 포트 범위 (선택사항)
    environment:
      # 1. 활성화할 AWS 서비스 지정
      - SERVICES=s3

      # 2. AWS Credentials (로컬 개발용 더미 값)
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - AWS_DEFAULT_REGION=ap-northeast-2

      # 3. 데이터 영속성 활성화
      - PERSISTENCE=1
      - DATA_DIR=/var/lib/localstack

      # 4. 선택적 설정
      # - DEBUG=1                    # 디버그 로그 활성화
      # - LAMBDA_EXECUTOR=docker     # Lambda 실행 모드
      # - SKIP_SSL_CERT_DOWNLOAD=1   # SSL 인증서 다운로드 스킵
    volumes:
      # 데이터 영속성을 위한 볼륨 마운트
      - ./localstack_data:/var/lib/localstack
      # 초기화 스크립트 (선택사항)
      # - ./init-scripts:/etc/localstack/init/ready.d
    networks:
      - portal-universe-net

networks:
  portal-universe-net:
    driver: bridge
```

### 2. 환경변수 설명

| 변수 | 값 | 설명 |
|------|-----|------|
| `SERVICES` | `s3` | 활성화할 서비스 (콤마로 구분: `s3,sqs,sns`) |
| `AWS_ACCESS_KEY_ID` | `test` | 로컬 개발용 더미 Access Key |
| `AWS_SECRET_ACCESS_KEY` | `test` | 로컬 개발용 더미 Secret Key |
| `AWS_DEFAULT_REGION` | `ap-northeast-2` | 서울 리전 (실제 배포 환경과 동일하게 설정 권장) |
| `PERSISTENCE` | `1` | 데이터 영속성 활성화 (재시작 후에도 데이터 유지) |
| `DATA_DIR` | `/var/lib/localstack` | 컨테이너 내부 데이터 저장 경로 |
| `DEBUG` | `1` | 디버그 모드 (문제 발생 시 활성화) |

### 3. 포트 구조

```
┌─────────────────────────────────────┐
│  LocalStack Container               │
│                                     │
│  ┌───────────────────────────────┐ │
│  │  Port 4566 (Gateway)          │ │  <- 모든 AWS API 엔드포인트
│  │  ├─ S3                        │ │
│  │  ├─ DynamoDB                  │ │
│  │  ├─ Lambda                    │ │
│  │  └─ ...                       │ │
│  └───────────────────────────────┘ │
│                                     │
│  Data: /var/lib/localstack         │
└─────────────────────────────────────┘
         │
         │ Volume Mount
         ▼
   ./localstack_data/
```

---

## 실습 예제

### 실습 1: LocalStack 설치 및 실행

#### Step 1: Docker Compose로 시작
```bash
cd /Users/laze/Laze/Project/portal-universe

# LocalStack 컨테이너 시작
docker-compose up -d localstack

# 로그 확인
docker logs -f localstack
```

#### Step 2: 헬스 체크
```bash
# LocalStack이 정상 실행 중인지 확인
curl http://localhost:4566/_localstack/health

# 예상 출력:
# {
#   "services": {
#     "s3": "running"
#   },
#   "version": "3.0.0"
# }
```

#### Step 3: 데이터 디렉토리 확인
```bash
# 데이터 영속성을 위한 디렉토리 생성 확인
ls -la ./localstack_data/

# 권한 문제 발생 시 (macOS/Linux)
sudo chown -R $(whoami):$(id -gn) ./localstack_data
```

### 실습 2: awslocal CLI 설치

#### awslocal이란?
AWS CLI의 LocalStack 래퍼로, 자동으로 `--endpoint-url=http://localhost:4566`을 추가합니다.

#### 설치 방법

**Option 1: pip로 설치 (권장)**
```bash
pip install awscli-local

# 또는 Python 없이 standalone 설치
pip install awscli-local[ver1]
```

**Option 2: Homebrew로 설치 (macOS)**
```bash
brew install awscli-local
```

**Option 3: npm으로 설치**
```bash
npm install -g @localstack/awscli-local
```

#### 사용 방법
```bash
# AWS CLI 명령어 앞에 'local'만 추가
awslocal s3 ls
awslocal s3 mb s3://my-bucket
awslocal s3 cp file.txt s3://my-bucket/

# 일반 AWS CLI와 동일한 문법
awslocal dynamodb list-tables
awslocal lambda list-functions
```

### 실습 3: S3 버킷 생성 및 테스트

```bash
# 1. 버킷 생성
awslocal s3 mb s3://portal-blog-uploads

# 2. 버킷 목록 확인
awslocal s3 ls

# 3. 테스트 파일 업로드
echo "Hello LocalStack" > test.txt
awslocal s3 cp test.txt s3://portal-blog-uploads/

# 4. 파일 목록 확인
awslocal s3 ls s3://portal-blog-uploads/

# 5. 파일 다운로드
awslocal s3 cp s3://portal-blog-uploads/test.txt downloaded.txt
cat downloaded.txt
```

### 실습 4: Spring Boot 애플리케이션 연동

```yaml
# application-local.yml
spring:
  cloud:
    aws:
      s3:
        endpoint: http://localhost:4566  # LocalStack 엔드포인트
        region:
          static: ap-northeast-2
        credentials:
          access-key: test
          secret-key: test
        path-style-access-enabled: true   # LocalStack은 path-style 필수
```

```java
// AWS S3 Client 설정
@Configuration
public class AwsConfig {

    @Value("${spring.cloud.aws.s3.endpoint}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test", "test")
                )
            )
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)  // LocalStack은 반드시 true
                    .build()
            )
            .build();
    }
}
```

### 실습 5: 컨테이너 관리

```bash
# LocalStack 재시작 (데이터 유지)
docker-compose restart localstack

# LocalStack 중지
docker-compose stop localstack

# LocalStack 완전 삭제 (데이터 유지)
docker-compose down

# LocalStack 완전 삭제 + 볼륨 삭제 (⚠️ 데이터 손실!)
docker-compose down -v

# 특정 서비스만 재시작
docker-compose up -d --force-recreate localstack
```

---

## 핵심 요약

### LocalStack 기본 개념
- AWS 서비스를 로컬에서 에뮬레이트하는 개발 도구
- Docker를 통해 단일 컨테이너로 여러 AWS 서비스 실행
- 포트 4566으로 모든 AWS API 요청 처리

### 필수 설정 항목
1. **SERVICES**: 사용할 AWS 서비스 목록
2. **Credentials**: test/test (로컬 개발용 더미 값)
3. **PERSISTENCE**: 데이터 영속성 활성화
4. **Volume**: 데이터 저장을 위한 볼륨 마운트

### Portal Universe 사용 중인 서비스
- **S3**: Blog Service의 파일 업로드 (이미지, 첨부파일)
- **추후 확장 가능**: SQS (비동기 작업), SNS (알림), Lambda (이벤트 처리)

### 주의사항
- `docker-compose down -v` 실행 시 모든 데이터 삭제됨
- Spring Boot에서는 `pathStyleAccessEnabled(true)` 필수 설정
- Region은 실제 배포 환경과 동일하게 설정 권장 (`ap-northeast-2`)

### 트러블슈팅 명령어
```bash
# 헬스 체크
curl http://localhost:4566/_localstack/health

# 로그 확인
docker logs -f localstack

# 디버그 모드 활성화
docker-compose up -d -e DEBUG=1 localstack
```

---

## 관련 문서
- [LocalStack Persistence](./localstack-persistence.md) - 데이터 영속성 문제 해결 ⭐
- [LocalStack Services](./localstack-services.md) - 지원 서비스별 가이드
- [LocalStack Troubleshooting](./localstack-troubleshooting.md) - 트러블슈팅 가이드
- [AWS S3 Fundamentals](../s3/s3-fundamentals.md) - S3 기본 개념
- [Portal Universe Docker Compose](../../../../docker-compose.yml) - 전체 인프라 구성
