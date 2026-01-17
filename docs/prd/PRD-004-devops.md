# PRD-004: 운영 / DevOps

## 1. 개요

### 1.1 목적
프로덕션 레벨의 CI/CD 파이프라인, 부하 테스트, 모니터링 시스템을 구축하여 안정적인 운영 환경 확보

### 1.2 배경
- Phase 1~3에서 핵심 비즈니스 기능 구현 완료
- 수동 배포 및 모니터링의 한계
- 성능 검증 없이 프로덕션 배포 위험
- 장애 대응 체계 부재

### 1.3 범위
| 기능 | 설명 | 우선순위 |
|------|------|----------|
| CI/CD | 자동 빌드/테스트/배포 | P0 |
| 부하 테스트 | 성능 검증 자동화 | P0 |
| 모니터링 | 메트릭/로그/알림 | P0 |
| IaC | 인프라 코드화 | P1 |

---

## 2. 기술 스택

### 2.1 CI/CD
| 기술 | 용도 |
|------|------|
| GitHub Actions | CI/CD 파이프라인 |
| Docker | 컨테이너 빌드 |
| Kubernetes | 컨테이너 오케스트레이션 |
| Helm | K8s 패키지 매니저 |
| ArgoCD | GitOps 배포 (선택) |

### 2.2 테스트
| 기술 | 용도 |
|------|------|
| k6 | 부하 테스트 |
| Gatling | 성능 테스트 (대안) |
| JaCoCo | 코드 커버리지 |
| SonarQube | 코드 품질 (선택) |

### 2.3 모니터링
| 기술 | 용도 |
|------|------|
| Prometheus | 메트릭 수집 |
| Grafana | 대시보드/시각화 |
| Alertmanager | 알림 |
| Loki | 로그 집계 |
| Zipkin | 분산 추적 |

---

## 3. CI/CD 파이프라인

### 3.1 파이프라인 개요

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CI/CD Pipeline                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  [Push/PR] ──► [Build] ──► [Test] ──► [Scan] ──► [Docker] ──► [Deploy]     │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ PR to main                                                          │   │
│  │  ├── Build (Gradle/npm)                                             │   │
│  │  ├── Unit Tests                                                     │   │
│  │  ├── Integration Tests                                              │   │
│  │  ├── Code Coverage (JaCoCo)                                         │   │
│  │  └── Security Scan (선택)                                            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Merge to main                                                       │   │
│  │  ├── Docker Build (multi-platform)                                  │   │
│  │  ├── Push to Registry                                               │   │
│  │  ├── Update K8s manifests                                           │   │
│  │  └── Deploy to Staging                                              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Release Tag (v*.*.*)                                                │   │
│  │  ├── Load Test (k6)                                                 │   │
│  │  ├── Deploy to Production                                           │   │
│  │  └── Notify (Slack/Discord)                                         │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 GitHub Actions Workflow

#### 3.2.1 PR 검증 (ci.yml)

```yaml
# .github/workflows/ci.yml
name: CI

on:
  pull_request:
    branches: [main, dev]
  push:
    branches: [main, dev]

env:
  JAVA_VERSION: '17'
  NODE_VERSION: '20'

jobs:
  # ========================================
  # Backend Build & Test
  # ========================================
  backend:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: password
          MYSQL_DATABASE: test_db
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping"
          --health-interval=10s
          --health-timeout=5s
          --health-retries=5

      mongodb:
        image: mongo:6.0
        ports:
          - 27017:27017

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'gradle'

      - name: Grant execute permission
        run: chmod +x gradlew

      - name: Build
        run: ./gradlew build -x test

      - name: Run Tests
        run: ./gradlew test
        env:
          SPRING_PROFILES_ACTIVE: test

      - name: Generate Coverage Report
        run: ./gradlew jacocoTestReport

      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          files: '**/build/reports/jacoco/test/jacocoTestReport.xml'
          fail_ci_if_error: false

      - name: Upload Test Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: backend-test-results
          path: '**/build/test-results/test/*.xml'

  # ========================================
  # Frontend Build & Test
  # ========================================
  frontend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend

    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Lint
        run: npm run lint

      - name: Type Check
        run: npm run type-check

      - name: Unit Tests
        run: npm run test -- --coverage

      - name: Build
        run: npm run build

      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          files: frontend/coverage/lcov.info
          fail_ci_if_error: false

  # ========================================
  # E2E Tests (PR only)
  # ========================================
  e2e:
    runs-on: ubuntu-latest
    needs: [backend, frontend]
    if: github.event_name == 'pull_request'

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Compose
        run: docker-compose -f docker-compose.test.yml up -d

      - name: Wait for services
        run: |
          timeout 120 bash -c 'until curl -s http://localhost:8080/actuator/health | grep -q "UP"; do sleep 5; done'

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}

      - name: Install Playwright
        run: |
          cd e2e-tests
          npm ci
          npx playwright install --with-deps

      - name: Run E2E Tests
        run: |
          cd e2e-tests
          npm run test

      - name: Upload E2E Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: e2e-results
          path: e2e-tests/test-results/

      - name: Cleanup
        if: always()
        run: docker-compose -f docker-compose.test.yml down -v
```

