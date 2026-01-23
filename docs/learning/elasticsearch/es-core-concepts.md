# Elasticsearch Core Concepts

Elasticsearch의 핵심 개념과 내부 구조를 깊이 있게 이해합니다.

---

## 목차

1. [Document와 Index](#1-document와-index)
2. [Mapping](#2-mapping)
3. [Shard와 Replica](#3-shard와-replica)
4. [Inverted Index](#4-inverted-index)
5. [Cluster Architecture](#5-cluster-architecture)
6. [Node Types](#6-node-types)

---

## 1. Document와 Index

### Document

Elasticsearch에서 데이터의 최소 단위입니다. JSON 형태로 저장됩니다.

```json
{
  "_index": "products",
  "_id": "1",
  "_source": {
    "name": "스마트폰",
    "price": 1200000,
    "category": "electronics",
    "description": "최신 5G 스마트폰",
    "created_at": "2024-01-15T10:30:00Z"
  }
}
```

### Index

Document의 논리적 그룹입니다. RDBMS의 Table과 유사합니다.

```
┌─────────────────────────────────────────────────────────┐
│                     Elasticsearch                        │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │   Index:    │  │   Index:    │  │   Index:    │     │
│  │  products   │  │    users    │  │   orders    │     │
│  ├─────────────┤  ├─────────────┤  ├─────────────┤     │
│  │ Document 1  │  │ Document 1  │  │ Document 1  │     │
│  │ Document 2  │  │ Document 2  │  │ Document 2  │     │
│  │ Document 3  │  │ Document 3  │  │ Document 3  │     │
│  │    ...      │  │    ...      │  │    ...      │     │
│  └─────────────┘  └─────────────┘  └─────────────┘     │
└─────────────────────────────────────────────────────────┘
```

### RDBMS vs Elasticsearch 용어 비교

| RDBMS | Elasticsearch | 설명 |
|-------|---------------|------|
| Database | Cluster | 데이터 저장소의 최상위 단위 |
| Table | Index | 데이터 구조의 논리적 그룹 |
| Row | Document | 하나의 데이터 레코드 |
| Column | Field | 데이터의 속성 |
| Schema | Mapping | 필드 타입 정의 |
| Primary Key | _id | 고유 식별자 |

### Index 생성 및 관리

```bash
# Index 생성
PUT /products
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1
  }
}

# Index 목록 확인
GET /_cat/indices?v

# Index 삭제
DELETE /products

# Index 설정 확인
GET /products/_settings
```

---

## 2. Mapping

### Mapping이란?

Document의 필드가 어떻게 저장되고 인덱싱되는지 정의하는 스키마입니다.

```
┌────────────────────────────────────────────────────────┐
│                    Mapping Definition                   │
├────────────────────────────────────────────────────────┤
│                                                         │
│   Field Name    │    Type     │    Options             │
│  ───────────────┼─────────────┼──────────────────────  │
│   name          │   text      │   analyzer: standard   │
│   price         │   integer   │   index: true          │
│   category      │   keyword   │   index: true          │
│   description   │   text      │   analyzer: korean     │
│   created_at    │   date      │   format: ISO8601      │
│   location      │   geo_point │                        │
│                                                         │
└────────────────────────────────────────────────────────┘
```

### 핵심 Field Types

#### Text vs Keyword

```json
{
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "standard"
      },
      "status": {
        "type": "keyword"
      },
      "tags": {
        "type": "keyword"
      },
      "content": {
        "type": "text",
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 256
          }
        }
      }
    }
  }
}
```

| 구분 | text | keyword |
|------|------|---------|
| 분석 | 분석기를 통해 토큰화 | 분석하지 않음 (원본 그대로) |
| 검색 | Full-text search | Exact match, 집계 |
| 예시 | 본문, 설명, 제목 | ID, 상태값, 태그 |

#### Numeric Types

```json
{
  "properties": {
    "count": { "type": "integer" },
    "price": { "type": "long" },
    "rating": { "type": "float" },
    "precise_value": { "type": "double" },
    "small_num": { "type": "short" },
    "tiny_num": { "type": "byte" }
  }
}
```

#### Date Type

```json
{
  "properties": {
    "created_at": {
      "type": "date",
      "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
    }
  }
}
```

#### Nested Type

배열 내의 객체를 독립적으로 인덱싱합니다.

```json
{
  "mappings": {
    "properties": {
      "comments": {
        "type": "nested",
        "properties": {
          "user": { "type": "keyword" },
          "text": { "type": "text" },
          "date": { "type": "date" }
        }
      }
    }
  }
}
```

### Dynamic Mapping

```json
{
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "name": { "type": "text" },
      "metadata": {
        "type": "object",
        "dynamic": true
      }
    }
  }
}
```

| 설정 | 동작 |
|------|------|
| `true` | 새 필드 자동 추가 (기본값) |
| `false` | 새 필드 무시, 검색 불가 |
| `strict` | 새 필드 거부, 오류 발생 |
| `runtime` | Runtime field로 추가 |

### 실전 Mapping 예제

```json
PUT /products
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "korean_analyzer": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["lowercase", "nori_readingform"]
        }
      }
    }
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "id": { "type": "keyword" },
      "name": {
        "type": "text",
        "analyzer": "korean_analyzer",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "korean_analyzer"
      },
      "price": { "type": "integer" },
      "discount_rate": { "type": "float" },
      "category": { "type": "keyword" },
      "tags": { "type": "keyword" },
      "stock": { "type": "integer" },
      "is_active": { "type": "boolean" },
      "created_at": {
        "type": "date",
        "format": "yyyy-MM-dd'T'HH:mm:ss.SSSZ||yyyy-MM-dd"
      },
      "updated_at": {
        "type": "date"
      },
      "location": { "type": "geo_point" },
      "specifications": {
        "type": "nested",
        "properties": {
          "key": { "type": "keyword" },
          "value": { "type": "text" }
        }
      }
    }
  }
}
```

---

## 3. Shard와 Replica

### Shard (샤드)

Index를 물리적으로 분할한 단위입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                     Index: products                              │
│                     (Total: 6 Documents)                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐          │
│   │  Shard 0    │   │  Shard 1    │   │  Shard 2    │          │
│   │  (Primary)  │   │  (Primary)  │   │  (Primary)  │          │
│   ├─────────────┤   ├─────────────┤   ├─────────────┤          │
│   │  Doc 1      │   │  Doc 2      │   │  Doc 3      │          │
│   │  Doc 4      │   │  Doc 5      │   │  Doc 6      │          │
│   └─────────────┘   └─────────────┘   └─────────────┘          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Replica (복제본)

Primary Shard의 복사본으로 고가용성과 검색 성능을 제공합니다.

```
┌──────────────────────────────────────────────────────────────────────┐
│                        3-Node Cluster                                 │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│   Node 1              Node 2              Node 3                     │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐            │
│   │  P0         │    │  P1         │    │  P2         │            │
│   │  (Primary)  │    │  (Primary)  │    │  (Primary)  │            │
│   ├─────────────┤    ├─────────────┤    ├─────────────┤            │
│   │  R1         │    │  R2         │    │  R0         │            │
│   │  (Replica)  │    │  (Replica)  │    │  (Replica)  │            │
│   └─────────────┘    └─────────────┘    └─────────────┘            │
│                                                                       │
│   P = Primary Shard,  R = Replica Shard                              │
│   Primary와 Replica는 반드시 다른 Node에 배치됨                        │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

### Shard 할당 전략

```json
PUT /products
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "routing.allocation.total_shards_per_node": 2
  }
}
```

### Shard 개수 결정 가이드

| 데이터 크기 | 권장 Shard 크기 | 권장 Shard 수 |
|------------|-----------------|---------------|
| < 1GB | 1 Shard | 1 |
| 1GB ~ 50GB | 10-50GB/Shard | 1-5 |
| 50GB ~ 500GB | 20-50GB/Shard | 10-25 |
| > 500GB | 30-50GB/Shard | 필요에 따라 |

**주의사항:**
- Primary Shard 수는 Index 생성 후 변경 불가 (Reindex 필요)
- Replica 수는 동적으로 변경 가능
- 과도한 Shard는 오버헤드 발생 (Shard per Node < 1000 권장)

```bash
# Replica 수 동적 변경
PUT /products/_settings
{
  "number_of_replicas": 2
}
```

---

## 4. Inverted Index

### Inverted Index란?

문서에서 단어를 추출하여 단어 → 문서 위치 매핑을 만드는 구조입니다.

```
┌────────────────────────────────────────────────────────────────┐
│                     원본 Documents                              │
├────────────────────────────────────────────────────────────────┤
│  Doc 1: "스마트폰 최신 모델 출시"                                │
│  Doc 2: "최신 노트북 할인 행사"                                  │
│  Doc 3: "스마트폰 케이스 특가"                                   │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    [ 분석(Analysis) 과정 ]
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                     Inverted Index                              │
├──────────────┬─────────────────────────────────────────────────┤
│    Term      │          Posting List                           │
├──────────────┼─────────────────────────────────────────────────┤
│  스마트폰    │  [Doc1:pos0, Doc3:pos0]                         │
│  최신        │  [Doc1:pos1, Doc2:pos0]                         │
│  모델        │  [Doc1:pos2]                                     │
│  출시        │  [Doc1:pos3]                                     │
│  노트북      │  [Doc2:pos1]                                     │
│  할인        │  [Doc2:pos2]                                     │
│  행사        │  [Doc2:pos3]                                     │
│  케이스      │  [Doc3:pos1]                                     │
│  특가        │  [Doc3:pos2]                                     │
└──────────────┴─────────────────────────────────────────────────┘
```

### Analysis 과정

텍스트를 토큰으로 분리하는 과정입니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Analysis Pipeline                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Input Text: "The Quick BROWN Fox's"                           │
│                      │                                           │
│                      ▼                                           │
│   ┌─────────────────────────────────────┐                       │
│   │     Character Filters               │                       │
│   │     (HTML 제거, 특수문자 변환)       │                       │
│   └──────────────────┬──────────────────┘                       │
│                      │ "The Quick BROWN Foxs"                   │
│                      ▼                                           │
│   ┌─────────────────────────────────────┐                       │
│   │     Tokenizer                       │                       │
│   │     (텍스트를 토큰으로 분리)         │                       │
│   └──────────────────┬──────────────────┘                       │
│                      │ ["The", "Quick", "BROWN", "Foxs"]        │
│                      ▼                                           │
│   ┌─────────────────────────────────────┐                       │
│   │     Token Filters                   │                       │
│   │     (소문자화, 불용어 제거, 형태소)  │                       │
│   └──────────────────┬──────────────────┘                       │
│                      │ ["quick", "brown", "fox"]                │
│                      ▼                                           │
│   Output Tokens: ["quick", "brown", "fox"]                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 분석기 테스트

```bash
# Standard Analyzer 테스트
POST /_analyze
{
  "analyzer": "standard",
  "text": "The Quick BROWN Fox's jumped!"
}

# 결과: ["the", "quick", "brown", "fox's", "jumped"]

# Custom Analyzer 테스트
POST /products/_analyze
{
  "field": "name",
  "text": "스마트폰 최신 모델"
}
```

### 내장 Analyzer 종류

| Analyzer | 설명 | 토큰화 결과 (입력: "The 2 QUICK Brown-Foxes") |
|----------|------|---------------------------------------------|
| standard | 기본값, Unicode 기반 | [the, 2, quick, brown, foxes] |
| simple | 문자만 추출, 소문자화 | [the, quick, brown, foxes] |
| whitespace | 공백 기준 분리 | [The, 2, QUICK, Brown-Foxes] |
| keyword | 분리하지 않음 | [The 2 QUICK Brown-Foxes] |
| stop | standard + 불용어 제거 | [2, quick, brown, foxes] |

---

## 5. Cluster Architecture

### Cluster 구성 요소

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         Elasticsearch Cluster                             │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│    ┌─────────────────────────────────────────────────────────────────┐   │
│    │                    Master Node (Active)                         │   │
│    │    • Cluster State 관리                                          │   │
│    │    • Index 생성/삭제                                             │   │
│    │    • Shard 할당 결정                                             │   │
│    └─────────────────────────────────────────────────────────────────┘   │
│                                                                           │
│    ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐   │
│    │   Data Node 1    │   │   Data Node 2    │   │   Data Node 3    │   │
│    │  ┌────┐ ┌────┐   │   │  ┌────┐ ┌────┐   │   │  ┌────┐ ┌────┐   │   │
│    │  │ P0 │ │ R1 │   │   │  │ P1 │ │ R2 │   │   │  │ P2 │ │ R0 │   │   │
│    │  └────┘ └────┘   │   │  └────┘ └────┘   │   │  └────┘ └────┘   │   │
│    └──────────────────┘   └──────────────────┘   └──────────────────┘   │
│                                                                           │
│    ┌─────────────────────────────────────────────────────────────────┐   │
│    │                    Coordinating Node                             │   │
│    │    • 요청 라우팅                                                  │   │
│    │    • 결과 취합                                                    │   │
│    │    • 로드 밸런싱                                                  │   │
│    └─────────────────────────────────────────────────────────────────┘   │
│                                                                           │
└──────────────────────────────────────────────────────────────────────────┘
```

### 검색 요청 흐름

```
Client Request
      │
      ▼
┌─────────────────┐
│  Coordinating   │  1. 요청 수신
│     Node        │
└────────┬────────┘
         │
         │  2. Scatter (쿼리 분산)
         ▼
┌────────┴────────┬────────────────┐
│                 │                 │
▼                 ▼                 ▼
┌─────┐       ┌─────┐         ┌─────┐
│Shard│       │Shard│         │Shard│  3. 각 Shard에서 검색
│  0  │       │  1  │         │  2  │
└──┬──┘       └──┬──┘         └──┬──┘
   │             │               │
   │  4. Gather (결과 수집)       │
   └─────────────┼───────────────┘
                 ▼
       ┌─────────────────┐
       │  Coordinating   │  5. 결과 병합 및 정렬
       │     Node        │
       └────────┬────────┘
                │
                ▼
          Final Response
```

---

## 6. Node Types

### Node Role 설정

```yaml
# elasticsearch.yml

# Master-eligible Node
node.roles: [ master ]

# Data Node
node.roles: [ data ]

# Coordinating Only Node
node.roles: [ ]

# Multi-role Node (개발 환경)
node.roles: [ master, data, ingest ]
```

### Node Types 상세

| Node Type | Role | 주요 역할 |
|-----------|------|----------|
| Master | master | Cluster 상태 관리, Index/Shard 관리 |
| Data | data | 데이터 저장, CRUD, 검색, 집계 |
| Ingest | ingest | Document 전처리 파이프라인 |
| ML | ml | Machine Learning 작업 |
| Coordinating | (없음) | 요청 라우팅, 결과 취합 |
| Transform | transform | 데이터 변환 |

### 프로덕션 권장 구성

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Production Cluster (최소 구성)                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   Master Nodes (3개, Quorum)                                            │
│   ┌───────────┐   ┌───────────┐   ┌───────────┐                        │
│   │ Master 1  │   │ Master 2  │   │ Master 3  │                        │
│   │ (Active)  │   │ (Standby) │   │ (Standby) │                        │
│   └───────────┘   └───────────┘   └───────────┘                        │
│                                                                          │
│   Data Nodes (3개 이상)                                                  │
│   ┌───────────┐   ┌───────────┐   ┌───────────┐                        │
│   │  Data 1   │   │  Data 2   │   │  Data 3   │                        │
│   │  64GB+    │   │  64GB+    │   │  64GB+    │                        │
│   │  SSD      │   │  SSD      │   │  SSD      │                        │
│   └───────────┘   └───────────┘   └───────────┘                        │
│                                                                          │
│   Coordinating Nodes (2개 이상)                                          │
│   ┌───────────┐   ┌───────────┐                                        │
│   │  Coord 1  │   │  Coord 2  │                                        │
│   └───────────┘   └───────────┘                                        │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 핵심 요약

```
┌─────────────────────────────────────────────────────────────────────────┐
│                 Elasticsearch Core Concepts Summary                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   Document                                                               │
│   └── 데이터의 최소 단위 (JSON)                                          │
│                                                                          │
│   Index                                                                  │
│   └── Document의 논리적 그룹 (RDBMS Table)                               │
│                                                                          │
│   Mapping                                                                │
│   └── 필드 타입 정의 (Schema)                                            │
│       ├── text: 분석 O, 전문 검색용                                      │
│       └── keyword: 분석 X, 정확한 매칭용                                  │
│                                                                          │
│   Shard                                                                  │
│   ├── Primary: Index의 물리적 분할 단위                                   │
│   └── Replica: Primary의 복제본 (HA, 검색 성능)                          │
│                                                                          │
│   Inverted Index                                                         │
│   └── 단어 → 문서 위치 매핑 (빠른 검색의 핵심)                            │
│                                                                          │
│   Cluster                                                                │
│   ├── Master Node: 클러스터 관리                                         │
│   ├── Data Node: 데이터 저장/검색                                        │
│   └── Coordinating Node: 요청 라우팅                                     │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 다음 단계

- [Query DSL](./es-query-dsl.md) - 검색 쿼리 작성법
- [Aggregations](./es-aggregations.md) - 집계와 분석
- [Korean Analysis](./es-korean.md) - 한글 분석기 설정
