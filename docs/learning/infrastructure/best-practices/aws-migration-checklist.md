# AWS Migration Checklist

LocalStack에서 실제 AWS로 마이그레이션하기 위한 종합 체크리스트입니다.

---

## 1. 학습 목표

- AWS 마이그레이션 전 사전 준비 사항 이해
- 단계별 마이그레이션 절차 습득
- 보안 및 비용 최적화 체크포인트 파악
- 롤백 및 장애 대응 계획 수립

---

## 2. 마이그레이션 단계 개요

```
Phase 1: 사전 준비 (1-2일)
    ↓
Phase 2: AWS 리소스 생성 (1일)
    ↓
Phase 3: 애플리케이션 설정 (1일)
    ↓
Phase 4: 테스트 및 검증 (1-2일)
    ↓
Phase 5: 프로덕션 배포 (1일)
    ↓
Phase 6: 모니터링 및 최적화 (지속)
```

---

## 3. Phase 1: 사전 준비

### 3.1 AWS 계정 설정

- [ ] **AWS 계정 생성**
  ```bash
  # AWS Console 접속
  https://aws.amazon.com/console/

  # 루트 사용자 로그인
  # 이메일 및 비밀번호 설정
  ```

- [ ] **MFA (Multi-Factor Authentication) 활성화**
  ```
  IAM → 사용자 → 보안 자격 증명
  → MFA 디바이스 할당
  → Virtual MFA device (Google Authenticator 등)
  ```

- [ ] **결제 정보 등록**
  ```
  Billing and Cost Management
  → Payment methods
  → 신용카드 등록
  ```

- [ ] **비용 알림 설정**
  ```
  Billing and Cost Management
  → Budgets
  → Create budget

  예시:
  - 월 예산: $100
  - 알림 임계값: 80%, 90%, 100%
  - 이메일 알림 설정
  ```

### 3.2 IAM 설정

- [ ] **IAM 관리자 계정 생성**
  ```json
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": "*",
        "Resource": "*"
      }
    ]
  }
  ```

  ```bash
  # IAM User 생성
  aws iam create-user --user-name admin-user

  # AdministratorAccess Policy 연결
  aws iam attach-user-policy \
    --user-name admin-user \
    --policy-arn arn:aws:iam::aws:policy/AdministratorAccess

  # Access Key 생성
  aws iam create-access-key --user-name admin-user
  ```

- [ ] **개발자 계정 생성 (최소 권한)**
  ```json
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "s3:*",
          "eks:Describe*",
          "eks:List*",
          "cloudwatch:Get*",
          "cloudwatch:List*"
        ],
        "Resource": "*"
      }
    ]
  }
  ```

- [ ] **서비스 역할 생성 (EKS용)**
  - Blog Service Role
  - Auth Service Role
  - Shopping Service Role

### 3.3 비용 계획

- [ ] **예상 비용 산출**

  | 서비스 | 리소스 | 월 예상 비용 |
  |--------|--------|--------------|
  | **S3** | 100GB 저장, 10만 요청 | ~$3 |
  | **EKS** | 1 클러스터 | $73 |
  | **EC2** | t3.medium × 2 | ~$60 |
  | **RDS** | db.t3.micro | ~$15 |
  | **NAT Gateway** | 1개 | ~$32 |
  | **CloudWatch** | 로그 10GB | ~$5 |
  | **총계** | | **~$188** |

- [ ] **무료 티어 확인**
  ```
  S3: 5GB 스토리지, 2만 GET, 2천 PUT (12개월)
  EC2: t2.micro 750시간/월 (12개월)
  RDS: db.t2.micro 750시간/월 (12개월)
  CloudWatch: 10개 메트릭 (무료)
  ```

- [ ] **비용 태그 전략 수립**
  ```yaml
  tags:
    Environment: production
    Service: blog-service
    CostCenter: engineering
    Owner: team-backend
  ```

---

## 4. Phase 2: AWS 리소스 생성

### 4.1 S3 버킷

