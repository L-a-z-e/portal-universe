#!/bin/bash

# =============================================================================
# build-and-load.sh
#
# 역할:
#   모든 백엔드 및 프론트엔드 서비스를 빌드하고, Docker 이미지를 생성한 후,
#   로컬 Kind 클러스터에 이미지를 로드합니다.
#
# 사용법:
#   ./k8s/scripts/build-and-load.sh
#
# 실행 순서:
#   1. 모든 백엔드 서비스에 대해 Gradle 빌드를 실행합니다.
#   2. 모든 프론트엔드 서비스에 대해 npm 빌드를 실행합니다.
#   3. 빌드된 결과물을 사용하여 각 서비스의 Docker 이미지를 생성합니다.
#   4. 생성된 모든 Docker 이미지를 Kind 클러스터로 로드합니다.
# =============================================================================

set -e  # 오류 발생 시 즉시 스크립트를 중단합니다.

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# --- 색상 변수 ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# --- 빌드 대상 서비스 목록 ---
BACKEND_SERVICES=(
    "api-gateway"
    "auth-service"
    "blog-service"
    "shopping-service"
    "shopping-seller-service"
    "shopping-settlement-service"
    "notification-service"
    "drive-service"
)

FRONTEND_SERVICES=(
    "portal-shell"
    "blog-frontend"
    "shopping-frontend"
    "prism-frontend"
    "admin-frontend"
    "drive-frontend"
    "shopping-seller-frontend"
)

CLUSTER_NAME="portal-universe"

echo -e "${BLUE}🚀 Portal Universe - Build & Load to Kind${NC}"
echo ""

# --- 1. 백엔드: Gradle 빌드 ---
echo -e "${YELLOW}📦 Step 1: Gradle Build (Backend Services)${NC}"
cd "$PROJECT_ROOT"

for SERVICE in "${BACKEND_SERVICES[@]}"; do
    echo -e "${BLUE}Building ${SERVICE}...${NC}"
    ./gradlew :services:${SERVICE}:clean :services:${SERVICE}:build -x test

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ ${SERVICE} built${NC}"
    else
        echo -e "${RED}❌ ${SERVICE} build failed${NC}"
        exit 1
    fi
done

# --- 2. 프론트엔드: npm 빌드 ---
echo ""
echo -e "${YELLOW}📦 Step 2: npm Build (Frontend Services)${NC}"

# 먼저 frontend 루트에서 의존성 설치
cd "$PROJECT_ROOT/frontend"
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}  Installing npm dependencies (root)...${NC}"
    npm ci
fi

# design-system을 먼저 빌드 (다른 프론트엔드 서비스의 의존성)
echo -e "${BLUE}Building design-system...${NC}"
npm run build:design

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ design-system built${NC}"
else
    echo -e "${RED}❌ design-system build failed${NC}"
    exit 1
fi

# bridge 라이브러리 빌드 (vue-bridge → react-bridge → react-bootstrap)
echo -e "${BLUE}Building bridge libraries...${NC}"
npm run build:libs

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ bridge libraries built${NC}"
else
    echo -e "${RED}❌ bridge libraries build failed${NC}"
    exit 1
fi

# 프론트엔드 서비스 빌드
for SERVICE in "${FRONTEND_SERVICES[@]}"; do
    echo -e "${BLUE}Building ${SERVICE}...${NC}"
    cd "$PROJECT_ROOT/frontend/${SERVICE}"

    npm run build:k8s

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ ${SERVICE} built${NC}"
    else
        echo -e "${RED}❌ ${SERVICE} build failed${NC}"
        exit 1
    fi
    cd "$PROJECT_ROOT"
done

# --- 2.5. NestJS (Prism Service) 빌드 ---
echo ""
echo -e "${YELLOW}📦 Step 2.5: NestJS Build (Prism Service)${NC}"
cd "$PROJECT_ROOT/services/prism-service"
npm ci
npm run build
echo -e "${GREEN}✅ prism-service built${NC}"
cd "$PROJECT_ROOT"

# --- 3. Docker 이미지 빌드 (백엔드 - Spring Boot) ---
echo ""
echo -e "${YELLOW}🐳 Step 3: Docker Build (Backend Services)${NC}"

