# IAM 정책 (IAM Policies)

## 학습 목표
- IAM Policy 문서 구조와 작성 방법 이해
- Effect, Action, Resource, Condition 요소 활용
- AWS 관리형 정책과 고객 관리형 정책 구분
- 최소 권한 원칙(Principle of Least Privilege) 적용

---

## 1. IAM Policy란?

IAM Policy는 **권한을 정의하는 JSON 문서**입니다. "누가", "어떤 리소스에", "어떤 작업을", "어떤 조건에서" 할 수 있는지 명시합니다.

### Policy 기본 구조

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::my-bucket/*"
    }
  ]
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| **Version** | ✓ | Policy 언어 버전 (항상 `2012-10-17`) |
| **Statement** | ✓ | 권한 규칙 배열 |
| **Effect** | ✓ | `Allow` 또는 `Deny` |
| **Action** | ✓ | 허용/거부할 작업 |
| **Resource** | ✓ | 대상 리소스 ARN |
| **Condition** | ✗ | 조건부 적용 규칙 |

---

## 2. Statement 요소

### 2.1 Effect

권한을 **허용(Allow)** 또는 **거부(Deny)** 합니다.

```json
{
  "Effect": "Allow"  // 또는 "Deny"
}
```

**평가 우선순위:**
```
1. 명시적 Deny (최우선)
2. 명시적 Allow
3. 암묵적 Deny (기본값)
```

**예시:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "s3:*",
      "Resource": "*"
    },
    {
      "Effect": "Deny",
      "Action": "s3:DeleteBucket",
      "Resource": "*"
    }
  ]
}
```

결과: S3 모든 작업 가능하지만 **버킷 삭제만 불가** (Deny 우선)

### 2.2 Action

허용/거부할 **API 작업**을 지정합니다.

```json
{
  "Action": "s3:GetObject"           // 단일 작업
}

{
  "Action": [
    "s3:GetObject",
    "s3:PutObject"                    // 여러 작업
  ]
}

{
  "Action": "s3:*"                    // 와일드카드 (모든 S3 작업)
}
```

**서비스별 Action 형식:**
- S3: `s3:GetObject`, `s3:PutObject`, `s3:ListBucket`
- EC2: `ec2:DescribeInstances`, `ec2:RunInstances`
- DynamoDB: `dynamodb:GetItem`, `dynamodb:PutItem`

**Portal Universe 예시:**
```json
{
  "Action": [
    "s3:PutObject",      // 상품 이미지 업로드
    "s3:GetObject"       // 상품 이미지 조회
  ]
}
```

### 2.3 Resource

권한이 적용되는 **AWS 리소스**를 ARN으로 지정합니다.

**ARN (Amazon Resource Name) 형식:**
```
arn:partition:service:region:account-id:resource-type/resource-id
```

**예시:**
```json
{
  "Resource": "arn:aws:s3:::my-bucket/*"
}

{
  "Resource": [
    "arn:aws:s3:::product-images/*",
    "arn:aws:s3:::blog-attachments/*"
  ]
}

{
  "Resource": "*"  // 모든 리소스 (주의!)
}
```

**S3 ARN 패턴:**
```json
// 버킷 자체
"arn:aws:s3:::my-bucket"

// 버킷 내 모든 객체
"arn:aws:s3:::my-bucket/*"

// 특정 폴더
"arn:aws:s3:::my-bucket/images/*"

// 와일드카드
"arn:aws:s3:::product-*"
```

### 2.4 Condition (선택)

**조건부로** 권한을 적용합니다.

```json
{
  "Effect": "Allow",
  "Action": "s3:GetObject",
  "Resource": "arn:aws:s3:::my-bucket/*",
  "Condition": {
    "IpAddress": {
      "aws:SourceIp": "203.0.113.0/24"
    }
  }
}
```

**자주 사용하는 Condition:**
| 조건 | 설명 | 예시 |
|------|------|------|
| **IpAddress** | 특정 IP에서만 접근 | 사무실 IP 대역 |
| **DateGreaterThan** | 특정 날짜 이후 | 임시 권한 부여 |
| **StringLike** | 문자열 패턴 매칭 | 특정 태그가 있는 리소스 |
| **Bool** | Boolean 값 확인 | MFA 활성화 여부 |

---

## 3. Policy 종류

### 3.1 AWS 관리형 정책

AWS가 미리 만든 **재사용 가능한 정책**입니다.

```
┌────────────────────────────────────┐
│  AWS 관리형 정책 예시              │
├────────────────────────────────────┤
│  - AmazonS3FullAccess              │
│  - AmazonS3ReadOnlyAccess          │
│  - AmazonEC2ReadOnlyAccess         │
│  - PowerUserAccess                 │
│  - ReadOnlyAccess                  │
└────────────────────────────────────┘
```

**장점:**
- 바로 사용 가능
- AWS가 업데이트 관리
- 일반적 사용 사례 커버

**단점:**
- 세밀한 제어 불가
- 과도한 권한 부여 가능

### 3.2 고객 관리형 정책

사용자가 직접 만든 **커스텀 정책**입니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::portal-universe-products/*"
    }
  ]
}
```

**장점:**
- 정확히 필요한 권한만 부여
- 최소 권한 원칙 구현
- 조직 요구사항 반영

**단점:**
- 작성/관리 필요
- 잘못된 정책 시 보안 위험

### 3.3 인라인 정책

특정 User/Role에 **직접 삽입**하는 정책입니다.

```
User: jane-developer
└─ 인라인 정책 (삭제 시 함께 삭제됨)
```

**사용 시기:**
- 1:1 전용 권한
- 정책 재사용 불필요
- User/Role 삭제 시 정책도 삭제되어야 함

---

## 4. Policy 예시

### 4.1 S3 읽기 전용

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::my-bucket",
        "arn:aws:s3:::my-bucket/*"
      ]
    }
  ]
}
```

### 4.2 S3 전체 접근

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "s3:*",
      "Resource": [
        "arn:aws:s3:::my-bucket",
        "arn:aws:s3:::my-bucket/*"
      ]
    }
  ]
}
```

### 4.3 Portal Universe - Shopping Service

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ProductImageUpload",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:PutObjectAcl"
      ],
      "Resource": "arn:aws:s3:::portal-universe-products/images/*"
    },
    {
      "Sid": "ProductImageRead",
      "Effect": "Allow",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::portal-universe-products/images/*"
    },
    {
      "Sid": "ListBuckets",
      "Effect": "Allow",
      "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::portal-universe-products"
    }
  ]
}
```

### 4.4 조건부 접근 (IP 제한)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "s3:*",
      "Resource": "*",
      "Condition": {
        "IpAddress": {
          "aws:SourceIp": [
            "203.0.113.0/24",
            "198.51.100.0/24"
          ]
        }
      }
    }
  ]
}
```

### 4.5 MFA 필수

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "s3:DeleteBucket",
      "Resource": "*",
      "Condition": {
        "Bool": {
          "aws:MultiFactorAuthPresent": "true"
        }
      }
    }
  ]
}
```

---

## 5. 최소 권한 원칙

### 5.1 Principle of Least Privilege

**필요한 최소한의 권한만** 부여하는 보안 원칙입니다.

```
❌ 나쁜 예:
{
  "Effect": "Allow",
  "Action": "*",          // 모든 작업
  "Resource": "*"         // 모든 리소스
}

✅ 좋은 예:
{
  "Effect": "Allow",
  "Action": [
    "s3:GetObject",
    "s3:PutObject"
  ],
  "Resource": "arn:aws:s3:::my-app-bucket/uploads/*"
}
```

### 5.2 단계적 권한 부여

```
1단계: 최소 권한으로 시작
   └─ 예: S3 읽기만

2단계: 필요 시 추가
   └─ 예: 쓰기 권한 추가

3단계: 정기 검토
   └─ 불필요한 권한 제거
```

### 5.3 Portal Universe 적용

| 서비스 | 초기 권한 | 확장 권한 |
|--------|----------|----------|
| **Shopping Service** | S3 상품 이미지 읽기 | 이미지 업로드 추가 |
| **Blog Service** | S3 첨부파일 읽기 | 첨부파일 업로드 추가 |
| **Notification Service** | SNS 토픽 조회 | 메시지 발행 추가 |

---

## 6. Policy 평가 로직

### 6.1 평가 순서

```
요청 발생
  ↓
1. 명시적 Deny 존재? → 즉시 거부
  ↓
2. 명시적 Allow 존재? → 허용
  ↓
3. 그 외 → 암묵적 Deny (거부)
```

### 6.2 여러 Policy 적용 시

```
User: jane-developer
  ├─ Group: developers (Policy A: Allow s3:GetObject)
  ├─ User Policy (Policy B: Allow s3:PutObject)
  └─ Inline Policy (Policy C: Deny s3:DeleteObject)

결과:
  ✓ s3:GetObject (Policy A)
  ✓ s3:PutObject (Policy B)
  ✗ s3:DeleteObject (Policy C - Deny 우선)
  ✗ s3:DeleteBucket (허용 없음 - 암묵적 Deny)
```

---

## 7. Portal Universe 실습

### 7.1 LocalStack에서 Policy 테스트

```bash
# LocalStack에서는 IAM Policy가 기본적으로 적용되지 않음
# Pro 버전에서만 지원

# 1. S3 버킷 생성
aws --endpoint-url=http://localhost:4566 s3 mb s3://test-bucket

# 2. 객체 업로드 (항상 성공 - 권한 체크 없음)
echo "test" > test.txt
aws --endpoint-url=http://localhost:4566 s3 cp test.txt s3://test-bucket/

# 3. 객체 조회 (항상 성공)
aws --endpoint-url=http://localhost:4566 s3 ls s3://test-bucket/
```

### 7.2 Shopping Service Policy 설계

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ProductImageManagement",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::portal-universe-products/images/*"
    },
    {
      "Sid": "PreventBucketDeletion",
      "Effect": "Deny",
      "Action": [
        "s3:DeleteBucket",
        "s3:DeleteBucketPolicy"
      ],
      "Resource": "arn:aws:s3:::portal-universe-products"
    }
  ]
}
```

---

## 8. 핵심 정리

| 개념 | 설명 |
|------|------|
| **Policy** | 권한을 정의하는 JSON 문서 |
| **Effect** | Allow 또는 Deny |
| **Action** | 허용/거부할 API 작업 |
| **Resource** | 대상 리소스 ARN |
| **Condition** | 조건부 적용 규칙 |
| **AWS 관리형** | AWS 제공 재사용 정책 |
| **고객 관리형** | 사용자 정의 정책 |
| **최소 권한 원칙** | 필요 최소한만 허용 |
| **Deny 우선** | 명시적 Deny가 최우선 |

---

## 다음 학습

- [IAM 모범 사례](./iam-best-practices.md)
- [IAM Role 심화](./iam-roles-deep-dive.md)
- [S3 Bucket Policy](../s3/s3-bucket-policies.md)

---

## 참고 자료

- [IAM Policy 문법](https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_grammar.html)
- [IAM Policy Simulator](https://policysim.aws.amazon.com/)
- [IAM Policy Examples](https://docs.aws.amazon.com/IAM/latest/UserGuide/access_policies_examples.html)
