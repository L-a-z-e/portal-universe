# LocalStack Services

## 학습 목표
- LocalStack이 지원하는 AWS 서비스 목록을 파악한다
- Portal Universe에서 현재 사용 중인 서비스를 이해한다
- 추후 확장 가능한 서비스들을 학습한다
- 각 서비스별 기본 사용법을 익힌다

---

## 개념 설명

### LocalStack 서비스 계층

```
┌─────────────────────────────────────────────────────┐
│  LocalStack Community (무료)                        │
│  ├─ S3                 (Object Storage)             │
│  ├─ DynamoDB           (NoSQL Database)             │
│  ├─ Lambda             (Serverless Functions)       │
│  ├─ SQS                (Message Queue)              │
│  ├─ SNS                (Pub/Sub Notifications)      │
│  ├─ Kinesis            (Streaming)                  │
│  ├─ CloudWatch         (Monitoring/Logs)            │
│  ├─ API Gateway        (REST/WebSocket APIs)        │
│  ├─ CloudFormation     (Infrastructure as Code)     │
│  ├─ Secrets Manager    (Secret Storage)             │
│  └─ ... (30+ services)                              │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  LocalStack Pro (유료)                              │
│  ├─ RDS                (Relational Database)        │
│  ├─ EKS                (Kubernetes)                 │
│  ├─ ECS                (Container Orchestration)    │
│  ├─ Cognito            (User Authentication)        │
│  ├─ Amplify            (Full-stack Apps)            │
│  ├─ AppSync            (GraphQL)                    │
│  └─ ... (Pro features)                              │
└─────────────────────────────────────────────────────┘
```

### SERVICES 환경변수

```yaml
# 방법 1: 특정 서비스만 활성화 (권장)
environment:
  - SERVICES=s3,sqs,sns

# 방법 2: 모든 서비스 활성화 (느림)
environment:
  - SERVICES=

# 방법 3: 그룹으로 활성화
environment:
  - SERVICES=serverless  # Lambda, API Gateway, DynamoDB 등
```

---

## Portal Universe 적용

### 현재 사용 중인 서비스

#### S3 (Simple Storage Service)
```yaml
# docker-compose.yml
environment:
  - SERVICES=s3
```

**용도**: Blog Service의 파일 업로드
- 블로그 게시글 이미지
- 첨부 파일
- 사용자 프로필 이미지 (추후)

**Spring Boot 설정**:
```yaml
# application-local.yml
spring:
  cloud:
    aws:
      s3:
        endpoint: http://localhost:4566
        bucket-name: portal-blog-uploads
```

### 추후 확장 가능한 서비스

```
┌─────────────────────────────────────────────────────┐
│  Portal Universe 확장 계획                          │
├─────────────────────────────────────────────────────┤
│  S3         ✅ 현재 사용 중                          │
│  SQS        🔜 비동기 작업 큐 (이메일 발송 등)       │
│  SNS        🔜 이벤트 발행 (알림 서비스)             │
│  Lambda     🔜 이벤트 기반 처리 (이미지 리사이징)    │
│  DynamoDB   💡 세션 저장소 대안                      │
│  Secrets    💡 민감 정보 관리                        │
└─────────────────────────────────────────────────────┘
```

---

## 서비스별 가이드

### 1. S3 (Simple Storage Service)

#### 기본 개념
- 객체 스토리지 (파일 저장소)
- 버킷(Bucket)에 객체(Object) 저장
- HTTP/HTTPS로 접근 가능

#### 주요 작업

**버킷 생성**
```bash
# awslocal 사용
awslocal s3 mb s3://my-bucket

# AWS CLI (endpoint 명시)
aws --endpoint-url=http://localhost:4566 s3 mb s3://my-bucket
```

**파일 업로드/다운로드**
```bash
# 파일 업로드
awslocal s3 cp local-file.txt s3://my-bucket/

# 파일 다운로드
awslocal s3 cp s3://my-bucket/local-file.txt downloaded.txt

# 디렉토리 동기화
awslocal s3 sync ./local-dir s3://my-bucket/prefix/
```

