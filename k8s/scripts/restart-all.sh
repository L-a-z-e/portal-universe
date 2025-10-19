#!/bin/bash

echo "🔄 Restarting Portal Universe Kubernetes Deployment..."

# Rollout restart (Pod만 재생성, Service/Deployment 유지)
echo "🔍 Restarting discovery service..."
kubectl rollout restart deployment/discovery-service -n portal-universe

echo "⚙️  Restarting config service..."
kubectl rollout restart deployment/config-service -n portal-universe

echo "💼 Restarting business services..."
kubectl rollout restart deployment/auth-service -n portal-universe
kubectl rollout restart deployment/blog-service -n portal-universe
kubectl rollout restart deployment/shopping-service -n portal-universe
kubectl rollout restart deployment/notification-service -n portal-universe

echo "🌐 Restarting API gateway..."
kubectl rollout restart deployment/api-gateway -n portal-universe

echo "🖥️ Restarting frontend services..."
kubectl rollout restart deployment/auth-service -n portal-universe
kubectl rollout restart deployment/blog-service -n portal-universe
kubectl rollout restart deployment/shopping-service -n portal-universe

# Infrastructure도 재시작 (선택사항)
echo ""
read -p "❓ Restart infrastructure (MySQL, MongoDB, Kafka)? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    kubectl rollout restart deployment/mysql-db -n portal-universe
    kubectl rollout restart deployment/mongodb -n portal-universe
    kubectl rollout restart deployment/kafka -n portal-universe
fi

echo ""
echo "✅ Restart initiated!"
echo "📊 Watch status: kubectl get pods -n portal-universe -w"