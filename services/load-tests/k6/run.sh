#!/bin/bash
# k6 Load Test Runner
# Usage: ./run.sh <scenario> [env] [prometheus_url]

SCENARIO=${1:-"a-shopping-flow"}
ENV=${2:-"local"}
PROMETHEUS_URL=${3:-"http://localhost:9090/api/v1/write"}

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== k6 Load Test ==="
echo "Scenario: ${SCENARIO}"
echo "Environment: ${ENV}"
echo "Prometheus: ${PROMETHEUS_URL}"
echo "===================="

k6 run \
  --out experimental-prometheus-rw \
  --env K6_PROMETHEUS_RW_SERVER_URL="${PROMETHEUS_URL}" \
  --env K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=true \
  --env TARGET_ENV="${ENV}" \
  "${SCRIPT_DIR}/scenarios/${SCENARIO}.js"
