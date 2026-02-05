# 리전과 가용영역

## 학습 목표

- AWS 글로벌 인프라 구조 이해
- Region(리전)과 Availability Zone(가용영역)의 차이 파악
- 서울 리전(ap-northeast-2)의 특징 이해
- LocalStack에서 리전 설정의 의미 파악

---

## 1. AWS 글로벌 인프라 개요

AWS는 전 세계에 **물리적으로 분산된 데이터센터**를 운영하며, 이를 계층적으로 조직화합니다.

```
AWS Global Infrastructure
│
├── Region (리전) - 지리적 영역
│   ├── Availability Zone A (가용영역 A) - 데이터센터 클러스터
│   ├── Availability Zone B (가용영역 B)
│   └── Availability Zone C (가용영역 C)
│
└── Edge Location (엣지 로케이션) - CDN 캐시 서버
```

### 주요 개념

| 개념 | 설명 | 예시 |
|------|------|------|
| **Region** | 독립적인 지리적 영역 | 서울, 도쿄, 버지니아 |
| **Availability Zone** | Region 내 격리된 데이터센터 | ap-northeast-2a, 2b, 2c |
| **Edge Location** | CloudFront CDN 캐시 서버 | 전 세계 400+ 지점 |

---

## 2. Region (리전)

### 2.1 리전이란?

**지리적으로 격리된 영역**으로, 각각 독립적인 전력, 냉각, 네트워크를 가집니다.

```
🌏 아시아-태평양
├── ap-northeast-2 (서울)
├── ap-northeast-1 (도쿄)
├── ap-southeast-1 (싱가포르)
└── ap-south-1 (뭄바이)

🌎 북미
├── us-east-1 (버지니아 북부)
├── us-west-2 (오레곤)
└── ca-central-1 (캐나다)

🌍 유럽
├── eu-west-1 (아일랜드)
├── eu-central-1 (프랑크푸르트)
└── eu-north-1 (스톡홀름)
```

### 2.2 리전의 특징

| 특징 | 설명 |
|------|------|
| **완전 격리** | 한 리전의 장애가 다른 리전에 영향 없음 |
| **데이터 주권** | 데이터는 선택한 리전 밖으로 나가지 않음 |
| **지연시간 최적화** | 사용자와 가까운 리전 선택으로 성능 향상 |
| **법규 준수** | 국가별 데이터 보관 규정 준수 |

### 2.3 리전 선택 기준

```
리전 선택 시 고려사항:

1. 지연시간 (Latency)
   └─► 사용자와 가까운 리전 선택
       예: 한국 사용자 → ap-northeast-2 (서울)

2. 비용 (Cost)
   └─► 리전별 요금 차이 존재
       예: us-east-1이 일반적으로 가장 저렴

3. 서비스 가용성
   └─► 새 서비스는 특정 리전에서만 제공
       예: 최신 AI 서비스는 us-east-1에 먼저 출시

4. 법규 준수 (Compliance)
   └─► 데이터 보관 위치 규정
       예: 금융 데이터는 한국 내 보관 필수
```

### 2.4 글로벌 리전 현황 (2024년)

```
전체: 33개 리전
├── 운영 중: 33개
└── 개설 예정: 4개

AZ(가용영역): 105개
Edge Location: 400+ 개
```

---

## 3. Availability Zone (가용영역)

### 3.1 가용영역이란?

리전 내에 있는 **물리적으로 분리된 데이터센터 클러스터**입니다.

```
ap-northeast-2 (서울 리전)
┌───────────────────────────────────────────┐
│                                           │
│  ┌──────────────┐  ┌──────────────┐      │
│  │ AZ-A (2a)    │  │ AZ-B (2b)    │      │
│  │ 독립 전력    │  │ 독립 전력    │      │
│  │ 독립 냉각    │  │ 독립 냉각    │      │
│  │ 독립 네트워크│  │ 독립 네트워크│      │
│  └──────────────┘  └──────────────┘      │
│                                           │
│         ┌──────────────┐                  │
│         │ AZ-C (2c)    │                  │
│         │ 독립 전력    │                  │
│         │ 독립 냉각    │                  │
│         │ 독립 네트워크│                  │
│         └──────────────┘                  │
│                                           │
└───────────────────────────────────────────┘
      고속 프라이빗 네트워크로 연결
         (지연시간 < 2ms)
```

### 3.2 가용영역의 특징

| 특징 | 설명 |
|------|------|
| **물리적 격리** | 화재, 정전 등 한 AZ 장애가 다른 AZ에 영향 없음 |
| **고속 연결** | AZ 간 지연시간 < 2ms (프라이빗 네트워크) |
| **높은 가용성** | 여러 AZ에 리소스 배치로 99.99% 가용성 달성 |
| **자동 복제** | RDS Multi-AZ는 자동으로 다른 AZ에 복제 |

