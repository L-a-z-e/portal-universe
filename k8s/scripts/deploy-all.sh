#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# 색상
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🚀 Portal Universe - Deploy to Kubernetes${NC}"
echo -e "📂 Project root: $PROJECT_ROOT"
echo ""

# ============================================
# Step 0: Ingress Controller 설치
# ============================================
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

# ============================================
# Step 1: Base 설정
# ============================================
echo ""
echo -e "${YELLOW}📦 Step 1: Apply Base Configuration${NC}"

kubectl apply -f "$PROJECT_ROOT/k8s/base/namespace.yaml"
echo -e "${GREEN}✅ Namespace created${NC}"

kubectl apply -f "$PROJECT_ROOT/k8s/base/secret.yaml"
echo -e "${GREEN}✅ Secrets created${NC}"

# ============================================
# Step 2: Infrastructure 배포
# ============================================
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

# ============================================
# Step 3: Core Services 배포
# ============================================
echo ""
echo -e "${YELLOW}⚙️  Step 3: Deploy Core Services${NC}"

echo -e "${BLUE}Deploying discovery-service...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/services/discovery-service.yaml"
kubectl rollout status deployment/discovery-service -n portal-universe

echo -e "${BLUE}Deploying config-service...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/services/config-service.yaml"
kubectl rollout status deployment/config-service -n portal-universe

# ============================================
# Step 4: Business Services 배포
# ============================================
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

# ============================================
# Step 5: API Gateway 배포
# ============================================
echo ""
echo -e "${YELLOW}🌐 Step 5: Deploy API Gateway${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/services/api-gateway.yaml"
kubectl rollout status deployment/api-gateway -n portal-universe

# ============================================
# Step 6: Frontend 배포
# ============================================
echo ""
echo -e "${YELLOW}🎨 Step 6: Deploy Frontend${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/services/portal-shell.yaml"
kubectl rollout status deployment/portal-shell -n portal-universe

# ============================================
# Step 7: Ingress 배포
# ============================================
echo ""
echo -e "${YELLOW}🚪 Step 7: Deploy Ingress${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/ingress.yaml"
echo -e "${GREEN}✅ Ingress deployed${NC}"

# ============================================
# Step 8: 배포 확인
# ============================================
echo ""
echo -e "${YELLOW}📊 Step 8: Verify Deployment${NC}"
echo ""

echo "Pods:"
kubectl get pods -n portal-universe

echo ""
echo "Services:"
kubectl get svc -n portal-universe

echo ""
echo "Ingress:"
kubectl get ingress -n portal-universe

echo ""
echo "Ingress Controller:"
kubectl get svc -n ingress-nginx

echo ""
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo -e "${GREEN}🎉 Deployment completed!${NC}"
echo -e "${GREEN}════════════════════════════════════════${NC}"

# ============================================
# Step 9: 접속 정보
# ============================================
echo ""
NODE_PORT=$(kubectl get svc ingress-nginx-controller -n ingress-nginx -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")
pkill -f "port-forward.*ingress-nginx"
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80

echo -e "${YELLOW}📋 Access your application:${NC}"
echo ""
if [ "$NODE_PORT" != "N/A" ]; then
    echo -e "  ${BLUE}http://portal-universe:8080${NC}"
else
    echo -e "  ${RED}Unable to get NodePort${NC}"
fi
echo ""