#### 3.2.2 Docker 빌드 및 푸시 (docker.yml)

```yaml
# .github/workflows/docker.yml
name: Docker Build & Push

on:
  push:
    branches: [main]
    tags: ['v*.*.*']

env:
  REGISTRY: docker.io
  IMAGE_PREFIX: lazehub

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service:
          - api-gateway
          - auth-service
          - blog-service
          - shopping-service
          - notification-service
          - config-service

    steps:
      - uses: actions/checkout@v4

      - name: Set up QEMU
        uses: docker/setup-qemu-action@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}/${{ matrix.service }}
          tags: |
            type=ref,event=branch
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=sha,prefix=

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          file: services/${{ matrix.service }}/Dockerfile
          platforms: linux/amd64,linux/arm64
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # Frontend 빌드
  build-frontend:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        app:
          - portal-shell
          - blog-frontend

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}/${{ matrix.app }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: frontend
          file: frontend/${{ matrix.app }}/Dockerfile
          platforms: linux/amd64,linux/arm64
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

#### 3.2.3 배포 (deploy.yml)

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  workflow_run:
    workflows: ["Docker Build & Push"]
    types: [completed]
    branches: [main]

  workflow_dispatch:
    inputs:
      environment:
        description: 'Deployment environment'
        required: true
        default: 'staging'
        type: choice
        options:
          - staging
          - production

jobs:
  deploy-staging:
    runs-on: ubuntu-latest
    if: >
      github.event_name == 'workflow_dispatch' && github.event.inputs.environment == 'staging' ||
      github.event_name == 'workflow_run' && github.event.workflow_run.conclusion == 'success'
    environment: staging

    steps:
      - uses: actions/checkout@v4

      - name: Set up kubectl
        uses: azure/setup-kubectl@v3

      - name: Configure kubeconfig
        run: |
          mkdir -p ~/.kube
          echo "${{ secrets.KUBE_CONFIG_STAGING }}" | base64 -d > ~/.kube/config

      - name: Update image tags
        run: |
          cd k8s/services
          for file in *.yaml; do
            sed -i "s|:latest|:${{ github.sha }}|g" "$file"
          done

      - name: Deploy to Staging
        run: |
          kubectl apply -k k8s/overlays/staging
          kubectl rollout status deployment -n portal-universe --timeout=300s

      - name: Verify deployment
        run: |
          kubectl get pods -n portal-universe
          kubectl get svc -n portal-universe

      - name: Notify Slack
        if: always()
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "Staging deployment ${{ job.status }}: ${{ github.repository }}@${{ github.sha }}"
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}

  deploy-production:
    runs-on: ubuntu-latest
    if: github.event_name == 'workflow_dispatch' && github.event.inputs.environment == 'production'
    environment: production
    needs: [load-test]  # 부하 테스트 통과 후 배포

    steps:
      - uses: actions/checkout@v4

      - name: Configure kubeconfig
        run: |
          mkdir -p ~/.kube
          echo "${{ secrets.KUBE_CONFIG_PRODUCTION }}" | base64 -d > ~/.kube/config

      - name: Deploy to Production (Blue-Green)
        run: |
          # Blue-Green 배포 스크립트
          ./k8s/scripts/blue-green-deploy.sh

      - name: Health Check
        run: |
          timeout 60 bash -c 'until curl -sf https://api.portal-universe.com/actuator/health; do sleep 5; done'

      - name: Notify
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "🚀 Production deployment completed: ${{ github.repository }}@${{ github.ref_name }}"
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}

  load-test:
    runs-on: ubuntu-latest
    needs: [deploy-staging]
    if: github.event.inputs.environment == 'production'

    steps:
      - uses: actions/checkout@v4

      - name: Run k6 Load Test
        uses: grafana/k6-action@v0.3.1
        with:
          filename: k6/load-test.js
          flags: --out json=results.json
        env:
          K6_TARGET_URL: ${{ secrets.STAGING_URL }}

      - name: Check Results
        run: |
          # 성능 기준 검증
          python3 k6/check-results.py results.json

      - name: Upload Results
        uses: actions/upload-artifact@v4
        with:
          name: load-test-results
          path: results.json
```

