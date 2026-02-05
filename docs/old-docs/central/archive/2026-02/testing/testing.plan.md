# Performance & Load Testing Strategy - Plan

> Feature: testing
> Phase: Plan
> Created: 2026-02-03
> Author: AI-assisted

---

## 1. Feature Overview

Portal Universe 마이크로서비스 아키텍처에 대한 성능/부하 테스트 체계를 구축한다. 트래픽 생성 도구, 테스트 시나리오, 모니터링 강화를 통해 병목 지점을 식별하고 장애 상황에서의 복원력을 검증한다.

### 배경

- 현재 통합 테스트(JUnit 기반 동시성 테스트)는 존재하지만 전문 부하 테스트 도구 미도입
- Prometheus + Grafana + Loki + Zipkin + Alertmanager 모니터링 스택은 구축 완료
- 미들웨어(MySQL, Redis, Kafka) 전용 Exporter 미설정 → 인프라 레벨 병목 식별 불가

### 대상 서비스

| 서비스 | 포트 | 스택 | 테스트 중점 |
|--------|------|------|-----------|
| api-gateway | 8080 | Spring Cloud Gateway | 라우팅 성능, Rate Limit |
| auth-service | 8081 | Spring Boot | JWT 발급/검증, 동시 로그인 |
| blog-service | 8082 | Spring Boot | Read-heavy, 캐시 효율 |
| shopping-service | 8083 | Spring Boot | Write-heavy, 동시성, 검색 |
| notification-service | 8084 | Spring Boot | Kafka Consumer 처리량 |
| prism-service | 8085 | NestJS | AI 실행 타임아웃, SSE |
| chatbot-service | 8086 | FastAPI | RAG 응답 시간 |

### 인프라 의존성

| 인프라 | 버전 | 역할 | 테스트 관점 |
|--------|------|------|-----------|
| MySQL 8.0 | 3307 | 관계형 데이터 | Connection Pool, Slow Query |
| MongoDB 8.0 | 27017 | 블로그 문서 | 읽기 성능, 인덱스 |
| PostgreSQL 18 | 5432 | Prism 데이터 | Connection 관리 |
| Redis 7.4 | 6379 | 캐시, 세션, 동시성 | Hit Rate, Eviction, Memory |
| Kafka 4.1 | 9092 | 이벤트 스트리밍 | Consumer Lag, Throughput |
| Elasticsearch 8.18 | 9200 | 상품 검색 | 검색 Latency, 클러스터 상태 |

---

## 2. Current Status

### 2.1 모니터링 스택 (구축 완료)

| 구성요소 | 버전 | 상태 |
|----------|------|------|
| Prometheus | v2.53.5 | 15초 스크래핑, 30일 보관 |
| Grafana | v11.4.0 | 6개 대시보드, Pyroscope 플러그인 |
| Loki + Promtail | v2.9.0 | 7일 보관 |
| Zipkin | v3.4.2 | in-memory (영구 저장 필요) |
| Alertmanager | v0.28.1 | 29개 규칙, Slack 연동 |
| Dozzle | v8.14.5 | 컨테이너 로그 뷰어 |

### 2.2 기존 Grafana 대시보드

1. `portal-universe-monitoring.json` — 전체 개요
2. `api-performance.json` — API 성능
3. `jvm-deep-dive.json` — JVM 상세 분석
4. `logs-traces.json` — 로그/추적 통합
5. `service-overview.json` — 서비스별 현황
6. `slo-sli.json` — SLO/SLI 모니터링

### 2.3 기존 알림 규칙 (29개)

**주요 규칙 검토**:

| 규칙 | 현재 임계값 | 업계 기준 | 판단 |
|------|-----------|----------|------|
| HighLatency | p95 > 500ms | p95 < 200ms | 느슨, 조정 권장 |
| VeryHighLatency | p99 > 1s | p99 < 500ms | 적절~느슨 |
| HighMemoryUsage | Heap > 85% | Heap > 85% | 적절 |
| RedisMemoryHigh | Memory > 85% | Memory > 85% | 적절 |
| KafkaConsumerLag | Lag > 1,000 | Lag > 1,000 | 적절 |
| MySQLConnectionsHigh | > 80% | > 80% | 적절 |

### 2.4 기존 테스트 (부하 테스트 아님)

- `integration-tests/` — JUnit 5 기반
  - 동시성 테스트: `CouponConcurrencyTest` (100명 → 50개 쿠폰)
  - E2E 플로우: Auth, Checkout, Coupon, TimeDeal
  - 사용 기술: RestAssured, Testcontainers, Awaitility