### 3.3 Multi-AZ 아키텍처

```
Single-AZ (가용성 낮음)
┌─────────────────┐
│   AZ-A          │
│  ┌───────┐      │
│  │Web Srv│      │
│  └───────┘      │
│  ┌───────┐      │
│  │  DB   │      │
│  └───────┘      │
└─────────────────┘
❌ AZ 장애 시 전체 서비스 중단


Multi-AZ (고가용성)
┌─────────────┐    ┌─────────────┐
│   AZ-A      │    │   AZ-B      │
│  ┌───────┐  │    │  ┌───────┐  │
│  │Web Srv│  │    │  │Web Srv│  │
│  └───────┘  │    │  └───────┘  │
│  ┌───────┐  │    │  ┌───────┐  │
│  │DB(주) │──┼────┼─►│DB(대기)│  │
│  └───────┘  │    │  └───────┘  │
└─────────────┘    └─────────────┘
✓ 한 AZ 장애 시에도 서비스 계속 운영
```

---

## 4. 서울 리전 (ap-northeast-2)

### 4.1 기본 정보

| 항목 | 정보 |
|------|------|
| **리전 코드** | `ap-northeast-2` |
| **위치** | 대한민국 서울 |
| **개설 시기** | 2016년 1월 |
| **가용영역 수** | 4개 (2a, 2b, 2c, 2d) |
| **지연시간** | 서울 ↔ 도쿄: ~30ms |

### 4.2 서울 리전 사용 이유

```
✓ 최저 지연시간
  └─► 한국 사용자에게 5~20ms 응답 시간

✓ 데이터 주권
  └─► 개인정보보호법, 금융 규정 준수

✓ 안정성
  └─► 4개 AZ로 높은 가용성

✓ 서비스 지원
  └─► 대부분의 AWS 서비스 사용 가능
```

### 4.3 Portal Universe 설정

```yaml
# application-k8s.yml (프로덕션 배포 시)
aws:
  region: ap-northeast-2  # 서울 리전
  s3:
    bucket: portal-blog-files-prod
    endpoint: https://s3.ap-northeast-2.amazonaws.com
```

```java
// S3Config.java
@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.AP_NORTHEAST_2)  // 서울 리전
            .build();
    }
}
```

---

## 5. LocalStack에서의 리전

### 5.1 LocalStack 리전 동작

LocalStack은 **실제 리전이 아닌 에뮬레이션**을 제공합니다.

```
실제 AWS:
ap-northeast-2 → 서울 물리적 데이터센터

LocalStack:
ap-northeast-2 → 로컬 컴퓨터의 Docker 컨테이너
                (실제 서울과 무관)
```

### 5.2 LocalStack 리전 설정

```yaml
# docker-compose.yml
services:
  localstack:
    image: localstack/localstack
    environment:
      - DEFAULT_REGION=ap-northeast-2  # 기본 리전 설정
      - SERVICES=s3,sqs,sns
    ports:
      - "4566:4566"
```

### 5.3 왜 LocalStack에서도 리전을 지정하나?

```
이유:
1. AWS SDK 호환성
   └─► AWS SDK는 리전을 필수로 요구

2. 프로덕션 일관성
   └─► 로컬과 프로덕션 설정을 동일하게 유지

3. 다중 리전 테스트
   └─► 리전 간 복제 시나리오 테스트 가능
```

---

## 6. Portal Universe 적용

### 6.1 로컬 개발 환경

```bash
# LocalStack S3 - 서울 리전으로 설정
awslocal s3 mb s3://portal-blog-files --region ap-northeast-2

# 리전 확인
awslocal s3api get-bucket-location --bucket portal-blog-files
# {
#     "LocationConstraint": "ap-northeast-2"
# }
```

### 6.2 환경별 리전 설정

```yaml
# application-local.yml
aws:
  region: ap-northeast-2
  s3:
    endpoint: http://localhost:4566  # LocalStack

# application-k8s.yml (프로덕션)
aws:
  region: ap-northeast-2
  s3:
    endpoint: https://s3.ap-northeast-2.amazonaws.com  # 실제 AWS
```

### 6.3 Multi-AZ 배포 (향후)

```yaml
# Kubernetes Deployment - Multi-AZ
apiVersion: apps/v1
kind: Deployment
metadata:
  name: blog-service
spec:
  replicas: 3
  template:
    spec:
      affinity:
        podAntiAffinity:  # Pod를 다른 AZ에 분산
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchLabels:
                app: blog-service
            topologyKey: topology.kubernetes.io/zone
      containers:
      - name: blog-service
        image: blog-service:1.0.0
```

