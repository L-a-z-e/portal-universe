# S3 권한 설정

## 학습 목표
- S3 보안 모델 이해
- 버킷 정책과 IAM 정책 차이점 파악
- CORS 설정 방법 학습
- Pre-signed URL의 보안 메커니즘 이해

---

## 1. S3 보안 개요

### 1.1 보안 레이어

```
┌──────────────────────────────────────────────┐
│  1. 계정 레벨 (IAM 정책)                      │
│     - 누가 (IAM User/Role) S3를 사용할 수 있는가? │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│  2. 버킷 레벨 (버킷 정책)                     │
│     - 어떤 버킷에 누가 접근할 수 있는가?       │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│  3. 객체 레벨 (ACL - 권장하지 않음)           │
│     - 개별 파일별 권한 (레거시)               │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│  4. 네트워크 레벨 (Public Access Block)       │
│     - 퍼블릭 접근 차단                        │
└──────────────────────────────────────────────┘
```

### 1.2 기본 원칙

| 원칙 | 설명 |
|------|------|
| **기본 거부** | 명시적 허용이 없으면 모든 접근 거부 |
| **명시적 거부 우선** | Allow보다 Deny가 우선 |
| **최소 권한** | 필요한 최소한의 권한만 부여 |

---

## 2. 버킷 정책 (Bucket Policy)

### 2.1 개념

