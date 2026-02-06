# AWS 학습 가이드

## 개요

이 가이드는 **LocalStack을 활용한 로컬 AWS 개발**을 중심으로 AWS 서비스를 학습하는 초보자를 위한 자료입니다.

실제 AWS 클라우드 계정 없이도 로컬 환경에서 AWS 서비스를 실습하고, Portal Universe 프로젝트에서 어떻게 활용되는지 이해할 수 있습니다.

---

## Portal Universe에서의 AWS 활용

| 서비스 | 용도 | 담당 서비스 |
|--------|------|------------|
| **S3** | 파일 업로드/저장 | Blog Service |
| **EKS** | Kubernetes 관리 | 전체 (프로덕션) |
| **EC2** | EKS 워커 노드 | 전체 (프로덕션) |
| **CloudWatch** | 로그/메트릭 수집 | 전체 (향후) |
| **SQS** | 메시지 큐 | 전체 (향후) |

현재 Blog Service에서 S3를 사용하여 블로그 게시물의 첨부 파일을 저장하고 있으며, 프로덕션 환경에서는 EKS를 통해 Kubernetes 클러스터를 운영합니다.

```
로컬 개발:
┌─────────────────────────────────────┐
│  Kind (Kubernetes in Docker)        │
│  ├─ Blog Service                    │
│  ├─ Shopping Service                │
│  └─ LocalStack S3                   │
└─────────────────────────────────────┘

프로덕션:
┌─────────────────────────────────────┐
│  EKS Cluster (AWS)                  │
│  ├─ EC2 워커 노드                    │
│  │  ├─ Blog Service (Pod)           │
│  │  └─ Shopping Service (Pod)       │
│  └─ AWS S3                          │
└─────────────────────────────────────┘
```

---

## 학습 목적

1. **로컬 개발 환경 구축**: LocalStack으로 AWS 서비스 에뮬레이션
2. **클라우드 기초 이해**: AWS 핵심 개념 습득
3. **실무 적용**: Portal Universe 프로젝트에서 사용되는 패턴 학습
4. **비용 효율성**: 실제 AWS 계정 없이 학습 가능

---

## 권장 학습 순서

### 1단계: AWS 기초 (필수)

| 순서 | 문서 | 내용 | 소요 시간 |
|------|------|------|-----------|
| 1 | [AWS 개요](./fundamentals/aws-overview.md) | 클라우드 컴퓨팅과 AWS 핵심 서비스 | 30분 |
| 2 | [리전과 가용영역](./fundamentals/region-az.md) | AWS 글로벌 인프라 이해 | 20분 |
| 3 | [AWS CLI 설정](./fundamentals/aws-cli-setup.md) | CLI 설치 및 프로필 설정 | 30분 |

### 2단계: 주요 서비스 (선택)

| 순서 | 문서 | 내용 | 소요 시간 |
|------|------|------|-----------|
| 4 | [IAM 기초](./iam/iam-fundamentals.md) | 인증과 권한 관리 | 40분 |
| 5 | [S3 소개](./s3/s3-introduction.md) | S3 개념과 구조 이해 | 30분 |
| 6 | [S3 CRUD 연산](./s3/s3-operations.md) | AWS CLI로 S3 조작 | 40분 |
| 7 | [Spring Boot S3 SDK 통합](./s3/s3-sdk-integration.md) | Blog Service S3 통합 | 50분 |
| 8 | [S3 권한 설정](./s3/s3-permissions.md) | 버킷 정책과 CORS | 40분 |
| 9 | [S3 모범 사례](./s3/s3-best-practices.md) | 네이밍, 라이프사이클, 비용 최적화 | 40분 |

### 3단계: LocalStack 활용 (필수) ⭐

| 순서 | 문서 | 내용 | 소요 시간 |
|------|------|------|-----------|
| 10 | [LocalStack Setup](./localstack/localstack-setup.md) | LocalStack 설치 및 환경 설정 | 30분 |
| 11 | [LocalStack Persistence](./localstack/localstack-persistence.md) | 데이터 영속성 문제 해결 (핵심!) | 40분 |
| 12 | [LocalStack Services](./localstack/localstack-services.md) | 지원 서비스별 가이드 | 30분 |
| 13 | [LocalStack Troubleshooting](./localstack/localstack-troubleshooting.md) | 트러블슈팅 가이드 | 40분 |