### 3.3 Dockerfile 최적화

```dockerfile
# services/auth-service/Dockerfile
# Multi-stage build for optimized image

# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Gradle wrapper 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY services/common-library services/common-library
COPY services/auth-service services/auth-service

# 의존성 캐싱
RUN ./gradlew :services:auth-service:dependencies --no-daemon

# 빌드
RUN ./gradlew :services:auth-service:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 보안: non-root 사용자
RUN addgroup -g 1000 spring && adduser -u 1000 -G spring -s /bin/sh -D spring
USER spring:spring

# JAR 복사
COPY --from=builder /app/services/auth-service/build/libs/*.jar app.jar

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

EXPOSE 8081

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

---

## 4. 부하 테스트

### 4.1 테스트 시나리오

| 시나리오 | 설명 | 목표 |
|----------|------|------|
| Smoke | 기본 기능 검증 | 에러 없음 |
| Load | 예상 부하 테스트 | 1,000 VU, p95 < 500ms |
| Stress | 한계 테스트 | 최대 처리량 확인 |
| Spike | 급격한 부하 | 복구 시간 확인 |
| Soak | 장시간 테스트 | 메모리 누수 확인 |

### 4.2 k6 스크립트

#### 4.2.1 기본 부하 테스트

```javascript
// k6/load-test.js
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// 커스텀 메트릭
const errorRate = new Rate('errors');
const productSearchTrend = new Trend('product_search_duration');

// 설정
export const options = {
  scenarios: {
    // Smoke Test
    smoke: {
      executor: 'constant-vus',
      vus: 5,
      duration: '1m',
      tags: { scenario: 'smoke' },
    },
    // Load Test
    load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 100 },   // ramp up
        { duration: '5m', target: 100 },   // stay
        { duration: '2m', target: 500 },   // ramp up
        { duration: '5m', target: 500 },   // stay
        { duration: '2m', target: 1000 },  // ramp up
        { duration: '5m', target: 1000 },  // stay
        { duration: '2m', target: 0 },     // ramp down
      ],
      tags: { scenario: 'load' },
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
    errors: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.K6_TARGET_URL || 'http://localhost:8080';

// 테스트 데이터
const users = JSON.parse(open('./data/users.json'));

export function setup() {
  // 테스트 사용자 로그인 및 토큰 획득
  const tokens = users.map(user => {
    const res = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify(user), {
      headers: { 'Content-Type': 'application/json' },
    });
    return res.json('data.accessToken');
  });
  return { tokens };
}

