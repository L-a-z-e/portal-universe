# AWS CLI 설정

## 학습 목표

- AWS CLI와 awslocal CLI의 차이점 이해
- AWS CLI 설치 및 프로필 설정 방법 습득
- LocalStack 개발을 위한 awslocal CLI 활용
- 주요 AWS CLI 명령어 실습

---

## 1. AWS CLI란?

**AWS Command Line Interface**는 터미널에서 AWS 서비스를 관리할 수 있는 통합 도구입니다.

### 웹 콘솔 vs CLI

```
[AWS Management Console (웹)]
✓ 시각적 인터페이스
✓ 초보자 친화적
✗ 반복 작업 비효율적
✗ 자동화 어려움


[AWS CLI (터미널)]
✓ 빠른 작업 실행
✓ 스크립트 자동화 가능
✓ CI/CD 파이프라인 통합
✗ 학습 곡선 존재
```

### CLI의 장점

| 장점 | 설명 | 예시 |
|------|------|------|
| **자동화** | 스크립트로 반복 작업 처리 | 매일 백업 스크립트 |
| **속도** | 명령어 한 줄로 작업 완료 | 버킷 생성 1초 |
| **CI/CD** | 배포 파이프라인에 통합 | GitHub Actions |
| **정확성** | 오타나 클릭 실수 방지 | 코드 리뷰 가능 |

---

## 2. AWS CLI vs awslocal CLI

Portal Universe에서는 두 가지 CLI를 사용합니다.

| CLI | 용도 | 엔드포인트 | 설치 |
|-----|------|-----------|------|
| **aws** | 실제 AWS 서비스 | aws.amazon.com | AWS 공식 |
| **awslocal** | LocalStack (로컬) | localhost:4566 | LocalStack |

### awslocal이란?

**awslocal**은 AWS CLI의 래퍼(wrapper)로, 모든 요청을 LocalStack으로 자동 라우팅합니다.

```bash
# AWS CLI (실제 AWS)
aws s3 ls --endpoint-url http://localhost:4566

# awslocal (자동으로 LocalStack 사용)
awslocal s3 ls
```

---

## 3. AWS CLI 설치

### 3.1 macOS

```bash
# Homebrew 사용 (권장)
brew install awscli

# 직접 설치
curl "https://awscli.amazonaws.com/AWSCLIV2.pkg" -o "AWSCLIV2.pkg"
sudo installer -pkg AWSCLIV2.pkg -target /

# 설치 확인
aws --version
# aws-cli/2.15.0 Python/3.11.6 Darwin/23.3.0 source/arm64
```

### 3.2 Linux

```bash
# 최신 버전 다운로드
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"

# 압축 해제
unzip awscliv2.zip

# 설치
sudo ./aws/install

# 설치 확인
aws --version
```

### 3.3 Windows

```powershell
# MSI 인스톨러 다운로드 후 실행
# https://awscli.amazonaws.com/AWSCLIV2.msi

# PowerShell에서 확인
aws --version
```

---

## 4. awslocal CLI 설치

### 4.1 pip 사용 (권장)

```bash
# Python pip로 설치
pip install awscli-local

# 설치 확인
awslocal --version
# aws-cli/2.15.0 Python/3.11.6
```

### 4.2 대안: Bash 래퍼

```bash
# ~/.bashrc 또는 ~/.zshrc에 추가
alias awslocal='aws --endpoint-url http://localhost:4566'

# 적용
source ~/.zshrc
```

---

## 5. AWS 프로필 설정

### 5.1 자격 증명 구조

AWS CLI는 두 개의 설정 파일을 사용합니다.

```
~/.aws/
├── credentials  # Access Key (보안 자격 증명)
└── config       # 리전, 출력 형식 등 설정
```

### 5.2 credentials 파일

```ini
# ~/.aws/credentials

[default]
aws_access_key_id = AKIAIOSFODNN7EXAMPLE
aws_secret_access_key = wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

[localstack]
aws_access_key_id = test
aws_secret_access_key = test

[production]
aws_access_key_id = AKIA...REAL_KEY
aws_secret_access_key = real...secret
```