### 4단계: EC2와 컨테이너 (선택)

| 순서 | 문서 | 내용 | 소요 시간 |
|------|------|------|-----------|
| 14 | [EC2 소개](./ec2/ec2-introduction.md) | 가상 서버 개념 이해 | 40분 |
| 15 | [EC2 vs Kubernetes](./ec2/ec2-vs-kubernetes.md) | 컨테이너 서비스 비교 | 30분 |

### 5단계: 배포 (고급)

| 순서 | 문서 | 내용 | 소요 시간 |
|------|------|------|-----------|
| 16 | [AWS 배포 전략](./deployment/deployment-strategies.md) | 배포 방법 비교 | 30분 |

---

## 전제 조건

### 필수 도구

```bash
# Docker (LocalStack 실행용)
docker --version
# Docker version 20.10.0 이상

# awslocal CLI (LocalStack용 AWS CLI)
awslocal --version
# aws-cli/2.x.x

# AWS CLI (선택: 실제 AWS 사용 시)
aws --version
# aws-cli/2.x.x
```

### 필수 지식

- 기본 리눅스 명령어 (ls, cd, cat 등)
- HTTP/REST API 개념
- Docker 기본 사용법

---

## 실습 환경

### LocalStack 시작

```bash
# Portal Universe 루트 디렉토리에서
cd docker-localstack
docker-compose up -d

# LocalStack 상태 확인
docker-compose ps

# S3 버킷 리스트 확인
awslocal s3 ls
```

### LocalStack 엔드포인트

| 서비스 | 엔드포인트 |
|--------|-----------|
| S3 | http://localhost:4566 |
| SQS | http://localhost:4566 |
| SNS | http://localhost:4566 |
| CloudWatch | http://localhost:4566 |

모든 서비스가 **단일 포트 4566**을 사용합니다.

---

## 학습 리소스

### 공식 문서