export default function(data) {
  const token = data.tokens[Math.floor(Math.random() * data.tokens.length)];
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  group('상품 조회', () => {
    // 상품 목록 조회
    let res = http.get(`${BASE_URL}/api/v1/shopping/products?page=0&size=20`, { headers });
    check(res, {
      'products list status 200': (r) => r.status === 200,
      'products list has data': (r) => r.json('data.content').length > 0,
    }) || errorRate.add(1);

    // 상품 상세 조회
    res = http.get(`${BASE_URL}/api/v1/shopping/products/1`, { headers });
    check(res, {
      'product detail status 200': (r) => r.status === 200,
    }) || errorRate.add(1);

    sleep(1);
  });

  group('상품 검색', () => {
    const start = Date.now();
    const res = http.get(`${BASE_URL}/api/search/products?keyword=노트북&size=20`, { headers });
    productSearchTrend.add(Date.now() - start);

    check(res, {
      'search status 200': (r) => r.status === 200,
    }) || errorRate.add(1);

    sleep(0.5);
  });

  group('장바구니', () => {
    // 장바구니 조회
    let res = http.get(`${BASE_URL}/api/v1/shopping/cart`, { headers });
    check(res, {
      'cart status 200': (r) => r.status === 200,
    }) || errorRate.add(1);

    // 장바구니 추가
    res = http.post(`${BASE_URL}/api/v1/shopping/cart/items`, JSON.stringify({
      productId: Math.floor(Math.random() * 100) + 1,
      quantity: 1,
    }), { headers });
    check(res, {
      'add to cart status 200': (r) => r.status === 200 || r.status === 409,
    }) || errorRate.add(1);

    sleep(1);
  });
}

export function teardown(data) {
  // 정리 작업
  console.log('Test completed');
}
```

#### 4.2.2 쿠폰 동시성 테스트

```javascript
// k6/coupon-stress.js
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const successCount = new Counter('coupon_success');
const failCount = new Counter('coupon_fail');

export const options = {
  scenarios: {
    coupon_rush: {
      executor: 'shared-iterations',
      vus: 1000,
      iterations: 10000,
      maxDuration: '1m',
    },
  },
};

const BASE_URL = __ENV.K6_TARGET_URL || 'http://localhost:8080';
const COUPON_ID = __ENV.COUPON_ID || '1';

export default function() {
  const token = __ENV.TEST_TOKEN;
  const res = http.post(
    `${BASE_URL}/api/v1/shopping/coupons/${COUPON_ID}/issue`,
    null,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    }
  );

  if (res.status === 200) {
    successCount.add(1);
  } else {
    failCount.add(1);
  }

  check(res, {
    'status is 200 or 409': (r) => r.status === 200 || r.status === 409,
  });
}

export function handleSummary(data) {
  const success = data.metrics.coupon_success?.values?.count || 0;
  const fail = data.metrics.coupon_fail?.values?.count || 0;

  console.log(`\n=== Coupon Issue Results ===`);
  console.log(`Success: ${success}`);
  console.log(`Fail: ${fail}`);
  console.log(`Total: ${success + fail}`);

  return {
    'stdout': JSON.stringify({
      success,
      fail,
      total: success + fail,
    }, null, 2),
  };
}
```

### 4.3 결과 검증 스크립트

```python
# k6/check-results.py
import json
import sys

def check_results(filepath):
    with open(filepath, 'r') as f:
        results = json.load(f)

    metrics = results.get('metrics', {})

    # 기준 정의
    thresholds = {
        'http_req_duration_p95': 500,    # ms
        'http_req_duration_p99': 1000,   # ms
        'http_req_failed_rate': 0.01,    # 1%
        'errors_rate': 0.01,             # 1%
    }

    failures = []

    # p95 체크
    p95 = metrics.get('http_req_duration', {}).get('values', {}).get('p(95)', 0)
    if p95 > thresholds['http_req_duration_p95']:
        failures.append(f"p95 latency {p95}ms exceeds {thresholds['http_req_duration_p95']}ms")

    # p99 체크
    p99 = metrics.get('http_req_duration', {}).get('values', {}).get('p(99)', 0)
    if p99 > thresholds['http_req_duration_p99']:
        failures.append(f"p99 latency {p99}ms exceeds {thresholds['http_req_duration_p99']}ms")

    # 에러율 체크
    error_rate = metrics.get('http_req_failed', {}).get('values', {}).get('rate', 0)
    if error_rate > thresholds['http_req_failed_rate']:
        failures.append(f"Error rate {error_rate:.2%} exceeds {thresholds['http_req_failed_rate']:.2%}")

    if failures:
        print("❌ Load test FAILED:")
        for f in failures:
            print(f"  - {f}")
        sys.exit(1)
    else:
        print("✅ Load test PASSED")
        print(f"  - p95: {p95}ms")
        print(f"  - p99: {p99}ms")
        print(f"  - Error rate: {error_rate:.2%}")
        sys.exit(0)

if __name__ == '__main__':
    check_results(sys.argv[1])
