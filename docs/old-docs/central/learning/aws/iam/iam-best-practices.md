# IAM 모범 사례 (IAM Best Practices)

## 학습 목표
- IAM 보안 강화를 위한 모범 사례 이해
- Root 계정 보호 및 MFA 적용 방법
- Credentials 안전한 관리 전략
- IAM Role 활용 및 정기적 권한 검토

---

## 1. Root 계정 사용 금지

### 1.1 Root 계정이란?

AWS 계정 생성 시 만들어지는 **최상위 권한 계정**입니다.

```
┌────────────────────────────────────┐
│  Root 계정                          │
├────────────────────────────────────┤
│  - Email: admin@company.com        │
│  - 권한: 모든 AWS 서비스/리소스    │
│  - 제한: 불가능 (IAM Policy 무시)  │
│  - 위험: 탈취 시 전체 계정 손실    │
└────────────────────────────────────┘
```

### 1.2 Root 계정의 위험성

```
❌ Root 계정 탈취 시:
  - 모든 리소스 삭제 가능
  - 청구 정보 변경 가능
  - 계정 닫기 가능
  - 복구 불가능한 손실 발생
```

### 1.3 Root 계정 사용 원칙

```
✅ Root 계정은 오직 이 경우에만:
  1. 계정 초기 설정 (IAM User 생성)
  2. MFA 활성화
  3. 계정 설정 변경 (이메일, 결제 정보)
  4. 지원 플랜 변경
  5. IAM User로 불가능한 작업 (매우 드묾)

✅ 일상 작업은 IAM User 사용
```

### 1.4 Root 계정 보호

```bash
# 1. 강력한 비밀번호 설정
최소 16자 이상
대소문자, 숫자, 특수문자 조합

# 2. MFA 활성화 (필수!)
AWS Console → IAM → My Security Credentials → MFA

# 3. Access Key 생성 금지
Root는 Console 로그인만 사용

# 4. 정기적 활동 검토
CloudTrail에서 Root 계정 사용 내역 확인
```

---

## 2. MFA (Multi-Factor Authentication) 활성화

### 2.1 MFA란?

**2단계 인증**으로 비밀번호 외 추가 인증 수단을 요구합니다.

```
로그인 시도
  ↓
1단계: 비밀번호 입력
  ↓
2단계: MFA 코드 입력 (6자리 숫자)
  ↓
로그인 성공
```

### 2.2 MFA 종류

| 유형 | 설명 | 예시 |
|------|------|------|
| **Virtual MFA** | 스마트폰 앱 | Google Authenticator, Authy |
| **Hardware MFA** | 물리적 디바이스 | YubiKey, Gemalto |
| **SMS MFA** | 문자 메시지 | (AWS는 미지원) |

### 2.3 MFA 설정

```bash
# AWS Console에서 설정
1. IAM → Users → [사용자 선택]
2. Security credentials 탭
3. Assigned MFA device → Manage
4. Virtual MFA device 선택
5. Google Authenticator로 QR 코드 스캔
6. 연속된 2개의 MFA 코드 입력
```

### 2.4 MFA 정책 적용

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Deny",
      "Action": "*",
      "Resource": "*",
      "Condition": {
        "BoolIfExists": {
          "aws:MultiFactorAuthPresent": "false"
        }
      }
    }
  ]
}
```

**효과:** MFA 없이는 AWS 접근 불가

---

## 3. Credentials 안전한 관리

### 3.1 절대 금지 사항

```
❌ 절대 하지 말 것:
  1. 코드에 Access Key 하드코딩
  2. Git에 Credentials 커밋
  3. 공개 저장소에 .env 파일 푸시
  4. Slack/Email로 Access Key 공유
  5. 로그에 Secret Key 출력
```

**잘못된 예시:**

```java
❌ // 절대 금지!
public class S3Service {
    private static final String ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG...";
}
```

```bash
❌ # .env 파일을 Git에 커밋
git add .env
git commit -m "Add environment variables"  # 위험!
```

### 3.2 안전한 관리 방법

#### 방법 1: 환경 변수

```bash
# .env 파일 (Git 제외!)
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG...
AWS_REGION=us-east-1

# .gitignore에 추가
echo ".env" >> .gitignore
```

```java
// Spring Boot - 환경 변수에서 로드
@Value("${aws.accessKeyId}")
private String accessKeyId;

@Value("${aws.secretKey}")
private String secretKey;
```

#### 방법 2: AWS Secrets Manager

```bash
# Secret 생성
aws secretsmanager create-secret \
  --name prod/shopping-service/db \
  --secret-string '{"username":"admin","password":"secret123"}'

# Secret 조회
aws secretsmanager get-secret-value \
  --secret-id prod/shopping-service/db
```

```java
// Spring Boot에서 Secrets Manager 사용
@Configuration
public class SecretsManagerConfig {

    @Bean
    public DataSource dataSource() {
        String secret = getSecret("prod/shopping-service/db");
        // JSON 파싱 후 DataSource 구성
    }

