---
id: TS-20260123-001
title: LocalStack S3 데이터 영속성 문제
type: troubleshooting
status: resolved
created: 2026-01-23
updated: 2026-01-23
author: Laze
severity: medium
resolved: true
affected_services: [localstack, blog-service]
tags: [localstack, s3, docker, persistence, data-loss]
---

# LocalStack S3 데이터 영속성 문제

## 요약

| 항목 | 내용 |
|------|------|
| **심각도** | 🟠 Medium |
| **발생일** | 2026-01-23 |
| **해결일** | 2026-01-23 |
| **영향 서비스** | LocalStack, Blog Service (S3 파일 업로드) |

## 증상 (Symptoms)

### 현상
- LocalStack S3에 이미지/파일 업로드 후 정상 작동
- Docker 컨테이너 재시작(`docker-compose restart`) 후 저장된 데이터 사라짐
- 버킷은 애플리케이션 `@PostConstruct`로 자동 재생성되지만, 업로드된 파일은 복구 불가

### 에러 메시지
```bash
$ awslocal s3 ls s3://portal-blog-uploads/
# (출력 없음 - 파일 사라짐)

$ curl http://localhost:4566/portal-blog-uploads/abc-123.jpg
# NoSuchKey 또는 404 에러
```

### 모니터링 지표
- `localstack_data/state/` 디렉토리가 비어있음
- LocalStack 컨테이너 재시작 시 영속성 데이터 미적용

## 원인 분석 (Root Cause)

### 초기 추정
- PERSISTENCE 설정 누락으로 인한 데이터 휘발

### 실제 원인

| 원인 | 상세 |
|------|------|
| **DATA_DIR deprecated** | 최신 LocalStack에서 `DATA_DIR` 환경변수는 무시됨 (경고만 출력) |
| **container_name 미설정** | 컨테이너 재시작 시 새 컨테이너 생성 가능성 |
| **Named Volume 미사용** | Bind mount(`./localstack_data`)는 권한 문제 발생 가능 |
| **PERSISTENCE Pro 기능** | LocalStack Community 버전에서는 영속성이 제한적으로 지원됨 |

### 분석 과정

1. **docker-compose.yml 설정 확인**
   ```yaml
   # 기존 설정 (문제)
   environment:
     - PERSISTENCE=1
     - DATA_DIR=/var/lib/localstack  # deprecated, 무시됨
   volumes:
     - ./localstack_data:/var/lib/localstack  # Bind mount
   ```

2. **LocalStack 공식 문서 확인**
   > "If `DATA_DIR` is set, its value is ignored, a warning is logged and `PERSISTENCE` is set to `1`."

3. **state 디렉토리 확인**
   ```bash
   $ ls -la ./localstack_data/state/
   # 비어있음 - 데이터가 저장되지 않음
   ```

4. **PERSISTENCE 기능 제한 확인**
   - LocalStack Pro 버전에서만 완전한 영속성 지원
   - Community 버전에서는 제한적

## 해결 방법 (Solution)

### 즉시 조치 (Immediate Fix)

Community 버전에서 최대한의 영속성을 확보하는 설정 적용:

```yaml
# docker-compose.yml
localstack:
  image: localstack/localstack:latest
  container_name: localstack  # 고정된 컨테이너 이름
  ports:
    - "4566:4566"
  environment:
    - SERVICES=s3
    - AWS_ACCESS_KEY_ID=test
    - AWS_SECRET_ACCESS_KEY=test
    - AWS_DEFAULT_REGION=ap-northeast-2
    - PERSISTENCE=1
    # DATA_DIR은 deprecated - 제거
  volumes:
    - localstack-data:/var/lib/localstack  # Named Volume 사용
  networks:
    - portal-universe-net
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:4566/_localstack/health"]
    interval: 10s
    timeout: 5s
    retries: 5

volumes:
  localstack-data:  # Named Volume 정의
```

### 영구 조치 (Permanent Fix)

1. **Named Volume 사용**: Bind mount 대신 Docker Named Volume 사용으로 권한 문제 해결
2. **container_name 고정**: 재시작 시 동일 컨테이너 유지
3. **DATA_DIR 제거**: deprecated 환경변수 제거
4. **healthcheck 추가**: 서비스 의존성에서 LocalStack 준비 상태 확인 가능

### 수정된 파일

| 파일 경로 | 수정 내용 |
|----------|----------|
| `/docker-compose.yml` | LocalStack 설정 수정, Named Volume 추가 |
| `/docker-compose-local.yml` | 동일한 설정 적용 |

### 주요 변경 사항

```diff
  localstack:
    image: localstack/localstack:latest
+   container_name: localstack
    ports:
      - "4566:4566"
    environment:
      - SERVICES=s3
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
      - AWS_DEFAULT_REGION=ap-northeast-2
      - PERSISTENCE=1
-     - DATA_DIR=/var/lib/localstack
+     # DATA_DIR은 deprecated - PERSISTENCE=1만 사용
    volumes:
-     - ./localstack_data:/var/lib/localstack
+     - localstack-data:/var/lib/localstack
    networks:
      - portal-universe-net
+   healthcheck:
+     test: ["CMD", "curl", "-f", "http://localhost:4566/_localstack/health"]
+     interval: 10s
+     timeout: 5s
+     retries: 5
```

## 재발 방지 (Prevention)

### 모니터링
- LocalStack healthcheck 상태 모니터링 추가
- 영속성 테스트 자동화 (CI에서 재시작 후 데이터 확인)

### 프로세스 개선

1. **안전한 컨테이너 관리**
   ```bash
   # 안전 (데이터 유지)
   docker-compose restart localstack
   docker-compose down && docker-compose up -d

   # 위험 (데이터 삭제!)
   docker-compose down -v  # 절대 사용 금지
   ```

2. **버킷 자동 생성 유지**
   - `FileService.ensureBucketExists()` (@PostConstruct) 유지
   - 재시작 시 버킷 메타데이터 복구

3. **Pro 버전 고려**
   - 완전한 영속성이 필요한 경우 LocalStack Pro 도입 검토

## 학습 포인트

1. **LocalStack DATA_DIR은 deprecated됨**: 최신 버전에서는 `PERSISTENCE=1`만 사용
2. **PERSISTENCE는 Pro 기능**: Community 버전에서는 제한적으로 지원됨
3. **Named Volume vs Bind Mount**: Docker 관리 볼륨이 권한 문제가 적음
4. **container_name 고정 필수**: 재시작 시 컨테이너 ID 변경 방지
5. **healthcheck 활용**: 서비스 의존성에서 준비 상태 확인 가능

## Community vs Pro 비교

| 기능 | Community | Pro |
|------|-----------|-----|
| S3 기본 기능 | O | O |
| PERSISTENCE | 제한적 | 완전 지원 |
| 버킷 메타데이터 영속성 | X | O |
| 파일 데이터 영속성 | 제한적 | O |

## 관련 링크

- [LocalStack Persistence 문서](https://docs.localstack.cloud/references/persistence-mechanism/)
- [LocalStack 학습 문서](/docs/learning/aws/localstack/localstack-persistence.md)
- [LocalStack Troubleshooting 가이드](/docs/learning/aws/localstack/localstack-troubleshooting.md)

## 검증 방법

```bash
# 1. LocalStack 시작
docker-compose up -d localstack

# 2. 버킷 생성 및 파일 업로드
awslocal s3 mb s3://test-bucket
echo "test" | awslocal s3 cp - s3://test-bucket/test.txt

# 3. 재시작
docker-compose restart localstack

# 4. 데이터 확인
awslocal s3 ls s3://test-bucket/
# test.txt가 있으면 성공
```
