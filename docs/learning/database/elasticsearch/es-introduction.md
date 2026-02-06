# Elasticsearch 소개

## 학습 목표
- Elasticsearch의 핵심 아키텍처 이해
- 검색 엔진의 기본 원리 파악
- Portal Universe 상품 검색에서의 활용 개요

---

## 1. Elasticsearch란?

Elasticsearch는 **분산 검색 및 분석 엔진**입니다. Apache Lucene 기반으로 구축되었으며, 대용량 데이터의 실시간 검색과 분석을 제공합니다.

### 핵심 특징

| 특성 | 설명 |
|------|------|
| **분산 아키텍처** | 샤딩과 복제로 확장성 제공 |
| **실시간 검색** | Near Real-Time (NRT) 검색 |
| **RESTful API** | HTTP 기반 간편한 인터페이스 |
| **스키마 유연성** | 동적 매핑 지원 |
| **풍부한 쿼리** | Full-text, 필터, 집계 |
| **다국어 지원** | 한국어 포함 다양한 언어 분석기 |

---

## 2. 핵심 개념

### 2.1 Index (인덱스)

데이터를 저장하는 **논리적 컨테이너**입니다. RDBMS의 테이블과 유사합니다.

```
Elasticsearch 클러스터
├── products 인덱스      ← 상품 데이터
├── orders 인덱스        ← 주문 데이터
└── logs 인덱스          ← 로그 데이터
```

### 2.2 Document (문서)

인덱스에 저장되는 **JSON 형식의 데이터 단위**입니다.

```json
{
  "_index": "products",
  "_id": "1",
  "_source": {
    "id": 1,
    "name": "삼성 갤럭시 S24",
    "description": "최신 스마트폰",
    "price": 1200000,
    "stock": 100,
    "category": "스마트폰"
  }
}
```

### 2.3 Shard (샤드)

인덱스를 **물리적으로 분할**한 단위입니다.

```
products 인덱스
├── Shard 0 (Primary)   ─ Replica 0
├── Shard 1 (Primary)   ─ Replica 1
└── Shard 2 (Primary)   ─ Replica 2
```

- **Primary Shard**: 원본 데이터
- **Replica Shard**: 복제본 (장애 대비, 읽기 분산)

### 2.4 용어 비교

| RDBMS | Elasticsearch | 설명 |
|-------|---------------|------|
| Database | Cluster | 전체 시스템 |
| Table | Index | 데이터 컬렉션 |
| Row | Document | 데이터 단위 |
| Column | Field | 속성 |
| Schema | Mapping | 필드 정의 |
| SQL | Query DSL | 쿼리 언어 |

---

## 3. 검색 원리: 역인덱스 (Inverted Index)

### 3.1 일반 인덱스 vs 역인덱스

```
[일반 인덱스 - Forward Index]
문서 → 단어
Doc1: "삼성 갤럭시 스마트폰"
Doc2: "애플 아이폰 스마트폰"

검색 "스마트폰" → 모든 문서 순회 (느림)

[역인덱스 - Inverted Index]
단어 → 문서
"삼성"     → [Doc1]
"갤럭시"   → [Doc1]
"애플"     → [Doc2]
"아이폰"   → [Doc2]
"스마트폰" → [Doc1, Doc2]

검색 "스마트폰" → 바로 Doc1, Doc2 반환 (빠름)
```

### 3.2 텍스트 분석 과정

```
원본 텍스트: "삼성 Galaxy S24 최신 스마트폰!"

1. Character Filter (문자 필터)
   → "삼성 Galaxy S24 최신 스마트폰"  (특수문자 제거)

2. Tokenizer (토큰화)
   → ["삼성", "Galaxy", "S24", "최신", "스마트폰"]

3. Token Filter (토큰 필터)
   → ["삼성", "galaxy", "s24", "최신", "스마트폰"]  (소문자 변환)

4. 역인덱스 저장
   "삼성"     → [doc_id: 1, position: 0]
   "galaxy"   → [doc_id: 1, position: 1]
   ...
```

---

## 4. 한국어 검색: Nori 분석기

### 4.1 왜 한국어 분석기가 필요한가?

```
[Standard Analyzer - 공백 기준]
"삼성갤럭시스마트폰" → ["삼성갤럭시스마트폰"]  (하나의 토큰)
검색 "갤럭시" → 매칭 실패!

[Nori Analyzer - 형태소 분석]
"삼성갤럭시스마트폰" → ["삼성", "갤럭시", "스마트폰"]
검색 "갤럭시" → 매칭 성공!
```

### 4.2 Nori 분석기 구성

```json
{
  "settings": {
    "analysis": {
      "analyzer": {
        "korean": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["lowercase", "nori_readingform"]
        }
      }
    }
  }
}
```

| 구성요소 | 역할 |
|----------|------|
| `nori_tokenizer` | 한국어 형태소 분석 |
| `lowercase` | 영문 소문자 변환 |
| `nori_readingform` | 한자 → 한글 변환 |

---

## 5. Mapping (매핑)

### 5.1 필드 타입

| 타입 | 설명 | 사용 예 |
|------|------|---------|
| `text` | 전문 검색용 (분석됨) | 상품명, 설명 |
| `keyword` | 정확한 값 (분석 안됨) | ID, 상태, 태그 |
| `integer/long` | 정수 | 재고, ID |
| `double/float` | 실수 | 가격, 평점 |
| `boolean` | 참/거짓 | 활성화 여부 |
| `date` | 날짜/시간 | 생성일, 수정일 |
| `completion` | 자동완성 전용 | 검색어 제안 |