```

---

## 5. 모니터링 시스템

### 5.1 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Monitoring Architecture                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                     │
│  │   Service   │    │   Service   │    │   Service   │                     │
│  │  (metrics)  │    │   (logs)    │    │  (traces)   │                     │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘                     │
│         │                  │                  │                             │
│         ▼                  ▼                  ▼                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                     │
│  │ Prometheus  │    │    Loki     │    │   Zipkin    │                     │
│  │  (scrape)   │    │  (collect)  │    │  (collect)  │                     │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘                     │
│         │                  │                  │                             │
│         └────────┬─────────┴─────────┬────────┘                             │
│                  │                   │                                      │
│                  ▼                   ▼                                      │
│           ┌─────────────┐     ┌─────────────┐                              │
│           │   Grafana   │     │ Alertmanager│                              │
│           │ (dashboard) │     │  (alerts)   │                              │
│           └─────────────┘     └──────┬──────┘                              │
│                                      │                                      │
│                                      ▼                                      │
│                               ┌─────────────┐                              │
│                               │Slack/Discord│                              │
│                               │   PagerDuty │                              │
│                               └─────────────┘                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Prometheus 설정

```yaml
# monitoring/prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

rule_files:
  - '/etc/prometheus/rules/*.yml'

scrape_configs:
  # API Gateway
  - job_name: 'api-gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['api-gateway:8080']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'api-gateway'

  # Auth Service
  - job_name: 'auth-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['auth-service:8081']

  # Shopping Service
  - job_name: 'shopping-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['shopping-service:8083']

  # Blog Service
  - job_name: 'blog-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['blog-service:8082']

  # Notification Service
  - job_name: 'notification-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['notification-service:8084']

  # Redis
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

  # MySQL
  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql-exporter:9104']

  # Elasticsearch
  - job_name: 'elasticsearch'
    static_configs:
      - targets: ['elasticsearch-exporter:9114']

  # Kafka
  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka-exporter:9308']
