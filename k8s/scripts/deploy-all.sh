#!/bin/bash

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
# 1. Namespace 생성
# ============================================
echo -e "${YELLOW}📦 Step 1: Create Namespace${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/namespace.yaml"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Namespace created/updated${NC}"
else
    echo -e "${RED}❌ Namespace creation failed${NC}"
    exit 1
fi

# ============================================
# 2. Secrets 생성
# ============================================
echo ""
echo -e "${YELLOW}🔐 Step 2: Create Secrets${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/secret.yaml"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Secrets created/updated${NC}"
else
    echo -e "${RED}❌ Secret creation failed${NC}"
    exit 1
fi

# ============================================
# 3. Infrastructure 배포
# ============================================
echo ""
echo -e "${YELLOW}🗄️  Step 3: Deploy Infrastructure${NC}"

INFRA_SERVICES=(
    "mysql-db"
    "mongodb"
    "kafka"
    "zipkin"
)

for SERVICE in "${INFRA_SERVICES[@]}"; do
    echo -e "${BLUE}Deploying ${SERVICE}...${NC}"
    kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/${SERVICE}.yaml"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ ${SERVICE} deployed${NC}"
    else
        echo -e "${RED}❌ ${SERVICE} deployment failed${NC}"
        exit 1
    fi
done

# ============================================
# 4. Core Services 배포
# ============================================
echo ""
echo -e "${YELLOW}⚙️  Step 4: Deploy Core Services${NC}"

echo -e "${BLUE}Deploying discovery-service...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/services/discovery-service.yaml"
kubectl rollout status deployment/discovery-service -n portal-universe --timeout=300s

echo -e "${BLUE}Deploying config-service...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/services/config-service.yaml"
kubectl rollout status deployment/config-service -n portal-universe --timeout=300s

# ============================================
# 5. Business Services 배포
# ============================================
echo ""
echo -e "${YELLOW}💼 Step 5: Deploy Business Services${NC}"

BUSINESS_SERVICES=(
    "auth-service"
    "blog-service"
    "shopping-service"
    "notification-service"
)

for SERVICE in "${BUSINESS_SERVICES[@]}"; do
    echo -e "${BLUE}Deploying ${SERVICE}...${NC}"
    kubectl apply -f "$PROJECT_ROOT/k8s/services/${SERVICE}.yaml"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ ${SERVICE} deployed${NC}"
    else
        echo -e "${RED}❌ ${SERVICE} deployment failed${NC}"
        exit 1
    fi
done

# ============================================
# 6. API Gateway 배포
# ============================================
echo ""
echo -e "${YELLOW}🌐 Step 6: Deploy API Gateway${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/services/api-gateway.yaml"
kubectl rollout status deployment/api-gateway -n portal-universe --timeout=300s

# ============================================
# 7. Frontend 배포
# ============================================
echo ""
echo -e "${YELLOW}🎨 Step 7: Deploy Frontend${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/services/portal-shell.yaml"
kubectl rollout status deployment/portal-shell -n portal-universe --timeout=300s

# ============================================
# 8. Ingress 배포
# ============================================
echo ""
echo -e "${YELLOW}🚪 Step 8: Deploy Ingress${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/ingress.yaml"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Ingress deployed${NC}"
else
    echo -e "${RED}❌ Ingress deployment failed${NC}"
    exit 1
fi

# ============================================
# 9. 배포 확인
# ============================================
echo ""
echo -e "${YELLOW}📊 Step 9: Verify Deployment${NC}"
echo ""
echo -e "${BLUE}Pods:${NC}"
kubectl get pods -n portal-universe

echo ""
echo -e "${BLUE}Services:${NC}"
kubectl get svc -n portal-universe

echo ""
echo -e "${BLUE}Ingress:${NC}"
kubectl get ingress -n portal-universe

echo ""
echo -e "${GREEN}🎉 Deployment completed!${NC}"
echo ""
echo -e "${YELLOW}📋 Access your application:${NC}"
echo "  Frontend:    http://portal-universe"
echo "  API Gateway: http://portal-universe/api"
echo "  Auth:        http://portal-universe/auth-service"
echo ""
echo -e "${YELLOW}⚠️  Don't forget to add to /etc/hosts:${NC}"
echo "  127.0.0.1 portal-universe"
echo ""