### 2.5 부족한 부분

| # | 영역 | 현재 상태 | 필요 사항 |
|---|------|----------|----------|
| G1 | 부하 테스트 도구 | 없음 | k6 + Custom Bot 도입 |
| G2 | 미들웨어 Exporter | 없음 | cAdvisor, mysqld, redis, kafka exporter |
| G3 | 부하 테스트 대시보드 | 없음 | Load Test Overview + Bottleneck Detection |
| G4 | Zipkin 영구 저장 | in-memory | Elasticsearch 백엔드 전환 |
| G5 | Chaos Engineering | 없음 | Chaos Mesh 또는 Toxiproxy |
| G6 | Continuous Profiling | 플러그인만 있음 | Pyroscope 서버 추가 |

---

## 3. Performance Testing Types

### 테스트 유형 6가지

| 유형 | 목적 | 부하 패턴 | 프로젝트 적용 예시 |
|------|------|----------|-----------------|
| **Load Test** | 예상 트래픽에서 정상 동작 확인 | 일정 부하 유지 | 1,000 VU × 30분 |
| **Stress Test** | 한계점 파악 | 점진적 증가 | 1,000→5,000→10,000 VU |
| **Soak Test** | 메모리 누수, 커넥션 풀 고갈 | 중간 부하 장시간 | 500 VU × 24시간 |
| **Spike Test** | 급증 트래픽 대응 | 급격한 변화 | 100→5,000→100 VU |
| **Breakpoint Test** | 정확한 파괴 지점 | 세밀한 증가 | 100명 단위 증가 |
| **Chaos Test** | 인프라 장애 복원력 | 정상 부하 + 장애 주입 | Redis 다운, Kafka 장애 |

---

## 4. SLA/SLO Targets

### API 응답시간

| 지표 | 프로젝트 목표 | 일반 기준 | 엄격 기준 (금융) |
|------|-------------|-----------|----------------|
| p50 | < 80ms | < 100ms | < 50ms |
| p95 | < 150ms | < 200ms | < 100ms |
| p99 | < 300ms | < 500ms | < 100ms |

### Throughput

| 서비스 유형 | 목표 RPS |
|------------|----------|
| Read-heavy (블로그 조회) | 5,000+ (캐시 적용) |
| Write-heavy (주문) | 500~1,000 |
| 검색 (ES) | 1,000~3,000 |
| 인증 (JWT) | 10,000+ |

### Error Rate & Availability

| 서비스 | Error Rate | Availability |
|--------|-----------|-------------|
| 블로그, 쇼핑 조회 | < 0.1% | 99.9% |
| 주문, 결제 | < 0.01% | 99.95% |
| 인증 | < 0.01% | 99.95% |
| 알림 (비동기) | < 0.5% | 99.5% |

### 인프라 지표

| 지표 | 정상 | 알림 | 위험 |
|------|------|------|------|
| JVM Heap | < 70% | > 85% | > 95% |
| GC Pause p99 | < 50ms | > 100ms | > 500ms |
| DB Connection Pool | < 70% | > 80% | > 95% |
| Redis Memory | < 70% | > 85% | > 95% |
| Redis Hit Rate | > 90% | < 70% | < 50% |
| Kafka Consumer Lag | < 1,000 | > 5,000 | > 50,000 |

---

## 5. Test Scenarios (8 Scenarios)

### A. Shopping E2E Flow (Load Test)

| 항목 | 값 |
|------|-----|
| 목적 | 쇼핑 핵심 플로우 종단간 성능 검증 |
| 플로우 | Login → Product List → Product Detail → Add to Cart → Order |
| VU / 시간 | 1,000 VU / 30분 (Ramp 5분 + Steady 20분 + Down 5분) |
| 성공 기준 | p95 < 200ms, Error Rate < 0.1%, Kafka 이벤트 100% 발행 |
| 측정 | API 응답시간, DB Connection, Redis Hit, Kafka Lag |

### B. Blog Read (Read-Heavy Load Test)

| 항목 | 값 |
|------|-----|
| 목적 | 캐시 효율성 검증, Cold vs Warm Cache 비교 |
| 플로우 | GET /api/v1/posts (목록) + GET /api/v1/posts/{id} (상세) |
| VU / 시간 | 1,000 VU / 30분 |
| 성공 기준 | Warm Cache에서 5,000 RPS, Redis Hit Rate > 95%, MySQL Pool < 30% |

### C. Coupon Spike (Spike Test)

