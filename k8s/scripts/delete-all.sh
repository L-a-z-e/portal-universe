#!/bin/bash

# =============================================================================
# delete-all.sh
#
# 역할:
#   Kubernetes 클러스터에 배포된 Portal Universe 관련 모든 리소스를 삭제합니다.
#   삭제 순서는 의존성의 역순(Ingress -> Frontend -> Backend -> Infra)으로 진행됩니다.
#
# 사용법:
#   ./k8s/scripts/delete-all.sh                    # 레거시 모드 (개별 삭제)
#   ./k8s/scripts/delete-all.sh --overlay kind     # Kustomize Kind 리소스 삭제
#   ./k8s/scripts/delete-all.sh --overlay aws      # Kustomize AWS 리소스 삭제
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

# --- 인자 파싱 ---
OVERLAY=""
while [[ $# -gt 0 ]]; do
    case $1 in
        --overlay)
            OVERLAY="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [--overlay kind|aws]"
            echo ""
            echo "Options:"
            echo "  --overlay kind    Delete Kind overlay resources (Kustomize)"
            echo "  --overlay aws     Delete AWS overlay resources (Kustomize)"
            echo "  (no args)         Legacy mode (individual kubectl delete -f)"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Usage: $0 [--overlay kind|aws]"
            exit 1
            ;;
    esac
done

if [[ -n "$OVERLAY" && "$OVERLAY" != "kind" && "$OVERLAY" != "aws" ]]; then
    echo -e "${RED}Invalid overlay: $OVERLAY (must be 'kind' or 'aws')${NC}"
    exit 1
fi

# =============================================================================
# Kustomize 삭제 모드
# =============================================================================
if [[ -n "$OVERLAY" ]]; then
    OVERLAY_DIR="$PROJECT_ROOT/k8s/overlays/$OVERLAY"

    if [[ ! -d "$OVERLAY_DIR" ]]; then
        echo -e "${RED}Overlay directory not found: $OVERLAY_DIR${NC}"
        exit 1
    fi

    echo -e "${RED}🛑 Deleting Portal Universe (Kustomize: $OVERLAY)${NC}"
    echo -e "📂 Overlay: $OVERLAY_DIR"
    echo ""

    # Kustomize 리소스 일괄 삭제
    echo -e "${YELLOW}🗑️  Deleting all Kustomize resources...${NC}"
    kubectl delete -k "$OVERLAY_DIR" --ignore-not-found=true --timeout=60s 2>/dev/null || true
    echo -e "${GREEN}✅ Kustomize resources deleted${NC}"

    # Metrics Server 삭제 (Kind overlay — kube-system 네임스페이스)
    if [[ "$OVERLAY" == "kind" ]]; then
        echo ""
        echo -e "${YELLOW}📊 Deleting Metrics Server...${NC}"
        kubectl delete deployment metrics-server -n kube-system --ignore-not-found=true --timeout=30s 2>/dev/null || true
        kubectl delete service metrics-server -n kube-system --ignore-not-found=true --timeout=10s 2>/dev/null || true
        kubectl delete apiservice v1beta1.metrics.k8s.io --ignore-not-found=true --timeout=10s 2>/dev/null || true
        echo -e "${GREEN}✅ Metrics Server deleted${NC}"
    fi

    # Namespace 삭제 (선택)
    echo ""
    echo -e "${YELLOW}📦 Delete Namespace (Optional)${NC}"
    read -p "❓ Delete namespace 'portal-universe'? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        kubectl delete namespace portal-universe --timeout=60s 2>/dev/null || true
        echo -e "${GREEN}✅ Namespace deleted${NC}"
    else
        echo -e "${BLUE}ℹ️  Namespace kept${NC}"
    fi

    # Ingress Controller 삭제 (Kind만, 선택)
    if [[ "$OVERLAY" == "kind" ]]; then
        echo ""
        echo -e "${YELLOW}🌐 Delete Ingress Controller (Optional)${NC}"
        read -p "❓ Delete Ingress Controller? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            kubectl delete namespace ingress-nginx --timeout=60s 2>/dev/null || true
            echo -e "${GREEN}✅ Ingress Controller deleted${NC}"
        else
            echo -e "${BLUE}ℹ️  Ingress Controller kept${NC}"
        fi
    fi

    echo ""
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo -e "${GREEN}✅ Portal Universe deleted! (overlay: $OVERLAY)${NC}"
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    exit 0
fi

# =============================================================================
# 레거시 삭제 모드 (인자 없이 실행 시 기존 동작 유지)
# =============================================================================

echo -e "${RED}🛑 Deleting Portal Universe Kubernetes Deployment${NC}"
echo -e "📂 Project root: $PROJECT_ROOT"
echo ""

# --- 1. Ingress 삭제 ---
echo -e "${YELLOW}🚪 Step 1: Delete Ingress${NC}"
kubectl delete -f "$PROJECT_ROOT/k8s/infrastructure/ingress.yaml" --ignore-not-found=true --timeout=30s
echo -e "${GREEN}✅ Ingress deleted${NC}"

# --- 2. Network Policy 삭제 ---
echo ""
echo -e "${YELLOW}🔒 Step 2: Delete Network Policy${NC}"
kubectl delete -f "$PROJECT_ROOT/k8s/infrastructure/network-policy.yaml" --ignore-not-found=true --timeout=30s
echo -e "${GREEN}✅ Network Policy deleted${NC}"

# --- 3. Monitoring 삭제 ---
echo ""
echo -e "${YELLOW}📈 Step 3: Delete Monitoring${NC}"
kubectl delete -f "$PROJECT_ROOT/k8s/infrastructure/grafana.yaml" --ignore-not-found=true --timeout=30s
kubectl delete -f "$PROJECT_ROOT/k8s/infrastructure/prometheus.yaml" --ignore-not-found=true --timeout=30s
echo -e "${GREEN}✅ Monitoring deleted${NC}"

# --- 4. Frontend 삭제 ---
echo ""
echo -e "${YELLOW}🎨 Step 4: Delete Frontend${NC}"

FRONTEND_SERVICES=(
    "portal-shell"
    "shopping-seller-frontend"
    "drive-frontend"
    "admin-frontend"
    "prism-frontend"
    "shopping-frontend"
    "blog-frontend"
)

for SERVICE in "${FRONTEND_SERVICES[@]}"; do
    echo -e "${BLUE}Deleting ${SERVICE}...${NC}"
    kubectl delete -f "$PROJECT_ROOT/k8s/services/${SERVICE}.yaml" --ignore-not-found=true --timeout=30s
    echo -e "${GREEN}✅ ${SERVICE} deleted${NC}"
done

# --- 5. API Gateway 삭제 ---
echo ""
echo -e "${YELLOW}🌐 Step 5: Delete API Gateway${NC}"
kubectl delete -f "$PROJECT_ROOT/k8s/services/api-gateway.yaml" --ignore-not-found=true --timeout=30s
echo -e "${GREEN}✅ API Gateway deleted${NC}"

# --- 6. Business Services 삭제 ---
echo ""
echo -e "${YELLOW}💼 Step 6: Delete Business Services${NC}"

BUSINESS_SERVICES=(
    "chatbot-service"
    "prism-service"
    "drive-service"
    "notification-service"
    "shopping-settlement-service"
    "shopping-seller-service"
    "shopping-service"
    "blog-service"
    "auth-service"
)

for SERVICE in "${BUSINESS_SERVICES[@]}"; do
    echo -e "${BLUE}Deleting ${SERVICE}...${NC}"
    kubectl delete -f "$PROJECT_ROOT/k8s/services/${SERVICE}.yaml" --ignore-not-found=true --timeout=30s
    echo -e "${GREEN}✅ ${SERVICE} deleted${NC}"
done

# --- 7. Infrastructure 삭제 ---
echo ""
echo -e "${YELLOW}🗄️  Step 7: Delete Infrastructure${NC}"

INFRA_SERVICES=(
    "localstack"
    "elasticsearch"
    "redis"
    "zipkin"
    "kafka"
    "postgresql"
    "mongodb"
    "mysql-db"
)

for SERVICE in "${INFRA_SERVICES[@]}"; do
    echo -e "${BLUE}Deleting ${SERVICE}...${NC}"
    kubectl delete -f "$PROJECT_ROOT/k8s/infrastructure/${SERVICE}.yaml" --ignore-not-found=true --timeout=30s
    echo -e "${GREEN}✅ ${SERVICE} deleted${NC}"
done

# --- 8. Base 설정 삭제 ---
echo ""
echo -e "${YELLOW}🔐 Step 8: Delete Base Configuration${NC}"

kubectl delete -f "$PROJECT_ROOT/k8s/infrastructure/configmap.yaml" --ignore-not-found=true --timeout=10s
kubectl delete -f "$PROJECT_ROOT/k8s/base/jwt-secrets.yaml" --ignore-not-found=true --timeout=10s
kubectl delete -f "$PROJECT_ROOT/k8s/base/secret.yaml" --ignore-not-found=true --timeout=10s
if [ -f "$PROJECT_ROOT/k8s/base/tls-secret.yaml" ]; then
    kubectl delete -f "$PROJECT_ROOT/k8s/base/tls-secret.yaml" --ignore-not-found=true --timeout=10s
fi
echo -e "${GREEN}✅ Base configuration deleted${NC}"

# --- 9. Namespace 삭제 (선택사항) ---
echo ""
echo -e "${YELLOW}📦 Step 9: Delete Namespace (Optional)${NC}"
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

# --- 10. Ingress Controller 삭제 (선택사항) ---
echo ""
echo -e "${YELLOW}🌐 Step 10: Delete Ingress Controller (Optional)${NC}"
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
