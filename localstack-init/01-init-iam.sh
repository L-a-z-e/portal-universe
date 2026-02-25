#!/bin/bash

# LocalStack IAM 초기화 스크립트
# 서비스별 IAM User + 최소 권한 Policy를 생성합니다.
# 파일명 순서: init-iam.sh → init-s3.sh (IAM 먼저 실행)

echo "🔐 [$(date '+%Y-%m-%d %H:%M:%S')] Initializing LocalStack IAM..."

MAX_WAIT=60
WAIT=0

while [ $WAIT -lt $MAX_WAIT ]; do
  if curl -s "http://localhost:4566/_localstack/health" 2>/dev/null | grep -q '"iam".*"available\|running"'; then
    echo "✓ LocalStack IAM service is ready"
    break
  fi
  echo "⏳ Waiting for LocalStack IAM... ($WAIT/$MAX_WAIT)"
  sleep 2
  WAIT=$((WAIT + 2))
done

if [ $WAIT -ge $MAX_WAIT ]; then
  echo "❌ LocalStack IAM did not become ready in time"
  exit 1
fi

REGION="ap-northeast-2"
ACCOUNT_ID="000000000000"

# ============================================================
# Helper: IAM User + Policy 생성 (멱등)
# ============================================================
create_service_user() {
  local SERVICE_NAME="$1"
  local POLICY_NAME="${SERVICE_NAME}-policy"
  local POLICY_DOC="$2"

  # User 생성
  awslocal iam create-user --user-name "$SERVICE_NAME" 2>/dev/null \
    && echo "✓ Created IAM user: $SERVICE_NAME" \
    || echo "✓ IAM user already exists: $SERVICE_NAME"

  # Policy 생성 (이미 있으면 삭제 후 재생성 — Policy 내용 업데이트 용)
  EXISTING_ARN=$(awslocal iam list-policies --query "Policies[?PolicyName=='${POLICY_NAME}'].Arn" --output text 2>/dev/null)
  if [ -n "$EXISTING_ARN" ] && [ "$EXISTING_ARN" != "None" ]; then
    awslocal iam detach-user-policy --user-name "$SERVICE_NAME" --policy-arn "$EXISTING_ARN" 2>/dev/null
    awslocal iam delete-policy --policy-arn "$EXISTING_ARN" 2>/dev/null
  fi

  POLICY_ARN=$(awslocal iam create-policy \
    --policy-name "$POLICY_NAME" \
    --policy-document "$POLICY_DOC" \
    --query 'Policy.Arn' --output text 2>/dev/null)

  if [ -n "$POLICY_ARN" ] && [ "$POLICY_ARN" != "None" ]; then
    echo "✓ Created policy: $POLICY_NAME"
    awslocal iam attach-user-policy --user-name "$SERVICE_NAME" --policy-arn "$POLICY_ARN" 2>/dev/null
    echo "✓ Attached $POLICY_NAME → $SERVICE_NAME"
  else
    echo "⚠ Failed to create policy: $POLICY_NAME"
  fi
}

# ============================================================
# 1. blog-service: S3 blog-bucket 읽기/쓰기
# ============================================================
create_service_user "blog-service" '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "BlogBucketAccess",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::blog-bucket/*"
    },
    {
      "Sid": "BlogBucketList",
      "Effect": "Allow",
      "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::blog-bucket"
    },
    {
      "Sid": "BlogBucketHead",
      "Effect": "Allow",
      "Action": "s3:HeadBucket",
      "Resource": "*"
    }
  ]
}'

# ============================================================
# 2. drive-service: S3 drive-bucket + Multipart Upload
# ============================================================
create_service_user "drive-service" '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DriveBucketAccess",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:CreateMultipartUpload",
        "s3:UploadPart",
        "s3:CompleteMultipartUpload",
        "s3:AbortMultipartUpload",
        "s3:ListMultipartUploadParts"
      ],
      "Resource": "arn:aws:s3:::drive-bucket/*"
    },
    {
      "Sid": "DriveBucketList",
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket",
        "s3:ListBucketMultipartUploads",
        "s3:GetBucketVersioning",
        "s3:GetLifecycleConfiguration"
      ],
      "Resource": "arn:aws:s3:::drive-bucket"
    },
    {
      "Sid": "DriveBucketHead",
      "Effect": "Allow",
      "Action": "s3:HeadBucket",
      "Resource": "*"
    }
  ]
}'

# ============================================================
# 3. notification-service: SQS + SNS
# ============================================================
create_service_user "notification-service" "{
  \"Version\": \"2012-10-17\",
  \"Statement\": [
    {
      \"Sid\": \"SQSSend\",
      \"Effect\": \"Allow\",
      \"Action\": [
        \"sqs:SendMessage\",
        \"sqs:ReceiveMessage\",
        \"sqs:DeleteMessage\",
        \"sqs:GetQueueUrl\",
        \"sqs:GetQueueAttributes\",
        \"sqs:ChangeMessageVisibility\"
      ],
      \"Resource\": \"arn:aws:sqs:${REGION}:${ACCOUNT_ID}:notification-*\"
    },
    {
      \"Sid\": \"SNSPublish\",
      \"Effect\": \"Allow\",
      \"Action\": [
        \"sns:Publish\",
        \"sns:Subscribe\",
        \"sns:Unsubscribe\"
      ],
      \"Resource\": \"arn:aws:sns:${REGION}:${ACCOUNT_ID}:notification-*\"
    }
  ]
}"

# ============================================================
# 4. shopping-service: SQS + EventBridge
# ============================================================
create_service_user "shopping-service" "{
  \"Version\": \"2012-10-17\",
  \"Statement\": [
    {
      \"Sid\": \"SQSOrderQueues\",
      \"Effect\": \"Allow\",
      \"Action\": [
        \"sqs:SendMessage\",
        \"sqs:ReceiveMessage\",
        \"sqs:DeleteMessage\",
        \"sqs:GetQueueUrl\",
        \"sqs:GetQueueAttributes\"
      ],
      \"Resource\": \"arn:aws:sqs:${REGION}:${ACCOUNT_ID}:order-*\"
    },
    {
      \"Sid\": \"EventBridgePutEvents\",
      \"Effect\": \"Allow\",
      \"Action\": \"events:PutEvents\",
      \"Resource\": \"arn:aws:events:${REGION}:${ACCOUNT_ID}:event-bus/portal-universe\"
    }
  ]
}"

# ============================================================
# 검증: 생성된 IAM 리소스 출력
# ============================================================
echo ""
echo "📋 IAM Users:"
awslocal iam list-users --query 'Users[].UserName' --output table 2>/dev/null

echo ""
echo "📋 IAM Policies:"
awslocal iam list-policies --scope Local --query 'Policies[].[PolicyName,Arn]' --output table 2>/dev/null

echo ""
echo "✅ [$(date '+%Y-%m-%d %H:%M:%S')] LocalStack IAM initialization complete!"