| 항목 | 값 |
|------|-----|
| 목적 | 선착순 쿠폰 발급 동시성 검증 |
| 플로우 | POST /api/v1/coupons/{id}/issue |
| VU / 시간 | 0→5,000 VU / 30초 (Spike) |
| 성공 기준 | 정확히 N장 발급 (과발급 0건), Deadlock 0건, p99 < 500ms |

### D. Elasticsearch Search (Load Test)

| 항목 | 값 |
|------|-----|
| 목적 | 검색 엔진 성능 검증 |
| 플로우 | GET /api/v1/products/search?q= (단순 + 복합 필터) |
| VU / 시간 | 500 VU / 20분 |
| 성공 기준 | p95 < 100ms, ES Cluster Green, CPU < 70% |

### E. Thundering Herd (Stress Test)

| 항목 | 값 |
|------|-----|
| 목적 | 캐시 만료 시 동시 DB 접근 부하 검증 |
| 플로우 | 캐시 만료 직후 GET /api/v1/products/{popular_id} 동시 요청 |
| VU / 시간 | 1,000 VU / 1분 (만료 직후) |
| 성공 기준 | Singleflight 적용 시 DB 접근 1회, Connection Pool < 50% |

### F. Kafka Consumer Lag (Spike Test)

| 항목 | 값 |
|------|-----|
| 목적 | 이벤트 급증 시 Consumer 처리 능력 검증 |
| 플로우 | Custom Bot으로 10,000 msg/s 발행 |
| VU / 시간 | 5분 Spike + 10분 정상 |
| 성공 기준 | Lag < 10,000, 회복 < 5분, 메시지 손실 0건 |

### G. OOM Simulation (Stress Test)

| 항목 | 값 |
|------|-----|
| 목적 | 메모리 부족 시 K8s 복원력 검증 |
| 플로우 | 대용량 파일 업로드 동시 요청 |
| VU / 시간 | 50 VU / 10분 |
| 성공 기준 | Pod Restart 감지, 30초 내 Recovery, 다른 서비스 무영향 |

### H. DB Connection Pool Exhaustion (Stress Test)

| 항목 | 값 |
|------|-----|
| 목적 | Connection Pool 고갈 시 동작 검증 |
| 플로우 | Long Query 20개로 Pool 독점 + 100 VU 일반 요청 |
| VU / 시간 | 120 VU / 5분 |
| 성공 기준 | Timeout 에러 반환 (앱 다운 아님), Long Query 종료 후 자동 복구 |

---

## 6. Tool Selection

### Primary: k6 (HTTP API Load Testing)

**선정 이유**:
1. Grafana 네이티브 통합 (Prometheus Remote Write)
2. JavaScript 스크립트 — Vue/React 팀에 익숙
3. Go 기반 — 단일 머신에서 수만 VU 가능
4. CI/CD 통합 — GitHub Actions 지원
5. Threshold 기반 pass/fail 자동 판정

### Secondary: Custom Python Bot (Kafka/Redis)

**선정 이유**:
- k6로 불가능한 Kafka Producer 대량 발행
- Redis 특수 시나리오 (캐시 무효화 등)
- Prometheus client로 메트릭 노출 가능

### Chaos Engineering: Chaos Mesh (K8s) / Toxiproxy (Docker)

**선정 이유**:
- CNCF 프로젝트, CRD 기반 선언적 관리
- Docker 환경에서는 Toxiproxy로 네트워크 장애 시뮬레이션

---

## 7. Monitoring Enhancement

### 우선순위 1: 필수

| # | 항목 | 설명 |
|---|------|------|
| M1 | cAdvisor | 컨테이너 CPU, Memory, Network I/O |
| M2 | mysqld_exporter | MySQL Connection Pool, Slow Query, Buffer Pool |
| M3 | redis_exporter | Redis Hit Rate, Eviction, Commands/sec |
| M4 | kafka_exporter | Kafka Broker, Topic Throughput, Consumer Lag |
| M5 | Load Test Overview Dashboard | k6 메트릭 + 인프라 메트릭 통합 |
| M6 | Bottleneck Detection Dashboard | RED/USE Method, Latency Heatmap |
| M7 | Zipkin ES Backend | 분산 추적 영구 저장 |

### 우선순위 2: 권장

| # | 항목 | 설명 |
|---|------|------|
| M8 | Pyroscope Server | CPU/Memory Flame Graph (플러그인은 이미 있음) |
| M9 | JSON 구조화 로그 | Loki 검색 + Trace ID 연동 |
| M10 | 알림 규칙 조정 | p95 > 500ms → p95 > 200ms로 강화 |

