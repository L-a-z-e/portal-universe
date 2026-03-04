# Load Tests

Portal Universe 성능/부하 테스트 도구 모음.

## 구조

```
load-tests/
├── k6/                    # k6 HTTP 부하 테스트
│   ├── scenarios/         # 테스트 시나리오 (5개)
│   ├── lib/               # 공통 모듈 (auth, config, checks)
│   └── run.sh             # 실행 헬퍼
└── bots/                  # Custom Bot (Kafka 등)
    ├── kafka_producer.py  # Kafka 메시지 발행 Bot
    └── requirements.txt
```

## 사전 준비

### k6 설치

```bash
brew install k6
```

### 환경별 인프라

| 환경 | 실행 방법 | BASE_URL |
|------|----------|----------|
| local | 개별 서비스 직접 실행 | `http://localhost:8080` |
| docker | `docker compose up -d` | `http://host.docker.internal:8080` |
| k8s | Kind 클러스터 + port-forward | `http://localhost:80` (Host: portal-universe) |

### K8s 환경 준비

```bash
# 1. Ingress Controller port-forward (터미널 1)
kubectl port-forward svc/ingress-nginx-controller 80:80 -n ingress-nginx > /dev/null 2>&1 &

# 2. Prometheus port-forward (메트릭 수집 시, 터미널 2)
kubectl port-forward svc/prometheus 9090:9090 -n portal-universe > /dev/null 2>&1 &

# 3. Grafana port-forward (대시보드 확인 시, 터미널 3)
kubectl port-forward svc/grafana 3000:3000 -n portal-universe > /dev/null 2>&1 &
```

### Rate Limit 완화 (부하 테스트 전)

K8s 환경에서는 프로덕션 수준의 rate limit이 적용됩니다.
부하 테스트 전에 완화가 필요합니다. 자세한 내용은 [부하 테스트 가이드](../../docs/guides/operations/load-testing.md)를 참조하세요.

## k6 테스트 실행

### 방법 1: run.sh 사용

```bash
cd services/load-tests

# ./k6/run.sh <시나리오> [환경] [prometheus_url] [use_bulk] [vu_accounts]
./k6/run.sh a-shopping-flow k8s http://localhost:9090/api/v1/write true 100
```

| 파라미터 | 기본값 | 설명 |
|---------|--------|------|
| 시나리오 | `a-shopping-flow` | 시나리오 파일명 (확장자 제외) |
| 환경 | `local` | `local` / `docker` / `k8s` |
| prometheus_url | `http://localhost:9090/api/v1/write` | Prometheus Remote Write URL |
| use_bulk | `false` | bulk 계정 사용 여부 |
| vu_accounts | (config 기본값) | bulk 로그인 계정 수 |

### 방법 2: k6 직접 실행

```bash
cd services/load-tests/k6

# 최소 부하 (디버깅용)
k6 run --vus 1 --iterations 3 --env TARGET_ENV=k8s scenarios/a-shopping-flow.js

# Bulk 모드 (rate limit 우회)
k6 run --vus 5 --iterations 50 \
  --env TARGET_ENV=k8s \
  --env USE_BULK=true \
  --env VU_ACCOUNTS=100 \
  scenarios/a-shopping-flow.js

# 시나리오 options 그대로 실행 + Prometheus 메트릭 전송
k6 run \
  --out experimental-prometheus-rw \
  --env K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
  --env K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=true \
  --env TARGET_ENV=k8s \
  --env USE_BULK=true \
  scenarios/a-shopping-flow.js
```

### Bulk 모드란?

기본 모드는 단일 계정(`test@test.com`)으로 모든 VU가 동일 토큰을 공유합니다.
이 경우 API Gateway의 rate limiter에 걸려 **성능이 아닌 rate limit을 테스트**하게 됩니다.

`USE_BULK=true`를 사용하면:
- `BulkTestAccountInitializer`가 생성한 10,000개 계정 중 `VU_ACCOUNTS`개에 로그인
- 각 VU가 서로 다른 토큰을 사용 → rate limit 분산
- 실제 다수 사용자 트래픽 시뮬레이션

