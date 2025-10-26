#!/bin/bash

# =============================================================================
# check-status.sh
#
# 역할:
#   배포된 Portal Universe 애플리케이션의 현재 상태를 확인합니다.
#   주요 쿠버네티스 리소스(Namespace, Pod, Service, Ingress 등)의 목록을
#   보여주어 배포 상태를 한눈에 파악할 수 있도록 돕습니다.
#
# 사용법:
#   ./k8s/scripts/check-status.sh
# =============================================================================

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# --- 색상 변수 ---
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}📊 Portal Universe - Status Check${NC}"
echo ""

echo -e "${YELLOW}1️⃣  Namespaces:${NC}"
kubectl get namespace | grep -E "NAME|portal-universe|ingress-nginx"

echo ""
echo -e "${YELLOW}2️⃣  Pods (portal-universe):${NC}"
kubectl get pods -n portal-universe -o wide

echo ""
echo -e "${YELLOW}3️⃣  Services (portal-universe):${NC}"
kubectl get svc -n portal-universe

echo ""
echo -e "${YELLOW}4️⃣  Ingress:${NC}"
kubectl get ingress -n portal-universe

echo ""
echo -e "${YELLOW}5️⃣  Ingress Controller:${NC}"
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx

echo ""
echo -e "${YELLOW}6️⃣  PersistentVolumeClaims:${NC}"
kubectl get pvc -n portal-universe

echo ""
# Ingress Controller의 NodePort를 조회하여 접근 URL을 알려줍니다.
NODE_PORT=$(kubectl get svc ingress-nginx-controller -n ingress-nginx -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")

if [ "$NODE_PORT" != "N/A" ]; then
    echo -e "${GREEN}📋 Access URL: ${BLUE}http://localhost:${NODE_PORT}${NC}"
else
    echo -e "${YELLOW}⚠️  Ingress Controller NodePort not found${NC}"
fi