---

## 8. Implementation Scope

### In-Scope (이번 PDCA 사이클)

| # | 항목 | 설명 |
|---|------|------|
| S1 | k6 설치 및 Grafana 연동 | brew install k6 + Prometheus Remote Write |
| S2 | Exporter 4종 추가 | cAdvisor, mysqld, redis, kafka exporter |
| S3 | Grafana 대시보드 2개 | Load Test Overview + Bottleneck Detection |
| S4 | k6 시나리오 스크립트 A~E | HTTP API 기본 부하 테스트 5개 |
| S5 | Custom Bot (Kafka) | 시나리오 F용 Python 스크립트 |
| S6 | Zipkin ES Backend | Elasticsearch 저장소 전환 |

### Out-of-Scope (향후 사이클)

| # | 항목 | 이유 |
|---|------|------|
| O1 | Chaos Mesh 설치 | K8s 환경에서만 의미, 별도 사이클 |
| O2 | Pyroscope 서버 | 부하 테스트 기본 체계 구축 후 |
| O3 | JSON 구조화 로그 | 전 서비스 로그 포맷 변경은 별도 작업 |
| O4 | CI/CD 통합 | 기본 스크립트 검증 후 자동화 |
| O5 | 시나리오 G, H | 기본 시나리오 완성 후 확장 |

---

## 9. Directory Structure

```
services/load-tests/
├── k6/
│   ├── scenarios/
│   │   ├── shopping-flow.js       # 시나리오 A
│   │   ├── blog-read.js           # 시나리오 B
│   │   ├── coupon-spike.js        # 시나리오 C
│   │   ├── search-load.js         # 시나리오 D
│   │   └── cache-thundering.js    # 시나리오 E
│   ├── lib/
│   │   ├── auth.js                # 공통 인증 헬퍼
│   │   └── config.js              # 환경별 설정 (local/docker/k8s)
│   └── thresholds.js              # 공통 성능 기준
├── bots/
│   └── kafka_producer.py          # 시나리오 F
└── README.md
```

---

## 10. Success Criteria

| # | 기준 | 목표 |
|---|------|------|
| C1 | k6 + Grafana 연동 | Prometheus Remote Write로 k6 메트릭이 Grafana에 표시 |
| C2 | Exporter 메트릭 수집 | 4종 Exporter가 Prometheus에 정상 스크래핑 |
| C3 | 대시보드 가시성 | 부하 테스트 중 병목을 5분 내 식별 가능 |
| C4 | 시나리오 A 실행 | 1,000 VU × 10분 이상 정상 실행 + pass/fail 판정 |
| C5 | Kafka Bot 실행 | 1,000 msg/s 이상 발행 + Lag 모니터링 가능 |
| C6 | Zipkin 영구 저장 | ES에 Trace 저장 → 재시작 후에도 조회 가능 |

---

## 11. Dependencies

| 의존성 | 설명 | 상태 |
|--------|------|------|
| Docker Compose | 전체 스택 실행 | 구축 완료 |
| Prometheus | 메트릭 서버 | 구축 완료 |
| Grafana | 대시보드 | 구축 완료, 6개 대시보드 |
| Elasticsearch | Zipkin 백엔드 + 검색 | 구축 완료 |
| Spring Boot Actuator | 서비스 메트릭 | `/actuator/prometheus` 노출 중 |
| k6 | 부하 테스트 도구 | 미설치 (brew install 필요) |

---

## 12. Risk Assessment

| 리스크 | 영향도 | 대응 방안 |
|--------|-------|----------|
| k6 Prometheus Remote Write 호환성 | MEDIUM | xk6-output-prometheus 빌드 또는 StatsD 경유 |
| Exporter 리소스 오버헤드 | LOW | 개발 환경에서는 미미, 프로덕션에서 스크래핑 주기 조정 |
| 부하 테스트 중 로컬 머신 자원 부족 | MEDIUM | VU 수 조정, Docker Desktop 메모리 할당 증가 |
| Zipkin ES 전환 시 설정 호환 | LOW | Zipkin 공식 ES 설정 문서 참고 |
| Kafka Bot → Prometheus 메트릭 노출 | LOW | prometheus_client Python 라이브러리 사용 |

---

## 13. Next Steps

1. `/pdca design testing` — 상세 설계 문서 작성
2. k6 스크립트 구조 설계
3. Exporter Docker Compose 설정 설계
4. Grafana 대시보드 패널 구성 설계
