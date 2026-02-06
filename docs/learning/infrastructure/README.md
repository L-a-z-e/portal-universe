# 고가용성(High Availability) 학습 로드맵

Portal Universe 프로젝트를 활용한 실전 고가용성 학습 가이드입니다.

## 개요

| 항목 | 내용 |
|------|------|
| **예상 학습 시간** | 38-50시간 (약 2-3주) |
| **대상** | 중급 이상 개발자, DevOps 엔지니어 |
| **필수 환경** | Kubernetes, Docker, kubectl |
| **난이도** | ★★★☆☆ ~ ★★★★☆ |

## 학습 목표

이 로드맵을 완료하면 다음을 할 수 있습니다:

1. **가용성 지표 설계** - SLA, SLO, SLI를 정의하고 모니터링할 수 있습니다
2. **장애 시뮬레이션** - Chaos Engineering 원칙에 따라 장애를 안전하게 재현할 수 있습니다
3. **장애 대응** - 인프라 장애 발생 시 신속하게 원인을 파악하고 복구할 수 있습니다
4. **고가용성 아키텍처 설계** - 단일 장애점(SPOF)을 제거하고 이중화를 구성할 수 있습니다
5. **운영 체계 수립** - Runbook, 알림, 인시던트 대응 프로세스를 수립할 수 있습니다

---

## Phase 별 학습 내용

### Phase 1: 고가용성 기초 개념 (4-6시간)

이론적 기초를 다집니다.

| 문서 | 내용 | 시간 |
|------|------|------|
| [01-ha-fundamentals.md](./phase-1-concepts/01-ha-fundamentals.md) | SLA, SLO, SLI, 9s 개념 | 1시간 |
| [02-failure-modes.md](./phase-1-concepts/02-failure-modes.md) | 장애 유형 분류 | 45분 |
| [03-metrics-mttr-mtbf.md](./phase-1-concepts/03-metrics-mttr-mtbf.md) | MTTR, MTBF, 가용성 계산 | 1시간 |
| [04-redundancy-patterns.md](./phase-1-concepts/04-redundancy-patterns.md) | 이중화 패턴 (Active-Active, Active-Passive) | 1시간 |
| [05-current-state-analysis.md](./phase-1-concepts/05-current-state-analysis.md) | Portal Universe 현재 상태 분석 | 45분 |

### Phase 2: Chaos Engineering 기초 (6-8시간)

장애 주입 기법을 학습합니다.

| 문서 | 내용 | 시간 |
|------|------|------|
| [01-chaos-engineering-intro.md](./phase-2-chaos-engineering/01-chaos-engineering-intro.md) | Chaos Engineering 원칙 | 1시간 |
| [02-observability-setup.md](./phase-2-chaos-engineering/02-observability-setup.md) | 모니터링 대시보드 설정 | 2시간 |
| [03-basic-fault-injection.md](./phase-2-chaos-engineering/03-basic-fault-injection.md) | 기본 장애 주입 (kubectl/docker) | 2시간 |
| [04-steady-state-hypothesis.md](./phase-2-chaos-engineering/04-steady-state-hypothesis.md) | 정상 상태 가설 수립 | 1시간 |
| [05-game-day-template.md](./phase-2-chaos-engineering/05-game-day-template.md) | Game Day 실행 템플릿 | 1시간 |

### Phase 3: 인프라 장애 시나리오 (12-16시간)

실제 장애를 재현하고 대응합니다.

#### Kafka 장애 (3-4시간)
| 문서 | 내용 |
|------|------|
| [kafka/01-kafka-broker-down.md](./phase-3-infrastructure-failures/kafka/01-kafka-broker-down.md) | 브로커 다운 시나리오 |
| [kafka/02-kafka-memory-oom.md](./phase-3-infrastructure-failures/kafka/02-kafka-memory-oom.md) | 메모리 부족 (OOM) |
| [kafka/03-kafka-partition-loss.md](./phase-3-infrastructure-failures/kafka/03-kafka-partition-loss.md) | 파티션 손실 |
| [kafka/04-kafka-consumer-lag.md](./phase-3-infrastructure-failures/kafka/04-kafka-consumer-lag.md) | Consumer Lag 급증 |

