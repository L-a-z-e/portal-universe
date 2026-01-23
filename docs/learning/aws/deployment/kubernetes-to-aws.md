# Kubernetes to AWS 전환

LocalStack에서 실제 AWS 서비스로 전환하는 방법을 학습합니다.

---

## 1. 학습 목표

- LocalStack → AWS 전환 프로세스 이해
- AWS 리소스 프로비저닝 방법 습득
- Spring Boot 설정 전환 학습
- EKS 배포 시 보안 고려사항 파악

---

## 2. 전환 개요

```
LocalStack (로컬/개발)
    ↓
실제 AWS (스테이징)
    ↓
실제 AWS (프로덕션)
```

| 환경 | S3 Endpoint | IAM | 비용 |
|------|-------------|-----|------|
| **LocalStack** | http://localstack:4566 | test/test | 무료 |
| **AWS Staging** | 기본 (자동) | IAM User/Role | 소액 |
| **AWS Production** | 기본 (자동) | IAM Role (IRSA) | 실제 과금 |

---

## 3. LocalStack → AWS 전환 체크리스트

### Phase 1: AWS 계정 및 리소스 준비

- [ ] AWS 계정 생성 및 결제 설정
- [ ] IAM 사용자/역할 생성
- [ ] S3 버킷 생성 (리전 선택)
- [ ] 버킷 정책 설정
- [ ] CORS 설정
- [ ] 비용 알림 설정

### Phase 2: 애플리케이션 설정 변경

- [ ] `endpoint` 설정 제거 또는 조건부 적용
- [ ] IAM credentials 설정 (개발 환경)
- [ ] IAM Role 설정 (프로덕션 환경)
- [ ] 환경별 프로필 분리
- [ ] S3 버킷 이름 업데이트

### Phase 3: 보안 검토

- [ ] Public Access Block 확인
- [ ] IAM 최소 권한 원칙 적용
- [ ] 암호화 설정 (at-rest, in-transit)
- [ ] 버킷 버저닝 활성화
- [ ] 접근 로그 설정

### Phase 4: 모니터링 및 최적화

- [ ] CloudWatch 메트릭 설정
- [ ] S3 Access Logging 활성화
- [ ] 비용 모니터링 설정
- [ ] 적절한 스토리지 클래스 선택
- [ ] 라이프사이클 규칙 설정

---

## 4. AWS S3 버킷 생성

### AWS CLI로 생성

```bash
# AWS CLI 설치 확인
aws --version

# AWS 자격 증명 설정
aws configure
# AWS Access Key ID: [YOUR_ACCESS_KEY]
# AWS Secret Access Key: [YOUR_SECRET_KEY]
# Default region name: ap-northeast-2
# Default output format: json

# S3 버킷 생성
aws s3 mb s3://portal-universe-uploads-prod --region ap-northeast-2

# 버킷 목록 확인
aws s3 ls
```

### Terraform으로 생성 (권장)

```hcl
# terraform/s3.tf
resource "aws_s3_bucket" "uploads" {
  bucket = "portal-universe-uploads-prod"

  tags = {
    Name        = "Portal Universe Uploads"
    Environment = "production"
    Service     = "blog"
  }
}

# 버저닝 활성화
resource "aws_s3_bucket_versioning" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  versioning_configuration {
    status = "Enabled"
  }
}

# 암호화 설정
resource "aws_s3_bucket_server_side_encryption_configuration" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Public Access Block
resource "aws_s3_bucket_public_access_block" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# CORS 설정
resource "aws_s3_bucket_cors_configuration" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST", "DELETE"]
    allowed_origins = ["https://portal.example.com"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}

# 라이프사이클 규칙
resource "aws_s3_bucket_lifecycle_configuration" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  rule {
    id     = "delete-old-versions"
    status = "Enabled"

    noncurrent_version_expiration {
      noncurrent_days = 90
    }
  }

  rule {
    id     = "transition-to-glacier"
    status = "Enabled"

    transition {
      days          = 90
      storage_class = "GLACIER"
    }
  }
}
```

```bash
# Terraform 실행
cd terraform
terraform init
terraform plan
terraform apply
```

---

## 5. IAM 권한 설정

