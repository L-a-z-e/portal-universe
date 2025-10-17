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

SERVICES=(
    "discovery-service"
    "config-service"
    "api-gateway"
    "auth-service"
    "blog-service"
    "shopping-service"
    "notification-service"
)

CLUSTER_NAME="portal-universe"  # Kind 클러스터 이름

echo -e "${BLUE}🚀 Portal Universe - Build & Load to Kind${NC}"
echo ""

# 1. Gradle 빌드
echo -e "${YELLOW}📦 Step 1: Gradle Build${NC}"
cd "$PROJECT_ROOT"

for SERVICE in "${SERVICES[@]}"; do
    echo -e "${BLUE}Building ${SERVICE}...${NC}"
    ./gradlew :services:${SERVICE}:clean :services:${SERVICE}:build -x test

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ ${SERVICE} built${NC}"
    else
        echo -e "${RED}❌ ${SERVICE} build failed${NC}"
        exit 1
    fi
done

# 2. Docker 이미지 빌드
echo ""
echo -e "${YELLOW}🐳 Step 2: Docker Build (local only)${NC}"

for SERVICE in "${SERVICES[@]}"; do
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

# 3. Kind 클러스터에 이미지 로드
echo ""
echo -e "${YELLOW}📥 Step 3: Load images to Kind cluster${NC}"

for SERVICE in "${SERVICES[@]}"; do
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
echo -e "${GREEN}🎉 All services built and loaded to Kind!${NC}"
echo ""
echo -e "${YELLOW}📋 Next steps:${NC}"
echo "  1. Deploy: ./k8s/scripts/deploy-all.sh"
echo "  2. Check: kubectl get pods -n portal-universe"