#### Redis 장애 (3-4시간)
| 문서 | 내용 |
|------|------|
| [redis/01-redis-oom.md](./phase-3-infrastructure-failures/redis/01-redis-oom.md) | 메모리 OOM |
| [redis/02-redis-connection-pool.md](./phase-3-infrastructure-failures/redis/02-redis-connection-pool.md) | 연결 풀 고갈 |
| [redis/03-redis-latency-spike.md](./phase-3-infrastructure-failures/redis/03-redis-latency-spike.md) | 지연 급증 |
| [redis/04-redis-eviction.md](./phase-3-infrastructure-failures/redis/04-redis-eviction.md) | 키 Eviction |

#### MySQL 장애 (3-4시간)
| 문서 | 내용 |
|------|------|
| [mysql/01-mysql-connection-pool.md](./phase-3-infrastructure-failures/mysql/01-mysql-connection-pool.md) | 커넥션 풀 고갈 |
| [mysql/02-mysql-slow-query.md](./phase-3-infrastructure-failures/mysql/02-mysql-slow-query.md) | 슬로우 쿼리 |
| [mysql/03-mysql-lock-contention.md](./phase-3-infrastructure-failures/mysql/03-mysql-lock-contention.md) | 락 경합 |
| [mysql/04-mysql-disk-full.md](./phase-3-infrastructure-failures/mysql/04-mysql-disk-full.md) | 디스크 풀 |

#### Elasticsearch 장애 (2-3시간)
| 문서 | 내용 |
|------|------|
| [elasticsearch/01-es-memory-oom.md](./phase-3-infrastructure-failures/elasticsearch/01-es-memory-oom.md) | 힙 메모리 부족 |
| [elasticsearch/02-es-index-corruption.md](./phase-3-infrastructure-failures/elasticsearch/02-es-index-corruption.md) | 인덱스 손상 |
| [elasticsearch/03-es-query-timeout.md](./phase-3-infrastructure-failures/elasticsearch/03-es-query-timeout.md) | 쿼리 타임아웃 |

#### 서비스 장애 (2-3시간)
| 문서 | 내용 |
|------|------|
| [services/01-service-memory-leak.md](./phase-3-infrastructure-failures/services/01-service-memory-leak.md) | 메모리 누수 |
| [services/02-service-thread-exhaustion.md](./phase-3-infrastructure-failures/services/02-service-thread-exhaustion.md) | 스레드 고갈 |
| [services/03-service-cascade-failure.md](./phase-3-infrastructure-failures/services/03-service-cascade-failure.md) | 연쇄 장애 |
| [services/04-network-partition.md](./phase-3-infrastructure-failures/services/04-network-partition.md) | 네트워크 파티션 |

### Phase 4: 고가용성 아키텍처 개선 (10-12시간)

SPOF를 제거하고 이중화를 구성합니다.

| 문서 | 내용 | 시간 |
|------|------|------|
| [01-replicas-scaling.md](./phase-4-ha-architecture/01-replicas-scaling.md) | Replicas 확장 | 1시간 |
| [02-hpa-setup.md](./phase-4-ha-architecture/02-hpa-setup.md) | HPA (Horizontal Pod Autoscaler) 설정 | 1.5시간 |
| [03-pdb-setup.md](./phase-4-ha-architecture/03-pdb-setup.md) | PDB (Pod Disruption Budget) 설정 | 1시간 |
| [04-kafka-replication.md](./phase-4-ha-architecture/04-kafka-replication.md) | Kafka 3-노드 클러스터 | 2시간 |
| [05-redis-sentinel.md](./phase-4-ha-architecture/05-redis-sentinel.md) | Redis Sentinel 설정 | 2시간 |
| [06-mysql-replication.md](./phase-4-ha-architecture/06-mysql-replication.md) | MySQL Primary-Replica | 2시간 |
| [07-graceful-shutdown.md](./phase-4-ha-architecture/07-graceful-shutdown.md) | Graceful Shutdown 구현 | 1시간 |
| [08-multi-zone-deployment.md](./phase-4-ha-architecture/08-multi-zone-deployment.md) | Multi-AZ 배포 (개념) | 1시간 |

