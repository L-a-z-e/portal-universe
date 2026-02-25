#!/bin/bash

# LocalStack Lambda 초기화 스크립트
# 이미지 썸네일 생성 Lambda 함수 배포 + S3 이벤트 트리거 설정

echo "🚀 [$(date '+%Y-%m-%d %H:%M:%S')] Initializing LocalStack Lambda..."

LOCALSTACK_HOST="${LOCALSTACK_HOST:-localstack}"
LOCALSTACK_PORT="${LOCALSTACK_PORT:-4566}"

# LocalStack Lambda 서비스 대기
MAX_WAIT=60
WAIT=0
while [ $WAIT -lt $MAX_WAIT ]; do
  if curl -s "http://${LOCALSTACK_HOST}:${LOCALSTACK_PORT}/_localstack/health" 2>/dev/null | grep -q "lambda.*available\|lambda.*running"; then
    echo "✓ LocalStack Lambda service is ready"
    break
  fi
  echo "⏳ Waiting for LocalStack Lambda... ($WAIT/$MAX_WAIT)"
  sleep 2
  WAIT=$((WAIT + 2))
done

if [ $WAIT -ge $MAX_WAIT ]; then
  echo "❌ LocalStack Lambda did not become ready in time"
  exit 1
fi

# ===== Lambda IAM Role =====

echo "🔑 Creating Lambda execution role..."

TRUST_POLICY='{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": { "Service": "lambda.amazonaws.com" },
      "Action": "sts:AssumeRole"
    }
  ]
}'

awslocal iam create-role \
  --role-name image-thumbnail-lambda-role \
  --assume-role-policy-document "$TRUST_POLICY" 2>/dev/null \
  && echo "✓ Created IAM role: image-thumbnail-lambda-role" \
  || echo "✓ image-thumbnail-lambda-role already exists"

# S3 + CloudWatch Logs 권한 부여
ROLE_POLICY='{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject", "s3:PutObject"],
      "Resource": "arn:aws:s3:::blog-bucket/*"
    },
    {
      "Effect": "Allow",
      "Action": ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}'

awslocal iam put-role-policy \
  --role-name image-thumbnail-lambda-role \
  --policy-name image-thumbnail-policy \
  --policy-document "$ROLE_POLICY" 2>/dev/null \
  && echo "✓ Attached S3+Logs policy to role" \
  || echo "⚠ Failed to attach policy"

ROLE_ARN="arn:aws:iam::000000000000:role/image-thumbnail-lambda-role"

# ===== Lambda 함수 배포 =====

echo "⚡ Deploying image-thumbnail Lambda function..."

# Lambda 소스 경로 탐색 (docker-compose에서 ./lambda:/lambda로 마운트)
LAMBDA_DIR=""
for candidate in "/lambda/image-thumbnail" "/etc/localstack/init/ready.d/../lambda/image-thumbnail"; do
  if [ -f "$candidate/handler.py" ]; then
    LAMBDA_DIR="$candidate"
    break
  fi
done

if [ -z "$LAMBDA_DIR" ]; then
  echo "❌ Lambda handler not found. Ensure ./lambda is mounted to /lambda in docker-compose."
  exit 1
fi
echo "  Found handler at: $LAMBDA_DIR/handler.py"

# Lambda 패키지: 사전 빌드된 package.zip 사용 (Linux x86_64 호환 Pillow 포함)
# package.zip은 lambda/image-thumbnail/build.sh로 빌드
# macOS Pillow C extension은 Linux Lambda 컨테이너에서 동작하지 않으므로
# 반드시 Docker 기반으로 Linux에서 빌드해야 함
PACKAGE_ZIP="$LAMBDA_DIR/package.zip"

if [ ! -f "$PACKAGE_ZIP" ]; then
  echo "❌ package.zip not found at $PACKAGE_ZIP"
  echo "   Run lambda/image-thumbnail/build.sh first to create the package."
  exit 1
fi

cp "$PACKAGE_ZIP" /tmp/image-thumbnail.zip
ZIP_SIZE=$(stat -c%s /tmp/image-thumbnail.zip 2>/dev/null || stat -f%z /tmp/image-thumbnail.zip 2>/dev/null)
echo "  Using pre-built package: ${ZIP_SIZE} bytes"

