# AWS IAM 소개

## 학습 목표
- IAM(Identity and Access Management)의 핵심 개념 이해
- User, Group, Role, Policy의 차이점 파악
- 인증(Authentication)과 권한부여(Authorization) 구분
- LocalStack에서의 IAM 테스트 환경 이해

---

## 1. IAM이란?

AWS IAM(Identity and Access Management)은 **AWS 리소스에 대한 접근을 안전하게 제어**하는 서비스입니다.

### 핵심 역할

| 기능 | 설명 |
|------|------|
| **인증 (Authentication)** | "누구인가?"를 확인 (로그인, Access Key) |
| **권한부여 (Authorization)** | "무엇을 할 수 있는가?"를 결정 (Policy) |
| **접근 제어** | AWS 서비스/리소스 사용 권한 관리 |
| **감사 (Auditing)** | 누가 언제 무엇을 했는지 추적 (CloudTrail) |

### IAM이 없다면?

```
❌ 모든 사용자가 Root 계정 사용
❌ 누구나 모든 리소스 접근 가능
❌ S3 버킷 삭제, EC2 종료 등 위험한 작업 가능
❌ 개발자, 운영자, 시스템 계정 구분 불가
```

---

## 2. 핵심 개념

### 2.1 User (사용자)

**개인이나 애플리케이션**을 나타내는 IAM 엔티티입니다.

```
┌─────────────────────────────┐
│   IAM User: jane-developer  │
├─────────────────────────────┤
│ - Access Key: AKIAIOSFODNN7 │
│ - Secret Key: wJalrXUtnFEMI │
│ - Password: (Console login)  │
│ - MFA: Enabled              │
└─────────────────────────────┘
```

**User 종류:**
- **Human User**: 개발자, 운영자 등 실제 사람
- **System User**: 애플리케이션, 스크립트 등

### 2.2 Group (그룹)

**User 집합**으로 권한을 일괄 관리합니다.

```
┌───────────────────────────────────┐
│  Group: developers                │
├───────────────────────────────────┤
│  Members:                         │
│    - jane-developer               │
│    - john-backend                 │
│    - sarah-frontend               │
├───────────────────────────────────┤
│  Attached Policies:               │
│    - AmazonS3ReadOnlyAccess       │
│    - AmazonEC2ReadOnlyAccess      │
└───────────────────────────────────┘
```

**장점:**
- 사용자마다 개별 정책 부여 불필요
- 팀 단위 권한 관리 용이
- 직무 변경 시 그룹 이동만으로 권한 변경

### 2.3 Role (역할)

**임시로 권한을 위임**받을 수 있는 IAM 엔티티입니다.

```
                  ┌──────────────────┐
                  │   IAM Role:      │
                  │ shopping-service │
                  └──────────────────┘
                           ↑
                           │ Assume Role
                  ┌────────┴─────────┐
                  │                  │
           ┌──────────┐       ┌──────────┐
           │  EC2     │       │  Lambda  │
           │ Instance │       │ Function │
           └──────────┘       └──────────┘
```

**User vs Role:**
| 특성 | User | Role |
|------|------|------|
| **대상** | 특정 개인/애플리케이션 | 서비스, 임시 사용자 |
| **자격증명** | 영구 (Access Key) | 임시 (STS Token) |
| **사용 사례** | 개발자 로그인 | EC2, Lambda, EKS Pod |

### 2.4 Policy (정책)

**권한을 정의하는 JSON 문서**입니다.

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

**Policy 종류:**
- **AWS 관리형**: AWS가 미리 만든 정책 (예: `AmazonS3FullAccess`)
- **고객 관리형**: 사용자가 직접 만든 정책
- **인라인 정책**: 특정 User/Role에 직접 삽입

---

## 3. 인증 vs 권한부여

### 3.1 인증 (Authentication)

"당신은 누구인가?"를 확인하는 단계입니다.

```
┌──────────────────────────────────────────────┐
│  인증 방식                                    │
├──────────────────────────────────────────────┤
│  1. Console Login                            │
│     - Username: jane-developer               │
│     - Password: ********                     │
│     - MFA: 123456                            │
│                                              │
│  2. Programmatic Access (CLI, SDK)           │
│     - Access Key ID: AKIAIOSFODNN7EXAMPLE    │
│     - Secret Access Key: wJalrXUtnFEMI/...   │
└──────────────────────────────────────────────┘
```

### 3.2 권한부여 (Authorization)

"당신은 무엇을 할 수 있는가?"를 결정하는 단계입니다.

```
인증 성공 → IAM Policy 평가 → 권한 있음/없음

예시:
User: jane-developer (인증 ✓)
Action: s3:DeleteBucket
Policy Check:
  ❌ S3 Read 권한만 있음
  ❌ Delete 권한 없음
Result: Access Denied
```

