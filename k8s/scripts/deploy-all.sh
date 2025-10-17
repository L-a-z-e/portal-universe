#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ========== 빌드 & 로드 ==========
echo -e "${YELLOW}🔨 Building and loading images...${NC}"
bash "$SCRIPT_DIR/build-and-load.sh"

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build & Load failed${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✅ Images ready, starting deployment...${NC}"
echo ""

# Pod가 Ready 상태가 될 때까지 대기하는 함수
wait_for_pod() {
    local label=$1
    local timeout=${2:-180}
    local namespace="portal-universe"

    echo -e "${YELLOW}⏳ Waiting for pod with label ${label} to be ready...${NC}"

    local elapsed=0
    while [ $elapsed -lt $timeout ]; do
        local ready=$(kubectl get pods -n $namespace -l $label -o jsonpath='{.items[0].status.containerStatuses[0].ready}' 2>/dev/null)

        if [ "$ready" == "true" ]; then
            echo -e "${GREEN}✅ Pod ${label} is ready!${NC}"
            return 0
        fi

        echo -n "."
        sleep 5
        elapsed=$((elapsed + 5))
    done

    echo -e "${RED}❌ Timeout waiting for ${label}${NC}"
    return 1
}

echo "🚀 Starting Portal Universe Kubernetes Deployment..."
echo "📂 Project root: $PROJECT_ROOT"
echo ""

# 1. Namespace 생성
echo "📦 Creating namespace..."
kubectl create namespace portal-universe --dry-run=client -o yaml | kubectl apply -f -

# 2. Secrets 적용
echo "🔐 Applying secrets..."
kubectl apply -f "$PROJECT_ROOT/k8s/secret.yaml"

# 3. Infrastructure 배포
echo ""
echo "🗄️  Deploying infrastructure..."
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/mysql-db.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/mongodb.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/kafka.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/zipkin.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/prometheus.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/infrastructure/grafana.yaml"

wait_for_pod "app=mysql-db" 180
wait_for_pod "app=mongodb" 180
wait_for_pod "app=kafka" 180
wait_for_pod "app=zipkin" 180
wait_for_pod "app=prometheus" 180
wait_for_pod "app=grafana" 180

# 4. Discovery Service 배포
echo ""
echo "🔍 Deploying discovery service..."
kubectl apply -f "$PROJECT_ROOT/k8s/services/discovery-service.yaml"
wait_for_pod "app=discovery-service" 120

# 5. Config Service 배포
echo ""
echo "⚙️  Deploying config service..."
kubectl apply -f "$PROJECT_ROOT/k8s/services/config-service.yaml"
wait_for_pod "app=config-service" 180

# 6. Business Services 배포
echo ""
echo "💼 Deploying business services..."
kubectl apply -f "$PROJECT_ROOT/k8s/services/auth-service.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/services/blog-service.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/services/shopping-service.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/services/notification-service.yaml"

# 7. API Gateway 배포
echo ""
echo "🌐 Deploying API gateway..."
kubectl apply -f "$PROJECT_ROOT/k8s/services/api-gateway.yaml"

echo ""
echo "⏳ Waiting 30 seconds for all services to stabilize..."
sleep 30

echo ""
echo "✅ Deployment complete! Final status:"
kubectl get pods -n portal-universe

echo ""
echo "📝 Useful commands:"
echo "  - Watch pods: kubectl get pods -n portal-universe -w"
echo "  - View logs: kubectl logs -f deployment/<name> -n portal-universe"