### 5.2 Portal Universe 상품 매핑

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "name": {
        "type": "text",
        "analyzer": "korean",
        "fields": {
          "keyword": { "type": "keyword" },
          "suggest": {
            "type": "completion",
            "analyzer": "korean"
          }
        }
      },
      "description": {
        "type": "text",
        "analyzer": "korean"
      },
      "price": { "type": "double" },
      "stock": { "type": "integer" },
      "category": { "type": "keyword" },
      "createdAt": { "type": "date" }
    }
  }
}
```

**Multi-field 매핑:**
- `name`: 전문 검색용 (text)
- `name.keyword`: 정렬/집계용 (keyword)
- `name.suggest`: 자동완성용 (completion)

---

## 6. 기본 Query DSL

### 6.1 Match Query (전문 검색)

```json
{
  "query": {
    "match": {
      "name": "삼성 스마트폰"
    }
  }
}
```

분석기를 통해 토큰화 후 검색합니다.

### 6.2 Term Query (정확한 값)

```json
{
  "query": {
    "term": {
      "category": "스마트폰"
    }
  }
}
```

분석 없이 정확히 일치하는 값을 찾습니다.

### 6.3 Bool Query (복합 조건)

```json
{
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "갤럭시" } }
      ],
      "filter": [
        { "range": { "price": { "gte": 500000, "lte": 1500000 } } }
      ]
    }
  }
}
```

| 조건 | 설명 |
|------|------|
| `must` | 반드시 일치 (점수 영향) |
| `should` | 선택적 일치 (점수 가산) |
| `filter` | 필터링 (점수 무관, 캐싱) |
| `must_not` | 제외 조건 |

### 6.4 Range Query (범위 검색)

```json
{
  "query": {
    "range": {
      "price": {
        "gte": 100000,
        "lte": 500000
      }
    }
  }
}
```

---

## 7. Portal Universe에서의 역할

### 7.1 검색 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                      Shopping Frontend                           │
│                    (검색창, 필터, 정렬)                           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Shopping Service                            │
│  ┌─────────────┐    ┌──────────────┐    ┌─────────────────┐    │
│  │ProductService│───▶│SearchService │───▶│ Elasticsearch   │    │
│  │   (CRUD)    │    │  (검색API)   │    │   (products)    │    │
│  └──────┬──────┘    └──────────────┘    └─────────────────┘    │
│         │                                        ▲              │
│         ▼                                        │              │
│  ┌─────────────┐                                │              │
│  │   MySQL     │────────────────────────────────┘              │
│  │  (원본DB)   │         데이터 동기화                          │
│  └─────────────┘                                               │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 제공 기능

| 기능 | 설명 |
|------|------|
| **키워드 검색** | 상품명/설명 전문 검색 |
| **오타 교정** | Fuzzy 검색으로 오타 허용 |
| **자동완성** | Completion Suggester |
| **필터링** | 가격 범위, 카테고리 |
| **정렬** | 관련성, 가격, 최신순 |
| **하이라이트** | 검색어 강조 표시 |

### 7.3 데이터 흐름

```
1. 상품 생성/수정 (MySQL)
        ↓
2. ProductSearchService.indexProduct()
        ↓
3. Elasticsearch 인덱싱
        ↓
4. 사용자 검색 요청
        ↓
5. SearchController → ProductSearchService.search()
        ↓
6. Elasticsearch Query 실행
        ↓
7. 검색 결과 반환 (하이라이트, 점수 포함)
```

---

## 8. MySQL vs Elasticsearch

| 특성 | MySQL | Elasticsearch |
|------|-------|---------------|
| **용도** | OLTP, 트랜잭션 | 검색, 분석 |
| **쿼리** | SQL | Query DSL |
| **전문 검색** | FULLTEXT (제한적) | 강력 (분석기) |
| **JOIN** | 지원 | 비추천 |
| **트랜잭션** | ACID | X |
| **실시간성** | 즉시 | Near Real-Time |
| **확장** | 수직 확장 | 수평 확장 |

**Portal Universe 선택:**
- **MySQL**: 상품 마스터 데이터 (원본)
- **Elasticsearch**: 검색 인덱스 (복제)

---

## 9. 핵심 정리

| 개념 | 설명 |
|------|------|
| **Index** | 데이터 저장 컨테이너 |
| **Document** | JSON 형식 데이터 단위 |
| **Shard** | 인덱스의 물리적 분할 |
| **역인덱스** | 단어 → 문서 매핑 (빠른 검색) |
| **Mapping** | 필드 타입과 분석기 정의 |
| **Nori** | 한국어 형태소 분석기 |
| **Query DSL** | JSON 기반 쿼리 언어 |

---

## 다음 학습

- [Elasticsearch Query DSL 심화](./es-query-dsl.md)
- [Elasticsearch 한국어 검색](./es-korean.md)
- [Elasticsearch Spring 통합](./es-spring-integration.md)

---

## 참고 자료

- [Elasticsearch 공식 문서](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Nori 분석기 가이드](https://www.elastic.co/guide/en/elasticsearch/plugins/current/analysis-nori.html)
- [Query DSL 레퍼런스](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html)