- [ ] **버킷 생성**
  ```bash
  # 프로덕션 버킷
  aws s3 mb s3://portal-universe-uploads-prod --region ap-northeast-2

  # 스테이징 버킷
  aws s3 mb s3://portal-universe-uploads-staging --region ap-northeast-2

  # 백업 버킷
  aws s3 mb s3://portal-universe-backups --region ap-northeast-2
  ```

- [ ] **버킷 정책 설정**
  ```json
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "AllowServiceAccess",
        "Effect": "Allow",
        "Principal": {
          "AWS": "arn:aws:iam::123456789012:role/BlogServiceRole"
        },
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

  ```bash
  aws s3api put-bucket-policy \
    --bucket portal-universe-uploads-prod \
    --policy file://bucket-policy.json
  ```

- [ ] **CORS 설정**
  ```json
  {
    "CORSRules": [
      {
        "AllowedHeaders": ["*"],
        "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
        "AllowedOrigins": ["https://portal.example.com"],
        "ExposeHeaders": ["ETag"],
        "MaxAgeSeconds": 3000
      }
    ]
  }
  ```

  ```bash
  aws s3api put-bucket-cors \
    --bucket portal-universe-uploads-prod \
    --cors-configuration file://cors.json
  ```

- [ ] **버저닝 활성화**
  ```bash
  aws s3api put-bucket-versioning \
    --bucket portal-universe-uploads-prod \
    --versioning-configuration Status=Enabled
  ```

- [ ] **암호화 설정**
  ```bash
  aws s3api put-bucket-encryption \
    --bucket portal-universe-uploads-prod \
    --server-side-encryption-configuration '{
      "Rules": [{
        "ApplyServerSideEncryptionByDefault": {
          "SSEAlgorithm": "AES256"
        }
      }]
    }'
  ```

- [ ] **Public Access Block**
  ```bash
  aws s3api put-public-access-block \
    --bucket portal-universe-uploads-prod \
    --public-access-block-configuration \
      "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
  ```

### 4.2 라이프사이클 규칙

- [ ] **임시 파일 삭제 규칙**
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
          "Days": 7
        }
      }
    ]
  }
  ```

- [ ] **오래된 버전 정리**
  ```json
  {
    "Rules": [
      {
        "Id": "DeleteOldVersions",
        "Status": "Enabled",
        "NoncurrentVersionExpiration": {
          "NoncurrentDays": 90
        }
      }
    ]
  }
  ```

- [ ] **스토리지 클래스 전환**
  ```json
  {
    "Rules": [
      {
        "Id": "TransitionToIA",
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
      }
    ]
  }
  ```

### 4.3 IAM Role (EKS용)

- [ ] **OIDC Provider 생성**
  ```bash
  eksctl utils associate-iam-oidc-provider \
    --cluster=portal-universe-cluster \
    --approve
  ```

- [ ] **Service Account 역할 생성**
  ```bash
  eksctl create iamserviceaccount \
    --name blog-service-sa \
    --namespace default \
    --cluster portal-universe-cluster \
    --attach-policy-arn arn:aws:iam::123456789012:policy/BlogServiceS3Policy \
    --approve
  ```

---

## 5. Phase 3: 애플리케이션 설정

### 5.1 Spring Boot 설정

- [ ] **프로필 파일 생성**
  ```yaml
  # src/main/resources/application-production.yml
  aws:
    s3:
      bucket-name: portal-universe-uploads-prod
      region: ap-northeast-2
      # endpoint 제거 (실제 AWS 사용)

  spring:
    data:
      mongodb:
        uri: ${MONGODB_URI}
    kafka:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
  ```

- [ ] **S3 Configuration 업데이트**
  ```java
  @Configuration
  @Profile("production")
  public class AwsProductionConfig {

      @Value("${aws.s3.region}")
      private String region;

      @Bean
      public S3Client s3Client() {
          return S3Client.builder()
              .region(Region.of(region))
              .credentialsProvider(DefaultCredentialsProvider.create())
              .build();
      }
  }
  ```

