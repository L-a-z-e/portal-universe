# Load Tests

Portal Universe 성능/부하 테스트 도구 모음.

## 구조

```
load-tests/
├── k6/                    # k6 HTTP 부하 테스트
│   ├── scenarios/         # 테스트 시나리오
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

### Prometheus Remote Write 활성화

docker-compose.yml의 Prometheus에 `--web.enable-remote-write-receiver` 플래그가 이미 설정되어 있음.

### 인프라 실행

```bash
# Exporter 포함 전체 인프라 실행
docker compose -f docker-compose-local.yml up -d
```

## k6 테스트 실행

### 개별 시나리오 실행

```bash
cd services/load-tests

# 쇼핑 E2E 플로우 (기본)
./k6/run.sh a-shopping-flow

# 블로그 Read-Heavy
./k6/run.sh b-blog-read

# 쿠폰 선착순 Spike
./k6/run.sh c-coupon-spike

# ES 검색 부하
./k6/run.sh d-search-load

# Thundering Herd (캐시 만료)
./k6/run.sh e-cache-thundering
```

### 환경 지정

```bash
# Docker 환경 대상
./k6/run.sh a-shopping-flow docker

# Prometheus URL 지정
./k6/run.sh a-shopping-flow local http://localhost:9090/api/v1/write
```

### 직접 실행

```bash
k6 run k6/scenarios/a-shopping-flow.js
```

## 시나리오 목록

| 시나리오 | 파일 | 목적 | VU/Rate |
|----------|------|------|---------|
| A. Shopping Flow | `a-shopping-flow.js` | 쇼핑 E2E (조회→장바구니) | 100 VU |
| B. Blog Read | `b-blog-read.js` | Read-Heavy 부하 | 500 req/s |
| C. Coupon Spike | `c-coupon-spike.js` | 선착순 동시 요청 | 500 VU × 1회 |
| D. Search Load | `d-search-load.js` | ES 검색 부하 | 200 req/s |
| E. Cache Thundering | `e-cache-thundering.js` | 캐시 만료 후 동시 요청 | 200 VU |

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

# Spike: 1000 msg/s × 60초
python bots/kafka_producer.py --rate 1000 --duration 60
```

Bot 실행 중 `http://localhost:8000/metrics`에서 Prometheus 메트릭 확인 가능.

## Grafana 대시보드

테스트 실행 시 아래 대시보드에서 실시간 모니터링:

| 대시보드 | 용도 |
|----------|------|
| **Load Test Overview** | k6 RPS, Error Rate, VU + 인프라 메트릭 통합 |
| **Bottleneck Detection** | RED/USE Method 기반 병목 탐지 |

### 대시보드 보는 순서

1. **Load Test Overview** — 테스트 정상 실행 여부, 어느 서비스에서 에러 발생하는지
2. **Bottleneck Detection** — 가장 느린 서비스, DB/Redis/Kafka 병목 확인
3. 기존 대시보드 (JVM Deep Dive, Logs & Traces) — 상세 분석

## Threshold 기준

| 지표 | 목표 |
|------|------|
| p95 Response Time | < 200ms |
| p99 Response Time | < 500ms |
| Error Rate | < 0.1% |
| Redis Hit Rate | > 90% |
| Kafka Consumer Lag | < 1,000 |
