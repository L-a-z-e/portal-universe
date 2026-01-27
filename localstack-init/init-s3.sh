#!/bin/bash

# LocalStack S3 초기화 스크립트
# LocalStack Community 버전은 PERSISTENCE가 제한적이므로
# 시작 시 필요한 버킷과 기본 데이터를 자동으로 생성합니다.

echo "🚀 [$(date '+%Y-%m-%d %H:%M:%S')] Initializing LocalStack S3..."

# LocalStack이 준비될 때까지 대기 (최대 60초)
MAX_WAIT=60
WAIT=0
LOCALSTACK_HOST="${LOCALSTACK_HOST:-localstack}"
LOCALSTACK_PORT="${LOCALSTACK_PORT:-4566}"

while [ $WAIT -lt $MAX_WAIT ]; do
  if curl -s "http://${LOCALSTACK_HOST}:${LOCALSTACK_PORT}/_localstack/health" 2>/dev/null | grep -q "s3.*available\|s3.*running"; then
    echo "✓ LocalStack S3 service is ready"
    break
  fi
  echo "⏳ Waiting for LocalStack to be ready... ($WAIT/$MAX_WAIT)"
  sleep 2
  WAIT=$((WAIT + 2))
done

if [ $WAIT -ge $MAX_WAIT ]; then
  echo "❌ LocalStack did not become ready in time"
  exit 1
fi

# 버킷 생성 (이미 존재하면 무시)
echo "📦 Creating S3 buckets..."
awslocal s3 mb s3://blog-bucket 2>/dev/null && echo "✓ Created blog-bucket" || echo "✓ blog-bucket already exists"
awslocal s3 mb s3://portal-universe-images 2>/dev/null && echo "✓ Created portal-universe-images" || echo "✓ portal-universe-images already exists"
awslocal s3 mb s3://portal-universe-documents 2>/dev/null && echo "✓ Created portal-universe-documents" || echo "✓ portal-universe-documents already exists"
awslocal s3 mb s3://portal-universe-backups 2>/dev/null && echo "✓ Created portal-universe-backups" || echo "✓ portal-universe-backups already exists"

# Public Read 정책 설정 (이미지 버킷)
echo "🔓 Setting public read policy on portal-universe-images..."
awslocal s3api put-bucket-policy --bucket portal-universe-images --policy '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::portal-universe-images/*"
    }
  ]
}' 2>/dev/null && echo "✓ Policy applied" || echo "⚠ Policy application failed (may already exist)"

echo ""
echo "✅ [$(date '+%Y-%m-%d %H:%M:%S')] LocalStack S3 initialization complete!"
echo ""
echo "Available buckets:"
awslocal s3 ls
