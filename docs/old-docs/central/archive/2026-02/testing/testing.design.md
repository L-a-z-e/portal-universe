# Performance & Load Testing - Design

> Feature: testing
> Phase: Design
> Created: 2026-02-03
> Author: AI-assisted
> Plan Reference: `docs/pdca/01-plan/features/testing.plan.md`

---

## 1. Implementation Overview

Plan 문서(S1~S6)의 In-Scope 항목을 기반으로 구체적인 설계를 정의한다.

### 구현 순서

```
Phase 1: Exporter 추가 + Prometheus 설정      (S2)
Phase 2: Zipkin ES Backend 전환               (S6)
Phase 3: k6 설치 + 스크립트 구조 + Grafana 연동 (S1, S4)
Phase 4: Grafana 부하 테스트 대시보드 2개       (S3)
Phase 5: Kafka Custom Bot                     (S5)
```

> Phase 1~2를 먼저 하는 이유: k6 테스트 실행 전에 미들웨어 메트릭이 수집되어야 병목을 볼 수 있음

---

## 2. Phase 1: Exporter 추가 (S2)

### 2.1 docker-compose.yml에 추가할 서비스

#### cAdvisor

```yaml
cadvisor:
  image: gcr.io/cadvisor/cadvisor:v0.49.1
  container_name: cadvisor
  ports:
    - "8888:8080"
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock:ro
    - /sys:/sys:ro
    - /var/lib/docker/:/var/lib/docker:ro
  networks:
    - portal-universe-net
  restart: unless-stopped
```

> **Port 8888**: cAdvisor 기본 8080은 API Gateway와 충돌하므로 8888 사용

#### mysqld_exporter

```yaml
mysql-exporter:
  image: prom/mysqld-exporter:v0.16.0
  container_name: mysql-exporter
  ports:
    - "9104:9104"
  environment:
    DATA_SOURCE_NAME: "exporter:exporter@tcp(mysql:3306)/"
  depends_on:
    mysql:
      condition: service_healthy
  networks:
    - portal-universe-net
  restart: unless-stopped
```

> **MySQL 사용자 생성 필요**: `infrastructure/mysql/init.sql`에 exporter 계정 추가

```sql
-- init.sql에 추가
CREATE USER IF NOT EXISTS 'exporter'@'%' IDENTIFIED BY 'exporter';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';
FLUSH PRIVILEGES;
```

#### redis_exporter

```yaml
redis-exporter:
  image: oliver006/redis_exporter:v1.66.0
  container_name: redis-exporter
  ports:
    - "9121:9121"
  environment:
    REDIS_ADDR: "redis://redis:6379"
  depends_on:
    redis:
      condition: service_healthy
  networks:
    - portal-universe-net
  restart: unless-stopped
```

#### kafka_exporter

```yaml
kafka-exporter:
  image: danielqsj/kafka-exporter:v1.8.0
  container_name: kafka-exporter
  ports:
    - "9308:9308"
  command:
    - "--kafka.server=kafka:29092"
    - "--topic.filter=.*"
    - "--group.filter=.*"
  depends_on:
    - kafka
  networks:
    - portal-universe-net
  restart: unless-stopped
```

### 2.2 docker-compose-local.yml 변경

로컬 개발에서도 Exporter를 사용하므로 동일하게 추가. 단, 타겟이 Docker 내부 서비스이므로 설정 동일.

### 2.3 Prometheus scrape config 변경

**`monitoring/prometheus/prometheus.yml`** — 주석 해제 + cAdvisor 추가:

```yaml
  # ========================================
  # cAdvisor (Container Metrics)
  # ========================================
  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']
    relabel_configs:
      - target_label: service
        replacement: 'cadvisor'

  # ========================================
  # MySQL Exporter
  # ========================================
  - job_name: 'mysql-exporter'
    static_configs:
      - targets: ['mysql-exporter:9104']
    relabel_configs:
      - target_label: service
        replacement: 'mysql'

  # ========================================
  # Redis Exporter
  # ========================================
  - job_name: 'redis-exporter'
    static_configs:
      - targets: ['redis-exporter:9121']
    relabel_configs:
      - target_label: service
        replacement: 'redis'

  # ========================================
  # Kafka Exporter
  # ========================================
  - job_name: 'kafka-exporter'
    static_configs:
      - targets: ['kafka-exporter:9308']
    relabel_configs:
      - target_label: service
        replacement: 'kafka'
```