- [ ] **환경 변수 확인**
  ```bash
  # 필요한 환경 변수 목록
  - SPRING_PROFILES_ACTIVE=production
  - AWS_REGION=ap-northeast-2
  - S3_BUCKET_NAME=portal-universe-uploads-prod
  - MONGODB_URI=mongodb://...
  - KAFKA_BOOTSTRAP_SERVERS=kafka:29092
  ```

### 5.2 Kubernetes 설정

- [ ] **ConfigMap 생성**
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

- [ ] **Secret 생성 (필요시)**
  ```yaml
  # k8s/blog-service/secret.yaml
  apiVersion: v1
  kind: Secret
  metadata:
    name: blog-service-secret
  type: Opaque
  stringData:
    MONGODB_PASSWORD: "secure-password"
    KAFKA_PASSWORD: "secure-password"
  ```

- [ ] **ServiceAccount 설정**
  ```yaml
  # k8s/blog-service/serviceaccount.yaml
  apiVersion: v1
  kind: ServiceAccount
  metadata:
    name: blog-service-sa
    annotations:
      eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/BlogServiceRole
  ```

- [ ] **Deployment 업데이트**
  ```yaml
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: blog-service
  spec:
    template:
      spec:
        serviceAccountName: blog-service-sa
        containers:
          - name: blog-service
            image: blog-service:1.0.0
            envFrom:
              - configMapRef:
                  name: blog-service-config
              - secretRef:
                  name: blog-service-secret
  ```

---

## 6. Phase 4: 보안 검토

### 6.1 S3 보안

- [ ] **Public Access 차단 확인**
  ```bash
  aws s3api get-public-access-block \
    --bucket portal-universe-uploads-prod
  ```

- [ ] **버킷 정책 최소 권한 확인**
  - ✅ 필요한 서비스만 접근 가능
  - ✅ 특정 리소스만 접근 가능
  - ✅ 특정 작업만 허용

- [ ] **암호화 활성화 확인**
  ```bash
  aws s3api get-bucket-encryption \
    --bucket portal-universe-uploads-prod
  ```

- [ ] **접근 로그 설정**
  ```bash
  aws s3api put-bucket-logging \
    --bucket portal-universe-uploads-prod \
    --bucket-logging-status '{
      "LoggingEnabled": {
        "TargetBucket": "portal-universe-logs",
        "TargetPrefix": "s3-access-logs/"
      }
    }'
  ```

### 6.2 IAM 보안

- [ ] **최소 권한 원칙 적용**
  - ✅ 필요한 작업만 허용
  - ✅ 필요한 리소스만 접근
  - ✅ 불필요한 권한 제거

- [ ] **IAM Policy Simulator 테스트**
  ```bash
  aws iam simulate-principal-policy \
    --policy-source-arn arn:aws:iam::123456789012:role/BlogServiceRole \
    --action-names s3:GetObject s3:PutObject \
    --resource-arns arn:aws:s3:::portal-universe-uploads-prod/*
  ```

- [ ] **Access Analyzer 활성화**
  ```bash
  aws accessanalyzer create-analyzer \
    --analyzer-name portal-universe-analyzer \
    --type ACCOUNT
  ```

### 6.3 네트워크 보안

- [ ] **VPC Endpoint 설정 (선택)**
  ```bash
  # S3 VPC Endpoint 생성
  aws ec2 create-vpc-endpoint \
    --vpc-id vpc-xxx \
    --service-name com.amazonaws.ap-northeast-2.s3 \
    --route-table-ids rtb-xxx
  ```

- [ ] **보안 그룹 검토**
  - ✅ 인바운드 규칙 최소화
  - ✅ 아웃바운드 규칙 필요한 것만 허용

---

## 7. Phase 5: 테스트 및 검증

### 7.1 연결 테스트