```

### 5.3 Alert Rules

```yaml
# monitoring/prometheus/rules/alerts.yml
groups:
  - name: service-alerts
    rules:
      # 서비스 다운
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"
          description: "{{ $labels.job }} has been down for more than 1 minute."

      # 높은 에러율
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)
          /
          sum(rate(http_server_requests_seconds_count[5m])) by (job)
          > 0.05
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High error rate on {{ $labels.job }}"
          description: "Error rate is {{ $value | humanizePercentage }} on {{ $labels.job }}."

      # 높은 레이턴시
      - alert: HighLatency
        expr: |
          histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job))
          > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High latency on {{ $labels.job }}"
          description: "p95 latency is {{ $value | humanizeDuration }} on {{ $labels.job }}."

      # 메모리 부족
      - alert: HighMemoryUsage
        expr: |
          jvm_memory_used_bytes{area="heap"}
          /
          jvm_memory_max_bytes{area="heap"}
          > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage on {{ $labels.job }}"
          description: "Heap memory usage is {{ $value | humanizePercentage }}."

      # 디스크 부족
      - alert: LowDiskSpace
        expr: |
          (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"})
          < 0.15
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Low disk space"
          description: "Disk space is below 15%."

  - name: database-alerts
    rules:
      # MySQL 연결 부족
      - alert: MySQLConnectionsHigh
        expr: |
          mysql_global_status_threads_connected
          /
          mysql_global_variables_max_connections
          > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "MySQL connections running high"

      # Redis 메모리
      - alert: RedisMemoryHigh
        expr: |
          redis_memory_used_bytes / redis_memory_max_bytes > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis memory usage is high"

  - name: business-alerts
    rules:
      # 주문 실패율
      - alert: HighOrderFailureRate
        expr: |
          sum(rate(order_created_total{status="failed"}[5m]))
          /
          sum(rate(order_created_total[5m]))
          > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High order failure rate"
          description: "Order failure rate is {{ $value | humanizePercentage }}."

      # 결제 실패율
      - alert: HighPaymentFailureRate
        expr: |
          sum(rate(payment_processed_total{status="failed"}[5m]))
          /
          sum(rate(payment_processed_total[5m]))
          > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High payment failure rate"
```

### 5.4 Alertmanager 설정

```yaml
# monitoring/alertmanager/alertmanager.yml
global:
  resolve_timeout: 5m
  slack_api_url: 'https://hooks.slack.com/services/xxx/yyy/zzz'

route:
  group_by: ['alertname', 'job']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'slack-notifications'

  routes:
    # Critical alerts -> PagerDuty + Slack
    - match:
        severity: critical
      receiver: 'pagerduty-critical'
      continue: true

    # Warning alerts -> Slack only
    - match:
        severity: warning
      receiver: 'slack-notifications'

receivers:
  - name: 'slack-notifications'
    slack_configs:
      - channel: '#portal-alerts'
        username: 'Prometheus'
        icon_emoji: ':prometheus:'
        send_resolved: true
        title: '{{ .Status | toUpper }}: {{ .CommonLabels.alertname }}'
        text: >-
          {{ range .Alerts }}
          *Alert:* {{ .Annotations.summary }}
          *Description:* {{ .Annotations.description }}
          *Severity:* {{ .Labels.severity }}
          {{ end }}

  - name: 'pagerduty-critical'
    pagerduty_configs:
      - service_key: '<pagerduty-service-key>'
        send_resolved: true

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'job']
```

### 5.5 Grafana 대시보드

#### 5.5.1 서비스 개요 대시보드

```json
{
  "dashboard": {
    "title": "Portal Universe - Service Overview",
    "panels": [
      {
        "title": "Service Health",
        "type": "stat",
        "targets": [
          {
            "expr": "up",
            "legendFormat": "{{ job }}"
          }
        ],
        "options": {
          "colorMode": "background",
          "graphMode": "none"
        }
      },
      {
        "title": "Request Rate",
        "type": "timeseries",
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count[5m])) by (job)",
            "legendFormat": "{{ job }}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "timeseries",
        "targets": [
          {
            "expr": "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])) by (job) / sum(rate(http_server_requests_seconds_count[5m])) by (job)",
            "legendFormat": "{{ job }}"
          }
        ]
      },
      {
        "title": "p95 Latency",
        "type": "timeseries",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job))",
            "legendFormat": "{{ job }}"
          }
        ]
      },
      {
        "title": "JVM Heap Usage",
        "type": "gauge",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{area=\"heap\"} / jvm_memory_max_bytes{area=\"heap\"}",
            "legendFormat": "{{ job }}"
          }
        ]
      }
    ]
  }
}
```

### 5.6 Loki 로그 집계

```yaml
# monitoring/loki/loki-config.yml
auth_enabled: false

server:
  http_listen_port: 3100

ingester:
  lifecycler:
    ring:
      kvstore:
        store: inmemory
      replication_factor: 1
  chunk_idle_period: 5m
  chunk_retain_period: 30s

schema_config:
  configs:
    - from: 2024-01-01
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h

storage_config:
  boltdb_shipper:
    active_index_directory: /loki/index
    cache_location: /loki/cache
    shared_store: filesystem
  filesystem:
    directory: /loki/chunks

limits_config:
  enforce_metric_name: false
  reject_old_samples: true
  reject_old_samples_max_age: 168h

chunk_store_config:
  max_look_back_period: 0s

table_manager:
  retention_deletes_enabled: false
  retention_period: 0s