### 개발/스테이징 환경 (IAM User)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "S3BucketAccess",
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket",
        "s3:GetBucketLocation"
      ],
      "Resource": "arn:aws:s3:::portal-universe-uploads-*"
    },
    {
      "Sid": "S3ObjectAccess",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::portal-universe-uploads-*/*"
    }
  ]
}
```

### 프로덕션 환경 (IAM Role for EKS)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::portal-universe-uploads-prod"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::portal-universe-uploads-prod/*"
    }
  ]
}
```

### Terraform IAM 설정

```hcl
# terraform/iam.tf

# IAM Policy
resource "aws_iam_policy" "blog_s3_policy" {
  name        = "BlogServiceS3Policy"
  description = "Policy for Blog Service S3 access"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:ListBucket"
        ]
        Resource = aws_s3_bucket.uploads.arn
      },
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject"
        ]
        Resource = "${aws_s3_bucket.uploads.arn}/*"
      }
    ]
  })
}

# IAM Role for EKS Service Account
resource "aws_iam_role" "blog_service_role" {
  name = "BlogServiceRole"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/${replace(data.aws_eks_cluster.cluster.identity[0].oidc[0].issuer, "https://", "")}"
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "${replace(data.aws_eks_cluster.cluster.identity[0].oidc[0].issuer, "https://", "")}:sub" = "system:serviceaccount:default:blog-service-sa"
          }
        }
      }
    ]
  })
}

# Attach Policy to Role
resource "aws_iam_role_policy_attachment" "blog_s3_attach" {
  role       = aws_iam_role.blog_service_role.name
  policy_arn = aws_iam_policy.blog_s3_policy.arn
}
```

---

## 6. Spring Boot 설정 전환

### 환경별 프로필 분리

```
src/main/resources/
├── application.yml                    # 공통 설정
├── application-local.yml              # LocalStack (개발)
├── application-docker.yml             # LocalStack (Docker Compose)
├── application-kubernetes.yml         # LocalStack (Kind)
├── application-staging.yml            # 실제 AWS (스테이징)
└── application-production.yml         # 실제 AWS (프로덕션)
```

### application-local.yml (LocalStack)

```yaml
aws:
  s3:
    endpoint: http://localhost:4566
    bucket-name: blog-bucket
    region: ap-northeast-2
    access-key: test
    secret-key: test
```

### application-staging.yml (AWS - IAM User)

```yaml
aws:
  s3:
    # endpoint 제거 (기본 AWS 엔드포인트 사용)
    bucket-name: portal-universe-uploads-staging
    region: ap-northeast-2
    # IAM User credentials (환경 변수로 주입)
    access-key: ${AWS_ACCESS_KEY_ID}
    secret-key: ${AWS_SECRET_ACCESS_KEY}
```

### application-production.yml (AWS - IAM Role)

```yaml
aws:
  s3:
    # endpoint 제거
    bucket-name: portal-universe-uploads-prod
    region: ap-northeast-2
    # IAM Role 사용 (credentials 설정 불필요)
```

### S3 Configuration 클래스 수정

```java
@Configuration
public class AwsConfig {

    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key:}")
    private String accessKey;

    @Value("${aws.s3.secret-key:}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
            .region(Region.of(region));

        // LocalStack 사용 시 (endpoint가 설정된 경우)
        if (!s3Endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(s3Endpoint))
                   .credentialsProvider(StaticCredentialsProvider.create(
                       AwsBasicCredentials.create(accessKey, secretKey)
                   ))
                   .serviceConfiguration(S3Configuration.builder()
                       .pathStyleAccessEnabled(true)
                       .build());
        }
        // AWS 사용 시
        else {
            // IAM User credentials가 있으면 사용
            if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
                builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
                ));
            }
            // 없으면 Default Credential Provider Chain 사용
            // (환경 변수, EC2 Instance Profile, EKS Pod Identity 등)
            else {
                builder.credentialsProvider(DefaultCredentialsProvider.create());
            }
        }

        return builder.build();
    }
}
```

---

## 7. EKS 배포 설정

### IRSA (IAM Roles for Service Accounts)

```yaml
# k8s/blog-service/serviceaccount.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: blog-service-sa
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/BlogServiceRole
```

```yaml
# k8s/blog-service/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: blog-service
spec:
  template:
    spec:
      serviceAccountName: blog-service-sa  # ServiceAccount 지정
      containers:
        - name: blog-service
          image: blog-service:1.0.0
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "production"
            # IAM Role 사용으로 인해 AWS credentials 불필요
```

### Pod Identity (EKS 1.24+)

```yaml
# k8s/blog-service/pod-identity-association.yaml
apiVersion: eks.amazonaws.com/v1
kind: PodIdentityAssociation
metadata:
  name: blog-service-pod-identity
spec:
  serviceAccount: blog-service-sa
  roleArn: arn:aws:iam::123456789012:role/BlogServiceRole
```

---

## 8. Kubernetes Secret 관리

### AWS Secrets Manager 사용

```yaml
# k8s/external-secrets/secret-store.yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aws-secrets-manager
spec:
  provider:
    aws:
      service: SecretsManager
      region: ap-northeast-2
      auth:
        jwt:
          serviceAccountRef:
            name: blog-service-sa

---
# k8s/external-secrets/external-secret.yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: blog-service-secrets
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: blog-service-secret
    creationPolicy: Owner
  data:
    - secretKey: aws-access-key
      remoteRef:
        key: portal-universe/blog-service
        property: AWS_ACCESS_KEY_ID
    - secretKey: aws-secret-key
      remoteRef:
        key: portal-universe/blog-service
        property: AWS_SECRET_ACCESS_KEY
```

---

## 9. CloudFormation 예시

```yaml
# cloudformation/s3-stack.yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: Portal Universe S3 Bucket Stack

Parameters:
  Environment:
    Type: String
    Default: production
    AllowedValues:
      - staging
      - production

Resources:
  UploadsBucket:
    Type: AWS::S3::Bucket
    Properties:
      BucketName: !Sub portal-universe-uploads-${Environment}
      VersioningConfiguration:
        Status: Enabled
      BucketEncryption:
        ServerSideEncryptionConfiguration:
          - ServerSideEncryptionByDefault:
              SSEAlgorithm: AES256
      PublicAccessBlockConfiguration:
        BlockPublicAcls: true
        BlockPublicPolicy: true
        IgnorePublicAcls: true
        RestrictPublicBuckets: true
      CorsConfiguration:
        CorsRules:
          - AllowedHeaders:
              - '*'
            AllowedMethods:
              - GET
              - PUT
              - POST
              - DELETE
            AllowedOrigins:
              - 'https://portal.example.com'
            ExposedHeaders:
              - ETag
            MaxAge: 3000
      LifecycleConfiguration:
        Rules:
          - Id: DeleteOldVersions
            Status: Enabled
            NoncurrentVersionExpiration:
              NoncurrentDays: 90
          - Id: TransitionToGlacier
            Status: Enabled
            Transitions:
              - StorageClass: GLACIER
                TransitionInDays: 90

Outputs:
  BucketName:
    Description: Name of the S3 bucket
    Value: !Ref UploadsBucket
    Export:
      Name: !Sub ${AWS::StackName}-BucketName

  BucketArn:
    Description: ARN of the S3 bucket
    Value: !GetAtt UploadsBucket.Arn
    Export:
      Name: !Sub ${AWS::StackName}-BucketArn
```

```bash
# CloudFormation 스택 생성
aws cloudformation create-stack \
  --stack-name portal-universe-s3 \
  --template-body file://cloudformation/s3-stack.yaml \
  --parameters ParameterKey=Environment,ParameterValue=production

# 스택 상태 확인
aws cloudformation describe-stacks --stack-name portal-universe-s3
```

---

## 10. 비용 최적화

### 스토리지 클래스 선택

| 클래스 | 용도 | 비용 | 접근 빈도 |
|--------|------|------|----------|
| **Standard** | 자주 접근 | 높음 | 일일 |
| **Intelligent-Tiering** | 자동 최적화 | 중간 | 변동 |
| **Standard-IA** | 가끔 접근 | 낮음 | 월간 |
| **Glacier** | 아카이브 | 매우 낮음 | 년간 |

### 라이프사이클 정책 예시

```json
{
  "Rules": [
    {
      "Id": "Transition old files",
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
      ]
    },
    {
      "Id": "Delete temporary files",
      "Status": "Enabled",
      "Expiration": {
        "Days": 7
      },
      "Filter": {
        "Prefix": "temp/"
      }
    }
  ]
}
```

---

## 11. 모니터링 설정

### CloudWatch 메트릭

```hcl
# terraform/cloudwatch.tf
resource "aws_cloudwatch_metric_alarm" "s3_4xx_errors" {
  alarm_name          = "s3-high-4xx-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "4xxErrors"
  namespace           = "AWS/S3"
  period              = "300"
  statistic           = "Sum"
  threshold           = "100"
  alarm_description   = "This metric monitors S3 4xx errors"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    BucketName = aws_s3_bucket.uploads.id
  }
}

resource "aws_cloudwatch_metric_alarm" "s3_5xx_errors" {
  alarm_name          = "s3-high-5xx-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "5xxErrors"
  namespace           = "AWS/S3"
  period              = "300"
  statistic           = "Sum"
  threshold           = "10"
  alarm_description   = "This metric monitors S3 5xx errors"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    BucketName = aws_s3_bucket.uploads.id
  }
}
```

### S3 Access Logging

```hcl
resource "aws_s3_bucket" "logs" {
  bucket = "portal-universe-logs"
}

resource "aws_s3_bucket_logging" "uploads_logging" {
  bucket = aws_s3_bucket.uploads.id

  target_bucket = aws_s3_bucket.logs.id
  target_prefix = "s3-access-logs/"
}
```

---

## 12. 전환 절차

### Step 1: AWS 리소스 프로비저닝

```bash
# Terraform 실행
cd terraform
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

### Step 2: 애플리케이션 설정 업데이트

```yaml
# k8s/blog-service/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: blog-service-config
data:
  SPRING_PROFILES_ACTIVE: "production"
  AWS_REGION: "ap-northeast-2"
  S3_BUCKET_NAME: "portal-universe-uploads-prod"
```

### Step 3: ServiceAccount 및 IAM Role 연결

```bash
# ServiceAccount 생성
kubectl apply -f k8s/blog-service/serviceaccount.yaml

# Deployment 업데이트
kubectl apply -f k8s/blog-service/deployment.yaml

# 확인
kubectl describe sa blog-service-sa
kubectl describe pod -l app=blog-service
```

### Step 4: 테스트

```bash
# Pod 내부에서 AWS credentials 확인
kubectl exec -it blog-service-xxx -- env | grep AWS

# 로그 확인
kubectl logs -f deployment/blog-service

# 파일 업로드 테스트
curl -X POST http://blog-service/api/v1/files/upload \
  -F "file=@test.jpg"

# S3 버킷 확인
aws s3 ls s3://portal-universe-uploads-prod/
```

---

## 13. 핵심 요약

### LocalStack → AWS 주요 변경사항

| 항목 | LocalStack | AWS |
|------|------------|-----|
| **Endpoint** | http://localstack:4566 | 제거 (기본) |
| **Credentials** | test/test | IAM User/Role |
| **PathStyle** | 필수 (true) | 불필요 (기본 false) |
| **버킷 생성** | 수동/스크립트 | Terraform/CloudFormation |
| **모니터링** | 없음 | CloudWatch |

### EKS 배포 권장사항

1. **IRSA 사용**: IAM Role for Service Accounts
2. **Secrets Manager**: 민감 정보 중앙 관리
3. **CloudWatch**: 메트릭 및 로그 모니터링
4. **비용 알림**: 예산 초과 방지

---

## 14. 관련 문서

- [Environment Profiles](./environment-profiles.md) - 환경별 프로필
- [Local to Kubernetes](./local-to-kubernetes.md) - K8s 전환 가이드
- [AWS Migration Checklist](../best-practices/aws-migration-checklist.md) - 마이그레이션 체크리스트
- [LocalStack S3](../infra/localstack-s3.md) - LocalStack 사용법
- [S3 Fundamentals](../s3/s3-fundamentals.md) - S3 기초