### Phase 5: 운영 가이드 (6-8시간)

운영 체계를 수립합니다.

| 문서 | 내용 | 시간 |
|------|------|------|
| [01-runbook-template.md](./phase-5-operations/01-runbook-template.md) | 장애 대응 Runbook 작성 | 1.5시간 |
| [02-alerting-setup.md](./phase-5-operations/02-alerting-setup.md) | AlertManager 알림 설정 | 1.5시간 |
| [03-incident-response.md](./phase-5-operations/03-incident-response.md) | 인시던트 대응 프로세스 | 1시간 |
| [04-post-mortem-template.md](./phase-5-operations/04-post-mortem-template.md) | Post-Mortem 작성 가이드 | 1시간 |
| [05-comprehensive-test.md](./phase-5-operations/05-comprehensive-test.md) | 종합 테스트 시나리오 | 2시간 |

---

## 부록

| 문서 | 내용 |
|------|------|
| [glossary.md](./appendix/glossary.md) | 용어 사전 |
| [commands-cheatsheet.md](./appendix/commands-cheatsheet.md) | 명령어 치트시트 |
| [monitoring-queries.md](./appendix/monitoring-queries.md) | PromQL 쿼리 모음 |

---

## Portal Universe 현재 상태

### 이미 구현된 것들

| 항목 | 상태 | 위치 |
|------|------|------|
| k6 부하 테스트 | ✅ 5개 시나리오 | `services/load-tests/k6/scenarios/` |
| Circuit Breaker | ✅ Resilience4j | `services/api-gateway/.../application.yml` |
| Rate Limiting | ✅ Redis 기반 5가지 | `services/api-gateway/.../RateLimiterConfig.java` |
| Monitoring | ✅ Prometheus + Grafana | `monitoring/` |
| Distributed Tracing | ✅ Zipkin | 모든 서비스 |
| Health Check | ✅ 3-tier probes | K8s 매니페스트 |

### 고가용성 개선이 필요한 부분

| 컴포넌트 | 현재 | 문제점 |
|----------|------|--------|
| Kafka | replicas: 1 | 단일 장애점 (SPOF) |
| Redis | replicas: 1 | 캐시/세션 손실 위험 |
| MySQL | replicas: 1 | 데이터 손실 위험 |
| 서비스들 | replicas: 1 | 무중단 배포 불가 |
| HPA | 미설정 | 자동 스케일링 없음 |
| PDB | 미설정 | 유지보수 시 가용성 저하 |

---

## 학습 순서 권장

```
Phase 1 (이론) → Phase 2 (도구 학습) → Phase 3 (장애 재현)
                                            ↓
                    Phase 5 (운영) ← Phase 4 (개선 적용)
```

1. **Phase 1**: 이론적 기초 없이 실습하면 "왜"를 이해하지 못합니다
2. **Phase 2**: 장애 주입 전 관찰 방법을 먼저 익혀야 합니다
3. **Phase 3**: 실제 장애를 재현하면서 증상을 파악합니다
4. **Phase 4**: 문제를 이해한 후 해결책을 적용합니다
5. **Phase 5**: 반복 가능한 운영 체계를 수립합니다

---

## 사전 요구사항

자세한 내용은 [00-prerequisites.md](./00-prerequisites.md)를 참조하세요.

### 필수 도구

- `kubectl` (v1.28+)
- `docker` (v24+)
- `k6` (부하 테스트)
- `helm` (일부 설정에 필요)

### 필수 지식

- Kubernetes 기본 개념 (Pod, Deployment, Service)
- Docker 컨테이너 기본
- Linux 명령어 (ps, top, netstat, ss)
- HTTP/REST API 기본

---

## 주의사항

> ⚠️ **장애 주입 실습은 반드시 개발/테스트 환경에서만 수행하세요.**
>
> Production 환경에서의 Chaos Engineering은 충분한 안전장치와 Rollback 계획이 필요합니다.

---

## 피드백

학습 중 발견한 문제나 개선 사항은 이슈로 등록해 주세요.
