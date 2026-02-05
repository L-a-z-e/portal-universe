# S3 모범 사례

## 학습 목표
- S3 키 네이밍 전략 이해
- 스토리지 클래스 선택 기준 학습
- 라이프사이클 규칙 설정 방법 파악
- 비용 최적화 전략 습득

---

## 1. 키 네이밍 전략

### 1.1 왜 중요한가?

S3는 **키 이름의 첫 몇 글자**로 파티셔닝합니다.

```
❌ 나쁜 예 (Hot Partition):
/2024/01/22/file001.jpg
/2024/01/22/file002.jpg
/2024/01/22/file003.jpg
...
→ 같은 파티션에 집중, 성능 저하

✅ 좋은 예 (분산):
550e8400_2024-01-22_file001.jpg
7d8a3b20_2024-01-22_file002.jpg
9f3c5e80_2024-01-22_file003.jpg
→ 다른 파티션에 분산, 성능 향상
```

### 1.2 UUID Prefix 패턴 (Portal Universe)

```java
// 추천: UUID prefix로 자동 분산
private String generateKey(String originalFilename) {
    return UUID.randomUUID() + "_" + originalFilename;
}

// 생성 예시:
// 550e8400-e29b-41d4-a716-446655440000_profile.jpg
// 7d8a3b20-5f1e-4c2d-9a3b-123456789abc_banner.png
```

**이점:**
- 자동으로 균등 분산
- 파일명 중복 방지
- 높은 처리량 (초당 3,500 PUT/5,500 GET)

### 1.3 날짜 기반 네이밍 (주의)

```
❌ 나쁜 예 (시간순 집중):
/uploads/2024/01/22/10/file001.jpg
/uploads/2024/01/22/10/file002.jpg

✅ 개선 (해시 prefix):
/uploads/abc_2024/01/22/file001.jpg
/uploads/def_2024/01/22/file002.jpg

✅ 더 나은 방법 (UUID first):
abc123_uploads_2024-01-22_file001.jpg
def456_uploads_2024-01-22_file002.jpg
```

### 1.4 논리적 그룹핑

```
사용자별 파일:
uuid_user-{userId}_filename.jpg
└─ 550e8400_user-123_profile.jpg

서비스별 파일:
uuid_{service}_filename.jpg
└─ 7d8a3b20_blog_banner.png
└─ 9f3c5e80_shopping_product.jpg

날짜별 파일 (분석용):
uuid_{service}_{date}_filename.jpg
└─ abc123_blog_2024-01-22_post.jpg
```

---

## 2. 스토리지 클래스 선택

### 2.1 의사결정 트리

```
파일을 얼마나 자주 접근하나?

├─ 자주 접근 (일 1회+)
│  └─ Standard ───────────────────┐
│                                 │
├─ 가끔 접근 (월 1회 미만)         │
│  ├─ 즉시 필요?                   │
│  │  ├─ Yes → Standard-IA ───────┤
│  │  └─ No → Glacier Instant ────┤
│  │                               │
│  └─ 재생성 가능?                  │
│     └─ Yes → One Zone-IA ────────┤
│                                  │
└─ 거의 접근 안 함 (연 1회 미만)    │
   ├─ 몇 시간 대기 가능?            │
   │  └─ Yes → Glacier Flexible ───┤
   │                                │
   └─ 장기 보관 (7~10년)            │
      └─ Glacier Deep Archive ──────┤
                                    │
                                    ▼
                              선택 완료
```

### 2.2 사용 사례별 권장

| 사용 사례 | 스토리지 클래스 | 이유 |
|-----------|----------------|------|
| **웹사이트 이미지** | Standard | 자주 접근, 빠른 응답 필요 |
| **동영상 스트리밍** | Standard | 높은 처리량 필요 |
| **로그 파일** | Standard-IA | 월 1회 정도 조회 |
| **백업 데이터** | Glacier Flexible | 재해 시에만 복구 |
| **규정 준수 데이터** | Glacier Deep Archive | 법적 보관 의무 |
| **썸네일 (재생성 가능)** | One Zone-IA | 원본에서 재생성 가능 |

### 2.3 Portal Universe 전략

```yaml
Blog Service 파일 전략:

1. 게시글 첨부 이미지
   - Standard (자주 조회)
   - TTL: 없음 (영구 보관)

2. 임시 업로드 파일
   - Standard (즉시 접근)
   - TTL: 24시간 (라이프사이클)

3. 게시글 백업
   - Glacier Flexible
   - 재해 복구용
```

---

## 3. 라이프사이클 규칙

### 3.1 개념

**자동으로** 객체의 스토리지 클래스를 변경하거나 삭제합니다.

```
객체 생성
    │
    │ 30일 후
    ▼
Standard → Standard-IA
    │
    │ 90일 후
    ▼
Standard-IA → Glacier Flexible
    │
    │ 365일 후
    ▼
삭제
```

### 3.2 전환 규칙