- [AWS 공식 문서](https://docs.aws.amazon.com/)
- [LocalStack 공식 문서](https://docs.localstack.cloud/)
- [AWS CLI 명령 참조](https://awscli.amazonaws.com/v2/documentation/api/latest/index.html)

### Portal Universe 관련

- [LocalStack S3 설정](../../infra/localstack-s3.md)
- [Blog Service 아키텍처](../../../architecture/blog-service.md)
- [파일 업로드 API](../../../api/blog-file-upload.md)

### 외부 자료

- [AWS 10분 튜토리얼](https://aws.amazon.com/getting-started/hands-on/)
- [LocalStack 튜토리얼](https://docs.localstack.cloud/tutorials/)

---

## 실습 예제

### 빠른 시작: LocalStack S3 사용

```bash
# 1. LocalStack 시작
cd docker-localstack && docker-compose up -d

# 2. S3 버킷 생성
awslocal s3 mb s3://my-test-bucket

# 3. 파일 업로드
echo "Hello LocalStack!" > test.txt
awslocal s3 cp test.txt s3://my-test-bucket/

# 4. 파일 목록 확인
awslocal s3 ls s3://my-test-bucket/

# 5. 파일 다운로드
awslocal s3 cp s3://my-test-bucket/test.txt downloaded.txt
cat downloaded.txt
```

---

## 자주 묻는 질문

### Q1. LocalStack과 실제 AWS의 차이는?

LocalStack은 AWS API를 **에뮬레이션**하므로 대부분의 기능은 동일하지만 일부 고급 기능은 지원하지 않을 수 있습니다.

| 항목 | LocalStack | AWS |
|------|-----------|-----|
| **비용** | 무료 | 사용량 기반 과금 |
| **네트워크** | 로컬호스트 | 인터넷 |
| **성능** | 로컬 머신 의존 | 글로벌 인프라 |
| **기능** | 주요 기능 지원 | 모든 기능 |

### Q2. 실제 AWS 계정이 필요한가요?

**로컬 개발은 불필요**합니다. LocalStack만으로 충분히 학습할 수 있습니다.

프로덕션 배포나 고급 기능 테스트 시에만 실제 AWS 계정이 필요합니다.

### Q3. 어떤 순서로 학습해야 하나요?

1. **fundamentals** (필수): AWS 기초 개념
2. **s3**: Blog Service에서 사용 중
3. **iam**: 권한 관리 이해
4. **localstack**: 로컬 개발 환경
5. **ec2**: EC2와 Kubernetes 비교 (선택)
6. **deployment**: 배포 방법 (선택)

### Q4. Portal Universe는 왜 EC2 대신 Kubernetes를 사용하나요?

**로컬 개발 환경의 일관성**과 **클라우드 벤더 독립성** 때문입니다:

- Kind로 로컬에서 Kubernetes 클러스터 실행 가능
- AWS, GCP, Azure 모두에서 동일하게 작동
- 마이크로서비스 아키텍처에 최적화
- 자동 스케일링 및 자가 치유

자세한 내용은 [EC2 vs Kubernetes](./ec2/ec2-vs-kubernetes.md)를 참고하세요.

---

## 문서 구조

```
aws/
├── README.md                          # 이 파일
├── fundamentals/                      # AWS 기초
│   ├── aws-overview.md                # AWS 개요
│   ├── region-az.md                   # 리전과 가용영역
│   └── aws-cli-setup.md               # CLI 설정
├── iam/                               # 인증/권한
│   ├── iam-fundamentals.md
│   └── iam-best-practices.md
├── s3/                                # 객체 스토리지
│   ├── s3-introduction.md             # S3 소개
│   ├── s3-operations.md               # S3 CRUD 연산
│   ├── s3-sdk-integration.md          # Spring Boot 통합
│   ├── s3-permissions.md              # 권한 설정
│   └── s3-best-practices.md           # 모범 사례
├── localstack/                        # 로컬 개발
│   ├── localstack-setup.md            # LocalStack 설치 및 설정
│   ├── localstack-persistence.md      # 데이터 영속성 (핵심!)
│   ├── localstack-services.md         # 지원 서비스 가이드
│   └── localstack-troubleshooting.md  # 트러블슈팅 가이드
├── ec2/                               # 가상 서버
│   ├── ec2-introduction.md            # EC2 개요
│   └── ec2-vs-kubernetes.md           # 컨테이너 서비스 비교
├── deployment/                        # 배포 전략 (향후)
└── best-practices/                    # 모범 사례 (향후)
```

---

## 기여 가이드

이 문서는 Portal Universe 팀원들이 함께 작성하고 개선합니다.

### 문서 추가 시

1. 적절한 카테고리 디렉토리 선택
2. 기존 문서 구조 따르기 (학습 목표 → 개념 설명 → 실습 → 정리)
3. Portal Universe 적용 예시 포함
4. 이 README에 링크 추가

### 문서 개선 제안

- 이해하기 어려운 부분
- 추가 필요한 예제
- 오타나 오류

위 사항은 GitHub Issue로 등록해주세요.

---

## 다음 단계

### 빠른 시작 (LocalStack 문제 해결)
Portal Universe 프로젝트에서 "서버 껐다키면 저장이 안됨" 문제를 겪고 있다면:
1. 👉 [LocalStack Persistence](./localstack/localstack-persistence.md) - 데이터 영속성 문제 해결 ⭐ (핵심!)
2. [LocalStack Troubleshooting](./localstack/localstack-troubleshooting.md) - 트러블슈팅 가이드

### 체계적 학습
1. [AWS 개요](./fundamentals/aws-overview.md)부터 시작하세요
2. [LocalStack Setup](./localstack/localstack-setup.md)으로 로컬 환경 구축
3. 각 문서의 "실습 예제"를 직접 따라해보세요
4. Blog Service 코드에서 S3 사용 부분을 찾아보세요

```bash
# Blog Service S3 관련 코드 확인
cd services/blog-service
grep -r "S3Client" src/
grep -r "@PostConstruct" src/  # 버킷 자동 생성 로직 확인
```

**Happy Learning! ☁️**
