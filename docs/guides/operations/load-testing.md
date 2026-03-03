---
id: load-testing-guide
title: 부하 테스트 실행 가이드
type: guide
status: current
created: 2026-03-02
author: Laze
tags: [k6, load-testing, rate-limiting, kubernetes, grafana]
---

# 부하 테스트 실행 가이드

**카테고리**: Operations

## 개요

K8s Kind 클러스터에서 k6 부하 테스트를 실행하는 전체 절차.
Rate limit 완화, port-forward 설정, 테스트 실행, 결과 확인까지 다룹니다.

## 사전 조건

- Kind 클러스터 실행 중 (`kubectl get nodes`로 확인)
- k6 설치 (`brew install k6`)
- 모든 서비스 Pod가 Ready 상태

```bash
# Pod 상태 확인
kubectl get pods -n portal-universe -o custom-columns="NAME:.metadata.name,READY:.status.containerStatuses[0].ready,RESTARTS:.status.containerStatuses[0].restartCount" | sort
```

## 1단계: Rate Limit 완화

K8s 환경에서는 프로덕션 수준의 rate limit이 적용되어 부하 테스트에 방해가 됩니다.
ConfigMap의 환경변수를 변경하여 리빌드 없이 완화할 수 있습니다.

### 현재 Rate Limit 설정

| Rate Limiter | 용도 | 프로덕션 (기본) | 완화 (부하 테스트용) |
|-------------|------|----------------|-------------------|
| strict | 로그인 | 1 req/s, burst 5 | 100 req/s, burst 500 |
| signup | 회원가입 | 1 req/s, burst 3 | 100 req/s, burst 500 |
| authenticated | 인증 사용자 | 2 req/s, burst 100 | 200 req/s, burst 2000 |
| unauthenticated | 비인증 사용자 | 1 req/s, burst 30 | 200 req/s, burst 2000 |
| default | 일반 API | 10 req/s, burst 20 | 200 req/s, burst 2000 |

### 완화 적용

api-gateway deployment에 환경변수를 추가합니다:

```bash
kubectl set env deployment/api-gateway -n portal-universe \
  RATE_LIMITER_DEFAULT_REPLENISH_RATE=200 \
  RATE_LIMITER_DEFAULT_BURST_CAPACITY=2000 \
  RATE_LIMITER_STRICT_REPLENISH_RATE=100 \
  RATE_LIMITER_STRICT_BURST_CAPACITY=500 \
  RATE_LIMITER_SIGNUP_REPLENISH_RATE=100 \
  RATE_LIMITER_SIGNUP_BURST_CAPACITY=500 \
  RATE_LIMITER_AUTHENTICATED_REPLENISH_RATE=200 \
  RATE_LIMITER_AUTHENTICATED_BURST_CAPACITY=2000 \
  RATE_LIMITER_UNAUTHENTICATED_REPLENISH_RATE=200 \
  RATE_LIMITER_UNAUTHENTICATED_BURST_CAPACITY=2000
```

`kubectl set env`는 deployment를 수정하고 자동으로 rollout restart를 트리거합니다.

```bash
# rollout 완료 대기
kubectl rollout status deployment/api-gateway -n portal-universe --timeout=120s
```

### 완화 확인

```bash
# api-gateway 로그에서 Rate Limiter 설정 확인
kubectl logs -l app=api-gateway -n portal-universe --tail=20 | grep "Rate Limiter"
```

다음과 같은 로그가 나오면 성공:
```
Default Rate Limiter: replenishRate=200, burstCapacity=2000
Strict Rate Limiter: replenishRate=100, burstCapacity=500
```

## 2단계: Port-Forward 설정

```bash
# Ingress (필수 - k6 트래픽 진입점)
kubectl port-forward svc/ingress-nginx-controller 80:80 -n ingress-nginx > /dev/null 2>&1 &

# Prometheus (선택 - k6 메트릭을 Prometheus에 전송할 때)
kubectl port-forward svc/prometheus 9090:9090 -n portal-universe > /dev/null 2>&1 &

# Grafana (선택 - 대시보드 확인)
kubectl port-forward svc/grafana 3000:3000 -n portal-universe > /dev/null 2>&1 &
```

## 3단계: 테스트 실행