# 기존 함수 삭제 후 재생성
awslocal lambda delete-function --function-name image-thumbnail 2>/dev/null

awslocal lambda create-function \
  --function-name image-thumbnail \
  --runtime python3.11 \
  --handler handler.handler \
  --zip-file fileb:///tmp/image-thumbnail.zip \
  --role "$ROLE_ARN" \
  --timeout 30 \
  --memory-size 256 \
  --environment "Variables={AWS_ENDPOINT_URL=http://host.docker.internal:4566,AWS_DEFAULT_REGION=ap-northeast-2}" 2>/dev/null \
  && echo "✓ Created Lambda function: image-thumbnail" \
  || echo "⚠ Failed to create Lambda function"

# Lambda 함수가 Active 상태가 될 때까지 대기
echo "⏳ Waiting for Lambda function to be active..."
for i in $(seq 1 15); do
  STATE=$(awslocal lambda get-function \
    --function-name image-thumbnail \
    --query 'Configuration.State' --output text 2>/dev/null)
  if [ "$STATE" = "Active" ]; then
    echo "✓ Lambda function is Active"
    break
  fi
  sleep 1
done

# ===== S3 Event Notification → Lambda =====

echo "🔗 Configuring S3 → Lambda event trigger..."

# Lambda에 S3 호출 권한 부여
awslocal lambda add-permission \
  --function-name image-thumbnail \
  --principal s3.amazonaws.com \
  --statement-id s3-trigger \
  --action "lambda:InvokeFunction" \
  --source-arn arn:aws:s3:::blog-bucket 2>/dev/null \
  && echo "✓ Added S3 invoke permission to Lambda" \
  || echo "✓ S3 invoke permission already exists"

LAMBDA_ARN=$(awslocal lambda get-function \
  --function-name image-thumbnail \
  --query 'Configuration.FunctionArn' --output text 2>/dev/null)

# S3 Event Notification 설정 (PutObject → Lambda)
# 무한 루프 방지: Lambda 코드에서 thumbnails/ 프리픽스 스킵
awslocal s3api put-bucket-notification-configuration \
  --bucket blog-bucket \
  --notification-configuration "{
    \"LambdaFunctionConfigurations\": [
      {
        \"Id\": \"image-thumbnail-trigger\",
        \"LambdaFunctionArn\": \"${LAMBDA_ARN}\",
        \"Events\": [\"s3:ObjectCreated:Put\", \"s3:ObjectCreated:CompleteMultipartUpload\"]
      }
    ]
  }" 2>/dev/null \
  && echo "✓ S3 event notification configured: blog-bucket → image-thumbnail" \
  || echo "⚠ Failed to configure S3 event notification"

# ===== DLQ for Lambda =====

echo "💀 Setting up Lambda DLQ..."

awslocal sqs create-queue --queue-name image-thumbnail-dlq 2>/dev/null \
  && echo "✓ Created image-thumbnail-dlq" \
  || echo "✓ image-thumbnail-dlq already exists"

DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://sqs.ap-northeast-2.localhost.localstack.cloud:4566/000000000000/image-thumbnail-dlq \
  --attribute-names QueueArn --query 'Attributes.QueueArn' --output text 2>/dev/null)

# Lambda 업데이트 전 Active 상태 재확인 (LocalStack race condition 방지)
echo "⏳ Waiting before DLQ configuration..."
sleep 5

awslocal lambda update-function-configuration \
  --function-name image-thumbnail \
  --dead-letter-config "TargetArn=${DLQ_ARN}" 2>/dev/null \
  && echo "✓ DLQ configured: image-thumbnail → image-thumbnail-dlq" \
  || echo "⚠ Failed to configure DLQ (non-critical, can be set manually)"

# ===== 검증 =====

echo ""
echo "✅ [$(date '+%Y-%m-%d %H:%M:%S')] LocalStack Lambda initialization complete!"
echo ""
echo "Lambda functions:"
awslocal lambda list-functions --query 'Functions[].{Name:FunctionName,Runtime:Runtime,State:State}' --output table 2>/dev/null
echo ""
echo "S3 event notification on blog-bucket:"
awslocal s3api get-bucket-notification-configuration --bucket blog-bucket 2>/dev/null | python3 -m json.tool 2>/dev/null || echo "(no notification config)"
