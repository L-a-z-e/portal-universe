#!/bin/bash
set -euo pipefail

# =================================================================
# Docker Build + Kind Load + Rollout Restart
# 전체 커스텀 서비스 이미지를 빌드하여 Kind 클러스터에 배포한다.
#
# Usage:
#   ./scripts/docker-to-k8s.sh          # 전체 빌드+배포
#   ./scripts/docker-to-k8s.sh api-gateway auth-service  # 지정 서비스만
# =================================================================

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
KIND_CLUSTER="portal-universe"
NAMESPACE="portal-universe"

# --- 서비스 목록 (빌드 context별 분류) ---

# 루트 context: docker build -f <Dockerfile> <ROOT_DIR>
JAVA_SERVICES=(api-gateway auth-service blog-service shopping-service shopping-seller-service shopping-settlement-service drive-service notification-service)
REACT_FRONTENDS=(blog-frontend shopping-frontend prism-frontend admin-frontend drive-frontend shopping-seller-frontend)

# frontend/ context: docker build -f <Dockerfile> frontend/
PORTAL_SHELL="portal-shell"

# 서비스 디렉토리 context: docker build <service-dir>
SELF_CONTEXT_SERVICES=(prism-service chatbot-service)

# --- 함수 ---

build_and_load() {
  local name=$1
  local image="portal-universe-${name}:latest"

  echo ""
  echo "=== [$name] Building image... ==="

  if [[ " ${JAVA_SERVICES[*]} " == *" $name "* ]]; then
    docker build -f "$ROOT_DIR/services/$name/Dockerfile" -t "$image" "$ROOT_DIR"
  elif [[ " ${REACT_FRONTENDS[*]} " == *" $name "* ]]; then
    docker build -f "$ROOT_DIR/frontend/$name/Dockerfile" -t "$image" "$ROOT_DIR"
  elif [[ "$name" == "$PORTAL_SHELL" ]]; then
    docker build -f "$ROOT_DIR/frontend/$name/Dockerfile" -t "$image" "$ROOT_DIR/frontend"
  elif [[ " ${SELF_CONTEXT_SERVICES[*]} " == *" $name "* ]]; then
    docker build -t "$image" "$ROOT_DIR/services/$name"
  else
    echo "  [SKIP] Unknown service: $name"
    return 1
  fi

  echo "  Loading to Kind cluster..."
  kind load docker-image "$image" --name "$KIND_CLUSTER"
  echo "  [$name] Done."
}

rollout_restart() {
  local name=$1
  echo "  Restarting deployment/$name..."
  kubectl rollout restart "deployment/$name" -n "$NAMESPACE" 2>/dev/null || echo "  [WARN] deployment/$name not found"
}

# --- 메인 ---

ALL_SERVICES=("${JAVA_SERVICES[@]}" "$PORTAL_SHELL" "${REACT_FRONTENDS[@]}" "${SELF_CONTEXT_SERVICES[@]}")

# 인자가 있으면 지정 서비스만, 없으면 전체
if [[ $# -gt 0 ]]; then
  TARGETS=("$@")
else
  TARGETS=("${ALL_SERVICES[@]}")
fi

echo "=== Portal Universe: Docker Build + Kind Load ==="
echo "Target services: ${TARGETS[*]}"
echo ""

FAILED=()
for svc in "${TARGETS[@]}"; do
  if build_and_load "$svc"; then
    rollout_restart "$svc"
  else
    FAILED+=("$svc")
  fi
done

echo ""
echo "=== Waiting for rollouts... ==="
for svc in "${TARGETS[@]}"; do
  local_failed=" ${FAILED[*]:-} "
  if [[ ! "$local_failed" == *" $svc "* ]]; then
    kubectl rollout status "deployment/$svc" -n "$NAMESPACE" --timeout=180s 2>/dev/null &
  fi
done
wait

echo ""
if [[ ${#FAILED[@]} -gt 0 ]]; then
  echo "=== FAILED: ${FAILED[*]} ==="
else
  echo "=== All services updated successfully! ==="
fi