### 5.3 config 파일

```ini
# ~/.aws/config

[default]
region = ap-northeast-2
output = json

[profile localstack]
region = ap-northeast-2
output = json

[profile production]
region = ap-northeast-2
output = json
```

### 5.4 프로필 사용

```bash
# default 프로필 사용
aws s3 ls

# 특정 프로필 사용
aws s3 ls --profile production

# 환경 변수로 프로필 지정
export AWS_PROFILE=production
aws s3 ls
```

---

## 6. LocalStack 설정

### 6.1 LocalStack용 자격 증명

LocalStack은 **임의의 자격 증명**을 허용합니다.

```bash
# awslocal 사용 시 자동 설정
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=ap-northeast-2
```

### 6.2 Portal Universe 설정

```bash
# .env 또는 .zshrc에 추가
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=ap-northeast-2
export AWS_ENDPOINT_URL=http://localhost:4566
```

---

## 7. 기본 AWS CLI 명령어

### 7.1 S3 명령어

```bash
# 버킷 목록 조회
awslocal s3 ls

# 버킷 생성 (mb = make bucket)
awslocal s3 mb s3://my-bucket

# 파일 업로드
awslocal s3 cp file.txt s3://my-bucket/

# 파일 다운로드
awslocal s3 cp s3://my-bucket/file.txt downloaded.txt

# 버킷 내용 확인
awslocal s3 ls s3://my-bucket/

# 버킷 삭제 (rb = remove bucket)
awslocal s3 rb s3://my-bucket --force

# 동기화 (로컬 ↔ S3)
awslocal s3 sync ./local-folder s3://my-bucket/
```

### 7.2 S3 API 명령어 (s3api)

더 세밀한 제어가 필요할 때 사용합니다.

```bash
# 버킷 생성 (리전 지정)
awslocal s3api create-bucket \
  --bucket my-bucket \
  --region ap-northeast-2 \
  --create-bucket-configuration LocationConstraint=ap-northeast-2

# 버킷 위치 확인
awslocal s3api get-bucket-location --bucket my-bucket

# 객체 메타데이터 확인
awslocal s3api head-object \
  --bucket my-bucket \
  --key file.txt

# 버킷 정책 설정 (퍼블릭 읽기)
awslocal s3api put-bucket-policy \
  --bucket my-bucket \
  --policy '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::my-bucket/*"
    }]
  }'
```

### 7.3 SQS 명령어 (향후 사용)

```bash
# 큐 생성
awslocal sqs create-queue --queue-name my-queue

# 큐 목록
awslocal sqs list-queues

# 메시지 전송
awslocal sqs send-message \
  --queue-url http://localhost:4566/000000000000/my-queue \
  --message-body "Hello from CLI"

# 메시지 수신
awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/my-queue
```

---

## 8. Portal Universe 적용

### 8.1 Blog Service S3 초기 설정

```bash
#!/bin/bash
# scripts/setup-localstack-s3.sh

echo "🚀 LocalStack S3 초기 설정 시작..."

# LocalStack 실행 확인
if ! curl -s http://localhost:4566/_localstack/health > /dev/null; then
  echo "❌ LocalStack이 실행되지 않았습니다."
  echo "💡 'cd docker-localstack && docker-compose up -d' 실행"
  exit 1
fi

# S3 버킷 생성
echo "📦 S3 버킷 생성 중..."
awslocal s3 mb s3://portal-blog-files

# 버킷 정책 설정 (로컬 개발용)
awslocal s3api put-bucket-acl \
  --bucket portal-blog-files \
  --acl public-read

# 테스트 파일 업로드
echo "Test file for LocalStack S3" > test.txt
awslocal s3 cp test.txt s3://portal-blog-files/

# 확인
echo "✅ 설정 완료!"
awslocal s3 ls s3://portal-blog-files/

# 정리
rm test.txt
```

### 8.2 실행 권한 부여 및 실행

```bash
# 스크립트 실행 권한
chmod +x scripts/setup-localstack-s3.sh

# 실행
./scripts/setup-localstack-s3.sh
```

