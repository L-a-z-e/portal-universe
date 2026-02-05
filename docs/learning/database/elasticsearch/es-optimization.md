# Elasticsearch Optimization

Elasticsearch 성능 최적화를 위한 인덱스 설정, 쿼리 튜닝, 샤딩 전략을 학습합니다.

---

## 목차

1. [Index 설정 최적화](#1-index-설정-최적화)
2. [Mapping 최적화](#2-mapping-최적화)
3. [Query 최적화](#3-query-최적화)
4. [Shard 전략](#4-shard-전략)
5. [Cluster 최적화](#5-cluster-최적화)
6. [모니터링 및 진단](#6-모니터링-및-진단)

---

## 1. Index 설정 최적화

### Refresh Interval

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Refresh Interval 이해                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Document 색인 요청                                                   │
│        │                                                             │
│        ▼                                                             │
│  ┌─────────────┐                                                    │
│  │ In-memory   │  색인 버퍼에 저장                                   │
│  │   Buffer    │                                                    │
│  └──────┬──────┘                                                    │
│         │                                                            │
│         │  refresh (기본 1초)                                        │
│         ▼                                                            │
│  ┌─────────────┐                                                    │
│  │  Segment    │  검색 가능한 Segment 생성                           │
│  │ (Searchable)│                                                    │
│  └──────┬──────┘                                                    │
│         │                                                            │
│         │  flush (주기적)                                            │
│         ▼                                                            │
│  ┌─────────────┐                                                    │
│  │    Disk     │  영구 저장                                          │
│  └─────────────┘                                                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

```json
// 인덱스 생성 시 설정
PUT /products
{
  "settings": {
    "refresh_interval": "30s",
    "number_of_shards": 3,
    "number_of_replicas": 1
  }
}

// 대량 색인 시 임시 비활성화
PUT /products/_settings
{
  "refresh_interval": "-1"
}

// 색인 완료 후 복원
PUT /products/_settings
{
  "refresh_interval": "1s"
}

// 즉시 refresh 강제 실행
POST /products/_refresh
```

### 권장 Refresh Interval

| 상황 | 권장값 | 설명 |
|------|--------|------|
| 실시간 검색 필요 | 1s (기본값) | 검색 가능까지 최대 1초 지연 |
| 약간의 지연 허용 | 5s ~ 30s | 색인 성능 향상 |
| 대량 색인 중 | -1 | 완전 비활성화 |
| 로그/시계열 데이터 | 30s ~ 60s | 대량 데이터 처리에 적합 |

### Translog 설정

```json
PUT /products
{
  "settings": {
    "index.translog.durability": "async",
    "index.translog.sync_interval": "5s",
    "index.translog.flush_threshold_size": "512mb"
  }
}
```

| 설정 | 값 | 설명 |
|------|-----|------|
| durability | request | 매 요청마다 fsync (안전, 느림) |
| durability | async | 비동기 fsync (빠름, 데이터 손실 가능) |
| sync_interval | 5s | async 모드에서 동기화 간격 |
| flush_threshold_size | 512mb | Translog 크기 기반 flush |

### Index Lifecycle Management (ILM)

```json
// ILM 정책 생성
PUT _ilm/policy/logs_policy
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_age": "1d",
            "max_size": "50gb",
            "max_docs": 100000000
          },
          "set_priority": {
            "priority": 100
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "shrink": {
            "number_of_shards": 1
          },
          "forcemerge": {
            "max_num_segments": 1
          },
          "set_priority": {
            "priority": 50
          }
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "freeze": {},
          "set_priority": {
            "priority": 0
          }
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}

// Index Template에 ILM 적용
PUT _index_template/logs_template
{
  "index_patterns": ["logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "index.lifecycle.name": "logs_policy",
      "index.lifecycle.rollover_alias": "logs"
    }
  }
}
```

---

## 2. Mapping 최적화

### 불필요한 필드 비활성화

```json
PUT /products
{
  "mappings": {
    "properties": {
      "internal_id": {
        "type": "keyword",
        "index": false,
        "doc_values": false
      },
      "raw_data": {
        "type": "object",
        "enabled": false
      },
      "description": {
        "type": "text",
        "norms": false
      },
      "log_message": {
        "type": "text",
        "index_options": "docs"
      }
    }
  }
}
```

### 필드 옵션 설명

| 옵션 | 기본값 | 비활성화 효과 |
|------|--------|--------------|
| index | true | 검색 불가, 저장 공간 절약 |
| doc_values | true | 정렬/집계 불가, 메모리 절약 |
| norms | true (text) | 점수 계산 시 길이 정규화 제외 |
| enabled | true | 필드 완전 무시 |
| store | false | _source 없이 필드 직접 저장 |
| index_options | positions | docs: term 존재 여부만 |

### Dynamic Templates

```json
PUT /logs
{
  "mappings": {
    "dynamic_templates": [
      {
        "strings_as_keywords": {
          "match_mapping_type": "string",
          "match": "*_id",
          "mapping": {
            "type": "keyword",
            "ignore_above": 256
          }
        }
      },
      {
        "strings_as_text": {
          "match_mapping_type": "string",
          "mapping": {
            "type": "text",
            "fields": {
              "keyword": {
                "type": "keyword",
                "ignore_above": 256
              }
            }
          }
        }
      },
      {
        "longs_as_integers": {
          "match_mapping_type": "long",
          "mapping": {
            "type": "integer"
          }
        }
      }
    ]
  }
}
```

### Eager Global Ordinals

자주 사용하는 keyword 필드의 집계 성능 향상.

```json
PUT /products
{
  "mappings": {
    "properties": {
      "category": {
        "type": "keyword",
        "eager_global_ordinals": true
      },
      "brand": {
        "type": "keyword",
        "eager_global_ordinals": true
      }
    }
  }
}
```

---

## 3. Query 최적화

### Filter Context 활용

```
┌─────────────────────────────────────────────────────────────────────┐
│              Query Context vs Filter Context                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Query Context                 Filter Context                        │
│  ├── _score 계산 O            ├── _score 계산 X                     │
│  ├── 캐싱 X                   ├── 캐싱 O (빠른 재사용)              │
│  └── CPU 사용 높음            └── CPU 사용 낮음                     │
│                                                                      │
│  사용 예:                                                            │
│  ├── 전문 검색 (match)        ├── 필터링 (term, range)              │
│  └── 관련성 순위 필요          └── 예/아니오 판단만 필요              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

```json
// 비효율적인 쿼리
GET /products/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "스마트폰" } },
        { "term": { "category": "electronics" } },
        { "range": { "price": { "lte": 1000000 } } },
        { "term": { "is_active": true } }
      ]
    }
  }
}