**`monitoring/prometheus/prometheus-local.yml`** — 동일하게 추가 (Docker 내부 서비스이므로 호스트명 동일)

### 2.4 수집되는 핵심 메트릭

| Exporter | 메트릭 | 용도 |
|----------|--------|------|
| **cAdvisor** | `container_cpu_usage_seconds_total` | 컨테이너 CPU |
| | `container_memory_usage_bytes` | 컨테이너 메모리 |
| | `container_network_receive_bytes_total` | 네트워크 I/O |
| **mysqld** | `mysql_global_status_threads_connected` | 활성 연결 수 |
| | `mysql_global_status_slow_queries` | Slow Query 누적 |
| | `mysql_global_status_innodb_buffer_pool_reads` | Buffer Pool Miss |
| | `mysql_global_variables_max_connections` | 최대 연결 수 |
| **redis** | `redis_keyspace_hits_total` / `redis_keyspace_misses_total` | Hit Rate |
| | `redis_memory_used_bytes` | 메모리 사용량 |
| | `redis_commands_processed_total` | Commands/sec |
| | `redis_connected_clients` | 연결된 클라이언트 |
| **kafka** | `kafka_consumergroup_lag` | Consumer Lag |
| | `kafka_topic_partition_current_offset` | Topic Offset |
| | `kafka_brokers` | Broker 수 |

---

## 3. Phase 2: Zipkin ES Backend (S6)

### 3.1 docker-compose.yml 변경

**Before:**
```yaml
zipkin:
  image: openzipkin/zipkin:3.4.2
  container_name: zipkin
  ports:
    - "9411:9411"
  environment:
    - STORAGE_TYPE=mem
  networks:
    - portal-universe-net
```

**After:**
```yaml
zipkin:
  image: openzipkin/zipkin:3.4.2
  container_name: zipkin
  ports:
    - "9411:9411"
  environment:
    - STORAGE_TYPE=elasticsearch
    - ES_HOSTS=http://elasticsearch:9200
    - ES_INDEX=zipkin
    - ES_INDEX_REPLICAS=0
    - ES_INDEX_SHARDS=1
  depends_on:
    elasticsearch:
      condition: service_healthy
  networks:
    - portal-universe-net
```

> **ES_INDEX_REPLICAS=0**: 단일 노드이므로 replica 불필요
> **ES_INDEX_SHARDS=1**: 개발 환경에서는 1 shard 충분

### 3.2 검증 방법

```bash
# Zipkin 재시작 후 ES 인덱스 확인
curl http://localhost:9200/_cat/indices?v | grep zipkin
# zipkin-span-YYYY-MM-DD 인덱스가 생성되어야 함

# API 요청 후 트레이스 확인
curl http://localhost:9411/api/v2/traces?limit=5
```

---

## 4. Phase 3: k6 스크립트 구조 설계 (S1, S4)

### 4.1 디렉토리 구조

```
services/load-tests/
├── k6/
│   ├── scenarios/
│   │   ├── a-shopping-flow.js      # 시나리오 A: 쇼핑 E2E
│   │   ├── b-blog-read.js          # 시나리오 B: 블로그 Read
│   │   ├── c-coupon-spike.js       # 시나리오 C: 쿠폰 Spike
│   │   ├── d-search-load.js        # 시나리오 D: ES 검색
│   │   └── e-cache-thundering.js   # 시나리오 E: Thundering Herd
│   ├── lib/
│   │   ├── auth.js                 # JWT 토큰 획득 헬퍼
│   │   ├── config.js               # 환경별 BASE_URL 설정
│   │   └── checks.js               # 공통 check 함수
│   └── run.sh                      # 실행 헬퍼 스크립트
├── bots/
│   ├── kafka_producer.py           # Kafka 대량 발행 Bot
│   └── requirements.txt            # Python 의존성
└── README.md                       # 실행 방법 문서
```

### 4.2 공통 모듈 설계

#### `lib/config.js`

