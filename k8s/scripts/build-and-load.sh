#!/bin/bash

set -e  # 오류 시 중단

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# 색상
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Backend 서비스 (Gradle, services/ 아래)
BACKEND_SERVICES=(
    "discovery-service"
    "config-service"
    "api-gateway"
    "auth-service"
    "blog-service"
    "shopping-service"
    "notification-service"
)

# Frontend 서비스 (npm, frontend/ 아래)
FRONTEND_SERVICES=(
    "portal-shell"
    # "blog-frontend"      # 추후 추가
    # "shopping-frontend"  # 추후 추가
)

CLUSTER_NAME="portal-universe"

echo -e "${BLUE}🚀 Portal Universe - Build & Load to Kind${NC}"
echo ""

# ============================================
# 1. Backend: Gradle 빌드
# ============================================
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

# ============================================
# 2. Frontend: npm 빌드
# ============================================
echo ""
echo -e "${YELLOW}📦 Step 2: npm Build (Frontend Services)${NC}"

for SERVICE in "${FRONTEND_SERVICES[@]}"; do
    echo -e "${BLUE}Building ${SERVICE}...${NC}"

    # frontend/${SERVICE} 디렉토리로 이동
    cd "$PROJECT_ROOT/frontend/${SERVICE}"

    # npm 의존성이 없으면 설치
    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}  Installing npm dependencies...${NC}"
        npm ci
    fi

    # k8s용 빌드
    npm run build:k8s

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ ${SERVICE} built${NC}"
    else
        echo -e "${RED}❌ ${SERVICE} build failed${NC}"
        exit 1
    fi

    # 프로젝트 루트로 돌아가기
    cd "$PROJECT_ROOT"
done

# ============================================
# 3. Docker 이미지 빌드 (Backend)
# ============================================
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

# ============================================
# 4. Docker 이미지 빌드 (Frontend)
# ============================================
echo ""
echo -e "${YELLOW}🐳 Step 4: Docker Build (Frontend Services)${NC}"

for SERVICE in "${FRONTEND_SERVICES[@]}"; do
    echo -e "${BLUE}Building Docker image: ${SERVICE}...${NC}"

    # frontend/${SERVICE} 디렉토리로 이동
    cd "$PROJECT_ROOT/frontend/${SERVICE}"

    docker build \
        --build-arg BUILD_MODE=k8s \
        -t portal-universe-${SERVICE}:latest \
        -f Dockerfile \
        .

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ ${SERVICE} image built${NC}"
    else
        echo -e "${RED}❌ ${SERVICE} image build failed${NC}"
        exit 1
    fi

    # 프로젝트 루트로 돌아가기
    cd "$PROJECT_ROOT"
done

# ============================================
# 5. Kind 클러스터에 이미지 로드
# ============================================
echo ""
echo -e "${YELLOW}📥 Step 5: Load Images to Kind Cluster${NC}"

ALL_SERVICES=("${BACKEND_SERVICES[@]}" "${FRONTEND_SERVICES[@]}")

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

echo ""
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo -e "${GREEN}🎉 All services built and loaded!${NC}"
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo ""
echo -e "${YELLOW}📋 Next steps:${NC}"
echo "  1. Deploy: ${BLUE}./k8s/scripts/deploy-all.sh${NC}"
echo "  2. Check:  ${BLUE}kubectl get pods -n portal-universe${NC}"
echo ""