- [ ] **AWS CLI로 S3 접근 테스트**
  ```bash
  # 파일 업로드
  echo "test" > test.txt
  aws s3 cp test.txt s3://portal-universe-uploads-prod/test/

  # 파일 다운로드
  aws s3 cp s3://portal-universe-uploads-prod/test/test.txt ./downloaded.txt

  # 파일 삭제
  aws s3 rm s3://portal-universe-uploads-prod/test/test.txt
  ```

- [ ] **애플리케이션에서 S3 접근 테스트**
  ```bash
  # Pod 내부에서 테스트
  kubectl exec -it blog-service-xxx -- /bin/sh

  # AWS credentials 확인
  env | grep AWS

  # S3 접근 테스트 (Java)
  # 로그에서 S3 업로드/다운로드 확인
  ```

### 7.2 기능 테스트

- [ ] **파일 업로드 API 테스트**
  ```bash
  curl -X POST http://blog-service/api/v1/files/upload \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@sample.jpg"
  ```

- [ ] **파일 다운로드 테스트**
  ```bash
  curl http://blog-service/api/v1/files/download/sample.jpg \
    -H "Authorization: Bearer $TOKEN" \
    -o downloaded.jpg
  ```

- [ ] **파일 삭제 테스트**
  ```bash
  curl -X DELETE http://blog-service/api/v1/files/sample.jpg \
    -H "Authorization: Bearer $TOKEN"
  ```

### 7.3 성능 테스트

- [ ] **부하 테스트 (k6, JMeter)**
  ```javascript
  // k6-upload-test.js
  import http from 'k6/http';
  import { check } from 'k6';

  export let options = {
    vus: 10,
    duration: '30s',
  };

  export default function() {
    let file = open('./sample.jpg', 'b');
    let data = {
      file: http.file(file, 'sample.jpg'),
    };

    let res = http.post('http://blog-service/api/v1/files/upload', data);
    check(res, {
      'status is 200': (r) => r.status === 200,
    });
  }
  ```

  ```bash
  k6 run k6-upload-test.js
  ```

- [ ] **레이턴시 측정**
  - 업로드 레이턴시: < 500ms (1MB 파일)
  - 다운로드 레이턴시: < 200ms (1MB 파일)

---

## 8. Phase 6: 모니터링 설정

### 8.1 CloudWatch 메트릭

- [ ] **S3 메트릭 활성화**
  ```bash
  aws s3api put-bucket-metrics-configuration \
    --bucket portal-universe-uploads-prod \
    --id EntireBucket \
    --metrics-configuration '{
      "Id": "EntireBucket"
    }'
  ```

- [ ] **CloudWatch 알람 생성**
  ```bash
  # 4xx 오류 알람
  aws cloudwatch put-metric-alarm \
    --alarm-name s3-high-4xx-errors \
    --alarm-description "S3 4xx errors exceed threshold" \
    --metric-name 4xxErrors \
    --namespace AWS/S3 \
    --statistic Sum \
    --period 300 \
    --evaluation-periods 2 \
    --threshold 100 \
    --comparison-operator GreaterThanThreshold \
    --dimensions Name=BucketName,Value=portal-universe-uploads-prod

  # 5xx 오류 알람
  aws cloudwatch put-metric-alarm \
    --alarm-name s3-high-5xx-errors \
    --alarm-description "S3 5xx errors exceed threshold" \
    --metric-name 5xxErrors \
    --namespace AWS/S3 \
    --statistic Sum \
    --period 300 \
    --evaluation-periods 1 \
    --threshold 10 \
    --comparison-operator GreaterThanThreshold \
    --dimensions Name=BucketName,Value=portal-universe-uploads-prod
  ```

### 8.2 로그 수집

- [ ] **S3 Access Logging**
  - ✅ 로그 버킷 생성
  - ✅ Logging 활성화
  - ✅ 로그 분석 쿼리 준비

- [ ] **CloudTrail 활성화**
  ```bash
  aws cloudtrail create-trail \
    --name portal-universe-trail \
    --s3-bucket-name portal-universe-cloudtrail

  aws cloudtrail start-logging \
    --name portal-universe-trail
  ```