**파일 목록 조회**
```bash
# 버킷 목록
awslocal s3 ls

# 버킷 내 파일 목록
awslocal s3 ls s3://my-bucket/

# 특정 prefix 검색
awslocal s3 ls s3://my-bucket/images/
```

#### Spring Boot 연동
```java
@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    public String uploadFile(MultipartFile file) {
        String key = generateKey(file.getOriginalFilename());

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build(),
            RequestBody.fromInputStream(
                file.getInputStream(),
                file.getSize()
            )
        );

        return generateUrl(key);
    }

    public byte[] downloadFile(String key) {
        ResponseBytes<GetObjectResponse> objectBytes =
            s3Client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()
            );

        return objectBytes.asByteArray();
    }

    public void deleteFile(String key) {
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()
        );
    }
}
```

---

### 2. SQS (Simple Queue Service) 🔜

#### 기본 개념
- 완전 관리형 메시지 큐 서비스
- 분산 시스템 간 비동기 통신
- 최소 1회 전달 보장

#### 사용 시나리오 (Portal Universe)
```
┌──────────────┐  이벤트 발행   ┌──────────────┐
│ Auth Service │ ────────────> │  SQS Queue   │
│ (회원가입)   │                 │              │
└──────────────┘                 └──────────────┘
                                        │
                                        │ 폴링
                                        ▼
                           ┌──────────────────────┐
                           │ Notification Service │
                           │ (환영 이메일 발송)   │
                           └──────────────────────┘
```

#### LocalStack 설정
```yaml
environment:
  - SERVICES=s3,sqs
```

#### 기본 작업
```bash
# 큐 생성
awslocal sqs create-queue --queue-name user-events

# 메시지 전송
awslocal sqs send-message \
  --queue-url http://localhost:4566/000000000000/user-events \
  --message-body '{"userId":"123","event":"signup"}'

# 메시지 수신
awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/user-events

# 메시지 삭제
awslocal sqs delete-message \
  --queue-url http://localhost:4566/000000000000/user-events \
  --receipt-handle "receipt-handle-from-receive"
```

#### Spring Boot 연동
```java
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final SqsClient sqsClient;
    private final String queueUrl;

    public void publishUserSignupEvent(User user) {
        String messageBody = objectMapper.writeValueAsString(
            new UserSignupEvent(user.getId(), user.getEmail())
        );

        sqsClient.sendMessage(SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(messageBody)
            .build()
        );
    }
}

@Component
@RequiredArgsConstructor
public class EventConsumer {

    private final SqsClient sqsClient;
    private final String queueUrl;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 5000)
    public void pollMessages() {
        ReceiveMessageResponse response = sqsClient.receiveMessage(
            ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(20)  // Long polling
                .build()
        );

        for (Message message : response.messages()) {
            processMessage(message);
            deleteMessage(message.receiptHandle());
        }
    }
}
```

---

### 3. SNS (Simple Notification Service) 🔜

#### 기본 개념
- Pub/Sub 메시징 서비스
- 1:N 메시지 전달
- 다중 구독자 지원 (SQS, Lambda, HTTP, Email 등)

#### 사용 시나리오 (Portal Universe)
```
                    ┌────────────────┐
                    │   SNS Topic    │
                    │ "user-events"  │
                    └────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌────────────┐      ┌────────────┐      ┌────────────┐
│ Email SQS  │      │ SMS SQS    │      │ Webhook    │
└────────────┘      └────────────┘      └────────────┘
```

#### LocalStack 설정
```yaml
environment:
  - SERVICES=s3,sqs,sns
```