```json
{
  "Rules": [
    {
      "Id": "MoveToIA",
      "Status": "Enabled",
      "Transitions": [
        {
          "Days": 30,
          "StorageClass": "STANDARD_IA"
        },
        {
          "Days": 90,
          "StorageClass": "GLACIER"
        }
      ],
      "Expiration": {
        "Days": 365
      }
    }
  ]
}
```

### 3.3 임시 파일 자동 삭제

```json
{
  "Rules": [
    {
      "Id": "DeleteTempFiles",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "temp/"
      },
      "Expiration": {
        "Days": 1
      }
    }
  ]
}
```

**적용:**
```bash
awslocal s3api put-bucket-lifecycle-configuration \
  --bucket blog-bucket \
  --lifecycle-configuration file://lifecycle.json
```

### 3.4 Portal Universe 예시

```json
{
  "Rules": [
    {
      "Id": "TemporaryUploads",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "temp/"
      },
      "Expiration": {
        "Days": 1
      }
    },
    {
      "Id": "ArchiveOldBackups",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "backup/"
      },
      "Transitions": [
        {
          "Days": 90,
          "StorageClass": "GLACIER"
        }
      ]
    },
    {
      "Id": "DeleteIncompleteUploads",
      "Status": "Enabled",
      "AbortIncompleteMultipartUpload": {
        "DaysAfterInitiation": 7
      }
    }
  ]
}
```

---

## 4. 버전 관리 활용

### 4.1 언제 활성화해야 하나?

| 활성화 권장 | 비활성화 가능 |
|------------|--------------|
| 중요한 문서 | 로그 파일 |
| 사용자 업로드 파일 | 일회성 파일 |
| 규정 준수 필요 | 재생성 가능한 파일 |
| 실수로 삭제 위험 | 임시 파일 |

### 4.2 라이프사이클과 결합

```json
{
  "Rules": [
    {
      "Id": "ArchiveOldVersions",
      "Status": "Enabled",
      "NoncurrentVersionTransitions": [
        {
          "NoncurrentDays": 30,
          "StorageClass": "STANDARD_IA"
        },
        {
          "NoncurrentDays": 90,
          "StorageClass": "GLACIER"
        }
      ],
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 365
      }
    }
  ]
}
```

**효과:**
- 최신 버전: Standard
- 30일 지난 이전 버전: Standard-IA
- 90일 지난 이전 버전: Glacier
- 365일 지난 이전 버전: 삭제

---

## 5. 비용 최적화

### 5.1 비용 구조

| 항목 | 비용 | 최적화 방법 |
|------|------|-----------|
| **스토리지** | GB/월 | 스토리지 클래스 최적화 |
| **요청** | 요청당 | 캐싱, 배치 처리 |
| **데이터 전송** | GB | CloudFront CDN 사용 |
| **버전 관리** | 모든 버전 저장 | 라이프사이클로 정리 |

### 5.2 최적화 전략

#### 1) Intelligent-Tiering 사용

```bash
# 접근 패턴이 불명확한 경우
awslocal s3api put-object \
  --bucket blog-bucket \
  --key file.jpg \
  --storage-class INTELLIGENT_TIERING
```

**자동으로:**
- 30일 미접근 → IA 티어로 이동
- 90일 미접근 → Archive 티어로 이동
- 접근 시 → 자동으로 Standard로 복귀

#### 2) CloudFront 사용

```
Before (S3 Direct):
사용자 → S3 (요청당 과금)
      → 데이터 전송 (GB당 과금)

After (CloudFront):
사용자 → CloudFront (캐싱)
      → S3 (요청 감소 90%)
```

#### 3) 불필요한 버전 삭제

```bash
# 이전 버전들 삭제
awslocal s3api list-object-versions --bucket blog-bucket

# 특정 버전 삭제
awslocal s3api delete-object \
  --bucket blog-bucket \
  --key file.jpg \
  --version-id abc123...
```

#### 4) 미완성 Multipart Upload 정리

```json
// 7일 후 자동 삭제
{
  "Rules": [
    {
      "Id": "CleanupMultipart",
      "Status": "Enabled",
      "AbortIncompleteMultipartUpload": {
        "DaysAfterInitiation": 7
      }
    }
  ]
}
```

### 5.3 비용 모니터링

```bash
# S3 Storage Lens 활성화 (AWS 콘솔)
# 또는

# 버킷 사이즈 확인
awslocal s3api list-objects-v2 \
  --bucket blog-bucket \
  --query 'sum(Contents[].Size)' \
  --output text | awk '{print $1/1024/1024/1024 " GB"}'

# 객체 수 확인
awslocal s3api list-objects-v2 \
  --bucket blog-bucket \
  --query 'length(Contents)'
```

---

## 6. 보안 모범 사례

### 6.1 체크리스트

```
✅ Public Access Block 활성화
✅ 버킷 정책으로 최소 권한 부여
✅ IAM Role 사용 (Access Key 비활성화)
✅ S3 접근 로그 활성화
✅ 암호화 활성화 (SSE-S3 또는 SSE-KMS)
✅ MFA Delete 활성화 (중요 버킷)
✅ 버전 관리 활성화 (실수 삭제 방지)
❌ ACL 사용하지 않기
```

