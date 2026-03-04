#!/bin/bash

# =============================================================================
# load-test-setup.sh
#
# 역할:
#   K8s Kind 환경에서 부하 테스트를 위한 HA 클러스터를 관리합니다.
#   3-worker Kind 클러스터 생성, 상태 확인, 삭제를 자동화합니다.
#
# 사용법:
#   ./k8s/scripts/load-test-setup.sh create    # 클러스터 생성 + 빌드 + 배포
#   ./k8s/scripts/load-test-setup.sh status    # HPA, PDB, Pod, Node 현황
#   ./k8s/scripts/load-test-setup.sh teardown  # 클러스터 삭제
# =============================================================================

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "$SCRIPT_DIR/../.." && pwd )"

# --- 색상 변수 ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

CLUSTER_NAME="portal-universe"
KIND_CONFIG="$PROJECT_ROOT/k8s/base/kind-config.yaml"

# =============================================================================
# 함수 정의
# =============================================================================

usage() {
    echo -e "${BLUE}Usage:${NC} $0 {create|status|teardown}"
    echo ""
    echo "Commands:"
    echo "  create    Create 3-worker Kind cluster, build images, deploy with HA overlay"
    echo "  status    Show HPA, PDB, Pod, and Node resource status"
    echo "  teardown  Delete the Kind cluster"
    exit 1
}

check_prerequisites() {
    local missing=()
    for cmd in kind kubectl docker; do
        if ! command -v "$cmd" &> /dev/null; then
            missing+=("$cmd")
        fi
    done
    if [[ ${#missing[@]} -gt 0 ]]; then
        echo -e "${RED}Missing prerequisites: ${missing[*]}${NC}"
        exit 1
    fi
}

# --- create ---
cmd_create() {
    echo -e "${BLUE}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  Load Test Environment Setup (3-worker Kind + HA)${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════════${NC}"
    echo ""

    # 기존 클러스터 확인
    if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
        echo -e "${YELLOW}⚠️  Cluster '${CLUSTER_NAME}' already exists.${NC}"
        read -p "Delete and recreate? (y/N): " confirm
        if [[ "$confirm" =~ ^[Yy]$ ]]; then
            echo -e "${YELLOW}Deleting existing cluster...${NC}"
            kind delete cluster --name "$CLUSTER_NAME"
        else
            echo -e "${YELLOW}Skipping cluster creation. Using existing cluster.${NC}"
            echo ""
            # 기존 클러스터가 있으면 빌드+배포만 진행
            step_build
            step_deploy
            cmd_status
            return
        fi
    fi

    # Step 1: Kind 클러스터 생성
    echo -e "${CYAN}📦 Step 1/3: Creating Kind cluster (3 workers)...${NC}"
    kind create cluster --config "$KIND_CONFIG"
    echo -e "${GREEN}✅ Kind cluster created${NC}"
    echo ""

    # Step 2: 빌드 + 이미지 로드
    step_build

    # Step 3: 배포
    step_deploy

    echo ""
    echo -e "${GREEN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  Load Test Environment Ready${NC}"
    echo -e "${GREEN}══════════════════════════════════════════════════════════════${NC}"
    echo ""

    # 상태 출력
    cmd_status
}

step_build() {
    echo -e "${CYAN}🔨 Step 2/3: Building and loading images...${NC}"
    bash "$SCRIPT_DIR/build-and-load.sh"
    echo -e "${GREEN}✅ Images built and loaded${NC}"
    echo ""
}

step_deploy() {
    echo -e "${CYAN}🚀 Step 3/3: Deploying with Kind HA overlay...${NC}"
    bash "$SCRIPT_DIR/deploy-all.sh" --overlay kind
    echo -e "${GREEN}✅ Deployment complete${NC}"
    echo ""
}

# --- status ---
cmd_status() {
    echo -e "${BLUE}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  Load Test Environment Status${NC}"
    echo -e "${BLUE}══════════════════════════════════════════════════════════════${NC}"
    echo ""

    # Node 상태
    echo -e "${CYAN}📊 Nodes:${NC}"
    kubectl get nodes -o wide 2>/dev/null || echo -e "${RED}Cluster not reachable${NC}"
    echo ""

    # Node 리소스
    echo -e "${CYAN}📊 Node Resources:${NC}"
    kubectl top nodes 2>/dev/null || echo -e "${YELLOW}(metrics not available yet)${NC}"
    echo ""

    # HPA 상태
    echo -e "${CYAN}📊 HorizontalPodAutoscalers:${NC}"
    kubectl get hpa -n portal-universe 2>/dev/null || echo -e "${YELLOW}(no HPA found)${NC}"
    echo ""

    # PDB 상태
    echo -e "${CYAN}📊 PodDisruptionBudgets:${NC}"
    kubectl get pdb -n portal-universe 2>/dev/null || echo -e "${YELLOW}(no PDB found)${NC}"
    echo ""

    # Pod 상태
    echo -e "${CYAN}📊 Pods:${NC}"
    kubectl get pods -n portal-universe -o wide 2>/dev/null || echo -e "${YELLOW}(no pods found)${NC}"
    echo ""

    # Pod 리소스 사용량
    echo -e "${CYAN}📊 Pod Resources:${NC}"
    kubectl top pods -n portal-universe 2>/dev/null || echo -e "${YELLOW}(metrics not available yet)${NC}"
    echo ""
}

# --- teardown ---
cmd_teardown() {
    echo -e "${YELLOW}🗑️  Deleting Kind cluster '${CLUSTER_NAME}'...${NC}"

    if ! kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
        echo -e "${YELLOW}Cluster '${CLUSTER_NAME}' does not exist.${NC}"
        exit 0
    fi

    kind delete cluster --name "$CLUSTER_NAME"
    echo -e "${GREEN}✅ Cluster deleted${NC}"
}

# =============================================================================
# 메인
# =============================================================================

check_prerequisites

case "${1:-}" in
    create)
        cmd_create
        ;;
    status)
        cmd_status
        ;;
    teardown)
        cmd_teardown
        ;;
    *)
        usage
        ;;
esac