for SERVICE in "${BACKEND_SERVICES[@]}"; do
    echo -e "${BLUE}Building Docker image: ${SERVICE}...${NC}"

    docker build \
        -t portal-universe-${SERVICE}:latest \
        -f services/${SERVICE}/Dockerfile \
        .

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ ${SERVICE} image built${NC}"
    else
        echo -e "${RED}❌ ${SERVICE} image build failed${NC}"
        exit 1
    fi
done

# --- 3.5. Docker 이미지 빌드 (NestJS - Prism Service) ---
echo ""
echo -e "${YELLOW}🐳 Step 3.5: Docker Build (Prism Service)${NC}"

docker build \
    -t portal-universe-prism-service:latest \
    -f services/prism-service/Dockerfile \
    services/prism-service/

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ prism-service image built${NC}"
else
    echo -e "${RED}❌ prism-service image build failed${NC}"
    exit 1
fi

# --- 3.6. Docker 이미지 빌드 (Python - Chatbot Service) ---
echo ""
echo -e "${YELLOW}🐳 Step 3.6: Docker Build (Chatbot Service)${NC}"

docker build \
    -t portal-universe-chatbot-service:latest \
    -f services/chatbot-service/Dockerfile \
    services/chatbot-service/

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ chatbot-service image built${NC}"
else
    echo -e "${RED}❌ chatbot-service image build failed${NC}"
    exit 1
fi

# --- 4. Docker 이미지 빌드 (프론트엔드) ---
echo ""
echo -e "${YELLOW}🐳 Step 4: Docker Build (Frontend Services)${NC}"

for SERVICE in "${FRONTEND_SERVICES[@]}"; do
    echo -e "${BLUE}Building Docker image: ${SERVICE}...${NC}"

    # frontend/ 디렉토리를 빌드 컨텍스트로 사용 (workspace 루트 기준 COPY)
    docker build \
        --build-arg BUILD_MODE=k8s \
        -t portal-universe-${SERVICE}:latest \
        -f frontend/${SERVICE}/Dockerfile \
        frontend/

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ ${SERVICE} image built${NC}"
    else
        echo -e "${RED}❌ ${SERVICE} image build failed${NC}"
        exit 1
    fi
done

# --- 4.5 Docker 이미지 빌드 (Elasticsearch custom) ---
echo ""
echo -e "${YELLOW}🐳 Step 4.5: Docker Build (Elasticsearch)${NC}"

docker build \
    -t portal-universe-elasticsearch:v1.0.0 \
    -f infrastructure/elasticsearch/Dockerfile \
    infrastructure/elasticsearch/

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ elasticsearch image built${NC}"
else
    echo -e "${RED}❌ elasticsearch image build failed${NC}"
    exit 1
fi

# --- 5. Kind 클러스터에 이미지 로드 ---
echo ""
echo -e "${YELLOW}📥 Step 5: Load Images to Kind Cluster${NC}"

ALL_SERVICES=("${BACKEND_SERVICES[@]}" "prism-service" "chatbot-service" "${FRONTEND_SERVICES[@]}")

for SERVICE in "${ALL_SERVICES[@]}"; do
    echo -e "${BLUE}Loading ${SERVICE} to Kind...${NC}"

    kind load docker-image portal-universe-${SERVICE}:latest --name ${CLUSTER_NAME}

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ ${SERVICE} loaded to Kind${NC}"
    else
        echo -e "${RED}❌ ${SERVICE} load failed${NC}"
        exit 1
    fi
done

# Elasticsearch (다른 태그)
echo -e "${BLUE}Loading elasticsearch to Kind...${NC}"
kind load docker-image portal-universe-elasticsearch:v1.0.0 --name ${CLUSTER_NAME}
echo -e "${GREEN}✅ elasticsearch loaded to Kind${NC}"

echo ""
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo -e "${GREEN}🎉 All services built and loaded!${NC}"
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}📋 Next steps:${NC}"
echo "  1. Deploy: ${BLUE}./k8s/scripts/deploy-all.sh${NC}"
echo "  2. Check:  ${BLUE}kubectl get pods -n portal-universe${NC}"
echo ""
