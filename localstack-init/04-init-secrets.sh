#!/bin/bash

# LocalStack Secrets Manager + SSM Parameter Store 초기화 스크립트
#
# 호스트에서 실행 (LocalStack 컨테이너 안이 아님)
# .env.local에서 실제 값을 읽어 LocalStack SM/SSM에 등록
#
# 사용법: ./localstack-init/init-secrets.sh (프로젝트 루트에서)
# 전제조건: LocalStack 실행 중, aws --profile localstack 설정 완료

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
AWS_PROFILE="localstack"

echo "🔐 Initializing LocalStack Secrets Manager & SSM..."

# LocalStack 상태 확인
if ! curl -sf http://localhost:4566/_localstack/health > /dev/null 2>&1; then
  echo "❌ LocalStack is not running. Start it first: docker compose -f docker-compose-local.yml up -d localstack"
  exit 1
fi

# ===== .env.local 로드 =====
AUTH_ENV="${PROJECT_ROOT}/services/auth-service/.env.local"
if [ -f "$AUTH_ENV" ]; then
  set -a
  source "$AUTH_ENV"
  set +a
  echo "✓ Loaded ${AUTH_ENV}"
else
  echo "⚠ ${AUTH_ENV} not found. Using environment variables or defaults."
fi

# ===== Helper: SM secret 생성 또는 업데이트 =====
upsert_secret() {
  local name="$1"
  local value="$2"
  local desc="$3"
  aws --profile $AWS_PROFILE secretsmanager create-secret \
    --name "$name" --secret-string "$value" --description "$desc" 2>/dev/null \
    && echo "  ✓ Created $name" \
    || { aws --profile $AWS_PROFILE secretsmanager update-secret \
      --secret-id "$name" --secret-string "$value" 2>/dev/null \
      && echo "  ✓ Updated $name"; }
}

# ===== Helper: SSM parameter 등록 =====
put_param() {
  local name="$1"
  local value="$2"
  aws --profile $AWS_PROFILE ssm put-parameter \
    --name "$name" --value "$value" --type String --overwrite 2>/dev/null \
    && echo "  ✓ $name"
}

# ===================================================================
# Secrets Manager (민감 정보)
# ===================================================================
echo ""
echo "🔑 Registering Secrets Manager secrets..."

# ----- auth-service -----
echo ""
echo "  [auth-service]"
upsert_secret "/portal-universe/local/auth-service/db-credentials" \
  "{\"username\":\"${DB_USER:-laze}\",\"password\":\"${DB_PASSWORD:-CHANGE_ME}\"}" \
  "auth-service PostgreSQL credentials"

upsert_secret "/portal-universe/local/auth-service/jwt-secret" \
  "{\"secret-key\":\"${JWT_SECRET_KEY:-your-256-bit-secret-key-for-jwt-signing-minimum-32-characters-required}\"}" \
  "auth-service JWT signing key"

upsert_secret "/portal-universe/local/auth-service/oauth-credentials" \
  "{\"google-client-id\":\"${GOOGLE_CLIENT_ID:-dummy}\",\"google-client-secret\":\"${GOOGLE_CLIENT_SECRET:-dummy}\",\"naver-client-id\":\"${NAVER_CLIENT_ID:-dummy}\",\"naver-client-secret\":\"${NAVER_CLIENT_SECRET:-dummy}\",\"kakao-client-id\":\"${KAKAO_CLIENT_ID:-dummy}\",\"kakao-client-secret\":\"${KAKAO_CLIENT_SECRET:-dummy}\"}" \
  "auth-service OAuth client credentials"

# ----- blog-service -----
echo ""
echo "  [blog-service]"
upsert_secret "/portal-universe/local/blog-service/db-credentials" \
  "{\"username\":\"${DB_USER:-laze}\",\"password\":\"${MONGO_PASSWORD:-${DB_PASSWORD:-CHANGE_ME}}\"}" \
  "blog-service MongoDB credentials"

# ----- shopping-service -----
echo ""
echo "  [shopping-service]"
upsert_secret "/portal-universe/local/shopping-service/db-credentials" \
  "{\"username\":\"${DB_USER:-laze}\",\"password\":\"${DB_PASSWORD:-CHANGE_ME}\"}" \
  "shopping-service PostgreSQL credentials"

