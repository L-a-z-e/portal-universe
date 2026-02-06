# Elasticsearch Query DSL

Elasticsearch의 Query DSL (Domain Specific Language)을 활용한 검색 쿼리 작성법을 학습합니다.

---

## 목차

1. [Query DSL 기본 구조](#1-query-dsl-기본-구조)
2. [Full-text Queries](#2-full-text-queries)
3. [Term-level Queries](#3-term-level-queries)
4. [Compound Queries](#4-compound-queries)
5. [Nested Queries](#5-nested-queries)
6. [실전 쿼리 예제](#6-실전-쿼리-예제)

---

## 1. Query DSL 기본 구조

### Query Context vs Filter Context

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Query vs Filter Context                           │
├──────────────────────────────┬──────────────────────────────────────┤
│        Query Context         │         Filter Context               │
├──────────────────────────────┼──────────────────────────────────────┤
│  "얼마나 잘 매칭되는가?"       │   "매칭되는가/안되는가?"              │
│  _score 계산 O               │   _score 계산 X                      │
│  캐싱 X                      │   캐싱 O (성능 향상)                  │
│  Full-text 검색에 적합        │   필터링에 적합                      │
├──────────────────────────────┼──────────────────────────────────────┤
│  예: match, multi_match      │   예: term, range, exists            │
└──────────────────────────────┴──────────────────────────────────────┘
```

### 기본 검색 구조

```json
GET /products/_search
{
  "query": {
    "bool": {
      "must": [...],      // AND, _score에 영향
      "should": [...],    // OR, _score에 영향
      "must_not": [...],  // NOT, Filter context
      "filter": [...]     // AND, Filter context (캐싱)
    }
  },
  "from": 0,              // 시작 위치 (pagination)
  "size": 10,             // 반환 개수
  "sort": [...],          // 정렬
  "_source": [...],       // 반환 필드 지정
  "highlight": {...}      // 하이라이팅
}
```

---

## 2. Full-text Queries

### match Query

가장 기본적인 전문 검색 쿼리입니다.

```json
// 기본 match
GET /products/_search
{
  "query": {
    "match": {
      "description": "스마트폰 케이스"
    }
  }
}

// OR 조건 (기본값)
// "스마트폰" OR "케이스"를 포함하는 문서

// AND 조건
GET /products/_search
{
  "query": {
    "match": {
      "description": {
        "query": "스마트폰 케이스",
        "operator": "and"
      }
    }
  }
}

// 최소 매칭 비율
GET /products/_search
{
  "query": {
    "match": {
      "description": {
        "query": "스마트폰 케이스 가죽 프리미엄",
        "minimum_should_match": "75%"
      }
    }
  }
}
```

### match_phrase Query

단어 순서를 유지하며 검색합니다.

```json
// 정확한 구문 검색
GET /products/_search
{
  "query": {
    "match_phrase": {
      "description": "스마트폰 케이스"
    }
  }
}

// slop: 단어 사이 허용 거리
GET /products/_search
{
  "query": {
    "match_phrase": {
      "description": {
        "query": "스마트폰 케이스",
        "slop": 2
      }
    }
  }
}
// "스마트폰 가죽 케이스" 매칭 (1단어 거리)
```

### multi_match Query

여러 필드에서 동시 검색합니다.

```json
// 기본 multi_match
GET /products/_search
{
  "query": {
    "multi_match": {
      "query": "스마트폰",
      "fields": ["name", "description", "category"]
    }
  }
}

// 필드별 가중치 부여
GET /products/_search
{
  "query": {
    "multi_match": {
      "query": "스마트폰",
      "fields": ["name^3", "description^2", "category"],
      "type": "best_fields"
    }
  }
}
```

### multi_match Types

```
┌────────────────────────────────────────────────────────────────────────┐
│                      multi_match Type Options                          │
├─────────────────┬──────────────────────────────────────────────────────┤
│  best_fields    │  가장 잘 매칭되는 필드의 점수 사용 (기본값)             │
│                 │  tie_breaker로 다른 필드 점수 일부 반영 가능            │
├─────────────────┼──────────────────────────────────────────────────────┤
│  most_fields    │  모든 필드 점수 합산                                   │
│                 │  동의어가 있는 필드들에 유용                            │
├─────────────────┼──────────────────────────────────────────────────────┤
│  cross_fields   │  모든 필드를 하나의 큰 필드처럼 취급                    │
│                 │  이름(first_name + last_name) 검색에 유용              │
├─────────────────┼──────────────────────────────────────────────────────┤
│  phrase         │  match_phrase로 각 필드 검색                          │
├─────────────────┼──────────────────────────────────────────────────────┤
│  phrase_prefix  │  match_phrase_prefix로 각 필드 검색                   │
│                 │  자동완성에 유용                                       │
└─────────────────┴──────────────────────────────────────────────────────┘
```

```json
// cross_fields 예제 (사람 이름 검색)
GET /users/_search
{
  "query": {
    "multi_match": {
      "query": "홍길동",
      "fields": ["first_name", "last_name"],
      "type": "cross_fields",
      "operator": "and"
    }
  }
}
```

### match_phrase_prefix Query

자동완성에 유용한 쿼리입니다.

```json
GET /products/_search
{
  "query": {
    "match_phrase_prefix": {
      "name": {
        "query": "아이폰 케",
        "max_expansions": 50
      }
    }
  }
}
// "아이폰 케이스", "아이폰 케이블" 등 매칭
```

---

## 3. Term-level Queries

분석(Analysis) 과정을 거치지 않고 정확한 값으로 검색합니다.

### term Query

정확한 값 매칭 (keyword 필드용).

```json
// 단일 값 매칭
GET /products/_search
{
  "query": {
    "term": {
      "category": {
        "value": "electronics"
      }
    }
  }
}

// case_insensitive 옵션 (7.10+)
GET /products/_search
{
  "query": {
    "term": {
      "category": {
        "value": "ELECTRONICS",
        "case_insensitive": true
      }
    }
  }
}
```

### terms Query

여러 값 중 하나라도 매칭 (OR 조건).

```json
GET /products/_search
{
  "query": {
    "terms": {
      "category": ["electronics", "computers", "phones"]
    }
  }
}
```

### range Query

범위 검색에 사용합니다.

```json
// 숫자 범위
GET /products/_search
{
  "query": {
    "range": {
      "price": {
        "gte": 10000,
        "lte": 100000
      }
    }
  }
}

// 날짜 범위
GET /products/_search
{
  "query": {
    "range": {
      "created_at": {
        "gte": "2024-01-01",
        "lt": "2024-02-01",
        "format": "yyyy-MM-dd"
      }
    }
  }
}

// 상대 날짜
GET /products/_search
{
  "query": {
    "range": {
      "created_at": {
        "gte": "now-7d/d",
        "lte": "now/d"
      }
    }
  }
}
```

### Range 연산자

| 연산자 | 설명 | 수학 기호 |
|-------|------|----------|
| gt | Greater than | > |
| gte | Greater than or equal | >= |
| lt | Less than | < |
| lte | Less than or equal | <= |

### 날짜 수학 표현식

```
now          현재 시간
now-1d       1일 전
now-1M       1개월 전
now/d        오늘 시작 (00:00:00)
now/M        이번 달 시작
2024-01-01||+1M    2024-01-01에서 1개월 후
```

### exists Query

필드 존재 여부를 확인합니다.

```json
GET /products/_search
{
  "query": {
    "exists": {
      "field": "discount_rate"
    }
  }
}
```

### prefix Query

접두사로 검색합니다.

```json
GET /products/_search
{
  "query": {
    "prefix": {
      "name.keyword": {
        "value": "아이폰"
      }
    }
  }
}
```

### wildcard Query

와일드카드 패턴으로 검색합니다.

```json
GET /products/_search
{
  "query": {
    "wildcard": {
      "name.keyword": {
        "value": "*폰*케이스"
      }
    }
  }
}
// * : 0개 이상의 문자
// ? : 정확히 1개의 문자
```

### regexp Query

정규식으로 검색합니다.

```json
GET /products/_search
{
  "query": {
    "regexp": {
      "product_code": {
        "value": "PROD-[0-9]{4}",
        "flags": "ALL"
      }
    }
  }
}
```

---

## 4. Compound Queries

### bool Query

여러 쿼리를 조합합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         bool Query Structure                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   bool                                                               │
│   ├── must: [...]        // AND 조건, _score 영향 O                  │
│   │   └── 모든 조건 충족 필수                                         │
│   │                                                                  │
│   ├── filter: [...]      // AND 조건, _score 영향 X, 캐싱 O          │
│   │   └── 필터링 용도 (범위, 존재 여부 등)                            │
│   │                                                                  │
│   ├── should: [...]      // OR 조건, _score 영향 O                   │
│   │   └── minimum_should_match로 최소 매칭 수 지정                   │
│   │                                                                  │
│   └── must_not: [...]    // NOT 조건, Filter context                │
│       └── 제외 조건                                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

```json
GET /products/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "name": "스마트폰"
          }
        }
      ],
      "filter": [
        {
          "term": {
            "category": "electronics"
          }
        },
        {
          "range": {
            "price": {
              "gte": 100000,
              "lte": 1000000
            }
          }
        }
      ],
      "should": [
        {
          "term": {
            "brand": "삼성"
          }
        },
        {
          "term": {
            "brand": "애플"
          }
        }
      ],
      "must_not": [
        {
          "term": {
            "is_discontinued": true
          }
        }
      ],
      "minimum_should_match": 1
    }
  }
}
```

### boosting Query

Positive와 Negative 점수를 조절합니다.

```json
GET /products/_search
{
  "query": {
    "boosting": {
      "positive": {
        "match": {
          "name": "스마트폰"
        }
      },
      "negative": {
        "term": {
          "condition": "used"
        }
      },
      "negative_boost": 0.5
    }
  }
}
// 중고 상품은 점수 50% 감소
```

### constant_score Query

모든 매칭 문서에 동일한 점수를 부여합니다.

```json
GET /products/_search
{
  "query": {
    "constant_score": {
      "filter": {
        "term": {
          "category": "electronics"
        }
      },
      "boost": 1.2
    }
  }
}
```

### dis_max Query

여러 쿼리 중 가장 높은 점수를 사용합니다.

```json
GET /products/_search
{
  "query": {
    "dis_max": {
      "queries": [
        { "match": { "name": "스마트폰" } },
        { "match": { "description": "스마트폰" } }
      ],
      "tie_breaker": 0.3
    }
  }
}
// tie_breaker: 다른 매칭 쿼리의 점수 반영 비율
```

### function_score Query

사용자 정의 점수 계산 함수를 적용합니다.

```json
GET /products/_search
{
  "query": {
    "function_score": {
      "query": {
        "match": {
          "name": "스마트폰"
        }
      },
      "functions": [
        {
          "filter": { "term": { "is_featured": true } },
          "weight": 2
        },
        {
          "field_value_factor": {
            "field": "popularity",
            "factor": 1.2,
            "modifier": "sqrt",
            "missing": 1
          }
        },
        {
          "gauss": {
            "created_at": {
              "origin": "now",
              "scale": "7d",
              "decay": 0.5
            }
          }
        }
      ],
      "score_mode": "multiply",
      "boost_mode": "multiply"
    }
  }
}
```

### function_score 옵션

| 옵션 | 값 | 설명 |
|------|-----|------|
| score_mode | multiply, sum, avg, first, max, min | 여러 function 점수 결합 방식 |
| boost_mode | multiply, replace, sum, avg, max, min | query 점수와 function 점수 결합 방식 |

---

## 5. Nested Queries

Nested 타입 필드를 검색합니다.

### Nested가 필요한 이유

```
일반 Object 배열의 문제점:

