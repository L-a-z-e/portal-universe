# Elasticsearch Aggregations

Elasticsearch의 집계(Aggregation) 기능을 활용한 데이터 분석과 통계 처리를 학습합니다.

---

## 목차

1. [Aggregation 기본 개념](#1-aggregation-기본-개념)
2. [Metric Aggregations](#2-metric-aggregations)
3. [Bucket Aggregations](#3-bucket-aggregations)
4. [Pipeline Aggregations](#4-pipeline-aggregations)
5. [Sub-aggregations](#5-sub-aggregations)
6. [실전 예제](#6-실전-예제)

---

## 1. Aggregation 기본 개념

### Aggregation 구조

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Aggregation Types                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────┐                                                │
│  │ Bucket Aggs     │  문서를 그룹(버킷)으로 분류                      │
│  │                 │  예: terms, range, date_histogram              │
│  └────────┬────────┘                                                │
│           │                                                          │
│           ▼                                                          │
│  ┌─────────────────┐                                                │
│  │ Metric Aggs     │  각 버킷의 통계값 계산                          │
│  │                 │  예: avg, sum, min, max, cardinality           │
│  └────────┬────────┘                                                │
│           │                                                          │
│           ▼                                                          │
│  ┌─────────────────┐                                                │
│  │ Pipeline Aggs   │  다른 집계 결과를 기반으로 추가 계산             │
│  │                 │  예: derivative, moving_avg, bucket_sort       │
│  └─────────────────┘                                                │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 기본 쿼리 구조

```json
GET /products/_search
{
  "size": 0,              // 검색 결과는 필요없고 집계만 필요할 때
  "query": {              // 집계 대상 필터링 (선택사항)
    "match": { "category": "electronics" }
  },
  "aggs": {               // 집계 정의
    "aggregation_name": { // 사용자 정의 이름
      "agg_type": {       // 집계 유형
        // 집계 설정
      }
    }
  }
}
```

---

## 2. Metric Aggregations

### 단일 값 반환 Metrics

#### avg, sum, min, max

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "avg_price": {
      "avg": { "field": "price" }
    },
    "total_sales": {
      "sum": { "field": "sales_count" }
    },
    "min_price": {
      "min": { "field": "price" }
    },
    "max_price": {
      "max": { "field": "price" }
    }
  }
}
```

**응답:**
```json
{
  "aggregations": {
    "avg_price": { "value": 150000.0 },
    "total_sales": { "value": 52340 },
    "min_price": { "value": 10000.0 },
    "max_price": { "value": 2500000.0 }
  }
}
```

#### value_count (문서 개수)

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "product_count": {
      "value_count": { "field": "id" }
    }
  }
}
```

#### cardinality (고유값 개수, 근사치)

```json
GET /orders/_search
{
  "size": 0,
  "aggs": {
    "unique_customers": {
      "cardinality": {
        "field": "customer_id",
        "precision_threshold": 1000
      }
    }
  }
}
```

### 다중 값 반환 Metrics

#### stats (기본 통계)

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "price_stats": {
      "stats": { "field": "price" }
    }
  }
}
```

**응답:**
```json
{
  "aggregations": {
    "price_stats": {
      "count": 1000,
      "min": 10000.0,
      "max": 2500000.0,
      "avg": 150000.0,
      "sum": 150000000.0
    }
  }
}
```

#### extended_stats (확장 통계)

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "price_extended_stats": {
      "extended_stats": { "field": "price" }
    }
  }
}
```

**응답:**
```json
{
  "aggregations": {
    "price_extended_stats": {
      "count": 1000,
      "min": 10000.0,
      "max": 2500000.0,
      "avg": 150000.0,
      "sum": 150000000.0,
      "sum_of_squares": 4.5e+13,
      "variance": 1.2e+10,
      "variance_population": 1.2e+10,
      "variance_sampling": 1.2e+10,
      "std_deviation": 109544.5,
      "std_deviation_population": 109544.5,
      "std_deviation_sampling": 109599.2,
      "std_deviation_bounds": {
        "upper": 369089.0,
        "lower": -69089.0
      }
    }
  }
}
```

#### percentiles (백분위수)

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "price_percentiles": {
      "percentiles": {
        "field": "price",
        "percents": [25, 50, 75, 90, 95, 99]
      }
    }
  }
}
```

**응답:**
```json
{
  "aggregations": {
    "price_percentiles": {
      "values": {
        "25.0": 50000.0,
        "50.0": 120000.0,
        "75.0": 200000.0,
        "90.0": 350000.0,
        "95.0": 500000.0,
        "99.0": 1200000.0
      }
    }
  }
}
```

#### percentile_ranks (백분위 순위)

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "price_percentile_ranks": {
      "percentile_ranks": {
        "field": "price",
        "values": [100000, 200000, 500000]
      }
    }
  }
}
// 100000원은 상위 몇 %인지?
```

