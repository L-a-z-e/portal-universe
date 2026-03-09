#!/bin/bash
set -euo pipefail

# =================================================================
# generate-dashboard-configmaps.sh
# 정본 JSON 대시보드 → Grafana Sidecar용 ConfigMap 변환
#
# 정본: monitoring/grafana/provisioning/dashboards/json/*.json (13개)
# Sidecar: grafana_dashboard: "1" 라벨이 있는 ConfigMap을 자동 감지
#
# 사용법:
#   ./k8s/helm/monitoring/generate-dashboard-configmaps.sh
#   NAMESPACE=other-ns ./k8s/helm/monitoring/generate-dashboard-configmaps.sh
# =================================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"
DASHBOARD_DIR="$ROOT_DIR/monitoring/grafana/provisioning/dashboards/json"
NAMESPACE="${NAMESPACE:-portal-universe}"

if [ ! -d "$DASHBOARD_DIR" ]; then
  echo "ERROR: Dashboard directory not found: $DASHBOARD_DIR"
  exit 1
fi

echo "=== Generating dashboard ConfigMaps ==="
echo "Source: $DASHBOARD_DIR"
echo "Namespace: $NAMESPACE"
echo ""

count=0
for json_file in "$DASHBOARD_DIR"/*.json; do
  [ -f "$json_file" ] || continue

  filename=$(basename "$json_file" .json)
  configmap_name="grafana-dashboard-${filename}"

  echo "  Creating ConfigMap: $configmap_name"

  # ConfigMap 생성
  kubectl create configmap "$configmap_name" \
    --from-file="${filename}.json=${json_file}" \
    --namespace="$NAMESPACE" \
    --save-config --dry-run=client -o yaml | \
    kubectl apply -f -

  # Sidecar 감지용 라벨 추가
  kubectl label configmap "$configmap_name" \
    --namespace="$NAMESPACE" \
    grafana_dashboard="1" --overwrite

  count=$((count + 1))
done

echo ""
echo "=== Done: $count dashboard ConfigMaps created ==="