### 6.2 암호화 설정

```bash
# 서버 사이드 암호화 (AES-256)
awslocal s3api put-bucket-encryption \
  --bucket blog-bucket \
  --server-side-encryption-configuration '{
    "Rules": [
      {
        "ApplyServerSideEncryptionByDefault": {
          "SSEAlgorithm": "AES256"
        },
        "BucketKeyEnabled": true
      }
    ]
  }'
```

### 6.3 접근 로그 활성화

```bash
# 로그 버킷 생성
awslocal s3 mb s3://blog-bucket-logs

# 로깅 활성화
awslocal s3api put-bucket-logging \
  --bucket blog-bucket \
  --bucket-logging-status '{
    "LoggingEnabled": {
      "TargetBucket": "blog-bucket-logs",
      "TargetPrefix": "access-logs/"
    }
  }'
```

---

## 7. 성능 최적화

### 7.1 병렬 업로드

```java
// TransferManager 사용 (자동 병렬 처리)
import software.amazon.awssdk.transfer.s3.S3TransferManager;

S3TransferManager transferManager = S3TransferManager.create();

// 자동으로 Multipart Upload + 병렬 처리
FileUpload upload = transferManager.uploadFile(req -> req
        .putObjectRequest(por -> por.bucket(bucketName).key(key))
        .source(Paths.get(filePath))
);

upload.completionFuture().join();
```

### 7.2 Range GET

```java
// 큰 파일의 일부만 다운로드
GetObjectRequest getRequest = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(key)
        .range("bytes=0-1023")  // 첫 1KB만
        .build();

ResponseBytes<GetObjectResponse> objectBytes =
        s3Client.getObjectAsBytes(getRequest);
```

### 7.3 CloudFront 활용

```yaml
# CloudFront 배포 생성
Distribution:
  Origin: blog-bucket.s3.ap-northeast-2.amazonaws.com
  CacheBehavior:
    MinTTL: 86400  # 24시간
    MaxTTL: 31536000  # 1년
  ViewerProtocolPolicy: redirect-to-https
```

---

## 8. Portal Universe 종합 전략

### 8.1 Blog Service 파일 관리

```
┌─────────────────────────────────────────────┐
│  blog-bucket                                 │
├─────────────────────────────────────────────┤
│                                              │
│  [Standard - 일반 게시글 첨부]               │
│  ├─ uuid_image1.jpg                          │
│  ├─ uuid_document.pdf                        │
│  └─ uuid_video.mp4                           │
│                                              │
│  [Intelligent-Tiering - 접근 패턴 불명확]    │
│  └─ user-uploads/                            │
│     ├─ uuid_file1.jpg                        │
│     └─ uuid_file2.png                        │
│                                              │
│  [Standard + 24시간 TTL - 임시 파일]         │
│  └─ temp/                                    │
│     ├─ uuid_temp1.jpg                        │
│     └─ uuid_temp2.png                        │
│                                              │
│  [Glacier - 백업]                            │
│  └─ backup/                                  │
│     ├─ 2024-01-22-backup.tar.gz              │
│     └─ 2024-01-21-backup.tar.gz              │
└─────────────────────────────────────────────┘
```

### 8.2 라이프사이클 정책

```json
{
  "Rules": [
    {
      "Id": "TempFileCleanup",
      "Filter": {"Prefix": "temp/"},
      "Status": "Enabled",
      "Expiration": {"Days": 1}
    },
    {
      "Id": "BackupArchival",
      "Filter": {"Prefix": "backup/"},
      "Status": "Enabled",
      "Transitions": [
        {"Days": 0, "StorageClass": "GLACIER"}
      ]
    }
  ]
}
```

---

## 9. 핵심 정리

| 항목 | 권장 사항 |
|------|----------|
| **키 네이밍** | UUID prefix로 균등 분산 |
| **스토리지 클래스** | 접근 빈도에 따라 선택 |
| **라이프사이클** | 자동 전환 및 삭제 설정 |
| **버전 관리** | 중요 파일만 활성화 |
| **암호화** | 서버 사이드 암호화 필수 |
| **접근 제어** | IAM Role + 버킷 정책 |
| **비용 절감** | Intelligent-Tiering + CloudFront |

---

## 다음 학습

- [S3 소개](./s3-introduction.md)
- [S3 CRUD 연산](./s3-operations.md)
- [S3 SDK 통합](./s3-sdk-integration.md)
- [S3 권한 설정](./s3-permissions.md)

---

## 참고 자료

- [S3 Performance Guidelines](https://docs.aws.amazon.com/AmazonS3/latest/userguide/optimizing-performance.html)
- [S3 Lifecycle](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lifecycle-mgmt.html)
- [S3 Cost Optimization](https://aws.amazon.com/s3/cost-optimization/)
- [S3 Best Practices](https://docs.aws.amazon.com/AmazonS3/latest/userguide/best-practices.html)