#### top_hits (상위 문서)

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "by_category": {
      "terms": { "field": "category", "size": 5 },
      "aggs": {
        "top_products": {
          "top_hits": {
            "size": 3,
            "sort": [{ "sales_count": "desc" }],
            "_source": ["name", "price", "sales_count"]
          }
        }
      }
    }
  }
}
```

---

## 3. Bucket Aggregations

### terms Aggregation

필드 값별로 버킷을 생성합니다.

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "by_category": {
      "terms": {
        "field": "category",
        "size": 10,
        "order": { "_count": "desc" },
        "min_doc_count": 1
      }
    }
  }
}
```

**응답:**
```json
{
  "aggregations": {
    "by_category": {
      "doc_count_error_upper_bound": 0,
      "sum_other_doc_count": 150,
      "buckets": [
        { "key": "electronics", "doc_count": 500 },
        { "key": "clothing", "doc_count": 350 },
        { "key": "books", "doc_count": 200 }
      ]
    }
  }
}
```

### 정렬 옵션

```json
// 문서 개수순
"order": { "_count": "desc" }

// 키 알파벳순
"order": { "_key": "asc" }

// Sub-aggregation 값 기준
"order": { "avg_price": "desc" }
```

### range Aggregation

숫자 범위별 버킷을 생성합니다.

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "price_ranges": {
      "range": {
        "field": "price",
        "ranges": [
          { "key": "저가", "to": 50000 },
          { "key": "중가", "from": 50000, "to": 200000 },
          { "key": "고가", "from": 200000 }
        ]
      }
    }
  }
}
```

### date_range Aggregation

날짜 범위별 버킷을 생성합니다.

```json
GET /orders/_search
{
  "size": 0,
  "aggs": {
    "order_periods": {
      "date_range": {
        "field": "created_at",
        "format": "yyyy-MM-dd",
        "ranges": [
          { "key": "이번주", "from": "now-1w/d", "to": "now/d" },
          { "key": "이번달", "from": "now-1M/M", "to": "now/M" },
          { "key": "올해", "from": "now-1y/y", "to": "now/y" }
        ]
      }
    }
  }
}
```

### histogram Aggregation

일정 간격으로 버킷을 생성합니다.

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "price_histogram": {
      "histogram": {
        "field": "price",
        "interval": 50000,
        "min_doc_count": 0,
        "extended_bounds": {
          "min": 0,
          "max": 500000
        }
      }
    }
  }
}
```

### date_histogram Aggregation

시간 간격별 버킷을 생성합니다.

```json
GET /orders/_search
{
  "size": 0,
  "aggs": {
    "orders_over_time": {
      "date_histogram": {
        "field": "created_at",
        "calendar_interval": "month",
        "format": "yyyy-MM",
        "min_doc_count": 0,
        "time_zone": "Asia/Seoul"
      }
    }
  }
}
```

### Calendar Intervals vs Fixed Intervals

```
┌────────────────────────────────────────────────────────────────────┐
│              calendar_interval vs fixed_interval                   │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  calendar_interval                                                 │
│  ├── minute, hour, day, week, month, quarter, year                │
│  └── 달력 기준 (월마다 날수 다름)                                   │
│                                                                    │
│  fixed_interval                                                    │
│  ├── 1d, 12h, 30m, 60s                                            │
│  └── 고정 시간 간격                                                 │
│                                                                    │
│  예시:                                                              │
│  calendar_interval: "month" → 1월(31일), 2월(28/29일)             │
│  fixed_interval: "30d"      → 항상 30일                            │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### filter Aggregation

특정 조건의 문서만 집계합니다.

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "active_products": {
      "filter": {
        "term": { "is_active": true }
      },
      "aggs": {
        "avg_price": { "avg": { "field": "price" } }
      }
    }
  }
}
```

