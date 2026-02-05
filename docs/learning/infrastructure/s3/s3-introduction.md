# Amazon S3 소개

## 학습 목표
- S3의 핵심 개념과 구조 이해
- 객체 스토리지와 다른 스토리지의 차이점 파악
- S3 버킷과 객체의 네이밍 규칙 학습
- 스토리지 클래스와 버전 관리 이해

---

## 1. S3란?

Amazon S3(Simple Storage Service)는 **객체 스토리지 서비스**입니다. 파일을 객체(Object) 단위로 저장하며, 높은 내구성(99.999999999%)과 가용성을 제공합니다.

### 핵심 특징

| 특성 | 설명 |
|------|------|
| **무제한 용량** | 개별 객체는 최대 5TB, 버킷 용량은 무제한 |
| **내구성** | 11 nines (99.999999999%) 내구성 보장 |
| **가용성** | 99.99% 가용성 (Standard 클래스) |
| **확장성** | 자동 확장, 용량 관리 불필요 |
| **비용 효율** | 사용한 만큼만 지불 |

### S3 vs 다른 스토리지

```
[블록 스토리지 (EBS)]
- OS 레벨 파일 시스템
- 단일 인스턴스 연결
- 낮은 지연시간 (ms 단위)
- 용도: 데이터베이스, 부트 볼륨

[파일 스토리지 (EFS)]
- 네트워크 파일 시스템
- 여러 인스턴스 동시 연결
- POSIX 호환
- 용도: 공유 파일 시스템

[객체 스토리지 (S3)]
- HTTP API 기반 접근
- 무제한 동시 접근
- 메타데이터 지원
- 용도: 정적 파일, 백업, 데이터 레이크
```

---

## 2. 핵심 개념

### 2.1 버킷 (Bucket)

S3의 **최상위 컨테이너**입니다. 모든 객체는 버킷에 저장됩니다.

```
┌──────────────────────────────────────┐
│  Bucket: blog-bucket                  │
├──────────────────────────────────────┤
│  ├─ images/                           │
│  │  ├─ uuid_profile.jpg              │
│  │  └─ uuid_banner.png               │
│  ├─ documents/                        │
│  │  └─ uuid_report.pdf               │
│  └─ videos/                           │
│     └─ uuid_intro.mp4                │
└──────────────────────────────────────┘
```

**버킷 네이밍 규칙:**
- 3~63자 길이
- 소문자, 숫자, 하이픈(-), 점(.)만 사용
- 글로벌 고유성 (전 세계에서 유일한 이름)
- DNS 호환 이름 (점으로 시작/끝나지 않음)

```
✅ 올바른 예시:
- my-blog-images-2024
- portal-universe-files
- company.backup.bucket

❌ 잘못된 예시:
- MyBucket (대문자 불가)
- bucket_name (언더스코어 불가)
- -start-hyphen (하이픈으로 시작 불가)
- 192.168.0.1 (IP 주소 형식 불가)
```

**Portal Universe 버킷:**
```yaml
# blog-service application.yml
aws:
  s3:
    bucket-name: blog-bucket  # Blog 첨부파일용
```

### 2.2 객체 (Object)

S3에 저장되는 **개별 파일**입니다.

**구성 요소:**
| 요소 | 설명 |
|------|------|
| **Key** | 객체의 고유 식별자 (파일 경로) |
| **Value** | 실제 파일 데이터 (최대 5TB) |
| **Metadata** | 객체 속성 (Content-Type, 사용자 정의 등) |
| **Version ID** | 버전 관리 활성화 시 버전 ID |

```
객체 예시:
┌────────────────────────────────────────────┐
│ Key: images/uuid_profile.jpg               │
│ Value: [바이너리 데이터]                    │
│ Size: 2.5 MB                               │
│ Metadata:                                  │
│   - Content-Type: image/jpeg               │
│   - x-amz-meta-uploader: user123           │
│   - x-amz-meta-upload-date: 2024-01-22     │
│ Version ID: abc123def456 (버전 관리 시)    │
└────────────────────────────────────────────┘
```

### 2.3 키 (Key)

객체의 **고유 식별자**로, 파일 경로처럼 보이지만 실제로는 단순 문자열입니다.

```
키 예시:
- images/profile/uuid_user123.jpg
- documents/2024/01/report.pdf
- videos/intro.mp4

⚠️ 주의: S3에는 실제 디렉토리가 없습니다!
슬래시(/)를 포함한 문자열일 뿐입니다.
```

**Portal Universe 키 생성 패턴:**
```java
// FileService.java - UUID prefix로 중복 방지
private String generateUniqueKey(String originalFilename) {
    return UUID.randomUUID() + "_" + originalFilename;
}

// 생성 예시:
// "550e8400-e29b-41d4-a716-446655440000_myimage.jpg"
```

---

## 3. 스토리지 클래스

객체의 **접근 빈도와 가용성**에 따라 다양한 클래스를 제공합니다.

| 클래스 | 용도 | 지연시간 | 가용성 | 비용 |
|--------|------|----------|--------|------|
| **Standard** | 자주 접근하는 데이터 | ms | 99.99% | 높음 |
| **Intelligent-Tiering** | 접근 패턴 불명확 | ms | 99.9% | 자동 최적화 |
| **Standard-IA** | 가끔 접근 (월 1회 미만) | ms | 99.9% | 중간 |
| **One Zone-IA** | 재생성 가능한 데이터 | ms | 99.5% | 낮음 |
| **Glacier Instant** | 즉시 검색 필요한 보관 | ms | 99.9% | 매우 낮음 |
| **Glacier Flexible** | 몇 분~몇 시간 대기 가능 | 분~시간 | 99.99% | 극히 낮음 |
| **Glacier Deep Archive** | 장기 보관 (연 1회 미만) | 12시간+ | 99.99% | 최저 |