#### 기본 작업
```bash
# Topic 생성
awslocal sns create-topic --name user-events

# 구독 생성 (SQS)
awslocal sns subscribe \
  --topic-arn arn:aws:sns:ap-northeast-2:000000000000:user-events \
  --protocol sqs \
  --notification-endpoint arn:aws:sqs:ap-northeast-2:000000000000:notification-queue

# 메시지 발행
awslocal sns publish \
  --topic-arn arn:aws:sns:ap-northeast-2:000000000000:user-events \
  --message '{"userId":"123","event":"signup"}'
```

#### Spring Boot 연동
```java
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final SnsClient snsClient;
    private final String topicArn;

    public void publishEvent(String eventType, Object payload) {
        String message = objectMapper.writeValueAsString(payload);

        snsClient.publish(PublishRequest.builder()
            .topicArn(topicArn)
            .subject(eventType)
            .message(message)
            .build()
        );
    }
}
```

---

### 4. Lambda (Serverless Functions) 🔜

#### 기본 개념
- 서버리스 컴퓨팅 서비스
- 이벤트 기반 코드 실행
- 자동 스케일링

#### 사용 시나리오 (Portal Universe)
```
S3 버킷에 이미지 업로드
        │
        ▼
Lambda 함수 트리거
        │
        ├─ 썸네일 생성 (500x500)
        ├─ 워터마크 추가
        └─ 최적화된 이미지 S3 저장
```

#### LocalStack 설정
```yaml
environment:
  - SERVICES=s3,lambda
```

#### 기본 작업
```bash
# Lambda 함수 생성 (ZIP 파일)
zip function.zip index.js

awslocal lambda create-function \
  --function-name image-processor \
  --runtime nodejs18.x \
  --role arn:aws:iam::000000000000:role/lambda-role \
  --handler index.handler \
  --zip-file fileb://function.zip

# 함수 호출
awslocal lambda invoke \
  --function-name image-processor \
  --payload '{"key":"image.jpg"}' \
  response.json
```

---

### 5. DynamoDB (NoSQL Database) 💡

#### 기본 개념
- 완전 관리형 NoSQL 데이터베이스
- Key-Value 및 Document 스토어
- 자동 스케일링

#### 사용 시나리오 (Portal Universe)
- 세션 저장소 (Spring Session 대안)
- 실시간 채팅 메시지
- 사용자 활동 로그

#### LocalStack 설정
```yaml
environment:
  - SERVICES=s3,dynamodb
```

#### 기본 작업
```bash
# 테이블 생성
awslocal dynamodb create-table \
  --table-name Sessions \
  --attribute-definitions \
    AttributeName=SessionId,AttributeType=S \
  --key-schema \
    AttributeName=SessionId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

# 아이템 추가
awslocal dynamodb put-item \
  --table-name Sessions \
  --item '{
    "SessionId": {"S": "session-123"},
    "UserId": {"S": "user-456"},
    "ExpiresAt": {"N": "1706000000"}
  }'

# 아이템 조회
awslocal dynamodb get-item \
  --table-name Sessions \
  --key '{"SessionId": {"S": "session-123"}}'
```

---

### 6. Secrets Manager 💡

#### 기본 개념
- 민감한 정보 암호화 저장
- 자동 로테이션 지원
- 애플리케이션에서 안전하게 접근

#### 사용 시나리오 (Portal Universe)
- Database credentials
- API keys (외부 서비스 연동)
- JWT secret keys

#### LocalStack 설정
```yaml
environment:
  - SERVICES=s3,secretsmanager
```

#### 기본 작업
```bash
# Secret 생성
awslocal secretsmanager create-secret \
  --name db-credentials \
  --secret-string '{
    "username":"admin",
    "password":"secret123"
  }'

# Secret 조회
awslocal secretsmanager get-secret-value \
  --secret-id db-credentials
```

