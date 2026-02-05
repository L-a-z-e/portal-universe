# LocalStack S3

LocalStack을 사용하여 AWS S3를 로컬 환경에서 에뮬레이션하는 방법을 학습합니다.

---

## 1. LocalStack 개요

### LocalStack이란?

**LocalStack**은 AWS 클라우드 서비스를 로컬 환경에서 에뮬레이션하는 도구입니다. 실제 AWS 계정 없이도 S3, SQS, SNS, DynamoDB 등의 서비스를 테스트할 수 있습니다.

### 장점

| 장점 | 설명 |
|------|------|
| 비용 절감 | AWS 비용 없이 개발/테스트 가능 |
| 빠른 피드백 | 네트워크 지연 없이 즉시 테스트 |
| 오프라인 개발 | 인터넷 연결 없이 개발 가능 |
| CI/CD 통합 | 테스트 파이프라인에 쉽게 통합 |

### 지원 서비스 (무료)

- **S3** - 객체 스토리지
- **SQS** - 메시지 큐
- **SNS** - 알림 서비스
- **DynamoDB** - NoSQL 데이터베이스
- **Lambda** - 서버리스 함수
- **API Gateway** - API 관리

---

## 2. Portal Universe LocalStack 설정

### docker-compose.yml 설정

```yaml
# docker-compose.yml
services:
  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"              # 모든 서비스의 단일 엔드포인트
    environment:
      - SERVICES=s3              # 사용할 서비스 지정
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - AWS_DEFAULT_REGION=ap-northeast-2
      - PERSISTENCE=1            # 데이터 영속성 활성화
      - DATA_DIR=/var/lib/localstack
    volumes:
      - ./localstack_data:/var/lib/localstack  # 데이터 영속화
    networks:
      - portal-universe-net
```

### 환경 변수 설명

| 환경 변수 | 설명 |
|----------|------|
| `SERVICES` | 활성화할 AWS 서비스 목록 |
| `AWS_ACCESS_KEY_ID` | 테스트용 액세스 키 |
| `AWS_SECRET_ACCESS_KEY` | 테스트용 시크릿 키 |
| `AWS_DEFAULT_REGION` | 기본 리전 |
| `PERSISTENCE` | 재시작 후 데이터 유지 (1=활성화) |
| `DATA_DIR` | 데이터 저장 경로 |

---

## 3. AWS CLI 설정

### LocalStack 전용 프로필 생성

```bash
# ~/.aws/credentials에 추가
[localstack]
aws_access_key_id = test
aws_secret_access_key = test

# ~/.aws/config에 추가
[profile localstack]
region = ap-northeast-2
output = json
```

### 엔드포인트 지정 사용

```bash
# 매번 --endpoint-url 지정
aws --endpoint-url=http://localhost:4566 s3 ls

# 또는 alias 설정
alias awslocal='aws --endpoint-url=http://localhost:4566'
awslocal s3 ls
```

### awslocal CLI 설치 (권장)

```bash
# pip로 설치
pip install awscli-local

# 사용
awslocal s3 ls
```

---

## 4. S3 버킷 생성 및 관리

### 버킷 생성

```bash
# 버킷 생성
awslocal s3 mb s3://portal-universe-uploads

# 버킷 목록 확인
awslocal s3 ls

# 버킷 설정 확인
awslocal s3api get-bucket-location --bucket portal-universe-uploads
```

### 파일 업로드/다운로드

```bash
# 단일 파일 업로드
awslocal s3 cp ./image.png s3://portal-universe-uploads/images/

# 디렉토리 전체 업로드
awslocal s3 sync ./uploads/ s3://portal-universe-uploads/

# 파일 다운로드
awslocal s3 cp s3://portal-universe-uploads/images/image.png ./downloaded.png

# 버킷 내용 확인
awslocal s3 ls s3://portal-universe-uploads/ --recursive
```

### 파일 삭제

```bash
# 단일 파일 삭제
awslocal s3 rm s3://portal-universe-uploads/images/image.png

# 버킷 전체 비우기
awslocal s3 rm s3://portal-universe-uploads/ --recursive

# 버킷 삭제 (비어있어야 함)
awslocal s3 rb s3://portal-universe-uploads
```

---

## 5. Spring Boot S3 연동

### 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    implementation("software.amazon.awssdk:s3:2.20.0")
    implementation("software.amazon.awssdk:sts:2.20.0")
}
```

### S3 Configuration

```java
@Configuration
@Profile({"local", "docker"})
public class LocalStackS3Config {

    @Value("${aws.s3.endpoint:http://localhost:4566}")
    private String s3Endpoint;

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .endpointOverride(URI.create(s3Endpoint))
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test", "test")
            ))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)  // LocalStack 필수
                .build())
            .build();
    }
}
```

### application.yml 설정

```yaml
# application-local.yml
aws:
  s3:
    endpoint: http://localhost:4566
    bucket: portal-universe-uploads
  region: ap-northeast-2

# application-docker.yml
aws:
  s3:
    endpoint: http://localstack:4566
    bucket: portal-universe-uploads
  region: ap-northeast-2