// 최적화된 쿼리
GET /products/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "스마트폰" } }
      ],
      "filter": [
        { "term": { "category": "electronics" } },
        { "range": { "price": { "lte": 1000000 } } },
        { "term": { "is_active": true } }
      ]
    }
  }
}
```

### _source 필터링

```json
// 필요한 필드만 반환
GET /products/_search
{
  "query": { "match_all": {} },
  "_source": ["name", "price", "category"],
  "size": 100
}

// 특정 필드 제외
GET /products/_search
{
  "query": { "match_all": {} },
  "_source": {
    "excludes": ["description", "specifications"]
  }
}

// _source 완전 비활성화 (doc_count만 필요할 때)
GET /products/_search
{
  "query": { "match_all": {} },
  "_source": false,
  "size": 0
}
```

### Pagination 최적화

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Pagination 방식 비교                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  from/size (얕은 페이지)                                             │
│  ├── 장점: 간단, 임의 페이지 접근 가능                               │
│  ├── 단점: from + size <= 10000 제한                                │
│  └── 사용: 일반적인 검색 결과 (처음 몇 페이지)                       │
│                                                                      │
│  search_after (깊은 페이지)                                          │
│  ├── 장점: 무제한 스크롤, 실시간 데이터                              │
│  ├── 단점: 이전 페이지로 돌아갈 수 없음                              │
│  └── 사용: 무한 스크롤, 대량 데이터 순회                             │
│                                                                      │
│  scroll (대량 내보내기)                                              │
│  ├── 장점: 대량 데이터 처리에 최적화                                 │
│  ├── 단점: 스냅샷 기반 (실시간 변경 반영 X)                          │
│  └── 사용: 데이터 내보내기, 마이그레이션                             │
│                                                                      │
│  Point in Time (PIT) + search_after                                  │
│  ├── 장점: 일관된 뷰 + 효율적 페이지네이션                           │
│  └── 사용: 대량 데이터의 일관된 순회                                  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

```json
// search_after 예제
// 첫 번째 요청
GET /products/_search
{
  "query": { "match": { "category": "electronics" } },
  "sort": [
    { "created_at": "desc" },
    { "_id": "asc" }
  ],
  "size": 20
}