Document:
{
  "comments": [
    { "user": "kim", "text": "좋아요" },
    { "user": "lee", "text": "별로예요" }
  ]
}

내부 저장 방식 (Flattened):
{
  "comments.user": ["kim", "lee"],
  "comments.text": ["좋아요", "별로예요"]
}

문제: "user가 kim이고 text가 별로예요인 comment" 검색 시
→ 잘못된 결과 반환 (kim의 "좋아요"와 lee의 "별로예요"가 섞임)
```

### Nested Mapping 설정

```json
PUT /posts
{
  "mappings": {
    "properties": {
      "title": { "type": "text" },
      "comments": {
        "type": "nested",
        "properties": {
          "user": { "type": "keyword" },
          "text": { "type": "text" },
          "created_at": { "type": "date" }
        }
      }
    }
  }
}
```

### Nested Query

```json
GET /posts/_search
{
  "query": {
    "nested": {
      "path": "comments",
      "query": {
        "bool": {
          "must": [
            { "term": { "comments.user": "kim" } },
            { "match": { "comments.text": "좋아요" } }
          ]
        }
      },
      "inner_hits": {
        "size": 3,
        "highlight": {
          "fields": {
            "comments.text": {}
          }
        }
      }
    }
  }
}
```

### Nested Aggregation과 함께 사용

```json
GET /posts/_search
{
  "query": {
    "match_all": {}
  },
  "aggs": {
    "comments_agg": {
      "nested": {
        "path": "comments"
      },
      "aggs": {
        "top_commenters": {
          "terms": {
            "field": "comments.user",
            "size": 10
          }
        }
      }
    }
  }
}
```

---

## 6. 실전 쿼리 예제

### 상품 검색 (E-commerce)

```json
GET /products/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "multi_match": {
            "query": "무선 이어폰",
            "fields": ["name^3", "description^2", "brand"],
            "type": "best_fields",
            "fuzziness": "AUTO"
          }
        }
      ],
      "filter": [
        { "term": { "is_active": true } },
        { "range": { "price": { "gte": 50000, "lte": 200000 } } },
        { "terms": { "category": ["electronics", "audio"] } }
      ],
      "should": [
        { "term": { "is_featured": { "value": true, "boost": 2 } } },
        { "range": { "rating": { "gte": 4.0, "boost": 1.5 } } }
      ]
    }
  },
  "sort": [
    { "_score": "desc" },
    { "sales_count": "desc" }
  ],
  "highlight": {
    "fields": {
      "name": {},
      "description": { "fragment_size": 150 }
    },
    "pre_tags": ["<em>"],
    "post_tags": ["</em>"]
  },
  "from": 0,
  "size": 20,
  "_source": ["name", "price", "brand", "thumbnail", "rating"]
}
```

### 블로그 포스트 검색

```json
GET /posts/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "multi_match": {
            "query": "Elasticsearch 튜토리얼",
            "fields": ["title^3", "content", "tags^2"],
            "type": "most_fields"
          }
        }
      ],
      "filter": [
        { "term": { "status": "published" } },
        { "range": { "published_at": { "gte": "now-1y" } } }
      ],
      "should": [
        {
          "nested": {
            "path": "comments",
            "query": {
              "range": {
                "comments.created_at": {
                  "gte": "now-7d"
                }
              }
            }
          }
        }
      ]
    }
  },
  "sort": [
    { "_score": "desc" },
    { "view_count": "desc" }
  ],
  "aggs": {
    "by_tag": {
      "terms": {
        "field": "tags",
        "size": 10
      }
    },
    "by_month": {
      "date_histogram": {
        "field": "published_at",
        "calendar_interval": "month"
      }
    }
  }
}
```

### 사용자 검색 (자동완성)

```json
GET /users/_search
{
  "query": {
    "bool": {
      "should": [
        {
          "match_phrase_prefix": {
            "name": {
              "query": "김철",
              "max_expansions": 50,
              "boost": 2
            }
          }
        },
        {
          "match": {
            "name": {
              "query": "김철",
              "fuzziness": "AUTO"
            }
          }
        }
      ]
    }
  },
  "size": 10,
  "_source": ["id", "name", "profile_image", "department"]
}
```

### 지역 기반 검색 (Geo)

```json
GET /stores/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "category": "카페" } }
      ],
      "filter": [
        {
          "geo_distance": {
            "distance": "5km",
            "location": {
              "lat": 37.5665,
              "lon": 126.9780
            }
          }
        }
      ]
    }
  },
  "sort": [
    {
      "_geo_distance": {
        "location": {
          "lat": 37.5665,
          "lon": 126.9780
        },
        "order": "asc",
        "unit": "km"
      }
    }
  ]
}
```

### 복합 필터 (Faceted Search)

```json
GET /products/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "name": "노트북" } }
      ],
      "filter": [
        { "term": { "brand": "삼성" } },
        { "range": { "price": { "gte": 1000000, "lte": 2000000 } } },
        { "terms": { "specs.ram": ["16GB", "32GB"] } }
      ]
    }
  },
  "aggs": {
    "brands": {
      "terms": { "field": "brand", "size": 20 }
    },
    "price_ranges": {
      "range": {
        "field": "price",
        "ranges": [
          { "to": 1000000, "key": "100만원 미만" },
          { "from": 1000000, "to": 2000000, "key": "100-200만원" },
          { "from": 2000000, "key": "200만원 이상" }
        ]
      }
    },
    "ram_options": {
      "terms": { "field": "specs.ram", "size": 10 }
    }
  },
  "post_filter": {
    "term": { "brand": "삼성" }
  }
}
```

---

## 핵심 요약

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Query DSL Quick Reference                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Full-text Queries (text 필드용)                                     │
│  ├── match: 기본 전문 검색                                           │
│  ├── match_phrase: 구문 검색 (순서 유지)                              │
│  ├── multi_match: 여러 필드 검색                                      │
│  └── match_phrase_prefix: 자동완성                                   │
│                                                                      │
│  Term-level Queries (keyword 필드용)                                 │
│  ├── term/terms: 정확한 값 매칭                                       │
│  ├── range: 범위 검색                                                │
│  ├── exists: 필드 존재 확인                                          │
│  ├── prefix/wildcard: 패턴 매칭                                      │
│  └── regexp: 정규식 검색                                             │
│                                                                      │
│  Compound Queries                                                    │
│  ├── bool: must/filter/should/must_not 조합                         │
│  ├── boosting: positive/negative 점수 조절                           │
│  ├── dis_max: 최고 점수 선택                                         │
│  └── function_score: 커스텀 스코어링                                  │
│                                                                      │
│  Best Practices                                                      │
│  ├── 필터링은 filter context 사용 (캐싱)                              │
│  ├── keyword 필드에는 term, text 필드에는 match                       │
│  └── 복잡한 쿼리는 bool로 조합                                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 다음 단계

- [Aggregations](./es-aggregations.md) - 집계와 통계 분석
- [Korean Analysis](./es-korean.md) - 한글 검색 최적화
- [Spring Integration](./es-spring-integration.md) - Spring Boot 연동