```javascript
const ENV = __ENV.TARGET_ENV || 'local';

const configs = {
  local: {
    BASE_URL: 'http://localhost:8080',
    AUTH_URL: 'http://localhost:8080/api/v1/auth',
  },
  docker: {
    BASE_URL: 'http://host.docker.internal:8080',
    AUTH_URL: 'http://host.docker.internal:8080/api/v1/auth',
  },
};

export const config = configs[ENV] || configs.local;
```

#### `lib/auth.js`

```javascript
import http from 'k6/http';
import { config } from './config.js';

export function login(email, password) {
  const res = http.post(`${config.AUTH_URL}/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (res.status !== 200) {
    console.error(`Login failed: ${res.status} ${res.body}`);
    return null;
  }

  return res.json('data.accessToken');
}

export function authHeaders(token) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };
}
```

#### `lib/checks.js`

```javascript
import { check } from 'k6';

export function checkStatus(res, name, expectedStatus = 200) {
  return check(res, {
    [`${name} status ${expectedStatus}`]: (r) => r.status === expectedStatus,
    [`${name} no error`]: (r) => !r.json('error'),
  });
}

export function checkApiResponse(res, name) {
  return check(res, {
    [`${name} status 200`]: (r) => r.status === 200,
    [`${name} success true`]: (r) => r.json('success') === true,
    [`${name} has data`]: (r) => r.json('data') !== null,
  });
}
```

### 4.3 시나리오 스크립트 설계

#### 시나리오 A: Shopping E2E Flow (`a-shopping-flow.js`)

```javascript
import http from 'k6/http';
import { sleep } from 'k6';
import { login, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';
import { checkApiResponse } from '../lib/checks.js';

export const options = {
  scenarios: {
    shopping_flow: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 100 },    // Ramp-up
        { duration: '10m', target: 100 },   // Steady
        { duration: '2m', target: 0 },      // Ramp-down
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<200', 'p(99)<500'],
    http_req_failed: ['rate<0.001'],
    'http_req_duration{name:login}': ['p(95)<150'],
    'http_req_duration{name:product_list}': ['p(95)<100'],
    'http_req_duration{name:product_detail}': ['p(95)<80'],
    'http_req_duration{name:add_to_cart}': ['p(95)<120'],
    'http_req_duration{name:create_order}': ['p(95)<300'],
  },
};

export function setup() {
  // 테스트용 계정으로 사전 토큰 발급
  const token = login('loadtest@test.com', 'loadtest123');
  return { token };
}