// 다음 페이지 요청 (이전 결과의 마지막 sort 값 사용)
GET /products/_search
{
  "query": { "match": { "category": "electronics" } },
  "sort": [
    { "created_at": "desc" },
    { "_id": "asc" }
  ],
  "size": 20,
  "search_after": ["2024-01-15T10:30:00.000Z", "prod_12345"]
}
```

```json
// Point in Time (PIT) 사용
// PIT 생성
POST /products/_pit?keep_alive=5m

// PIT로 검색
GET /_search
{
  "pit": {
    "id": "46ToAwMDaWR5BXV1aW...",
    "keep_alive": "5m"
  },
  "query": { "match_all": {} },
  "sort": [{ "created_at": "desc" }],
  "size": 100
}

// PIT 삭제
DELETE /_pit
{
  "id": "46ToAwMDaWR5BXV1aW..."
}
```

### Profile API로 쿼리 분석

```json
GET /products/_search
{
  "profile": true,
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "스마트폰" } }
      ],
      "filter": [
        { "term": { "category": "electronics" } }
      ]
    }
  }
}
```

### 쿼리 캐시 활용

```json
// 인덱스 설정에서 캐시 크기 조정
PUT /products/_settings
{
  "index.queries.cache.enabled": true
}

// 쿼리에서 캐시 명시적 요청
GET /products/_search?request_cache=true
{
  "query": { ... },
  "size": 0,
  "aggs": { ... }
}
```

---

## 4. Shard 전략

### Shard 크기 가이드라인

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Shard 크기 결정 가이드                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  권장 Shard 크기: 10GB ~ 50GB                                       │
│                                                                      │
│  예시 계산:                                                          │
│  ├── 예상 데이터: 200GB                                             │
│  ├── 목표 Shard 크기: 25GB                                          │
│  ├── 필요 Primary Shard: 200GB / 25GB = 8개                        │
│  └── Replica 포함 총 Shard: 8 × (1 + 1) = 16개                     │
│                                                                      │
│  주의사항:                                                           │
│  ├── Shard당 오버헤드 존재 (최소 ~1MB 메모리)                        │
│  ├── 노드당 1000개 미만 Shard 권장                                  │
│  ├── 너무 작은 Shard: 오버헤드 증가                                  │
│  └── 너무 큰 Shard: 복구 시간 증가                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 시계열 데이터 인덱스 전략

```json
// Index Template 생성
PUT _index_template/logs_template
{
  "index_patterns": ["logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "index.lifecycle.name": "logs_policy",
      "index.lifecycle.rollover_alias": "logs-write"
    },
    "aliases": {
      "logs-read": {}
    }
  }
}

// 초기 인덱스 생성
PUT /logs-000001
{
  "aliases": {
    "logs-write": {
      "is_write_index": true
    }
  }
}

// 색인은 write alias로
POST /logs-write/_doc
{
  "message": "log message",
  "@timestamp": "2024-01-15T10:30:00Z"
}

// 검색은 read alias로
GET /logs-read/_search
{
  "query": { "match_all": {} }
}
```

### Routing 활용

```json
// 사용자별 데이터 라우팅
PUT /orders/_doc/order_123?routing=user_456
{
  "order_id": "order_123",
  "user_id": "user_456",
  "items": [...]
}

