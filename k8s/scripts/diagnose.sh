#!/bin/bash

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔍 Portal Universe - Diagnostics${NC}"
echo ""

# 1. Ingress 확인
echo -e "${YELLOW}1. Ingress Configuration:${NC}"
kubectl describe ingress portal-universe-ingress -n portal-universe | grep -A 10 "Rules:"

# 2. Services 확인
echo ""
echo -e "${YELLOW}2. Services:${NC}"
kubectl get svc -n portal-universe

# 3. Pods 확인
echo ""
echo -e "${YELLOW}3. Pods:${NC}"
kubectl get pods -n portal-universe

# 4. portal-shell 직접 테스트
echo ""
echo -e "${YELLOW}4. Testing portal-shell directly...${NC}"

# 기존 port-forward 종료
pkill -f "port-forward.*portal-shell" 2>/dev/null || true

# 새 port-forward
kubectl port-forward -n portal-universe svc/portal-shell 8888:80 > /dev/null 2>&1 &
PF_PID=$!

sleep 2

# 테스트
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8888)

kill $PF_PID 2>/dev/null || true

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✅ portal-shell is working (HTTP 200)${NC}"
    echo -e "${YELLOW}   → Problem is likely in Ingress configuration${NC}"
else
    echo -e "${RED}❌ portal-shell returned HTTP $HTTP_CODE${NC}"
    echo -e "${YELLOW}   → Problem is in portal-shell or API Gateway${NC}"
fi

# 5. Ingress Controller 로그
echo ""
echo -e "${YELLOW}5. Recent Ingress Controller logs:${NC}"
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller --tail=10 | grep -i "portal-universe\|error" || echo "No errors found"

echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${GREEN}Diagnostics completed!${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"