    private String getSecret(String secretName) {
        AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
            .withRegion("us-east-1")
            .build();

        GetSecretValueRequest request = new GetSecretValueRequest()
            .withSecretId(secretName);

        return client.getSecretValue(request).getSecretString();
    }
}
```

#### 방법 3: IAM Role (권장)

```yaml
# EKS Pod에 IAM Role 할당 (IRSA - IAM Roles for Service Accounts)
apiVersion: v1
kind: ServiceAccount
metadata:
  name: shopping-service-sa
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/shopping-service-role
```

```java
// Access Key 불필요! SDK가 자동으로 Role 사용
S3Client s3Client = S3Client.builder()
    .region(Region.US_EAST_1)
    .build();  // Credentials 지정 불필요
```

### 3.3 Portal Universe 전략

| 환경 | Credentials 관리 |
|------|------------------|
| **Local** | LocalStack (`test/test`) |
| **Development** | IAM User + 환경 변수 |
| **Staging** | IAM Role (EKS IRSA) |
| **Production** | IAM Role (EKS IRSA) |

---

## 4. IAM Role 활용

### 4.1 User vs Role

```
┌─────────────────────────────────────────┐
│  IAM User (피해야 할 패턴)              │
├─────────────────────────────────────────┤
│  EC2 Instance                           │
│    └─ Access Key 저장 (파일/환경변수)  │
│       ❌ 위험: 인스턴스 탈취 시 Key 노출│
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  IAM Role (권장 패턴)                   │
├─────────────────────────────────────────┤
│  EC2 Instance                           │
│    └─ IAM Role 할당                     │
│       ✓ 임시 Credentials 자동 로테이션  │
│       ✓ Key 노출 위험 없음              │
└─────────────────────────────────────────┘
```

### 4.2 Role 사용 사례

| 서비스 | Role 사용 |
|--------|----------|
| **EC2** | Instance Profile에 Role 할당 |
| **Lambda** | Execution Role 자동 부여 |
| **EKS Pod** | IRSA (Service Account에 Role 연결) |
| **CodeBuild** | Service Role |

### 4.3 EKS IRSA 설정 예시

```bash
# 1. OIDC Provider 생성
eksctl utils associate-iam-oidc-provider \
  --cluster=portal-universe \
  --approve

# 2. IAM Role 생성
eksctl create iamserviceaccount \
  --name shopping-service-sa \
  --namespace default \
  --cluster portal-universe \
  --role-name shopping-service-role \
  --attach-policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess \
  --approve
```

```yaml
# 3. Deployment에서 ServiceAccount 사용
apiVersion: apps/v1
kind: Deployment
metadata:
  name: shopping-service
spec:
  template:
    spec:
      serviceAccountName: shopping-service-sa  # IAM Role 자동 적용
      containers:
        - name: shopping-service
          image: shopping-service:latest
```

---

## 5. 정기적 키 로테이션

### 5.1 Access Key 로테이션

```
┌──────────────────────────────────────┐
│  키 로테이션 주기                     │
├──────────────────────────────────────┤
│  90일마다 Access Key 교체            │
│                                      │
│  Day 1-90:  Key A 사용               │
│  Day 90:    Key B 생성               │
│  Day 91:    Key B로 전환             │
│  Day 92:    Key A 삭제               │
└──────────────────────────────────────┘
```

### 5.2 로테이션 절차

```bash
# 1. 현재 Access Key 확인
aws iam list-access-keys --user-name jane-developer

# 2. 새 Access Key 생성
aws iam create-access-key --user-name jane-developer

# 3. 애플리케이션에 새 Key 배포
# 환경 변수 업데이트 후 재시작

# 4. 구 Key 비활성화 (테스트 기간)
aws iam update-access-key \
  --user-name jane-developer \
  --access-key-id AKIAIOSFODNN7EXAMPLE \
  --status Inactive

# 5. 문제 없으면 구 Key 삭제
aws iam delete-access-key \
  --user-name jane-developer \
  --access-key-id AKIAIOSFODNN7EXAMPLE
```

### 5.3 자동 로테이션

```python
# Lambda 함수로 자동 로테이션
import boto3
from datetime import datetime, timedelta

def rotate_access_keys(event, context):
    iam = boto3.client('iam')

    # 모든 사용자 조회
    users = iam.list_users()['Users']

    for user in users:
        username = user['UserName']
        keys = iam.list_access_keys(UserName=username)['AccessKeyMetadata']

        for key in keys:
            # 90일 이상된 Key 찾기
            age = (datetime.now(key['CreateDate'].tzinfo) - key['CreateDate']).days

            if age > 90:
                # Slack 알림
                send_notification(f"User {username} has old key: {key['AccessKeyId']}")
```

---

## 6. 권한 최소화 및 정기 검토

### 6.1 정기 검토 체크리스트

```
□ 90일 이상 미사용 User 삭제
□ 불필요한 권한 제거
□ Group 정책 재검토
□ 과도한 권한 부여 확인 (PowerUser, AdministratorAccess)
□ Inline Policy → 관리형 Policy 전환
□ Access Key 로테이션 상태 확인
```

### 6.2 IAM Access Analyzer

```bash
# Access Analyzer 활성화
aws accessanalyzer create-analyzer \
  --analyzer-name portal-universe-analyzer \
  --type ACCOUNT

