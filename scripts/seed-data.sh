#!/usr/bin/env bash
#
# seed-data.sh - Shopping 서비스 샘플 데이터 생성
#
# Docker 환경에서 Admin API를 호출해 상품, 카테고리, 쿠폰, 타임딜 데이터를 생성합니다.
# 이미 존재하는 데이터는 건너뜁니다 (멱등성).
#
# Usage:
#   ./scripts/seed-data.sh              # 기본 (https://localhost:30000)
#   BASE_URL=http://localhost:8080 ./scripts/seed-data.sh  # Gateway 직접
#
set -euo pipefail

BASE_URL="${BASE_URL:-https://localhost:30000}"
CURL_OPTS="-s -k"  # -k: self-signed cert 허용

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_ok()   { echo -e "${GREEN}✅ $1${NC}"; }
log_skip() { echo -e "${YELLOW}⏭️  $1${NC}"; }
log_fail() { echo -e "${RED}❌ $1${NC}"; }
log_info() { echo -e "   $1"; }

# ─────────────────────────────────────────────────
# 1. Admin 로그인
# ─────────────────────────────────────────────────
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🌱 Shopping 시드 데이터 생성"
echo "   BASE_URL: $BASE_URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "🔐 Admin 로그인..."
LOGIN_RESP=$(curl $CURL_OPTS -X POST "$BASE_URL/auth-service/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}')

ACCESS_TOKEN=$(echo "$LOGIN_RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    token = d.get('data',{}).get('accessToken') or d.get('accessToken','')
    print(token)
except:
    print('')
" 2>/dev/null)

if [ -z "$ACCESS_TOKEN" ]; then
  log_fail "Admin 로그인 실패. auth-service가 실행 중인지 확인하세요."
  echo "  응답: $LOGIN_RESP"
  exit 1
fi
log_ok "Admin 로그인 성공"
echo ""

AUTH_HEADER="Authorization: Bearer $ACCESS_TOKEN"

# Helper: POST with JSON
api_post() {
  local url="$1"
  local data="$2"
  curl $CURL_OPTS -X POST "$BASE_URL$url" \
    -H "$AUTH_HEADER" \
    -H "Content-Type: application/json" \
    -d "$data"
}

# Helper: GET
api_get() {
  local url="$1"
  curl $CURL_OPTS -X GET "$BASE_URL$url" \
    -H "$AUTH_HEADER" \
    -H "Accept: application/json"
}

# ─────────────────────────────────────────────────
# 2. 상품 생성 (10개)
# ─────────────────────────────────────────────────
echo "📦 상품 생성..."

# 기존 상품 확인
EXISTING=$(api_get "/api/v1/shopping/products?page=0&size=100")
EXISTING_COUNT=$(echo "$EXISTING" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    content = d.get('data',{}).get('content',[])
    print(len(content))
except:
    print(0)
" 2>/dev/null)

if [ "$EXISTING_COUNT" -gt "5" ] 2>/dev/null; then
  log_skip "이미 상품 ${EXISTING_COUNT}개 존재. 건너뜁니다."
else
  PRODUCTS=(
    '{"name":"프리미엄 노트북 Pro 16","description":"16인치 고성능 노트북. M3 Pro 칩, 36GB RAM, 512GB SSD. 개발자와 크리에이터를 위한 최고의 선택.","price":2490000,"stock":50}'
    '{"name":"무선 블루투스 이어폰","description":"액티브 노이즈 캔슬링, 30시간 배터리, IPX5 방수. 통화 품질 최상.","price":189000,"stock":200}'
    '{"name":"기계식 키보드 RGB","description":"체리 MX 브라운 스위치, 풀 RGB 백라이트, 알루미늄 하우징. PBT 키캡 포함.","price":129000,"stock":150}'
    '{"name":"4K 울트라와이드 모니터 34인치","description":"3440x1440 해상도, 144Hz, 1ms 응답속도. USB-C 90W 충전 지원.","price":799000,"stock":30}'
    '{"name":"에르고노믹 오피스 체어","description":"메시 등받이, 4D 팔걸이, 허리 지지대 조절. 12시간 편안한 착석.","price":459000,"stock":80}'
    '{"name":"스마트 워치 Ultra","description":"GPS, 심박수, 혈중산소, 수면 추적. 10일 배터리. 티타늄 케이스.","price":599000,"stock":100}'
    '{"name":"포터블 SSD 2TB","description":"읽기 2000MB/s, USB 3.2 Gen 2x2. 충격 방지, 비밀번호 보호.","price":219000,"stock":300}'
    '{"name":"프리미엄 백팩","description":"방수 원단, 15.6인치 노트북 수납, USB 충전 포트. 출장과 일상 겸용.","price":89000,"stock":500}'
    '{"name":"LED 데스크 램프","description":"색온도 5단계, 밝기 10단계 조절. 무선 충전 패드 내장. 눈 보호 설계.","price":59000,"stock":400}'
    '{"name":"휴대용 블루투스 스피커","description":"360도 사운드, 24시간 배터리, IP67 방수방진. 듀얼 페어링 지원.","price":79000,"stock":250}'
  )

  CREATED_IDS=()
  for product in "${PRODUCTS[@]}"; do
    NAME=$(echo "$product" | python3 -c "import sys,json; print(json.load(sys.stdin)['name'])" 2>/dev/null)
    RESP=$(api_post "/api/v1/shopping/admin/products" "$product")
    ID=$(echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('data',{}).get('id',''))
except:
    print('')
" 2>/dev/null)

    if [ -n "$ID" ] && [ "$ID" != "None" ]; then
      log_ok "상품 생성: $NAME (id: $ID)"
      CREATED_IDS+=("$ID")
    else
      log_fail "상품 생성 실패: $NAME"
      log_info "$(echo "$RESP" | head -c 200)"
    fi
  done
fi

# 상품 ID 목록 (타임딜용)
PRODUCT_IDS=$(api_get "/api/v1/shopping/products?page=0&size=10" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    ids = [str(p['id']) for p in d.get('data',{}).get('content',[])]
    print(' '.join(ids))
except:
    print('')
" 2>/dev/null)

echo ""

# ─────────────────────────────────────────────────
# 3. 쿠폰 생성 (4개)
# ─────────────────────────────────────────────────
echo "🎟️  쿠폰 생성..."

NOW=$(date -u +"%Y-%m-%dT%H:%M:%S")
MONTH_LATER=$(date -u -v+30d +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || date -u -d "+30 days" +"%Y-%m-%dT%H:%M:%S" 2>/dev/null)
WEEK_LATER=$(date -u -v+7d +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || date -u -d "+7 days" +"%Y-%m-%dT%H:%M:%S" 2>/dev/null)

COUPONS=(
  "{\"code\":\"WELCOME10\",\"name\":\"신규 가입 10% 할인\",\"description\":\"회원가입을 축하합니다! 전 상품 10% 할인\",\"discountType\":\"PERCENTAGE\",\"discountValue\":10,\"minimumOrderAmount\":10000,\"maximumDiscountAmount\":50000,\"totalQuantity\":1000,\"startsAt\":\"$NOW\",\"expiresAt\":\"$MONTH_LATER\"}"
  "{\"code\":\"FLAT5000\",\"name\":\"5000원 즉시 할인\",\"description\":\"3만원 이상 구매 시 5000원 할인\",\"discountType\":\"FIXED\",\"discountValue\":5000,\"minimumOrderAmount\":30000,\"maximumDiscountAmount\":5000,\"totalQuantity\":500,\"startsAt\":\"$NOW\",\"expiresAt\":\"$MONTH_LATER\"}"
  "{\"code\":\"WEEKEND20\",\"name\":\"주말 특별 20% 할인\",\"description\":\"이번 주 한정! 최대 10만원 할인\",\"discountType\":\"PERCENTAGE\",\"discountValue\":20,\"minimumOrderAmount\":50000,\"maximumDiscountAmount\":100000,\"totalQuantity\":200,\"startsAt\":\"$NOW\",\"expiresAt\":\"$WEEK_LATER\"}"
  "{\"code\":\"PREMIUM30\",\"name\":\"프리미엄 회원 30% 할인\",\"description\":\"프리미엄 회원 전용 특별 할인\",\"discountType\":\"PERCENTAGE\",\"discountValue\":30,\"minimumOrderAmount\":100000,\"maximumDiscountAmount\":200000,\"totalQuantity\":50,\"startsAt\":\"$NOW\",\"expiresAt\":\"$MONTH_LATER\"}"
)

for coupon in "${COUPONS[@]}"; do
  CODE=$(echo "$coupon" | python3 -c "import sys,json; print(json.load(sys.stdin)['code'])" 2>/dev/null)
  RESP=$(api_post "/api/v1/shopping/admin/coupons" "$coupon")
  SUCCESS=$(echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print('true' if d.get('success') else 'false')
except:
    print('false')
" 2>/dev/null)

  if [ "$SUCCESS" = "true" ]; then
    log_ok "쿠폰 생성: $CODE"
  else
    ERROR=$(echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    e = d.get('error',{})
    print(e.get('message','') if isinstance(e,dict) else str(e)[:100])
except:
    print('unknown')
" 2>/dev/null)
    if echo "$ERROR" | grep -qi "exist\|duplicate\|already"; then
      log_skip "쿠폰 이미 존재: $CODE"
    else
      log_fail "쿠폰 생성 실패: $CODE ($ERROR)"
    fi
  fi
done

echo ""

# ─────────────────────────────────────────────────
# 4. 타임딜 생성 (2개)
# ─────────────────────────────────────────────────
echo "⏰ 타임딜 생성..."

HOUR_LATER=$(date -u -v+1H +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || date -u -d "+1 hour" +"%Y-%m-%dT%H:%M:%S" 2>/dev/null)
DAY_LATER=$(date -u -v+1d +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || date -u -d "+1 day" +"%Y-%m-%dT%H:%M:%S" 2>/dev/null)
TWO_DAYS=$(date -u -v+2d +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || date -u -d "+2 days" +"%Y-%m-%dT%H:%M:%S" 2>/dev/null)

# 상품 ID가 있으면 타임딜 생성
read -ra PID_ARRAY <<< "$PRODUCT_IDS"

if [ ${#PID_ARRAY[@]} -ge 2 ]; then
  PID1=${PID_ARRAY[0]}
  PID2=${PID_ARRAY[1]}

  TIMEDEALS=(
    "{\"name\":\"오늘의 특가 - 노트북\",\"description\":\"하루만! 프리미엄 노트북 30% 할인\",\"startsAt\":\"$HOUR_LATER\",\"endsAt\":\"$DAY_LATER\",\"products\":[{\"productId\":$PID1,\"dealPrice\":1743000,\"dealQuantity\":10,\"maxPerUser\":1}]}"
    "{\"name\":\"주간 베스트 - 이어폰 & 키보드\",\"description\":\"인기 상품 2종 특가\",\"startsAt\":\"$DAY_LATER\",\"endsAt\":\"$TWO_DAYS\",\"products\":[{\"productId\":$PID2,\"dealPrice\":132300,\"dealQuantity\":20,\"maxPerUser\":2}]}"
  )

  for timedeal in "${TIMEDEALS[@]}"; do
    NAME=$(echo "$timedeal" | python3 -c "import sys,json; print(json.load(sys.stdin)['name'])" 2>/dev/null)
    RESP=$(api_post "/api/v1/shopping/admin/time-deals" "$timedeal")
    SUCCESS=$(echo "$RESP" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print('true' if d.get('success') else 'false')
except:
    print('false')
" 2>/dev/null)

    if [ "$SUCCESS" = "true" ]; then
      log_ok "타임딜 생성: $NAME"
    else
      log_fail "타임딜 생성 실패: $NAME"
      log_info "$(echo "$RESP" | head -c 200)"
    fi
  done
else
  log_skip "상품이 부족하여 타임딜 생성을 건너뜁니다."
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🎉 시드 데이터 생성 완료!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
