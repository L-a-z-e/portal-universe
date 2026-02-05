# File Upload - S3 (LocalStack)

## 개요

블로그 이미지 및 파일 업로드를 위한 S3 (LocalStack) 연동 방법을 학습합니다.

## 아키텍처

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Blog Service  │────▶│   S3 Client     │────▶│   LocalStack    │
│                 │     │   (AWS SDK V2)  │     │   (S3 API)      │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │
        ▼
┌─────────────────┐
│   MongoDB       │
│   (URL 저장)    │
└─────────────────┘
```

## S3 설정

### S3Config.java

```java
@Configuration
public class S3Config {

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.endpoint}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                )
            )
            .endpointOverride(URI.create(endpoint))
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true)  // LocalStack 필수
                    .build()
            )
            .build();
    }
}
```

### application-local.yml

```yaml
aws:
  s3:
    access-key: test
    secret-key: test
    region: us-east-1
    endpoint: http://localhost:4566
    bucket-name: blog-bucket
```

## FileService 구현

### 버킷 자동 생성

```java
@PostConstruct
public void ensureBucketExists() {
    try {
        s3Client.headBucket(HeadBucketRequest.builder()
            .bucket(bucketName)
            .build());
        log.info("S3 버킷 존재 확인: {}", bucketName);
    } catch (NoSuchBucketException e) {
        log.warn("S3 버킷이 존재하지 않습니다. 생성 중: {}", bucketName);
        s3Client.createBucket(CreateBucketRequest.builder()
            .bucket(bucketName)
            .build());
        log.info("S3 버킷 생성 완료: {}", bucketName);
    }
}
```

### 파일 업로드

```java
public String uploadFile(MultipartFile file) {
    validateFile(file);

    String key = generateUniqueKey(file.getOriginalFilename());

    try {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(file.getContentType())
            .contentLength(file.getSize())
            .build();

        s3Client.putObject(
            putObjectRequest,
            RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        String url = s3Client.utilities()
            .getUrl(builder -> builder.bucket(bucketName).key(key))
            .toString();

        log.info("파일 업로드 완료 - Key: {}, URL: {}", key, url);
        return url;

    } catch (IOException e) {
        throw new CustomBusinessException(BlogErrorCode.FILE_UPLOAD_FAILED);
    } catch (S3Exception e) {
        throw new CustomBusinessException(BlogErrorCode.FILE_UPLOAD_FAILED);
    }
}
```

### 파일 삭제

```java
public void deleteFile(String fileUrl) {
    try {
        String key = extractKeyFromUrl(fileUrl);

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();

        s3Client.deleteObject(deleteObjectRequest);
        log.info("파일 삭제 완료 - Key: {}", key);

    } catch (S3Exception e) {
        throw new CustomBusinessException(BlogErrorCode.FILE_DELETE_FAILED);
    }
}
```

## 파일 유효성 검증

```java
private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
    "jpg", "jpeg", "png", "gif", "webp", "svg"
);
private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

private void validateFile(MultipartFile file) {
    // 빈 파일 체크
    if (file.isEmpty()) {
        throw new CustomBusinessException(BlogErrorCode.FILE_EMPTY);
    }

    // 파일 크기 체크
    if (file.getSize() > MAX_FILE_SIZE) {
        throw new CustomBusinessException(BlogErrorCode.FILE_SIZE_EXCEEDED);
    }

    // 확장자 체크 (필요 시)
    String extension = getFileExtension(file.getOriginalFilename());
    // if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
    //     throw new CustomBusinessException(BlogErrorCode.FILE_TYPE_NOT_ALLOWED);
    // }
}
```

## 유니크 키 생성

```java
/**
 * 파일명 중복 방지를 위한 고유 키 생성
 * 형식: UUID_원본파일명
 */
private String generateUniqueKey(String originalFilename) {
    return UUID.randomUUID() + "_" + originalFilename;
}

// 예시 결과:
// "550e8400-e29b-41d4-a716-446655440000_my-image.png"
```

## URL에서 키 추출

```java
/**
 * S3 URL에서 객체 키(key) 추출
 * URL 형식: http://localhost:4566/blog-bucket/uuid_filename.jpg
 */
private String extractKeyFromUrl(String url) {
    String[] parts = url.split(bucketName + "/");
    if (parts.length < 2) {
        throw new CustomBusinessException(BlogErrorCode.INVALID_FILE_URL);
    }
    return parts[1];
}
```

## Controller

```java
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        String url = fileService.uploadFile(file);
        return ResponseEntity.ok(
            ApiResponse.success(new FileUploadResponse(url))
        );
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @RequestBody FileDeleteRequest request) {
        fileService.deleteFile(request.fileUrl());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

## Post와 이미지 연동

```java
// Post.java
private List<String> images = new ArrayList<>();

public void addImage(String imageUrl) {
    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
        this.images.add(imageUrl);
    }
}

public void removeImage(String imageUrl) {
    this.images.remove(imageUrl);
}

// 썸네일 자동 설정
public void setDefaultThumbnailIfNeeded() {
    if ((this.thumbnailUrl == null || this.thumbnailUrl.isEmpty())
            && !this.images.isEmpty()) {
        this.thumbnailUrl = this.images.get(0);
    }
}
```

## LocalStack 설정

### docker-compose.yml

```yaml
localstack:
  image: localstack/localstack:latest
  ports:
    - "4566:4566"
  environment:
    - SERVICES=s3
    - DEBUG=1
    - DATA_DIR=/tmp/localstack/data
  volumes:
    - ./localstack-data:/tmp/localstack
```

### AWS CLI로 확인

```bash
# 버킷 목록 확인
aws --endpoint-url=http://localhost:4566 s3 ls

# 버킷 내 파일 목록
aws --endpoint-url=http://localhost:4566 s3 ls s3://blog-bucket/

# 파일 다운로드 테스트
aws --endpoint-url=http://localhost:4566 s3 cp s3://blog-bucket/test.jpg ./
```

## Error Codes

```java
// BlogErrorCode.java
FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "B060", "File upload failed"),
FILE_EMPTY(HttpStatus.BAD_REQUEST, "B061", "File is empty"),
FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "B062", "File size exceeds limit"),
FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "B063", "File type not allowed"),
FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "B064", "File delete failed"),
INVALID_FILE_URL(HttpStatus.BAD_REQUEST, "B065", "Invalid file URL format");
```

## 핵심 포인트

| 항목 | 설명 |
|------|------|
| SDK | AWS SDK V2 (software.amazon.awssdk) |
| LocalStack | pathStyleAccessEnabled = true 필수 |
| 키 생성 | UUID + 원본 파일명 |
| 검증 | 크기, 확장자 체크 |
| 버킷 | 애플리케이션 시작 시 자동 생성 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/file/service/FileService.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/file/config/S3Config.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/file/controller/FileController.java`
