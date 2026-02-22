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
kubectl apply -f "$PROJECT_ROOT/k8s/base/jwt-secrets.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/configmap.yaml"

# TLS Secret (mkcert 인증서) 적용 — 파일이 존재할 때만
if [ -f "$PROJECT_ROOT/k8s/base/tls-secret.yaml" ]; then
    kubectl apply -f "$PROJECT_ROOT/k8s/base/tls-secret.yaml"
    echo -e "${GREEN}✅ TLS Secret applied${NC}"
else
    echo -e "${YELLOW}⚠️  TLS Secret not found (k8s/base/tls-secret.yaml). HTTPS may use self-signed fallback.${NC}"
fi

echo -e "${GREEN}✅ Base configuration applied${NC}"

# --- 2. Infrastructure 배포 ---
echo ""
echo -e "${YELLOW}🗄️  Step 2: Deploy Infrastructure${NC}"

INFRA_SERVICES=(
    "mysql-db"
    "mongodb"
    "kafka"
    "zipkin"
    "redis"
    "elasticsearch"
    "postgresql"
    "localstack"
)

for SERVICE in "${INFRA_SERVICES[@]}"; do
    echo -e "${BLUE}Deploying ${SERVICE}...${NC}"
    kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/${SERVICE}.yaml"
    echo -e "${GREEN}✅ ${SERVICE} deployed${NC}"
done

# --- 2.5 Infrastructure Ready 대기 ---
echo ""
echo -e "${YELLOW}⏳ Step 2.5: Wait for Infrastructure to be Ready${NC}"

echo "Waiting for MySQL..."
kubectl wait --for=condition=ready pod -l app=mysql-db -n portal-universe --timeout=120s

echo "Waiting for MongoDB..."
kubectl wait --for=condition=ready pod -l app=mongodb -n portal-universe --timeout=120s

echo "Waiting for Kafka..."
kubectl wait --for=condition=ready pod -l app=kafka -n portal-universe --timeout=120s

echo "Waiting for Redis..."
kubectl wait --for=condition=ready pod -l app=redis -n portal-universe --timeout=120s

echo "Waiting for PostgreSQL..."
kubectl wait --for=condition=ready pod -l app=postgresql -n portal-universe --timeout=120s

echo "Waiting for Elasticsearch..."
kubectl wait --for=condition=ready pod -l app=elasticsearch -n portal-universe --timeout=180s

echo "Waiting for Zipkin..."
kubectl wait --for=condition=ready pod -l app=zipkin -n portal-universe --timeout=120s

echo -e "${GREEN}✅ All infrastructure services are ready${NC}"

# --- 3. Business Services 배포 ---
echo ""
echo -e "${YELLOW}💼 Step 3: Deploy Business Services${NC}"

BUSINESS_SERVICES=(
    "auth-service"
    "blog-service"
    "shopping-service"
    "shopping-seller-service"
    "shopping-settlement-service"
    "notification-service"
    "drive-service"
    "prism-service"
    "chatbot-service"
)

for SERVICE in "${BUSINESS_SERVICES[@]}"; do
    echo -e "${BLUE}Deploying ${SERVICE}...${NC}"
    kubectl apply -f "$PROJECT_ROOT/k8s/services/${SERVICE}.yaml"
    echo -e "${GREEN}✅ ${SERVICE} deployed${NC}"
done

# --- 4. API Gateway 배포 (상태 확인) ---
echo ""
echo -e "${YELLOW}🌐 Step 4: Deploy API Gateway${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/services/api-gateway.yaml"
kubectl rollout status deployment/api-gateway -n portal-universe --timeout=300s

# --- 5. Frontend 배포 (상태 확인) ---
echo ""
echo -e "${YELLOW}🎨 Step 5: Deploy Frontend${NC}"

FRONTEND_SERVICES=(
    "blog-frontend"
    "shopping-frontend"
    "prism-frontend"
    "admin-frontend"
    "drive-frontend"
    "shopping-seller-frontend"
    "portal-shell"
)

for SERVICE in "${FRONTEND_SERVICES[@]}"; do
    echo -e "${BLUE}Deploying ${SERVICE}...${NC}"
    kubectl apply -f "$PROJECT_ROOT/k8s/services/${SERVICE}.yaml"
    echo -e "${GREEN}✅ ${SERVICE} deployed${NC}"
done

kubectl rollout status deployment/portal-shell -n portal-universe --timeout=120s

# --- 6. Monitoring Services 배포 ---
echo ""
echo -e "${YELLOW}📈 Step 6: Deploy Monitoring Services${NC}"

echo -e "${BLUE}Deploying Prometheus...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/prometheus.yaml"
echo -e "${GREEN}✅ Prometheus deployed${NC}"

echo -e "${BLUE}Deploying Grafana...${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/grafana.yaml"
echo -e "${GREEN}✅ Grafana deployed${NC}"

# --- 7. Network Policy 배포 ---
echo ""
echo -e "${YELLOW}🔒 Step 7: Deploy Network Policy${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/network-policy.yaml"
echo -e "${GREEN}✅ Network Policy deployed${NC}"

# --- 8. Ingress 배포 ---
echo ""
echo -e "${YELLOW}🚪 Step 8: Deploy Ingress${NC}"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/ingress.yaml"
echo -e "${GREEN}✅ Ingress deployed${NC}"

# --- 9. 배포 결과 확인 ---
echo ""
echo -e "${YELLOW}📊 Step 9: Verify Deployment${NC}"
echo ""

kubectl get pods -n portal-universe

echo ""
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo -e "${GREEN}🎉 Deployment completed!${NC}"
echo -e "${GREEN}════════════════════════════════════════${NC}"

# --- 10. 접속 정보 ---
echo ""
echo -e "${YELLOW}📋 Access your application:${NC}"
echo ""
echo -e "  Kind extraPortMappings가 호스트 80/443을 직접 매핑합니다."
echo -e "  /etc/hosts에 '127.0.0.1 portal-universe'가 설정되어 있어야 합니다."
echo ""
echo -e "  ${BLUE}Main Application:${NC}  https://portal-universe"
echo -e "  ${BLUE}Grafana:${NC}           https://portal-universe/grafana"
echo -e "  ${BLUE}Prometheus:${NC}        https://portal-universe/prometheus"
echo -e "  ${BLUE}Zipkin:${NC}            https://portal-universe/zipkin"
echo ""