// 같은 라우팅 값으로 검색 (단일 Shard만 검색)
GET /orders/_search?routing=user_456
{
  "query": {
    "term": { "user_id": "user_456" }
  }
}
```

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Routing 효과                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Routing 없이 검색:                                                  │
│  ┌─────┐   ┌─────┐   ┌─────┐                                       │
│  │Shard│   │Shard│   │Shard│   모든 Shard 검색                      │
│  │  0  │ + │  1  │ + │  2  │                                       │
│  └─────┘   └─────┘   └─────┘                                       │
│                                                                      │
│  Routing으로 검색:                                                   │
│  ┌─────┐                                                            │
│  │Shard│   단일 Shard만 검색 (성능 향상)                            │
│  │  1  │                                                            │
│  └─────┘                                                            │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Shrink와 Split

```json
// Shrink: Shard 수 줄이기 (읽기 전용 인덱스에 유용)
// 1. 인덱스를 단일 노드로 이동
PUT /logs-2024-01/_settings
{
  "settings": {
    "index.routing.allocation.require._name": "node-1",
    "index.blocks.write": true
  }
}

// 2. Shrink 실행
POST /logs-2024-01/_shrink/logs-2024-01-shrunk
{
  "settings": {
    "index.number_of_shards": 1,
    "index.number_of_replicas": 1,
    "index.codec": "best_compression"
  }
}

// Split: Shard 수 늘리기
POST /products/_split/products_split
{
  "settings": {
    "index.number_of_shards": 6
  }
}
```

---

## 5. Cluster 최적화

### JVM Heap 설정

```yaml
# jvm.options
-Xms16g
-Xmx16g

# 권장사항:
# - Xms = Xmx (같은 값)
# - 물리 메모리의 50% 이하
# - 32GB 미만 (Compressed OOPs)
```

### Thread Pool 설정

```yaml
# elasticsearch.yml
thread_pool:
  search:
    size: 25
    queue_size: 1000
  write:
    size: 12
    queue_size: 10000
```

### Circuit Breakers

```yaml
# elasticsearch.yml
indices.breaker.total.limit: 70%
indices.breaker.fielddata.limit: 40%
indices.breaker.request.limit: 40%
```

### 노드 역할 분리

```yaml
# Master Node (클러스터 관리 전용)
node.roles: [ master ]

# Data Node (데이터 저장 전용)
node.roles: [ data ]

# Coordinating Node (요청 라우팅 전용)
node.roles: [ ]

# Ingest Node (전처리 전용)
node.roles: [ ingest ]
```

```
┌─────────────────────────────────────────────────────────────────────┐
│                    노드 역할 분리 아키텍처                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Client Request                                                     │
│         │                                                            │
│         ▼                                                            │
│   ┌─────────────────────────────────┐                               │
│   │   Coordinating Nodes (2+)       │  요청 분배, 결과 취합          │
│   └────────────────┬────────────────┘                               │
│                    │                                                 │
│         ┌──────────┴──────────┐                                     │
│         ▼                     ▼                                     │
│   ┌───────────┐         ┌───────────┐                               │
│   │ Data Node │  ...    │ Data Node │   실제 데이터 처리             │
│   │    (n)    │         │    (n)    │                               │
│   └───────────┘         └───────────┘                               │
│                                                                      │
│   ┌─────────────────────────────────┐                               │
│   │   Master Nodes (3, odd number)  │  클러스터 상태 관리            │
│   └─────────────────────────────────┘                               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 6. 모니터링 및 진단

### Cluster Health

```bash
# 클러스터 상태 확인
GET /_cluster/health

# 인덱스별 상태
GET /_cluster/health?level=indices

# Shard별 상태
GET /_cluster/health?level=shards
```

### 응답 상태 의미

| Status | 설명 | 조치 |
|--------|------|------|
| green | 모든 Shard 정상 | 정상 |
| yellow | Primary OK, Replica 일부 미할당 | Replica 확인 |
| red | Primary 일부 미할당 | 즉시 조치 필요 |

### Cluster Stats

```bash
# 클러스터 전체 통계
GET /_cluster/stats

# 노드별 통계
GET /_nodes/stats

# 특정 노드
GET /_nodes/node_id/stats

# 인덱스 통계
GET /products/_stats
```

### 느린 쿼리 로그