## 시나리오 목록

| 시나리오 | 파일 | Executor | 목적 |
|----------|------|----------|------|
| A. Shopping Flow | `a-shopping-flow.js` | ramping-vus (100 VU) | 쇼핑 E2E: 목록→상세→장바구니 |
| B. Blog Read | `b-blog-read.js` | constant-arrival-rate (200 RPS) | Read-Heavy 패턴, 목록+상세 |
| C. Coupon Spike | `c-coupon-spike.js` | shared-iterations (500 VU × 1회) | 선착순 쿠폰 동시 발급 |
| D. Search Load | `d-search-load.js` | ramping-arrival-rate (→200 RPS) | ES 검색 부하 테스트 |
| E. Cache Thundering | `e-cache-thundering.js` | shared-iterations (200 VU) | 캐시 만료 후 Thundering Herd |

## k6 결과 읽는 법

```
     ✓ product_list status 200     # check 통과 (기능 정상)
     ✗ product_list success        # check 실패 (기능 문제)
       ↳  63% — ✓ 19 / ✗ 11       # 성공/실패 비율

   ✗ http_req_duration.........: p(95)=308ms   # threshold 위반 (성능 미달)
   ✓ http_req_failed...........: 0.00%         # threshold 통과
```

- **checks**: API 응답의 기능적 정확성 (status 200, success:true)
- **thresholds**: 성능 기준 (p95 응답시간, 에러율)
- checks 100%인데 threshold 실패 → 기능은 되지만 느림
- checks 실패 → API 자체가 에러 반환 (429 rate limit, 500 서버 에러 등)

## 디버깅

### 실패 원인 추적

```bash
# 1. 실시간 로그 감시 (터미널 1)
kubectl logs -l app=api-gateway -n portal-universe -f | grep "429\|500\|503"

# 2. k6 실행 (터미널 2)
k6 run --vus 1 --iterations 3 --env TARGET_ENV=k8s scenarios/a-shopping-flow.js

# 3. 특정 서비스 로그
kubectl logs -l app=shopping-service -n portal-universe -f
```

### 흔한 문제

| 증상 | 원인 | 해결 |
|------|------|------|
| check 실패 + 429 | Rate limit | `USE_BULK=true` 또는 rate limit 완화 |
| check 실패 + 500 | 서비스 에러 | 해당 서비스 로그 확인 |
| threshold 실패 + check 100% | Cold start / 리소스 부족 | 반복 실행 / Pod 리소스 증가 |
| `loginBulk: 6/100 tokens` | 로그인 rate limit | Rate limit 완화 필요 |
| Pod OOMKilled | 메모리 부족 | K8s deployment memory limits 증가 |

## Grafana 대시보드

```
http://localhost:3000 (admin/admin)
```

| 대시보드 | 용도 |
|----------|------|
| **Load Test Overview** | k6 RPS, Error Rate, VU + 인프라 메트릭 통합 |
| **Bottleneck Detection** | RED/USE Method 기반 병목 탐지 |
| **JVM Deep Dive** | Java 서비스 힙 메모리, GC, 스레드 |

### 대시보드 보는 순서

1. **Load Test Overview** — 테스트 정상 실행 여부, 에러 발생 서비스 확인
2. **Bottleneck Detection** — 가장 느린 서비스, DB/Redis/Kafka 병목 확인
3. 기존 대시보드 (JVM Deep Dive, Logs & Traces) — 상세 분석

## Threshold 기준

| 지표 | 목표 |
|------|------|
| p95 Response Time | < 200ms |
| p99 Response Time | < 500ms |
| Error Rate | < 0.1% |

## Kafka Bot 실행

```bash
cd services/load-tests

# 의존성 설치
pip install -r bots/requirements.txt

# 100 msg/s × 60초
python bots/kafka_producer.py \
  --bootstrap-servers localhost:9092 \
  --topic order-created \
  --rate 100 \
  --duration 60
```