# 외부 공유 리소스 확인
aws accessanalyzer list-findings \
  --analyzer-arn arn:aws:access-analyzer:us-east-1:123456789012:analyzer/portal-universe-analyzer
```

### 6.3 Credential Report

```bash
# IAM Credential Report 생성
aws iam generate-credential-report

# Report 다운로드 (CSV)
aws iam get-credential-report --output text > credentials.csv

# 분석 항목:
# - password_last_used (비밀번호 마지막 사용)
# - access_key_1_last_used_date (Key 마지막 사용)
# - mfa_active (MFA 활성화 여부)
```

---

## 7. Portal Universe 보안 전략

### 7.1 환경별 전략

```
┌──────────────────────────────────────────┐
│  Local (LocalStack)                      │
├──────────────────────────────────────────┤
│  - Credentials: test/test (고정)         │
│  - MFA: 사용 안 함                        │
│  - 목적: 빠른 개발                        │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  Development                             │
├──────────────────────────────────────────┤
│  - Credentials: IAM User + 환경변수      │
│  - MFA: 권장                              │
│  - 권한: 개발용 리소스만                  │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  Production                              │
├──────────────────────────────────────────┤
│  - Credentials: IAM Role (EKS IRSA)      │
│  - MFA: 필수 (사람 접근 시)               │
│  - 권한: 최소 권한 원칙                   │
│  - 감사: CloudTrail 활성화                │
└──────────────────────────────────────────┘
```

### 7.2 서비스별 IAM Role

```yaml
# shopping-service-role
Policies:
  - AmazonS3ReadWriteAccess (상품 이미지)
  - AmazonSNSPublishAccess (알림)

# blog-service-role
Policies:
  - AmazonS3ReadWriteAccess (첨부파일)
  - CloudWatchLogsFullAccess (로깅)

# notification-service-role
Policies:
  - AmazonSNSFullAccess (알림 발송)
  - AmazonSQSFullAccess (큐 처리)
```

---

## 8. 추가 모범 사례

### 8.1 CloudTrail 활성화

```bash
# CloudTrail로 모든 API 호출 기록
aws cloudtrail create-trail \
  --name portal-universe-trail \
  --s3-bucket-name portal-universe-cloudtrail

# Trail 시작
aws cloudtrail start-logging \
  --name portal-universe-trail
```

**기록되는 정보:**
- 누가 (User/Role)
- 언제 (Timestamp)
- 무엇을 (API Action)
- 어디서 (IP Address)
- 결과 (성공/실패)

### 8.2 IAM Policy Simulator

```bash
# Policy 테스트 (실제 실행 없이)
aws iam simulate-principal-policy \
  --policy-source-arn arn:aws:iam::123456789012:user/jane-developer \
  --action-names s3:DeleteBucket \
  --resource-arns arn:aws:s3:::my-bucket
```

### 8.3 AWS Organizations

```
Root OU
├── Production OU
│   ├── Account: prod-account
│   └── SCP: Prevent S3 public access
└── Development OU
    ├── Account: dev-account
    └── SCP: Restrict expensive services
```

**SCP (Service Control Policy):** 계정 레벨 권한 제한

---

## 9. 핵심 정리

| 모범 사례 | 설명 |
|-----------|------|
| **Root 금지** | 일상 작업은 IAM User 사용 |
| **MFA 필수** | Root + 관리자 계정은 MFA 활성화 |
| **Credentials 보호** | 코드/Git에 절대 포함 금지 |
| **환경 변수** | Local/Dev 환경에서 사용 |
| **Secrets Manager** | DB 비밀번호 등 민감 정보 |
| **IAM Role** | EC2/Lambda/EKS에서 우선 사용 |
| **키 로테이션** | 90일마다 Access Key 교체 |
| **최소 권한** | 필요한 권한만 부여 |
| **정기 검토** | 분기별 권한 및 사용자 점검 |
| **CloudTrail** | 모든 API 호출 기록 |

---

## 10. 보안 체크리스트

```
□ Root 계정 MFA 활성화
□ Root 계정 Access Key 삭제
□ 모든 관리자 User MFA 활성화
□ 코드에서 Access Key 하드코딩 제거
□ .env 파일 .gitignore 등록
□ Production에서 IAM Role 사용
□ 90일마다 Access Key 로테이션
□ 미사용 User/Key 정기 삭제
□ CloudTrail 활성화
□ IAM Access Analyzer 설정
□ 최소 권한 원칙 적용
□ Group으로 권한 관리
```

---

## 다음 학습

- [IAM 소개](./iam-introduction.md)
- [IAM 정책](./iam-policies.md)
- [AWS Secrets Manager](../security/secrets-manager.md)
- [CloudTrail 로깅](../security/cloudtrail-logging.md)

---

## 참고 자료

- [AWS IAM 보안 모범 사례](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)
- [AWS Security Hub](https://aws.amazon.com/security-hub/)
- [OWASP AWS 보안 가이드](https://owasp.org/www-project-serverless-top-10/)
- [CIS AWS Foundations Benchmark](https://www.cisecurity.org/benchmark/amazon_web_services)