### 8.3 Blog Service 파일 업로드 테스트

```bash
# 1. Blog Service 실행 확인
curl http://localhost:8081/actuator/health

# 2. 테스트 이미지 생성
echo "Fake image content" > test-image.png

# 3. API 호출 (실제로는 JWT 토큰 필요)
curl -X POST http://localhost:8081/api/v1/files/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@test-image.png"

# 4. LocalStack S3에서 확인
awslocal s3 ls s3://portal-blog-files/

# 5. 파일 다운로드로 검증
awslocal s3 cp s3://portal-blog-files/test-image.png downloaded.png
cat downloaded.png
```

---

## 9. 실습 예제

### 실습 1: LocalStack S3 전체 워크플로우

```bash
# 1. LocalStack 상태 확인
docker-compose ps

# 2. 버킷 생성
awslocal s3 mb s3://practice-bucket

# 3. 여러 파일 준비
mkdir practice-files
echo "File 1" > practice-files/file1.txt
echo "File 2" > practice-files/file2.txt
echo "File 3" > practice-files/file3.txt

# 4. 디렉토리 전체 업로드
awslocal s3 sync practice-files/ s3://practice-bucket/

# 5. 버킷 내용 확인
awslocal s3 ls s3://practice-bucket/
# 2024-01-22 10:30:00         6 file1.txt
# 2024-01-22 10:30:00         6 file2.txt
# 2024-01-22 10:30:00         6 file3.txt

# 6. 특정 파일 다운로드
awslocal s3 cp s3://practice-bucket/file1.txt ./

# 7. 파일 삭제
awslocal s3 rm s3://practice-bucket/file1.txt

# 8. 버킷 전체 삭제
awslocal s3 rb s3://practice-bucket --force

# 9. 정리
rm -rf practice-files
```

### 실습 2: S3 API 고급 기능

```bash
# 1. 버킷 생성 (버전 관리 활성화)
awslocal s3api create-bucket \
  --bucket versioned-bucket \
  --region ap-northeast-2

# 2. 버전 관리 활성화
awslocal s3api put-bucket-versioning \
  --bucket versioned-bucket \
  --versioning-configuration Status=Enabled

# 3. 파일 업로드 (여러 버전)
echo "Version 1" > versioned-file.txt
awslocal s3 cp versioned-file.txt s3://versioned-bucket/

echo "Version 2" > versioned-file.txt
awslocal s3 cp versioned-file.txt s3://versioned-bucket/

echo "Version 3" > versioned-file.txt
awslocal s3 cp versioned-file.txt s3://versioned-bucket/

# 4. 모든 버전 확인
awslocal s3api list-object-versions \
  --bucket versioned-bucket \
  --prefix versioned-file.txt

# 5. 특정 버전 다운로드
awslocal s3api get-object \
  --bucket versioned-bucket \
  --key versioned-file.txt \
  --version-id VERSION_ID \
  downloaded-v1.txt

# 6. 정리
awslocal s3 rb s3://versioned-bucket --force
rm versioned-file.txt downloaded-v1.txt
```

### 실습 3: 배치 작업 스크립트

```bash
#!/bin/bash
# backup-to-s3.sh - 로그 파일 백업 예제

BACKUP_DATE=$(date +%Y%m%d)
BACKUP_BUCKET="portal-backups"
LOG_DIR="logs"

# 버킷 생성 (이미 있으면 무시)
awslocal s3 mb s3://$BACKUP_BUCKET 2>/dev/null || true

# 로그 파일 백업
echo "📦 로그 파일 백업 중..."
awslocal s3 sync $LOG_DIR/ s3://$BACKUP_BUCKET/$BACKUP_DATE/

# 30일 이상 된 백업 삭제 (실제 AWS에서만 동작)
# awslocal s3 rm s3://$BACKUP_BUCKET/ \
#   --recursive \
#   --exclude "*" \
#   --include "$(date -d '30 days ago' +%Y%m%d)*"

echo "✅ 백업 완료: s3://$BACKUP_BUCKET/$BACKUP_DATE/"
```