### filters Aggregation

여러 필터를 동시에 적용합니다.

```json
GET /logs/_search
{
  "size": 0,
  "aggs": {
    "log_levels": {
      "filters": {
        "filters": {
          "errors": { "term": { "level": "ERROR" } },
          "warnings": { "term": { "level": "WARN" } },
          "info": { "term": { "level": "INFO" } }
        }
      }
    }
  }
}
```

### nested Aggregation

Nested 필드를 집계합니다.

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "reviews_agg": {
      "nested": {
        "path": "reviews"
      },
      "aggs": {
        "avg_rating": {
          "avg": { "field": "reviews.rating" }
        },
        "rating_distribution": {
          "terms": { "field": "reviews.rating" }
        }
      }
    }
  }
}
```

---

## 4. Pipeline Aggregations

다른 집계 결과를 입력으로 사용합니다.

### derivative (변화량)

```json
GET /orders/_search
{
  "size": 0,
  "aggs": {
    "sales_per_month": {
      "date_histogram": {
        "field": "created_at",
        "calendar_interval": "month"
      },
      "aggs": {
        "total_sales": {
          "sum": { "field": "amount" }
        },
        "sales_change": {
          "derivative": {
            "buckets_path": "total_sales"
          }
        }
      }
    }
  }
}
```

```
┌────────────────────────────────────────────────────────────────────┐
│                    Derivative 시각화                                │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  월별 매출    derivative (변화량)                                   │
│                                                                    │
│  1월: 100만   →  null (첫 번째)                                    │
│  2월: 150만   →  +50만                                             │
│  3월: 120만   →  -30만                                             │
│  4월: 200만   →  +80만                                             │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### cumulative_sum (누적 합계)

```json
GET /orders/_search
{
  "size": 0,
  "aggs": {
    "sales_per_day": {
      "date_histogram": {
        "field": "created_at",
        "calendar_interval": "day"
      },
      "aggs": {
        "daily_sales": {
          "sum": { "field": "amount" }
        },
        "cumulative_sales": {
          "cumulative_sum": {
            "buckets_path": "daily_sales"
          }
        }
      }
    }
  }
}
```

### moving_avg (이동 평균)

```json
GET /orders/_search
{
  "size": 0,
  "aggs": {
    "sales_per_day": {
      "date_histogram": {
        "field": "created_at",
        "calendar_interval": "day"
      },
      "aggs": {
        "daily_sales": {
          "sum": { "field": "amount" }
        },
        "moving_avg_sales": {
          "moving_fn": {
            "buckets_path": "daily_sales",
            "window": 7,
            "script": "MovingFunctions.unweightedAvg(values)"
          }
        }
      }
    }
  }
}
```

### bucket_sort (버킷 정렬 및 페이지네이션)

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "by_category": {
      "terms": {
        "field": "category",
        "size": 100
      },
      "aggs": {
        "total_sales": {
          "sum": { "field": "sales_count" }
        },
        "sort_by_sales": {
          "bucket_sort": {
            "sort": [{ "total_sales": { "order": "desc" } }],
            "from": 0,
            "size": 10
          }
        }
      }
    }
  }
}
```

### bucket_selector (버킷 필터링)

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "by_category": {
      "terms": { "field": "category", "size": 50 },
      "aggs": {
        "avg_price": { "avg": { "field": "price" } },
        "high_price_categories": {
          "bucket_selector": {
            "buckets_path": {
              "avgPrice": "avg_price"
            },
            "script": "params.avgPrice > 100000"
          }
        }
      }
    }
  }
}
```

### percentiles_bucket (버킷 백분위수)

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "by_category": {
      "terms": { "field": "category", "size": 50 },
      "aggs": {
        "avg_price": { "avg": { "field": "price" } }
      }
    },
    "price_percentiles": {
      "percentiles_bucket": {
        "buckets_path": "by_category>avg_price",
        "percents": [25, 50, 75]
      }
    }
  }
}
```

---

## 5. Sub-aggregations

버킷 내에서 추가 집계를 수행합니다.

### 기본 구조

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    "by_category": {                    // 1단계: 카테고리별 버킷
      "terms": { "field": "category" },
      "aggs": {
        "by_brand": {                   // 2단계: 브랜드별 버킷
          "terms": { "field": "brand" },
          "aggs": {
            "avg_price": {              // 3단계: 평균 가격
              "avg": { "field": "price" }
            },
            "total_sales": {            // 3단계: 총 판매량
              "sum": { "field": "sales_count" }
            }
          }
        },
        "category_avg_price": {         // 2단계: 카테고리 평균
          "avg": { "field": "price" }
        }
      }
    }
  }
}
```

