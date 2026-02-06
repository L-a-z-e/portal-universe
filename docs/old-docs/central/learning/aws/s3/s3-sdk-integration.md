# Spring Boot S3 SDK 통합

## 학습 목표
- AWS SDK v1과 v2의 차이점 이해
- Spring Boot에서 S3Client 설정 방법 학습
- Portal Universe의 FileService 구현 패턴 분석
- LocalStack 연동 방법 이해

---

## 1. AWS SDK 버전 비교

### 1.1 AWS SDK v1 vs v2

| 항목 | SDK v1 | SDK v2 |
|------|--------|--------|
| **패키지** | `com.amazonaws` | `software.amazon.awssdk` |
| **빌더 패턴** | 제한적 | 완전한 빌더 지원 |
| **비동기 지원** | 별도 클라이언트 | 통합 지원 |
| **성능** | 보통 | 향상 |
| **메모리** | 많음 | 최적화 |
| **Java 버전** | Java 6+ | Java 8+ |
| **상태** | 유지보수 모드 | 활발히 개발 |

**Portal Universe는 AWS SDK v2 사용합니다.**

### 1.2 주요 차이점

```java
// ===== AWS SDK v1 =====
AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
        .withRegion(Regions.AP_NORTHEAST_2)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();

// PutObject
s3Client.putObject(bucketName, key, file);

// ===== AWS SDK v2 =====
S3Client s3Client = S3Client.builder()
        .region(Region.AP_NORTHEAST_2)
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .build();

// PutObject (빌더 패턴)
PutObjectRequest putRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(key)
        .contentType("image/jpeg")
        .build();

s3Client.putObject(putRequest, RequestBody.fromFile(file));
```

---

## 2. Spring Boot 프로젝트 설정

### 2.1 Gradle 의존성

```groovy
// build.gradle
dependencies {
    // AWS SDK v2 BOM (Bill of Materials)
    implementation platform('software.amazon.awssdk:bom:2.20.26')

    // S3만 사용
    implementation 'software.amazon.awssdk:s3'

    // 또는 전체 AWS 서비스
    // implementation 'software.amazon.awssdk:aws-sdk-java'
}
```

**Portal Universe Blog Service:**
```groovy
// services/blog-service/build.gradle
dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // AWS SDK v2
    implementation platform('software.amazon.awssdk:bom:2.20.26')
    implementation 'software.amazon.awssdk:s3'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

### 2.2 application.yml 설정

```yaml
# services/blog-service/src/main/resources/application.yml
aws:
  s3:
    endpoint: http://localhost:4566  # LocalStack 엔드포인트
    region: ap-northeast-2
    access-key: test                 # LocalStack 기본값
    secret-key: test                 # LocalStack 기본값
    bucket-name: blog-bucket

---
# 프로덕션 환경 (AWS 실제 서비스)
spring:
  config:
    activate:
      on-profile: prod

aws:
  s3:
    endpoint: https://s3.ap-northeast-2.amazonaws.com
    region: ap-northeast-2
    access-key: ${AWS_ACCESS_KEY}   # 환경 변수
    secret-key: ${AWS_SECRET_KEY}   # 환경 변수
    bucket-name: prod-blog-bucket
```

---

## 3. S3Config 구성

### 3.1 기본 설정

```java
package com.portal.universe.blogservice.file.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

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
                // 1. 리전 설정
                .region(Region.of(region))

                // 2. 자격 증명 설정
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )

                // 3. 엔드포인트 오버라이드 (LocalStack용)
                .endpointOverride(URI.create(endpoint))

                // 4. Path-Style Access 활성화 (LocalStack 필수)
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build()
                )

                .build();
    }
}
```

### 3.2 설정 설명

#### Path-Style vs Virtual-Hosted-Style

```
Path-Style (LocalStack):
http://localhost:4566/blog-bucket/file.jpg
     └─endpoint─┘ └─bucket─┘ └─key─┘

Virtual-Hosted-Style (AWS 기본):
https://blog-bucket.s3.ap-northeast-2.amazonaws.com/file.jpg
       └─bucket─┘ └────region────┘           └─key─┘
```

**LocalStack은 Path-Style만 지원하므로 반드시 활성화해야 합니다:**
```java
.pathStyleAccessEnabled(true)
```

#### 프로덕션 환경 (AWS 실제 서비스)

```java
@Bean
@Profile("prod")
public S3Client s3ClientProd() {
    return S3Client.builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(
                    DefaultCredentialsProvider.create()  // IAM Role 자동 감지
            )
            // endpointOverride 없음 (AWS 기본 엔드포인트 사용)
            .build();
}
```

---

## 4. FileService 구현

### 4.1 전체 구조

```java
package com.portal.universe.blogservice.file.service;

