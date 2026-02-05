# S3 CRUD 연산

## 학습 목표
- AWS CLI를 사용한 S3 기본 조작 이해
- 버킷 생성, 파일 업로드/다운로드 실습
- Pre-signed URL 개념과 활용 학습
- Multipart Upload 이해

---

## 1. AWS CLI 설치 및 설정

### 1.1 설치

```bash
# macOS
brew install awscli

# Linux
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# 설치 확인
aws --version
```

### 1.2 LocalStack용 awslocal 설치

```bash
pip install awscli-local

# 확인
awslocal --version
```

`awslocal`은 `aws` CLI를 LocalStack 엔드포인트로 자동 리다이렉트합니다.

```bash
# 일반 AWS CLI (AWS 실제 서비스 호출)
aws s3 ls

# LocalStack CLI (로컬 LocalStack 호출)
awslocal s3 ls
# 또는
aws --endpoint-url=http://localhost:4566 s3 ls
```

---

## 2. 버킷 CRUD

### 2.1 버킷 생성 (mb = make bucket)

```bash
# LocalStack
awslocal s3 mb s3://my-bucket

# AWS 실제 환경
aws s3 mb s3://my-unique-bucket-name-2024 --region ap-northeast-2
```

**Portal Universe 예시:**
```bash
# Blog Service 버킷 생성
awslocal s3 mb s3://blog-bucket

# 확인
awslocal s3 ls
# 출력: 2024-01-22 10:30:00 blog-bucket
```

### 2.2 버킷 목록 조회

```bash
# 모든 버킷 나열
awslocal s3 ls

# 특정 버킷 내용 보기
awslocal s3 ls s3://blog-bucket

# 재귀적으로 모든 객체 보기
awslocal s3 ls s3://blog-bucket --recursive
```

### 2.3 버킷 삭제 (rb = remove bucket)

```bash
# 빈 버킷 삭제
awslocal s3 rb s3://my-bucket

# 객체가 있는 버킷 강제 삭제
awslocal s3 rb s3://my-bucket --force
```

---

## 3. 객체 CRUD

### 3.1 파일 업로드 (cp = copy)

```bash
# 로컬 → S3
awslocal s3 cp myfile.txt s3://blog-bucket/

# 특정 경로에 업로드
awslocal s3 cp image.jpg s3://blog-bucket/images/

# 메타데이터 추가
awslocal s3 cp report.pdf s3://blog-bucket/documents/ \
    --metadata uploader=john,date=2024-01-22

# Content-Type 명시
awslocal s3 cp video.mp4 s3://blog-bucket/videos/ \
    --content-type video/mp4
```

### 3.2 파일 다운로드

```bash
# S3 → 로컬
awslocal s3 cp s3://blog-bucket/myfile.txt ./

# 다른 이름으로 다운로드
awslocal s3 cp s3://blog-bucket/myfile.txt ./downloaded.txt

# 디렉토리 전체 다운로드
awslocal s3 cp s3://blog-bucket/images/ ./local-images/ --recursive
```

### 3.3 파일 삭제

```bash
# 단일 파일 삭제
awslocal s3 rm s3://blog-bucket/myfile.txt

# 디렉토리 전체 삭제
awslocal s3 rm s3://blog-bucket/images/ --recursive

# 특정 패턴 파일 삭제
awslocal s3 rm s3://blog-bucket/ --recursive --exclude "*" --include "*.tmp"
```

### 3.4 파일 이동/복사

```bash
# S3 내 파일 이동
awslocal s3 mv s3://blog-bucket/old.txt s3://blog-bucket/new.txt

# S3 간 복사
awslocal s3 cp s3://blog-bucket/file.txt s3://backup-bucket/
```

---

## 4. 동기화 (sync)

로컬 디렉토리와 S3 버킷을 **동기화**합니다.

### 4.1 로컬 → S3 동기화

```bash
# 로컬 디렉토리를 S3에 업로드 (변경된 파일만)
awslocal s3 sync ./local-dir s3://blog-bucket/backup/

# 삭제된 파일도 동기화
awslocal s3 sync ./local-dir s3://blog-bucket/backup/ --delete
```

### 4.2 S3 → 로컬 동기화

```bash
# S3 버킷을 로컬로 다운로드
awslocal s3 sync s3://blog-bucket/backup/ ./local-dir/

# 특정 파일 타입만 동기화
awslocal s3 sync s3://blog-bucket/ ./local-images/ \
    --exclude "*" --include "*.jpg"
```

### 4.3 실전 예시

```bash
# 웹사이트 정적 파일 배포
awslocal s3 sync ./dist s3://my-website-bucket/ \
    --delete \
    --cache-control "max-age=31536000" \
    --exclude "*.html" \
    --include "*"

awslocal s3 sync ./dist s3://my-website-bucket/ \
    --cache-control "max-age=0" \
    --include "*.html"
```

---

## 5. Pre-signed URL

**일시적인 접근 권한**을 부여하는 URL입니다.

### 5.1 개념

```
일반 S3 URL (비공개):
https://blog-bucket.s3.amazonaws.com/private-file.pdf
→ 403 Forbidden (권한 없음)

Pre-signed URL (만료 시간 포함):
https://blog-bucket.s3.amazonaws.com/private-file.pdf
  ?X-Amz-Algorithm=AWS4-HMAC-SHA256
  &X-Amz-Credential=...
  &X-Amz-Date=20240122T100000Z
  &X-Amz-Expires=3600
  &X-Amz-Signature=...
→ 200 OK (1시간 동안 다운로드 가능)
```

### 5.2 CLI로 생성