```bash
cd services/load-tests/k6

# 최소 부하 (동작 확인용)
k6 run --vus 1 --iterations 3 \
  --env TARGET_ENV=k8s \
  scenarios/a-shopping-flow.js

# Bulk 모드 + Prometheus 메트릭 전송
k6 run \
  --out experimental-prometheus-rw \
  --env K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
  --env K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=true \
  --env TARGET_ENV=k8s \
  --env USE_BULK=true \
  --env VU_ACCOUNTS=300 \
  scenarios/a-shopping-flow.js
```

### 시나리오별 실행

```bash
# B. Blog Read
k6 run --env TARGET_ENV=k8s --env USE_BULK=true scenarios/b-blog-read.js

# C. Coupon Spike (COUPON_ID 필요)
k6 run --env TARGET_ENV=k8s --env USE_BULK=true --env COUPON_ID=1 scenarios/c-coupon-spike.js

# D. Search Load
k6 run --env TARGET_ENV=k8s --env USE_BULK=true scenarios/d-search-load.js

# E. Cache Thundering (PRODUCT_ID 필요)
k6 run --env TARGET_ENV=k8s --env USE_BULK=true --env PRODUCT_ID=1 scenarios/e-cache-thundering.js
```

## 4단계: 결과 확인

### k6 Console Summary

```
checks.........................: 100.00% ✓ 18       ✗ 0    # 기능 정상
http_req_duration..............: p(95)=25ms                 # 성능 양호
http_req_failed................: 0.00%                      # 에러 없음
```

### Grafana 대시보드

`http://localhost:3000` (admin/admin)

1. **Load Test Overview** — RPS, Error Rate, VU 수
2. **Bottleneck Detection** — 가장 느린 서비스
3. **JVM Deep Dive** — 힙 메모리, GC

### 실시간 로그 모니터링

```bash
# 에러만 필터링
kubectl logs -l app=api-gateway -n portal-universe -f | grep "429\|500\|503"

# 특정 서비스
kubectl logs -l app=shopping-service -n portal-universe -f
```

## 5단계: Rate Limit 원복

테스트가 끝나면 반드시 원복합니다:

```bash
kubectl set env deployment/api-gateway -n portal-universe \
  RATE_LIMITER_DEFAULT_REPLENISH_RATE- \
  RATE_LIMITER_DEFAULT_BURST_CAPACITY- \
  RATE_LIMITER_STRICT_REPLENISH_RATE- \
  RATE_LIMITER_STRICT_BURST_CAPACITY- \
  RATE_LIMITER_SIGNUP_REPLENISH_RATE- \
  RATE_LIMITER_SIGNUP_BURST_CAPACITY- \
  RATE_LIMITER_AUTHENTICATED_REPLENISH_RATE- \
  RATE_LIMITER_AUTHENTICATED_BURST_CAPACITY- \
  RATE_LIMITER_UNAUTHENTICATED_REPLENISH_RATE- \
  RATE_LIMITER_UNAUTHENTICATED_BURST_CAPACITY-

kubectl rollout status deployment/api-gateway -n portal-universe --timeout=120s
```

환경변수명 뒤에 `-`를 붙이면 해당 환경변수를 제거합니다.
제거 후 application.yml의 기본값(프로덕션 rate limit)이 적용됩니다.

## 트러블슈팅

### loginBulk에서 대부분 429

**원인**: Rate limit 완화가 적용되지 않음
**확인**: `kubectl logs -l app=api-gateway --tail=10 | grep "Rate Limiter"`
**해결**: 1단계 다시 수행

### Pod OOMKilled로 재시작 반복

**원인**: Java 서비스 메모리 부족
**확인**: `kubectl describe pod <pod-name> -n portal-universe | grep -A3 "Last State"`
**해결**: deployment의 memory limits 증가 (최소 1Gi 권장)

### Grafana에 No Data

**원인**: Prometheus targets 미연결
**확인**: `http://localhost:9090/targets` 에서 target 상태 확인
**해결**: Prometheus RBAC, NetworkPolicy 확인

### port-forward 끊김

**원인**: 일정 시간 비활동 후 자동 종료
**해결**: 다시 실행. `> /dev/null 2>&1 &`로 백그라운드 실행 시 끊김 메시지 방지

## 참고

- [k6 스크립트 상세](../../services/load-tests/README.md)
- [Rate Limiting 아키텍처](../development/rate-limiting.md)
- [모니터링 가이드](./monitoring.md)
- [네트워크 정책](./network-policy.md)