---

## 7. 실습 예제

### 실습 1: LocalStack에서 리전 확인

```bash
# 1. LocalStack 시작
cd docker-localstack
docker-compose up -d

# 2. 기본 리전 확인
awslocal configure get region
# ap-northeast-2

# 3. 특정 리전으로 버킷 생성
awslocal s3 mb s3://seoul-bucket --region ap-northeast-2
awslocal s3 mb s3://tokyo-bucket --region ap-northeast-1

# 4. 버킷 위치 확인
awslocal s3api get-bucket-location --bucket seoul-bucket
awslocal s3api get-bucket-location --bucket tokyo-bucket

# 5. 리전별 버킷 목록
awslocal s3 ls --region ap-northeast-2
```

### 실습 2: Multi-AZ 개념 시뮬레이션

```bash
# 실제 AWS에서 RDS Multi-AZ 생성 시 (예시)
aws rds create-db-instance \
  --db-instance-identifier portal-universe-db \
  --db-instance-class db.t3.medium \
  --engine mysql \
  --master-username admin \
  --master-user-password secret \
  --allocated-storage 20 \
  --multi-az \  # Multi-AZ 활성화
  --region ap-northeast-2

# Primary: ap-northeast-2a
# Standby: ap-northeast-2b (자동 복제)
```

---

## 8. 리전 간 통신

### 8.1 Cross-Region 복제

```
서울 리전 (Primary)          도쿄 리전 (DR)
┌─────────────────┐         ┌─────────────────┐
│ ap-northeast-2  │         │ ap-northeast-1  │
│                 │         │                 │
│  S3 Bucket      │─────────►  S3 Bucket      │
│  portal-blog    │  복제   │  portal-blog-dr │
│                 │         │                 │
└─────────────────┘         └─────────────────┘
```

```bash
# S3 Cross-Region Replication 설정 (실제 AWS)
aws s3api put-bucket-replication \
  --bucket portal-blog-files \
  --replication-configuration '{
    "Role": "arn:aws:iam::ACCOUNT_ID:role/s3-replication",
    "Rules": [{
      "Status": "Enabled",
      "Priority": 1,
      "Destination": {
        "Bucket": "arn:aws:s3:::portal-blog-files-tokyo",
        "ReplicationTime": {
          "Status": "Enabled",
          "Time": {
            "Minutes": 15
          }
        }
      }
    }]
  }'
```

### 8.2 지연시간 비교

| 경로 | 지연시간 | 용도 |
|------|----------|------|
| 서울 리전 내 (AZ 간) | < 2ms | 일반적 통신 |
| 서울 ↔ 도쿄 | ~30ms | DR, 글로벌 서비스 |
| 서울 ↔ 싱가포르 | ~50ms | 동남아 서비스 |
| 서울 ↔ 버지니아 | ~180ms | 미국 서비스 |

---

## 9. 핵심 정리

| 개념 | 설명 | 예시 |
|------|------|------|
| **Region** | 지리적으로 격리된 영역 | ap-northeast-2 (서울) |
| **Availability Zone** | Region 내 격리된 데이터센터 | 2a, 2b, 2c, 2d |
| **Multi-AZ** | 여러 AZ에 리소스 배치로 고가용성 | RDS Multi-AZ |
| **Edge Location** | CloudFront CDN 캐시 서버 | 전 세계 400+ |
| **서울 리전** | ap-northeast-2, 4개 AZ | Portal Universe 사용 |

### 고가용성 원칙

```
✓ 여러 AZ에 리소스 분산
✓ 자동 장애 조치 (Failover) 설정
✓ 정기적인 백업 및 DR 계획
✓ 리전 간 복제 고려 (재해 복구)
```

---

## 10. 다음 학습

- [AWS CLI 설정](./aws-cli-setup.md) - CLI로 리전 설정 및 사용
- [IAM 기초](../iam/iam-fundamentals.md) - 리전별 권한 관리
- [S3 기초](../s3/s3-fundamentals.md) - 리전별 버킷 생성
- [고가용성 아키텍처](../best-practices/high-availability.md) - Multi-AZ 설계

---

## 참고 자료

- [AWS 리전 및 가용영역](https://aws.amazon.com/about-aws/global-infrastructure/regions_az/)
- [AWS 글로벌 인프라 맵](https://aws.amazon.com/about-aws/global-infrastructure/)
- [서울 리전 소개 (AWS 블로그)](https://aws.amazon.com/ko/blogs/korea/now-open-aws-asia-pacific-seoul-region/)
- [Multi-AZ 배포 모범 사례](https://docs.aws.amazon.com/whitepapers/latest/real-time-communication-on-aws/high-availability-and-scalability-on-aws.html)