---

## 10. 유용한 CLI 팁

### 10.1 출력 형식 변경

```bash
# JSON (기본)
awslocal s3api list-buckets --output json

# 테이블 형식 (가독성 좋음)
awslocal s3api list-buckets --output table

# 텍스트 (스크립트 친화적)
awslocal s3api list-buckets --output text

# YAML
awslocal s3api list-buckets --output yaml
```

### 10.2 jq와 함께 사용

```bash
# jq 설치
brew install jq  # macOS

# 버킷 이름만 추출
awslocal s3api list-buckets | jq -r '.Buckets[].Name'

# 특정 필드 필터링
awslocal s3api list-objects --bucket my-bucket \
  | jq '.Contents[] | {Key: .Key, Size: .Size}'
```

### 10.3 자동 완성 설정

```bash
# Bash
echo "complete -C '/usr/local/bin/aws_completer' aws" >> ~/.bashrc
source ~/.bashrc

# Zsh
echo "source /usr/local/bin/aws_zsh_completer.sh" >> ~/.zshrc
source ~/.zshrc

# 사용: aws s3 <TAB>
```

### 10.4 디버그 모드

```bash
# 상세 로그 출력
awslocal s3 ls --debug

# HTTP 요청/응답 확인
awslocal s3 ls --debug 2>&1 | grep -A 10 "HTTP request"
```

---

## 11. 트러블슈팅

### 문제 1: "Unable to locate credentials"

```bash
# 원인: 자격 증명 미설정
# 해결:
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
```

### 문제 2: "Could not connect to the endpoint URL"

```bash
# 원인: LocalStack 미실행 또는 엔드포인트 오류
# 해결:
docker-compose ps  # LocalStack 상태 확인
curl http://localhost:4566/_localstack/health

# awslocal 대신 aws 사용 시
aws s3 ls --endpoint-url http://localhost:4566
```

### 문제 3: "The bucket you are attempting to access must be addressed using the specified endpoint"

```bash
# 원인: 리전 불일치
# 해결: 버킷 생성 시 리전 명시
awslocal s3api create-bucket \
  --bucket my-bucket \
  --region ap-northeast-2 \
  --create-bucket-configuration LocationConstraint=ap-northeast-2
```

---

## 12. 핵심 정리

| 도구 | 용도 | 설치 | 엔드포인트 |
|------|------|------|-----------|
| **aws** | 실제 AWS | brew/pip | aws.amazon.com |
| **awslocal** | LocalStack | pip | localhost:4566 |

### 필수 명령어

```bash
# 버킷 관리
awslocal s3 mb s3://bucket-name        # 생성
awslocal s3 ls                         # 목록
awslocal s3 rb s3://bucket-name --force # 삭제

# 파일 작업
awslocal s3 cp file.txt s3://bucket/   # 업로드
awslocal s3 cp s3://bucket/file.txt .  # 다운로드
awslocal s3 sync ./dir s3://bucket/    # 동기화

# 설정
~/.aws/credentials  # 자격 증명
~/.aws/config       # 리전, 출력 형식
```

---

## 13. 다음 학습

- [IAM 기초](../iam/iam-fundamentals.md) - 액세스 키와 권한 관리
- [S3 기초](../s3/s3-fundamentals.md) - S3 심화 학습
- [LocalStack 소개](../localstack/localstack-introduction.md) - LocalStack 전체 기능
- [S3 Portal Universe 적용](../s3/s3-portal-universe.md) - Blog Service 통합

---

## 참고 자료

- [AWS CLI 공식 문서](https://docs.aws.amazon.com/cli/latest/userguide/)
- [AWS CLI 명령 참조](https://awscli.amazonaws.com/v2/documentation/api/latest/index.html)
- [awslocal GitHub](https://github.com/localstack/awscli-local)
- [S3 CLI 예제](https://docs.aws.amazon.com/cli/latest/userguide/cli-services-s3-commands.html)
- [jq 매뉴얼](https://stedolan.github.io/jq/manual/)
