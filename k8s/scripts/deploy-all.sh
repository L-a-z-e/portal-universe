#!/bin/bash

# =============================================================================
# deploy-all.sh
#
# 역할:
#   Portal Universe 애플리케이션의 모든 리소스를 Kubernetes 클러스터에 배포합니다.
#   의존성 순서에 따라 인프라 -> 핵심 서비스 -> 비즈니스 서비스 -> 게이트웨이 -> 프론트엔드 순으로 배포를 진행합니다.
#
# 사용법:
#   ./k8s/scripts/deploy-all.sh
#
# 전제조건:
#   - `build-and-load.sh` 스크립트가 먼저 실행되어 모든 Docker 이미지가 Kind 클러스터에 로드되어 있어야 합니다.
#   - `kubectl`이 올바른 클러스터를 대상으로 설정되어 있어야 합니다.
# =============================================================================

set -e # 오류 발생 시 즉시 스크립트를 중단합니다.

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# --- 색상 변수 ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🚀 Portal Universe - Deploy to Kubernetes${NC}"
echo -e "📂 Project root: $PROJECT_ROOT"
echo ""

# --- 0. Ingress Controller 설치 ---
echo -e "${YELLOW}🌐 Step 0: Install Ingress Controller${NC}"

if kubectl get namespace ingress-nginx &> /dev/null; then
    echo -e "${GREEN}✅ Ingress Controller already installed${NC}"
else
    echo "Installing Ingress Controller..."
    kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/ingress-controller.yaml"

    echo "Waiting for Ingress Controller to be ready..."
    kubectl wait --namespace ingress-nginx \
      --for=condition=ready pod \
      --selector=app.kubernetes.io/component=controller \
      --timeout=90s

    echo -e "${GREEN}✅ Ingress Controller installed${NC}"
fi

# --- 1. Base 설정 적용 ---
echo ""
echo -e "${YELLOW}📦 Step 1: Apply Base Configuration${NC}"

kubectl apply -f "$PROJECT_ROOT/k8s/base/namespace.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/base/secret.yaml"
echo -e "${GREEN}✅ Base configuration applied${NC}"

# --- 2. Infrastructure 배포 ---
echo ""
echo -e "${YELLOW}🗄️  Step 2: Deploy Infrastructure${NC}"

INFRA_SERVICES=(
    "mysql-db"
    "mongodb"
    "kafka"
    "zipkin"
)

for SERVICE in "${INFRA_SERVICES[@]}"; do
    echo -e "${BLUE}Deploying ${SERVICE}...${NC}"
    kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/${SERVICE}.yaml"
    echo -e "${GREEN}✅ ${SERVICE} deployed${NC}"
done

# --- 3. Core Services 배포 (순차적, 상태 확인) ---
echo ""
echo -e "${YELLOW}⚙️  Step 3: Deploy Core Services${NC}"

echo -e "${BLUE}Deploying discovery-service...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/services/discovery-service.yaml"
kubectl rollout status deployment/discovery-service -n portal-universe

echo -e "${BLUE}Deploying config-service...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/services/config-service.yaml"
kubectl rollout status deployment/config-service -n portal-universe

# --- 4. Business Services 배포 ---
echo ""
echo -e "${YELLOW}💼 Step 4: Deploy Business Services${NC}"

BUSINESS_SERVICES=(
    "auth-service"
    "blog-service"
    "shopping-service"
    "notification-service"
)

for SERVICE in "${BUSINESS_SERVICES[@]}"; do
    echo -e "${BLUE}Deploying ${SERVICE}...${NC}"
    kubectl apply -f "$PROJECT_ROOT/k8s/services/${SERVICE}.yaml"
    echo -e "${GREEN}✅ ${SERVICE} deployed${NC}"
done

# --- 5. API Gateway 배포 (상태 확인) ---
echo ""
echo -e "${YELLOW}🌐 Step 5: Deploy API Gateway${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/services/api-gateway.yaml"
kubectl rollout status deployment/api-gateway -n portal-universe

# --- 6. Frontend 배포 (상태 확인) ---
echo ""
echo -e "${YELLOW}🎨 Step 6: Deploy Frontend${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/services/portal-shell.yaml"
kubectl rollout status deployment/portal-shell -n portal-universe

# --- 7. Ingress 배포 ---
echo ""
echo -e "${YELLOW}🚪 Step 7: Deploy Ingress${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/ingress.yaml"
echo -e "${GREEN}✅ Ingress deployed${NC}"

# --- 8. 배포 결과 확인 ---
echo ""
echo -e "${YELLOW}📊 Step 8: Verify Deployment${NC}"
echo ""

kubectl get pods -n portal-universe

echo ""
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo -e "${GREEN}🎉 Deployment completed!${NC}"
echo -e "${GREEN}════════════════════════════════════════${NC}"

# --- 9. 접속 정보 및 Port Forwarding ---
echo ""
# 기존 port-forward 프로세스를 종료합니다.
pkill -f "port-forward.*ingress-nginx" 2>/dev/null || true
# Ingress Controller로 포트 포워딩을 시작하여 로컬에서 portal-universe:8080으로 접근할 수 있도록 합니다.
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80 > /dev/null 2>&1 &

echo -e "${YELLOW}📋 Access your application:${NC}"
echo ""
echo -e "  ${BLUE}http://portal-universe:8080${NC}"
echo ""
