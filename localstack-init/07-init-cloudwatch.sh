#!/bin/bash

# LocalStack CloudWatch 초기화 스크립트
# CloudWatch Alarm + SNS 알림 채널 설정
# 기존 SQS DLQ, Lambda, EventBridge 리소스에 대한 모니터링

echo "🚀 [$(date '+%Y-%m-%d %H:%M:%S')] Initializing LocalStack CloudWatch..."

LOCALSTACK_HOST="${LOCALSTACK_HOST:-localstack}"
LOCALSTACK_PORT="${LOCALSTACK_PORT:-4566}"

# LocalStack CloudWatch 서비스 대기
MAX_WAIT=60
WAIT=0
while [ $WAIT -lt $MAX_WAIT ]; do
  if curl -s "http://${LOCALSTACK_HOST}:${LOCALSTACK_PORT}/_localstack/health" 2>/dev/null | grep -q "cloudwatch.*available\|cloudwatch.*running"; then
    echo "✓ LocalStack CloudWatch service is ready"
    break
  fi
  echo "⏳ Waiting for LocalStack CloudWatch... ($WAIT/$MAX_WAIT)"
  sleep 2
  WAIT=$((WAIT + 2))
done

if [ $WAIT -ge $MAX_WAIT ]; then
  echo "❌ LocalStack CloudWatch did not become ready in time"
  exit 1
fi

# ===== SNS Alarm Topic =====

echo "📢 Creating CloudWatch alarm notification topic..."

awslocal sns create-topic --name cloudwatch-alarms 2>/dev/null \
  && echo "✓ Created SNS topic: cloudwatch-alarms" \
  || echo "✓ cloudwatch-alarms already exists"

ALARM_TOPIC_ARN="arn:aws:sns:ap-northeast-2:000000000000:cloudwatch-alarms"

# 알람 확인용 SQS 큐 (SNS → SQS 구독으로 알람 메시지 수신 확인)
awslocal sqs create-queue --queue-name cloudwatch-alarm-queue 2>/dev/null \
  && echo "✓ Created cloudwatch-alarm-queue (for alarm verification)" \
  || echo "✓ cloudwatch-alarm-queue already exists"

ALARM_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://sqs.ap-northeast-2.localhost.localstack.cloud:4566/000000000000/cloudwatch-alarm-queue \
  --attribute-names QueueArn --query 'Attributes.QueueArn' --output text 2>/dev/null)

awslocal sns subscribe \
  --topic-arn "$ALARM_TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$ALARM_QUEUE_ARN" 2>/dev/null \
  && echo "✓ Subscribed cloudwatch-alarm-queue to cloudwatch-alarms topic" \
  || echo "⚠ Subscription failed"

# ===== CloudWatch Alarms =====

echo "🔔 Creating CloudWatch Alarms..."

# --- Alarm 1: email-dlq 메시지 감지 ---
# 이메일 처리 실패 시 DLQ에 메시지가 쌓임 → 즉시 알림
awslocal cloudwatch put-metric-alarm \
  --alarm-name "email-dlq-messages" \
  --alarm-description "Email DLQ has messages - email processing failures detected" \
  --namespace "AWS/SQS" \
  --metric-name "ApproximateNumberOfMessagesVisible" \
  --dimensions "Name=QueueName,Value=email-dlq" \
  --statistic Sum \
  --period 60 \
  --evaluation-periods 1 \
  --threshold 1 \
  --comparison-operator GreaterThanOrEqualToThreshold \
  --alarm-actions "$ALARM_TOPIC_ARN" \
  --treat-missing-data notBreaching 2>/dev/null \
  && echo "✓ Created alarm: email-dlq-messages (threshold >= 1)" \
  || echo "⚠ Failed to create email-dlq-messages alarm"

# --- Alarm 2: image-thumbnail-dlq 메시지 감지 ---
# Lambda 썸네일 생성 3회 실패 시 DLQ에 격리 → 알림
awslocal cloudwatch put-metric-alarm \
  --alarm-name "image-thumbnail-dlq-messages" \
  --alarm-description "Image thumbnail DLQ has messages - Lambda processing failures" \
  --namespace "AWS/SQS" \
  --metric-name "ApproximateNumberOfMessagesVisible" \
  --dimensions "Name=QueueName,Value=image-thumbnail-dlq" \
  --statistic Sum \
  --period 60 \
  --evaluation-periods 1 \
  --threshold 1 \
  --comparison-operator GreaterThanOrEqualToThreshold \
  --alarm-actions "$ALARM_TOPIC_ARN" \
  --treat-missing-data notBreaching 2>/dev/null \
  && echo "✓ Created alarm: image-thumbnail-dlq-messages (threshold >= 1)" \
  || echo "⚠ Failed to create image-thumbnail-dlq-messages alarm"

# --- Alarm 3: Lambda 에러율 ---
# image-thumbnail Lambda 5분간 에러 3회 이상 → 알림
awslocal cloudwatch put-metric-alarm \
  --alarm-name "lambda-image-thumbnail-errors" \
  --alarm-description "Lambda image-thumbnail error rate too high" \
  --namespace "AWS/Lambda" \
  --metric-name "Errors" \
  --dimensions "Name=FunctionName,Value=image-thumbnail" \
  --statistic Sum \
  --period 300 \
  --evaluation-periods 1 \
  --threshold 3 \
  --comparison-operator GreaterThanOrEqualToThreshold \
  --alarm-actions "$ALARM_TOPIC_ARN" \
  --treat-missing-data notBreaching 2>/dev/null \
  && echo "✓ Created alarm: lambda-image-thumbnail-errors (threshold >= 3 in 5min)" \
  || echo "⚠ Failed to create lambda-image-thumbnail-errors alarm"

# --- Alarm 4: 주문 실패율 (Custom Metric) ---
# shopping-service가 PutMetricData로 발행하는 커스텀 메트릭
awslocal cloudwatch put-metric-alarm \
  --alarm-name "order-saga-failures" \
  --alarm-description "Order saga failure rate too high" \
  --namespace "PortalUniverse/Shopping" \
  --metric-name "OrderSagaFailure" \
  --statistic Sum \
  --period 300 \
  --evaluation-periods 1 \
  --threshold 5 \
  --comparison-operator GreaterThanOrEqualToThreshold \
  --alarm-actions "$ALARM_TOPIC_ARN" \
  --treat-missing-data notBreaching 2>/dev/null \
  && echo "✓ Created alarm: order-saga-failures (threshold >= 5 in 5min)" \
  || echo "⚠ Failed to create order-saga-failures alarm"

# ===== 검증 =====

echo ""
echo "✅ [$(date '+%Y-%m-%d %H:%M:%S')] LocalStack CloudWatch initialization complete!"
echo ""
echo "CloudWatch Alarms:"
awslocal cloudwatch describe-alarms \
  --query 'MetricAlarms[].{Name:AlarmName,Metric:MetricName,Threshold:Threshold,State:StateValue}' \
  --output table 2>/dev/null
echo ""
echo "SNS Topics:"
awslocal sns list-topics --output text 2>/dev/null