### 8.3 비용 모니터링

- [ ] **Cost Explorer 활성화**
  ```
  AWS Console → Cost Management → Cost Explorer
  → Enable Cost Explorer
  ```

- [ ] **비용 알림 설정**
  ```
  Budgets → Create budget

  Budget type: Cost budget
  Budget amount: $100/month
  Alert threshold: 80%, 90%, 100%
  Email: admin@example.com
  ```

- [ ] **태그 기반 비용 추적**
  ```yaml
  tags:
    Service: blog-service
    Environment: production
    CostCenter: engineering
  ```

---

## 9. 롤백 계획

### 9.1 롤백 시나리오

- [ ] **LocalStack으로 롤백 절차 문서화**
  ```yaml
  # Rollback Steps:
  1. ConfigMap 업데이트
     - SPRING_PROFILES_ACTIVE: kubernetes (LocalStack)

  2. Deployment 재배포
     kubectl rollout restart deployment/blog-service

  3. 확인
     kubectl logs -f deployment/blog-service
  ```

- [ ] **데이터 백업 확인**
  ```bash
  # S3에서 LocalStack으로 데이터 복사
  aws s3 sync s3://portal-universe-uploads-prod/ ./backup/

  # LocalStack으로 복원
  aws --endpoint-url=http://localhost:4566 \
    s3 sync ./backup/ s3://blog-bucket/
  ```

### 9.2 장애 대응

- [ ] **주요 장애 시나리오 문서화**

  | 시나리오 | 증상 | 해결 방법 |
  |----------|------|----------|
  | IAM 권한 오류 | 403 Forbidden | IAM Policy 검토, Role 확인 |
  | 네트워크 오류 | Timeout | 보안 그룹, VPC 엔드포인트 확인 |
  | 버킷 미존재 | 404 Not Found | 버킷 이름, 리전 확인 |
  | 비용 초과 | 예산 알림 | 라이프사이클 규칙, 불필요한 데이터 삭제 |

---

## 10. 최종 체크리스트

### 배포 전 최종 확인

- [ ] S3 버킷 생성 및 설정 완료
- [ ] IAM Role 및 Policy 설정 완료
- [ ] 애플리케이션 설정 변경 완료
- [ ] Kubernetes manifest 업데이트 완료
- [ ] 모든 테스트 통과
- [ ] 모니터링 및 알림 설정 완료
- [ ] 롤백 계획 수립 완료
- [ ] 팀 공유 및 승인 완료

### 배포 후 확인

- [ ] 애플리케이션 정상 작동 확인
- [ ] S3 접근 로그 확인
- [ ] CloudWatch 메트릭 확인
- [ ] 비용 발생 모니터링
- [ ] 사용자 피드백 수집

---

## 11. 핵심 요약

### 마이그레이션 우선순위

1. **보안 우선**: IAM 최소 권한, Public Access 차단
2. **비용 최적화**: 라이프사이클 규칙, 스토리지 클래스 선택
3. **모니터링**: CloudWatch, 비용 알림 필수
4. **롤백 계획**: 문제 발생 시 즉시 복구 가능

### 주의사항

⚠️ **비용 폭탄 방지**
- 라이프사이클 규칙 설정 필수
- 비용 알림 설정 필수
- 불필요한 데이터 정기 삭제

⚠️ **보안**
- Public Access Block 필수
- IAM 최소 권한 원칙
- 암호화 활성화

⚠️ **가용성**
- 버저닝 활성화
- 백업 계획 수립
- 롤백 절차 준비

---

## 12. 관련 문서

- [Environment Profiles](../deployment/environment-profiles.md) - 환경별 프로필 설정
- [Local to Kubernetes](../deployment/local-to-kubernetes.md) - K8s 전환
- [Kubernetes to AWS](../deployment/kubernetes-to-aws.md) - AWS 전환 상세
- [S3 Fundamentals](../s3/s3-fundamentals.md) - S3 기초
- [IAM Best Practices](../iam/iam-best-practices.md) - IAM 보안
