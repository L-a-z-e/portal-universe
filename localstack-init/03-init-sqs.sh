#!/bin/bash

# LocalStack SQS/SNS 초기화 스크립트
# email-queue + DLQ, SNS notification-events Topic 생성

echo "🚀 [$(date '+%Y-%m-%d %H:%M:%S')] Initializing LocalStack SQS/SNS..."

LOCALSTACK_HOST="${LOCALSTACK_HOST:-localstack}"
LOCALSTACK_PORT="${LOCALSTACK_PORT:-4566}"

# LocalStack SQS 서비스 대기
MAX_WAIT=60
WAIT=0
while [ $WAIT -lt $MAX_WAIT ]; do
  if curl -s "http://${LOCALSTACK_HOST}:${LOCALSTACK_PORT}/_localstack/health" 2>/dev/null | grep -q "sqs.*available\|sqs.*running"; then
    echo "✓ LocalStack SQS service is ready"
    break
  fi
  echo "⏳ Waiting for LocalStack SQS... ($WAIT/$MAX_WAIT)"
  sleep 2
  WAIT=$((WAIT + 2))
done

if [ $WAIT -ge $MAX_WAIT ]; then
  echo "❌ LocalStack SQS did not become ready in time"
  exit 1
fi

# ===== SQS Queues =====

# DLQ 먼저 생성 (RedrivePolicy에서 참조)
echo "📦 Creating SQS queues..."

awslocal sqs create-queue --queue-name email-dlq 2>/dev/null \
  && echo "✓ Created email-dlq" || echo "✓ email-dlq already exists"

# email-queue + RedrivePolicy (3번 실패 → DLQ)
DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://sqs.ap-northeast-2.localhost.localstack.cloud:4566/000000000000/email-dlq \
  --attribute-names QueueArn --query 'Attributes.QueueArn' --output text 2>/dev/null)

awslocal sqs create-queue \
  --queue-name email-queue \
  --attributes "{
    \"VisibilityTimeout\": \"60\",
    \"MessageRetentionPeriod\": \"86400\",
    \"RedrivePolicy\": \"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"
  }" 2>/dev/null \
  && echo "✓ Created email-queue (DLQ: email-dlq, maxReceiveCount: 3)" \
  || echo "✓ email-queue already exists"

# ===== SNS Topics =====

echo "📢 Creating SNS topics..."

awslocal sns create-topic --name notification-events 2>/dev/null \
  && echo "✓ Created notification-events topic" || echo "✓ notification-events topic already exists"

# SNS → email-queue 구독 (팬아웃)
EMAIL_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://sqs.ap-northeast-2.localhost.localstack.cloud:4566/000000000000/email-queue \
  --attribute-names QueueArn --query 'Attributes.QueueArn' --output text 2>/dev/null)

awslocal sns subscribe \
  --topic-arn arn:aws:sns:ap-northeast-2:000000000000:notification-events \
  --protocol sqs \
  --notification-endpoint "$EMAIL_QUEUE_ARN" 2>/dev/null \
  && echo "✓ Subscribed email-queue to notification-events" \
  || echo "⚠ Subscription failed"

echo ""
echo "✅ [$(date '+%Y-%m-%d %H:%M:%S')] LocalStack SQS/SNS initialization complete!"
echo ""
echo "Queues:"
awslocal sqs list-queues --output text 2>/dev/null
echo ""
echo "Topics:"
awslocal sns list-topics --output text 2>/dev/null