# ----- shopping-seller-service -----
echo ""
echo "  [shopping-seller-service]"
upsert_secret "/portal-universe/local/shopping-seller-service/db-credentials" \
  "{\"username\":\"${DB_USER:-laze}\",\"password\":\"${DB_PASSWORD:-CHANGE_ME}\"}" \
  "shopping-seller-service PostgreSQL credentials"

# ----- shopping-settlement-service -----
echo ""
echo "  [shopping-settlement-service]"
upsert_secret "/portal-universe/local/shopping-settlement-service/db-credentials" \
  "{\"username\":\"${DB_USER:-laze}\",\"password\":\"${DB_PASSWORD:-CHANGE_ME}\"}" \
  "shopping-settlement-service PostgreSQL credentials"

# ----- notification-service -----
echo ""
echo "  [notification-service]"
upsert_secret "/portal-universe/local/notification-service/db-credentials" \
  "{\"username\":\"${DB_USER:-laze}\",\"password\":\"${DB_PASSWORD:-CHANGE_ME}\"}" \
  "notification-service MySQL credentials"

# ----- drive-service -----
echo ""
echo "  [drive-service]"
upsert_secret "/portal-universe/local/drive-service/db-credentials" \
  "{\"username\":\"${DB_USER:-laze}\",\"password\":\"${DB_PASSWORD:-CHANGE_ME}\"}" \
  "drive-service PostgreSQL credentials"

# ===================================================================
# SSM Parameter Store (일반 설정값)
# ===================================================================
echo ""
echo "📋 Registering SSM Parameter Store parameters..."

# ----- auth-service -----
echo ""
echo "  [auth-service]"
put_param "/portal-universe/local/auth-service/db-host" "localhost"
put_param "/portal-universe/local/auth-service/db-port" "5432"
put_param "/portal-universe/local/auth-service/db-name" "auth_db"
put_param "/portal-universe/local/auth-service/redis-host" "localhost"
put_param "/portal-universe/local/auth-service/redis-port" "6379"

# ----- shopping-service -----
echo ""
echo "  [shopping-service]"
put_param "/portal-universe/local/shopping-service/db-host" "localhost"
put_param "/portal-universe/local/shopping-service/db-port" "5432"
put_param "/portal-universe/local/shopping-service/db-name" "shopping_db"

# ----- shopping-seller-service -----
echo ""
echo "  [shopping-seller-service]"
put_param "/portal-universe/local/shopping-seller-service/db-host" "localhost"
put_param "/portal-universe/local/shopping-seller-service/db-port" "5432"
put_param "/portal-universe/local/shopping-seller-service/db-name" "shopping_seller_db"

# ----- shopping-settlement-service -----
echo ""
echo "  [shopping-settlement-service]"
put_param "/portal-universe/local/shopping-settlement-service/db-host" "localhost"
put_param "/portal-universe/local/shopping-settlement-service/db-port" "5432"
put_param "/portal-universe/local/shopping-settlement-service/db-name" "shopping_settlement_db"

# ----- notification-service -----
echo ""
echo "  [notification-service]"
put_param "/portal-universe/local/notification-service/db-host" "localhost"
put_param "/portal-universe/local/notification-service/db-port" "3307"
put_param "/portal-universe/local/notification-service/db-name" "notification_db"

# ----- drive-service -----
echo ""
echo "  [drive-service]"
put_param "/portal-universe/local/drive-service/db-host" "localhost"
put_param "/portal-universe/local/drive-service/db-port" "5432"
put_param "/portal-universe/local/drive-service/db-name" "drive_db"

# ----- blog-service -----
echo ""
echo "  [blog-service]"
put_param "/portal-universe/local/blog-service/db-host" "localhost"
put_param "/portal-universe/local/blog-service/db-port" "27017"
put_param "/portal-universe/local/blog-service/db-name" "blog_db"

echo ""
echo "✅ Done! Verify with:"
echo "  aws --profile localstack secretsmanager list-secrets --query 'SecretList[].Name' --output table"
echo "  aws --profile localstack ssm describe-parameters --query 'Parameters[].Name' --output table"
