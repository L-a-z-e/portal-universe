#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# 색상
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
NODE_PORT=$(kubectl get svc ingress-nginx-controller -n ingress-nginx -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")

if [ "$NODE_PORT" != "N/A" ]; then
    echo -e "${GREEN}📋 Access URL: ${BLUE}http://localhost:${NODE_PORT}${NC}"
else
    echo -e "${YELLOW}⚠️  Ingress Controller NodePort not found${NC}"
fi