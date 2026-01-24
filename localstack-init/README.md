# LocalStack 초기화 스크립트

LocalStack Community 버전은 PERSISTENCE 기능이 제한적이므로, 컨테이너 재시작 시 데이터가 초기화될 수 있습니다. 이 디렉토리의 스크립트들은 LocalStack 시작 시 자동으로 실행되어 필요한 리소스를 생성합니다.

## 📁 구조

```
localstack-init/
├── README.md          # 이 파일
└── init-s3.sh         # S3 버킷 초기화 스크립트
```

## 🚀 작동 방식

1. **자동 실행**: `/etc/localstack/init/ready.d/` 디렉토리의 스크립트는 LocalStack이 준비되면 자동 실행
2. **멱등성**: 스크립트는 여러 번 실행해도 안전하게 설계 (이미 존재하는 리소스는 건너뜀)
3. **순차 실행**: 파일명 순서대로 실행됨

## 📄 init-s3.sh

### 기능

- `portal-universe-images` 버킷 생성 (이미지 파일 저장)
- `portal-universe-documents` 버킷 생성 (문서 파일 저장)
- `portal-universe-backups` 버킷 생성 (백업 파일 저장)
- `portal-universe-images` 버킷에 Public Read 정책 적용

### 수동 실행

```bash
# LocalStack 컨테이너 내부에서
docker exec localstack /etc/localstack/init/ready.d/init-s3.sh

# 또는 호스트에서
./localstack-init/init-s3.sh
```

## 🔧 새 초기화 스크립트 추가

### 1. 스크립트 생성

```bash
# 예: SQS 큐 초기화
cat > localstack-init/init-sqs.sh << 'EOF'
#!/bin/bash
echo "🚀 Initializing LocalStack SQS..."

# 큐 생성
awslocal sqs create-queue --queue-name order-events
awslocal sqs create-queue --queue-name notification-events

echo "✅ SQS initialization complete!"
EOF

chmod +x localstack-init/init-sqs.sh
```

### 2. Docker Compose에 추가

```yaml
# docker-compose.yml & docker-compose-local.yml
localstack:
  volumes:
    - ./localstack-init/init-s3.sh:/etc/localstack/init/ready.d/init-s3.sh
    - ./localstack-init/init-sqs.sh:/etc/localstack/init/ready.d/init-sqs.sh  # 추가
```

### 3. 컨테이너 재시작

```bash
docker-compose restart localstack

# 로그 확인
docker logs localstack -f
```

## ⚠️ 주의사항

### LocalStack Community vs Pro

| 기능 | Community | Pro |
|------|-----------|-----|
| Init Scripts | ✅ 지원 | ✅ 지원 |
| PERSISTENCE | ⚠️ 제한적 | ✅ 완전 지원 |
| Cloud Pods | ❌ 미지원 | ✅ 지원 |
| State Export/Import | ❌ 미지원 | ✅ 지원 |

### 데이터 영속성 전략

```
┌─────────────────────────────────────────┐
│   LocalStack Community 버전 전략        │
├─────────────────────────────────────────┤
│                                         │
│  1. Init Scripts (컨테이너 시작 시)     │
│     └─ 버킷, 큐 등 리소스 자동 생성     │
│                                         │
│  2. Application @PostConstruct          │
│     └─ 애플리케이션 시작 시 리소스 확인 │
│                                         │
│  3. CI/CD 파이프라인                    │
│     └─ 배포 전 리소스 생성 스크립트    │
│                                         │
└─────────────────────────────────────────┘
```

## 🧪 테스트

### 초기화 검증

```bash
# 1. LocalStack 시작
docker-compose -f docker-compose-local.yml up -d localstack

# 2. 로그에서 초기화 메시지 확인
docker logs localstack 2>&1 | grep "Initializing"
# 출력: 🚀 Initializing LocalStack S3...

# 3. 버킷 목록 확인
awslocal s3 ls
# 출력:
# portal-universe-images
# portal-universe-documents
# portal-universe-backups

# 4. Public Read 정책 확인
awslocal s3api get-bucket-policy --bucket portal-universe-images
```

### 재시작 후 검증

```bash
# 1. 재시작
docker-compose -f docker-compose-local.yml restart localstack

# 2. 30초 대기 (LocalStack 완전 시작 + Init Scripts 실행)
sleep 30

# 3. 버킷 확인 (자동으로 다시 생성됨)
awslocal s3 ls
```

## 📚 참고 자료

- [LocalStack Init Hooks 공식 문서](https://docs.localstack.cloud/references/init-hooks/)
- [프로젝트 학습 문서: LocalStack 영속성](../docs/learning/aws/localstack/localstack-persistence.md)
- [Troubleshooting: TS-20260123-001](../docs/troubleshooting/2026/01/TS-20260123-001-localstack-s3-persistence.md)

## 🆘 문제 해결

### Init Script가 실행되지 않음

```bash
# 스크립트가 실행 가능한지 확인
ls -la localstack-init/init-s3.sh
# -rwxr-xr-x (x 권한 필요)

# 없으면 추가
chmod +x localstack-init/init-s3.sh

# 컨테이너 재시작
docker-compose restart localstack
```

### 스크립트 오류 디버깅

```bash
# 상세 로그 확인
docker logs localstack 2>&1 | grep -A 10 "init-s3.sh"

# 컨테이너 내부에서 직접 실행
docker exec localstack /etc/localstack/init/ready.d/init-s3.sh
```

### 버킷이 생성되지 않음

```bash
# LocalStack 상태 확인
curl http://localhost:4566/_localstack/health

# S3 서비스가 실행 중인지 확인
# {"services": {"s3": "running", ...}}
```