### 응답 구조

```
┌────────────────────────────────────────────────────────────────────┐
│                    Sub-aggregation 결과 구조                        │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  aggregations                                                      │
│  └── by_category                                                   │
│      └── buckets[]                                                 │
│          ├── key: "electronics"                                    │
│          ├── doc_count: 500                                        │
│          ├── category_avg_price: { value: 350000 }                │
│          └── by_brand                                              │
│              └── buckets[]                                         │
│                  ├── key: "삼성"                                   │
│                  │   ├── doc_count: 200                           │
│                  │   ├── avg_price: { value: 400000 }             │
│                  │   └── total_sales: { value: 15000 }            │
│                  └── key: "LG"                                     │
│                      ├── doc_count: 150                           │
│                      ├── avg_price: { value: 380000 }             │
│                      └── total_sales: { value: 12000 }            │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

---

## 6. 실전 예제

### E-commerce 대시보드

```json
GET /orders/_search
{
  "size": 0,
  "query": {
    "range": {
      "created_at": {
        "gte": "now-30d/d",
        "lte": "now/d"
      }
    }
  },
  "aggs": {
    // 1. 핵심 지표
    "total_revenue": {
      "sum": { "field": "total_amount" }
    },
    "avg_order_value": {
      "avg": { "field": "total_amount" }
    },
    "unique_customers": {
      "cardinality": { "field": "customer_id" }
    },

    // 2. 일별 매출 추이
    "daily_sales": {
      "date_histogram": {
        "field": "created_at",
        "calendar_interval": "day"
      },
      "aggs": {
        "revenue": { "sum": { "field": "total_amount" } },
        "order_count": { "value_count": { "field": "_id" } },
        "cumulative_revenue": {
          "cumulative_sum": { "buckets_path": "revenue" }
        }
      }
    },

    // 3. 카테고리별 매출
    "by_category": {
      "terms": { "field": "category", "size": 10 },
      "aggs": {
        "revenue": { "sum": { "field": "total_amount" } },
        "avg_price": { "avg": { "field": "unit_price" } }
      }
    },

    // 4. 주문 금액 분포
    "order_amount_distribution": {
      "histogram": {
        "field": "total_amount",
        "interval": 50000
      }
    },

    // 5. 시간대별 주문
    "orders_by_hour": {
      "date_histogram": {
        "field": "created_at",
        "calendar_interval": "hour",
        "format": "HH"
      }
    }
  }
}
```

### 사용자 행동 분석

```json
GET /user_events/_search
{
  "size": 0,
  "query": {
    "range": {
      "timestamp": { "gte": "now-7d/d" }
    }
  },
  "aggs": {
    // 1. 이벤트 유형별 집계
    "by_event_type": {
      "terms": { "field": "event_type" },
      "aggs": {
        "unique_users": {
          "cardinality": { "field": "user_id" }
        }
      }
    },

    // 2. 사용자별 세션 분석
    "user_sessions": {
      "terms": {
        "field": "user_id",
        "size": 100,
        "order": { "session_count": "desc" }
      },
      "aggs": {
        "session_count": {
          "cardinality": { "field": "session_id" }
        },
        "total_events": {
          "value_count": { "field": "_id" }
        },
        "avg_session_duration": {
          "avg": { "field": "session_duration" }
        }
      }
    },

    // 3. 퍼널 분석
    "funnel": {
      "filters": {
        "filters": {
          "view_product": { "term": { "event_type": "view_product" } },
          "add_to_cart": { "term": { "event_type": "add_to_cart" } },
          "checkout": { "term": { "event_type": "checkout" } },
          "purchase": { "term": { "event_type": "purchase" } }
        }
      },
      "aggs": {
        "unique_users": {
          "cardinality": { "field": "user_id" }
        }
      }
    },

    // 4. 디바이스별 분석
    "by_device": {
      "terms": { "field": "device_type" },
      "aggs": {
        "unique_users": { "cardinality": { "field": "user_id" } },
        "avg_session_duration": { "avg": { "field": "session_duration" } }
      }
    }
  }
}
```

### 로그 분석

```json
GET /application_logs/_search
{
  "size": 0,
  "query": {
    "range": {
      "@timestamp": { "gte": "now-24h" }
    }
  },
  "aggs": {
    // 1. 로그 레벨별 집계
    "by_level": {
      "terms": { "field": "level" }
    },

    // 2. 시간대별 에러 추이
    "errors_over_time": {
      "filter": {
        "term": { "level": "ERROR" }
      },
      "aggs": {
        "by_time": {
          "date_histogram": {
            "field": "@timestamp",
            "fixed_interval": "1h"
          },
          "aggs": {
            "by_service": {
              "terms": { "field": "service_name" }
            }
          }
        }
      }
    },

    // 3. 에러 유형별 Top 10
    "top_errors": {
      "filter": {
        "term": { "level": "ERROR" }
      },
      "aggs": {
        "by_message": {
          "terms": {
            "field": "error_type",
            "size": 10
          },
          "aggs": {
            "sample_messages": {
              "top_hits": {
                "size": 3,
                "_source": ["message", "@timestamp", "stack_trace"]
              }
            }
          }
        }
      }
    },

    // 4. 서비스별 응답 시간 백분위수
    "response_times": {
      "terms": { "field": "service_name" },
      "aggs": {
        "percentiles": {
          "percentiles": {
            "field": "response_time_ms",
            "percents": [50, 90, 95, 99]
          }
        }
      }
    }
  }
}
```

### 제품 리뷰 분석

```json
GET /products/_search
{
  "size": 0,
  "aggs": {
    // Nested reviews 집계
    "reviews_analysis": {
      "nested": { "path": "reviews" },
      "aggs": {
        // 1. 평점 분포
        "rating_distribution": {
          "terms": { "field": "reviews.rating" }
        },

        // 2. 월별 리뷰 수
        "reviews_over_time": {
          "date_histogram": {
            "field": "reviews.created_at",
            "calendar_interval": "month"
          },
          "aggs": {
            "avg_rating": {
              "avg": { "field": "reviews.rating" }
            }
          }
        },

        // 3. 평점 통계
        "rating_stats": {
          "extended_stats": { "field": "reviews.rating" }
        },

        // 4. 상위 리뷰어
        "top_reviewers": {
          "terms": {
            "field": "reviews.user_id",
            "size": 10
          },
          "aggs": {
            "avg_rating": {
              "avg": { "field": "reviews.rating" }
            }
          }
        }
      }
    }
  }
}
```

---

## 핵심 요약

```
┌─────────────────────────────────────────────────────────────────────┐
│                 Aggregation Quick Reference                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Metric Aggregations (값 계산)                                       │
│  ├── avg, sum, min, max: 기본 통계                                   │
│  ├── stats, extended_stats: 통계 모음                                │
│  ├── cardinality: 고유값 개수 (근사치)                               │
│  ├── percentiles: 백분위수                                          │
│  └── top_hits: 상위 문서                                             │
│                                                                      │
│  Bucket Aggregations (그룹화)                                        │
│  ├── terms: 필드값별 버킷                                            │
│  ├── range: 숫자 범위별 버킷                                         │
│  ├── date_histogram: 시간 간격별 버킷                                │
│  ├── histogram: 숫자 간격별 버킷                                     │
│  ├── filter/filters: 조건별 버킷                                     │
│  └── nested: nested 필드 집계                                        │
│                                                                      │
│  Pipeline Aggregations (집계 후처리)                                  │
│  ├── derivative: 변화량                                              │
│  ├── cumulative_sum: 누적 합계                                       │
│  ├── moving_fn: 이동 평균                                            │
│  ├── bucket_sort: 버킷 정렬                                          │
│  └── bucket_selector: 버킷 필터링                                    │
│                                                                      │
│  Best Practices                                                      │
│  ├── size: 0 으로 검색 결과 제외 (집계만 필요할 때)                   │
│  ├── keyword 필드로 terms 집계                                        │
│  ├── sub-aggregation으로 다차원 분석                                  │
│  └── Pipeline으로 시계열 분석                                         │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 다음 단계

- [Query DSL](./es-query-dsl.md) - 검색 쿼리 작성
- [Korean Analysis](./es-korean.md) - 한글 분석기
- [Optimization](./es-optimization.md) - 성능 최적화
