#!/bin/bash

# =============================================================================
# delete-all.sh
#
# 역할:
#   Kubernetes 클러스터에 배포된 Portal Universe 관련 모든 리소스를 삭제합니다.
#   삭제 순서는 의존성의 역순(Ingress -> Frontend -> Backend -> Infra)으로 진행됩니다.
#
# 사용법:
#   ./k8s/scripts/delete-all.sh
#
# 주의:
#   이 스크립트는 되돌릴 수 없는 삭제 작업을 수행합니다. 신중하게 사용하세요.
#   Namespace와 Ingress Controller 삭제는 선택적으로 진행됩니다.
# =============================================================================

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# --- 색상 변수 ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${RED}🛑 Deleting Portal Universe Kubernetes Deployment${NC}"
echo -e "📂 Project root: $PROJECT_ROOT"
echo ""

# --- 1. Ingress 삭제 ---
echo -e "${YELLOW}🚪 Step 1: Delete Ingress${NC}"
kubectl delete -f "$PROJECT_ROOT/k8s/infrastructure/ingress.yaml" --ignore-not-found=true --timeout=30s
echo -e "${GREEN}✅ Ingress deleted${NC}"

# --- 2. Frontend 삭제 ---
echo ""
echo -e "${YELLOW}🎨 Step 2: Delete Frontend${NC}"
kubectl delete -f "$PROJECT_ROOT/k8s/services/portal-shell.yaml" --ignore-not-found=true --timeout=30s
echo -e "${GREEN}✅ Frontend deleted${NC}"

# --- 3. API Gateway 삭제 ---
echo ""
echo -e "${YELLOW}🌐 Step 3: Delete API Gateway${NC}"
kubectl delete -f "$PROJECT_ROOT/k8s/services/api-gateway.yaml" --ignore-not-found=true --timeout=30s
echo -e "${GREEN}✅ API Gateway deleted${NC}"

# --- 4. Business Services 삭제 ---
echo ""
echo -e "${YELLOW}💼 Step 4: Delete Business Services${NC}"

BUSINESS_SERVICES=(
    "notification-service"
    "shopping-service"
    "blog-service"
    "auth-service"
)

for SERVICE in "${BUSINESS_SERVICES[@]}"; do
    echo -e "${BLUE}Deleting ${SERVICE}...${NC}"
    kubectl delete -f "$PROJECT_ROOT/k8s/services/${SERVICE}.yaml" --ignore-not-found=true --timeout=30s
    echo -e "${GREEN}✅ ${SERVICE} deleted${NC}"
done

# --- 5. Infrastructure 삭제 ---
echo ""
echo -e "${YELLOW}🗄️  Step 5: Delete Infrastructure${NC}"

INFRA_SERVICES=(
    "kafka"
    "zipkin"
    "mongodb"
    "mysql-db"
)

for SERVICE in "${INFRA_SERVICES[@]}"; do
    echo -e "${BLUE}Deleting ${SERVICE}...${NC}"
    kubectl delete -f "$PROJECT_ROOT/k8s/infrastructure/${SERVICE}.yaml" --ignore-not-found=true --timeout=30s
    echo -e "${GREEN}✅ ${SERVICE} deleted${NC}"
done

# --- 6. Base 설정 삭제 ---
echo ""
echo -e "${YELLOW}🔐 Step 6: Delete Base Configuration${NC}"

kubectl delete -f "$PROJECT_ROOT/k8s/base/secret.yaml" --ignore-not-found=true --timeout=10s
echo -e "${GREEN}✅ Secrets deleted${NC}"

# --- 7. Namespace 삭제 (선택사항) ---
echo ""
echo -e "${YELLOW}📦 Step 7: Delete Namespace (Optional)${NC}"
read -p "❓ Delete namespace 'portal-universe'? (y/N): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}🗑️  Deleting namespace...${NC}"
    kubectl delete namespace portal-universe --timeout=60s 2>/dev/null || {
        echo -e "${YELLOW}⚠️  Namespace deletion timed out, forcing...${NC}"
        kubectl delete namespace portal-universe --grace-period=0 --force 2>/dev/null || true
    }
    echo -e "${GREEN}✅ Namespace deleted${NC}"
else
    echo -e "${BLUE}ℹ️  Namespace kept${NC}"
fi

# --- 8. Ingress Controller 삭제 (선택사항) ---
echo ""
echo -e "${YELLOW}🌐 Step 8: Delete Ingress Controller (Optional)${NC}"
read -p "❓ Delete Ingress Controller? (y/N): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${BLUE}🗑️  Deleting Ingress Controller...${NC}"
    kubectl delete namespace ingress-nginx --timeout=60s 2>/dev/null || {
        echo -e "${YELLOW}⚠️  Namespace deletion timed out, forcing...${NC}"
        kubectl delete namespace ingress-nginx --grace-period=0 --force 2>/dev/null || true
    }
    echo -e "${GREEN}✅ Ingress Controller deleted${NC}"
else
    echo -e "${BLUE}ℹ️  Ingress Controller kept${NC}"
fi

echo ""
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ Portal Universe deleted!${NC}"
echo -e "${GREEN}════════════════════════════════════════${NC}"