#### Spring Boot 연동
```java
@Configuration
public class SecretsConfig {

    @Bean
    public DataSource dataSource(SecretsManagerClient secretsClient) {
        GetSecretValueResponse response = secretsClient.getSecretValue(
            GetSecretValueRequest.builder()
                .secretId("db-credentials")
                .build()
        );

        Map<String, String> credentials =
            objectMapper.readValue(response.secretString(), Map.class);

        return DataSourceBuilder.create()
            .url("jdbc:mysql://localhost:3306/db")
            .username(credentials.get("username"))
            .password(credentials.get("password"))
            .build();
    }
}
```

---

## 서비스 조합 패턴

### 패턴 1: 비동기 파일 처리
```
S3 (업로드) → Lambda (처리) → S3 (저장) → SQS (알림)
```

**구현 예시**:
```yaml
environment:
  - SERVICES=s3,lambda,sqs
```

### 패턴 2: 이벤트 기반 알림
```
SNS (발행) → SQS (큐잉) → Lambda (처리) → 외부 API
```

**구현 예시**:
```yaml
environment:
  - SERVICES=sns,sqs,lambda
```

### 패턴 3: 세션 관리
```
API Gateway → Lambda → DynamoDB (세션 저장)
```

**구현 예시**:
```yaml
environment:
  - SERVICES=apigateway,lambda,dynamodb
```

---

## 실습 예제

### 실습 1: 다중 서비스 활성화

```yaml
# docker-compose.yml 수정
localstack:
  environment:
    - SERVICES=s3,sqs,sns
```

```bash
# LocalStack 재시작
docker-compose up -d --force-recreate localstack

# 활성화된 서비스 확인
curl http://localhost:4566/_localstack/health | jq '.services'

# 예상 출력:
# {
#   "s3": "running",
#   "sqs": "running",
#   "sns": "running"
# }
```

### 실습 2: S3 → SQS 이벤트 알림

```bash
# 1. SQS 큐 생성
awslocal sqs create-queue --queue-name s3-events

# 2. S3 버킷 생성
awslocal s3 mb s3://event-bucket

# 3. S3 이벤트 알림 설정
awslocal s3api put-bucket-notification-configuration \
  --bucket event-bucket \
  --notification-configuration '{
    "QueueConfigurations": [{
      "QueueArn": "arn:aws:sqs:ap-northeast-2:000000000000:s3-events",
      "Events": ["s3:ObjectCreated:*"]
    }]
  }'

# 4. 파일 업로드
echo "test" > test.txt
awslocal s3 cp test.txt s3://event-bucket/

# 5. SQS에서 이벤트 확인
awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/s3-events
```

---

## 핵심 요약

### Portal Universe 현재 상태
- ✅ **S3**: Blog Service 파일 업로드

### 확장 계획
- 🔜 **SQS**: 비동기 작업 큐 (이메일 발송, 이미지 처리 등)
- 🔜 **SNS**: 이벤트 Pub/Sub (알림 서비스)
- 🔜 **Lambda**: 서버리스 이벤트 처리
- 💡 **DynamoDB**: 세션 저장소 대안
- 💡 **Secrets Manager**: 민감 정보 관리

### SERVICES 환경변수 설정

| 설정 | 용도 | 시작 시간 |
|------|------|----------|
| `SERVICES=s3` | 단일 서비스 (현재) | 빠름 ⚡ |
| `SERVICES=s3,sqs,sns` | 다중 서비스 | 보통 |
| `SERVICES=` | 모든 서비스 | 느림 🐌 |

### 권장 사항
1. 필요한 서비스만 활성화 (성능 최적화)
2. 로컬 개발 시 Community 버전으로 충분
3. 프로덕션 환경은 실제 AWS 사용 권장

---

## 관련 문서
- [LocalStack Setup](./localstack-setup.md) - 설치 및 기본 설정
- [LocalStack Persistence](./localstack-persistence.md) - 데이터 영속성
- [S3 Fundamentals](../s3/s3-fundamentals.md) - S3 기본 개념
- [AWS Service Overview](../fundamentals/aws-services-overview.md) - AWS 서비스 개요
- [LocalStack Official Docs](https://docs.localstack.cloud/) - 공식 문서
