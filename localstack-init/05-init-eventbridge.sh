#!/bin/bash

# LocalStack EventBridge 초기화 스크립트
# Event Bus + SQS Targets + Rules 생성

echo "🚀 [$(date '+%Y-%m-%d %H:%M:%S')] Initializing LocalStack EventBridge..."

LOCALSTACK_HOST="${LOCALSTACK_HOST:-localstack}"
LOCALSTACK_PORT="${LOCALSTACK_PORT:-4566}"

# LocalStack EventBridge 서비스 대기
MAX_WAIT=60
WAIT=0
while [ $WAIT -lt $MAX_WAIT ]; do
  if curl -s "http://${LOCALSTACK_HOST}:${LOCALSTACK_PORT}/_localstack/health" 2>/dev/null | grep -q "events.*available\|events.*running"; then
    echo "✓ LocalStack EventBridge service is ready"
    break
  fi
  echo "⏳ Waiting for LocalStack EventBridge... ($WAIT/$MAX_WAIT)"
  sleep 2
  WAIT=$((WAIT + 2))
done

if [ $WAIT -ge $MAX_WAIT ]; then
  echo "❌ LocalStack EventBridge did not become ready in time"
  exit 1
fi

# ===== Custom Event Bus =====

echo "📡 Creating Event Bus..."

awslocal events create-event-bus --name portal-universe 2>/dev/null \
  && echo "✓ Created event bus: portal-universe" \
  || echo "✓ portal-universe event bus already exists"

# ===== SQS Target Queues =====

echo "📦 Creating SQS target queues..."

# DLQ 먼저 생성
awslocal sqs create-queue --queue-name high-value-order-dlq 2>/dev/null \
  && echo "✓ Created high-value-order-dlq" \
  || echo "✓ high-value-order-dlq already exists"

awslocal sqs create-queue --queue-name order-confirmation-dlq 2>/dev/null \
  && echo "✓ Created order-confirmation-dlq" \
  || echo "✓ order-confirmation-dlq already exists"

# DLQ ARN 조회
HV_DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://sqs.ap-northeast-2.localhost.localstack.cloud:4566/000000000000/high-value-order-dlq \
  --attribute-names QueueArn --query 'Attributes.QueueArn' --output text 2>/dev/null)

OC_DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://sqs.ap-northeast-2.localhost.localstack.cloud:4566/000000000000/order-confirmation-dlq \
  --attribute-names QueueArn --query 'Attributes.QueueArn' --output text 2>/dev/null)

# 고액 주문 관리자 알림 큐 (RedrivePolicy: 3번 실패 → DLQ)
awslocal sqs create-queue \
  --queue-name high-value-order-queue \
  --attributes "{
    \"VisibilityTimeout\": \"60\",
    \"MessageRetentionPeriod\": \"86400\",
    \"RedrivePolicy\": \"{\\\"deadLetterTargetArn\\\":\\\"${HV_DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"
  }" 2>/dev/null \
  && echo "✓ Created high-value-order-queue (DLQ: high-value-order-dlq)" \
  || echo "✓ high-value-order-queue already exists"

# 주문 이메일 확인 큐 (RedrivePolicy: 3번 실패 → DLQ)
awslocal sqs create-queue \
  --queue-name order-confirmation-queue \
  --attributes "{
    \"VisibilityTimeout\": \"60\",
    \"MessageRetentionPeriod\": \"86400\",
    \"RedrivePolicy\": \"{\\\"deadLetterTargetArn\\\":\\\"${OC_DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"
  }" 2>/dev/null \
  && echo "✓ Created order-confirmation-queue (DLQ: order-confirmation-dlq)" \
  || echo "✓ order-confirmation-queue already exists"

# ===== EventBridge Rules =====

echo "📋 Creating EventBridge rules..."

# Rule 1: 고액 주문 (10만원 이상)
awslocal events put-rule \
  --name high-value-order-rule \
  --event-bus-name portal-universe \
  --state ENABLED \
  --event-pattern '{
    "source": ["portal-universe.shopping-service"],
    "detail-type": ["OrderCreated"],
    "detail": {
      "totalAmount": [{"numeric": [">=", 100000]}]
    }
  }' 2>/dev/null \
  && echo "✓ Created rule: high-value-order-rule (totalAmount >= 100000)" \
  || echo "✓ high-value-order-rule already exists"

# Rule 2: 모든 주문
awslocal events put-rule \
  --name all-order-rule \
  --event-bus-name portal-universe \
  --state ENABLED \
  --event-pattern '{
    "source": ["portal-universe.shopping-service"],
    "detail-type": ["OrderCreated"]
  }' 2>/dev/null \
  && echo "✓ Created rule: all-order-rule (all OrderCreated events)" \
  || echo "✓ all-order-rule already exists"

# ===== Targets =====

echo "🎯 Connecting targets to rules..."

# Target ARN 조회
HV_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://sqs.ap-northeast-2.localhost.localstack.cloud:4566/000000000000/high-value-order-queue \
  --attribute-names QueueArn --query 'Attributes.QueueArn' --output text 2>/dev/null)

OC_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://sqs.ap-northeast-2.localhost.localstack.cloud:4566/000000000000/order-confirmation-queue \
  --attribute-names QueueArn --query 'Attributes.QueueArn' --output text 2>/dev/null)

# 고액 주문 → Admin SQS
awslocal events put-targets \
  --rule high-value-order-rule \
  --event-bus-name portal-universe \
  --targets "[{\"Id\":\"admin-sqs\",\"Arn\":\"${HV_QUEUE_ARN}\"}]" 2>/dev/null \
  && echo "✓ Connected high-value-order-rule → high-value-order-queue" \
  || echo "⚠ Failed to connect high-value-order-rule target"

# 모든 주문 → Email SQS
awslocal events put-targets \
  --rule all-order-rule \
  --event-bus-name portal-universe \
  --targets "[{\"Id\":\"email-sqs\",\"Arn\":\"${OC_QUEUE_ARN}\"}]" 2>/dev/null \
  && echo "✓ Connected all-order-rule → order-confirmation-queue" \
  || echo "⚠ Failed to connect all-order-rule target"

# ===== 검증 =====

echo ""
echo "✅ [$(date '+%Y-%m-%d %H:%M:%S')] LocalStack EventBridge initialization complete!"
echo ""
echo "Event Bus:"
awslocal events list-event-buses --output table 2>/dev/null
echo ""
echo "Rules:"
awslocal events list-rules --event-bus-name portal-universe --output table 2>/dev/null