```bash
# 1시간(3600초) 유효한 다운로드 URL 생성
awslocal s3 presign s3://blog-bucket/report.pdf --expires-in 3600

# 출력 예시:
# http://localhost:4566/blog-bucket/report.pdf
# ?X-Amz-Algorithm=AWS4-HMAC-SHA256
# &X-Amz-Credential=test/20240122/...
# &X-Amz-Expires=3600
# &X-Amz-Signature=abc123...
```

### 5.3 SDK로 생성 (Java)

```java
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import java.time.Duration;

public String generatePresignedUrl(String key) {
    try (S3Presigner presigner = S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2)
            .build()) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))  // 1시간 유효
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest =
                presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }
}
```

### 5.4 활용 사례

| 사용 사례 | 설명 |
|-----------|------|
| **파일 공유** | 비공개 파일을 특정 사용자에게 임시 공유 |
| **다운로드 링크** | 이메일로 보내는 다운로드 링크 |
| **업로드 허용** | 클라이언트가 직접 S3에 업로드 (서버 부하 감소) |
| **미디어 스트리밍** | 동영상 일시적 스트리밍 링크 |

---

## 6. Multipart Upload

**대용량 파일**(5GB 이상)을 여러 조각으로 나눠 업로드합니다.

### 6.1 이점

| 이점 | 설명 |
|------|------|
| **빠른 업로드** | 병렬 업로드로 속도 향상 |
| **재시도 효율** | 실패한 파트만 재업로드 |
| **대용량 지원** | 최대 5TB 파일 업로드 |
| **네트워크 안정성** | 일시적 오류 복구 가능 |

### 6.2 과정

```
1. Initiate Multipart Upload
   → Upload ID 발급

2. Upload Parts (병렬 가능)
   Part 1 (5MB)   ───┐
   Part 2 (5MB)   ───┤
   Part 3 (5MB)   ───┼→ S3
   ...            ───┤
   Part N (2MB)   ───┘

3. Complete Multipart Upload
   → 모든 파트 조합하여 최종 객체 생성
```

### 6.3 CLI 예시

```bash
# 1. Multipart Upload 시작
awslocal s3api create-multipart-upload \
    --bucket blog-bucket \
    --key large-video.mp4

# 출력: "UploadId": "abc123..."

# 2. 파일을 5MB 조각으로 분할
split -b 5242880 large-video.mp4 part_

# 3. 각 파트 업로드
awslocal s3api upload-part \
    --bucket blog-bucket \
    --key large-video.mp4 \
    --part-number 1 \
    --body part_aa \
    --upload-id abc123...

# 4. 업로드 완료
awslocal s3api complete-multipart-upload \
    --bucket blog-bucket \
    --key large-video.mp4 \
    --upload-id abc123... \
    --multipart-upload file://parts.json
```

### 6.4 SDK 예시 (Java)

AWS SDK v2는 **자동으로** Multipart Upload를 처리합니다.

```java
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileUpload;

public void uploadLargeFile(Path filePath, String key) {
    S3TransferManager transferManager = S3TransferManager.create();

    UploadFileRequest uploadRequest = UploadFileRequest.builder()
            .putObjectRequest(req -> req
                    .bucket(bucketName)
                    .key(key))
            .source(filePath)
            .build();

    FileUpload upload = transferManager.uploadFile(uploadRequest);

    // 진행률 추적 가능
    upload.progress().subscribe(progress -> {
        long percentage = (progress.transferredBytes() * 100) /
                          progress.totalBytes().orElse(1L);
        log.info("업로드 진행률: {}%", percentage);
    });

    // 완료 대기
    upload.completionFuture().join();
    log.info("업로드 완료");
}
```

---

## 7. Portal Universe 실습

### 7.1 LocalStack S3 조작

```bash
# 1. Docker Compose로 LocalStack 실행
cd /path/to/portal-universe
docker compose up -d localstack

# 2. Blog Service 버킷 생성
awslocal s3 mb s3://blog-bucket

# 3. 테스트 파일 업로드
echo "Hello S3" > test.txt
awslocal s3 cp test.txt s3://blog-bucket/

# 4. 확인
awslocal s3 ls s3://blog-bucket/

# 5. 다운로드
awslocal s3 cp s3://blog-bucket/test.txt ./downloaded.txt

# 6. Pre-signed URL 생성
awslocal s3 presign s3://blog-bucket/test.txt --expires-in 300

# 7. 브라우저에서 URL 열기 (파일 다운로드)
```

### 7.2 Blog Service 테스트

```bash
# Blog Service 실행
cd services/blog-service
./gradlew bootRun

# 파일 업로드 API 호출
curl -X POST http://localhost:8082/api/v1/files/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/image.jpg"

# 응답:
# {
#   "success": true,
#   "data": "http://localhost:4566/blog-bucket/uuid_image.jpg"
# }
```

---

## 8. 핵심 정리

| 명령어 | 용도 |
|--------|------|
| `s3 mb` | 버킷 생성 |
| `s3 cp` | 파일 복사 (업로드/다운로드) |
| `s3 sync` | 디렉토리 동기화 |
| `s3 rm` | 파일 삭제 |
| `s3 presign` | Pre-signed URL 생성 |
| `s3api` | 저수준 API 호출 (Multipart Upload 등) |

**Pre-signed URL:** 일시적 접근 권한 부여
**Multipart Upload:** 대용량 파일 분할 업로드 (5GB+ 권장)

---

## 다음 학습

- [S3 SDK 통합](./s3-sdk-integration.md)
- [S3 권한 설정](./s3-permissions.md)
- [S3 모범 사례](./s3-best-practices.md)

---

## 참고 자료

- [AWS CLI S3 명령어](https://docs.aws.amazon.com/cli/latest/reference/s3/)
- [S3 Presigned URL](https://docs.aws.amazon.com/AmazonS3/latest/userguide/PresignedUrlUploadObject.html)
- [Multipart Upload](https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html)
