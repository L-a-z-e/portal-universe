#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

echo "🛑 Stopping Portal Universe Kubernetes Deployment..."
echo "📂 Project root: $PROJECT_ROOT"
echo ""

# 1. Services 삭제 (역순)
echo "🌐 Deleting API Gateway..."
kubectl delete -f "$PROJECT_ROOT/k8s/services/api-gateway.yaml" --ignore-not-found=true --timeout=30s

echo "💼 Deleting business services..."
kubectl delete -f "$PROJECT_ROOT/k8s/services/notification-service.yaml" --ignore-not-found=true --timeout=30s
kubectl delete -f "$PROJECT_ROOT/k8s/services/shopping-service.yaml" --ignore-not-found=true --timeout=30s
kubectl delete -f "$PROJECT_ROOT/k8s/services/blog-service.yaml" --ignore-not-found=true --timeout=30s
kubectl delete -f "$PROJECT_ROOT/k8s/services/auth-service.yaml" --ignore-not-found=true --timeout=30s
kubectl delete -f "$PROJECT_ROOT/k8s/services/portal-shell.yaml" --ignore-not-found=true --timeout=30s

echo "⚙️  Deleting config service..."
kubectl delete -f "$PROJECT_ROOT/k8s/services/config-service.yaml" --ignore-not-found=true --timeout=30s

echo "🔍 Deleting discovery service..."
kubectl delete -f "$PROJECT_ROOT/k8s/services/discovery-service.yaml" --ignore-not-found=true --timeout=30s

# 2. Infrastructure 삭제
echo "🗄️  Deleting infrastructure..."
kubectl delete -f "$PROJECT_ROOT/k8s/infrastructure/kafka.yaml" --ignore-not-found=true --timeout=30s
kubectl delete -f "$PROJECT_ROOT/k8s/infrastructure/mongodb.yaml" --ignore-not-found=true --timeout=30s
kubectl delete -f "$PROJECT_ROOT/k8s/infrastructure/mysql-db.yaml" --ignore-not-found=true --timeout=30s
kubectl delete -f "$PROJECT_ROOT/k8s/infrastructure/zipkin.yaml" --ignore-not-found=true --timeout=30s

# 3. Secret 삭제
echo "🔐 Deleting secrets..."
kubectl delete -f "$PROJECT_ROOT/k8s/secret.yaml" --ignore-not-found=true --timeout=10s

# 4. Namespace 삭제 (선택사항)
echo ""
read -p "❓ Delete namespace 'portal-universe'? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🗑️  Deleting namespace (may take a moment)..."
    kubectl delete namespace portal-universe --timeout=60s 2>/dev/null || {
        echo "⚠️  Namespace deletion timed out, forcing..."
        kubectl delete namespace portal-universe --grace-period=0 --force 2>/dev/null || true
    }
fi

echo ""
echo "✅ Portal Universe stopped!"