```
사용 사례:

Standard:
- 웹사이트 이미지
- 동영상 스트리밍
- 자주 다운로드하는 파일

Standard-IA:
- 백업 데이터
- 재해 복구 파일
- 로그 아카이브

Glacier:
- 규정 준수용 장기 보관
- 의료 기록
- 법적 문서
```

---

## 4. 버전 관리 (Versioning)

객체의 **여러 버전을 보존**하는 기능입니다.

### 버전 관리 비활성화

```
PUT myfile.txt → myfile.txt (버전 없음)
PUT myfile.txt → myfile.txt (이전 버전 덮어씀)
DELETE myfile.txt → 영구 삭제
```

### 버전 관리 활성화

```
PUT myfile.txt → myfile.txt (버전 ID: v1)
PUT myfile.txt → myfile.txt (버전 ID: v2)  ← 최신 버전
                 myfile.txt (버전 ID: v1)  ← 이전 버전 보존

DELETE myfile.txt → 삭제 마커 추가 (실제로는 숨김)
                    복구 가능!
```

**장점:**
- 실수로 덮어쓴 파일 복구
- 실수로 삭제한 파일 복구
- 변경 이력 추적

**단점:**
- 스토리지 비용 증가 (모든 버전 저장)
- 관리 복잡도 증가

---

## 5. Portal Universe에서의 S3

### 5.1 Blog Service 파일 관리

```
┌──────────────────────────────────────────────┐
│            Blog Service                       │
│  ┌────────────────────────────────────────┐  │
│  │ FileService                            │  │
│  │ - uploadFile()                         │  │
│  │ - deleteFile()                         │  │
│  └────────────┬───────────────────────────┘  │
│               │                               │
│               │ AWS SDK v2                    │
│               ▼                               │
│  ┌────────────────────────────────────────┐  │
│  │ S3Config                               │  │
│  │ - S3Client Bean                        │  │
│  │ - LocalStack endpoint 설정             │  │
│  └────────────┬───────────────────────────┘  │
└───────────────┼───────────────────────────────┘
                │
                ▼
┌──────────────────────────────────────────────┐
│        S3 (LocalStack in Docker)             │
│  ┌────────────────────────────────────────┐  │
│  │ Bucket: blog-bucket                    │  │
│  │  ├─ uuid_image1.jpg                    │  │
│  │  ├─ uuid_image2.png                    │  │
│  │  └─ uuid_document.pdf                  │  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

### 5.2 로컬 개발 환경

Portal Universe는 **LocalStack**을 사용하여 로컬에서 S3를 시뮬레이션합니다.

```yaml
# docker-compose.yml
localstack:
  image: localstack/localstack:latest
  ports:
    - "4566:4566"  # S3 엔드포인트
  environment:
    - SERVICES=s3
    - DEFAULT_REGION=ap-northeast-2

# blog-service application.yml
aws:
  s3:
    endpoint: http://localhost:4566  # LocalStack 엔드포인트
    region: ap-northeast-2
    access-key: test
    secret-key: test
    bucket-name: blog-bucket
```

**자동 버킷 생성:**
```java
@PostConstruct
public void ensureBucketExists() {
    try {
        s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(bucketName)
                .build());
        log.info("✅ S3 버킷 존재 확인: {}", bucketName);
    } catch (NoSuchBucketException e) {
        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(bucketName)
                .build());
        log.info("✅ S3 버킷 생성 완료: {}", bucketName);
    }
}
```

---

## 6. S3 URL 형식

### Path-Style URL (LocalStack)

```
http://localhost:4566/blog-bucket/uuid_file.jpg
       └─endpoint─┘ └─bucket─┘ └────key────┘
```

### Virtual-Hosted-Style URL (AWS)

```
https://blog-bucket.s3.ap-northeast-2.amazonaws.com/uuid_file.jpg
       └─bucket─┘ └────region────┘           └────key────┘
```

Portal Universe는 **Path-Style** 사용:
```java
// S3Config.java
S3Configuration.builder()
    .pathStyleAccessEnabled(true)  // LocalStack 필수
    .build()
```

---

## 7. 핵심 정리

| 개념 | 설명 |
|------|------|
| **버킷** | 객체를 저장하는 최상위 컨테이너 (글로벌 고유 이름) |
| **객체** | S3에 저장되는 개별 파일 (최대 5TB) |
| **키** | 객체의 고유 식별자 (파일 경로처럼 보이는 문자열) |
| **스토리지 클래스** | 접근 빈도에 따른 비용 최적화 옵션 |
| **버전 관리** | 객체의 여러 버전 보존 (복구 가능) |

---

## 다음 학습

- [S3 CRUD 연산](./s3-operations.md)
- [Spring Boot S3 SDK 통합](./s3-sdk-integration.md)
- [S3 권한 설정](./s3-permissions.md)
- [S3 모범 사례](./s3-best-practices.md)

---

## 참고 자료

- [AWS S3 공식 문서](https://docs.aws.amazon.com/s3/)
- [S3 버킷 네이밍 규칙](https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html)
- [LocalStack S3](https://docs.localstack.cloud/user-guide/aws/s3/)