```json
PUT /products/_settings
{
  "index.search.slowlog.threshold.query.warn": "10s",
  "index.search.slowlog.threshold.query.info": "5s",
  "index.search.slowlog.threshold.query.debug": "2s",
  "index.search.slowlog.threshold.query.trace": "500ms",

  "index.search.slowlog.threshold.fetch.warn": "1s",
  "index.search.slowlog.threshold.fetch.info": "800ms",
  "index.search.slowlog.threshold.fetch.debug": "500ms",
  "index.search.slowlog.threshold.fetch.trace": "200ms",

  "index.indexing.slowlog.threshold.index.warn": "10s",
  "index.indexing.slowlog.threshold.index.info": "5s",
  "index.indexing.slowlog.threshold.index.debug": "2s",
  "index.indexing.slowlog.threshold.index.trace": "500ms"
}
```

### Hot Threads 분석

```bash
# CPU를 많이 사용하는 스레드 확인
GET /_nodes/hot_threads

# 특정 노드
GET /_nodes/node_id/hot_threads
```

### Task Management

```bash
# 실행 중인 작업 확인
GET /_tasks

# 장시간 실행 작업
GET /_tasks?detailed=true&actions=*search*

# 특정 작업 취소
POST /_tasks/task_id/_cancel
```

### Kibana Monitoring 설정

```yaml
# elasticsearch.yml
xpack.monitoring.collection.enabled: true
xpack.monitoring.elasticsearch.collection.enabled: true
```

### 알람 설정 (Watcher)

```json
PUT _watcher/watch/cluster_health_watch
{
  "trigger": {
    "schedule": {
      "interval": "1m"
    }
  },
  "input": {
    "http": {
      "request": {
        "host": "localhost",
        "port": 9200,
        "path": "/_cluster/health"
      }
    }
  },
  "condition": {
    "compare": {
      "ctx.payload.status": {
        "eq": "red"
      }
    }
  },
  "actions": {
    "notify_slack": {
      "webhook": {
        "scheme": "https",
        "host": "hooks.slack.com",
        "port": 443,
        "method": "post",
        "path": "/services/xxx/yyy/zzz",
        "body": "{\"text\": \"Elasticsearch cluster is RED!\"}"
      }
    }
  }
}
```

---

## 최적화 체크리스트

```
┌─────────────────────────────────────────────────────────────────────┐
│                 Elasticsearch Optimization Checklist                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Index 설정                                                          │
│  ☐ 적절한 refresh_interval 설정 (대량 색인 시 비활성화)              │
│  ☐ 시계열 데이터에 ILM 정책 적용                                     │
│  ☐ 불필요한 필드 비활성화 (index: false, doc_values: false)          │
│                                                                      │
│  Mapping                                                             │
│  ☐ keyword vs text 적절히 선택                                       │
│  ☐ 자주 집계하는 필드에 eager_global_ordinals                        │
│  ☐ Dynamic Template으로 일관된 매핑                                  │
│                                                                      │
│  Query                                                               │
│  ☐ 필터링에 filter context 사용                                      │
│  ☐ 필요한 _source 필드만 반환                                        │
│  ☐ 깊은 페이지네이션에 search_after 사용                             │
│  ☐ Profile API로 느린 쿼리 분석                                      │
│                                                                      │
│  Shard                                                               │
│  ☐ Shard 크기 10-50GB 유지                                          │
│  ☐ 노드당 Shard 수 1000개 미만                                       │
│  ☐ 적절한 Routing 전략 (사용자별, 테넌트별)                          │
│                                                                      │
│  Cluster                                                             │
│  ☐ JVM Heap: 물리 메모리 50%, 최대 32GB 미만                        │
│  ☐ 노드 역할 분리 (Master, Data, Coordinating)                       │
│  ☐ Master Node 홀수 개 (최소 3개)                                    │
│                                                                      │
│  모니터링                                                             │
│  ☐ Cluster Health 모니터링                                           │
│  ☐ Slow Log 설정                                                     │
│  ☐ 알람 설정 (Red 상태)                                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 다음 단계

- [Core Concepts](./es-core-concepts.md) - 핵심 개념 복습
- [Query DSL](./es-query-dsl.md) - 쿼리 최적화 실습
- [Spring Integration](./es-spring-integration.md) - 애플리케이션 연동
