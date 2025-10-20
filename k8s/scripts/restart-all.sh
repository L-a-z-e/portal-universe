#!/bin/bash

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# 색상
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔄 Portal Universe - Restart All Deployments${NC}"
echo ""

DEPLOYMENTS=(
    "discovery-service"
    "config-service"
    "auth-service"
    "blog-service"
    "shopping-service"
    "notification-service"
    "api-gateway"
    "portal-shell"
)

for DEPLOYMENT in "${DEPLOYMENTS[@]}"; do
    echo -e "${YELLOW}Restarting ${DEPLOYMENT}...${NC}"
    kubectl rollout restart deployment/${DEPLOYMENT} -n portal-universe
    echo -e "${GREEN}✅ ${DEPLOYMENT} restarted${NC}"
done

echo ""
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo -e "${GREEN}🎉 All deployments restarted!${NC}"
echo -e "${GREEN}════════════════════════════════════════${NC}"
echo ""
echo "Monitoring rollout status..."
kubectl get pods -n portal-universe -w