**리소스 기반 정책**으로, 버킷에 직접 연결됩니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::blog-bucket/*"
    }
  ]
}
```

**구성 요소:**
| 요소 | 설명 |
|------|------|
| **Effect** | Allow 또는 Deny |
| **Principal** | 누구에게 (AWS 계정, IAM 사용자, *) |
| **Action** | 무엇을 (s3:GetObject, s3:PutObject 등) |
| **Resource** | 어디에 (버킷 또는 객체 ARN) |
| **Condition** | 조건 (IP, 시간 등) |

### 2.2 퍼블릭 읽기 허용

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicRead",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::blog-bucket/*"
    }
  ]
}
```

**적용 방법:**
```bash
# 정책 파일 생성
cat > bucket-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::blog-bucket/*"
    }
  ]
}
EOF

# 정책 적용
aws s3api put-bucket-policy \
  --bucket blog-bucket \
  --policy file://bucket-policy.json
```

### 2.3 특정 IP만 허용

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::blog-bucket/*",
      "Condition": {
        "IpAddress": {
          "aws:SourceIp": [
            "203.0.113.0/24",
            "192.0.2.0/24"
          ]
        }
      }
    }
  ]
}
```

### 2.4 특정 AWS 계정만 허용

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::123456789012:user/blog-service"
      },
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::blog-bucket/*"
    }
  ]
}
```

---

## 3. IAM 정책

### 3.1 개념

**사용자/역할 기반 정책**으로, IAM 엔티티에 연결됩니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::blog-bucket/*"
    }
  ]
}
```

### 3.2 버킷 정책 vs IAM 정책

| 항목 | 버킷 정책 | IAM 정책 |
|------|----------|----------|
| **연결 대상** | S3 버킷 | IAM 사용자/역할 |
| **Principal** | 필수 (누구를) | 없음 (이미 사용자에 연결) |
| **교차 계정** | 가능 | 제한적 |
| **관리 주체** | 버킷 소유자 | IAM 관리자 |
| **용도** | 버킷 중심 접근 제어 | 사용자 권한 관리 |

### 3.3 Portal Universe 예시

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "BlogServiceS3Access",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::blog-bucket/*"
    },
    {
      "Sid": "BlogServiceBucketList",
      "Effect": "Allow",
      "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::blog-bucket"
    }
  ]
}
```

---

## 4. ACL (Access Control List)

### 4.1 개념

**객체/버킷별 권한 설정** (레거시, **권장하지 않음**)

```
버킷 ACL:
- READ: 버킷 내 객체 목록 조회
- WRITE: 버킷에 객체 생성/삭제
- READ_ACP: ACL 읽기
- WRITE_ACP: ACL 수정

객체 ACL:
- READ: 객체 읽기
- READ_ACP: ACL 읽기
- WRITE_ACP: ACL 수정
```

### 4.2 왜 권장하지 않는가?

| 이유 | 설명 |
|------|------|
| **관리 복잡도** | 각 객체마다 개별 설정 필요 |
| **일관성 부족** | 버킷 정책으로 통합 관리 불가 |
| **보안 위험** | 실수로 퍼블릭 노출 가능성 |
| **AWS 권장** | 버킷 정책 또는 IAM 정책 사용 권장 |

### 4.3 대안: 버킷 정책 사용

```json
// ❌ 각 객체에 ACL 설정 (비추천)
awslocal s3api put-object-acl \
  --bucket blog-bucket \
  --key uuid_file.jpg \
  --acl public-read

// ✅ 버킷 정책으로 통합 관리 (추천)
{
  "Effect": "Allow",
  "Principal": "*",
  "Action": "s3:GetObject",
  "Resource": "arn:aws:s3:::blog-bucket/public/*"
}
```

---

## 5. Public Access Block

### 5.1 개념

**실수로 퍼블릭 노출 방지**하는 안전장치입니다.

```
계정 레벨 → 버킷 레벨 → 버킷 정책
  (차단)      (차단)      (Allow)
    └──────────┴──────────┴─→ 최종 거부
```

### 5.2 4가지 설정

| 설정 | 설명 |
|------|------|
| **BlockPublicAcls** | 새 퍼블릭 ACL 차단 |
| **IgnorePublicAcls** | 기존 퍼블릭 ACL 무시 |
| **BlockPublicPolicy** | 새 퍼블릭 버킷 정책 차단 |
| **RestrictPublicBuckets** | 퍼블릭 버킷 정책 있어도 접근 차단 |

### 5.3 확인 및 설정

```bash
# 현재 설정 확인
awslocal s3api get-public-access-block --bucket blog-bucket

# 모두 차단 (프로덕션 권장)
awslocal s3api put-public-access-block \
  --bucket blog-bucket \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# 모두 허용 (개발 환경)
awslocal s3api put-public-access-block \
  --bucket blog-bucket \
  --public-access-block-configuration \
    "BlockPublicAcls=false,IgnorePublicAcls=false,BlockPublicPolicy=false,RestrictPublicBuckets=false"
```

---

## 6. CORS 설정

### 6.1 개념

**Cross-Origin Resource Sharing**: 다른 도메인에서 S3 리소스 접근 허용

```
브라우저 (https://portal-universe.com)
    │
    │ 이미지 요청: http://localhost:4566/blog-bucket/image.jpg
    ▼
S3 버킷 (http://localhost:4566)
    │
    │ CORS 정책 확인
    │ ✅ https://portal-universe.com 허용됨
    ▼
이미지 반환 + CORS 헤더
```

### 6.2 CORS 설정 예시

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
    "AllowedOrigins": [
      "http://localhost:30000",
      "http://localhost:30001",
      "http://localhost:30002",
      "https://portal-universe.com"
    ],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3600
  }
]
```

### 6.3 적용 방법

```bash
# CORS 설정 파일 생성
cat > cors-config.json << 'EOF'
{
  "CORSRules": [
    {
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST"],
      "AllowedOrigins": ["http://localhost:30000"],
      "MaxAgeSeconds": 3600
    }
  ]
}
EOF

# CORS 적용
awslocal s3api put-bucket-cors \
  --bucket blog-bucket \
  --cors-configuration file://cors-config.json

# CORS 확인
awslocal s3api get-bucket-cors --bucket blog-bucket
```

### 6.4 Portal Universe CORS 설정

```json
{
  "CORSRules": [
    {
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
      "AllowedOrigins": [
        "http://localhost:30000",  // portal-shell
        "http://localhost:30001",  // blog-frontend
        "http://localhost:30002"   // shopping-frontend
      ],
      "ExposeHeaders": ["ETag"],
      "MaxAgeSeconds": 3600
    }
  ]
}
```

---

## 7. Pre-signed URL 보안

### 7.1 메커니즘

```
1. 클라이언트가 서버에 다운로드 요청
   └─→ GET /api/files/download?fileId=123

2. 서버가 Pre-signed URL 생성
   - AWS 자격 증명으로 서명
   - 만료 시간 포함 (1시간)
   └─→ URL 반환

3. 클라이언트가 Pre-signed URL로 S3 직접 접근
   - S3가 서명 검증
   - 만료 시간 확인
   └─→ 파일 다운로드
```

### 7.2 보안 장점

| 장점 | 설명 |
|------|------|
| **서버 부하 감소** | 파일 전송이 S3 직접 처리 |
| **일시적 권한** | 만료 시간 후 자동 무효화 |
| **버킷 비공개** | 버킷 자체는 비공개 유지 |
| **추적 가능** | CloudTrail로 접근 로그 기록 |

### 7.3 구현 예시

```java
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.time.Duration;

public String generateDownloadUrl(String key) {
    try (S3Presigner presigner = S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2)
            .build()) {

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))  // 1시간 유효
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presignedRequest =
                presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }
}
```

### 7.4 업로드용 Pre-signed URL

```java
public String generateUploadUrl(String key) {
    try (S3Presigner presigner = S3Presigner.builder()
            .region(Region.AP_NORTHEAST_2)
            .build()) {

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("image/jpeg")
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))  // 15분 유효
                .putObjectRequest(putRequest)
                .build();

        PresignedPutObjectRequest presignedRequest =
                presigner.presignPutObject(presignRequest);

        return presignedRequest.url().toString();
    }
}
```

---

## 8. 실습: Portal Universe S3 보안 설정

### 8.1 LocalStack 환경 (개발)

```bash
# 1. Public Access Block 비활성화 (개발 편의)
awslocal s3api put-public-access-block \
  --bucket blog-bucket \
  --public-access-block-configuration \
    "BlockPublicAcls=false,IgnorePublicAcls=false,BlockPublicPolicy=false,RestrictPublicBuckets=false"

# 2. CORS 설정
cat > cors.json << 'EOF'
{
  "CORSRules": [
    {
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST"],
      "AllowedOrigins": ["http://localhost:30000", "http://localhost:30001"],
      "MaxAgeSeconds": 3600
    }
  ]
}
EOF

awslocal s3api put-bucket-cors \
  --bucket blog-bucket \
  --cors-configuration file://cors.json

# 3. 확인
awslocal s3api get-bucket-cors --bucket blog-bucket
```

### 8.2 프로덕션 환경 (AWS)

```bash
# 1. Public Access Block 활성화 (필수)
aws s3api put-public-access-block \
  --bucket prod-blog-bucket \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# 2. IAM 정책으로만 접근 허용
# (버킷 정책 없음 - IAM Role만 사용)

# 3. CORS 설정
cat > cors-prod.json << 'EOF'
{
  "CORSRules": [
    {
      "AllowedHeaders": ["Authorization", "Content-Type"],
      "AllowedMethods": ["GET"],
      "AllowedOrigins": ["https://portal-universe.com"],
      "MaxAgeSeconds": 3600
    }
  ]
}
EOF

aws s3api put-bucket-cors \
  --bucket prod-blog-bucket \
  --cors-configuration file://cors-prod.json
```

---

## 9. 핵심 정리

| 개념 | 설명 |
|------|------|
| **버킷 정책** | 리소스 기반, 교차 계정 접근 제어 |
| **IAM 정책** | 사용자 기반, 권한 관리 |
| **ACL** | 레거시, 사용 권장하지 않음 |
| **Public Access Block** | 실수로 퍼블릭 노출 방지 |
| **CORS** | 브라우저에서 교차 도메인 접근 허용 |
| **Pre-signed URL** | 일시적 접근 권한 부여 |

---

## 다음 학습

- [S3 모범 사례](./s3-best-practices.md)
- [S3 소개](./s3-introduction.md)

---

## 참고 자료

- [S3 버킷 정책](https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucket-policies.html)
- [S3 CORS](https://docs.aws.amazon.com/AmazonS3/latest/userguide/cors.html)
- [S3 Pre-signed URL](https://docs.aws.amazon.com/AmazonS3/latest/userguide/PresignedUrlUploadObject.html)