```

### S3 Service 구현

```java
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    /**
     * 파일 업로드
     */
    public String uploadFile(String key, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));

        return generateUrl(key);
    }

    /**
     * 파일 다운로드
     */
    public byte[] downloadFile(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();

        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    /**
     * 파일 삭제
     */
    public void deleteFile(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();

        s3Client.deleteObject(request);
    }

    /**
     * Pre-signed URL 생성
     */
    public String generatePresignedUrl(String key, Duration expiration) {
        S3Presigner presigner = S3Presigner.builder()
            .region(Region.of("ap-northeast-2"))
            .endpointOverride(URI.create("http://localhost:4566"))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test", "test")
            ))
            .build();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .getObjectRequest(getObjectRequest)
            .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }

    private String generateUrl(String key) {
        return String.format("http://localhost:4566/%s/%s", bucketName, key);
    }
}
```

### Controller 예시

```java
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final S3StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file) throws IOException {

        String key = "uploads/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        String url = storageService.uploadFile(
            key,
            file.getBytes(),
            file.getContentType()
        );

        return ResponseEntity.ok(ApiResponse.success(
            new FileUploadResponse(key, url)
        ));
    }

    @GetMapping("/download/{key}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String key) {
        byte[] content = storageService.downloadFile(key);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + key + "\"")
            .body(content);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String key) {
        storageService.deleteFile(key);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

---

## 6. 초기화 스크립트

### 버킷 자동 생성

```bash
#!/bin/bash
# init-localstack.sh

# LocalStack이 준비될 때까지 대기
echo "Waiting for LocalStack to be ready..."
until aws --endpoint-url=http://localhost:4566 s3 ls > /dev/null 2>&1; do
  sleep 1
done

echo "LocalStack is ready!"

# 버킷 생성
echo "Creating S3 buckets..."
aws --endpoint-url=http://localhost:4566 s3 mb s3://portal-universe-uploads
aws --endpoint-url=http://localhost:4566 s3 mb s3://portal-universe-backups

# 버킷 정책 설정 (퍼블릭 읽기)
cat > /tmp/bucket-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::portal-universe-uploads/*"
    }
  ]
}
EOF

aws --endpoint-url=http://localhost:4566 s3api put-bucket-policy \
  --bucket portal-universe-uploads \
  --policy file:///tmp/bucket-policy.json

echo "S3 buckets created successfully!"
```

### docker-compose에서 실행

```yaml
services:
  localstack-init:
    image: amazon/aws-cli
    depends_on:
      - localstack
    environment:
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - AWS_DEFAULT_REGION=ap-northeast-2
    entrypoint: /bin/sh
    command: >
      -c "
        until aws --endpoint-url=http://localstack:4566 s3 ls; do
          echo 'Waiting for LocalStack...'
          sleep 2
        done
        aws --endpoint-url=http://localstack:4566 s3 mb s3://portal-universe-uploads
      "
    networks:
      - portal-universe-net
```

---

## 7. 테스트 작성

### Integration Test

```java
@SpringBootTest
@Testcontainers
class S3StorageServiceTest {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:latest")
    ).withServices(LocalStackContainer.Service.S3);

    @Autowired
    private S3StorageService storageService;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.s3.endpoint",
            () -> localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
    }

    @BeforeAll
    static void createBucket() throws Exception {
        S3Client s3Client = S3Client.builder()
            .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    localstack.getAccessKey(),
                    localstack.getSecretKey()
                )
            ))
            .build();

        s3Client.createBucket(b -> b.bucket("portal-universe-uploads"));
    }

    @Test
    void uploadFile_ShouldSucceed() {
        // given
        byte[] content = "test content".getBytes();
        String key = "test/file.txt";

        // when
        String url = storageService.uploadFile(key, content, "text/plain");

        // then
        assertThat(url).contains(key);
    }

    @Test
    void downloadFile_ShouldReturnContent() {
        // given
        byte[] content = "test content".getBytes();
        String key = "test/download.txt";
        storageService.uploadFile(key, content, "text/plain");

        // when
        byte[] downloaded = storageService.downloadFile(key);

        // then
        assertThat(downloaded).isEqualTo(content);
    }
}
```

---

## 8. 트러블슈팅

### 일반적인 문제

| 문제 | 원인 | 해결 방법 |
|------|------|----------|
| 연결 실패 | LocalStack 미실행 | `docker-compose up localstack` 확인 |
| Bucket not found | 버킷 미생성 | 초기화 스크립트 실행 |
| SignatureDoesNotMatch | 잘못된 credentials | test/test 사용 확인 |
| Path-style 오류 | SDK 설정 문제 | `pathStyleAccessEnabled(true)` 설정 |

### 디버깅

```bash
# LocalStack 로그 확인
docker-compose logs localstack

# 헬스 체크
curl http://localhost:4566/_localstack/health

# 서비스 상태 확인
curl http://localhost:4566/_localstack/info
```

---

## 9. 관련 문서

- [Docker Compose](./docker-compose.md) - 멀티 컨테이너 구성
- [Portal Universe Infra Guide](./portal-universe-infra-guide.md) - 전체 인프라 가이드