```

```yaml
# monitoring/promtail/promtail-config.yml
server:
  http_listen_port: 9080

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: containers
    static_configs:
      - targets:
          - localhost
        labels:
          job: containerlogs
          __path__: /var/lib/docker/containers/*/*log

    pipeline_stages:
      - json:
          expressions:
            log: log
            stream: stream
            time: time
      - labels:
          stream:
      - timestamp:
          source: time
          format: RFC3339Nano
      - output:
          source: log
```

---

## 6. Infrastructure as Code (선택)

### 6.1 Helm Chart 구조

```
helm/
├── portal-universe/
│   ├── Chart.yaml
│   ├── values.yaml
│   ├── values-staging.yaml
│   ├── values-production.yaml
│   └── templates/
│       ├── _helpers.tpl
│       ├── configmap.yaml
│       ├── secret.yaml
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── ingress.yaml
│       ├── hpa.yaml
│       └── pdb.yaml
```

### 6.2 values.yaml

```yaml
# helm/portal-universe/values.yaml
global:
  imageRegistry: docker.io
  imagePullSecrets: []

services:
  apiGateway:
    enabled: true
    replicaCount: 2
    image:
      repository: lazehub/api-gateway
      tag: latest
    resources:
      requests:
        cpu: 200m
        memory: 512Mi
      limits:
        cpu: 1000m
        memory: 1Gi
    autoscaling:
      enabled: true
      minReplicas: 2
      maxReplicas: 10
      targetCPUUtilizationPercentage: 70

  authService:
    enabled: true
    replicaCount: 2
    image:
      repository: lazehub/auth-service
      tag: latest
    resources:
      requests:
        cpu: 200m
        memory: 512Mi
      limits:
        cpu: 1000m
        memory: 1Gi

  shoppingService:
    enabled: true
    replicaCount: 2
    image:
      repository: lazehub/shopping-service
      tag: latest
    resources:
      requests:
        cpu: 300m
        memory: 768Mi
      limits:
        cpu: 1500m
        memory: 1.5Gi

ingress:
  enabled: true
  className: nginx
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
  hosts:
    - host: api.portal-universe.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: portal-universe-tls
      hosts:
        - api.portal-universe.com

mysql:
  enabled: true
  auth:
    rootPassword: ""  # Secret에서 참조
    database: portal_db

redis:
  enabled: true
  auth:
    enabled: true
    password: ""  # Secret에서 참조

kafka:
  enabled: true
  replicaCount: 3
```

---

## 7. 에러 코드 (Phase 4)

```java
// 배포/운영 관련 에러 (내부 로깅용)
DEPLOYMENT_FAILED("D001", "배포에 실패했습니다"),
HEALTH_CHECK_FAILED("D002", "헬스 체크에 실패했습니다"),
ROLLBACK_TRIGGERED("D003", "롤백이 시작되었습니다"),
CONFIG_LOAD_FAILED("D004", "설정 로드에 실패했습니다"),
```

---

## 8. 체크리스트

### 8.1 CI/CD 체크리스트
- [ ] GitHub Actions workflow 파일 생성
- [ ] Docker Hub 시크릿 설정
- [ ] Kubernetes 시크릿 설정
- [ ] Staging/Production 환경 분리
- [ ] Blue-Green 배포 스크립트
- [ ] 롤백 절차 문서화

### 8.2 부하 테스트 체크리스트
- [ ] k6 스크립트 작성
- [ ] 테스트 데이터 준비
- [ ] 성능 기준 정의
- [ ] 결과 분석 자동화
- [ ] CI 파이프라인 통합

### 8.3 모니터링 체크리스트
- [ ] Prometheus 설정
- [ ] Grafana 대시보드
- [ ] Alert rules 정의
- [ ] Alertmanager 설정
- [ ] Loki 로그 집계
- [ ] Slack/Discord 연동

---

## 9. 구현 순서

| 순서 | 작업 | 예상 기간 |
|------|------|----------|
| 1 | CI workflow (build, test) | 1일 |
| 2 | Docker 빌드 최적화 | 0.5일 |
| 3 | Docker 푸시 workflow | 0.5일 |
| 4 | K8s manifests 정리 | 1일 |
| 5 | 배포 workflow (staging) | 1일 |
| 6 | k6 부하 테스트 스크립트 | 1.5일 |
| 7 | 부하 테스트 CI 통합 | 0.5일 |
| 8 | Prometheus 설정 | 0.5일 |
| 9 | Alert rules 정의 | 1일 |
| 10 | Alertmanager 설정 | 0.5일 |
| 11 | Grafana 대시보드 | 1.5일 |
| 12 | Loki 로그 집계 | 1일 |
| 13 | Production 배포 workflow | 1일 |
| 14 | 문서화 및 테스트 | 1일 |
| **총계** | | **~12일** |

---

## 10. 참고 자료

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [k6 Documentation](https://k6.io/docs/)
- [Prometheus Best Practices](https://prometheus.io/docs/practices/)
- [Grafana Dashboards](https://grafana.com/grafana/dashboards/)
- [Helm Charts](https://helm.sh/docs/)
- [ArgoCD](https://argo-cd.readthedocs.io/)
