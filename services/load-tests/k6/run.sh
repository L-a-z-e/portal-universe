#!/bin/bash
# k6 Load Test Runner
# Usage:
#   로컬 실행:      ./run.sh <scenario> [local|docker|k8s] [--vus N] [--duration Ns] [--bulk N]
#   클러스터 실행:   ./run.sh <scenario> k8s-pod [--vus N] [--duration Ns] [--bulk N]
#
# Examples:
#   ./run.sh a-shopping-flow local
#   ./run.sh a-shopping-flow k8s --vus 50 --duration 1m --bulk 50
#   ./run.sh a-shopping-flow k8s-pod --vus 200 --duration 2m --bulk 200

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SCENARIO=${1:-"a-shopping-flow"}
ENV=${2:-"local"}
shift 2 2>/dev/null || true

NAMESPACE="portal-universe"
JOB_NAME="k6-test"

# Parse optional arguments
VUS=""
DURATION=""
BULK_ACCOUNTS=""
USE_BULK="false"

while [[ $# -gt 0 ]]; do
  case $1 in
    --vus) VUS="$2"; shift 2 ;;
    --duration) DURATION="$2"; shift 2 ;;
    --bulk) USE_BULK="true"; BULK_ACCOUNTS="$2"; shift 2 ;;
    *) shift ;;
  esac
done

# === k8s-pod 모드: 클러스터 내부에서 k6 실행 ===
if [ "${ENV}" = "k8s-pod" ]; then
  echo "=== k6 Load Test (In-Cluster) ==="
  echo "Scenario: ${SCENARIO}"
  echo "VUs: ${VUS:-default}"
  echo "Duration: ${DURATION:-default}"
  echo "Bulk: ${USE_BULK} ${BULK_ACCOUNTS}"
  echo "=================================="

  # 1. 기존 Job 정리
  kubectl delete job "${JOB_NAME}" -n "${NAMESPACE}" --ignore-not-found 2>/dev/null

  # 2. ConfigMap 업데이트 (scenarios + lib 분리)
  echo "[1/3] Uploading scripts to ConfigMaps..."
  kubectl create configmap k6-scenarios \
    --from-file="${SCRIPT_DIR}/scenarios/" \
    -n "${NAMESPACE}" \
    --dry-run=client -o yaml | kubectl apply -f -

  kubectl create configmap k6-lib \
    --from-file="${SCRIPT_DIR}/lib/" \
    -n "${NAMESPACE}" \
    --dry-run=client -o yaml | kubectl apply -f -

  # 3. k6 args 구성
  K6_ARGS=()
  K6_ARGS+=("--env" "TARGET_ENV=k8s-pod")
  K6_ARGS+=("--out" "experimental-prometheus-rw")
  K6_ARGS+=("--env" "K6_PROMETHEUS_RW_SERVER_URL=http://prometheus.${NAMESPACE}.svc:9090/api/v1/write")
  K6_ARGS+=("--env" "K6_PROMETHEUS_RW_TREND_STATS=p(50),p(90),p(95),p(99),min,max,avg")

  if [ -n "${VUS}" ]; then
    K6_ARGS+=("--vus" "${VUS}")
  fi
  if [ -n "${DURATION}" ]; then
    K6_ARGS+=("--duration" "${DURATION}")
  fi
  if [ "${USE_BULK}" = "true" ]; then
    K6_ARGS+=("--env" "USE_BULK=true")
    if [ -n "${BULK_ACCOUNTS}" ]; then
      K6_ARGS+=("--env" "VU_ACCOUNTS=${BULK_ACCOUNTS}")
    fi
  fi

  K6_ARGS+=("/scripts/scenarios/${SCENARIO}.js")

  # 4. JSON args for Job spec
  ARGS_JSON=$(printf '%s\n' "${K6_ARGS[@]}" | jq -R . | jq -s .)

  # 5. Job 생성
  echo "[2/3] Creating k6 Job..."
  cat <<EOF | kubectl apply -f -
apiVersion: batch/v1
kind: Job
metadata:
  name: ${JOB_NAME}
  namespace: ${NAMESPACE}
spec:
  backoffLimit: 0
  ttlSecondsAfterFinished: 300
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: k6
          image: grafana/k6:latest
          command: ["k6", "run"]
          args: ${ARGS_JSON}
          volumeMounts:
            - name: scenarios
              mountPath: /scripts/scenarios
            - name: lib
              mountPath: /scripts/lib
          resources:
            requests:
              cpu: "500m"
              memory: "512Mi"
            limits:
              cpu: "2"
              memory: "1Gi"
      volumes:
        - name: scenarios
          configMap:
            name: k6-scenarios
        - name: lib
          configMap:
            name: k6-lib
EOF

  # 6. 로그 스트리밍
  echo "[3/3] Waiting for k6 pod to start..."
  kubectl wait --for=condition=Ready pod -l job-name="${JOB_NAME}" -n "${NAMESPACE}" --timeout=60s 2>/dev/null || true
  sleep 2
  kubectl logs -f "job/${JOB_NAME}" -n "${NAMESPACE}"

  echo ""
  echo "=== Test Complete ==="
  echo "Clean up: kubectl delete job ${JOB_NAME} -n ${NAMESPACE}"
  exit 0
fi

# === 로컬 실행 모드 (local, docker, k8s) ===
PROMETHEUS_URL="http://localhost:9090/api/v1/write"

echo "=== k6 Load Test ==="
echo "Scenario: ${SCENARIO}"
echo "Environment: ${ENV}"
echo "VUs: ${VUS:-default}"
echo "Duration: ${DURATION:-default}"
echo "Bulk: ${USE_BULK} ${BULK_ACCOUNTS}"
echo "===================="

EXTRA_ARGS=""
if [ -n "${VUS}" ]; then
  EXTRA_ARGS="${EXTRA_ARGS} --vus ${VUS}"
fi
if [ -n "${DURATION}" ]; then
  EXTRA_ARGS="${EXTRA_ARGS} --duration ${DURATION}"
fi
if [ "${USE_BULK}" = "true" ]; then
  EXTRA_ARGS="${EXTRA_ARGS} --env USE_BULK=true"
  if [ -n "${BULK_ACCOUNTS}" ]; then
    EXTRA_ARGS="${EXTRA_ARGS} --env VU_ACCOUNTS=${BULK_ACCOUNTS}"
  fi
fi

k6 run \
  --out experimental-prometheus-rw \
  --env K6_PROMETHEUS_RW_SERVER_URL="${PROMETHEUS_URL}" \
  --env K6_PROMETHEUS_RW_TREND_STATS=p(50),p(90),p(95),p(99),min,max,avg \
  --env TARGET_ENV="${ENV}" \
  ${EXTRA_ARGS} \
  "${SCRIPT_DIR}/scenarios/${SCENARIO}.js"