export default function (data) {
  const params = authHeaders(data.token);

  // 1. Product List
  const products = http.get(
    `${config.BASE_URL}/api/v1/products?page=0&size=20`,
    { ...params, tags: { name: 'product_list' } }
  );
  checkApiResponse(products, 'product_list');

  sleep(1);

  // 2. Product Detail (첫 번째 상품)
  const productId = products.json('data.content.0.id');
  if (productId) {
    const detail = http.get(
      `${config.BASE_URL}/api/v1/products/${productId}`,
      { ...params, tags: { name: 'product_detail' } }
    );
    checkApiResponse(detail, 'product_detail');
  }

  sleep(0.5);

  // 3. Add to Cart
  if (productId) {
    const cart = http.post(
      `${config.BASE_URL}/api/v1/cart/items`,
      JSON.stringify({ productId, quantity: 1 }),
      { ...params, tags: { name: 'add_to_cart' } }
    );
    checkApiResponse(cart, 'add_to_cart');
  }

  sleep(0.5);

  // 4. Create Order (선택적 - 데이터 오염 방지)
  // 부하 테스트 환경에서만 활성화
  // const order = http.post(
  //   `${config.BASE_URL}/api/v1/orders`,
  //   JSON.stringify({ ... }),
  //   { ...params, tags: { name: 'create_order' } }
  // );

  sleep(1);
}
```

> **VU 조정**: 로컬 환경에서는 100 VU로 시작. Docker Desktop 자원 한계 고려.
> **주문 생성 비활성화**: 기본적으로 주석 처리. 전용 테스트 DB에서만 활성화.

#### 시나리오 B: Blog Read (`b-blog-read.js`)

```javascript
import http from 'k6/http';
import { sleep } from 'k6';
import { login, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';
import { checkApiResponse } from '../lib/checks.js';

export const options = {
  scenarios: {
    blog_read: {
      executor: 'constant-arrival-rate',
      rate: 500,               // 초당 500 요청
      timeUnit: '1s',
      duration: '10m',
      preAllocatedVUs: 200,
      maxVUs: 500,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<100'],
    http_req_failed: ['rate<0.001'],
    'http_req_duration{name:post_list}': ['p(95)<80'],
    'http_req_duration{name:post_detail}': ['p(95)<50'],
  },
};

export function setup() {
  const token = login('loadtest@test.com', 'loadtest123');
  return { token };
}

export default function (data) {
  const params = authHeaders(data.token);

  // 70% 목록 조회, 30% 상세 조회 (실제 트래픽 패턴)
  if (Math.random() < 0.7) {
    const page = Math.floor(Math.random() * 10);
    const list = http.get(
      `${config.BASE_URL}/api/v1/posts?page=${page}&size=20`,
      { ...params, tags: { name: 'post_list' } }
    );
    checkApiResponse(list, 'post_list');
  } else {
    const postId = Math.floor(Math.random() * 100) + 1;
    const detail = http.get(
      `${config.BASE_URL}/api/v1/posts/${postId}`,
      { ...params, tags: { name: 'post_detail' } }
    );
    // 404는 정상 (존재하지 않는 ID)
  }

  sleep(0.1);
}
```

#### 시나리오 C: Coupon Spike (`c-coupon-spike.js`)

```javascript
import http from 'k6/http';
import { login, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';

export const options = {
  scenarios: {
    coupon_spike: {
      executor: 'shared-iterations',
      vus: 500,               // 500명이
      iterations: 500,        // 각 1회씩 = 총 500회 동시 요청
      maxDuration: '30s',
    },
  },
  thresholds: {
    http_req_duration: ['p(99)<500'],
    http_req_failed: ['rate<0.5'],  // 쿠폰 소진 시 실패는 정상
  },
};

export function setup() {
  const token = login('loadtest@test.com', 'loadtest123');
  // 테스트용 쿠폰 ID (사전 생성 필요)
  return { token, couponId: __ENV.COUPON_ID || '1' };
}

export default function (data) {
  const params = authHeaders(data.token);

  const res = http.post(
    `${config.BASE_URL}/api/v1/coupons/${data.couponId}/issue`,
    null,
    { ...params, tags: { name: 'coupon_issue' } }
  );

  // 성공 또는 SOLD_OUT 모두 정상 응답
  // 과발급 여부는 테스트 후 DB에서 검증
}
```

#### 시나리오 D: Search Load (`d-search-load.js`)

```javascript
import http from 'k6/http';
import { sleep } from 'k6';
import { login, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';

const SEARCH_TERMS = ['노트북', '스마트폰', '이어폰', '키보드', '마우스',
  '모니터', '태블릿', '카메라', '프린터', '스피커'];

export const options = {
  scenarios: {
    search_load: {
      executor: 'ramping-arrival-rate',
      startRate: 50,
      timeUnit: '1s',
      stages: [
        { duration: '3m', target: 200 },   // Ramp-up
        { duration: '10m', target: 200 },  // Steady
        { duration: '2m', target: 0 },     // Down
      ],
      preAllocatedVUs: 100,
      maxVUs: 300,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<100'],
    http_req_failed: ['rate<0.01'],
  },
};

export function setup() {
  const token = login('loadtest@test.com', 'loadtest123');
  return { token };
}

export default function (data) {
  const params = authHeaders(data.token);
  const term = SEARCH_TERMS[Math.floor(Math.random() * SEARCH_TERMS.length)];

  http.get(
    `${config.BASE_URL}/api/v1/products/search?keyword=${encodeURIComponent(term)}&page=0&size=20`,
    { ...params, tags: { name: 'product_search' } }
  );

  sleep(0.2);
}
```

#### 시나리오 E: Cache Thundering Herd (`e-cache-thundering.js`)

```javascript
import http from 'k6/http';
import { login, authHeaders } from '../lib/auth.js';
import { config } from '../lib/config.js';

export const options = {
  scenarios: {
    // Phase 1: 캐시 워밍
    cache_warm: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '10s',
      exec: 'warmCache',
      startTime: '0s',
    },
    // Phase 2: 캐시 만료 대기 후 동시 요청
    thundering_herd: {
      executor: 'shared-iterations',
      vus: 200,
      iterations: 200,
      maxDuration: '10s',
      exec: 'thunderingHerd',
      startTime: '15s',  // 캐시 TTL 이후
    },
  },
  thresholds: {
    'http_req_duration{scenario:thundering_herd}': ['p(95)<500'],
  },
};

const POPULAR_PRODUCT_ID = __ENV.PRODUCT_ID || '1';

export function setup() {
  const token = login('loadtest@test.com', 'loadtest123');
  return { token };
}

export function warmCache(data) {
  const params = authHeaders(data.token);
  http.get(`${config.BASE_URL}/api/v1/products/${POPULAR_PRODUCT_ID}`, params);
}

export function thunderingHerd(data) {
  const params = authHeaders(data.token);
  // 모든 VU가 동시에 같은 상품 조회 → 캐시 만료 직후
  http.get(
    `${config.BASE_URL}/api/v1/products/${POPULAR_PRODUCT_ID}`,
    { ...params, tags: { name: 'thundering_herd' } }
  );
}
```

### 4.4 실행 스크립트 (`run.sh`)

```bash
#!/bin/bash
# k6 부하 테스트 실행 헬퍼

SCENARIO=${1:-"a-shopping-flow"}
ENV=${2:-"local"}
PROMETHEUS_URL=${3:-"http://localhost:9090/api/v1/write"}

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
  "k6/scenarios/${SCENARIO}.js"
```

### 4.5 k6 Grafana 연동

k6는 `--out experimental-prometheus-rw` 옵션으로 Prometheus에 직접 Remote Write.

**Prometheus Remote Write 활성화** (`monitoring/prometheus/prometheus.yml`에 추가 불필요):
- k6가 직접 Prometheus API에 Write하므로 Prometheus 설정 변경 없음
- 단, Prometheus의 `--web.enable-remote-write-receiver` 플래그 필요

**docker-compose.yml의 Prometheus command 수정:**

```yaml
prometheus:
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.path=/prometheus'
    - '--storage.tsdb.retention.time=30d'
    - '--web.enable-lifecycle'
    - '--web.enable-remote-write-receiver'  # 추가
```

---

## 5. Phase 4: Grafana 대시보드 (S3)

### 5.1 Load Test Overview 대시보드

**파일**: `monitoring/grafana/provisioning/dashboards/json/load-test-overview.json`

**패널 구성 (4행 × 3열 = 12패널)**:

```
┌─────────────────┬─────────────────┬─────────────────┐
│ k6: Total RPS   │ k6: Error Rate  │ k6: Active VUs  │
│ (gauge)         │ (stat + color)  │ (time series)   │
├─────────────────┼─────────────────┼─────────────────┤
│ k6: Response    │ k6: p95 Latency │ k6: p99 Latency │
│ Time (heatmap)  │ per endpoint    │ per endpoint    │
├─────────────────┼─────────────────┼─────────────────┤
│ CPU: Per Service│ Memory: Per Svc │ Network I/O     │
│ (cAdvisor)      │ (cAdvisor)      │ (cAdvisor)      │
├─────────────────┼─────────────────┼─────────────────┤
│ MySQL: Active   │ Redis: Hit Rate │ Kafka: Consumer │
│ Connections     │ & Memory        │ Lag             │
└─────────────────┴─────────────────┴─────────────────┘
```

**핵심 PromQL 쿼리**:

| 패널 | 쿼리 |
|------|------|
| k6 Total RPS | `sum(rate(k6_http_reqs_total[1m]))` |
| k6 Error Rate | `sum(rate(k6_http_reqs_total{expected_response="false"}[1m])) / sum(rate(k6_http_reqs_total[1m]))` |
| k6 Active VUs | `k6_vus` |
| k6 p95 Latency | `histogram_quantile(0.95, sum(rate(k6_http_req_duration_seconds_bucket[1m])) by (le, name))` |
| CPU per service | `rate(container_cpu_usage_seconds_total{name=~".*-service\|api-gateway"}[1m])` |
| Memory per service | `container_memory_usage_bytes{name=~".*-service\|api-gateway"}` |
| MySQL Connections | `mysql_global_status_threads_connected` |
| Redis Hit Rate | `rate(redis_keyspace_hits_total[1m]) / (rate(redis_keyspace_hits_total[1m]) + rate(redis_keyspace_misses_total[1m]))` |
| Kafka Consumer Lag | `sum(kafka_consumergroup_lag) by (consumergroup, topic)` |

### 5.2 Bottleneck Detection 대시보드

**파일**: `monitoring/grafana/provisioning/dashboards/json/bottleneck-detection.json`

**패널 구성 (RED + USE Method)**:

```
┌─────────────────────────────────────────────────────┐
│              Service Latency Heatmap                │
│  (X: time, Y: service, Color: p95 latency)         │
├─────────────────┬─────────────────┬─────────────────┤
│ RED: Request    │ RED: Error Rate │ RED: Duration   │
│ Rate per Svc    │ per Service     │ p50/p95/p99     │
├─────────────────┼─────────────────┼─────────────────┤
│ USE: CPU        │ USE: Memory     │ USE: Network    │
│ Utilization     │ Saturation      │ Errors          │
├─────────────────┼─────────────────┼─────────────────┤
│ DB Pool: Active │ DB Pool: Wait   │ HikariCP:       │
│ vs Max          │ Time            │ Timeout Count   │
├─────────────────┼─────────────────┼─────────────────┤
│ Redis: Ops/sec  │ Redis: Eviction │ Redis: Memory   │
│ by command      │ Rate            │ vs maxmemory    │
├─────────────────┼─────────────────┼─────────────────┤
│ Kafka: Produce  │ Kafka: Consume  │ Kafka: Lag      │
│ Rate            │ Rate            │ Trend           │
└─────────────────┴─────────────────┴─────────────────┘
```

**핵심 PromQL 쿼리 (추가)**:

| 패널 | 쿼리 |
|------|------|
| Service Latency Heatmap | `job:http_latency_p95:rate5m` (recording rule 활용) |
| HikariCP Active | `hikaricp_connections_active{service=~"$service"}` |
| HikariCP Wait | `hikaricp_connections_pending{service=~"$service"}` |
| HikariCP Timeout | `increase(hikaricp_connections_timeout_total[5m])` |
| Redis Ops/sec | `rate(redis_commands_total[1m])` |
| Redis Eviction | `rate(redis_evicted_keys_total[1m])` |
| Kafka Produce Rate | `sum(rate(kafka_topic_partition_current_offset[1m])) by (topic)` |

### 5.3 대시보드 Variables

두 대시보드 모두 공통 변수:

```json
{
  "templating": {
    "list": [
      {
        "name": "service",
        "type": "query",
        "query": "label_values(up{job=~\".*-service|api-gateway\"}, job)"
      },
      {
        "name": "interval",
        "type": "interval",
        "options": ["1m", "5m", "15m"]
      }
    ]
  }
}
```

---

## 6. Phase 5: Kafka Custom Bot (S5)

### 6.1 Python 스크립트 설계

**`bots/kafka_producer.py`**:

```python
"""
Kafka Load Test Bot
- Prometheus 메트릭 노출 (:8000/metrics)
- 지정된 속도로 Kafka 메시지 발행
"""
import argparse
import json
import time
import random
import uuid
from datetime import datetime
from kafka import KafkaProducer
from prometheus_client import start_http_server, Counter, Histogram, Gauge

# Prometheus Metrics
messages_sent = Counter('kafka_bot_messages_sent_total', 'Total messages sent', ['topic'])
send_duration = Histogram('kafka_bot_send_duration_seconds', 'Send duration', ['topic'])
send_errors = Counter('kafka_bot_send_errors_total', 'Send errors', ['topic'])
target_rate = Gauge('kafka_bot_target_rate', 'Target messages per second')
actual_rate = Gauge('kafka_bot_actual_rate', 'Actual messages per second')

def create_order_event():
    return {
        'eventType': 'ORDER_CREATED',
        'orderId': str(uuid.uuid4()),
        'userId': random.randint(1, 10000),
        'totalAmount': round(random.uniform(1000, 500000), 0),
        'items': [
            {'productId': random.randint(1, 1000), 'quantity': random.randint(1, 5)}
        ],
        'createdAt': datetime.now().isoformat()
    }

def run(bootstrap_servers, topic, rate, duration):
    producer = KafkaProducer(
        bootstrap_servers=bootstrap_servers,
        value_serializer=lambda v: json.dumps(v).encode('utf-8'),
        acks='all',
        retries=3,
    )

    target_rate.set(rate)
    interval = 1.0 / rate
    sent_count = 0
    start_time = time.time()

    print(f"Starting: {rate} msg/s for {duration}s to {topic}")

    while time.time() - start_time < duration:
        loop_start = time.time()
        try:
            event = create_order_event()
            with send_duration.labels(topic=topic).time():
                producer.send(topic, value=event)
            messages_sent.labels(topic=topic).inc()
            sent_count += 1
        except Exception as e:
            send_errors.labels(topic=topic).inc()
            print(f"Error: {e}")

        elapsed = time.time() - start_time
        if elapsed > 0:
            actual_rate.set(sent_count / elapsed)

        sleep_time = interval - (time.time() - loop_start)
        if sleep_time > 0:
            time.sleep(sleep_time)

    producer.flush()
    producer.close()
    print(f"Done: {sent_count} messages in {duration}s ({sent_count/duration:.1f} msg/s)")

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--bootstrap-servers', default='localhost:9092')
    parser.add_argument('--topic', default='order-created')
    parser.add_argument('--rate', type=int, default=100, help='Messages per second')
    parser.add_argument('--duration', type=int, default=60, help='Duration in seconds')
    parser.add_argument('--metrics-port', type=int, default=8000)
    args = parser.parse_args()

    start_http_server(args.metrics_port)
    run(args.bootstrap_servers, args.topic, args.rate, args.duration)
```

**`bots/requirements.txt`**:

```
kafka-python-ng>=2.2.2
prometheus-client>=0.21.0
```

### 6.2 Prometheus 연동

Kafka Bot이 `:8000/metrics`로 Prometheus 메트릭을 노출하므로:

**prometheus.yml에 추가 (테스트 실행 시에만)**:

```yaml
  # Kafka Load Test Bot (테스트 시에만 활성화)
  # - job_name: 'kafka-bot'
  #   static_configs:
  #     - targets: ['host.docker.internal:8000']
  #   relabel_configs:
  #     - target_label: service
  #       replacement: 'kafka-bot'
```

### 6.3 실행 예시

```bash
# 100 msg/s for 5분
python bots/kafka_producer.py \
  --bootstrap-servers localhost:9092 \
  --topic order-created \
  --rate 100 \
  --duration 300

# Spike: 1000 msg/s for 1분
python bots/kafka_producer.py \
  --rate 1000 \
  --duration 60
```

---

## 7. 수정 대상 파일 목록

### 신규 생성

| # | 파일 | 설명 |
|---|------|------|
| 1 | `services/load-tests/k6/lib/config.js` | 환경별 설정 |
| 2 | `services/load-tests/k6/lib/auth.js` | 인증 헬퍼 |
| 3 | `services/load-tests/k6/lib/checks.js` | 공통 check |
| 4 | `services/load-tests/k6/scenarios/a-shopping-flow.js` | 시나리오 A |
| 5 | `services/load-tests/k6/scenarios/b-blog-read.js` | 시나리오 B |
| 6 | `services/load-tests/k6/scenarios/c-coupon-spike.js` | 시나리오 C |
| 7 | `services/load-tests/k6/scenarios/d-search-load.js` | 시나리오 D |
| 8 | `services/load-tests/k6/scenarios/e-cache-thundering.js` | 시나리오 E |
| 9 | `services/load-tests/k6/run.sh` | 실행 헬퍼 |
| 10 | `services/load-tests/bots/kafka_producer.py` | Kafka Bot |
| 11 | `services/load-tests/bots/requirements.txt` | Python deps |
| 12 | `services/load-tests/README.md` | 사용법 문서 |
| 13 | `monitoring/grafana/provisioning/dashboards/json/load-test-overview.json` | 대시보드 |
| 14 | `monitoring/grafana/provisioning/dashboards/json/bottleneck-detection.json` | 대시보드 |

### 수정

| # | 파일 | 변경 내용 |
|---|------|----------|
| 15 | `docker-compose.yml` | Exporter 4종 추가, Zipkin ES 전환, Prometheus remote-write 플래그 |
| 16 | `docker-compose-local.yml` | Exporter 4종 추가 |
| 17 | `monitoring/prometheus/prometheus.yml` | Exporter scrape config 주석 해제 + cAdvisor 추가 |
| 18 | `monitoring/prometheus/prometheus-local.yml` | Exporter scrape config 추가 |
| 19 | `infrastructure/mysql/init.sql` | exporter 계정 추가 |

---

## 8. 구현 순서 체크리스트

### Phase 1: Exporter + Prometheus (예상 변경: 4파일)
- [ ] `infrastructure/mysql/init.sql` — exporter 계정 추가
- [ ] `docker-compose.yml` — cAdvisor, mysqld_exporter, redis_exporter, kafka_exporter 추가
- [ ] `docker-compose-local.yml` — 동일하게 추가
- [ ] `monitoring/prometheus/prometheus.yml` — scrape config 주석 해제 + cAdvisor
- [ ] `monitoring/prometheus/prometheus-local.yml` — scrape config 추가
- [ ] 검증: `docker compose up -d` → Prometheus Targets 페이지에서 4종 UP 확인

### Phase 2: Zipkin ES Backend (예상 변경: 1파일)
- [ ] `docker-compose.yml` — Zipkin STORAGE_TYPE=elasticsearch
- [ ] 검증: ES 인덱스 생성 확인, Zipkin UI에서 트레이스 조회

### Phase 3: k6 스크립트 (예상 생성: 9파일)
- [ ] `services/load-tests/` 디렉토리 구조 생성
- [ ] `lib/config.js`, `lib/auth.js`, `lib/checks.js` 생성
- [ ] 시나리오 A~E 스크립트 5개 생성
- [ ] `run.sh` 실행 헬퍼 생성
- [ ] `docker-compose.yml` — Prometheus `--web.enable-remote-write-receiver` 추가
- [ ] 검증: `k6 run` → Grafana에서 k6 메트릭 확인

### Phase 4: Grafana 대시보드 (예상 생성: 2파일)
- [ ] `load-test-overview.json` 생성
- [ ] `bottleneck-detection.json` 생성
- [ ] 검증: Grafana에서 대시보드 자동 로드 확인

### Phase 5: Kafka Bot (예상 생성: 3파일)
- [ ] `bots/kafka_producer.py` 생성
- [ ] `bots/requirements.txt` 생성
- [ ] `README.md` 생성
- [ ] 검증: Bot 실행 → Kafka Consumer Lag 증가 → Grafana에서 확인

---

## 9. 테스트 데이터 요구사항

| 항목 | 설명 | 생성 방법 |
|------|------|----------|
| 테스트 계정 | `loadtest@test.com` / `loadtest123` | Auth API로 회원가입 또는 seed data |
| 상품 데이터 | 최소 100개 상품 | Shopping DB seed |
| 블로그 데이터 | 최소 100개 포스트 | Blog DB seed |
| 쿠폰 데이터 | 수량 제한 쿠폰 1개 | Shopping API로 생성 |

> seed data 스크립트는 Out-of-Scope. 수동으로 API 호출 또는 기존 개발 데이터 활용.

---

## 10. Verification Plan

| 단계 | 검증 방법 | 성공 기준 |
|------|----------|----------|
| Exporter | `curl localhost:9104/metrics` (각 포트) | 메트릭 응답 200 |
| Prometheus | Prometheus UI → Status → Targets | 4종 모두 UP |
| Zipkin ES | `curl localhost:9200/_cat/indices \| grep zipkin` | 인덱스 존재 |
| k6 실행 | `k6 run scenarios/a-shopping-flow.js` | 에러 없이 완료 |
| k6 → Grafana | Load Test Overview 대시보드 | k6 메트릭 패널에 데이터 표시 |
| Kafka Bot | `python kafka_producer.py --rate 100 --duration 30` | 100 msg/s 달성 |
| 대시보드 | 부하 테스트 중 Bottleneck Detection | 서비스별 지표 실시간 갱신 |