---

## 4. Access Key & Secret Key

### 4.1 개념

AWS API를 호출하기 위한 **자격증명 쌍**입니다.

```
Access Key ID:     AKIAIOSFODNN7EXAMPLE
Secret Access Key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

| 구분 | Access Key ID | Secret Access Key |
|------|---------------|-------------------|
| **노출** | 공개 가능 (ID 역할) | **절대 노출 금지** (비밀번호) |
| **길이** | 20자 | 40자 |
| **용도** | 사용자 식별 | 서명 생성 (HMAC) |

### 4.2 사용 예시

```bash
# AWS CLI 설정
aws configure
AWS Access Key ID [None]: AKIAIOSFODNN7EXAMPLE
AWS Secret Access Key [None]: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
Default region name [None]: us-east-1
Default output format [None]: json

# S3 버킷 리스트 조회
aws s3 ls
```

**내부 동작:**
1. AWS CLI가 Access Key로 요청 서명 생성
2. AWS가 서명 검증 (Secret Key 일치 확인)
3. IAM Policy 평가
4. 권한 있으면 API 실행

---

## 5. LocalStack에서의 IAM

### 5.1 LocalStack 특징

Portal Universe는 로컬 개발 시 **LocalStack**을 사용합니다.

```yaml
# docker-compose.yml
services:
  localstack:
    image: localstack/localstack
    environment:
      - SERVICES=s3,dynamodb,sns,sqs
      - DEFAULT_REGION=us-east-1
    ports:
      - "4566:4566"
```

### 5.2 테스트 Credentials

LocalStack은 **모든 Access Key를 허용**합니다.

```bash
# Portal Universe 개발 환경
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_REGION=us-east-1
export AWS_ENDPOINT_URL=http://localhost:4566
```

**특징:**
- Access Key/Secret Key 검증 없음 (항상 `test/test` 사용)
- IAM Policy는 LocalStack Pro에서만 지원
- 로컬 개발에서는 권한 제어 없이 모든 작업 가능

### 5.3 실제 AWS vs LocalStack

| 항목 | 실제 AWS | LocalStack |
|------|----------|------------|
| **인증** | 실제 Access Key 필요 | `test/test` 허용 |
| **권한부여** | IAM Policy 엄격 적용 | Pro만 지원 |
| **비용** | 사용량 과금 | 무료 |
| **용도** | 프로덕션 | 로컬 개발/테스트 |

---

## 6. Portal Universe 적용

### 6.1 서비스별 IAM 전략

```
┌─────────────────────────────────────────────┐
│  Local 환경 (LocalStack)                    │
├─────────────────────────────────────────────┤
│  - Credentials: test/test                   │
│  - IAM Policy: 사용 안 함 (모든 권한)       │
│  - 목적: 빠른 개발, 비용 없음               │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│  Production 환경 (실제 AWS)                 │
├─────────────────────────────────────────────┤
│  - Credentials: IAM Role (EKS Pod)          │
│  - IAM Policy: 최소 권한 원칙               │
│  - 목적: 보안, 감사 추적                    │
└─────────────────────────────────────────────┘
```

### 6.2 서비스별 필요 권한

| 서비스 | AWS 리소스 | 필요 권한 |
|--------|-----------|----------|
| **Shopping Service** | S3 (상품 이미지) | `s3:PutObject`, `s3:GetObject` |
| **Blog Service** | S3 (블로그 첨부파일) | `s3:PutObject`, `s3:GetObject` |
| **Notification Service** | SNS/SQS | `sns:Publish`, `sqs:SendMessage` |

---

## 7. 핵심 정리

| 개념 | 설명 |
|------|------|
| **IAM** | AWS 리소스 접근 제어 서비스 |
| **User** | 영구 자격증명을 가진 개인/애플리케이션 |
| **Group** | User 집합으로 권한 일괄 관리 |
| **Role** | 임시 권한 위임 (EC2, Lambda, EKS) |
| **Policy** | 권한을 정의하는 JSON 문서 |
| **Access Key** | API 호출용 자격증명 (공개 ID + 비밀 Key) |
| **인증** | "누구인가?" 확인 |
| **권한부여** | "무엇을 할 수 있는가?" 결정 |
| **LocalStack** | 로컬 개발용 AWS 모킹 (test/test) |

---

## 다음 학습

- [IAM 정책 심화](./iam-policies.md)
- [IAM 모범 사례](./iam-best-practices.md)
- [LocalStack S3 활용](../infra/localstack-s3.md)

---

## 참고 자료

- [AWS IAM 공식 문서](https://docs.aws.amazon.com/IAM/latest/UserGuide/)
- [LocalStack IAM 문서](https://docs.localstack.cloud/user-guide/aws/iam/)
- [AWS 보안 모범 사례](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)