import com.portal.universe.blogservice.exception.BlogErrorCode;
import com.portal.universe.commonlibrary.exception.CustomBusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    // 버킷 자동 생성
    @PostConstruct
    public void ensureBucketExists() { /* ... */ }

    // 파일 업로드
    public String uploadFile(MultipartFile file) { /* ... */ }

    // 파일 삭제
    public void deleteFile(String fileUrl) { /* ... */ }

    // 유틸리티 메서드들
    private void validateFile(MultipartFile file) { /* ... */ }
    private String generateUniqueKey(String originalFilename) { /* ... */ }
    private String extractKeyFromUrl(String url) { /* ... */ }
}
```

### 4.2 버킷 자동 생성

```java
/**
 * 애플리케이션 시작 시 S3 버킷 존재 확인 및 자동 생성
 */
@PostConstruct
public void ensureBucketExists() {
    try {
        // HeadBucket - 버킷 존재 확인
        s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(bucketName)
                .build());

        log.info("✅ S3 버킷 존재 확인: {}", bucketName);

    } catch (NoSuchBucketException e) {
        // 버킷이 없으면 생성
        log.warn("⚠️ S3 버킷이 존재하지 않습니다. 생성 중: {}", bucketName);

        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(bucketName)
                .build());

        log.info("✅ S3 버킷 생성 완료: {}", bucketName);

    } catch (Exception e) {
        log.error("❌ S3 버킷 확인 중 오류 발생: {}", e.getMessage());
    }
}
```

**이점:**
- 개발 환경에서 수동 버킷 생성 불필요
- LocalStack 재시작 시 자동 복구
- 인프라 코드화 (Infrastructure as Code)

### 4.3 파일 업로드

```java
public String uploadFile(MultipartFile file) {
    // 1. 유효성 검증
    validateFile(file);

    // 2. 고유 키 생성 (UUID_원본파일명)
    String key = generateUniqueKey(file.getOriginalFilename());

    try {
        // 3. PutObjectRequest 생성
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())  // MIME 타입 설정
                .contentLength(file.getSize())       // 파일 크기
                .build();

        // 4. S3에 업로드
        s3Client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        // 5. URL 생성
        String url = s3Client.utilities()
                .getUrl(builder -> builder.bucket(bucketName).key(key))
                .toString();

        log.info("✅ 파일 업로드 완료 - Key: {}, URL: {}", key, url);
        return url;

    } catch (IOException e) {
        log.error("❌ 파일 읽기 실패: {}", e.getMessage());
        throw new CustomBusinessException(BlogErrorCode.FILE_UPLOAD_FAILED);

    } catch (S3Exception e) {
        log.error("❌ S3 업로드 실패: {}", e.getMessage());
        throw new CustomBusinessException(BlogErrorCode.FILE_UPLOAD_FAILED);
    }
}
```

### 4.4 파일 유효성 검증

```java
private void validateFile(MultipartFile file) {
    // 1. 빈 파일 체크
    if (file.isEmpty()) {
        throw new CustomBusinessException(BlogErrorCode.FILE_EMPTY);
    }

    // 2. 파일 크기 체크 (100MB)
    if (file.getSize() > MAX_FILE_SIZE) {
        throw new CustomBusinessException(BlogErrorCode.FILE_SIZE_EXCEEDED);
    }

    // 3. 확장자 검증 (선택적)
    String extension = getFileExtension(file.getOriginalFilename());
    List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif");

    if (!allowedExtensions.contains(extension.toLowerCase())) {
        throw new CustomBusinessException(BlogErrorCode.INVALID_FILE_TYPE);
    }
}
```

### 4.5 고유 키 생성

```java
/**
 * 파일명 중복 방지를 위한 고유 키 생성
 * 형식: UUID_원본파일명
 */
private String generateUniqueKey(String originalFilename) {
    return UUID.randomUUID() + "_" + originalFilename;
}

// 생성 예시:
// "550e8400-e29b-41d4-a716-446655440000_profile.jpg"
```

**이점:**
- 동일한 파일명 업로드 시 덮어쓰기 방지
- 파일 추적 용이
- 원본 파일명 보존

### 4.6 파일 삭제

```java
public void deleteFile(String fileUrl) {
    try {
        // 1. URL에서 키 추출
        String key = extractKeyFromUrl(fileUrl);

        // 2. DeleteObjectRequest 생성
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        // 3. S3에서 삭제
        s3Client.deleteObject(deleteRequest);

        log.info("✅ 파일 삭제 완료 - Key: {}", key);

    } catch (S3Exception e) {
        log.error("❌ S3 파일 삭제 실패: {}", e.getMessage());
        throw new CustomBusinessException(BlogErrorCode.FILE_DELETE_FAILED);
    }
}

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

---

## 5. Controller 구현

```java
package com.portal.universe.blogservice.file.controller;

import com.portal.universe.blogservice.file.service.FileService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 파일 업로드
     *
     * POST /api/v1/files/upload
     * Content-Type: multipart/form-data
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        String fileUrl = fileService.uploadFile(file);
        return ResponseEntity.ok(ApiResponse.success(fileUrl));
    }

    /**
     * 파일 삭제
     *
     * DELETE /api/v1/files?url=http://localhost:4566/blog-bucket/uuid_file.jpg
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @RequestParam("url") String fileUrl) {

        fileService.deleteFile(fileUrl);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

---

## 6. 실습 예제

### 6.1 LocalStack 실행

```bash
# Docker Compose로 LocalStack 실행
cd /path/to/portal-universe
docker compose up -d localstack

# 로그 확인
docker compose logs -f localstack
```

### 6.2 Blog Service 실행

```bash
cd services/blog-service
./gradlew bootRun

# 실행 시 로그:
# ✅ S3 버킷 존재 확인: blog-bucket
# 또는
# ⚠️ S3 버킷이 존재하지 않습니다. 생성 중: blog-bucket
# ✅ S3 버킷 생성 완료: blog-bucket
```

### 6.3 파일 업로드 테스트

```bash
# cURL로 파일 업로드
curl -X POST http://localhost:8082/api/v1/files/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/test-image.jpg"

# 응답:
# {
#   "success": true,
#   "data": "http://localhost:4566/blog-bucket/550e8400-e29b-41d4-a716-446655440000_test-image.jpg",
#   "error": null
# }
```

### 6.4 파일 확인

```bash
# AWS CLI로 버킷 내용 확인
awslocal s3 ls s3://blog-bucket/

# 출력:
# 2024-01-22 10:30:00  1048576 550e8400-e29b-41d4-a716-446655440000_test-image.jpg
```

### 6.5 파일 삭제 테스트

```bash
curl -X DELETE "http://localhost:8082/api/v1/files?url=http://localhost:4566/blog-bucket/550e8400-e29b-41d4-a716-446655440000_test-image.jpg" \
  -H "Authorization: Bearer YOUR_TOKEN"

# 응답:
# {
#   "success": true,
#   "data": null,
#   "error": null
# }
```

---

## 7. 에러 처리

### 7.1 BlogErrorCode 정의

```java
package com.portal.universe.blogservice.exception;

import com.portal.universe.commonlibrary.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BlogErrorCode implements ErrorCode {

    // 파일 관련 에러 (B050~B059)
    FILE_EMPTY("B050", "업로드할 파일이 없습니다"),
    FILE_SIZE_EXCEEDED("B051", "파일 크기가 100MB를 초과합니다"),
    INVALID_FILE_TYPE("B052", "허용되지 않는 파일 형식입니다"),
    FILE_UPLOAD_FAILED("B053", "파일 업로드에 실패했습니다"),
    FILE_DELETE_FAILED("B054", "파일 삭제에 실패했습니다"),
    INVALID_FILE_URL("B055", "유효하지 않은 파일 URL입니다");

    private final String code;
    private final String message;
}
```

### 7.2 GlobalExceptionHandler (공통 처리)

```java
// common-library에서 모든 CustomBusinessException을 처리
@ExceptionHandler(CustomBusinessException.class)
public ResponseEntity<ApiResponse<Void>> handleBusinessException(
        CustomBusinessException e) {

    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(e.getErrorCode()));
}
```

---

## 8. 핵심 정리

| 항목 | 내용 |
|------|------|
| **AWS SDK v2** | `software.amazon.awssdk:s3` |
| **S3Client** | 빌더 패턴으로 생성, Bean 등록 |
| **Path-Style** | LocalStack 필수 설정 |
| **@PostConstruct** | 버킷 자동 생성 |
| **UUID Prefix** | 파일명 중복 방지 |
| **에러 처리** | CustomBusinessException → GlobalExceptionHandler |

---

## 다음 학습

- [S3 권한 설정](./s3-permissions.md)
- [S3 모범 사례](./s3-best-practices.md)

---

## 참고 자료

- [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [S3Client API](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/S3Client.html)
- [LocalStack S3](https://docs.localstack.cloud/user-guide/aws/s3/)
